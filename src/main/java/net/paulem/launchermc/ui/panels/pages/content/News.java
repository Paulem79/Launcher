package net.paulem.launchermc.ui.panels.pages.content;

import animatefx.animation.FadeIn;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import net.paulem.launchermc.ui.panels.PanelManager;
import net.paulem.launchermc.updater.Changelog;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Lists the changelogs of the GitHub releases, newest first.
 */
public class News extends ContentPanel {
    private static final DateTimeFormatter DATE = DateTimeFormatter
            .ofLocalizedDate(FormatStyle.LONG).withLocale(Locale.FRANCE).withZone(ZoneId.systemDefault());
    private static final Pattern LINK = Pattern.compile("\\[([^]]*)]\\([^)]*\\)");
    // The launcher font can't render emojis, so headings keep only their text
    private static final Pattern LEADING_SYMBOLS = Pattern.compile("^[^\\p{L}\\p{N}]+");

    private final VBox newsList = new VBox(12);
    private final Label status = new Label("Chargement des actualités…");

    @Override
    public String getName() {
        return "news";
    }

    @Override
    public String getStylesheetPath() {
        return "css/content/news.css";
    }

    @Override
    public void init(PanelManager panelManager) {
        super.init(panelManager);

        this.layout.getStyleClass().add("news-layout");
        this.layout.setPadding(new Insets(40, 40, 40, 85));
        setCanTakeAllSize(this.layout);

        contentPane.getStyleClass().add("content-pane");
        setCanTakeAllSize(contentPane);
        this.layout.getChildren().add(contentPane);

        VBox root = new VBox(14);
        root.setPadding(new Insets(28, 25, 25, 25));
        setCanTakeAllSize(root);
        contentPane.getChildren().add(root);

        Label title = new Label("Actualités");
        title.getStyleClass().add("news-title");

        status.getStyleClass().add("news-details");
        status.setWrapText(true);

        newsList.setFillWidth(true);
        ScrollPane scroll = new ScrollPane(newsList);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.getStyleClass().add("news-scroll");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        root.getChildren().addAll(title, status, scroll);
        load();
    }

    private void load() {
        Thread thread = new Thread(() -> {
            try {
                List<Changelog> changelogs = Changelog.fetchAll();
                Platform.runLater(() -> show(changelogs));
            } catch (IOException | RuntimeException e) {
                logger.printStackTrace(e);
                Platform.runLater(() -> status.setText("Impossible de récupérer les actualités : " + e.getMessage()));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "news-fetch");
        thread.setDaemon(true);
        thread.start();
    }

    private void show(List<Changelog> changelogs) {
        boolean empty = changelogs.isEmpty();
        status.setText("Aucune actualité pour le moment.");
        status.setVisible(empty);
        status.setManaged(empty);
        for (Changelog changelog : changelogs) newsList.getChildren().add(buildCard(changelog));
    }

    private VBox buildCard(Changelog changelog) {
        Label title = new Label(changelog.title());
        title.getStyleClass().add("news-release-title");
        Label date = new Label(DATE.format(changelog.publishedAt()));
        date.getStyleClass().add("news-details");
        VBox heading = new VBox(2, title, date);
        HBox.setHgrow(heading, Priority.ALWAYS);

        Button open = new Button("Voir sur GitHub");
        open.getStyleClass().add("news-btn");
        open.setOnAction(e -> openUrl(changelog.pageUrl()));

        HBox header = new HBox(10, heading, open);
        header.setAlignment(Pos.CENTER_LEFT);

        VBox card = new VBox(6, header);
        card.getStyleClass().add("news-card");
        renderBody(changelog.body(), card);
        return card;
    }

    /**
     * The release notes are markdown written by the CI: "## / ### " headings, "- " items and a trailing
     * "Full Changelog" link. Only that subset is rendered.
     */
    private static void renderBody(String body, VBox card) {
        for (String raw : body.split("\\R")) {
            String line = raw.strip();
            if (line.isEmpty() || line.startsWith("**Full Changelog**")) continue;

            Label label;
            if (line.startsWith("#")) {
                String heading = plain(line.replaceFirst("^#+\\s*", ""));
                label = new Label(LEADING_SYMBOLS.matcher(heading).replaceFirst(""));
                label.getStyleClass().add("news-section");
            } else if (line.startsWith("- ") || line.startsWith("* ")) {
                label = new Label("•  " + plain(line.substring(2)));
                label.getStyleClass().add("news-item");
            } else {
                label = new Label(plain(line));
                label.getStyleClass().add("news-item");
            }
            label.setWrapText(true);
            card.getChildren().add(label);
        }
    }

    private static String plain(String markdown) {
        return LINK.matcher(markdown).replaceAll("$1").replace("**", "").replace("`", "");
    }

    private void openUrl(String url) {
        try {
            if (Desktop.isDesktopSupported()) Desktop.getDesktop().browse(URI.create(url));
        } catch (Exception e) {
            logger.warn("Unable to open " + url);
        }
    }

    @Override
    public void onShow() {
        super.onShow();
        new FadeIn(contentPane).setSpeed(3).play();
    }
}
