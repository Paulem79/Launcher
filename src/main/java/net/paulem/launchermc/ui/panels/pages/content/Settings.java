package net.paulem.launchermc.ui.panels.pages.content;

import animatefx.animation.FadeIn;
import com.sun.management.OperatingSystemMXBean;
import fr.flowarg.materialdesignfontfx.MaterialDesignIcon;
import fr.flowarg.materialdesignfontfx.MaterialDesignIconView;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import net.paulem.launchermc.Launcher;
import net.paulem.launchermc.game.minecraft.MinecraftInfos;
import net.paulem.launchermc.game.minecraft.ModsSource;
import net.paulem.launchermc.config.SaveSystem;
import net.paulem.launchermc.ui.components.GradientButton;
import net.paulem.launchermc.ui.panels.PanelManager;
import net.paulem.launchermc.utils.Constants;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.util.Duration;

import java.io.File;
import java.io.IOException;
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
        this.layout.setPadding(new Insets(40, 40, 40, 85));
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
                throw new NumberFormatException();
            }
        } catch (NumberFormatException ex) {
            saver.set(Constants.CONFIG_MAXRAM, String.valueOf(defaultRamAmount));
            saver.save();
        }

        double toFormatRam = defaultRamAmount / 1024.0;
        // Round to nearest GB
        comboBox.setValue(Math.round(toFormatRam * 2) / 2.0 + " GB");

        setLeft(comboBox);
        setCanTakeAllSize(comboBox);
        setTop(comboBox);
        comboBox.setTranslateX(35d);
        comboBox.setTranslateY(130d);
        contentPane.getChildren().add(comboBox);
        
        SaveSystem saveSystem = new SaveSystem();
        
        saveSystem.add(() -> {
            double comboBoxRamValue = Double.parseDouble(comboBox.getValue().replace(" GB", ""));
            comboBoxRamValue *= 1024;
            saver.set(Constants.CONFIG_MAXRAM, String.valueOf((int) comboBoxRamValue));
        });

        double emplacementY = 150d;

        // ZGC label + checkbox
        Label zgcLabel = new Label("ZGC");
        zgcLabel.getStyleClass().add("settings-labels");
        setLeft(zgcLabel);
        setCanTakeAllSize(zgcLabel);
        setTop(zgcLabel);
        zgcLabel.setTextAlignment(TextAlignment.LEFT);
        zgcLabel.setTranslateX(25d);
        zgcLabel.setTranslateY(emplacementY += 20d);
        contentPane.getChildren().add(zgcLabel);

        // ZGC checkbox
        CheckBox zgcCheckBox = new CheckBox();
        zgcCheckBox.getStyleClass().add("settings-checkbox");
        setLeft(zgcCheckBox);
        setCanTakeAllSize(zgcCheckBox);
        setTop(zgcCheckBox);
        zgcCheckBox.setTranslateX(35d);
        zgcCheckBox.setTranslateY(emplacementY += 20d);

        boolean zgc = saver.get(Constants.CONFIG_ENABLE_ZGC, "false").equals("true");
        zgcCheckBox.setSelected(zgc);

        contentPane.getChildren().add(zgcCheckBox);

        saveSystem.add(() ->
                saver.set(Constants.CONFIG_ENABLE_ZGC, String.valueOf(zgcCheckBox.isSelected()))
        );
        
        // check system is linux based
        if (System.getProperty("os.name").toLowerCase().contains("linux")) {
            // Mangohud label + checkbox
            Label mangohudLabel = new Label("MangoHud");
            mangohudLabel.getStyleClass().add("settings-labels");
            setLeft(mangohudLabel);
            setCanTakeAllSize(mangohudLabel);
            setTop(mangohudLabel);
            mangohudLabel.setTextAlignment(TextAlignment.LEFT);
            mangohudLabel.setTranslateX(25d);
            mangohudLabel.setTranslateY(emplacementY += 20d);
            contentPane.getChildren().add(mangohudLabel);
            
            // MangoHud checkbox
            CheckBox mangohudCheckBox = new CheckBox();
            mangohudCheckBox.getStyleClass().add("settings-checkbox");
            setLeft(mangohudCheckBox);
            setCanTakeAllSize(mangohudCheckBox);
            setTop(mangohudCheckBox);
            mangohudCheckBox.setTranslateX(35d);
            mangohudCheckBox.setTranslateY(emplacementY += 20d);
            
            boolean mangoHud = saver.get(Constants.CONFIG_ENABLE_MANGOHUD, "false").equals("true");
            mangohudCheckBox.setSelected(mangoHud);
            
            contentPane.getChildren().add(mangohudCheckBox);
            
            saveSystem.add(() ->
                    saver.set(Constants.CONFIG_ENABLE_MANGOHUD, String.valueOf(mangohudCheckBox.isSelected()))
            );
            
            // Zink label + checkbox
            Label zinkLabel = new Label("Zink");
            zinkLabel.getStyleClass().add("settings-labels");
            setLeft(zinkLabel);
            setCanTakeAllSize(zinkLabel);
            setTop(zinkLabel);
            zinkLabel.setTextAlignment(TextAlignment.LEFT);
            zinkLabel.setTranslateX(25d);
            zinkLabel.setTranslateY(emplacementY += 20d);
            contentPane.getChildren().add(zinkLabel);
            
            // Zink checkbox
            CheckBox zinkCheckBox = new CheckBox();
            zinkCheckBox.getStyleClass().add("settings-checkbox");
            setLeft(zinkCheckBox);
            setCanTakeAllSize(zinkCheckBox);
            setTop(zinkCheckBox);
            zinkCheckBox.setTranslateX(35d);
            zinkCheckBox.setTranslateY(emplacementY += 20d);
            
            boolean zink = saver.get(Constants.CONFIG_ENABLE_ZINK, "false").equals("true");
            zinkCheckBox.setSelected(zink);
            
            contentPane.getChildren().add(zinkCheckBox);

            saveSystem.add(() ->
                    saver.set(Constants.CONFIG_ENABLE_ZINK, String.valueOf(zinkCheckBox.isSelected()))
            );
            
            // Feral Gamemode label + checkbox
            Label gamemodeLabel = new Label("Feral Gamemode");
            gamemodeLabel.getStyleClass().add("settings-labels");
            setLeft(gamemodeLabel);
            setCanTakeAllSize(gamemodeLabel);
            setTop(gamemodeLabel);
            gamemodeLabel.setTextAlignment(TextAlignment.LEFT);
            gamemodeLabel.setTranslateX(25d);
            gamemodeLabel.setTranslateY(emplacementY += 20d);
            contentPane.getChildren().add(gamemodeLabel);
            
            // Feral Gamemode checkbox
            CheckBox gamemodeCheckBox = new CheckBox();
            gamemodeCheckBox.getStyleClass().add("settings-checkbox");
            setLeft(gamemodeCheckBox);
            setCanTakeAllSize(gamemodeCheckBox);
            setTop(gamemodeCheckBox);
            gamemodeCheckBox.setTranslateX(35d);
            gamemodeCheckBox.setTranslateY(emplacementY += 20d);
            
            boolean gamemode = saver.get(Constants.CONFIG_ENABLE_FERAL_GAMEMODE, "false").equals("true");
            gamemodeCheckBox.setSelected(gamemode);
            
            contentPane.getChildren().add(gamemodeCheckBox);
            
            saveSystem.add(() ->
                    saver.set(Constants.CONFIG_ENABLE_FERAL_GAMEMODE, String.valueOf(gamemodeCheckBox.isSelected()))
            );
            
            // DGPU label + checkbox
            Label dgpuLabel = new Label("Activer la carte graphique dédiée (DGPU)");
            dgpuLabel.getStyleClass().add("settings-labels");
            setLeft(dgpuLabel);
            setCanTakeAllSize(dgpuLabel);
            setTop(dgpuLabel);
            dgpuLabel.setTextAlignment(TextAlignment.LEFT);
            dgpuLabel.setTranslateX(25d);
            dgpuLabel.setTranslateY(emplacementY += 20d);
            contentPane.getChildren().add(dgpuLabel);

            // DGPU checkbox
            CheckBox dgpuCheckBox = new CheckBox();
            dgpuCheckBox.getStyleClass().add("settings-checkbox");
            setLeft(dgpuCheckBox);
            setCanTakeAllSize(dgpuCheckBox);
            setTop(dgpuCheckBox);
            dgpuCheckBox.setTranslateX(35d);
            dgpuCheckBox.setTranslateY(emplacementY += 20d);

            boolean dgpu = saver.get(Constants.CONFIG_ENABLE_DGPU, "false").equals("true");
            dgpuCheckBox.setSelected(dgpu);

            contentPane.getChildren().add(dgpuCheckBox);

            saveSystem.add(() ->
                    saver.set(Constants.CONFIG_ENABLE_DGPU, String.valueOf(dgpuCheckBox.isSelected()))
            );
        }

        /*
         * Mods list (imported file > custom URL > default URL)
         */
        final double modsX = 330d;

        Label modsLabel = new Label("Liste des mods (mods.json)");
        modsLabel.getStyleClass().add("settings-labels");
        setLeft(modsLabel);
        setCanTakeAllSize(modsLabel);
        setTop(modsLabel);
        modsLabel.setTextAlignment(TextAlignment.LEFT);
        modsLabel.setTranslateX(modsX);
        modsLabel.setTranslateY(100d);
        contentPane.getChildren().add(modsLabel);

        TextField modsUrlField = new TextField(ModsSource.getCustomUrl(saver));
        modsUrlField.getStyleClass().add("settings-text-field");
        modsUrlField.setPromptText(MinecraftInfos.MODS_LIST_URL);
        modsUrlField.setPrefWidth(420d);
        setLeft(modsUrlField);
        setTop(modsUrlField);
        modsUrlField.setTranslateX(modsX + 10d);
        modsUrlField.setTranslateY(130d);
        contentPane.getChildren().add(modsUrlField);

        Label modsStatus = new Label();
        modsStatus.getStyleClass().add("settings-labels");
        setLeft(modsStatus);
        setTop(modsStatus);
        modsStatus.setTranslateX(modsX + 10d);
        modsStatus.setTranslateY(230d);
        contentPane.getChildren().add(modsStatus);

        Runnable refreshModsStatus = () -> {
            boolean imported = ModsSource.hasImportedFile(saver);
            modsUrlField.setDisable(imported);
            modsStatus.setText(imported
                    ? "Fichier importé utilisé (l'URL est ignorée)"
                    : "Vide = liste par défaut");
        };
        refreshModsStatus.run();

        GradientButton importModsBtn = new GradientButton("Importer un mods.json", 14, 1.06);
        importModsBtn.getStyleClass().add("save-btn");
        setLeft(importModsBtn);
        setTop(importModsBtn);
        importModsBtn.setTranslateX(modsX + 10d);
        importModsBtn.setTranslateY(180d);
        importModsBtn.setOnAction(e -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Importer un mods.json");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON", "*.json"));
            File file = chooser.showOpenDialog(importModsBtn.getScene().getWindow());
            if (file == null) return;

            try {
                ModsSource.importFile(saver, file.toPath());
            } catch (IOException ex) {
                Launcher.getInstance().getLogger().printStackTrace(ex);
                modsStatus.setText("Import impossible : " + ex.getMessage());
                return;
            }
            refreshModsStatus.run();
        });
        contentPane.getChildren().add(importModsBtn);

        GradientButton resetModsBtn = new GradientButton("Réinitialiser", 14, 1.06);
        resetModsBtn.getStyleClass().add("save-btn");
        setLeft(resetModsBtn);
        setTop(resetModsBtn);
        resetModsBtn.setTranslateX(modsX + 260d);
        resetModsBtn.setTranslateY(180d);
        resetModsBtn.setOnAction(e -> {
            try {
                ModsSource.clearImportedFile(saver);
            } catch (IOException ex) {
                Launcher.getInstance().getLogger().printStackTrace(ex);
            }
            modsUrlField.clear();
            saver.remove(Constants.CONFIG_MODS_URL);
            saver.save();
            refreshModsStatus.run();
        });
        contentPane.getChildren().add(resetModsBtn);

        saveSystem.add(() -> {
            String url = modsUrlField.getText().trim();
            if (url.isEmpty()) saver.remove(Constants.CONFIG_MODS_URL);
            else saver.set(Constants.CONFIG_MODS_URL, url);
        });

        /*
         * Save Button
         */
        GradientButton saveBtn = new GradientButton("Enregistrer", 14, 1.06);
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
        
        saveSystem.setSaveUi(() -> {
            saveBtn.setGraphic(iconCheck);
            saveBtn.setText("Enregistré !");

            Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(4), event -> {
                saveBtn.setGraphic(iconView);
                saveBtn.setText("Enregistrer");
            }));
            timeline.play();
        });

        saveBtn.setOnMouseClicked(e -> {
            for (Runnable run : saveSystem.getRuns()) {
                run.run();
            }
            
            saveSystem.getSaveUi().run();
        });
        contentPane.getChildren().add(saveBtn);
    }

    @Override
    public void onShow() {
        super.onShow();
        // Animate content appearance (slide up instead of fade to avoid flickering on dark bg)
        new FadeIn(contentPane).setSpeed(3).play();
    }
}
