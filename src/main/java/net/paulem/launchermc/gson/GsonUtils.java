package net.paulem.launchermc.gson;

import com.google.gson.Gson;
import net.paulem.launchermc.utils.FileUtils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class GsonUtils {
    private static String readUrl(String urlString) throws Exception {
        BufferedReader reader = null;
        try {
            URL url = new URI(urlString).toURL();
            reader = new BufferedReader(new InputStreamReader(url.openStream()));
            StringBuilder buffer = new StringBuilder();
            int read;
            char[] chars = new char[1024];
            while ((read = reader.read(chars)) != -1)
                buffer.append(chars, 0, read);

            return buffer.toString();
        } finally {
            if (reader != null)
                reader.close();
        }
    }

    public static<T> T parseJson(Class<T> type) throws Exception {
        String address = Files.readAllLines(FileUtils.getJarPath().getParent().resolve("ip.txt"), StandardCharsets.UTF_8).get(0);
        String json = readUrl("https://api.minetools.eu/ping/" + address.replace(":", "/"));

        Gson gson = new Gson();
        return gson.fromJson(json, type);
    }
}
