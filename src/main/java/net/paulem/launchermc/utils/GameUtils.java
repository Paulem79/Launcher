package net.paulem.launchermc.utils;

import net.paulem.launchermc.Launcher;
import net.paulem.launchermc.Main;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class GameUtils {
    /**
     * Generate the game directory by the given
     * server name, in a portable mode.
     *
     * @param serverName The server name that will be the directory
     *                   name.
     * @return The generated game directory
     */
    public static @NotNull Path createGameDir(String serverName)
    {
        try {
            Path folderPath = getJarPath().getParent();

            return folderPath.resolve('.' + serverName);
        } catch (URISyntaxException e) {
            Launcher.getInstance().getLogger().err("Unable to get the game directory.");
            Launcher.getInstance().getLogger().printStackTrace(e);
            return Paths.get("");
        }
    }

    public static Path getJarPath() throws URISyntaxException {
        return Paths.get(Main.class.getProtectionDomain().getCodeSource().getLocation()
                .toURI());
    }

    public static boolean createLauncherDir(Launcher launcher) {
        if (Files.exists(launcher.getLauncherDir())) {
            return true;
        }
        
        try {
            Files.createDirectory(launcher.getLauncherDir());
            
            return true;
        } catch (IOException e) {
            launcher.getLogger().err("Unable to create launcher folder");
            launcher.getLogger().printStackTrace(e);
        }
        
        return false;
    }
}
