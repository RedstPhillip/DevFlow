package com.devflow.model;

import java.time.LocalDateTime;

public class Message {

    private long id;
    private long chatId;
    private long transmitterId;
    private String content;
    private LocalDateTime createdAt;

    public Message() {}

    public Message(long id, long chatId, long transmitterId, String content, LocalDateTime createdAt) {
        this.id = id;
        this.chatId = chatId;
        this.transmitterId = transmitterId;
        this.content = content;
        this.createdAt = createdAt;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getChatId() { return chatId; }
    public void setChatId(long chatId) { this.chatId = chatId; }

    public long getTransmitterId() { return transmitterId; }
    public void setTransmitterId(long transmitterId) { this.transmitterId = transmitterId; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
