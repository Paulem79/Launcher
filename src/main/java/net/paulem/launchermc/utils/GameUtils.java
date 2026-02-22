package net.paulem.launchermc.utils;

import net.paulem.launchermc.Main;
import org.jetbrains.annotations.NotNull;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

public class GameUtils {
    /**
     * Generate the game directory of the current OS by the given
     * server name, in a portable mode.
     *
     * @param serverName The server name that will be the directory
     *                   name.
     * @param inLinuxLocalShare if true, the game dir would be ~/[jarfile-parent]/share/server ; ~/[jarfile-parent] else
     * @return The generated game directory
     */
    public static @NotNull Path createGameDir(String serverName, boolean inLinuxLocalShare)
    {
        try {
            Path folderPath = getJarPath().getParent();

            final String os = Objects.requireNonNull(System.getProperty("os.name")).toLowerCase();
            if (os.contains("win")) return folderPath.resolve('.' + serverName);
            else if (os.contains("mac")) return folderPath.resolve("Library").resolve("Application Support").resolve(serverName);
            else
            {
                if(inLinuxLocalShare && os.contains("linux")) return folderPath.resolve(".local").resolve("share").resolve(serverName);
                else return folderPath.resolve('.' + serverName);
            }
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public static Path getJarPath() throws URISyntaxException {
        return Paths.get(Main.class.getProtectionDomain().getCodeSource().getLocation()
                .toURI());
    }
}
