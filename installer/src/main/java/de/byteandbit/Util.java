package de.byteandbit;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.byteandbit.api.TranslationApi;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static de.byteandbit.Constants.UI_ARTIFICIAL_DELAY_MS;

/**
 * Utility class for file downloads and OS detection.
 */
public class Util {


    /**
     * Extracts a resource file from the JAR to a temporary file.
     *
     * @param resourcePath URL to the resource (e.g., from getClassLoader().getResource())
     * @return Temporary file containing the extracted resource
     * @throws IOException if reading or writing fails
     */
    public static File extractResourceToTempFile(URL resourcePath) throws IOException {
        if (resourcePath == null) {
            throw new IllegalArgumentException("Resource path cannot be null");
        }

        // Extract file extension from the resource path
        String extension = "";
        String path = resourcePath.getPath();
        int lastDot = path.lastIndexOf('.');
        int lastSlash = path.lastIndexOf('/');
        if (lastDot > lastSlash && lastDot != -1) {
            extension = path.substring(lastDot);
        }

        File tempFile = Files.createTempFile("extracted_", extension).toFile();

        try (InputStream in = resourcePath.openStream();
             FileOutputStream out = new FileOutputStream(tempFile)) {
            tempFile.deleteOnExit();

            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }

            return tempFile;
        } catch (IOException e) {
            tempFile.delete(); // Clean up on failure
            throw e;
        }
    }

    /**
     * Downloads a file from the given URL to the specified folder.
     * The filename is derived from the URL.
     */
    public static File downloadFileToFolder(String url, File folder, Consumer<Integer> progressCallback) throws IOException {
        if (!folder.exists() || !folder.isDirectory()) {
            throw new IOException("Invalid folder: " + folder.getAbsolutePath());
        }

        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setInstanceFollowRedirects(true);
        conn.setRequestProperty("User-Agent", "BAB-Installer");

        int status = conn.getResponseCode();
        if (status >= 400) {
            throw new IOException("HTTP error " + status);
        }

        // --- Determine filename ---
        String filename = null;
        String disposition = conn.getHeaderField("Content-Disposition");

        if (disposition != null) {
            Matcher m = Pattern.compile(
                    "filename\\*=UTF-8''([^;]+)|filename=\"?([^\";]+)\"?",
                    Pattern.CASE_INSENSITIVE
            ).matcher(disposition);

            if (m.find()) {
                filename = m.group(1) != null
                        ? URLDecoder.decode(m.group(1), String.valueOf(StandardCharsets.UTF_8))
                        : m.group(2);
            }
        }

        // Fallback: URL path
        if (filename == null || filename.isEmpty()) {
            String path = conn.getURL().getPath();
            filename = Paths.get(path).getFileName().toString();
        }

        if (filename.isEmpty()) {
            throw new IOException("Could not determine filename");
        }

        File destination = new File(folder, filename);
        System.out.println("Downloading " + url + " to " + destination.getAbsolutePath());
        // --- Download ---
        try (InputStream in = conn.getInputStream();
             OutputStream out = new BufferedOutputStream(
                     Files.newOutputStream(destination.toPath()))) {
            long totalLength = conn.getContentLengthLong(); // -1 if unknown
            long bytesReadSoFar = 0;
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
                bytesReadSoFar += read;
                long finalBytesReadSoFar = bytesReadSoFar;
                if (progressCallback != null && totalLength > 0) {
                    int percent = (int) ((finalBytesReadSoFar * 100) / totalLength);
                    progressCallback.accept(percent);
                }
            }
        }

        return destination;
    }

    public static void ui_wait() {
        try {
            Thread.sleep(UI_ARTIFICIAL_DELAY_MS);
        } catch (InterruptedException ignored) {
        }
    }

    /**
     * Fetches JSON from a URL and deserializes it into the specified class.
     */
    public static <T> T getJsonResponse(String url, Class<T> clazz, ObjectMapper mapper) throws IOException {
        URL downloadUrl = new URL(url);
        System.out.println("Fetching " + url);
        HttpURLConnection connection = (HttpURLConnection) downloadUrl.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Accept", "application/json");
        connection.setConnectTimeout(10000);
        connection.setReadTimeout(10000);

        try (InputStream in = connection.getInputStream()) {
            return mapper.readValue(in, clazz);
        } finally {
            connection.disconnect();
        }
    }

    public static String uiText(String key) {
        return TranslationApi.getInstance().get(key);
    }

    public static <T> Consumer<T> uiThrottle(Consumer<T> delegate) {
        return throttle(delegate, 300);
    }

    public static <T> Consumer<T> throttle(Consumer<T> delegate, long intervalMillis) {
        AtomicLong lastRun = new AtomicLong(0);

        return value -> {
            long now = System.currentTimeMillis();
            long last = lastRun.get();

            if (now - last >= intervalMillis &&
                    lastRun.compareAndSet(last, now)) {
                delegate.accept(value);
            }
        };
    }

    public static boolean tryDelete(File f) {
        boolean wasDeleted = false;
        try {
            f.delete();
            wasDeleted = true;
        } catch (Exception ignored) {
        }
        return wasDeleted;
    }
}
