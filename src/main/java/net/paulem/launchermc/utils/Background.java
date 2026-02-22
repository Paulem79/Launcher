package net.paulem.launchermc.utils;

public enum Background {
    VOLCANIC("volcanic"),
    SPAWN("spawn"),
    FOREST("forest"),
    WATER("water"),
    ISLAND("island"),
    FORTRESS("fortress"),
    BLACK_NETHER("black_nether");

    final String name;

    Background(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
