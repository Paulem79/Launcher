package net.paulem.launchermc.updater;

import net.paulem.launchermc.Launcher;
import net.paulem.launchermc.utils.FileUtils;
import org.jetbrains.annotations.Nullable;
import org.kohsuke.github.GHAsset;

import javax.swing.*;
import java.io.File;
import java.nio.file.Path;
import java.util.List;

public class UpdateDownloader {
    private final List<GHAsset> assets;
    
    public UpdateDownloader(List<GHAsset> assets) {
        this.assets = assets;
    }

    @Nullable
    public GHAsset getOSCorrectAsset() {
        var os = System.getProperty("os.name").toLowerCase();

        if (os.contains("win")) {
            return findAsset(".msi", ".exe");
        } else if (os.contains("mac")) {
            return findAsset(".dmg");
        } else if (os.contains("nux")) {
            // Détection de la famille Linux
            if (isDebianBased()) {
                return findAsset(".deb");
            } else if (isRpmBased()) {
                return findAsset(".rpm");
            }
            
            // Fallback si rien n'est trouvé : on cherche les deux
            return findAsset(".deb", ".rpm");
        }
        return null;
    }

    private GHAsset findAsset(String... extensions) {
        return assets.stream()
                .filter(asset -> {
                    for (String ext : extensions) {
                        if (asset.getName().endsWith(ext)) return true;
                    }
                    return false;
                })
                .findFirst()
                .orElse(null);
    }

    private boolean isDebianBased() {
        return new File("/usr/bin/dpkg").exists() || checkOsRelease("debian");
    }

    private boolean isRpmBased() {
        return new File("/usr/bin/rpm").exists() || checkOsRelease("fedora", "suse", "rhel");
    }

    private boolean checkOsRelease(String... keywords) {
        File osRelease = new File("/etc/os-release");
        if (!osRelease.exists()) return false;

        try {
            String content = org.apache.commons.io.FileUtils.readFileToString(osRelease, "UTF-8").toLowerCase();
            for (String key : keywords) {
                if (content.contains(key)) return true;
            }
        } catch (Exception ex) {
            Launcher.getInstance().getLogger().err("Error while checking OS release.");
            Launcher.getInstance().getLogger().printStackTrace(ex);
            return false;
        }
        return false;
    }
    
    public void download() {
        GHAsset asset = getOSCorrectAsset();
        if (asset == null) {
            Launcher.getInstance().getLogger().err("No compatible asset found for the current OS.");
            return;
        }
        
        // Download asset
        try {
            Launcher.getInstance().getLogger().info("Downloading update from: " + asset.getBrowserDownloadUrl());
            
            // Download file in subfolder update of launcher dir
            Path downloadPath = Launcher.getInstance().getLauncherDir().resolve("launcher-update").resolve(asset.getName());
            File parentFolder = downloadPath.getParent().toFile();
            
            if(parentFolder.exists()) {
                // Delete old folder
                deleteOldUpdateFiles(parentFolder);
            } else {
                // Create folder
                boolean created = parentFolder.mkdirs();
                
                if (!created) {
                    Launcher.getInstance().getLogger().warn("Failed to create update folder.");

                    JOptionPane.showMessageDialog(null, "Impossible de créer le dossier de mise à jour !\nTéléchargez-la depuis la page GitHub : " + asset.getBrowserDownloadUrl(), "Erreur de mise à jour", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            }
            
            File file = FileUtils.downloadFile(asset.getBrowserDownloadUrl(), downloadPath);
            
            Launcher.getInstance().getLogger().info("Update downloaded successfully to: " + downloadPath);
            
            // Open file folder in explorer
            var desktop = java.awt.Desktop.getDesktop();
            desktop.open(file.getParentFile());
            
            // Stop launcher
            Launcher.getInstance().getLogger().info("Stopping launcher...");
            Launcher.getInstance().stop();
        } catch (Exception e) {
            Launcher.getInstance().getLogger().err("Failed to download the update.");
            Launcher.getInstance().getLogger().printStackTrace(e);
        }
    }

    private static void deleteOldUpdateFiles(File parentFolder) {
        try {
            Launcher.getInstance().getLogger().info("Deleting old update files in: " + parentFolder.getAbsolutePath());
            org.apache.commons.io.FileUtils.cleanDirectory(parentFolder);
        } catch (Exception e) {
            Launcher.getInstance().getLogger().warn("Failed to delete old update files. Old files might be present in the update folder.");
            Launcher.getInstance().getLogger().printStackTrace(e);
        }
    }

    public void startOnAnotherThread() {
        new Thread(this::download).start();
    }
}
