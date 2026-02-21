package ovh.paulem.launchermc.ui.panels.pages.content;

import animatefx.animation.FadeIn;
import animatefx.animation.Pulse;
import fr.flowarg.materialdesignfontfx.MaterialDesignIcon;
import fr.flowarg.materialdesignfontfx.MaterialDesignIconView;
import ovh.paulem.launchermc.Launcher;
import ovh.paulem.launchermc.game.Launch;
import ovh.paulem.launchermc.ui.components.GradientButton;
import ovh.paulem.launchermc.ui.panels.PanelManager;
import ovh.paulem.launchermc.utils.Constants;
import javafx.geometry.Insets;
import javafx.geometry.VPos;
import javafx.scene.control.*;
import javafx.scene.layout.RowConstraints;

public class Home extends ContentPanel {
    private Launch launch;

    private boolean isDownloadingOrPlaying = false;

    private final ProgressBar progressBar = new ProgressBar();
    private final Label stepLabel = new Label();
    private final Label fileLabel = new Label();

    @Override
    public String getName() {
        return "home";
    }

    @Override
    public String getStylesheetPath() {
        return "css/content/home.css";
    }

    @Override
    public void init(PanelManager panelManager) {
        super.init(panelManager);

        launch = new Launch(this, saver, logger, contentPane, progressBar, stepLabel, fileLabel);

        Launcher.getInstance().getDiscordRPC().editPresence(Constants.RPC_LAUNCHER);

        RowConstraints rowConstraints = new RowConstraints();
        rowConstraints.setValignment(VPos.CENTER);
        rowConstraints.setMinHeight(75);
        rowConstraints.setMaxHeight(75);
        this.layout.getRowConstraints().addAll(rowConstraints, new RowConstraints());
        contentPane.getStyleClass().add("box-pane");
        setCanTakeAllSize(contentPane);
        contentPane.setPadding(new Insets(20, 20, 20, 85));
        this.layout.add(contentPane, 0, 0);
        this.layout.getStyleClass().add("home-layout");

        progressBar.getStyleClass().add("download-progress");
        stepLabel.getStyleClass().add("download-status");
        fileLabel.getStyleClass().add("download-status");

        progressBar.setTranslateY(-15);
        setCenterH(progressBar);
        setCanTakeAllWidth(progressBar);

        stepLabel.setTranslateY(5);
        setCenterH(stepLabel);
        setCanTakeAllSize(stepLabel);

        fileLabel.setTranslateY(20);
        setCenterH(fileLabel);
        setCanTakeAllSize(fileLabel);

        this.showPlayButton();
    }

    public void showPlayButton() {
        contentPane.getChildren().clear();

        GradientButton playBtn = new GradientButton("JOUER", 25, 1.08)
                .withLargeShadow();
        final var playIcon = new MaterialDesignIconView<>(MaterialDesignIcon.G.GAMEPAD);
        playIcon.getStyleClass().add("play-icon");
        playIcon.setSize("28");
        playBtn.setGraphic(playIcon);
        setCanTakeAllSize(playBtn);
        setCenterH(playBtn);
        setCenterV(playBtn);
        playBtn.getStyleClass().add("play-btn");
        playBtn.setOnMouseClicked(e -> launch.play());

        contentPane.getChildren().add(playBtn);

        // Pulse animation on button appearance, then reset scale
        Pulse pulse = new Pulse(playBtn);
        pulse.setSpeed(0.8);
        pulse.setOnFinished(e -> {
            playBtn.setScaleX(1.0);
            playBtn.setScaleY(1.0);
            playBtn.setCache(false);
        });
        pulse.play();
    }

    @Override
    public void onShow() {
        super.onShow();
        // Animate content appearance
        new FadeIn(contentPane).setSpeed(0.8).play();
    }

    public boolean isDownloadingOrPlaying() {
        return isDownloadingOrPlaying;
    }

    public void setDownloadingOrPlaying(boolean downloadingOrPlaying) {
        isDownloadingOrPlaying = downloadingOrPlaying;
    }
}