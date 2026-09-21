package net.paulem.launchermc.updater;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * The changelog of a published GitHub release: its notes are the release body written by the CI.
 */
public record Changelog(String tag, String title, Instant publishedAt, String body, String pageUrl) {
    private static final String RELEASES = Updater.API + "/repos/Paulem79/Launcher/releases?per_page=20";

    public static List<Changelog> fetchAll() throws IOException, InterruptedException {
        HttpClient http = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        HttpRequest request = HttpRequest.newBuilder(URI.create(RELEASES))
                .timeout(Duration.ofSeconds(15))
                .header("Accept", "application/vnd.github+json")
                .GET().build();
        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) throw new IOException("HTTP " + response.statusCode() + " from " + RELEASES);

        JsonArray releases = JsonParser.parseString(response.body()).getAsJsonArray();
        List<Changelog> changelogs = new ArrayList<>();
        for (JsonElement element : releases) {
            JsonObject json = element.getAsJsonObject();
            if (json.get("draft").getAsBoolean()) continue;

            String tag = json.get("tag_name").getAsString();
            changelogs.add(new Changelog(
                    tag,
                    string(json, "name", tag),
                    Instant.parse(string(json, "published_at", Instant.now().toString())),
                    string(json, "body", ""),
                    json.get("html_url").getAsString()));
        }
        return changelogs;
    }

    private static String string(JsonObject json, String key, String fallback) {
        JsonElement value = json.get(key);
        return value == null || value.isJsonNull() ? fallback : value.getAsString();
    }
}
