package com.devflow.service;

import com.devflow.config.AppConfig;
import com.devflow.model.Workspace;
import com.devflow.model.WorkspaceMember;
import com.devflow.util.JsonUtil;
import com.google.gson.JsonObject;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Client-side wrapper around the Phase-2b workspace endpoints.
 *
 * <p>One method per backend endpoint, same calling style as
 * {@link ChatService} (CompletableFuture + RuntimeException-on-error). Error
 * messages include the status code AND the raw response body so UI dialogs
 * can distinguish 404/409/400/403 on the invite-code join flow and surface
 * the backend's human-readable message.</p>
 */
public class WorkspaceService {

    private final HttpService http = HttpService.getInstance();

    public CompletableFuture<List<Workspace>> listWorkspaces() {
        return http.get(AppConfig.API_URL + "/workspaces")
                .thenApply(response -> {
                    if (response.statusCode() == 200) {
                        return JsonUtil.fromJsonList(response.body(), Workspace.class);
                    }
                    throw httpError("list workspaces", response.statusCode(), response.body());
                });
    }

    public CompletableFuture<Workspace> createWorkspace(String name) {
        JsonObject body = new JsonObject();
        body.addProperty("name", name);

        return http.post(AppConfig.API_URL + "/workspaces", body.toString())
                .thenApply(response -> {
                    if (response.statusCode() == 200 || response.statusCode() == 201) {
                        return JsonUtil.fromJson(response.body(), Workspace.class);
                    }
                    throw httpError("create workspace", response.statusCode(), response.body());
                });
    }

    public CompletableFuture<Workspace> getWorkspace(long workspaceId) {
        return http.get(AppConfig.API_URL + "/workspaces/" + workspaceId)
                .thenApply(response -> {
                    if (response.statusCode() == 200) {
                        return JsonUtil.fromJson(response.body(), Workspace.class);
                    }
                    throw httpError("get workspace", response.statusCode(), response.body());
                });
    }

    public CompletableFuture<Workspace> renameWorkspace(long workspaceId, String name) {
        JsonObject body = new JsonObject();
        if (name != null) body.addProperty("name", name);

        return http.put(AppConfig.API_URL + "/workspaces/" + workspaceId, body.toString())
                .thenApply(response -> {
                    if (response.statusCode() == 200) {
                        return JsonUtil.fromJson(response.body(), Workspace.class);
                    }
                    throw httpError("rename workspace", response.statusCode(), response.body());
                });
    }

    public CompletableFuture<Workspace> joinWorkspace(String inviteCode) {
        JsonObject body = new JsonObject();
        body.addProperty("inviteCode", inviteCode);

        return http.post(AppConfig.API_URL + "/workspaces/join", body.toString())
                .thenApply(response -> {
                    if (response.statusCode() == 200 || response.statusCode() == 201) {
                        return JsonUtil.fromJson(response.body(), Workspace.class);
                    }
                    throw httpError("join workspace", response.statusCode(), response.body());
                });
    }

    public CompletableFuture<List<WorkspaceMember>> getMembers(long workspaceId) {
        return http.get(AppConfig.API_URL + "/workspaces/" + workspaceId + "/members")
                .thenApply(response -> {
                    if (response.statusCode() == 200) {
                        return JsonUtil.fromJsonList(response.body(), WorkspaceMember.class);
                    }
                    throw httpError("list members", response.statusCode(), response.body());
                });
    }

    public CompletableFuture<Void> removeMember(long workspaceId, long userId) {
        return http.delete(AppConfig.API_URL + "/workspaces/" + workspaceId + "/members/" + userId)
                .thenApply(response -> {
                    if (response.statusCode() == 200 || response.statusCode() == 204) {
                        return null;
                    }
                    throw httpError("remove member", response.statusCode(), response.body());
                });
    }

    /**
     * Consistent error shape the dialogs can parse. The status code is
     * prepended so callers can branch on {@code startsWith("HTTP 404")}; the
     * raw body is appended so the UI can fall back to the backend's message
     * text (JSON like {@code {"error": "..."}}).
     */
    private static RuntimeException httpError(String op, int status, String body) {
        String safeBody = body == null ? "" : body;
        return new RuntimeException("HTTP " + status + " on " + op + ": " + safeBody);
    }
}
