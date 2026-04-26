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

    /**
     * Create a group chat scoped to a workspace, optionally filed under a
     * workspace-group (folder). Since Phase 2b the backend requires
     * {@code workspaceId} — a group chat can never be "orphan" or
     * cross-workspace. Phase 2c adds the optional {@code groupId}: when
     * non-null it must belong to the same {@code workspaceId} (backend returns
     * 400 otherwise); when null the chat lands in the implicit "Allgemein"
     * section of the sidebar.
     *
     * <p>Argument order is {@code (name, ids, policy, workspaceId, groupId)} —
     * payload-shape independent, the serialized body fields match the backend
     * contract in {@code api-endpoints.md}.</p>
     */
    public CompletableFuture<Chat> createGroupChat(String name,
                                                   List<Long> memberIds,
                                                   String memberAddPolicy,
                                                   long workspaceId,
                                                   Long groupId) {
        JsonObject body = new JsonObject();
        body.addProperty("name", name);
        body.addProperty("workspaceId", workspaceId);
        body.addProperty("memberAddPolicy", memberAddPolicy);
        JsonArray members = new JsonArray();
        for (Long id : memberIds) members.add(new JsonPrimitive(id));
        body.add("memberIds", members);
        // Only emit groupId when set; omitting it (rather than sending null)
        // keeps the 2b-era payload shape for clients that never target a folder
        // and lets the backend treat "missing" = "Allgemein" unambiguously.
        if (groupId != null) body.addProperty("groupId", groupId.longValue());

        // Wire path /chats/group is the current backend contract; renaming the
        // Java method alone doesn't require a server change.
        return http.post(AppConfig.API_URL + "/chats/group", body.toString())
                .thenApply(response -> {
                    if (response.statusCode() == 200 || response.statusCode() == 201) {
                        return JsonUtil.fromJson(response.body(), Chat.class);
                    }
                    throw new RuntimeException("Failed to create group chat: " + response.statusCode());
                });
    }

    public CompletableFuture<Chat> updateGroupChat(long chatId, String name, String memberAddPolicy) {
        JsonObject body = new JsonObject();
        if (name != null) body.addProperty("name", name);
        if (memberAddPolicy != null) body.addProperty("memberAddPolicy", memberAddPolicy);

        return http.put(AppConfig.API_URL + "/chats/" + chatId, body.toString())
                .thenApply(response -> {
                    if (response.statusCode() == 200) {
                        return JsonUtil.fromJson(response.body(), Chat.class);
                    }
                    throw new RuntimeException("Failed to update group chat: " + response.statusCode());
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

    public CompletableFuture<Void> leaveGroupChat(long chatId) {
        return http.delete(AppConfig.API_URL + "/chats/" + chatId + "/leave")
                .thenApply(response -> {
                    if (response.statusCode() == 200 || response.statusCode() == 204) {
                        return null;
                    }
                    throw new RuntimeException("Failed to leave group chat: " + response.statusCode());
                });
    }
}
