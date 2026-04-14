package com.devflow.service;

import com.devflow.config.AppConfig;
import com.devflow.model.User;
import com.devflow.util.JsonUtil;

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

    public CompletableFuture<User> getMe() {
        return http.get(AppConfig.API_URL + "/users/me")
                .thenApply(response -> {
                    if (response.statusCode() == 200) {
                        return JsonUtil.fromJson(response.body(), User.class);
                    }
                    throw new RuntimeException("Failed to fetch current user: " + response.statusCode());
                });
    }
}
