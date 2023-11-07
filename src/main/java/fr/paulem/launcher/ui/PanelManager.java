package fr.paulem.launcher.ui;

import fr.paulem.launcher.Launcher;
import fr.paulem.launcher.ui.panel.IPanel;
import fr.paulem.launcher.ui.panels.partials.TopBar;
import com.goxr3plus.fxborderlessscene.borderless.BorderlessScene;
import fr.flowarg.flowcompat.Platform;
import javafx.geometry.VPos;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.RowConstraints;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class PanelManager {
    private final Launcher launcher;
    private final Stage stage;
    private final GridPane contentPane = new GridPane();

    public PanelManager(Launcher launcher, Stage stage) {
        this.launcher = launcher;
        this.stage = stage;
    }

    public void init() {
        this.stage.setTitle("Launcher");
        this.stage.setFullScreen(true);
        this.stage.centerOnScreen();
        this.stage.getIcons().add(new Image("images/icon.png"));

        GridPane layout = new GridPane();

        if (Platform.isOnLinux()) {
            Scene scene = new Scene(layout);
            this.stage.setScene(scene);
        } else {
            this.stage.initStyle(StageStyle.UNDECORATED);

            TopBar topBar = new TopBar();
            BorderlessScene scene = new BorderlessScene(this.stage, StageStyle.UNDECORATED, layout);
            scene.setResizable(true);
            scene.setMoveControl(topBar.getLayout());
            scene.removeDefaultCSS();

            this.stage.setScene(scene);

            RowConstraints topPaneContraints = new RowConstraints();
            topPaneContraints.setValignment(VPos.TOP);
            topPaneContraints.setMinHeight(25);
            topPaneContraints.setMaxHeight(25);
            layout.getRowConstraints().addAll(topPaneContraints, new RowConstraints());
            layout.add(topBar.getLayout(), 0, 0);
            topBar.init(this);
        }

        layout.add(this.contentPane, 0, 1);
        GridPane.setVgrow(this.contentPane, Priority.ALWAYS);
        GridPane.setHgrow(this.contentPane, Priority.ALWAYS);

        this.stage.show();
    }

    public void showPanel(IPanel panel) {
        this.contentPane.getChildren().clear();
        this.contentPane.getChildren().add(panel.getLayout());
        if (panel.getStylesheetPath() != null) {
            this.stage.getScene().getStylesheets().clear();
            this.stage.getScene().getStylesheets().add(panel.getStylesheetPath());
        }
        panel.init(this);
        panel.onShow();
    }

    public Stage getStage() {
        return stage;
    }

    public Launcher getLauncher() {
        return launcher;
    }
}
