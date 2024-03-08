package fr.paulem.launcherupdater;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.swing.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyManagementException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

public class Main {
    public static void main(String[] args) throws Exception {
        downloadFromGithub();
    }

    public static void launch() throws IOException {
        final JarFile jarFile = new JarFile("Launcher.jar");
        final Manifest manifest = jarFile.getManifest();
        final Attributes attributes = manifest.getMainAttributes();
        final String javaVersion = attributes.getValue("Version");

        if (javaVersion.isEmpty())
            throw new IllegalStateException("Version isn't specified, ask paulem on Discord!");

        if (javaVersion.equals("8"))
            new ProcessBuilder("j8\\bin\\java.exe", "-jar", "Launcher.jar").start();

        else if (javaVersion.equals("17"))
            new ProcessBuilder("j17\\bin\\java.exe", "-jar", "Launcher.jar").start();

        else
            throw new IllegalStateException("Error with java " + javaVersion);
    }

    public static void downloadFromGithub() throws Exception {
        final String apiUrl = "https://api.github.com/repos/Paulem79/Launcher/releases/latest";
        final String jsonString = getJsonStringFromUrl(apiUrl);
        final String downloadUrl = getDownloadUrlFromJsonString(jsonString);
        final URL fileUrl = new URL(downloadUrl);
        final HttpURLConnection connection = (HttpURLConnection)fileUrl.openConnection();

        compareFile(connection, downloadUrl);
        launch();
    }

    public static void downloadFile(final String url) throws IOException {
        //final JFrame jframe = showUpdateWindow();
        final Path localFilePath = Paths.get("Launcher.jar");
        final URL fileUrl = new URL(url);
        final HttpURLConnection connection = (HttpURLConnection)fileUrl.openConnection();

        try (final InputStream in = connection.getInputStream();
             final OutputStream out = Files.newOutputStream(localFilePath.toFile().toPath())) {
            final byte[] dataBuffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = in.read(dataBuffer, 0, 1024)) != -1) {
                out.write(dataBuffer, 0, bytesRead);
            }
        }

        //jframe.dispose();
    }

    public static String getJsonStringFromUrl(final String url) throws IOException, NoSuchAlgorithmException, KeyManagementException {
        final URL apiUrl = new URL(url);
        final HttpsURLConnection connection = (HttpsURLConnection)apiUrl.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Accept", "application/vnd.github.v3+json");
        final SSLContext sc = SSLContext.getInstance("SSL");
        sc.init(null, new TrustManager[] { new X509TrustManager() {
            @Override
            public void checkClientTrusted(final X509Certificate[] x509Certificates, final String s) {
            }

            @Override
            public void checkServerTrusted(final X509Certificate[] x509Certificates, final String s) {
            }

            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return new X509Certificate[0];
            }
        } }, new SecureRandom());

        HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
        HttpsURLConnection.setDefaultHostnameVerifier((hostname, session) -> true);

        try (final BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
            final StringBuilder responseBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                responseBuilder.append(line);
            }

            return responseBuilder.toString();
        }
    }

    public static String getDownloadUrlFromJsonString(final String jsonString) {
        final int startIndex = jsonString.indexOf("\"name\": \"Launcher.jar\"");
        final int endIndex = jsonString.indexOf("\"browser_download_url\":", startIndex) + 24;
        final int endIndex2 = jsonString.indexOf("\"", endIndex);

        return jsonString.substring(endIndex, endIndex2);
    }

    public static void showUpdateWindow() {
        JOptionPane.showMessageDialog(null, "Mise à jour du launcher", "Launcher", JOptionPane.INFORMATION_MESSAGE);
        /*final JLabel comp = new JLabel("Mise a jour du launcher !", SwingConstants.CENTER);
        comp.setFont(new Font("Arial", Font.PLAIN, 14));

        final JFrame frame = new JFrame();
        frame.setUndecorated(true);
        frame.add(comp);
        frame.setSize(200, 50);
        frame.getContentPane().setBackground(Color.LIGHT_GRAY);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        return frame;*/
    }

    public static byte[] getSha256Hash(final InputStream inputStream) throws Exception {
        final MessageDigest digest = MessageDigest.getInstance("SHA-256");
        final byte[] buffer = new byte[8192];
        int bytesRead;

        while ((bytesRead = inputStream.read(buffer)) != -1)
            digest.update(buffer, 0, bytesRead);

        return digest.digest();
    }

    public static void compareFile(final HttpURLConnection connection, final String downloadUrl) throws Exception {
        final File localFile = new File("Launcher.jar");
        if (localFile.exists() && localFile.isFile()) {
            final int responseCode = connection.getResponseCode();
            if (responseCode == 200) {
                try (final InputStream remoteStream = connection.getInputStream();
                     final InputStream localStream = Files.newInputStream(localFile.toPath())) {
                    final byte[] remoteFileHash = getSha256Hash(remoteStream);
                    final byte[] localFileHash = getSha256Hash(localStream);
                    if (!MessageDigest.isEqual(remoteFileHash, localFileHash))
                        downloadFile(downloadUrl);
                }
            }
        }
        else {
            downloadFile(downloadUrl);
        }
    }
}