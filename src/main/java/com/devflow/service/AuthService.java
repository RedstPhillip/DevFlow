package com.devflow.service;

import com.devflow.config.AppConfig;
import com.devflow.config.TokenStore;
import com.devflow.model.AuthToken;
import com.devflow.util.JsonUtil;
import com.google.gson.JsonObject;

import java.util.concurrent.CompletableFuture;

public class AuthService {

    private final HttpService http = HttpService.getInstance();

    public CompletableFuture<AuthToken> login(String username, String password) {
        JsonObject body = new JsonObject();
        body.addProperty("username", username);
        body.addProperty("password", password);

        return http.postNoAuth(AppConfig.API_URL + "/auth/login", body.toString())
                .thenApply(response -> {
                    if (response.statusCode() == 200) {
                        AuthToken token = JsonUtil.fromJson(response.body(), AuthToken.class);
                        TokenStore.getInstance().setAuthToken(token);
                        return token;
                    }
                    String error = extractError(response.body());
                    throw new RuntimeException(error);
                });
    }

    public CompletableFuture<AuthToken> register(String username, String password) {
        JsonObject body = new JsonObject();
        body.addProperty("username", username);
        body.addProperty("password", password);

        return http.postNoAuth(AppConfig.API_URL + "/auth/register", body.toString())
                .thenApply(response -> {
                    if (response.statusCode() == 201) {
                        AuthToken token = JsonUtil.fromJson(response.body(), AuthToken.class);
                        TokenStore.getInstance().setAuthToken(token);
                        return token;
                    }
                    String error = extractError(response.body());
                    throw new RuntimeException(error);
                });
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
