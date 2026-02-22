package net.paulem.launchermc.utils;

import com.sun.management.OperatingSystemMXBean;
import net.paulem.launchermc.Launcher;

import java.lang.management.ManagementFactory;
import java.util.UUID;
import java.util.function.Supplier;

public class Constants {
    public static final double TITLE_OFFSET_Y = 15d;
    public static final double NAVBUTTON_OFFSET_Y = 1d;

    public static final Supplier<Integer> DEFAULT_MAX_RAM = () -> {
        OperatingSystemMXBean os = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
        long totalBytes;
        try {
            totalBytes = os.getTotalMemorySize();
        } catch (Throwable ignored) {
            totalBytes = Runtime.getRuntime().maxMemory();
        }
        long halfBytes = totalBytes / 2L;
        int ramAmount = (int) (halfBytes / (1024L * 1024L));
        Launcher.getInstance().getLogger().info("Default max ram amount : " + ramAmount);
        return ramAmount;
    };
    public static final String CONFIG_MAXRAM = "maxRam";

    public static final String RPC_APP_ID = "1266045291161976884";
    public static final String RPC_LAUNCHER = "Dans le launcher";
    public static final String RPC_LOGIN = "Se connecte";
    public static final String RPC_UPDATE = "Télécharge les fichiers";
    public static final String RPC_CONNECTED = "En jeu";
    public static final String RPC_LARGE_IMG_KEY = "icon";
    public static final String RPC_STATE_PLAYERS = "Joueurs";
    public static final String RPC_STATE_NO_PLAYERS = "Pas de joueur connecté";
    public static final String RPC_PARTY_ID = UUID.randomUUID().toString();
}
