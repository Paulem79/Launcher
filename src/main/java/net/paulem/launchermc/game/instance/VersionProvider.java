package net.paulem.launchermc.game.instance;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Looks up the available Minecraft and mod loader versions online. All methods block on the network.
 */
public final class VersionProvider {
    private static final String MOJANG_MANIFEST = "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json";
    private static final String FABRIC_META = "https://meta.fabricmc.net/v2/versions/loader/";
    private static final String QUILT_META = "https://meta.quiltmc.org/v3/versions/loader/";
    private static final String FORGE_PROMOTIONS = "https://files.minecraftforge.net/net/minecraftforge/forge/promotions_slim.json";
    private static final String NEOFORGE_VERSIONS = "https://maven.neoforged.net/api/maven/versions/releases/net/neoforged/neoforge";

    private VersionProvider() {
        /* This utility class should not be instantiated */
    }

    /** Release versions of Minecraft, newest first. */
    public static List<String> getGameVersions() throws IOException {
        List<String> versions = new ArrayList<>();
        for (JsonElement element : read(MOJANG_MANIFEST).getAsJsonObject().getAsJsonArray("versions")) {
            JsonObject version = element.getAsJsonObject();
            if ("release".equals(version.get("type").getAsString())) {
                versions.add(version.get("id").getAsString());
            }
        }
        return versions;
    }

    /**
     * The latest loader version compatible with the given Minecraft version, in the format FlowUpdater expects.
     *
     * @throws IOException if the lookup fails or the loader does not support this Minecraft version
     */
    public static String getLatestLoaderVersion(Loader loader, String gameVersion) throws IOException {
        return switch (loader) {
            case VANILLA -> null;
            case FABRIC -> latestFabricLike(FABRIC_META, loader, gameVersion);
            case QUILT -> latestFabricLike(QUILT_META, loader, gameVersion);
            case FORGE -> latestForge(gameVersion);
            case NEOFORGE -> latestNeoForge(gameVersion);
        };
    }

    private static String latestFabricLike(String meta, Loader loader, String gameVersion) throws IOException {
        JsonArray array = read(meta + gameVersion).getAsJsonArray();
        String fallback = null;
        for (JsonElement element : array) {
            JsonObject entry = element.getAsJsonObject().getAsJsonObject("loader");
            String version = entry.get("version").getAsString();
            // Quilt has no "stable" flag: treat pre-releases (containing "-") as unstable
            boolean stable = entry.has("stable") ? entry.get("stable").getAsBoolean() : !version.contains("-");
            if (stable) return version;
            if (fallback == null) fallback = version;
        }
        if (fallback == null) throw unsupported(loader, gameVersion);
        return fallback;
    }

    private static String latestForge(String gameVersion) throws IOException {
        JsonObject promos = read(FORGE_PROMOTIONS).getAsJsonObject().getAsJsonObject("promos");
        JsonElement forge = promos.get(gameVersion + "-latest");
        if (forge == null) forge = promos.get(gameVersion + "-recommended");
        if (forge == null) throw unsupported(Loader.FORGE, gameVersion);
        return gameVersion + "-" + forge.getAsString();
    }

    private static String latestNeoForge(String gameVersion) throws IOException {
        // NeoForge drops the leading "1." of the Minecraft version: 1.21.1 -> 21.1.x, 26.1 -> 26.1.x
        String[] parts = gameVersion.split("\\.");
        if (parts[0].equals("1")) {
            parts = Arrays.copyOfRange(parts, 1, parts.length);
        }
        if (parts.length == 1) {
            parts = new String[]{parts[0], "0"};
        }
        String prefix = String.join(".", parts) + ".";

        String latest = null;
        String latestBeta = null;
        for (JsonElement element : read(NEOFORGE_VERSIONS).getAsJsonObject().getAsJsonArray("versions")) {
            String version = element.getAsString();
            if (!version.startsWith(prefix)) continue;
            // ascending order: the last match is the newest
            if (version.contains("-")) latestBeta = version;
            else latest = version;
        }
        if (latest == null) latest = latestBeta;
        if (latest == null) throw unsupported(Loader.NEOFORGE, gameVersion);
        return latest;
    }

    private static IOException unsupported(Loader loader, String gameVersion) {
        return new IOException(loader.getDisplayName() + " ne supporte pas Minecraft " + gameVersion);
    }

    private static JsonElement read(String url) throws IOException {
        URLConnection connection = URI.create(url).toURL().openConnection();
        connection.setConnectTimeout(10_000);
        connection.setReadTimeout(15_000);
        connection.setRequestProperty("User-Agent", "LauncherMC");
        try (Reader reader = new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader);
        }
    }
}
