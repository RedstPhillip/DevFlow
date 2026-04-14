package com.devflow.controller;

import com.devflow.config.AppConfig;
import com.devflow.service.ChatService;
import com.devflow.view.ChatListView;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.util.Duration;

public class ChatListController {

    private final ChatListView view;
    private final ChatService chatService;
    private final MainController mainController;
    private Timeline refreshTimeline;

    public ChatListController(ChatListView view, ChatService chatService, MainController mainController) {
        this.view = view;
        this.chatService = chatService;
        this.mainController = mainController;

        view.setCurrentUserId(mainController.getCurrentUser().getId());
        view.setOnChatSelected(mainController::openChat);

        loadChats();
    }

    private void loadChats() {
        chatService.listMyChats()
                .thenAcceptAsync(chats -> view.setChats(chats), Platform::runLater)
                .exceptionally(ex -> {
                    System.err.println("Failed to load chats: " + ex.getMessage());
                    return null;
                });
    }

    public void startRefresh() {
        stopRefresh();
        refreshTimeline = new Timeline(
                new KeyFrame(Duration.millis(AppConfig.CHAT_LIST_REFRESH_MS), e -> loadChats())
        );
        refreshTimeline.setCycleCount(Timeline.INDEFINITE);
        refreshTimeline.play();
    }

    public void stopRefresh() {
        if (refreshTimeline != null) {
            refreshTimeline.stop();
            refreshTimeline = null;
        }
    }
}
