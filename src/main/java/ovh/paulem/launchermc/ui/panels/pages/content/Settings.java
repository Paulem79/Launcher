package ovh.paulem.launchermc.ui.panels.pages.content;

import animatefx.animation.FadeInUp;
import com.sun.management.OperatingSystemMXBean;
import fr.flowarg.materialdesignfontfx.MaterialDesignIcon;
import fr.flowarg.materialdesignfontfx.MaterialDesignIconView;
import ovh.paulem.launchermc.ui.panels.PanelManager;
import ovh.paulem.launchermc.utils.Constants;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.util.Duration;

import java.lang.management.ManagementFactory;

public class Settings extends ContentPanel {
    @Override
    public String getName() {
        return "settings";
    }

    @Override
    public String getStylesheetPath() {
        return "css/content/settings.css";
    }

    @Override
    public void init(PanelManager panelManager) {
        super.init(panelManager);

        // Background
        this.layout.getStyleClass().add("settings-layout");
        this.layout.setPadding(new Insets(40));
        setCanTakeAllSize(this.layout);

        // Content
        contentPane.getStyleClass().add("content-pane");
        setCanTakeAllSize(contentPane);
        this.layout.getChildren().add(contentPane);

        // Titre
        Label title = new Label("Paramètres");
        title.setFont(Font.font("Poppins", FontWeight.BOLD, FontPosture.REGULAR, 24f));
        title.getStyleClass().add("settings-title");
        setLeft(title);
        setCanTakeAllSize(title);
        setTop(title);
        title.setTextAlignment(TextAlignment.LEFT);
        title.setTranslateY(40d);
        title.setTranslateX(25d);
        contentPane.getChildren().add(title);

        // RAM
        Label ramLabel = new Label("Mémoire max");
        ramLabel.getStyleClass().add("settings-labels");
        setLeft(ramLabel);
        setCanTakeAllSize(ramLabel);
        setTop(ramLabel);
        ramLabel.setTextAlignment(TextAlignment.LEFT);
        ramLabel.setTranslateX(25d);
        ramLabel.setTranslateY(100d);
        contentPane.getChildren().add(ramLabel);

        // RAM Chooser
        ComboBox<String> comboBox = new ComboBox<>();
        comboBox.getStyleClass().add("ram-selector");

        long totalMemorySize = ((OperatingSystemMXBean) ManagementFactory
                .getOperatingSystemMXBean()).getTotalMemorySize();
        for (int i = 512; i <= Math.ceil(totalMemorySize / Math.pow(1024, 2)); i += 512) {
            comboBox.getItems().add(i / 1024.0 + " GB");
        }

        int defaultRamAmount = Constants.DEFAULT_MAX_RAM.get();
        String maxRam = saver.get(Constants.CONFIG_MAXRAM);
        try {
            if (maxRam != null) {
                defaultRamAmount = Integer.parseInt(maxRam);
            } else {
                throw new NumberFormatException("maxRam doesn't exist !");
            }
        } catch (NumberFormatException error) {
            saver.set(Constants.CONFIG_MAXRAM, String.valueOf(defaultRamAmount));
            saver.save();
        }

        if (comboBox.getItems().contains(defaultRamAmount / 1024.0 + " GB")) {
            comboBox.setValue(defaultRamAmount / 1024.0 + " GB");
        } else {
            comboBox.setValue("1.0 GB");
        }

        setLeft(comboBox);
        setCanTakeAllSize(comboBox);
        setTop(comboBox);
        comboBox.setTranslateX(35d);
        comboBox.setTranslateY(130d);
        contentPane.getChildren().add(comboBox);

        /*
         * Save Button
         */
        Button saveBtn = new Button("Enregistrer");
        saveBtn.getStyleClass().add("save-btn");
        final var iconView = new MaterialDesignIconView<>(MaterialDesignIcon.F.FLOPPY);
        iconView.getStyleClass().add("save-icon");
        final var iconCheck = new MaterialDesignIconView<>(MaterialDesignIcon.C.CHECK);
        iconCheck.getStyleClass().add("save-icon");
        saveBtn.setGraphic(iconView);
        setCanTakeAllSize(saveBtn);
        setBottom(saveBtn);
        setCenterH(saveBtn);
        saveBtn.setTranslateY(-30d);

        saveBtn.setOnMouseClicked(e -> {
            double comboBoxRamValue = Double.parseDouble(comboBox.getValue().replace(" GB", ""));
            comboBoxRamValue *= 1024;
            saver.set(Constants.CONFIG_MAXRAM, String.valueOf((int) comboBoxRamValue));

            saveBtn.setGraphic(iconCheck);
            saveBtn.setText("Enregistré !");

            Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(4), event -> {
                saveBtn.setGraphic(iconView);
                saveBtn.setText("Enregistrer");
            }));
            timeline.play();
        });
        contentPane.getChildren().add(saveBtn);
    }

    @Override
    public void onShow() {
        super.onShow();
        // Animate content appearance (slide up instead of fade to avoid flickering on dark bg)
        new FadeInUp(contentPane).setSpeed(1.2).play();
    }
}
