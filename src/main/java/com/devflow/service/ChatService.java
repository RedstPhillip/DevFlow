package com.devflow.service;

import com.devflow.config.AppConfig;
import com.devflow.model.Chat;
import com.devflow.util.JsonUtil;
import com.google.gson.JsonObject;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ChatService {

    private final HttpService http = HttpService.getInstance();

    public CompletableFuture<List<Chat>> listMyChats() {
        return http.get(AppConfig.API_URL + "/chats")
                .thenApply(response -> {
                    if (response.statusCode() == 200) {
                        return JsonUtil.fromJsonList(response.body(), Chat.class);
                    }
                    throw new RuntimeException("Failed to fetch chats: " + response.statusCode());
                });
    }

    public CompletableFuture<Chat> getOrCreateDmChat(long otherUserId) {
        JsonObject body = new JsonObject();
        body.addProperty("otherUserId", otherUserId);

        return http.post(AppConfig.API_URL + "/chats/dm", body.toString())
                .thenApply(response -> {
                    if (response.statusCode() == 200 || response.statusCode() == 201) {
                        return JsonUtil.fromJson(response.body(), Chat.class);
                    }
                    throw new RuntimeException("Failed to create DM chat: " + response.statusCode());
                });
    }
}
