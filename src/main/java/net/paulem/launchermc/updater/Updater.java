package net.paulem.launchermc.updater;

import net.paulem.launchermc.Launcher;
import org.jetbrains.annotations.NotNull;
import org.kohsuke.github.GHAsset;
import org.kohsuke.github.GHRelease;
import org.kohsuke.github.GitHub;

import javax.swing.*;
import java.io.IOException;
import java.lang.module.ModuleDescriptor;

public class Updater {
    private final GitHub github;
    
    public Updater() throws IOException {
        this.github = GitHub.connectAnonymously();
    }
    
    public void checkForUpdatesAsync() {
        new Thread(() -> {
            try {
                checkForUpdates();
            } catch (IOException e) {
                Launcher.getInstance().getLogger().err("Unable to check for updates");
                Launcher.getInstance().getLogger().printStackTrace(e);
            }
        }).start();
    }
    
    public void checkForUpdates() throws IOException {
        String actualVersion = Launcher.getInstance().getVersion();
        Launcher.getInstance().getLogger().info("Current version: " + actualVersion);

        if(actualVersion == null) return;
        
        // Check latest release of https://github.com/Paulem79/Launcher/releases/tag/release
        GHRelease latestRelease = this.github.getRepository("Paulem79/Launcher").getLatestRelease();
        
        // artifacts are like Launchermc-x.x.x.dmg / Launchermc-x.x.x.msi / launchermc_x.x.x_amd64.deb with x.x.x being the version
        GHAsset latestArtifact = latestRelease.getAssets().getFirst();
        
        // Only x.x.x is needed, one character after Launchermc and terminates when it's a . followed by a letter
        String latestVersion = getLatestVersionFromArtifact(latestArtifact);
        Launcher.getInstance().getLogger().info("Latest version: " + latestVersion + " found in " + latestArtifact.getName());
        
        // Check if the latest version is newer than the actual version
        ModuleDescriptor.Version actualVersionObj = ModuleDescriptor.Version.parse(actualVersion);
        ModuleDescriptor.Version latestVersionObj = ModuleDescriptor.Version.parse(latestVersion);
        
        if (latestVersionObj.compareTo(actualVersionObj) > 0) {            
            String message = "Une nouvelle version du launcher est disponible: " + latestVersion + "\nVoulez-vous la télécharger ?";
            
            int result = JOptionPane.showConfirmDialog(null, message, "Mise à jour disponible", JOptionPane.YES_NO_OPTION);
            boolean userWantsToUpdate = result == JOptionPane.YES_OPTION;
            
            if(!userWantsToUpdate) return;
            new UpdateDownloader(latestRelease.getAssets())
                    .startOnAnotherThread();
        }
    }

    /**
     * Extracts the latest version string in the format x.x.x from a given artifact.
     * The version is assumed to follow a specific naming pattern in the artifact name
     * where it starts immediately after the prefix "Launchermc-".
     *
     * @param latestArtifact the GitHub artifact from which the version will be extracted; 
     *                       it is assumed to have a name in the format "Launchermc-x.x.x.ext"
     *                       where x.x.x represents the version number.
     * @return the extracted version in the format x.x.x; the return value is guaranteed
     *         to be non-null and non-empty if the input conforms to the expected naming convention.
     */
    private static @NotNull String getLatestVersionFromArtifact(GHAsset latestArtifact) {
        String artifactPrefix = "Launchermc-";
        int versionStartIndex = artifactPrefix.length();

        String artifactName = latestArtifact.getName()
                .substring(versionStartIndex);
        
        String[] versionSegments = artifactName
                .split("\\.");
        
        return 
                versionSegments[0] + "."
                +
                versionSegments[1] + "."
                +
                versionSegments[2]
                        .split("[^0-9]")[0];
    }
}
