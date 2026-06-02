package org.example.client.core;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class UploadStorage {

    private static final Path UPLOAD_ROOT = Path.of(System.getProperty("user.dir"), "uploads");

    private UploadStorage() {
    }

    public static File getUploadDirectory() {
        File directory = UPLOAD_ROOT.toFile();
        if (!directory.exists()) {
            directory.mkdirs();
        }
        return directory;
    }

    public static String copyToUploads(File source, String username, String prefix) throws IOException {
        if (source == null || !source.exists()) {
            return null;
        }
        String safeUsername = username == null ? "unknown" : username.replaceAll("[^a-zA-Z0-9_-]", "_");
        String fileName = prefix + "_" + safeUsername + "_" + System.currentTimeMillis() + "_" + source.getName();
        File destination = new File(getUploadDirectory(), fileName);
        Files.copy(source.toPath(), destination.toPath(), StandardCopyOption.REPLACE_EXISTING);
        return destination.getAbsolutePath();
    }

    public static File resolveFile(String storedPath) {
        if (storedPath == null || storedPath.isBlank()) {
            return null;
        }
        File file = new File(storedPath);
        if (file.exists()) {
            return file;
        }
        File relative = new File(getUploadDirectory(), new File(storedPath).getName());
        return relative.exists() ? relative : null;
    }
}
