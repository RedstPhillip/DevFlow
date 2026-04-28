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
    public static final String APP_COMMIT = props.getProperty("app.commit", "");
    public static final String API_URL = props.getProperty("api.url", "https://devflow.redstphillip.uk/api");
    public static final String GITHUB_OWNER = props.getProperty("github.owner", "redstphillip");
    public static final String GITHUB_REPO = props.getProperty("github.repo", "devflow");
    public static final String GITHUB_BRANCH = props.getProperty("github.branch", "main");

    public static final int POLL_INTERVAL_MS = 2000;
    public static final int CHAT_LIST_REFRESH_MS = 10000;

    private AppConfig() {}
}
