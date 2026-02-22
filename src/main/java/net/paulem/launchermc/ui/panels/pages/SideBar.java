package net.paulem.launchermc.ui.panels.pages;

import animatefx.animation.FadeIn;
import fr.flowarg.materialdesignfontfx.MaterialDesignIcon;
import fr.flowarg.materialdesignfontfx.MaterialDesignIconView;
import javafx.scene.layout.*;
import net.paulem.launchermc.Launcher;
import net.paulem.launchermc.ui.panels.PanelManager;
import net.paulem.launchermc.ui.panels.Panel;
import net.paulem.launchermc.ui.panels.pages.content.ContentPanel;
import net.paulem.launchermc.ui.panels.pages.content.Home;
import net.paulem.launchermc.ui.panels.pages.content.Settings;
import javafx.animation.Animation;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;

public class SideBar extends Panel {
    private final VBox sidemenu = new VBox();
    private final GridPane navContent = new GridPane();

    private Node activeLink = null;
    private ContentPanel currentPage = null;

    private Button homeBtn, settingsBtn, newsBtn, storeBtn;

    private static final double SIDEBAR_COLLAPSED_WIDTH = 62;
    private static final double SIDEBAR_EXPANDED_WIDTH = 220;
    private static final double SIDEBAR_GAP = 10;

    private Label usernameLabel;
    private Button logoutBtn;

    private Timeline expandTimeline;
    private Timeline collapseTimeline;

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

        // Root layout: StackPane so sidebar floats over content
        this.layout.getStyleClass().add("app-layout");
        setCanTakeAllSize(this.layout);

        // ── Background Image (behind everything) ──
        GridPane bgImage = new GridPane();
        setCanTakeAllSize(bgImage);
        bgImage.getStyleClass().add("bg-image");

        // ── Nav content (takes full space) ──
        navContent.getStyleClass().add("nav-content");
        setCanTakeAllSize(navContent);

        // ── Floating sidebar ──
        sidemenu.getStyleClass().add("sidemenu");
        sidemenu.setAlignment(Pos.TOP_LEFT);
        sidemenu.setPrefWidth(SIDEBAR_COLLAPSED_WIDTH);
        sidemenu.setMinWidth(SIDEBAR_COLLAPSED_WIDTH);
        sidemenu.setMaxWidth(SIDEBAR_COLLAPSED_WIDTH);
        sidemenu.setSpacing(4);
        sidemenu.setPadding(new Insets(12, 6, 12, 6));
        sidemenu.setPickOnBounds(false);

        // Clip the sidebar so content (user pane) doesn't overflow
        Rectangle sideClip = new Rectangle();
        sideClip.setArcWidth(36);
        sideClip.setArcHeight(36);
        sideClip.widthProperty().bind(sidemenu.widthProperty());
        sideClip.heightProperty().bind(sidemenu.heightProperty());
        sidemenu.setClip(sideClip);

        // Use a StackPane as the content of the layout
        StackPane contentStack = new StackPane();
        setCanTakeAllSize(contentStack);
        contentStack.getChildren().addAll(bgImage, navContent, sidemenu);
        StackPane.setAlignment(sidemenu, Pos.CENTER_LEFT);
        StackPane.setMargin(sidemenu, new Insets(SIDEBAR_GAP, 0, SIDEBAR_GAP, SIDEBAR_GAP));

        this.layout.add(contentStack, 0, 0);

        // ── Build sidebar content ──
        buildSidebarButtons();
        buildUserPane();

