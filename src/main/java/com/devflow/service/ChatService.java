package com.devflow.service;

import com.devflow.config.AppConfig;
import com.devflow.model.Chat;
import com.devflow.util.JsonUtil;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

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

    public CompletableFuture<Chat> createGroup(String name, List<Long> memberIds, String memberAddPolicy) {
        JsonObject body = new JsonObject();
        body.addProperty("name", name);
        body.addProperty("memberAddPolicy", memberAddPolicy);
        JsonArray members = new JsonArray();
        for (Long id : memberIds) members.add(new JsonPrimitive(id));
        body.add("memberIds", members);

        return http.post(AppConfig.API_URL + "/chats/group", body.toString())
                .thenApply(response -> {
                    if (response.statusCode() == 200 || response.statusCode() == 201) {
                        return JsonUtil.fromJson(response.body(), Chat.class);
                    }
                    throw new RuntimeException("Failed to create group: " + response.statusCode());
                });
    }

    public CompletableFuture<Chat> updateGroup(long chatId, String name, String memberAddPolicy) {
        JsonObject body = new JsonObject();
        if (name != null) body.addProperty("name", name);
        if (memberAddPolicy != null) body.addProperty("memberAddPolicy", memberAddPolicy);

        return http.put(AppConfig.API_URL + "/chats/" + chatId, body.toString())
                .thenApply(response -> {
                    if (response.statusCode() == 200) {
                        return JsonUtil.fromJson(response.body(), Chat.class);
                    }
                    throw new RuntimeException("Failed to update group: " + response.statusCode());
                });
    }

    public CompletableFuture<Chat> addMember(long chatId, long userId) {
        JsonObject body = new JsonObject();
        body.addProperty("userId", userId);
        return http.post(AppConfig.API_URL + "/chats/" + chatId + "/members", body.toString())
                .thenApply(response -> {
                    if (response.statusCode() == 200 || response.statusCode() == 201) {
                        return JsonUtil.fromJson(response.body(), Chat.class);
                    }
                    throw new RuntimeException("Failed to add member: " + response.statusCode());
                });
    }

    public CompletableFuture<Void> removeMember(long chatId, long userId) {
        return http.delete(AppConfig.API_URL + "/chats/" + chatId + "/members/" + userId)
                .thenApply(response -> {
                    if (response.statusCode() == 200 || response.statusCode() == 204) {
                        return null;
                    }
                    throw new RuntimeException("Failed to remove member: " + response.statusCode());
                });
    }

    public CompletableFuture<Void> leaveGroup(long chatId) {
        return http.delete(AppConfig.API_URL + "/chats/" + chatId + "/leave")
                .thenApply(response -> {
                    if (response.statusCode() == 200 || response.statusCode() == 204) {
                        return null;
                    }
                    throw new RuntimeException("Failed to leave group: " + response.statusCode());
                });
    }
}
