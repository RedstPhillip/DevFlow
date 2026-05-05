package com.devflow.controller;

import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;

import com.devflow.config.ThemeManager;
import com.devflow.config.TokenStore;
import com.devflow.config.WorkspaceState;
import com.devflow.model.Chat;
import com.devflow.model.User;
import com.devflow.platform.PlatformWindowStyle;
import com.devflow.service.AuthService;
import com.devflow.service.ChatService;
import com.devflow.service.MessageService;
import com.devflow.service.UserService;
import com.devflow.service.WorkspaceService;
import com.devflow.view.ChatListView;
import com.devflow.view.ChatView;
import com.devflow.view.CustomTitleBar;
import com.devflow.view.GroupChatSettingsDialog;
import com.devflow.view.JoinWorkspaceDialog;
import com.devflow.view.LoginView;
import com.devflow.view.MainLayout;
import com.devflow.view.NewChatDialog;
import com.devflow.view.NewWorkspaceDialog;
import com.devflow.view.ProfileDialog;
import com.devflow.view.SettingsView;
import com.devflow.view.Sidebar;
import com.devflow.view.WorkspaceSettingsDialog;

import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

public class MainController {

    private final Stage stage;
    private Scene scene;
    private MainLayout mainLayout;

    private ChatListController chatListController;
    private ChatViewController chatViewController;
    @SuppressWarnings("unused")
    private LoginController loginController;

    private ChatListView chatListView;
    private SettingsView settingsView;
    private boolean settingsVisible = false;

    /** Held reference so we can remove it on the next login/logout cycle and avoid leak accumulation. */
    private javafx.beans.value.ChangeListener<Number> loginMaximizeListener;
    /** Held reference so we can dispose its ConnectionState listener on the next transition. */
    private CustomTitleBar loginTitleBar;

    private final AuthService authService;
    private final UserService userService;
    private final ChatService chatService;
    private final MessageService messageService;
    private final WorkspaceService workspaceService;

    public MainController(Stage stage) {
        this.stage = stage;
        this.authService = new AuthService();
        this.userService = new UserService();
        this.chatService = new ChatService();
        this.messageService = new MessageService();
        this.workspaceService = new WorkspaceService();
    }

    public void start() {
        if (TokenStore.getInstance().hasAuthToken()) {
            hydrateCurrentUserAndShowMainLayout();
        } else {
            showLogin();
        }
    }

    private void hydrateCurrentUserAndShowMainLayout() {
        var token = TokenStore.getInstance().getAuthToken();
        User cachedUser = token != null ? token.getUser() : null;
        if (cachedUser != null && cachedUser.getUsername() != null && !cachedUser.getUsername().isBlank()) {
            showMainLayout();
            return;
        }

        userService.getMe()
                .thenAccept(user -> {
                    TokenStore.getInstance().updateUser(user);
                    Platform.runLater(this::showMainLayout);
                })
                .exceptionally(ex -> {
                    System.err.println("Failed to restore current user: " + ex.getMessage());
                    Platform.runLater(this::showMainLayout);
                    return null;
                });
    }

    public void showLogin() {
        stopAllPolling();
        // Symmetric to showMainLayout(): release the Stage listeners the
        // outgoing MainLayout registered, otherwise a logout-then-relogin
        // cycle accumulates them on the singleton Stage.
        if (mainLayout != null) {
            mainLayout.dispose();
            mainLayout = null;
        }
        LoginView loginView = new LoginView();
        loginController = new LoginController(loginView, authService, this);

        // Dispose any previous login title bar so its ConnectionState
        // listener is detached before we replace it.
        if (loginTitleBar != null) {
            loginTitleBar.dispose();
            loginTitleBar = null;
        }
        CustomTitleBar titleBar = new CustomTitleBar(stage, "DevFlow");
        loginTitleBar = titleBar;
        VBox frame = new VBox(titleBar, loginView);
        VBox.setVgrow(loginView, Priority.ALWAYS);
        frame.getStyleClass().addAll("login-shell", "window-frame");

        StackPane outer = new StackPane(frame);
        outer.getStyleClass().add("main-layout");
        if (PlatformWindowStyle.usesOpaqueFramelessWindow()) {
            outer.getStyleClass().add("native-frameless");
            frame.getStyleClass().add("native-frameless");
        }
        attachLoginMaximizeListener(outer, frame);

        setScene(outer, "/styles/login.css");
    }

