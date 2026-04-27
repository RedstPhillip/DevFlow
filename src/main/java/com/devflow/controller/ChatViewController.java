package com.devflow.controller;

import com.devflow.config.AppConfig;
import com.devflow.config.WorkspaceState;
import com.devflow.model.Chat;
import com.devflow.model.Message;
import com.devflow.model.User;
import com.devflow.model.Workspace;
import com.devflow.service.MessageService;
import com.devflow.view.Avatar;
import com.devflow.view.ChatView;
import com.devflow.view.EmptyState;
import com.devflow.view.MessageBubble;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.StackPane;
import org.kordamp.ikonli.feather.Feather;
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
    private Runnable onOpenGroupChatSettings;
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

    public void setOnOpenGroupChatSettings(Runnable r) {
        this.onOpenGroupChatSettings = r;
        view.getInfoButton().setVisible(chat.isGroupChat());
        view.getInfoButton().setManaged(chat.isGroupChat());
        view.getInfoButton().setOnAction(e -> { if (onOpenGroupChatSettings != null) onOpenGroupChatSettings.run(); });
    }

    public void refreshFrom(Chat updated) {
        this.chat = updated;
        setupHeader();
    }

    private void setupHeader() {
        String displayName = chat.getDisplayName(currentUser.getId());
        Workspace workspace = WorkspaceState.getInstance().getCurrent();
        String workspaceName = workspace != null && workspace.getName() != null && !workspace.getName().isBlank()
                ? workspace.getName()
                : "Persönlich";
        view.getHeaderName().setText(displayName);

        if (chat.isGroupChat()) {
            int count = chat.getParticipants() == null ? 0 : chat.getParticipants().size();
            long online = chat.getParticipants() == null ? 0 : chat.getParticipants().stream().filter(User::isOnline).count();
            view.getAvatarHost().getChildren().setAll(buildChannelMark());
            view.getHeaderStatus().setText(count + " Mitglieder" + (online > 0 ? " - " + online + " online" : ""));
            view.setContextInfo(displayName, "Channel", count + " Mitglieder", workspaceName);
            view.getHeaderStatus().getStyleClass().remove("chat-header-status-online");
            if (!view.getHeaderStatus().getStyleClass().contains("chat-header-status")) {
                view.getHeaderStatus().getStyleClass().add("chat-header-status");
            }
        } else {
            User other = chat.getOtherParticipant(currentUser.getId());
            boolean online = other != null && other.isOnline();
            Avatar avatar = new Avatar(displayName, 40);
            view.getAvatarHost().getChildren().setAll(avatar);
            view.getHeaderStatus().setText(online ? "Online" : "Offline");
            view.setContextInfo(displayName, "Direktnachricht", "2 Personen", workspaceName);
            view.getHeaderStatus().getStyleClass().removeAll("chat-header-status", "chat-header-status-online");
            view.getHeaderStatus().getStyleClass().add(online ? "chat-header-status-online" : "chat-header-status");
        }
    }

    private StackPane buildChannelMark() {
        Label mark = new Label("#");
        mark.getStyleClass().add("channel-mark-symbol");
        StackPane host = new StackPane(mark);
        host.getStyleClass().add("channel-mark");
        host.setMinSize(40, 40);
        host.setPrefSize(40, 40);
        host.setMaxSize(40, 40);
        return host;
    }

    private void bindEvents() {
        view.getSendButton().setOnAction(e -> sendMessage());
        view.getSendButton().setDisable(true);
        view.getInputField().textProperty().addListener((obs, old, text) ->
                view.getSendButton().setDisable(text == null || text.trim().isEmpty()));
        view.getInputField().setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER && !e.isShiftDown()) {
                e.consume();
                sendMessage();
            }
        });
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
                    view.getSendButton().setDisable(view.getInputField().getText().trim().isEmpty());
                    view.getInputField().requestFocus();
                    // Always jump to bottom after sending.
                    view.scrollToBottom();
                }, Platform::runLater)
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        if (!active) return;
                        if (view.getInputField().getText().isBlank()) {
                            view.getInputField().setText(content);
                        }
                        view.getSendButton().setDisable(view.getInputField().getText().trim().isEmpty());
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
                    if (messages.isEmpty()) {
                        // Phase 3 P2: show a differentiated empty-state instead of an
                        // empty pane. Wording depends on chat type and ownership so the
                        // copy reflects "context I just walked into" vs "context I created".
                        view.getMessagesBox().getChildren().add(buildChatEmptyState());
                        return;
                    }
                    long previousSender = -1;
                    for (Message msg : messages) {
                        boolean showSender = chat.isGroupChat() && msg.getTransmitterId() != previousSender
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

    /**
     * Build the "no messages yet" placeholder for the current chat. Wording is
     * differentiated per the audit's P2 acceptance criteria:
     *   – DM:                 "Das ist der Anfang deiner Unterhaltung mit X"
     *   – Group I created:    "Du hast diese Gruppe erstellt …"
     *   – Group I joined:     "Du bist dieser Gruppe beigetreten …"
     */
    private EmptyState buildChatEmptyState() {
        String title;
        String subtitle;
        if (chat.isGroupChat()) {
            boolean ownedByMe = chat.getOwnerId() != null && chat.getOwnerId() == currentUser.getId();
            String name = chat.getDisplayName(currentUser.getId());
            if (ownedByMe) {
                title = "Du hast diese Gruppe erstellt";
                subtitle = "Sende die erste Nachricht in „" + name + "“ um die Unterhaltung zu starten.";
            } else {
                title = "Du bist dieser Gruppe beigetreten";
                subtitle = "Schreibe eine Nachricht in „" + name + "“ — alle Mitglieder sehen sie.";
            }
        } else {
            User other = chat.getOtherParticipant(currentUser.getId());
            String name = other != null ? other.getUsername() : "diesem Kontakt";
            title = "Das ist der Anfang deiner Unterhaltung mit " + name;
            subtitle = "Sage Hallo — deine Nachricht ist nur für euch beide sichtbar.";
        }
        return new EmptyState(Feather.MESSAGE_CIRCLE, title, subtitle);
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
        appendMessage(message, chat.isGroupChat() && message.getTransmitterId() != currentUser.getId());
    }

    private void appendMessage(Message message, boolean showSender) {
        // If the empty-state placeholder is up, drop it before adding the
        // first real bubble — otherwise the placeholder would push the bubble
        // off-frame on the first send.
        var children = view.getMessagesBox().getChildren();
        if (!children.isEmpty() && children.get(0) instanceof EmptyState) {
            children.clear();
        }
        boolean isOwn = message.getTransmitterId() == currentUser.getId();
        User sender = findUser(message.getTransmitterId());
        MessageBubble bubble = new MessageBubble(message, isOwn, sender, showSender);
        children.add(bubble);
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