        // ── Hover animations ──
        setupHoverAnimations();
    }

    private void buildSidebarButtons() {
        // Navigation buttons
        homeBtn = createNavButton("Accueil", new MaterialDesignIconView<>(MaterialDesignIcon.H.HOME));
        homeBtn.setOnMouseClicked(e -> setPage(new Home(), homeBtn));

        newsBtn = createNavButton("Actualités", new MaterialDesignIconView<>(MaterialDesignIcon.N.NEWSPAPER));
        newsBtn.setDisable(true);
        newsBtn.setOpacity(0.5);

        storeBtn = createNavButton("Boutique", new MaterialDesignIconView<>(MaterialDesignIcon.S.STORE));
        storeBtn.setDisable(true);
        storeBtn.setOpacity(0.5);

        settingsBtn = createNavButton("Paramètres", new MaterialDesignIconView<>(MaterialDesignIcon.C.COG));
        settingsBtn.setOnMouseClicked(e -> setPage(new Settings(), settingsBtn));

        // Spacer to push user pane to bottom
        VBox spacer = new VBox();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        sidemenu.getChildren().addAll(homeBtn, newsBtn, storeBtn, settingsBtn, spacer);
    }

    private Button createNavButton(String text, MaterialDesignIconView<?> iconView) {
        Button btn = new Button(text);
        btn.getStyleClass().add("sidemenu-nav-btn");
        iconView.getStyleClass().add("sidemenu-nav-btn-icon");
        iconView.setSize("20");
        btn.setGraphic(iconView);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setMinHeight(44);
        btn.setMaxHeight(44);
        btn.setEllipsisString("");
        return btn;
    }

    private void buildUserPane() {
        if (Launcher.getInstance().getAuthInfos() == null) return;

        // Separator spacer
        VBox userBox = new VBox();
        userBox.getStyleClass().add("user-pane");
        userBox.setAlignment(Pos.CENTER_LEFT);
        userBox.setSpacing(0);
        userBox.setMinHeight(60);
        userBox.setMaxHeight(60);

        // Avatar + username row
        HBox userRow = new HBox();
        userRow.setAlignment(Pos.CENTER_LEFT);
        userRow.setSpacing(10);

        String avatarUrl = "https://minotar.net/avatar/" + (
                saver.get("offline-username") != null ?
                        "MHF_Steve.png" :
                        Launcher.getInstance().getAuthInfos().getUuid() + ".png"
        );
        ImageView avatarView = new ImageView(new Image(avatarUrl));
        avatarView.setPreserveRatio(true);
        avatarView.setFitHeight(36d);
        avatarView.setFitWidth(36d);
        avatarView.setSmooth(true);

        usernameLabel = new Label(Launcher.getInstance().getAuthInfos().getUsername());
        usernameLabel.setFont(Font.font("Poppins", FontWeight.BOLD, FontPosture.REGULAR, 14f));
        usernameLabel.getStyleClass().add("username-label");
        usernameLabel.setOpacity(0);

        userRow.getChildren().addAll(avatarView, usernameLabel);

        // Logout button
        logoutBtn = new Button();
        final var logoutIcon = new MaterialDesignIconView<>(MaterialDesignIcon.L.LOGOUT);
        logoutIcon.getStyleClass().add("logout-icon");
        logoutIcon.setSize("18");
        logoutBtn.getStyleClass().add("logout-btn");
        logoutBtn.setGraphic(logoutIcon);
        logoutBtn.setOpacity(0);
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

        // Layout: avatar row + logout side by side
        HBox userContainer = new HBox();
        userContainer.setAlignment(Pos.CENTER_LEFT);
        userContainer.setSpacing(6);
        HBox logoutSpacer = new HBox();
        HBox.setHgrow(logoutSpacer, Priority.ALWAYS);
        userContainer.getChildren().addAll(userRow, logoutSpacer, logoutBtn);
        userContainer.setPadding(new Insets(0, 8, 0, 7));

        userBox.getChildren().add(userContainer);

        // Add bottom margin so user pane doesn't stick to the sidebar edge
        VBox.setMargin(userBox, new Insets(0, 0, 4, 0));

        sidemenu.getChildren().add(userBox);
    }

    private void setupHoverAnimations() {
        // Snappy interpolator
        Interpolator snappy = Interpolator.SPLINE(0.22, 0.95, 0.35, 1.0);
        Interpolator smooth = Interpolator.SPLINE(0.25, 0.1, 0.25, 1.0);

        double overshoot = SIDEBAR_EXPANDED_WIDTH + 12;

        expandTimeline = new Timeline();
        collapseTimeline = new Timeline();

        // Expand: overshoot then settle (bouncy)
        expandTimeline.getKeyFrames().addAll(
                new KeyFrame(Duration.millis(220),
                        new KeyValue(sidemenu.prefWidthProperty(), overshoot, snappy),
                        new KeyValue(sidemenu.minWidthProperty(), overshoot, snappy),
                        new KeyValue(sidemenu.maxWidthProperty(), overshoot, snappy)),
                new KeyFrame(Duration.millis(350),
                        new KeyValue(sidemenu.prefWidthProperty(), SIDEBAR_EXPANDED_WIDTH, smooth),
                        new KeyValue(sidemenu.minWidthProperty(), SIDEBAR_EXPANDED_WIDTH, smooth),
                        new KeyValue(sidemenu.maxWidthProperty(), SIDEBAR_EXPANDED_WIDTH, smooth))
        );

        // Collapse: smooth ease out
        collapseTimeline.getKeyFrames().add(
                new KeyFrame(Duration.millis(300),
                        new KeyValue(sidemenu.prefWidthProperty(), SIDEBAR_COLLAPSED_WIDTH, smooth),
                        new KeyValue(sidemenu.minWidthProperty(), SIDEBAR_COLLAPSED_WIDTH, smooth),
                        new KeyValue(sidemenu.maxWidthProperty(), SIDEBAR_COLLAPSED_WIDTH, smooth))
        );

        // Username & logout fade
        if (usernameLabel != null) {
            expandTimeline.getKeyFrames().add(new KeyFrame(Duration.millis(350),
                    new KeyValue(usernameLabel.opacityProperty(), 1, snappy)));
            collapseTimeline.getKeyFrames().add(new KeyFrame(Duration.millis(200),
                    new KeyValue(usernameLabel.opacityProperty(), 0, smooth)));
        }
        if (logoutBtn != null) {
            expandTimeline.getKeyFrames().add(new KeyFrame(Duration.millis(350),
                    new KeyValue(logoutBtn.opacityProperty(), 1, snappy)));
            collapseTimeline.getKeyFrames().add(new KeyFrame(Duration.millis(200),
                    new KeyValue(logoutBtn.opacityProperty(), 0, smooth)));
        }

        sidemenu.setOnMouseEntered(e -> {
            if (collapseTimeline.getStatus() == Animation.Status.RUNNING) collapseTimeline.stop();
            expandTimeline.playFromStart();
        });
        sidemenu.setOnMouseExited(e -> {
            if (expandTimeline.getStatus() == Animation.Status.RUNNING) expandTimeline.stop();
            collapseTimeline.playFromStart();
        });
    }

    @Override
    public void onShow() {
        super.onShow();
        // Animate sidebar appearance
        new FadeIn(sidemenu).setSpeed(0.8).play();
        setPage(new Home(), homeBtn);
    }

    public void setPage(ContentPanel panel, Node navButton) {
        if (currentPage instanceof Home home && home.isDownloadingOrPlaying()) {
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