    private void attachLoginMaximizeListener(StackPane outer, VBox frame) {
        // Detach any previous instance — without this, every login/logout cycle leaks four listeners on stage.
        detachLoginMaximizeListener();
        javafx.beans.value.ChangeListener<Number> listener = (obs, o, n) -> {
            var bounds = javafx.stage.Screen.getPrimary().getVisualBounds();
            boolean maximized = Math.abs(stage.getWidth() - bounds.getWidth()) < 2
                    && Math.abs(stage.getHeight() - bounds.getHeight()) < 2
                    && Math.abs(stage.getX() - bounds.getMinX()) < 2
                    && Math.abs(stage.getY() - bounds.getMinY()) < 2;
            if (maximized) {
                if (!outer.getStyleClass().contains("maximized")) outer.getStyleClass().add("maximized");
                if (!frame.getStyleClass().contains("maximized")) frame.getStyleClass().add("maximized");
            } else {
                outer.getStyleClass().remove("maximized");
                frame.getStyleClass().remove("maximized");
            }
        };
        loginMaximizeListener = listener;
        stage.xProperty().addListener(listener);
        stage.yProperty().addListener(listener);
        stage.widthProperty().addListener(listener);
        stage.heightProperty().addListener(listener);
        Platform.runLater(() -> listener.changed(null, 0, 0));
    }

    private void detachLoginMaximizeListener() {
        if (loginMaximizeListener == null) return;
        stage.xProperty().removeListener(loginMaximizeListener);
        stage.yProperty().removeListener(loginMaximizeListener);
        stage.widthProperty().removeListener(loginMaximizeListener);
        stage.heightProperty().removeListener(loginMaximizeListener);
        loginMaximizeListener = null;
    }

    public void showMainLayout() {
        // Idempotent: dispose any existing layout before replacing so the
        // four Stage-property listeners it registered are detached. Without
        // this every logout→login cycle leaks one quad of listeners on the
        // long-lived Stage. stopAllPolling() also tears down the controller
        // listeners on WorkspaceState/GroupState (their dispose() calls).
        stopAllPolling();
        detachLoginMaximizeListener();
        if (mainLayout != null) {
            mainLayout.dispose();
            mainLayout = null;
        }
        // The login title bar is replaced by MainLayout's own — release its
        // ConnectionState listener now so the singleton stays clean.
        if (loginTitleBar != null) {
            loginTitleBar.dispose();
            loginTitleBar = null;
        }

        CustomTitleBar titleBar = new CustomTitleBar(stage, "DevFlow");

        Sidebar sidebar = new Sidebar();
        sidebar.setCurrentUser(getCurrentUser());

        mainLayout = new MainLayout(titleBar, sidebar, stage);

        chatListView = new ChatListView();
        chatListController = new ChatListController(chatListView, chatService, this);
        sidebar.setListNode(chatListView);

        sidebar.setOnChatsClick(() -> {
            chatListController.startRefresh();
            showWelcomeContent();
        });
        sidebar.setOnSettingsClick(this::showSettings);
        sidebar.setOnNewChat(this::openNewChatDialog);
        sidebar.setOnProfileClick(this::openProfileDialog);
        sidebar.setOnSearch(query -> {
            if (chatListView != null) chatListView.setFilter(query);
        });
        sidebar.setWorkspaceActions(this::openNewWorkspaceDialog, this::openJoinWorkspaceDialog);
        sidebar.setOnWorkspaceSettings(this::openWorkspaceSettingsDialog);

        sidebar.setActive(Sidebar.RailKey.CHATS);
        chatListController.startRefresh();

        showWelcomeContent();
        updateTitleSubtitle(hasWorkspace ? "Workspace" : "Direktnachrichten");

        setScene(mainLayout, "/styles/chat.css");
    }

