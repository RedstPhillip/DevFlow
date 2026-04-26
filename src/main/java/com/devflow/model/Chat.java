package com.devflow.model;

import com.google.gson.annotations.SerializedName;

import java.time.LocalDateTime;
import java.util.List;

public class Chat {

    public static final String TYPE_DM = "DM";
    // TODO rename on the wire to "GROUP_CHAT" once the "Gruppe" (folder/workspace-group)
    // concept is stable and we coordinate a client+server migration. Until then
    // the serialized value stays "GROUP" to remain compatible with the existing backend.
    public static final String TYPE_GROUP = "GROUP";

    public static final String POLICY_OWNER_ONLY = "OWNER_ONLY";
    public static final String POLICY_ALL_MEMBERS = "ALL_MEMBERS";

    private long id;
    private String type;
    private String name;

    @SerializedName(value = "ownerId", alternate = {"owner_id"})
    private Long ownerId;

    @SerializedName(value = "memberAddPolicy", alternate = {"member_add_policy"})
    private String memberAddPolicy;

    /**
     * Workspace this GROUP chat belongs to. Populated by the backend from
     * Phase 2b onward; {@code null} for DMs (the backend uses
     * {@code @JsonInclude(NON_NULL)} so DM payloads omit the field entirely).
     * Used client-side to filter the "Gruppenchats" section by the current
     * workspace.
     */
    @SerializedName(value = "workspaceId", alternate = {"workspace_id"})
    private Long workspaceId;

    /**
     * Workspace-group (folder) this GROUP chat is filed under. Populated by
     * the backend from Phase 2c onward. {@code null} for DMs and also
     * {@code null} for group chats sitting directly under the workspace —
     * those render in the implicit "Allgemein" section of the sidebar. Pre-2c
     * legacy chats always have this field null.
     */
    @SerializedName(value = "groupId", alternate = {"group_id"})
    private Long groupId;

    @SerializedName(value = "createdAt", alternate = {"created_at"})
    private LocalDateTime createdAt;

    private List<User> participants;

    @SerializedName(value = "lastMessage", alternate = {"last_message"})
    private Message lastMessage;

    public Chat() {}

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Long getOwnerId() { return ownerId; }
    public void setOwnerId(Long ownerId) { this.ownerId = ownerId; }

    public String getMemberAddPolicy() { return memberAddPolicy; }
    public void setMemberAddPolicy(String memberAddPolicy) { this.memberAddPolicy = memberAddPolicy; }

    public Long getWorkspaceId() { return workspaceId; }
    public void setWorkspaceId(Long workspaceId) { this.workspaceId = workspaceId; }

    public Long getGroupId() { return groupId; }
    public void setGroupId(Long groupId) { this.groupId = groupId; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public List<User> getParticipants() { return participants; }
    public void setParticipants(List<User> participants) { this.participants = participants; }

    public Message getLastMessage() { return lastMessage; }
    public void setLastMessage(Message lastMessage) { this.lastMessage = lastMessage; }

    public boolean isGroupChat() { return TYPE_GROUP.equalsIgnoreCase(type); }

    public User getOtherParticipant(long currentUserId) {
        if (participants == null) return null;
        return participants.stream()
                .filter(u -> u.getId() != currentUserId)
                .findFirst()
                .orElse(null);
    }

    public String getDisplayName(long currentUserId) {
        if (isGroupChat()) {
            if (name != null && !name.isBlank()) return name;
            if (participants == null || participants.isEmpty()) return "Gruppenchat";
            StringBuilder sb = new StringBuilder();
            int count = 0;
            for (User u : participants) {
                if (u.getId() == currentUserId) continue;
                if (count > 0) sb.append(", ");
                sb.append(u.getUsername());
                count++;
                if (count >= 3) break;
            }
            int remaining = (participants.size() - 1) - count;
            if (remaining > 0) sb.append(" +").append(remaining);
            return sb.toString();
        }
        User other = getOtherParticipant(currentUserId);
        return other != null ? other.getUsername() : "Unbekannt";
    }

    public boolean canAddMembers(long userId) {
        if (!isGroupChat()) return false;
        if (POLICY_ALL_MEMBERS.equalsIgnoreCase(memberAddPolicy)) return true;
        return ownerId != null && ownerId == userId;
    }
}
