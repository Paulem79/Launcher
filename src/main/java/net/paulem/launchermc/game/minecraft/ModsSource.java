package net.paulem.launchermc.game.minecraft;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import fr.flowarg.flowupdater.download.json.CurseFileInfo;
import fr.flowarg.flowupdater.download.json.Mod;
import fr.flowarg.flowupdater.download.json.ModrinthVersionInfo;
import fr.flowarg.flowupdater.utils.FlowUpdaterException;
import fr.flowarg.flowupdater.utils.IOUtils;
import fr.theshark34.openlauncherlib.util.Saver;
import net.paulem.launchermc.Launcher;
import net.paulem.launchermc.utils.Constants;

import java.io.IOException;
import java.io.Reader;
import java.net.URL;
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

    /**
     * The mods described by a mods.json: direct downloads ("mods"), CurseForge files ("curseFiles")
     * and Modrinth versions ("modrinthMods"). Every section is optional.
     */
    public record ModList(List<Mod> mods, List<CurseFileInfo> curseFiles, List<ModrinthVersionInfo> modrinthMods) {
        public static final ModList EMPTY = new ModList(List.of(), List.of(), List.of());

        public int size() {
            return mods.size() + curseFiles.size() + modrinthMods.size();
        }
    }

    public static ModList load(Saver saver) throws IOException {
        if (hasImportedFile(saver)) {
            try (Reader reader = Files.newBufferedReader(getImportedFile(), StandardCharsets.UTF_8)) {
                return parse(JsonParser.parseReader(reader));
            }
        }

        String url = getCustomUrl(saver);
        try {
            return parse(IOUtils.readJson(new URL(url.isEmpty() ? MinecraftInfos.MODS_LIST_URL : url)));
        } catch (FlowUpdaterException e) {
            throw new IOException("Impossible de lire la liste des mods : " + e.getMessage(), e);
        }
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

    /** Reads and validates a mods.json file. */
    public static ModList parse(Path file) throws IOException {
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            return parse(JsonParser.parseReader(reader));
        }
    }

    private static ModList parse(JsonElement root) throws IOException {
        try {
            JsonObject object = root.getAsJsonObject();
            List<Mod> mods = new ArrayList<>();
            List<CurseFileInfo> curseFiles = new ArrayList<>();
            List<ModrinthVersionInfo> modrinthMods = new ArrayList<>();

            for (JsonElement element : array(object, "mods")) {
                mods.add(Mod.fromJson(element));
            }

            // "curseMods" is accepted as an alias of FlowUpdater's "curseFiles"
            for (JsonElement element : array(object, "curseFiles", "curseMods")) {
                JsonObject obj = element.getAsJsonObject();
                curseFiles.add(new CurseFileInfo(obj.get("projectID").getAsInt(), obj.get("fileID").getAsInt()));
            }

            for (JsonElement element : array(object, "modrinthMods")) {
                JsonObject obj = element.getAsJsonObject();
                JsonElement versionId = obj.get("versionId");

                if (versionId == null || versionId instanceof JsonNull) {
                    modrinthMods.add(new ModrinthVersionInfo(obj.get("projectReference").getAsString(), obj.get("versionNumber").getAsString()));
                } else {
                    modrinthMods.add(new ModrinthVersionInfo(versionId.getAsString()));
                }
            }

            ModList list = new ModList(mods, curseFiles, modrinthMods);
            if (list.size() == 0) {
                throw new IOException("Aucun mod trouvé (attendu : \"mods\", \"curseFiles\" ou \"modrinthMods\")");
            }
            return list;
        } catch (IOException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new IOException("mods.json invalide : " + e.getMessage(), e);
        }
    }

    private static JsonArray array(JsonObject object, String... names) {
        for (String name : names) {
            JsonElement element = object.get(name);
            if (element != null && element.isJsonArray()) return element.getAsJsonArray();
        }
        return new JsonArray();
    }
}
