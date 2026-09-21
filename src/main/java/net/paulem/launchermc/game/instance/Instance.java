package net.paulem.launchermc.game.instance;

import lombok.Getter;

/**
 * A game profile: its own Minecraft version, mod loader and game folder.
 * Serialized as-is into instances.json (the default instance is never written there).
 */
@Getter
public final class Instance {
    public static final String DEFAULT_ID = "default";

    private final String id;
    private final String name;
    private final Loader loader;
    private final String gameVersion;
    /** Loader version as expected by FlowUpdater (for Forge: "mc-forge"). Null for vanilla. */
    private final String loaderVersion;

    public Instance(String id, String name, Loader loader, String gameVersion, String loaderVersion) {
        this.id = id;
        this.name = name;
        this.loader = loader;
        this.gameVersion = gameVersion;
        this.loaderVersion = loaderVersion;
    }

    public boolean isDefault() {
        return DEFAULT_ID.equals(id);
    }

    /** The version NoFramework needs to find the mod loader json (Forge: without the Minecraft prefix). */
    public String getNoFrameworkLoaderVersion() {
        if (loaderVersion == null) return null;
        if (loader == Loader.FORGE) {
            String[] parts = loaderVersion.split("-");
            return parts.length >= 2 ? parts[1] : loaderVersion;
        }
        return loaderVersion;
    }

    public String describe() {
        return loader.hasMods()
                ? loader.getDisplayName() + " " + loaderVersion + " · Minecraft " + gameVersion
                : "Vanilla · Minecraft " + gameVersion;
    }

    @Override
    public String toString() {
        return name;
    }
}
