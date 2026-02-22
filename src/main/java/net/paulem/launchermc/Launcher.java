package net.paulem.launchermc;

import fr.flowarg.flowlogger.ILogger;
import fr.flowarg.flowlogger.Logger;
import fr.litarvan.openauth.microsoft.MicrosoftAuthResult;
import fr.litarvan.openauth.microsoft.MicrosoftAuthenticationException;
import fr.litarvan.openauth.microsoft.MicrosoftAuthenticator;
import net.paulem.launchermc.discord.RPC;
import net.paulem.launchermc.ui.panels.PanelManager;
import net.paulem.launchermc.ui.panels.pages.SideBar;
import net.paulem.launchermc.ui.panels.pages.Login;
import fr.theshark34.openlauncherlib.minecraft.AuthInfos;
import fr.theshark34.openlauncherlib.util.Saver;
import net.paulem.launchermc.utils.GameUtils;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

public final class Launcher extends Application {
    private static Launcher instance;

    private final ILogger logger;
    private final Path launcherDir = GameUtils.createGameDir("paulem-launcher", true);
    private final Saver saver;
    private PanelManager panelManager;
    private AuthInfos authInfos = null;

    private final RPC discordRPC;

    public Launcher() {
        instance = this;
        this.logger = new Logger("[Launcher]", this.launcherDir.resolve("launcher.log"));
        
        this.logger.info(this.launcherDir.toString());

        if (Files.notExists(this.launcherDir))
        {
            try
            {
                Files.createDirectory(this.launcherDir);
            } catch (IOException e)
            {
                this.logger.err("Unable to create launcher folder");
                this.logger.printStackTrace(e);
            }
        }

        this.saver = new Saver(this.launcherDir.resolve("config.properties"));
        this.saver.load();

        this.discordRPC = new RPC(this.logger);
        this.discordRPC.startPresence();
    }

    public static Launcher getInstance() {
        return instance;
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

    public AuthInfos getAuthInfos() {
        return authInfos;
    }

    public void setAuthInfos(AuthInfos authInfos) {
        this.authInfos = authInfos;
    }

    public ILogger getLogger() {
        return logger;
    }

    public Saver getSaver() {
        return saver;
    }

    public Path getLauncherDir() {
        return launcherDir;
    }

    public RPC getDiscordRPC() {
        return discordRPC;
    }

    @Override
    public void stop() {
        Platform.exit();
        System.exit(0);
    }

    public void hideWindow() {
        this.panelManager.getStage().hide();
    }
}
