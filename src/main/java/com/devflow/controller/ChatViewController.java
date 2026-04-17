package com.devflow.controller;

import com.devflow.config.AppConfig;
import com.devflow.model.Chat;
import com.devflow.model.Message;
import com.devflow.model.User;
import com.devflow.service.MessageService;
import com.devflow.view.Avatar;
import com.devflow.view.ChatView;
import com.devflow.view.MessageBubble;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.util.Duration;

public class ChatViewController {

    private final ChatView view;
    private final MessageService messageService;
    private Chat chat;
    private final User currentUser;
    private Timeline pollingTimeline;
    private long lastMessageId = 0;
    private Runnable onOpenGroupSettings;
    /** True between construction and dispose(); guards async callbacks against a detached view. */
    private volatile boolean active = true;

    public ChatViewController(ChatView view, MessageService messageService, Chat chat, User currentUser) {
        this.view = view;
        this.messageService = messageService;
        this.chat = chat;
        this.currentUser = currentUser;

        setupHeader();
        bindEvents();
        loadMessages();
    }

    public void setOnOpenGroupSettings(Runnable r) {
        this.onOpenGroupSettings = r;
        view.getInfoButton().setVisible(chat.isGroup());
        view.getInfoButton().setManaged(chat.isGroup());
        view.getInfoButton().setOnAction(e -> { if (onOpenGroupSettings != null) onOpenGroupSettings.run(); });
    }

    public void refreshFrom(Chat updated) {
        this.chat = updated;
        setupHeader();
    }

    private void setupHeader() {
        String displayName = chat.getDisplayName(currentUser.getId());
        view.getHeaderName().setText(displayName);

        Avatar avatar = new Avatar(displayName, 40);
        view.getAvatarHost().getChildren().setAll(avatar);

        if (chat.isGroup()) {
            int count = chat.getParticipants() == null ? 0 : chat.getParticipants().size();
            view.getHeaderStatus().setText(count + " Mitglieder");
            view.getHeaderStatus().getStyleClass().remove("chat-header-status-online");
            if (!view.getHeaderStatus().getStyleClass().contains("chat-header-status")) {
                view.getHeaderStatus().getStyleClass().add("chat-header-status");
            }
        } else {
            User other = chat.getOtherParticipant(currentUser.getId());
            boolean online = other != null && other.isOnline();
            view.getHeaderStatus().setText(online ? "Online" : "Offline");
            view.getHeaderStatus().getStyleClass().removeAll("chat-header-status", "chat-header-status-online");
            view.getHeaderStatus().getStyleClass().add(online ? "chat-header-status-online" : "chat-header-status");
        }
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
                    if (!active) return;
                    // Defensive: some servers don't echo transmitterId on POST. We know
                    // who sent it — fill it in so the bubble renders on the right side.
                    if (message.getTransmitterId() == 0) {
                        message.setTransmitterId(currentUser.getId());
                    }
                    appendMessage(message);
                    if (message.getId() > 0) lastMessageId = message.getId();
                    view.getSendButton().setDisable(false);
                    view.getInputField().requestFocus();
                    // Always jump to bottom after sending.
                    view.scrollToBottom();
                }, Platform::runLater)
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        if (!active) return;
                        view.getSendButton().setDisable(false);
                        System.err.println("Failed to send message: " + ex.getMessage());
                    });
                    return null;
                });
    }

    private void loadMessages() {
        messageService.getMessages(chat.getId(), null)
                .thenAcceptAsync(messages -> {
                    if (!active) return;
                    view.getMessagesBox().getChildren().clear();
                    long previousSender = -1;
                    for (Message msg : messages) {
                        boolean showSender = chat.isGroup() && msg.getTransmitterId() != previousSender
                                && msg.getTransmitterId() != currentUser.getId();
                        appendMessage(msg, showSender);
                        previousSender = msg.getTransmitterId();
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
                    if (!active) return;
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
        appendMessage(message, chat.isGroup() && message.getTransmitterId() != currentUser.getId());
    }

    private void appendMessage(Message message, boolean showSender) {
        boolean isOwn = message.getTransmitterId() == currentUser.getId();
        User sender = findUser(message.getTransmitterId());
        MessageBubble bubble = new MessageBubble(message, isOwn, sender, showSender);
        view.getMessagesBox().getChildren().add(bubble);
    }

    private User findUser(long id) {
        if (chat.getParticipants() == null) return null;
        for (User u : chat.getParticipants()) if (u.getId() == id) return u;
        return null;
    }

    public Chat getChat() { return chat; }

    public void startPolling() {
        stopPolling();
        if (!active) return;
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

    /**
     * Called by MainController when this view is being torn down. After this,
     * any in-flight async callback will be a no-op so it can't mutate the
     * (potentially detached) view or fire ghost selections.
     */
    public void dispose() {
        active = false;
        stopPolling();
    }
}
