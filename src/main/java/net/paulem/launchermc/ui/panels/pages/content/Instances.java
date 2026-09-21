package net.paulem.launchermc.ui.panels.pages.content;

import animatefx.animation.FadeIn;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import net.paulem.launchermc.game.instance.Instance;
import net.paulem.launchermc.game.instance.InstanceManager;
import net.paulem.launchermc.game.instance.Loader;
import net.paulem.launchermc.game.instance.VersionProvider;
import net.paulem.launchermc.ui.components.GradientButton;
import net.paulem.launchermc.ui.panels.PanelManager;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Lists the instances, lets the user pick the active one, create new ones and delete them.
 */
public class Instances extends ContentPanel {
    private final InstanceManager manager = InstanceManager.get();

    private final VBox instanceList = new VBox(8);
    private final Label createStatus = new Label();

    @Override
    public String getName() {
        return "instances";
    }

    @Override
    public String getStylesheetPath() {
        return "css/content/instances.css";
    }

    @Override
    public void init(PanelManager panelManager) {
        super.init(panelManager);

        this.layout.getStyleClass().add("instances-layout");
        this.layout.setPadding(new Insets(40, 40, 40, 85));
        setCanTakeAllSize(this.layout);

        contentPane.getStyleClass().add("content-pane");
        setCanTakeAllSize(contentPane);
        this.layout.getChildren().add(contentPane);

        VBox root = new VBox(14);
        root.setPadding(new Insets(28, 25, 25, 25));
        setCanTakeAllSize(root);
        contentPane.getChildren().add(root);

        Label title = new Label("Instances");
        title.getStyleClass().add("instances-title");

        instanceList.setFillWidth(true);
        ScrollPane scroll = new ScrollPane(instanceList);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.getStyleClass().add("instances-scroll");
        scroll.setMinHeight(120);
        VBox.setVgrow(scroll, Priority.ALWAYS);

        root.getChildren().addAll(title, scroll, buildCreateForm());
        refreshList();
    }

    private void refreshList() {
        instanceList.getChildren().clear();
        Instance active = manager.getActive();

        for (Instance instance : manager.getAll()) {
            instanceList.getChildren().add(buildRow(instance, instance.equals(active)));
        }
    }

    private HBox buildRow(Instance instance, boolean active) {
        Label name = new Label(instance.getName() + (instance.isDefault() ? "  (incluse)" : ""));
        name.getStyleClass().add("instance-name");
        Label details = new Label(instance.describe());
        details.getStyleClass().add("instance-details");
        VBox texts = new VBox(2, name, details);
        HBox.setHgrow(texts, Priority.ALWAYS);

        HBox row = new HBox(10, texts);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("instance-row");
        if (active) row.getStyleClass().add("instance-row-active");

        Button select = new Button(active ? "Sélectionnée" : "Sélectionner");
        select.getStyleClass().add("instance-btn");
        select.setDisable(active);
        select.setOnAction(e -> {
            manager.setActive(instance);
            refreshList();
        });
        row.getChildren().add(select);

        if (!instance.isDefault()) {
            Button delete = new Button("Supprimer");
            delete.getStyleClass().addAll("instance-btn", "instance-btn-danger");
            delete.setOnAction(e -> confirmDelete(instance, row));
            row.getChildren().add(delete);
        }
        return row;
    }

