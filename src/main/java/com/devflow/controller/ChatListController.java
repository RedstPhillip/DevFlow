package com.devflow.controller;

import com.devflow.config.AppConfig;
import com.devflow.config.GroupState;
import com.devflow.config.WorkspaceState;
import com.devflow.model.Group;
import com.devflow.model.Workspace;
import com.devflow.service.ChatService;
import com.devflow.view.ChatListView;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.util.Duration;

import java.util.List;
import java.util.function.Consumer;

public class ChatListController {

    private final ChatListView view;
    private final ChatService chatService;
    private Timeline refreshTimeline;

    /**
     * Held so we can detach on {@link #dispose()}. Registering the listener
     * directly as a lambda would leak it across logout/login cycles because
     * {@link WorkspaceState} is a singleton and holds references forever.
     */
    private final Consumer<Workspace> workspaceListener;
    /** Same lifecycle as {@link #workspaceListener} — detach on dispose. */
    private final Consumer<List<Group>> groupListener;

    public ChatListController(ChatListView view, ChatService chatService, MainController mainController) {
        this.view = view;
        this.chatService = chatService;

        view.setCurrentUserId(mainController.getCurrentUser().getId());
        view.setOnChatSelected(mainController::openChat);

        // Seed the view's workspace filter + groups cache from current state so
        // the first loadChats() already shows the right Gruppenchats subset
        // and the right folder tree. Subsequent changes flow through the two
        // listeners registered below.
        Workspace initial = WorkspaceState.getInstance().getCurrent();
        long initialWsId = initial != null ? initial.getId() : 0L;
        view.setCurrentWorkspaceId(initialWsId);
        view.setGroups(GroupState.getInstance().getGroups());

        this.workspaceListener = ws -> Platform.runLater(() -> {
            // WorkspaceState fires on whatever thread called setCurrent — hop
            // to FX before touching the view. Re-fetching chats here means a
            // freshly joined workspace's group chats appear immediately
            // without waiting for the 10 s poll tick.
            long wsId = ws != null ? ws.getId() : 0L;
            view.setCurrentWorkspaceId(wsId);
            // Reload the group tree for this workspace. GroupState handles the
            // placeholder (-1) and the 0 bootstrap state by clearing the cache.
            GroupState.getInstance().loadFor(wsId);
            loadChats();
        });
        WorkspaceState.getInstance().addListener(workspaceListener);

        this.groupListener = groups -> {
            // GroupState notifies on FX already; keep the call simple.
            view.setGroups(groups);
        };
        GroupState.getInstance().addListener(groupListener);

        // Kick off initial group-load for the seeded workspace. For new logins
        // with a valid workspace this populates the tree before the first chat
        // response lands.
        if (initialWsId != 0L) GroupState.getInstance().loadFor(initialWsId);

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

    /**
     * Detach from {@link WorkspaceState} and stop polling. Called by
     * {@link MainController} on logout so the singleton doesn't accumulate
     * stale listeners across login cycles.
     */
    public void dispose() {
        stopRefresh();
        WorkspaceState.getInstance().removeListener(workspaceListener);
        GroupState.getInstance().removeListener(groupListener);
    }
}
