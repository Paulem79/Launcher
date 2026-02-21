package ovh.paulem.launchermc.ui.panels;

import animatefx.animation.FadeIn;
import fr.flowarg.materialdesignfontfx.MaterialDesignIcon;
import fr.flowarg.materialdesignfontfx.MaterialDesignIconView;
import ovh.paulem.launchermc.Launcher;
import ovh.paulem.launchermc.ui.panels.pages.content.Settings;
import ovh.paulem.launchermc.utils.Background;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.ThreadLocalRandom;

public class PanelManager {
    private final Launcher launcher;
    private final Stage stage;
    private final GridPane contentPane = new GridPane();

    private static final String MAIN_CSS = "css/main.css";
    private static final double RESIZE_MARGIN = 8;

    // For window dragging
    private double xOffset = 0;
    private double yOffset = 0;

    // For window resizing
    private boolean isResizing = false;
    private Cursor resizeCursor = Cursor.DEFAULT;

    public PanelManager(Launcher launcher, Stage stage) {
        this.launcher = launcher;
        this.stage = stage;
    }

    public void init() {
        // Load custom fonts early
        Font.loadFont(getClass().getResourceAsStream("/fonts/Poppins-Regular.ttf"), 14);
        Font.loadFont(getClass().getResourceAsStream("/fonts/Poppins-Bold.ttf"), 14);
        Font.loadFont(getClass().getResourceAsStream("/fonts/Poppins-SemiBold.ttf"), 14);
        Font.loadFont(getClass().getResourceAsStream("/fonts/Poppins-Medium.ttf"), 14);

        // Transparent window for rounded corners on Linux
        this.stage.initStyle(StageStyle.TRANSPARENT);

        this.stage.setTitle("Launcher MC");
        this.stage.setFullScreen(false);
        this.stage.setMinWidth(854);
        this.stage.setMinHeight(480);

        // Size the window to half the screen and center it
        javafx.geometry.Rectangle2D screenBounds = javafx.stage.Screen.getPrimary().getVisualBounds();
        this.stage.setWidth(screenBounds.getWidth() / 2);
        this.stage.setHeight(screenBounds.getHeight() / 2);
        this.stage.setX((screenBounds.getWidth() - this.stage.getWidth()) / 2);
        this.stage.setY((screenBounds.getHeight() - this.stage.getHeight()) / 2);

        this.stage.getIcons().add(new Image("images/icon.png"));

        // Root layout with rounded corners
        final double CORNER_RADIUS = 16;
        VBox rootLayout = new VBox();
        rootLayout.setStyle("-fx-background-color: #121212; -fx-background-radius: " + CORNER_RADIUS + ";");

        // Clip the root layout to a rounded rectangle so corners are truly transparent
        javafx.scene.shape.Rectangle rootClip = new javafx.scene.shape.Rectangle();
        rootClip.setArcWidth(CORNER_RADIUS * 2);
        rootClip.setArcHeight(CORNER_RADIUS * 2);
        rootClip.widthProperty().bind(rootLayout.widthProperty());
        rootClip.heightProperty().bind(rootLayout.heightProperty());
        rootLayout.setClip(rootClip);

        // ── Custom Title Bar ──
        HBox titleBar = createTitleBar();

        // ── Content area ──
        VBox.setVgrow(this.contentPane, Priority.ALWAYS);

        // Clip content so panels don't overflow onto the title bar
        javafx.scene.shape.Rectangle contentClip = new javafx.scene.shape.Rectangle();
        contentClip.widthProperty().bind(this.contentPane.widthProperty());
        contentClip.heightProperty().bind(this.contentPane.heightProperty());
        this.contentPane.setClip(contentClip);

        // Ensure title bar stays above content (not overlapped)
        titleBar.setViewOrder(-1);

        rootLayout.getChildren().addAll(titleBar, this.contentPane);

        Scene scene = new Scene(rootLayout);
        scene.setFill(Color.TRANSPARENT);

        // Always load main.css
        scene.getStylesheets().add(MAIN_CSS);

        this.stage.setScene(scene);
        this.stage.show();

        // Custom resize handling for transparent/undecorated window
        setupResizeHandlers(scene);

        // FadeIn animation on startup
        new FadeIn(rootLayout).setSpeed(0.8).play();
    }

    private void setupResizeHandlers(Scene scene) {
        // Update cursor based on mouse position near edges
        scene.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_MOVED, event -> {
            resizeCursor = getResizeCursor(event.getX(), event.getY(), scene.getWidth(), scene.getHeight());
            scene.setCursor(resizeCursor);
        });

