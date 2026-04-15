package com.devflow.model;

import java.time.LocalDateTime;
import java.util.List;

public class Chat {

    public static final String TYPE_DM = "DM";
    public static final String TYPE_GROUP = "GROUP";

    public static final String POLICY_OWNER_ONLY = "OWNER_ONLY";
    public static final String POLICY_ALL_MEMBERS = "ALL_MEMBERS";

    private long id;
    private String type;
    private String name;
    private Long ownerId;
    private String memberAddPolicy;
    private LocalDateTime createdAt;
    private List<User> participants;
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

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public List<User> getParticipants() { return participants; }
    public void setParticipants(List<User> participants) { this.participants = participants; }

    public Message getLastMessage() { return lastMessage; }
    public void setLastMessage(Message lastMessage) { this.lastMessage = lastMessage; }

    public boolean isGroup() { return TYPE_GROUP.equalsIgnoreCase(type); }

    public User getOtherParticipant(long currentUserId) {
        if (participants == null) return null;
        return participants.stream()
                .filter(u -> u.getId() != currentUserId)
                .findFirst()
                .orElse(null);
    }

    public String getDisplayName(long currentUserId) {
        if (isGroup()) {
            if (name != null && !name.isBlank()) return name;
            if (participants == null || participants.isEmpty()) return "Gruppe";
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
        if (!isGroup()) return false;
        if (POLICY_ALL_MEMBERS.equalsIgnoreCase(memberAddPolicy)) return true;
        return ownerId != null && ownerId == userId;
    }
}
