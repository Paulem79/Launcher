package net.paulem.launchermc.game;

import fr.flowarg.flowlogger.ILogger;
import fr.flowarg.flowupdater.FlowUpdater;
import fr.flowarg.flowupdater.download.DownloadList;
import fr.flowarg.flowupdater.download.IProgressCallback;
import fr.flowarg.flowupdater.download.Step;
import fr.flowarg.flowupdater.versions.VanillaVersion;
import fr.flowarg.openlauncherlib.NoFramework;
import fr.theshark34.openlauncherlib.minecraft.GameFolder;
import fr.theshark34.openlauncherlib.util.Saver;
import net.paulem.launchermc.Launcher;
import net.paulem.launchermc.game.minecraft.MinecraftInfos;
import net.paulem.launchermc.game.minecraft.MinecraftVersion;
import net.paulem.launchermc.ui.panels.pages.content.Home;
import net.paulem.launchermc.utils.Constants;
import net.paulem.launchermc.utils.Errors;
import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.GridPane;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.util.Objects;

public record Launch(Home home, Saver saver, ILogger logger, GridPane boxPane, ProgressBar progressBar, Label stepLabel, Label fileLabel) {
    public void play() {
        home.setDownloadingOrPlaying(true);
        boxPane.getChildren().clear();
        setProgress(0, 0);
        boxPane.getChildren().addAll(progressBar, stepLabel, fileLabel);

        new Thread(this::update).start();
    }

    private void update() {
        IProgressCallback callback = new IProgressCallback() {
            private final DecimalFormat decimalFormat = new DecimalFormat("#.#");
            private String stepTxt = "";
            private String percentTxt = "0.0%";

            @Override
            public void step(Step step) {
                Platform.runLater(() -> {
                    StepInfo stepInfo = StepInfo.valueOf(step.name());
                    stepTxt = stepInfo.getDetails();
                    if(stepInfo == StepInfo.END) setStatus(String.format("%s", stepTxt));
                    else if(Objects.equals(percentTxt, "100%")) setStatus(String.format("%s", StepInfo.END.getDetails()));
                    else setStatus(String.format("%s (%s)", stepTxt, percentTxt));
                });
            }

            @Override
            public void update(DownloadList.DownloadInfo info) {
                Platform.runLater(() -> {
                    percentTxt = decimalFormat.format(info.getDownloadedBytes() * 100.d / info.getTotalToDownloadBytes());
                    setProgress(info.getDownloadedBytes(), info.getTotalToDownloadBytes());
                    if(!percentTxt.matches(".*\\d.*")) {
                        percentTxt = "100";
                        setProgress(1, 1);
                    }
                    percentTxt += "%";
                    setStatus(String.format("%s (%s)", stepTxt, percentTxt));
                });
            }

            @Override
            public void onFileDownloaded(Path path) {
                Platform.runLater(() -> {
                    String p = path.toString();
                    fileLabel.setText("..." + p.replace(Launcher.getInstance().getLauncherDir().toFile().getAbsolutePath(), ""));
                });
            }
        };

        try {
            try {
                Launcher.getInstance().getDiscordRPC().editPresence(Constants.RPC_UPDATE);
            } catch (Exception ignored) {}

            final VanillaVersion vanillaVersion = new VanillaVersion.VanillaVersionBuilder()
                    .withName(MinecraftInfos.GAME_VERSION)
                    .withSnapshot(false)
                    .build();

            final FlowUpdater updater = new FlowUpdater.FlowUpdaterBuilder()
                    .withVanillaVersion(vanillaVersion)
                    .withModLoaderVersion(MinecraftVersion.GAME)
                    .withLogger(this.logger)
                    .withProgressCallback(callback)
                    .build();

            updater.update(Launcher.getInstance().getLauncherDir());

            this.startGame(updater, updater.getVanillaVersion().getName());
        } catch (Exception e) {
            this.logger.printStackTrace(e);
            this.logger.info("Lancement en mode hors-ligne...");
            
            Platform.runLater(() -> {
                setProgress(1, 1);
                setStatus(String.format("%s", StepInfo.OFFLINE.getDetails()));
            });
            
            this.startGame(null, MinecraftInfos.GAME_VERSION);
        }
    }

    private void startGame(@Nullable FlowUpdater updater, String gameVersion) {
        try {
            NoFramework noFramework = new NoFramework(
                    Launcher.getInstance().getLauncherDir(),
                    Launcher.getInstance().getAuthInfos(),
                    GameFolder.FLOW_UPDATER
            );
            
            String modLoaderVersion = updater != null ? updater.getModLoaderVersion().getModLoaderVersion()
                    : MinecraftInfos.MODLOADER_VERSION.split("-").length >= 2 ? MinecraftInfos.MODLOADER_VERSION.split("-")[1] : MinecraftInfos.MODLOADER_VERSION;
            
            noFramework.getAdditionalVmArgs().add(this.getRamArgsFromSaver());
            Process p = noFramework.launch(gameVersion,
                    modLoaderVersion,
                    MinecraftInfos.MODLOADER);

            Launcher.getInstance().getDiscordRPC().editPresence(Constants.RPC_CONNECTED);

            new Thread(() -> checkStopped(p)).start();
        } catch (Exception e) {
            this.logger.printStackTrace(e);
            JOptionPane.showMessageDialog(
                    null,
                    "Une erreur est survenue ! Merci d'envoyer ceci à paulem :\n" + Errors.getStackTrace("Une erreur est survenue :", e),
                    "Erreur",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private void checkStopped(Process p) {
        try {
            p.waitFor();
            home.setDownloadingOrPlaying(false);
            Platform.runLater(home::showPlayButton);

            Launcher.getInstance().getDiscordRPC().editPresence(Constants.RPC_LAUNCHER);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public String getRamArgsFromSaver() {
        int val = Constants.DEFAULT_MAX_RAM.get();
        try {
            if (saver.get(Constants.CONFIG_MAXRAM) != null) {
                val = Integer.parseInt(saver.get(Constants.CONFIG_MAXRAM));
            } else {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException _) {
            saver.set(Constants.CONFIG_MAXRAM, String.valueOf(val));
            saver.save();
        }

        return "-Xmx" + val + "M";
    }

    public void setStatus(String status) {
        this.stepLabel.setText(status);
    }

    public void setProgress(double current, double max) {
        this.progressBar.setProgress(current / max);
    }

    @SuppressWarnings("unused")
    public enum StepInfo {
        READ("Lecture du fichier json..."),
        DL_LIBS("Téléchargement des libraries..."),
        DL_ASSETS("Téléchargement des ressources..."),
        EXTRACT_NATIVES("Extraction des natives..."),
        FORGE("Installation de forge..."),
        FABRIC("Installation de fabric..."),
        MODS("Téléchargement des mods..."),
        EXTERNAL_FILES("Téléchargement des fichier externes..."),
        POST_EXECUTIONS("Exécution post-installation..."),
        MOD_LOADER("Installation du mod loader..."),
        INTEGRATION("Intégration des mods..."),
        END("Terminé ! Lancement du jeu en cours..."),
        OFFLINE("Lancement du jeu en mode hors-ligne...");

        final String details;

        StepInfo(String details) {
            this.details = details;
        }

        public String getDetails() {
            return details;
        }
    }
}
