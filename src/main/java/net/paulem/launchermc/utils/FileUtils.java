package net.paulem.launchermc.utils;

import net.paulem.launchermc.Main;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FileUtils {
    private FileUtils(){}
    
    public static File downloadFile(String url, Path destination) throws IOException {
        URL website = URI.create(url).toURL();
        
        try (ReadableByteChannel rbc = Channels.newChannel(website.openStream());
             FileOutputStream fos = new FileOutputStream(destination.toFile())) {
            fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
        }
        
        return destination.toFile();
    }

    public static Path getJarPath() throws URISyntaxException {
        return Paths.get(Main.class.getProtectionDomain().getCodeSource().getLocation()
                .toURI());
    }
}
