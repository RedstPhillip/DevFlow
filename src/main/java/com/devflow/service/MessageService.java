package com.devflow.service;

import com.devflow.config.AppConfig;
import com.devflow.model.Message;
import com.devflow.util.JsonUtil;
import com.google.gson.JsonObject;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class MessageService {

    private final HttpService http = HttpService.getInstance();

    public CompletableFuture<List<Message>> getMessages(long chatId, Long afterId) {
        String url = AppConfig.API_URL + "/chats/" + chatId + "/messages";
        if (afterId != null) {
            url += "?afterId=" + afterId;
        }
        return http.get(url)
                .thenApply(response -> {
                    if (response.statusCode() == 200) {
                        return JsonUtil.fromJsonList(response.body(), Message.class);
                    }
                    throw new RuntimeException("Failed to fetch messages: " + response.statusCode());
                });
    }

    public CompletableFuture<Message> sendMessage(long chatId, String content) {
        JsonObject body = new JsonObject();
        body.addProperty("content", content);

        return http.post(AppConfig.API_URL + "/chats/" + chatId + "/messages", body.toString())
                .thenApply(response -> {
                    if (response.statusCode() == 201) {
                        return JsonUtil.fromJson(response.body(), Message.class);
                    }
                    throw new RuntimeException("Failed to send message: " + response.statusCode());
                });
    }
}
