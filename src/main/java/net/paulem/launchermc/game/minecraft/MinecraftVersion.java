package net.paulem.launchermc.game.minecraft;

import fr.flowarg.flowupdater.utils.ModFileDeleter;
import fr.flowarg.flowupdater.versions.IModLoaderVersion;
import fr.flowarg.flowupdater.versions.ModLoaderVersionBuilder;
import fr.flowarg.flowupdater.versions.fabric.FabricVersionBuilder;
import fr.flowarg.flowupdater.versions.fabric.QuiltVersionBuilder;
import fr.flowarg.flowupdater.versions.forge.ForgeVersionBuilder;
import fr.flowarg.flowupdater.versions.neoforge.NeoForgeVersionBuilder;
import net.paulem.launchermc.game.instance.Instance;
import net.paulem.launchermc.game.instance.InstanceManager;

import java.io.IOException;

public class MinecraftVersion {
    private MinecraftVersion() {
        /* This utility class should not be instantiated */
    }

    /**
     * The mod loader FlowUpdater must install for this instance, with its mods.
     *
     * @return null for a vanilla instance
     */
    public static IModLoaderVersion create(Instance instance) throws IOException {
        ModsSource.ModList list = InstanceManager.get().getMods(instance);

        ModLoaderVersionBuilder<?, ?> builder = switch (instance.getLoader()) {
            case VANILLA -> null;
            case FABRIC -> new FabricVersionBuilder().withFabricVersion(instance.getLoaderVersion());
            case QUILT -> new QuiltVersionBuilder().withQuiltVersion(instance.getLoaderVersion());
            case FORGE -> new ForgeVersionBuilder().withForgeVersion(instance.getLoaderVersion());
            case NEOFORGE -> new NeoForgeVersionBuilder().withNeoForgeVersion(instance.getLoaderVersion());
        };
        if (builder == null) return null;

        return builder
                .withMods(list.mods())
                .withCurseMods(list.curseFiles())
                .withModrinthMods(list.modrinthMods())
                .withFileDeleter(new ModFileDeleter(true))
                .build();
    }
}
