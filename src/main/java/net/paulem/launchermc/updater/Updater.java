package net.paulem.launchermc.updater;

import com.google.gson.JsonParser;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import net.paulem.launchermc.Launcher;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/**
 * Compares the release number baked into this build with the latest GitHub release (tag "v<N>") and, if
 * newer, offers to download and install it.
 */
public class Updater {
    // LAUNCHER_UPDATE_API only exists to point the updater to a fake server when testing
    private static final String API = System.getenv().getOrDefault("LAUNCHER_UPDATE_API", "https://api.github.com");
    private static final String LATEST_RELEASE = API + "/repos/Paulem79/Launcher/releases/latest";

    private final HttpClient http = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public void checkForUpdatesAsync() {
        Thread thread = new Thread(() -> {
            try {
                checkForUpdates();
            } catch (Exception e) {
                Launcher.getInstance().getLogger().err("Unable to check for updates");
                Launcher.getInstance().getLogger().printStackTrace(e);
            }
        }, "updater");
        thread.setDaemon(true);
        thread.start();
    }

    public void checkForUpdates() throws IOException, InterruptedException {
        int current = Launcher.getInstance().getReleaseNumber();
        Launcher.getInstance().getLogger().info("Current release: " + (current > 0 ? current : "dev build"));

        // Local/dev builds carry no release number
        if (current <= 0) return;

        cleanOldUpdateFiles();

        ReleaseInfo latest = fetchLatestRelease();
        Launcher.getInstance().getLogger().info("Latest release: " + latest.number() + " (" + latest.tag() + ")");
        if (latest.number() <= current) return;

        UpdateDownloader downloader = new UpdateDownloader(http);
        ReleaseInfo.Asset asset = downloader.pickAsset(latest);
        if (asset == null) {
            Launcher.getInstance().getLogger().warn("No installer found for this OS in release " + latest.tag());
            return;
        }

        if (!askUser(latest)) return;

        UpdateProgress progress = UpdateProgress.show();
        Path installer;
        try {
            installer = downloader.download(latest, asset, progress::set);
        } catch (Exception e) {
            progress.close();
            Launcher.getInstance().getLogger().err("Failed to download the update.");
            Launcher.getInstance().getLogger().printStackTrace(e);
            fail(latest, "Le téléchargement de la mise à jour a échoué.");
            return;
        }

        progress.installing();
        try {
            if (UpdateInstaller.installAndRestart(installer)) {
                Launcher.getInstance().getLogger().info("Installing update, stopping launcher...");
                Platform.runLater(() -> Launcher.getInstance().stop());
                return;
            }
        } catch (Exception e) {
            Launcher.getInstance().getLogger().err("Failed to start the installer.");
            Launcher.getInstance().getLogger().printStackTrace(e);
        }

        // Can't install automatically here (portable mode, unpackaged run...): hand the file to the user
        progress.close();
        openFolder(installer.getParent());
        fail(latest, "L'installation automatique n'est pas possible ici.\nLe fichier a été téléchargé, ouvrez-le pour installer la mise à jour.");
    }

    /**
     * The installer of a previous update is useless once we are running the new version.
     */
    private static void cleanOldUpdateFiles() {
        Path folder = Launcher.getInstance().getLauncherDir().resolve("launcher-update");
        try {
            if (Files.isDirectory(folder)) {
                try (var files = Files.list(folder)) {
                    for (Path file : files.toList()) Files.deleteIfExists(file);
                }
            }
        } catch (IOException e) {
            Launcher.getInstance().getLogger().warn("Unable to clean " + folder);
        }
    }

    private ReleaseInfo fetchLatestRelease() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(LATEST_RELEASE))
                .timeout(Duration.ofSeconds(15))
                .header("Accept", "application/vnd.github+json")
                .GET().build();
        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) throw new IOException("HTTP " + response.statusCode() + " from " + LATEST_RELEASE);
        return ReleaseInfo.fromJson(JsonParser.parseString(response.body()).getAsJsonObject());
    }

    private static boolean askUser(ReleaseInfo latest) {
        CompletableFuture<Boolean> answer = new CompletableFuture<>();
        Platform.runLater(() -> {
            ButtonType update = new ButtonType("Mettre à jour", ButtonBar.ButtonData.OK_DONE);
            ButtonType later = new ButtonType("Plus tard", ButtonBar.ButtonData.CANCEL_CLOSE);
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                    "Le launcher sera mis à jour puis redémarrera automatiquement.", update, later);
            alert.setTitle("Mise à jour disponible");
            alert.setHeaderText("Une nouvelle version du launcher est disponible (release " + latest.number() + ")");
            answer.complete(alert.showAndWait().orElse(later) == update);
        });
        return answer.join();
    }

    private static void fail(ReleaseInfo latest, String message) {
        Platform.runLater(() -> {
            ButtonType open = new ButtonType("Page de téléchargement", ButtonBar.ButtonData.OK_DONE);
            ButtonType close = new ButtonType("Fermer", ButtonBar.ButtonData.CANCEL_CLOSE);
            Alert alert = new Alert(Alert.AlertType.ERROR, message, open, close);
            alert.setTitle("Mise à jour");
            alert.setHeaderText("Impossible de mettre à jour automatiquement");
            if (alert.showAndWait().orElse(close) == open) openUrl(latest.pageUrl());
        });
    }

    private static void openFolder(Path folder) {
        try {
            if (Desktop.isDesktopSupported()) Desktop.getDesktop().open(folder.toFile());
        } catch (Exception e) {
            Launcher.getInstance().getLogger().warn("Unable to open " + folder);
        }
    }

    private static void openUrl(String url) {
        try {
            if (Desktop.isDesktopSupported()) Desktop.getDesktop().browse(URI.create(url));
        } catch (Exception e) {
            Launcher.getInstance().getLogger().warn("Unable to open " + url);
        }
    }

    /**
     * Small window showing the download progress.
     */
    private static final class UpdateProgress {
        private final Stage stage = new Stage(StageStyle.UTILITY);
        private final Label label = new Label("Téléchargement de la mise à jour...");
        private final ProgressBar bar = new ProgressBar(ProgressBar.INDETERMINATE_PROGRESS);

        private UpdateProgress() {
            bar.setPrefWidth(320);
            VBox box = new VBox(10, label, bar);
            box.setPadding(new Insets(20));
            stage.setTitle("Mise à jour");
            stage.setResizable(false);
            stage.setAlwaysOnTop(true);
            stage.setScene(new Scene(box));
            stage.setOnCloseRequest(Event -> Event.consume());
            stage.show();
        }

        static UpdateProgress show() {
            CompletableFuture<UpdateProgress> future = new CompletableFuture<>();
            Platform.runLater(() -> future.complete(new UpdateProgress()));
            return future.join();
        }

        void set(double value) {
            Platform.runLater(() -> bar.setProgress(value));
        }

        void installing() {
            Platform.runLater(() -> {
                label.setText("Installation en cours, le launcher va redémarrer...");
                bar.setProgress(ProgressBar.INDETERMINATE_PROGRESS);
            });
        }

        void close() {
            Platform.runLater(stage::close);
        }
    }
}
