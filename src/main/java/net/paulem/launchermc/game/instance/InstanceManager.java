package net.paulem.launchermc.game.instance;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import fr.theshark34.openlauncherlib.util.Saver;
import net.paulem.launchermc.Launcher;
import net.paulem.launchermc.game.minecraft.MinecraftInfos;
import net.paulem.launchermc.game.minecraft.ModsSource;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Keeps the list of instances (instances.json) and the selected one, and owns their folders.
 * <p>
 * The built-in default instance keeps living in the launcher directory itself, so existing installs
 * do not have to download anything again. Every other instance gets its own
 * {@code instances/<id>} folder, holding its game files, mods and mods.json.
 */
public final class InstanceManager {
    private static final String INSTANCES_FILE = "instances.json";
    private static final String INSTANCES_DIR = "instances";
    private static final String MODS_FILE = "mods.json";
    private static final String CONFIG_ACTIVE_INSTANCE = "activeInstance";
    private static final Type LIST_TYPE = new TypeToken<List<Instance>>() {}.getType();

    private static InstanceManager instance;

    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final Saver saver;
    private final Path launcherDir;
    private final Instance defaultInstance = new Instance(
            Instance.DEFAULT_ID, "Par défaut", Loader.FABRIC,
            MinecraftInfos.GAME_VERSION, MinecraftInfos.MODLOADER_VERSION);
    private final List<Instance> customInstances = new ArrayList<>();

    private InstanceManager(Saver saver, Path launcherDir) {
        this.saver = saver;
        this.launcherDir = launcherDir;
        load();
    }

    public static synchronized InstanceManager get() {
        if (instance == null) {
            instance = new InstanceManager(Launcher.getInstance().getSaver(), Launcher.getInstance().getLauncherDir());
        }
        return instance;
    }

    /** The default instance first, then the user's instances. */
    public synchronized List<Instance> getAll() {
        List<Instance> all = new ArrayList<>();
        all.add(defaultInstance);
        all.addAll(customInstances);
        return all;
    }

    public synchronized Instance getActive() {
        String id = saver.get(CONFIG_ACTIVE_INSTANCE);
        for (Instance candidate : customInstances) {
            if (candidate.getId().equals(id)) return candidate;
        }
        return defaultInstance;
    }

    public synchronized void setActive(Instance target) {
        saver.set(CONFIG_ACTIVE_INSTANCE, target.getId());
        saver.save();
    }

    /** The game folder of an instance. */
    public Path getDir(Instance target) {
        return target.isDefault() ? launcherDir : instancesRoot().resolve(target.getId());
    }

    /**
     * The mods an instance installs: the default instance uses the launcher-wide mods source,
     * the others use their own mods.json (none if they were created without one).
     */
    public ModsSource.ModList getMods(Instance target) throws IOException {
        if (target.isDefault()) return ModsSource.load(saver);

        Path modsFile = getDir(target).resolve(MODS_FILE);
        return Files.isRegularFile(modsFile) ? ModsSource.parse(modsFile) : ModsSource.ModList.EMPTY;
    }

    public boolean hasModsFile(Instance target) {
        return !target.isDefault() && Files.isRegularFile(getDir(target).resolve(MODS_FILE));
    }

    /**
     * Creates an instance and its folder.
     *
     * @param modsFile a mods.json to copy into the instance, or null for none
     * @throws IOException if the mods.json is invalid or the folder cannot be created
     */
    public synchronized Instance create(String name, Loader loader, String gameVersion, String loaderVersion, Path modsFile) throws IOException {
        if (modsFile != null) {
            ModsSource.parse(modsFile); // validate before touching the disk
        }

        Instance created = new Instance(newId(name), name, loader, gameVersion, loaderVersion);
        Path dir = getDir(created);
        Files.createDirectories(dir);
        try {
            if (modsFile != null) {
                Files.copy(modsFile, dir.resolve(MODS_FILE), StandardCopyOption.REPLACE_EXISTING);
            }
            customInstances.add(created);
            save();
        } catch (IOException | RuntimeException e) {
            customInstances.remove(created);
            deleteRecursively(dir);
            throw e;
        }
        return created;
    }

    /** Deletes an instance and its whole folder. The default instance cannot be deleted. */
    public synchronized void delete(Instance target) throws IOException {
        if (target.isDefault()) {
            throw new IOException("L'instance par défaut ne peut pas être supprimée");
        }

        Path dir = getDir(target).toAbsolutePath().normalize();
        if (!dir.startsWith(instancesRoot().toAbsolutePath().normalize()) || dir.equals(instancesRoot().toAbsolutePath().normalize())) {
            throw new IOException("Dossier d'instance invalide : " + dir);
        }

        boolean wasActive = getActive().equals(target);
        deleteRecursively(dir);

        customInstances.remove(target);
        save();
        if (wasActive) setActive(defaultInstance);
    }

    private Path instancesRoot() {
        return launcherDir.resolve(INSTANCES_DIR);
    }

    private String newId(String name) {
        String slug = name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("(^-+)|(-+$)", "");
        if (slug.isEmpty()) slug = "instance";
        if (slug.length() > 32) slug = slug.substring(0, 32);
        return slug + "-" + UUID.randomUUID().toString().substring(0, 6);
    }

    private void load() {
        Path file = launcherDir.resolve(INSTANCES_FILE);
        if (!Files.isRegularFile(file)) return;

        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            List<Instance> loaded = gson.fromJson(reader, LIST_TYPE);
            if (loaded != null) {
                for (Instance candidate : loaded) {
                    // Ignore corrupted entries, and ids that could escape the instances folder
                    if (candidate != null && candidate.getId() != null && candidate.getName() != null
                            && candidate.getLoader() != null && candidate.getGameVersion() != null
                            && candidate.getId().matches("[a-z0-9-]+") && !candidate.isDefault()) {
                        customInstances.add(candidate);
                    }
                }
            }
        } catch (IOException | RuntimeException e) {
            Launcher.getInstance().getLogger().err("Unable to read " + INSTANCES_FILE);
            Launcher.getInstance().getLogger().printStackTrace(e);
        }
    }

    private void save() throws IOException {
        Path file = launcherDir.resolve(INSTANCES_FILE);
        Path temp = launcherDir.resolve(INSTANCES_FILE + ".tmp");
        try (Writer writer = Files.newBufferedWriter(temp, StandardCharsets.UTF_8)) {
            gson.toJson(customInstances, LIST_TYPE, writer);
        }
        Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
    }

    private static void deleteRecursively(Path dir) throws IOException {
        if (!Files.exists(dir)) return;

        Files.walkFileTree(dir, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path directory, IOException exc) throws IOException {
                if (exc != null) throw exc;
                Files.delete(directory);
                return FileVisitResult.CONTINUE;
            }
        });
    }
}
