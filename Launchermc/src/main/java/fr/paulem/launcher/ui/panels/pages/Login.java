package fr.paulem.launcher.ui.panels.pages;

import fr.litarvan.openauth.microsoft.MicrosoftAuthenticator;
import fr.paulem.launcher.Launcher;
import fr.paulem.launcher.ui.PanelManager;
import fr.paulem.launcher.ui.panel.Panel;
import fr.theshark34.openlauncherlib.minecraft.AuthInfos;
import fr.theshark34.openlauncherlib.util.Saver;
import javafx.application.Platform;
import javafx.geometry.HPos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

public class Login extends Panel {
    final GridPane loginCard = new GridPane();

    final Saver saver = Launcher.getInstance().getSaver();
    final AtomicBoolean offlineAuth = new AtomicBoolean(false);

    final TextField userField = new TextField();
    final Label userErrorLabel = new Label();
    final Button btnLogin = new Button("Connexion");
    final Button msLoginBtn = new Button();

    @Override
    public String getName() {
        return null;
    }

    @Override
    public String getStylesheetPath() {
        return "css/login.css";
    }

    @Override
    public void init(PanelManager panelManager) {
        super.init(panelManager);

        // Background
        this.layout.getStyleClass().add("login-layout");

        ColumnConstraints columnConstraints = new ColumnConstraints();
        columnConstraints.setHalignment(HPos.LEFT);
        columnConstraints.setMinWidth(350);
        columnConstraints.setMaxWidth(350);
        this.layout.getColumnConstraints().addAll(columnConstraints, new ColumnConstraints());
        this.layout.add(loginCard, 0, 0);

        // Background image
        GridPane bgImage = new GridPane();
        setCanTakeAllSize(bgImage);
        bgImage.getStyleClass().add("bg-image");
        this.layout.add(bgImage, 1, 0);

        // Login card
        setCanTakeAllSize(this.layout);
        loginCard.getStyleClass().add("login-card");
        setLeft(loginCard);
        setCenterH(loginCard);
        setCenterV(loginCard);

        /*
         * Login sidebar
         */
        Label title = new Label("JavaFX Launcher");
        title.setFont(Font.font("Consolas", FontWeight.BOLD, FontPosture.REGULAR, 30f));
        title.getStyleClass().add("login-title");
        setCenterH(title);
        setCanTakeAllSize(title);
        setTop(title);
        title.setTextAlignment(TextAlignment.CENTER);
        title.setTranslateY(30d);
        loginCard.getChildren().add(title);

        // Username/E-Mail
        setCanTakeAllSize(userField);
        setCenterV(userField);
        setCenterH(userField);
        userField.setPromptText("Pseudo");
        userField.setMaxWidth(300);
        userField.setTranslateY(-70d);
        userField.getStyleClass().add("login-input");
        userField.textProperty().addListener((_a, oldValue, newValue) -> this.updateLoginBtnState(userField, userErrorLabel));

        // User error
        setCanTakeAllSize(userErrorLabel);
        setCenterV(userErrorLabel);
        setCenterH(userErrorLabel);
        userErrorLabel.getStyleClass().add("login-error");
        userErrorLabel.setTranslateY(-45d);
        userErrorLabel.setMaxWidth(280);
        userErrorLabel.setTextAlignment(TextAlignment.LEFT);

        // Login button
        setCanTakeAllSize(btnLogin);
        setCenterV(btnLogin);
        setCenterH(btnLogin);
        btnLogin.setDisable(true);
        btnLogin.setMaxWidth(300);
        //btnLogin.setTranslateY(20d);
        btnLogin.getStyleClass().add("login-log-btn");
        btnLogin.setOnMouseClicked(e -> this.authenticate(userField.getText()));

        Separator separator = new Separator();
        setCanTakeAllSize(separator);
        setCenterH(separator);
        setCenterV(separator);
        separator.getStyleClass().add("login-separator");
        separator.setMaxWidth(300);
        separator.setTranslateY(40d);

        // Login with label
        Label loginWithLabel = new Label("Ou se connecter avec:".toUpperCase());
        setCanTakeAllSize(loginWithLabel);
        setCenterV(loginWithLabel);
        setCenterH(loginWithLabel);
        loginWithLabel.setFont(Font.font(loginWithLabel.getFont().getFamily(), FontWeight.BOLD, FontPosture.REGULAR, 14d));
        loginWithLabel.getStyleClass().add("login-with-label");
        loginWithLabel.setTranslateY(60d);
        loginWithLabel.setMaxWidth(280d);

        // Microsoft login button
        ImageView view = new ImageView(new Image("images/microsoft.png"));
        view.setPreserveRatio(true);
        view.setFitHeight(30d);
        setCanTakeAllSize(msLoginBtn);
        setCenterH(msLoginBtn);
        setCenterV(msLoginBtn);
        msLoginBtn.getStyleClass().add("ms-login-btn");
        msLoginBtn.setMaxWidth(300);
        msLoginBtn.setTranslateY(95d);
        msLoginBtn.setGraphic(view);
        msLoginBtn.setOnMouseClicked(e -> this.authenticateMS());

        loginCard.getChildren().addAll(userField, userErrorLabel, btnLogin, separator, loginWithLabel, msLoginBtn);
    }

    public void updateLoginBtnState(TextField textField, Label errorLabel) {
        offlineAuth.set(!userField.getText().isEmpty());

        if (textField.getText().isEmpty()) {
            errorLabel.setText("Le champ ne peut être vide");
        } else {
            errorLabel.setText("");
        }

        btnLogin.setDisable(userField.getText().isEmpty());
    }

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

        panelManager.showPanel(new App());
    }

    public void authenticateMS() {
        offlineAuth.set(false);
        MicrosoftAuthenticator authenticator = new MicrosoftAuthenticator();
        authenticator.loginWithAsyncWebview().whenComplete((response, error) -> {
            if (error != null) {
                Launcher.getInstance().getLogger().err(error.toString());
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

            Launcher.getInstance().getLogger().info("Hello " + response.getProfile().getName());

            Platform.runLater(() -> panelManager.showPanel(new App()));
        });
    }
}