    public void showSettings() {
        if (chatViewController != null) {
            chatViewController.dispose();
            chatViewController = null;
        }
        if (chatListView != null) chatListView.clearSelection();
        if (settingsView == null) {
            settingsView = new SettingsView();
            settingsView.setOnLogout(this::logout);
        }
        settingsVisible = true;
        mainLayout.getSidebar().setActive(Sidebar.RailKey.SETTINGS);
        updateTitleSubtitle("Einstellungen");
        mainLayout.setMainContent(settingsView);
        settingsView.scrollToTop();
    }

    public void openChat(Chat chat) {
        if (chatViewController != null) {
            chatViewController.dispose();
            chatViewController = null;
        }
        settingsVisible = false;
        if (chatListView != null) chatListView.selectChat(chat);
        // Make sure the rail/panel are in Chats mode (e.g. when opening a chat from Settings)
        mainLayout.getSidebar().setActive(Sidebar.RailKey.CHATS);
        ChatView chatView = new ChatView();
        User currentUser = getCurrentUser();
        chatViewController = new ChatViewController(chatView, messageService, userService, chat, currentUser);
        if (chat.isGroupChat()) {
            chatViewController.setOnOpenGroupChatSettings(() -> openGroupChatSettings(chat));
        }
        updateTitleSubtitle(chat.getDisplayName(currentUser.getId()));
        mainLayout.setMainContent(chatView);
        chatViewController.startPolling();
    }

    public void handleWorkspaceChanged(com.devflow.model.Workspace workspace) {
        settingsView = null;
        if (chatViewController != null) {
            Chat open = chatViewController.getChat();
            long workspaceId = workspace != null ? workspace.getId() : 0L;
            boolean staleGroupChat = open != null
                    && open.isGroupChat()
                    && (workspaceId <= 0 || open.getWorkspaceId() == null || open.getWorkspaceId() != workspaceId);
            if (staleGroupChat) {
                showWelcomeContent();
                return;
            }
        }
        if (chatViewController == null && !settingsVisible) {
            showWelcomeContent();
            return;
        }
        updateTitleSubtitle(chatViewController != null ? chatViewController.getChatTitle() : "Workspace");
    }

    private void openGroupChatSettings(Chat chat) {
        GroupChatSettingsDialog dialog = new GroupChatSettingsDialog(
                mainLayout.getModalHost(),
                mainLayout.getBlurTarget(),
                chatService, userService,
                chat, getCurrentUser().getId(),
                updated -> {
                    if (updated == null) {
                        // User left / group chat dissolved
                        chatListController.startRefresh();
                        showWelcomeContent();
                    } else {
                        if (chatViewController != null && chatViewController.getChat().getId() == updated.getId()) {
                            chatViewController.refreshFrom(updated);
                        }
                        chatListController.startRefresh();
                    }
                });
        dialog.show();
    }

    private void openNewWorkspaceDialog() {
        NewWorkspaceDialog dialog = new NewWorkspaceDialog(
                mainLayout.getModalHost(),
                mainLayout.getBlurTarget(),
                workspaceService,
                created -> {
                    // Refresh the sidebar's cache so the new workspace shows
                    // up in the switcher menu, then adopt it as current —
                    // this cascades through WorkspaceState listeners
                    // (ChatListController re-fetches, sidebar header updates).
                    WorkspaceState.getInstance().setCurrent(created);
                    mainLayout.getSidebar().refreshWorkspaces();
                });
        dialog.show();
    }

    private void openWorkspaceSettingsDialog() {
        com.devflow.model.Workspace current = WorkspaceState.getInstance().getCurrent();
        // Placeholder workspace (id <= 0) is the bootstrap fallback when
        // listWorkspaces() failed — nothing to configure. Swallow the click
        // rather than showing a dialog full of disabled fields.
        if (current == null || current.getId() <= 0) return;
        WorkspaceSettingsDialog dialog = new WorkspaceSettingsDialog(
                mainLayout.getModalHost(),
                mainLayout.getBlurTarget(),
                workspaceService,
                current,
                getCurrentUser().getId(),
                updated -> {
                    // Rename result: push into WorkspaceState so the sidebar
                    // header + switcher menu labels update, then refresh the
                    // cache so the next menu open shows the new name.
                    WorkspaceState.getInstance().setCurrent(updated);
                    mainLayout.getSidebar().refreshWorkspaces();
                },
                () -> {
                    // After leaving, clear current. If no real workspace is
                    // available the app stays in DM-only mode.
                    WorkspaceState.getInstance().setCurrent(null);
                    mainLayout.getSidebar().refreshWorkspaces();
                });
        dialog.show();
    }

