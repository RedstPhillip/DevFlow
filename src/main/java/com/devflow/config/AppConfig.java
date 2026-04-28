package com.devflow.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class AppConfig {

    private static final Properties props = new Properties();
    private static final String version;

    static {
        try (InputStream in = AppConfig.class.getResourceAsStream("/config/app.properties")) {
            if (in != null) {
                props.load(in);
            }
        } catch (IOException e) {
            System.err.println("Failed to load app.properties: " + e.getMessage());
        }
        version = loadVersion();
    }

    public static final String APP_VERSION = version;
    public static final String API_URL = props.getProperty("api.url", "https://devflow.redstphillip.uk/api");
    public static final String GITHUB_OWNER = props.getProperty("github.owner", "redstphillip");
    public static final String GITHUB_REPO = props.getProperty("github.repo", "devflow");

    public static final int POLL_INTERVAL_MS = 2000;
    public static final int CHAT_LIST_REFRESH_MS = 10000;

    private AppConfig() {}

    private static String loadVersion() {
        try (InputStream in = AppConfig.class.getResourceAsStream("/VERSION")) {
            if (in != null) {
                String value = new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8).trim();
                if (!value.isBlank()) return value;
            }
        } catch (IOException e) {
            System.err.println("Failed to load VERSION: " + e.getMessage());
        }
        return props.getProperty("app.version", "1.0.0");
    }
}
