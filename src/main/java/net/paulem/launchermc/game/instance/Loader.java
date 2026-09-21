package net.paulem.launchermc.game.instance;

import fr.flowarg.openlauncherlib.NoFramework;
import lombok.Getter;

/**
 * The mod loaders an instance can use.
 */
@Getter
public enum Loader {
    VANILLA("Vanilla", NoFramework.ModLoader.VANILLA),
    FABRIC("Fabric", NoFramework.ModLoader.FABRIC),
    QUILT("Quilt", NoFramework.ModLoader.QUILT),
    FORGE("Forge", NoFramework.ModLoader.FORGE),
    NEOFORGE("NeoForge", NoFramework.ModLoader.NEO_FORGE);

    private final String displayName;
    private final NoFramework.ModLoader noFrameworkLoader;

    Loader(String displayName, NoFramework.ModLoader noFrameworkLoader) {
        this.displayName = displayName;
        this.noFrameworkLoader = noFrameworkLoader;
    }

    public boolean hasMods() {
        return this != VANILLA;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
