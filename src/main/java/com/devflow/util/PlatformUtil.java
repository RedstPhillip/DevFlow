package com.devflow.util;

import java.nio.file.Files;
import java.nio.file.Path;

public class PlatformUtil {

    private PlatformUtil() {}

    public static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }

    public static Path getAppDataDir() {
        Path dir = Path.of(System.getProperty("user.home"), ".devflow");
        try {
            Files.createDirectories(dir);
        } catch (Exception e) {
            System.err.println("Failed to create app data dir: " + e.getMessage());
        }
        return dir;
    }
}
