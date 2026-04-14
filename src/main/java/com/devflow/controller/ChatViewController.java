package com.devflow.controller;

import com.devflow.config.AppConfig;
import com.devflow.model.Chat;
import com.devflow.model.Message;
import com.devflow.model.User;
import com.devflow.service.MessageService;
import com.devflow.view.ChatView;
import com.devflow.view.MessageBubble;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.util.Duration;

import java.util.List;

public class ChatViewController {

    private final ChatView view;
    private final MessageService messageService;
    private final Chat chat;
    private final User currentUser;
    private Timeline pollingTimeline;
    private long lastMessageId = 0;

    public ChatViewController(ChatView view, MessageService messageService, Chat chat, User currentUser) {
        this.view = view;
        this.messageService = messageService;
        this.chat = chat;
        this.currentUser = currentUser;

        User other = chat.getOtherParticipant(currentUser.getId());
        view.getHeaderName().setText(other != null ? other.getUsername() : "Chat");

        bindEvents();
        loadMessages();
    }

    private void bindEvents() {
        view.getSendButton().setOnAction(e -> sendMessage());
        view.getInputField().setOnAction(e -> sendMessage());
    }

    private void sendMessage() {
        String content = view.getInputField().getText().trim();
        if (content.isEmpty()) return;

        view.getInputField().clear();
        view.getSendButton().setDisable(true);

        messageService.sendMessage(chat.getId(), content)
                .thenAcceptAsync(message -> {
                    appendMessage(message);
                    lastMessageId = message.getId();
                    view.getSendButton().setDisable(false);
                    view.getInputField().requestFocus();
                }, Platform::runLater)
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        view.getSendButton().setDisable(false);
                        System.err.println("Failed to send message: " + ex.getMessage());
                    });
                    return null;
                });
    }

    private void loadMessages() {
        messageService.getMessages(chat.getId(), null)
                .thenAcceptAsync(messages -> {
                    view.getMessagesBox().getChildren().clear();
                    for (Message msg : messages) {
                        appendMessage(msg);
                    }
                    if (!messages.isEmpty()) {
                        lastMessageId = messages.get(messages.size() - 1).getId();
                    }
                }, Platform::runLater)
                .exceptionally(ex -> {
                    System.err.println("Failed to load messages: " + ex.getMessage());
                    return null;
                });
    }

    private void pollNewMessages() {
        Long afterId = lastMessageId > 0 ? lastMessageId : null;
        messageService.getMessages(chat.getId(), afterId)
                .thenAcceptAsync(messages -> {
                    if (!messages.isEmpty()) {
                        for (Message msg : messages) {
                            appendMessage(msg);
                        }
                        lastMessageId = messages.get(messages.size() - 1).getId();
                    }
                }, Platform::runLater)
                .exceptionally(ex -> {
                    System.err.println("Polling error: " + ex.getMessage());
                    return null;
                });
    }

    private void appendMessage(Message message) {
        boolean isOwn = message.getTransmitterId() == currentUser.getId();
        MessageBubble bubble = new MessageBubble(message, isOwn);
        view.getMessagesBox().getChildren().add(bubble);
    }

    public void startPolling() {
        stopPolling();
        pollingTimeline = new Timeline(
                new KeyFrame(Duration.millis(AppConfig.POLL_INTERVAL_MS), e -> pollNewMessages())
        );
        pollingTimeline.setCycleCount(Timeline.INDEFINITE);
        pollingTimeline.play();
    }

    public void stopPolling() {
        if (pollingTimeline != null) {
            pollingTimeline.stop();
            pollingTimeline = null;
        }
    }
}
