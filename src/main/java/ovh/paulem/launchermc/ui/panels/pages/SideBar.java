package ovh.paulem.launchermc.ui.panels.pages;

import animatefx.animation.FadeIn;
import fr.flowarg.materialdesignfontfx.MaterialDesignIcon;
import fr.flowarg.materialdesignfontfx.MaterialDesignIconView;
import ovh.paulem.launchermc.Launcher;
import ovh.paulem.launchermc.ui.panels.PanelManager;
import ovh.paulem.launchermc.ui.panels.Panel;
import ovh.paulem.launchermc.ui.panels.pages.content.ContentPanel;
import ovh.paulem.launchermc.ui.panels.pages.content.Home;
import ovh.paulem.launchermc.ui.panels.pages.content.Settings;
import ovh.paulem.launchermc.utils.Constants;
import javafx.geometry.HPos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

public class SideBar extends Panel {
    private final GridPane sidemenu = new GridPane();
    private final GridPane navContent = new GridPane();

    private Node activeLink = null;
    private ContentPanel currentPage = null;

    private Button homeBtn, settingsBtn, newsBtn, storeBtn;

    @Override
    public String getName() {
        return null;
    }

    @Override
    public String getStylesheetPath() {
        return "css/sidebar.css";
    }

    @Override
    public void init(PanelManager panelManager) {
        super.init(panelManager);

        // Background
        this.layout.getStyleClass().add("app-layout");
        setCanTakeAllSize(this.layout);

        ColumnConstraints columnConstraints = new ColumnConstraints();
        columnConstraints.setHalignment(HPos.LEFT);
        columnConstraints.setMinWidth(260);
        columnConstraints.setMaxWidth(260);
        this.layout.getColumnConstraints().addAll(columnConstraints, new ColumnConstraints());

        // Side menu
        this.layout.add(sidemenu, 0, 0);
        sidemenu.getStyleClass().add("sidemenu");
        setLeft(sidemenu);
        setCenterH(sidemenu);
        setCenterV(sidemenu);

        // Background Image
        GridPane bgImage = new GridPane();
        setCanTakeAllSize(bgImage);
        bgImage.getStyleClass().add("bg-image");
        this.layout.add(bgImage, 1, 0);

        // Nav content
        this.layout.add(navContent, 1, 0);
        navContent.getStyleClass().add("nav-content");
        setLeft(navContent);
        setCenterH(navContent);
        setCenterV(navContent);

        /*
         * Side menu
         */
        // Titre
        Label title = new Label("Launcher MC");
        title.setFont(Font.font("Poppins", FontWeight.BOLD, FontPosture.REGULAR, 24f));
        title.getStyleClass().add("home-title");
        setCenterH(title);
        setCanTakeAllSize(title);
        setTop(title);
        title.setTextAlignment(TextAlignment.CENTER);
        title.setTranslateY(Constants.TITLE_OFFSET_Y);
        sidemenu.getChildren().add(title);

        // Navigation
        homeBtn = new Button("Accueil");
        homeBtn.getStyleClass().add("sidemenu-nav-btn");
        final var homeIcon = new MaterialDesignIconView<>(MaterialDesignIcon.H.HOME);
        homeIcon.getStyleClass().add("sidemenu-nav-btn-icon");
        homeIcon.setTranslateY(Constants.NAVBUTTON_OFFSET_Y);
        homeBtn.setGraphic(homeIcon);
        setCanTakeAllSize(homeBtn);
        setTop(homeBtn);
        homeBtn.setTranslateY(80d);
        homeBtn.setOnMouseClicked(e -> setPage(new Home(), homeBtn));

        newsBtn = new Button("Actualités");
        newsBtn.getStyleClass().add("sidemenu-nav-btn");
        final var newsIcon = new MaterialDesignIconView<>(MaterialDesignIcon.N.NEWSPAPER);
        newsIcon.getStyleClass().add("sidemenu-nav-btn-icon");
        newsIcon.setTranslateY(Constants.NAVBUTTON_OFFSET_Y);
        newsBtn.setGraphic(newsIcon);
        setCanTakeAllSize(newsBtn);
        setTop(newsBtn);
        newsBtn.setTranslateY(124d);
        newsBtn.setDisable(true);
        newsBtn.setOpacity(0.5);

        storeBtn = new Button("Boutique");
        storeBtn.getStyleClass().add("sidemenu-nav-btn");
        final var storeIcon = new MaterialDesignIconView<>(MaterialDesignIcon.S.STORE);
        storeIcon.getStyleClass().add("sidemenu-nav-btn-icon");
        storeIcon.setTranslateY(Constants.NAVBUTTON_OFFSET_Y);
        storeBtn.setGraphic(storeIcon);
        setCanTakeAllSize(storeBtn);
        setTop(storeBtn);
        storeBtn.setTranslateY(168d);
        storeBtn.setDisable(true);
        storeBtn.setOpacity(0.5);

        settingsBtn = new Button("Paramètres");
        settingsBtn.getStyleClass().add("sidemenu-nav-btn");
        final var settingsIcon = new MaterialDesignIconView<>(MaterialDesignIcon.C.COG);
        settingsIcon.getStyleClass().add("sidemenu-nav-btn-icon");
        settingsIcon.setTranslateY(Constants.NAVBUTTON_OFFSET_Y);
        settingsBtn.setGraphic(settingsIcon);
        setCanTakeAllSize(settingsBtn);
        setTop(settingsBtn);
        settingsBtn.setTranslateY(212d);
        settingsBtn.setOnMouseClicked(e -> setPage(new Settings(), settingsBtn));

        sidemenu.getChildren().addAll(homeBtn, newsBtn, storeBtn, settingsBtn);

        if (Launcher.getInstance().getAuthInfos() != null) {
            // Pseudo + avatar
            GridPane userPane = new GridPane();
            setCanTakeAllWidth(userPane);
            userPane.setMaxHeight(80);
            userPane.setMinWidth(80);
            userPane.getStyleClass().add("user-pane");
            setBottom(userPane);

            String avatarUrl = "https://minotar.net/avatar/" + (
                    saver.get("offline-username") != null ?
                            "MHF_Steve.png" :
                            Launcher.getInstance().getAuthInfos().getUuid() + ".png"
            );
            ImageView avatarView = new ImageView();
            Image avatarImg = new Image(avatarUrl);
            avatarView.setImage(avatarImg);
            avatarView.setPreserveRatio(true);
            avatarView.setFitHeight(44d);
            setCenterV(avatarView);
            setCanTakeAllSize(avatarView);
            setLeft(avatarView);
            avatarView.setTranslateX(15d);
            userPane.getChildren().add(avatarView);

            Label usernameLabel = new Label(Launcher.getInstance().getAuthInfos().getUsername());
            usernameLabel.setFont(Font.font("Poppins", FontWeight.BOLD, FontPosture.REGULAR, 18f));
            setCanTakeAllSize(usernameLabel);
            setCenterV(usernameLabel);
            setLeft(usernameLabel);
            usernameLabel.getStyleClass().add("username-label");
            usernameLabel.setTranslateX(70d);
            setCanTakeAllWidth(usernameLabel);
            userPane.getChildren().add(usernameLabel);

            Button logoutBtn = new Button();
            final var logoutIcon = new MaterialDesignIconView<>(MaterialDesignIcon.L.LOGOUT);
            logoutIcon.getStyleClass().add("logout-icon");
            setCanTakeAllSize(logoutBtn);
            setCenterV(logoutBtn);
            setRight(logoutBtn);
            logoutBtn.getStyleClass().add("logout-btn");
            logoutBtn.setGraphic(logoutIcon);
            logoutBtn.setOnMouseClicked(e -> {
                if (currentPage instanceof Home && ((Home) currentPage).isDownloadingOrPlaying()) {
                    return;
                }
                saver.remove("accessToken");
                saver.remove("clientToken");
                saver.remove("offline-username");
                saver.remove("msAccessToken");
                saver.remove("msRefreshToken");
                saver.save();
                Launcher.getInstance().setAuthInfos(null);
                this.panelManager.showPanel(new Login());
            });
            userPane.getChildren().add(logoutBtn);

            sidemenu.getChildren().add(userPane);
        }
    }

    @Override
    public void onShow() {
        super.onShow();
        // Animate sidebar appearance
        new FadeIn(sidemenu).setSpeed(0.8).play();
        setPage(new Home(), homeBtn);
    }

    public void setPage(ContentPanel panel, Node navButton) {
        if (currentPage instanceof Home && ((Home) currentPage).isDownloadingOrPlaying()) {
            return;
        }
        if (activeLink == navButton) {
            return;
        }
        if (activeLink != null)
            activeLink.getStyleClass().remove("active");
        activeLink = navButton;
        activeLink.getStyleClass().add("active");

        this.navContent.getChildren().clear();
        if (panel != null) {
            this.navContent.getChildren().add(panel.getLayout());
            currentPage = panel;
            if (panel.getStylesheetPath() != null) {
                this.panelManager.getStage().getScene().getStylesheets().clear();
                this.panelManager.getStage().getScene().getStylesheets().addAll(
                        "css/main.css",
                        this.getStylesheetPath(),
                        panel.getStylesheetPath()
                );
                PanelManager.setBackground(this.panelManager.getStage().getScene(), panel);
            }
            panel.init(this.panelManager);
            panel.onShow();
        }
    }
}