    private void confirmDelete(Instance instance, HBox row) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                "Supprimer l'instance « " + instance.getName() + " » ?\n"
                        + "Son dossier (mods, mondes, configuration) sera définitivement supprimé.",
                ButtonType.YES, ButtonType.CANCEL);
        alert.setTitle("Supprimer l'instance");
        alert.setHeaderText(null);
        if (alert.showAndWait().filter(button -> button == ButtonType.YES).isEmpty()) return;

        row.setDisable(true);
        Thread thread = new Thread(() -> {
            String error = null;
            try {
                manager.delete(instance);
            } catch (IOException ex) {
                logger.printStackTrace(ex);
                error = "Suppression impossible : " + ex.getMessage();
            }
            String message = error;
            Platform.runLater(() -> {
                createStatus.setText(message == null ? "" : message);
                refreshList();
            });
        }, "instance-delete");
        thread.setDaemon(true);
        thread.start();
    }

    private VBox buildCreateForm() {
        Label heading = new Label("Nouvelle instance");
        heading.getStyleClass().add("instances-subtitle");

        TextField nameField = new TextField();
        nameField.setPromptText("Nom de l'instance");
        nameField.getStyleClass().add("instances-field");
        HBox.setHgrow(nameField, Priority.ALWAYS);

        ComboBox<Loader> loaderBox = new ComboBox<>();
        loaderBox.getItems().addAll(Loader.values());
        loaderBox.setValue(Loader.FABRIC);
        loaderBox.getStyleClass().add("instances-combo");

        ComboBox<String> versionBox = new ComboBox<>();
        versionBox.setPromptText("Chargement des versions…");
        versionBox.setDisable(true);
        versionBox.getStyleClass().add("instances-combo");
        loadGameVersions(versionBox);

        HBox line1 = new HBox(10, nameField, loaderBox, versionBox);
        line1.setAlignment(Pos.CENTER_LEFT);

        AtomicReference<File> modsFile = new AtomicReference<>();
        Label modsLabel = new Label("Aucun mods.json");
        modsLabel.getStyleClass().add("instance-details");
        Button pickMods = new Button("Choisir un mods.json");
        pickMods.getStyleClass().add("instance-btn");
        Button clearMods = new Button("Retirer");
        clearMods.getStyleClass().add("instance-btn");
        clearMods.setVisible(false);
        clearMods.managedProperty().bind(clearMods.visibleProperty());

        pickMods.setOnAction(e -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Choisir un mods.json");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON", "*.json"));
            File file = chooser.showOpenDialog(pickMods.getScene().getWindow());
            if (file == null) return;
            modsFile.set(file);
            modsLabel.setText(file.getName());
            clearMods.setVisible(true);
        });
        clearMods.setOnAction(e -> {
            modsFile.set(null);
            modsLabel.setText("Aucun mods.json");
            clearMods.setVisible(false);
        });
        loaderBox.valueProperty().addListener((obs, old, loader) -> {
            boolean mods = loader != null && loader.hasMods();
            pickMods.setDisable(!mods);
            if (!mods) clearMods.fire();
        });

        HBox line2 = new HBox(10, pickMods, clearMods, modsLabel);
        line2.setAlignment(Pos.CENTER_LEFT);

        createStatus.getStyleClass().add("instance-details");
        createStatus.setWrapText(true);

        GradientButton create = new GradientButton("Créer l'instance", 14, 1.04);
        create.getStyleClass().add("instances-create-btn");
        create.setOnAction(e -> {
            String name = nameField.getText().trim();
            Loader loader = loaderBox.getValue();
            String gameVersion = versionBox.getValue();
            if (name.isEmpty() || loader == null || gameVersion == null) {
                createStatus.setText("Renseigne un nom, un loader et une version.");
                return;
            }

            create.setDisable(true);
            createStatus.setText("Création en cours…");
            File mods = modsFile.get();

            Thread thread = new Thread(() -> {
                String status;
                boolean created = false;
                try {
                    String loaderVersion = VersionProvider.getLatestLoaderVersion(loader, gameVersion);
                    manager.create(name, loader, gameVersion, loaderVersion, mods == null ? null : mods.toPath());
                    created = true;
                    status = "Instance « " + name + " » créée.";
                } catch (IOException | RuntimeException ex) {
                    logger.printStackTrace(ex);
                    status = "Création impossible : " + ex.getMessage();
                }

                String message = status;
                boolean reset = created;
                Platform.runLater(() -> {
                    createStatus.setText(message);
                    create.setDisable(false);
                    if (reset) {
                        nameField.clear();
                        clearMods.fire();
                        refreshList();
                    }
                });
            }, "instance-create");
            thread.setDaemon(true);
            thread.start();
        });

        VBox form = new VBox(10, heading, line1, line2, create, createStatus);
        form.getStyleClass().add("instances-form");
        return form;
    }

    private void loadGameVersions(ComboBox<String> versionBox) {
        Thread thread = new Thread(() -> {
            try {
                List<String> versions = VersionProvider.getGameVersions();
                Platform.runLater(() -> {
                    versionBox.getItems().setAll(versions);
                    if (!versions.isEmpty()) versionBox.setValue(versions.getFirst());
                    versionBox.setPromptText("Version");
                    versionBox.setDisable(false);
                });
            } catch (IOException | RuntimeException ex) {
                logger.printStackTrace(ex);
                Platform.runLater(() -> {
                    versionBox.setPromptText("Hors-ligne");
                    createStatus.setText("Impossible de récupérer les versions de Minecraft : " + ex.getMessage());
                });
            }
        }, "instance-versions");
        thread.setDaemon(true);
        thread.start();
    }

    @Override
    public void onShow() {
        super.onShow();
        new FadeIn(contentPane).setSpeed(3).play();
    }
}
