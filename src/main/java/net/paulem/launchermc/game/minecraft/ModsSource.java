package net.paulem.launchermc.game.minecraft;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import fr.flowarg.flowupdater.download.json.ModrinthVersionInfo;
import fr.theshark34.openlauncherlib.util.Saver;
import net.paulem.launchermc.Launcher;
import net.paulem.launchermc.utils.Constants;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

/**
 * Resolves where the mods list comes from: an imported mods.json first, then a custom URL, then the default URL.
 */
public final class ModsSource {
    private static final String IMPORTED_FILE_NAME = "custom-mods.json";

    private ModsSource() {
        /* This utility class should not be instantiated */
    }

    public static Path getImportedFile() {
        return Launcher.getInstance().getLauncherDir().resolve(IMPORTED_FILE_NAME);
    }

    public static boolean hasImportedFile(Saver saver) {
        return "true".equals(saver.get(Constants.CONFIG_MODS_IMPORTED)) && Files.isRegularFile(getImportedFile());
    }

    public static String getCustomUrl(Saver saver) {
        String url = saver.get(Constants.CONFIG_MODS_URL);
        return url == null ? "" : url.trim();
    }

    public static List<ModrinthVersionInfo> load(Saver saver) throws IOException {
        if (hasImportedFile(saver)) {
            return parse(getImportedFile());
        }

        String url = getCustomUrl(saver);
        return ModrinthVersionInfo.getModrinthVersionsFromJson(url.isEmpty() ? MinecraftInfos.MODS_LIST_URL : url);
    }

    /**
     * Validates the file as a mods.json, then copies it into the launcher directory.
     */
    public static void importFile(Saver saver, Path source) throws IOException {
        parse(source);
        Files.copy(source, getImportedFile(), StandardCopyOption.REPLACE_EXISTING);
        saver.set(Constants.CONFIG_MODS_IMPORTED, "true");
        saver.save();
    }

    public static void clearImportedFile(Saver saver) throws IOException {
        Files.deleteIfExists(getImportedFile());
        saver.remove(Constants.CONFIG_MODS_IMPORTED);
        saver.save();
    }

    private static List<ModrinthVersionInfo> parse(Path file) throws IOException {
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            JsonArray mods = JsonParser.parseReader(reader).getAsJsonObject().getAsJsonArray("modrinthMods");
            if (mods == null) throw new IOException("Le champ \"modrinthMods\" est manquant");

            List<ModrinthVersionInfo> result = new ArrayList<>();
            for (JsonElement element : mods) {
                JsonObject obj = element.getAsJsonObject();
                JsonElement versionId = obj.get("versionId");

                if (versionId == null || versionId instanceof JsonNull) {
                    result.add(new ModrinthVersionInfo(obj.get("projectReference").getAsString(), obj.get("versionNumber").getAsString()));
                } else {
                    result.add(new ModrinthVersionInfo(versionId.getAsString()));
                }
            }
            return result;
        } catch (RuntimeException e) {
            throw new IOException("mods.json invalide : " + e.getMessage(), e);
        }
    }
}
