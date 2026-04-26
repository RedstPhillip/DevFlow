package com.devflow.model;

import com.google.gson.annotations.SerializedName;

import java.time.LocalDateTime;

/**
 * Client-side POJO for a workspace as returned by {@code /api/workspaces/*}.
 *
 * <p>Every user has at least one workspace: the "Persoenlich" one created
 * automatically at registration ({@link #isPersonal} = {@code true}). It can
 * be renamed but not deleted/left. All other workspaces are either created
 * via {@code POST /api/workspaces} or joined via
 * {@code POST /api/workspaces/join} with an 8-char invite code.</p>
 *
 * <p>Role values come from the backend as {@code "OWNER"} or {@code "MEMBER"}
 * (no ADMIN in the 2b MVP).</p>
 */
public class Workspace {

    public static final String ROLE_OWNER = "OWNER";
    public static final String ROLE_MEMBER = "MEMBER";

    private long id;
    private String name;

    @SerializedName(value = "ownerId", alternate = {"owner_id"})
    private Long ownerId;

    @SerializedName(value = "inviteCode", alternate = {"invite_code"})
    private String inviteCode;

    @SerializedName(value = "isPersonal", alternate = {"is_personal"})
    private boolean isPersonal;

    @SerializedName(value = "createdAt", alternate = {"created_at"})
    private LocalDateTime createdAt;

    @SerializedName(value = "memberCount", alternate = {"member_count"})
    private int memberCount;

    private String role;

    public Workspace() {}

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Long getOwnerId() { return ownerId; }
    public void setOwnerId(Long ownerId) { this.ownerId = ownerId; }

    public String getInviteCode() { return inviteCode; }
    public void setInviteCode(String inviteCode) { this.inviteCode = inviteCode; }

    public boolean isPersonal() { return isPersonal; }
    public void setPersonal(boolean personal) { this.isPersonal = personal; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public int getMemberCount() { return memberCount; }
    public void setMemberCount(int memberCount) { this.memberCount = memberCount; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public boolean isOwner() { return ROLE_OWNER.equalsIgnoreCase(role); }

    @Override
    public String toString() { return name; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Workspace other)) return false;
        return id == other.id;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(id);
    }
}
