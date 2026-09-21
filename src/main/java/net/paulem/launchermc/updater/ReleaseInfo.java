package net.paulem.launchermc.updater;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A GitHub release. The release number is the one of the tag ("v73" -> 73), which is the CI run number
 * also baked into the launcher at build time.
 */
public record ReleaseInfo(int number, String tag, String pageUrl, List<Asset> assets) {
    private static final Pattern TAG_NUMBER = Pattern.compile("([0-9]+)");

    public record Asset(String name, String url, long size) {
    }

    public static ReleaseInfo fromJson(JsonObject json) {
        String tag = json.get("tag_name").getAsString();
        Matcher matcher = TAG_NUMBER.matcher(tag);
        if (!matcher.find()) throw new IllegalArgumentException("Unexpected release tag: " + tag);

        List<Asset> assets = new ArrayList<>();
        for (JsonElement element : json.getAsJsonArray("assets")) {
            JsonObject asset = element.getAsJsonObject();
            assets.add(new Asset(
                    asset.get("name").getAsString(),
                    asset.get("browser_download_url").getAsString(),
                    asset.get("size").getAsLong()));
        }

        return new ReleaseInfo(Integer.parseInt(matcher.group(1)), tag, json.get("html_url").getAsString(), assets);
    }

    public Asset findAsset(String... extensions) {
        for (String extension : extensions) {
            for (Asset asset : assets) {
                if (asset.name().endsWith(extension)) return asset;
            }
        }
        return null;
    }

    public Asset findByName(String name) {
        return assets.stream().filter(asset -> asset.name().equals(name)).findFirst().orElse(null);
    }
}
