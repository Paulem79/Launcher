package fr.paulem.launcher.game;

import fr.flowarg.flowupdater.download.json.Mod;
import fr.flowarg.flowupdater.utils.ModFileDeleter;
import fr.flowarg.flowupdater.versions.FabricVersion;
import fr.flowarg.openlauncherlib.NoFramework;

public class MinecraftInfos {
    public static final FabricVersion GAME = new FabricVersion.FabricVersionBuilder()
            .withFabricVersion(MinecraftInfos.MODLOADER_VERSION)
            .withMods(Mod.getModsFromJson(MinecraftInfos.MODS_LIST_URL))
            .withFileDeleter(new ModFileDeleter(true))
            .build();

    /* ----- POUR FORGE -----
    new ForgeVersionBuilder(MinecraftInfos.FORGE_VERSION_TYPE)
            .withForgeVersion(MinecraftInfos.FORGE_VERSION)
            .withMods(Mod.getModsFromJson(MinecraftInfos.MODS_LIST_URL))
            .withFileDeleter(new ModFileDeleter(true))
            .build();
    */

    public static final String GAME_VERSION = "1.20.2";
    //public static final ForgeVersionType FORGE_VERSION_TYPE = ForgeVersionType.NEW;
    public static final NoFramework.ModLoader MODLOADER = NoFramework.ModLoader.FABRIC;
    public static final String MODLOADER_VERSION = "0.14.24";

    public static final String MODS_LIST_URL = "https://raw.githubusercontent.com/Paulem79/Launcher/main/mods_list.json";
}
