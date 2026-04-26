package com.devflow.model;

import com.google.gson.annotations.SerializedName;

import java.time.LocalDateTime;

/**
 * Client-side POJO for a workspace group (folder) as returned by
 * {@code /api/workspaces/{workspaceId}/groups/*}.
 *
 * <p>Groups are purely organizational. They nest under a {@link Workspace}
 * and hold {@link Chat}s via {@link Chat#getGroupId()}. There is no
 * permissioning at the group level in 2c — every workspace member sees and
 * posts in every group of the workspace.</p>
 *
 * <p>Deletion is non-cascading on the server: deleting a group sets each
 * contained {@code chat.group_id} to {@code NULL}, so the chats themselves
 * survive and fall back into the implicit "Allgemein" section.</p>
 */
public class Group {

    private long id;

    @SerializedName(value = "workspaceId", alternate = {"workspace_id"})
    private long workspaceId;

    private String name;

    @SerializedName(value = "sortOrder", alternate = {"sort_order"})
    private int sortOrder;

    @SerializedName(value = "createdAt", alternate = {"created_at"})
    private LocalDateTime createdAt;

    public Group() {}

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getWorkspaceId() { return workspaceId; }
    public void setWorkspaceId(long workspaceId) { this.workspaceId = workspaceId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() { return name; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Group other)) return false;
        return id == other.id;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(id);
    }
}
