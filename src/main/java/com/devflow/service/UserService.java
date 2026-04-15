package com.devflow.service;

import com.devflow.config.AppConfig;
import com.devflow.model.User;
import com.devflow.util.JsonUtil;
import com.google.gson.JsonObject;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class UserService {

    private final HttpService http = HttpService.getInstance();

    public CompletableFuture<List<User>> listUsers() {
        return http.get(AppConfig.API_URL + "/users")
                .thenApply(response -> {
                    if (response.statusCode() == 200) {
                        return JsonUtil.fromJsonList(response.body(), User.class);
                    }
                    throw new RuntimeException("Failed to fetch users: " + response.statusCode());
                });
    }

    public CompletableFuture<List<User>> searchUsers(String query) {
        String q = URLEncoder.encode(query == null ? "" : query, StandardCharsets.UTF_8);
        return http.get(AppConfig.API_URL + "/users/search?q=" + q)
                .thenApply(response -> {
                    if (response.statusCode() == 200) {
                        return JsonUtil.fromJsonList(response.body(), User.class);
                    }
                    throw new RuntimeException("Failed to search users: " + response.statusCode());
                });
    }

    public CompletableFuture<User> getMe() {
        return http.get(AppConfig.API_URL + "/users/me")
                .thenApply(response -> {
                    if (response.statusCode() == 200) {
                        return JsonUtil.fromJson(response.body(), User.class);
                    }
                    throw new RuntimeException("Failed to fetch current user: " + response.statusCode());
                });
    }

    public CompletableFuture<User> updateProfile(String username, String avatarUrl) {
        JsonObject body = new JsonObject();
        if (username != null) body.addProperty("username", username);
        if (avatarUrl != null) body.addProperty("avatarUrl", avatarUrl);
        return http.put(AppConfig.API_URL + "/users/me", body.toString())
                .thenApply(response -> {
                    if (response.statusCode() == 200) {
                        return JsonUtil.fromJson(response.body(), User.class);
                    }
                    throw new RuntimeException("Failed to update profile: " + response.statusCode());
                });
    }
}
