package net.paulem.launchermc.game.minecraft;

import fr.flowarg.flowupdater.utils.ModFileDeleter;
import fr.flowarg.flowupdater.versions.ModLoaderVersionBuilder;
import fr.flowarg.flowupdater.versions.forge.ForgeVersionBuilder;
import fr.flowarg.openlauncherlib.NoFramework;

public class MinecraftInfos {
    private MinecraftInfos() {
        /* This utility class should not be instantiated */
    }

    public static final String GAME_VERSION = "1.20.1";
    public static final NoFramework.ModLoader MODLOADER = NoFramework.ModLoader.FORGE;
    public static final String MODLOADER_VERSION = "1.20.1-47.4.10";

    public static final String MODS_LIST_URL = "https://raw.githubusercontent.com/Paulem79/Launcher/updater/mods.json";

    public static final ModLoaderVersionBuilder<?, ?> GAME = new ForgeVersionBuilder()
            .withForgeVersion(MinecraftInfos.MODLOADER_VERSION)
            .withModrinthMods(MinecraftInfos.MODS_LIST_URL)
            .withFileDeleter(new ModFileDeleter(true));
}