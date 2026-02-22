package net.paulem.launchermc.game;

import fr.flowarg.flowlogger.ILogger;
import fr.litarvan.openauth.microsoft.MicrosoftAuthenticator;
import fr.theshark34.openlauncherlib.minecraft.AuthInfos;
import fr.theshark34.openlauncherlib.util.Saver;
import net.paulem.launchermc.Launcher;
import net.paulem.launchermc.ui.panels.PanelManager;
import net.paulem.launchermc.ui.panels.pages.SideBar;
import javafx.application.Platform;
import javafx.scene.control.Alert;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

public record Authentification(PanelManager panelManager, Saver saver, AtomicBoolean offlineAuth, ILogger logger) {
    public void authenticate(String user) {
        AuthInfos infos = new AuthInfos(
                user,
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString()
        );
        saver.set("offline-username", infos.getUsername());
        saver.save();
        Launcher.getInstance().setAuthInfos(infos);

        this.logger.info("Hello " + infos.getUsername());

        panelManager.showPanel(new SideBar());
    }

    public void authenticateMS() {
        offlineAuth.set(false);
        MicrosoftAuthenticator authenticator = new MicrosoftAuthenticator();
        authenticator.loginWithAsyncWebview().whenComplete((response, error) -> {
            if (error != null) {
                this.logger.err(error.toString());
                Platform.runLater(()-> {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Erreur");
                    alert.setContentText(error.getMessage());
                    alert.show();
                });

                return;
            }

            saver.set("msAccessToken", response.getAccessToken());
            saver.set("msRefreshToken", response.getRefreshToken());
            saver.save();
            Launcher.getInstance().setAuthInfos(new AuthInfos(
                    response.getProfile().getName(),
                    response.getAccessToken(),
                    response.getProfile().getId(),
                    response.getXuid(),
                    response.getClientId()
            ));

            this.logger.info("Hello " + response.getProfile().getName());

            Platform.runLater(() -> panelManager.showPanel(new SideBar()));
        });
    }
}
