package net.paulem.launchermc.game.minecraft;

import fr.flowarg.flowupdater.utils.ModFileDeleter;
import fr.flowarg.flowupdater.versions.ModLoaderVersionBuilder;
import fr.flowarg.flowupdater.versions.forge.ForgeVersionBuilder;
import fr.flowarg.openlauncherlib.NoFramework;
import org.jetbrains.annotations.Nullable;

public class MinecraftInfos {

    private MinecraftInfos() {
        /* This utility class should not be instantiated */
    }

    public static final String GAME_VERSION = "1.20.1";
    public static final NoFramework.ModLoader MODLOADER = NoFramework.ModLoader.FORGE;
    public static final String MODLOADER_VERSION = "1.20.1-47.4.22";
    @Nullable
    public static final String MCP_VERSION = "20230612.114412"; // Or null if not on Forge or doesn't need fixing

    public static final String MODS_LIST_URL = "https://raw.githubusercontent.com/Paulem79/Launcher/updater/mods.json";

    public static final ModLoaderVersionBuilder<?, ?> GAME = new ForgeVersionBuilder()
            .withForgeVersion(MinecraftInfos.MODLOADER_VERSION)
            .withModrinthMods(MinecraftInfos.MODS_LIST_URL)
            .withFileDeleter(new ModFileDeleter(true));
}