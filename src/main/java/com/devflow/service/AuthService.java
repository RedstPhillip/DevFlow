package com.devflow.service;

import java.util.concurrent.CompletableFuture;

import com.devflow.config.AppConfig;
import com.devflow.config.TokenStore;
import com.devflow.model.AuthToken;
import com.devflow.model.User;
import com.devflow.util.JsonUtil;
import com.google.gson.JsonObject;

public class AuthService {

    private final HttpService http = HttpService.getInstance();

    public CompletableFuture<AuthToken> login(String username, String password) {
        JsonObject body = new JsonObject();
        body.addProperty("username", username);
        body.addProperty("password", password);

        return http.postNoAuth(AppConfig.API_URL + "/auth/login", body.toString())
                .thenCompose(response -> {
                    if (response.statusCode() == 200) {
                        AuthToken token = JsonUtil.fromJson(response.body(), AuthToken.class);
                        TokenStore.getInstance().setAuthToken(token);
                        return attachCurrentUser(token);
                    }
                    String error = extractError(response.body());
                    return CompletableFuture.failedFuture(new RuntimeException(error));
                });
    }

    public CompletableFuture<AuthToken> register(String username, String password) {
        JsonObject body = new JsonObject();
        body.addProperty("username", username);
        body.addProperty("password", password);

        return http.postNoAuth(AppConfig.API_URL + "/auth/register", body.toString())
                .thenCompose(response -> {
                    if (response.statusCode() == 201) {
                        AuthToken token = JsonUtil.fromJson(response.body(), AuthToken.class);
                        TokenStore.getInstance().setAuthToken(token);
                        return attachCurrentUser(token);
                    }
                    String error = extractError(response.body());
                    return CompletableFuture.failedFuture(new RuntimeException(error));
                });
    }

    private CompletableFuture<AuthToken> attachCurrentUser(AuthToken token) {
        if (token == null || token.getUser() != null) {
            return CompletableFuture.completedFuture(token);
        }
        return http.get(AppConfig.API_URL + "/users/me")
                .thenApply(response -> {
                    if (response.statusCode() == 200) {
                        User user = JsonUtil.fromJson(response.body(), User.class);
                        token.setUser(user);
                        TokenStore.getInstance().setAuthToken(token);
                    }
                    return token;
                })
                .exceptionally(ex -> token);
    }

    private String extractError(String responseBody) {
        try {
            JsonObject obj = JsonUtil.gson().fromJson(responseBody, JsonObject.class);
            if (obj.has("error")) {
                return obj.get("error").getAsString();
            }
        } catch (RuntimeException ignored) {}
        return "Unbekannter Fehler";
    }
}
