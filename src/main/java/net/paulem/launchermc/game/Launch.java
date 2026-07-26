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
import net.paulem.launchermc.utils.GameUtils;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.util.List;
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

            String rawModLoaderVersion = updater != null
                    ? updater.getModLoaderVersion().getModLoaderVersion()
                    : MinecraftInfos.MODLOADER_VERSION;

            String modLoaderVersion = rawModLoaderVersion.split("-").length >= 2
                    ? rawModLoaderVersion.split("-")[1]
                    : rawModLoaderVersion;
            
            noFramework.getAdditionalVmArgs().add(this.getRamArgsFromSaver());

            noFramework.setLastCallback(externalLauncher -> {
                List<String> vmArgs = externalLauncher.getVmArgs();

                // Récupération des réglages
                String falseStr = "false";
                boolean enableMangoHud = saver.get(Constants.CONFIG_ENABLE_MANGOHUD, falseStr).equals("true");
                boolean enableGameMode = saver.get(Constants.CONFIG_ENABLE_FERAL_GAMEMODE, falseStr).equals("true"); // Supposant cette constante
                boolean enableZink = saver.get(Constants.CONFIG_ENABLE_ZINK, falseStr).equals("true");
                boolean enableDGPU = saver.get(Constants.CONFIG_ENABLE_DGPU, falseStr).equals("true");

                // Détection hardware
                boolean isNvidia = GameUtils.hasNvidiaGPU();
                logger.info("NVIDIA GPU detected : " + isNvidia);
                
                if (enableMangoHud) {
                    vmArgs.add("mangohud");
                }
                if (enableGameMode) {
                    vmArgs.add("gamemoderun");
                }
                
                if(enableDGPU || enableZink) {
                    vmArgs.add("env");
                }

                if (enableDGPU) {
                    if (isNvidia) {
                        // NVIDIA PRIME Offload
                        vmArgs.add("__NV_PRIME_RENDER_OFFLOAD=1");
                        vmArgs.add("__VK_LAYER_NV_optimus=NVIDIA_only");

                        if (enableZink) {
                            // Zink sur NVIDIA
                            vmArgs.add("__GLX_VENDOR_LIBRARY_NAME=mesa");
                            vmArgs.add("__EGL_VENDOR_LIBRARY_FILENAMES=/usr/share/glvnd/egl_vendor.d/50_mesa.json");
                            vmArgs.add("MESA_LOADER_DRIVER_OVERRIDE=zink");
                            vmArgs.add("GALLIUM_DRIVER=zink");
                        } else {
                            vmArgs.add("__GLX_VENDOR_LIBRARY_NAME=nvidia");
                        }
                    } else {
                        // AMD ou Intel dGPU
                        vmArgs.add("DRI_PRIME=1");
                        if (enableZink) {
                            vmArgs.add("MESA_LOADER_DRIVER_OVERRIDE=zink");
                            vmArgs.add("GALLIUM_DRIVER=zink");
                        }
                    }
                } else if (enableZink) {
                    // iGPU (Intel/AMD) + Zink
                    vmArgs.add("MESA_LOADER_DRIVER_OVERRIDE=zink");
                    vmArgs.add("GALLIUM_DRIVER=zink");
                }

                // Sécurité pour Kopper (nécessaire pour Zink dans certains cas)
                if (enableZink) {
                    vmArgs.add("LIBGL_KOPPER_DRI2=1");
                }

                externalLauncher.setVmArgs(vmArgs);
            });
            
            Process p = noFramework.launch(gameVersion,
                    modLoaderVersion,
                    MinecraftInfos.MODLOADER);

            Launcher.getInstance().getDiscordRPC().editPresence(Constants.RPC_CONNECTED);
            Platform.runLater(() -> Launcher.getInstance().hideWindow());

            new Thread(() -> checkStopped(p)).start();
        } catch (Exception e) {
            this.logger.printStackTrace(e);

            home.setDownloadingOrPlaying(false);
            Platform.runLater(() -> {
                Launcher.getInstance().showWindow();
                home.showPlayButton();
            });
            
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
            Platform.runLater(() -> {
                Launcher.getInstance().showWindow();
                home.showPlayButton();
            });

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
        } catch (NumberFormatException exception) {
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