        // On press, lock the resize direction
        scene.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_PRESSED, event -> {
            resizeCursor = getResizeCursor(event.getX(), event.getY(), scene.getWidth(), scene.getHeight());
            isResizing = resizeCursor != Cursor.DEFAULT;
        });

        // On drag, resize if we locked a direction
        scene.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_DRAGGED, event -> {
            if (!isResizing) return;

            event.consume(); // prevent title bar drag

            double mouseX = event.getScreenX();
            double mouseY = event.getScreenY();

            double stageX = stage.getX();
            double stageY = stage.getY();
            double stageW = stage.getWidth();
            double stageH = stage.getHeight();

            double minW = stage.getMinWidth();
            double minH = stage.getMinHeight();

            double newX = stageX;
            double newY = stageY;
            double newW = stageW;
            double newH = stageH;

            // East
            if (resizeCursor == Cursor.E_RESIZE || resizeCursor == Cursor.NE_RESIZE || resizeCursor == Cursor.SE_RESIZE) {
                newW = mouseX - stageX;
            }
            // South
            if (resizeCursor == Cursor.S_RESIZE || resizeCursor == Cursor.SE_RESIZE || resizeCursor == Cursor.SW_RESIZE) {
                newH = mouseY - stageY;
            }
            // West
            if (resizeCursor == Cursor.W_RESIZE || resizeCursor == Cursor.NW_RESIZE || resizeCursor == Cursor.SW_RESIZE) {
                newW = stageW + (stageX - mouseX);
                newX = mouseX;
            }
            // North
            if (resizeCursor == Cursor.N_RESIZE || resizeCursor == Cursor.NW_RESIZE || resizeCursor == Cursor.NE_RESIZE) {
                newH = stageH + (stageY - mouseY);
                newY = mouseY;
            }

            // Clamp to minimum — if clamped, don't move the origin
            if (newW < minW) {
                newW = minW;
                newX = stageX;
            }
            if (newH < minH) {
                newH = minH;
                newY = stageY;
            }

            // Only apply if dimensions actually changed and are safe for GTK
            boolean widthChanged = Math.abs(newW - stageW) > 1 || Math.abs(newX - stageX) > 1;
            boolean heightChanged = Math.abs(newH - stageH) > 1 || Math.abs(newY - stageY) > 1;

            if ((widthChanged || heightChanged) && newW >= minW && newH >= minH) {
                stage.setX(newX);
                stage.setY(newY);
                stage.setWidth(newW);
                stage.setHeight(newH);
            }
        });

        // On release, stop resizing
        scene.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_RELEASED, event -> {
            isResizing = false;
        });
    }

    private Cursor getResizeCursor(double x, double y, double w, double h) {
        boolean left = x < RESIZE_MARGIN;
        boolean right = x > w - RESIZE_MARGIN;
        boolean top = y < RESIZE_MARGIN;
        boolean bottom = y > h - RESIZE_MARGIN;

        if (left && top) return Cursor.NW_RESIZE;
        if (right && top) return Cursor.NE_RESIZE;
        if (left && bottom) return Cursor.SW_RESIZE;
        if (right && bottom) return Cursor.SE_RESIZE;
        if (left) return Cursor.W_RESIZE;
        if (right) return Cursor.E_RESIZE;
        if (top) return Cursor.N_RESIZE;
        if (bottom) return Cursor.S_RESIZE;
        return Cursor.DEFAULT;
    }

    private HBox createTitleBar() {
        HBox titleBar = new HBox();
        titleBar.getStyleClass().add("title-bar");
        titleBar.setAlignment(Pos.CENTER_LEFT);
        titleBar.setMinHeight(40);
        titleBar.setMaxHeight(40);
        titleBar.setPrefHeight(40);

        // Title label
        Label titleLabel = new Label("LAUNCHER MC");
        titleLabel.getStyleClass().add("title-label");
        titleLabel.setStyle("-fx-text-fill: #B0B0B0; -fx-font-size: 13px; -fx-font-weight: bold; -fx-font-family: 'Poppins';");

        // Spacer to push buttons to the right
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Minimize button
        Button minimizeBtn = new Button();
        var minimizeIcon = new MaterialDesignIconView<>(MaterialDesignIcon.M.MINUS);
        minimizeIcon.getStyleClass().add("title-bar-icon");
        minimizeIcon.setSize("16");
        minimizeBtn.setGraphic(minimizeIcon);
        minimizeBtn.getStyleClass().add("title-bar-btn");
        minimizeBtn.setOnAction(e -> stage.setIconified(true));

        // Close button
        Button closeBtn = new Button();
        var closeIcon = new MaterialDesignIconView<>(MaterialDesignIcon.C.CLOSE);
        closeIcon.getStyleClass().add("title-bar-icon");
        closeIcon.setSize("16");
        closeBtn.setGraphic(closeIcon);
        closeBtn.getStyleClass().addAll("title-bar-btn", "title-bar-btn-close");
        closeBtn.setOnAction(e -> {
            javafx.application.Platform.exit();
            System.exit(0);
        });

        titleBar.getChildren().addAll(titleLabel, spacer, minimizeBtn, closeBtn);

        // Make the title bar draggable (only when not resizing)
        titleBar.setOnMousePressed(event -> {
            if (!isResizing) {
                xOffset = event.getSceneX();
                yOffset = event.getSceneY();
            }
        });
        titleBar.setOnMouseDragged(event -> {
            if (!isResizing) {
                stage.setX(event.getScreenX() - xOffset);
                stage.setY(event.getScreenY() - yOffset);
            }
        });

        return titleBar;
    }

    public void showPanel(Panel panel) {
        this.contentPane.getChildren().clear();
        this.contentPane.getChildren().add(panel.getLayout());
        GridPane.setVgrow(panel.getLayout(), Priority.ALWAYS);
        GridPane.setHgrow(panel.getLayout(), Priority.ALWAYS);

        // Rebuild stylesheets: always keep main.css, then add panel-specific ones
        this.stage.getScene().getStylesheets().clear();
        this.stage.getScene().getStylesheets().add(MAIN_CSS);
        if (panel.getStylesheetPath() != null) {
            this.stage.getScene().getStylesheets().add(panel.getStylesheetPath());
            setBackground(this.stage.getScene(), null);
        }
        panel.init(this);
        panel.onShow();
    }

    public static void setBackground(Scene scene, @Nullable Panel actualPanel) {
        if (actualPanel instanceof Settings) return;

        Background[] backgroundsValues = Background.values();
        int index = ThreadLocalRandom.current().nextInt(backgroundsValues.length);
        Background background = backgroundsValues[index];

        String sessionBackground = "css/backgrounds/" + background.getName() + ".css";
        scene.getStylesheets().add(sessionBackground);
    }

    public Stage getStage() {
        return stage;
    }

    public Launcher getLauncher() {
        return launcher;
    }
}
