package net.paulem.launchermc;

import fr.flowarg.flowlogger.ILogger;
import fr.flowarg.flowlogger.Logger;
import fr.litarvan.openauth.microsoft.MicrosoftAuthResult;
import fr.litarvan.openauth.microsoft.MicrosoftAuthenticationException;
import fr.litarvan.openauth.microsoft.MicrosoftAuthenticator;
import fr.theshark34.openlauncherlib.minecraft.util.GameDirGenerator;
import lombok.Getter;
import lombok.Setter;
import net.paulem.launchermc.discord.RPC;
import net.paulem.launchermc.ui.panels.PanelManager;
import net.paulem.launchermc.ui.panels.pages.SideBar;
import net.paulem.launchermc.ui.panels.pages.Login;
import fr.theshark34.openlauncherlib.minecraft.AuthInfos;
import fr.theshark34.openlauncherlib.util.Saver;
import net.paulem.launchermc.updater.Updater;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

public final class Launcher extends Application {
    @Getter
    private static Launcher instance;

    @Getter
    private final ILogger logger;
    @Getter
    private final Path launcherDir = GameDirGenerator.createGameDir("paulem-launcher", true);
    @Getter
    private final Saver saver;
    private PanelManager panelManager;
    @Setter
    @Getter
    private AuthInfos authInfos = null;

    @Getter
    private final RPC discordRPC;

    public Launcher() {
        instance = this;
        this.logger = new Logger("[Launcher]", this.launcherDir.resolve("launcher.log"));

        this.logger.info("Running LauncherMC v" + getVersion());

        if (Files.notExists(this.launcherDir)) {
            try {
                Files.createDirectory(this.launcherDir);
            } catch (IOException e) {
                this.logger.err("Unable to create launcher folder");
                this.logger.printStackTrace(e);
            }
        }

        this.saver = new Saver(this.launcherDir.resolve("config.properties"));
        this.saver.load();

        try {
            Updater updater = new Updater();
            updater.checkForUpdatesAsync();
        } catch (Exception e) {
            this.logger.err("Unable to check for updates");
            this.logger.printStackTrace(e);
        }

        this.discordRPC = new RPC(this.logger);
        this.discordRPC.startPresence();
    }

    @Override
    public void start(Stage stage) {
        this.logger.info("Lancement du launcher...");
        this.panelManager = new PanelManager(this, stage);
        this.panelManager.init();

        if (this.isUserAlreadyLoggedIn()) {
            this.logger.info("Salut " + authInfos.getUsername() + " !");

            this.panelManager.showPanel(new SideBar());
        } else {
            this.panelManager.showPanel(new Login());
        }
    }

    public boolean isUserAlreadyLoggedIn() {
        if (saver.get("msAccessToken") != null && saver.get("msRefreshToken") != null) {
            try {
                MicrosoftAuthenticator authenticator = new MicrosoftAuthenticator();
                MicrosoftAuthResult response = authenticator.loginWithRefreshToken(saver.get("msRefreshToken"));

                saver.set("msAccessToken", response.getAccessToken());
                saver.set("msRefreshToken", response.getRefreshToken());
                saver.save();
                this.setAuthInfos(new AuthInfos(
                        response.getProfile().getName(),
                        response.getAccessToken(),
                        response.getProfile().getId(),
                        response.getXuid(),
                        response.getClientId()
                ));
                return true;
            } catch (MicrosoftAuthenticationException e) {
                saver.remove("msAccessToken");
                saver.remove("msRefreshToken");
                saver.save();
            }
        } else if (saver.get("offline-username") != null) {
            this.authInfos = new AuthInfos(saver.get("offline-username"), UUID.randomUUID().toString(), UUID.randomUUID().toString());
            return true;
        }

        return false;
    }

    @Override
    public void stop() {
        Platform.exit();
        System.exit(0);
    }

    public void hideWindow() {
        this.panelManager.getStage().setIconified(true);
    }
    
    public void showWindow() {
        this.panelManager.getStage().setIconified(false);
    }
    
    @Nullable
    public String getVersion() {
        return getClass().getPackage().getImplementationVersion();
    }
}
