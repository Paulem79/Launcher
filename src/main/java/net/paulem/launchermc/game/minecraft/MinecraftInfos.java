package net.paulem.launchermc.game.minecraft;

import fr.flowarg.flowupdater.utils.ModFileDeleter;
import fr.flowarg.flowupdater.versions.ModLoaderVersionBuilder;
import fr.flowarg.flowupdater.versions.fabric.FabricVersionBuilder;
import fr.flowarg.openlauncherlib.NoFramework;

public class MinecraftInfos {

    private MinecraftInfos() {
        /* This utility class should not be instantiated */
    }

    public static final String GAME_VERSION = "26.2";
    public static final NoFramework.ModLoader MODLOADER = NoFramework.ModLoader.FABRIC;
    public static final String MODLOADER_VERSION = "0.19.5";

    public static final String MODS_LIST_URL = "https://raw.githubusercontent.com/Paulem79/Launcher/updater/mods.json";

    public static final ModLoaderVersionBuilder<?, ?> GAME = new FabricVersionBuilder()
            .withFabricVersion(MinecraftInfos.MODLOADER_VERSION)
            .withModrinthMods(MinecraftInfos.MODS_LIST_URL)
            .withFileDeleter(new ModFileDeleter(true));
}