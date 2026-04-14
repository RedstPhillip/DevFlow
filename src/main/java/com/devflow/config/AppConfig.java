package com.devflow.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class AppConfig {

    private static final Properties props = new Properties();

    static {
        try (InputStream in = AppConfig.class.getResourceAsStream("/config/app.properties")) {
            if (in != null) {
                props.load(in);
            }
        } catch (IOException e) {
            System.err.println("Failed to load app.properties: " + e.getMessage());
        }
    }

    public static final String APP_VERSION = props.getProperty("app.version", "1.0.0");
    public static final String API_URL = props.getProperty("api.url", "http://localhost:8080/api");
    public static final String GITHUB_OWNER = props.getProperty("github.owner", "FplayG");
    public static final String GITHUB_REPO = props.getProperty("github.repo", "DevFlow");

    public static final int POLL_INTERVAL_MS = 2000;
    public static final int CHAT_LIST_REFRESH_MS = 10000;

    private AppConfig() {}
}