    private void openJoinWorkspaceDialog() {
        JoinWorkspaceDialog dialog = new JoinWorkspaceDialog(
                mainLayout.getModalHost(),
                mainLayout.getBlurTarget(),
                workspaceService,
                joined -> {
                    WorkspaceState.getInstance().setCurrent(joined);
                    mainLayout.getSidebar().refreshWorkspaces();
                });
        dialog.show();
    }

    private void openNewChatDialog() {
        NewChatDialog dialog = new NewChatDialog(
                mainLayout.getModalHost(),
                mainLayout.getBlurTarget(),
                userService, chatService, workspaceService,
                getCurrentUser().getId(),
                chat -> {
                    chatListController.startRefresh();
                    openChat(chat);
                });
        dialog.show();
    }

    private void openProfileDialog() {
        ProfileDialog dialog = new ProfileDialog(
                mainLayout.getModalHost(),
                mainLayout.getBlurTarget(),
                userService, getCurrentUser(),
                updated -> {
                    TokenStore.getInstance().updateUser(updated);
                    mainLayout.getSidebar().setCurrentUser(updated);
                });
        dialog.show();
    }

    public void startDmWith(User otherUser) {
        chatService.getOrCreateDmChat(otherUser.getId()).thenAcceptAsync(this::openChat, Platform::runLater);
    }

    private void showWelcomeContent() {
        if (chatViewController != null) {
            chatViewController.dispose();
            chatViewController = null;
        }
        settingsVisible = false;
        if (chatListView != null) chatListView.clearSelection();

        com.devflow.model.Workspace ws = WorkspaceState.getInstance().getCurrent();
        String workspace = ws != null && ws.getName() != null && !ws.getName().isBlank()
                ? ws.getName()
                : "Persönlich";
        boolean hasWorkspace = ws != null && ws.getId() > 0;
        if (!hasWorkspace) workspace = "Kein Workspace";
        FontIcon headerMark = new FontIcon(Feather.MESSAGE_SQUARE);
        headerMark.getStyleClass().add("workspace-home-header-mark");
        StackPane headerMarkHost = new StackPane(headerMark);
        headerMarkHost.getStyleClass().add("workspace-home-header-mark-host");

        Label headerTitle = new Label(workspace);
        headerTitle.getStyleClass().add("workspace-home-header-title");
        headerTitle.setWrapText(true);
        Label headerMeta = new Label(hasWorkspace ? "Workspace" : "Direktnachrichten");
        headerMeta.getStyleClass().add("workspace-home-header-meta");
        VBox headerCopy = new VBox(2, headerTitle, headerMeta);
        headerCopy.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(headerCopy, Priority.ALWAYS);

        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);

        HBox header = new HBox(12, headerMarkHost, headerCopy, headerSpacer);
        header.getStyleClass().add("workspace-home-header");
        header.setAlignment(Pos.CENTER_LEFT);

        FontIcon emptyIcon = new FontIcon(Feather.MESSAGE_SQUARE);
        emptyIcon.getStyleClass().add("workspace-home-empty-icon");

        Label kicker = new Label(hasWorkspace ? "WORKSPACE" : "DIREKTNACHRICHTEN");
        kicker.getStyleClass().add("workspace-home-kicker");
        Label title = new Label(workspace);
        title.getStyleClass().add("workspace-home-title");
        title.setWrapText(true);
        Label subtitle = new Label("Wähle links eine Unterhaltung oder starte einen neuen Chat.");
        if (!hasWorkspace) {
            subtitle.setText("Du bist in keinem Workspace. Du kannst Direktnachrichten schreiben oder einen Workspace erstellen bzw. beitreten.");
        }
        subtitle.getStyleClass().add("workspace-home-subtitle");
        subtitle.setWrapText(true);

