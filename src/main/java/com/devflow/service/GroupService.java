package com.devflow.service;

import com.devflow.config.AppConfig;
import com.devflow.model.Group;
import com.devflow.util.JsonUtil;
import com.google.gson.JsonObject;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Client-side wrapper around the Phase-2c workspace-group endpoints:
 *
 * <ul>
 *   <li>{@code GET    /api/workspaces/{wsId}/groups}</li>
 *   <li>{@code POST   /api/workspaces/{wsId}/groups}</li>
 *   <li>{@code PUT    /api/workspaces/{wsId}/groups/{gid}}</li>
 *   <li>{@code DELETE /api/workspaces/{wsId}/groups/{gid}}</li>
 * </ul>
 *
 * <p>Errors follow the same "HTTP &lt;code&gt; on &lt;op&gt;: &lt;body&gt;"
 * shape as {@link WorkspaceService} so dialogs can branch on status prefix.</p>
 */
public class GroupService {

    private final HttpService http = HttpService.getInstance();

    public CompletableFuture<List<Group>> listGroups(long workspaceId) {
        return http.get(AppConfig.API_URL + "/workspaces/" + workspaceId + "/groups")
                .thenApply(response -> {
                    if (response.statusCode() == 200) {
                        return JsonUtil.fromJsonList(response.body(), Group.class);
                    }
                    throw httpError("list groups", response.statusCode(), response.body());
                });
    }

    public CompletableFuture<Group> createGroup(long workspaceId, String name) {
        JsonObject body = new JsonObject();
        body.addProperty("name", name);
        return http.post(AppConfig.API_URL + "/workspaces/" + workspaceId + "/groups", body.toString())
                .thenApply(response -> {
                    if (response.statusCode() == 200 || response.statusCode() == 201) {
                        return JsonUtil.fromJson(response.body(), Group.class);
                    }
                    throw httpError("create group", response.statusCode(), response.body());
                });
    }

    public CompletableFuture<Group> renameGroup(long workspaceId, long groupId, String name) {
        JsonObject body = new JsonObject();
        body.addProperty("name", name);
        return http.put(AppConfig.API_URL + "/workspaces/" + workspaceId + "/groups/" + groupId, body.toString())
                .thenApply(response -> {
                    if (response.statusCode() == 200) {
                        return JsonUtil.fromJson(response.body(), Group.class);
                    }
                    throw httpError("rename group", response.statusCode(), response.body());
                });
    }

    public CompletableFuture<Void> deleteGroup(long workspaceId, long groupId) {
        return http.delete(AppConfig.API_URL + "/workspaces/" + workspaceId + "/groups/" + groupId)
                .thenApply(response -> {
                    if (response.statusCode() == 200 || response.statusCode() == 204) {
                        return null;
                    }
                    throw httpError("delete group", response.statusCode(), response.body());
                });
    }

    private static RuntimeException httpError(String op, int status, String body) {
        String safeBody = body == null ? "" : body;
        return new RuntimeException("HTTP " + status + " on " + op + ": " + safeBody);
    }
}
