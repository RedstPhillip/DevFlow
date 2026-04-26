package com.devflow.model;

import com.google.gson.annotations.SerializedName;

import java.time.LocalDateTime;

/**
 * Client-side POJO for an entry from
 * {@code GET /api/workspaces/{workspaceId}/members}.
 *
 * <p>Backend returns {@code userId}/{@code username}/{@code role}/{@code joinedAt}
 * per row. The list is already sorted server-side (Owner first, then by
 * joinedAt ASC), so the client can render it in iteration order.</p>
 */
public class WorkspaceMember {

    @SerializedName(value = "userId", alternate = {"user_id"})
    private long userId;

    private String username;

    private String role;

    @SerializedName(value = "joinedAt", alternate = {"joined_at"})
    private LocalDateTime joinedAt;

    public WorkspaceMember() {}

    public long getUserId() { return userId; }
    public void setUserId(long userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public LocalDateTime getJoinedAt() { return joinedAt; }
    public void setJoinedAt(LocalDateTime joinedAt) { this.joinedAt = joinedAt; }

    public boolean isOwner() { return Workspace.ROLE_OWNER.equalsIgnoreCase(role); }

    /**
     * Adapter for places that still render {@link User} rows (e.g. the existing
     * {@code NewChatDialog} user list). Kept small on purpose — we only need
     * id + username, the other User fields (avatar/online/createdAt) are not
     * exposed by the members endpoint.
     */
    public User toUser() {
        return new User(userId, username, null);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof WorkspaceMember other)) return false;
        return userId == other.userId;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(userId);
    }
}
