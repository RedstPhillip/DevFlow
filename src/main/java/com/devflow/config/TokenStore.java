package com.devflow.config;

import com.devflow.model.AuthToken;
import com.devflow.util.JsonUtil;
import com.devflow.util.PlatformUtil;
import com.google.gson.JsonObject;

import java.nio.file.Files;
import java.nio.file.Path;

public class TokenStore {

    private static final Path TOKEN_FILE = PlatformUtil.getAppDataDir().resolve("tokens.json");

    private String githubPat;
    private AuthToken authToken;

    private static final TokenStore INSTANCE = new TokenStore();

    private TokenStore() {
        load();
    }

    public static TokenStore getInstance() {
        return INSTANCE;
    }

    private void load() {
        try {
            if (Files.exists(TOKEN_FILE)) {
                String json = Files.readString(TOKEN_FILE);
                JsonObject obj = JsonUtil.gson().fromJson(json, JsonObject.class);
                if (obj.has("githubPat")) {
                    githubPat = obj.get("githubPat").getAsString();
                }
                if (obj.has("authToken")) {
                    authToken = JsonUtil.fromJson(obj.get("authToken").toString(), AuthToken.class);
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to load tokens: " + e.getMessage());
        }
    }

    private void save() {
        try {
            JsonObject obj = new JsonObject();
            if (githubPat != null) {
                obj.addProperty("githubPat", githubPat);
            }
            if (authToken != null) {
                obj.add("authToken", JsonUtil.gson().toJsonTree(authToken));
            }
            Files.createDirectories(TOKEN_FILE.getParent());
            Files.writeString(TOKEN_FILE, JsonUtil.gson().toJson(obj));
        } catch (Exception e) {
            System.err.println("Failed to save tokens: " + e.getMessage());
        }
    }

    public String getGithubPat() {
        return githubPat;
    }

    public void setGithubPat(String pat) {
        this.githubPat = pat;
        save();
    }

    public AuthToken getAuthToken() {
        return authToken;
    }

    public void setAuthToken(AuthToken token) {
        this.authToken = token;
        save();
    }

    public void clearAuthToken() {
        this.authToken = null;
        save();
    }

    public boolean hasAuthToken() {
        return authToken != null && authToken.getAccessToken() != null;
    }
}
