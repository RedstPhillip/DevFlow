package com.devflow.model;

import java.time.LocalDateTime;
import java.util.List;

public class Chat {

    private long id;
    private String type;
    private LocalDateTime createdAt;
    private List<User> participants;
    private Message lastMessage;

    public Chat() {}

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public List<User> getParticipants() { return participants; }
    public void setParticipants(List<User> participants) { this.participants = participants; }

    public Message getLastMessage() { return lastMessage; }
    public void setLastMessage(Message lastMessage) { this.lastMessage = lastMessage; }

    public User getOtherParticipant(long currentUserId) {
        if (participants == null) return null;
        return participants.stream()
                .filter(u -> u.getId() != currentUserId)
                .findFirst()
                .orElse(null);
    }
}
