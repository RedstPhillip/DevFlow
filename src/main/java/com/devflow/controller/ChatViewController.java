package com.devflow.controller;

import com.devflow.config.AppConfig;
import com.devflow.config.WorkspaceState;
import com.devflow.model.Chat;
import com.devflow.model.Message;
import com.devflow.model.User;
import com.devflow.model.Workspace;
import com.devflow.service.MessageService;
import com.devflow.service.UserService;
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

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ChatViewController {

    private static final long MAX_POLL_BACKOFF_MS = 30_000;

    private final ChatView view;
    private final MessageService messageService;
    private final UserService userService;
    private Chat chat;
    private final User currentUser;
    private Timeline pollingTimeline;
    private long lastMessageId = 0;
    private long nextOptimisticMessageId = -1;
    private final Set<Long> renderedMessageIds = new HashSet<>();
    private final Map<String, Integer> pendingOptimisticMessages = new HashMap<>();
    private int consecutivePollingFailures = 0;
    private long nextPollAttemptAtMs = 0;
    private String lastPollingErrorSignature = "";
    private Runnable onOpenGroupChatSettings;
    /** True between construction and dispose(); guards async callbacks against a detached view. */
    private volatile boolean active = true;

    public ChatViewController(ChatView view, MessageService messageService, UserService userService, Chat chat, User currentUser) {
        this.view = view;
        this.messageService = messageService;
        this.userService = userService;
        this.chat = chat;
        this.currentUser = currentUser;

        setupHeader();
        refreshPresence();
        bindEvents();
        loadMessages();
    }

    public void setOnOpenGroupChatSettings(Runnable r) {
        this.onOpenGroupChatSettings = r;
        view.getSettingsButton().setVisible(chat.isGroupChat());
        view.getSettingsButton().setManaged(chat.isGroupChat());
        view.getSettingsButton().setOnAction(e -> { if (onOpenGroupChatSettings != null) onOpenGroupChatSettings.run(); });
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
        if (workspace == null || workspace.getId() <= 0) workspaceName = "Kein Workspace";
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

        Message optimistic = new Message(nextOptimisticMessageId--, chat.getId(), currentUser.getId(),
                content, LocalDateTime.now());
        String pendingKey = pendingKey(optimistic);
        pendingOptimisticMessages.merge(pendingKey, 1, Integer::sum);
        MessageBubble optimisticBubble = appendMessage(optimistic);
        view.scrollToBottom();

        messageService.sendMessage(chat.getId(), content)
                .thenAcceptAsync(message -> {
                    if (!active) return;
                    // Defensive: some servers don't echo transmitterId on POST. We know
                    // who sent it — fill it in so the bubble renders on the right side.
                    if (message.getTransmitterId() == 0) {
                        message.setTransmitterId(currentUser.getId());
                    }
                    if (message.getId() > 0) renderedMessageIds.add(message.getId());
                    decrementPending(pendingKey);
                    if (message.getId() > 0) lastMessageId = message.getId();
                    view.getSendButton().setDisable(view.getInputField().getText().trim().isEmpty());
                    view.getInputField().requestFocus();
                    // Always jump to bottom after sending.
                    view.scrollToBottom();
                }, Platform::runLater)
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        if (!active) return;
                        decrementPending(pendingKey);
                        if (optimisticBubble != null) {
                            view.getMessagesBox().getChildren().remove(optimisticBubble);
                        }
                        if (view.getMessagesBox().getChildren().isEmpty()) {
                            view.getMessagesBox().getChildren().add(buildChatEmptyState());
                        }
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
                    renderedMessageIds.clear();
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
        long now = System.currentTimeMillis();
        if (now < nextPollAttemptAtMs) {
            return;
        }
        Long afterId = lastMessageId > 0 ? lastMessageId : null;
        messageService.getMessages(chat.getId(), afterId)
                .thenAcceptAsync(messages -> {
                    if (!active) return;
                    resetPollingBackoff();
                    if (!messages.isEmpty()) {
                        for (Message msg : messages) {
                            appendMessage(msg);
                        }
                        lastMessageId = messages.get(messages.size() - 1).getId();
                    }
                }, Platform::runLater)
                .exceptionally(ex -> {
                    handlePollingError(ex);
                    return null;
                });
    }

    private void handlePollingError(Throwable ex) {
        if (!active) return;
        Throwable root = rootCause(ex);
        String signature = root.getClass().getSimpleName() + ": " + safeMessage(root);
        consecutivePollingFailures++;
        long delay = Math.min(MAX_POLL_BACKOFF_MS,
                AppConfig.POLL_INTERVAL_MS * (1L << Math.min(4, consecutivePollingFailures - 1)));
        nextPollAttemptAtMs = System.currentTimeMillis() + delay;

        if (consecutivePollingFailures == 1 || !signature.equals(lastPollingErrorSignature)) {
            System.err.println("Polling paused: " + signature + " (retry in " + (delay / 1000) + "s)");
            lastPollingErrorSignature = signature;
        }
    }

    private void resetPollingBackoff() {
        if (consecutivePollingFailures > 0) {
            System.err.println("Polling recovered.");
        }
        consecutivePollingFailures = 0;
        nextPollAttemptAtMs = 0;
        lastPollingErrorSignature = "";
    }

    private Throwable rootCause(Throwable ex) {
        Throwable root = ex;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        return root;
    }

    private String safeMessage(Throwable ex) {
        String message = ex.getMessage();
        return message == null || message.isBlank() ? "no details" : message;
    }

    private MessageBubble appendMessage(Message message) {
        return appendMessage(message, chat.isGroupChat() && message.getTransmitterId() != currentUser.getId());
    }

    private MessageBubble appendMessage(Message message, boolean showSender) {
        if (message.getId() > 0 && renderedMessageIds.contains(message.getId())) {
            return null;
        }
        if (message.getId() > 0
                && message.getTransmitterId() == currentUser.getId()
                && hasPending(pendingKey(message))) {
            renderedMessageIds.add(message.getId());
            decrementPending(pendingKey(message));
            return null;
        }
        if (message.getId() > 0) {
            renderedMessageIds.add(message.getId());
        }
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
        return bubble;
    }

    private String pendingKey(Message message) {
        String content = message.getContent() == null ? "" : message.getContent();
        long keyChatId = message.getChatId() > 0 ? message.getChatId() : chat.getId();
        return keyChatId + ":" + message.getTransmitterId() + ":" + content;
    }

    private boolean hasPending(String key) {
        return pendingOptimisticMessages.getOrDefault(key, 0) > 0;
    }

    private void decrementPending(String key) {
        Integer count = pendingOptimisticMessages.get(key);
        if (count == null) return;
        if (count <= 1) {
            pendingOptimisticMessages.remove(key);
        } else {
            pendingOptimisticMessages.put(key, count - 1);
        }
    }

    private User findUser(long id) {
        if (chat.getParticipants() == null) return null;
        for (User u : chat.getParticipants()) if (u.getId() == id) return u;
        return null;
    }

    public Chat getChat() { return chat; }

    public String getChatTitle() {
        return chat == null ? "Workspace" : chat.getDisplayName(currentUser.getId());
    }

    private void refreshPresence() {
        if (userService == null || chat.getParticipants() == null || chat.getParticipants().isEmpty()) return;
        userService.listUsers()
                .thenAcceptAsync(users -> {
                    if (!active || users == null) return;
                    for (User participant : chat.getParticipants()) {
                        for (User fresh : users) {
                            if (fresh.getId() == participant.getId()) {
                                participant.setOnline(fresh.isOnline());
                                break;
                            }
                        }
                    }
                    setupHeader();
                }, Platform::runLater)
                .exceptionally(ex -> null);
    }

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
