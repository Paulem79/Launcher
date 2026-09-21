package net.paulem.launchermc.updater;

import net.paulem.launchermc.Launcher;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;
import java.util.function.DoubleConsumer;

/**
 * Downloads the installer matching the current OS and checks it against the published sha256 file.
 */
public class UpdateDownloader {
    private final HttpClient http;

    public UpdateDownloader(HttpClient http) {
        this.http = http;
    }

    /**
     * @return the installer to use on this OS, or null when none is published.
     */
    public ReleaseInfo.Asset pickAsset(ReleaseInfo release) {
        return switch (UpdateInstaller.OS) {
            case WINDOWS -> release.findAsset(".msi", ".exe");
            case MAC -> release.findAsset(".dmg");
            case LINUX -> UpdateInstaller.isRpmBased() && !UpdateInstaller.isDebianBased()
                    ? release.findAsset(".rpm", ".deb")
                    : release.findAsset(".deb", ".rpm");
            case OTHER -> null;
        };
    }

    public Path download(ReleaseInfo release, ReleaseInfo.Asset asset, DoubleConsumer progress) throws IOException, InterruptedException {
        Path folder = Launcher.getInstance().getLauncherDir().resolve("launcher-update");
        Files.createDirectories(folder);
        // Only keep the current update
        try (var files = Files.list(folder)) {
            for (Path old : files.toList()) Files.deleteIfExists(old);
        }

        Path target = folder.resolve(asset.name());
        Launcher.getInstance().getLogger().info("Downloading update from: " + asset.url());

        HttpResponse<InputStream> response = http.send(request(asset.url()), HttpResponse.BodyHandlers.ofInputStream());
        if (response.statusCode() != 200) throw new IOException("HTTP " + response.statusCode() + " for " + asset.url());

        long total = asset.size() > 0 ? asset.size() : response.headers().firstValueAsLong("Content-Length").orElse(-1);
        MessageDigest digest = sha256();
        try (InputStream in = response.body(); OutputStream out = Files.newOutputStream(target)) {
            byte[] buffer = new byte[64 * 1024];
            long done = 0;
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
                digest.update(buffer, 0, read);
                done += read;
                if (total > 0) progress.accept(Math.min(1d, (double) done / total));
            }
        }

        verifyChecksum(release, asset, HexFormat.of().formatHex(digest.digest()));
        Launcher.getInstance().getLogger().info("Update downloaded successfully to: " + target);
        return target;
    }

    private void verifyChecksum(ReleaseInfo release, ReleaseInfo.Asset asset, String actual) throws IOException, InterruptedException {
        ReleaseInfo.Asset checksumAsset = release.findByName(asset.name() + ".sha256");
        if (checksumAsset == null) {
            Launcher.getInstance().getLogger().warn("No checksum published for " + asset.name() + ", skipping verification.");
            return;
        }

        HttpResponse<String> response = http.send(request(checksumAsset.url()), HttpResponse.BodyHandlers.ofString());
        String expected = response.body().trim().split("\s+")[0];
        if (response.statusCode() != 200 || !expected.equalsIgnoreCase(actual)) {
            throw new IOException("Checksum mismatch for " + asset.name());
        }
    }

    private static HttpRequest request(String url) {
        return HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofMinutes(10)).GET().build();
    }

    private static MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