        Button primary = new Button(hasWorkspace ? "Neue Unterhaltung" : "Neue Direktnachricht");
        primary.getStyleClass().addAll("button-primary", "button-large", "workspace-home-primary");
        primary.setGraphic(new FontIcon(Feather.EDIT_3));
        primary.setOnAction(e -> openNewChatDialog());

        Button secondary = new Button("Workspace beitreten");
        secondary.getStyleClass().addAll("button-secondary", "button-large");
        secondary.setGraphic(new FontIcon(Feather.LOG_IN));
        secondary.setOnAction(e -> openJoinWorkspaceDialog());

        Button createWorkspace = new Button("Workspace erstellen");
        createWorkspace.getStyleClass().addAll("button-secondary", "button-large");
        createWorkspace.setGraphic(new FontIcon(Feather.PLUS));
        createWorkspace.setOnAction(e -> openNewWorkspaceDialog());

        HBox actions = hasWorkspace
                ? new HBox(10, primary, secondary)
                : new HBox(10, primary, createWorkspace, secondary);
        actions.getStyleClass().add("workspace-home-actions");
        actions.setAlignment(Pos.CENTER);

        VBox emptyState = new VBox(10, emptyIcon, kicker, title, subtitle, actions);
        emptyState.getStyleClass().add("workspace-home-empty");
        emptyState.setAlignment(Pos.CENTER);

        StackPane canvas = new StackPane(emptyState);
        canvas.getStyleClass().add("workspace-home-canvas");
        VBox.setVgrow(canvas, Priority.ALWAYS);

        VBox wrapper = new VBox(header, canvas);
        wrapper.getStyleClass().addAll("content-area", "workspace-home");
        updateTitleSubtitle(hasWorkspace ? "Workspace" : "Direktnachrichten");
        mainLayout.setMainContent(wrapper);
    }

    private void updateTitleSubtitle(String context) {
        if (mainLayout == null || mainLayout.getTitleBar() == null) return;
        com.devflow.model.Workspace ws = WorkspaceState.getInstance().getCurrent();
        String workspace = ws != null && ws.getName() != null && !ws.getName().isBlank()
                ? ws.getName()
                : "Persönlich";
        if (ws == null || ws.getId() <= 0) workspace = "Kein Workspace";
        mainLayout.getTitleBar().setSubtitle(workspace + " / " + context);
    }

    public void logout() {
        TokenStore.getInstance().clearAuthToken();
        // Clear workspace state so the next login starts clean; otherwise a
        // new user briefly inherits the previous user's selected workspace
        // before refreshWorkspaces() replaces it.
        WorkspaceState.getInstance().setCurrent(null);
        showLogin();
    }

    public User getCurrentUser() {
        if (TokenStore.getInstance().hasAuthToken() && TokenStore.getInstance().getAuthToken().getUser() != null) {
            return TokenStore.getInstance().getAuthToken().getUser();
        }
        return new User(0, "Gast", null);
    }

    private void stopAllPolling() {
        if (chatListController != null) {
            // Full dispose (not just stopRefresh) so the WorkspaceState
            // listener the controller registered is detached — otherwise the
            // singleton leaks one listener per login cycle.
            chatListController.dispose();
            chatListController = null;
        }
        if (chatViewController != null) {
            chatViewController.dispose();
            chatViewController = null;
        }
    }

    private void setScene(javafx.scene.Parent root, String extraStylesheet) {
        if (scene != null) {
            ThemeManager.getInstance().unregisterScene(scene);
        }
        scene = new Scene(root, 1100, 740);
        scene.setFill(PlatformWindowStyle.usesOpaqueFramelessWindow() ? Color.web("#07080c") : Color.TRANSPARENT);
        scene.getStylesheets().add(getClass().getResource("/styles/app.css").toExternalForm());
        if (extraStylesheet != null) {
            scene.getStylesheets().add(getClass().getResource(extraStylesheet).toExternalForm());
        }
        scene.getStylesheets().add(getClass().getResource("/styles/enterprise.css").toExternalForm());
        ThemeManager.getInstance().registerScene(scene);
        stage.setScene(scene);
        stage.show();
    }

    public Stage getStage() { return stage; }
}
