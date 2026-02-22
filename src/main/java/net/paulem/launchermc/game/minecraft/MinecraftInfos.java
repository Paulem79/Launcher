package net.paulem.launchermc.game.minecraft;

import fr.flowarg.flowupdater.utils.ModFileDeleter;
import fr.flowarg.flowupdater.versions.ModLoaderVersionBuilder;
import fr.flowarg.flowupdater.versions.fabric.FabricVersionBuilder;
import fr.flowarg.openlauncherlib.NoFramework;

public class MinecraftInfos {
    public static final String GAME_VERSION = "1.21.11";
    //public static final ForgeVersionType FORGE_VERSION_TYPE = ForgeVersionType.NEW;
    public static final NoFramework.ModLoader MODLOADER = NoFramework.ModLoader.FABRIC;
    public static final String MODLOADER_VERSION = "0.18.4";

    public static final String MODS_LIST_URL = "https://raw.githubusercontent.com/Paulem79/Launcher/updater/mods_potes.json";
    
    public static final ModLoaderVersionBuilder<?, ?> GAME = new FabricVersionBuilder()
            //.withFabricVersion(MinecraftInfos.MODLOADER_VERSION)
            /*.withMods(MinecraftInfos.MODS_LIST_URL)
            .withCurseMods(MinecraftInfos.MODS_LIST_URL)
            .withModrinthMods(MinecraftInfos.MODS_LIST_URL)*/
            .withFileDeleter(new ModFileDeleter(true));
}
