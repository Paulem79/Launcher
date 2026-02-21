package ovh.paulem.launchermc.ui.panels.pages;

import animatefx.animation.FadeIn;
import ovh.paulem.launchermc.Launcher;
import ovh.paulem.launchermc.game.Authentification;
import ovh.paulem.launchermc.ui.panels.PanelManager;
import ovh.paulem.launchermc.ui.panels.Panel;
import ovh.paulem.launchermc.utils.Constants;
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
import ovh.paulem.launchermc.ui.components.GradientButton;

import java.util.concurrent.atomic.AtomicBoolean;

public class Login extends Panel {
    private Authentification authentification;

    private final GridPane loginCard = new GridPane();

    private final AtomicBoolean offlineAuth = new AtomicBoolean(false);

    private final TextField userField = new TextField();
    private final Label userErrorLabel = new Label();
    private final GradientButton btnLogin = new GradientButton("Connexion", 18, 1.04);
    private final Button msLoginBtn = new Button();

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

        this.authentification = new Authentification(panelManager, saver, offlineAuth, logger);

        Launcher.getInstance().getDiscordRPC().editPresence(Constants.RPC_LOGIN);

        // Background
        this.layout.getStyleClass().add("login-layout");

        ColumnConstraints columnConstraints = new ColumnConstraints();
        columnConstraints.setHalignment(HPos.LEFT);
        columnConstraints.setMinWidth(380);
        columnConstraints.setMaxWidth(380);
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
        Label title = new Label("Launcher MC");
        title.setFont(Font.font("Poppins", FontWeight.BOLD, FontPosture.REGULAR, 28f));
        title.getStyleClass().add("login-title");
        setCenterH(title);
        setCanTakeAllSize(title);
        setTop(title);
        title.setTextAlignment(TextAlignment.CENTER);
        title.setTranslateY(Constants.TITLE_OFFSET_Y);
        loginCard.getChildren().add(title);

        // Username
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
        userErrorLabel.setTranslateY(-35d);
        userErrorLabel.setMaxWidth(280);
        userErrorLabel.setTextAlignment(TextAlignment.LEFT);

        // Login button
        setCanTakeAllSize(btnLogin);
        setCenterV(btnLogin);
        setCenterH(btnLogin);
        btnLogin.setDisable(true);
        btnLogin.setMaxWidth(300);
        btnLogin.getStyleClass().add("login-log-btn");
        btnLogin.setOnMouseClicked(e -> authentification.authenticate(userField.getText()));

        Separator separator = new Separator();
        setCanTakeAllSize(separator);
        setCenterH(separator);
        setCenterV(separator);
        separator.getStyleClass().add("login-separator");
        separator.setMaxWidth(300);
        separator.setTranslateY(40d);

        // Login with label
        Label loginWithLabel = new Label("Ou se connecter avec :".toUpperCase());
        setCanTakeAllSize(loginWithLabel);
        setCenterV(loginWithLabel);
        setCenterH(loginWithLabel);
        loginWithLabel.setFont(Font.font("Poppins", FontWeight.BOLD, FontPosture.REGULAR, 12d));
        loginWithLabel.getStyleClass().add("login-with-label");
        loginWithLabel.setTranslateY(60d);
        loginWithLabel.setMaxWidth(280d);

        // Microsoft login button
        ImageView view = new ImageView(new Image("images/assets/microsoft.png"));
        view.setPreserveRatio(true);
        view.setFitHeight(30d);
        view.setSmooth(true);
        setCanTakeAllSize(msLoginBtn);
        setCenterH(msLoginBtn);
        setCenterV(msLoginBtn);
        msLoginBtn.getStyleClass().add("ms-login-btn");
        msLoginBtn.setMaxWidth(300);
        msLoginBtn.setTranslateY(95d);
        msLoginBtn.setGraphic(view);
        msLoginBtn.setOnMouseClicked(e -> authentification.authenticateMS());

        loginCard.getChildren().addAll(userField, userErrorLabel, btnLogin, separator, loginWithLabel, msLoginBtn);
    }

    @Override
    public void onShow() {
        super.onShow();
        // Animate the login card appearance
        new FadeIn(loginCard).setSpeed(0.7).play();
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
}