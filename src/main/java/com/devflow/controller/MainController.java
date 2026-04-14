package com.devflow.controller;

import com.devflow.config.ThemeManager;
import com.devflow.config.TokenStore;
import com.devflow.model.Chat;
import com.devflow.model.User;
import com.devflow.service.AuthService;
import com.devflow.service.ChatService;
import com.devflow.service.MessageService;
import com.devflow.service.UserService;
import com.devflow.view.ChatListView;
import com.devflow.view.ChatView;
import com.devflow.view.LoginView;
import com.devflow.view.MainLayout;
import com.devflow.view.SettingsView;
import com.devflow.view.Sidebar;
import com.devflow.view.UserListView;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class MainController {

    private final Stage stage;
    private Scene scene;
    private MainLayout mainLayout;

    private ChatListController chatListController;
    private ChatViewController chatViewController;
    private UserListController userListController;
    private LoginController loginController;

    private ChatListView chatListView;
    private UserListView userListView;

    private final AuthService authService;
    private final UserService userService;
    private final ChatService chatService;
    private final MessageService messageService;

    public MainController(Stage stage) {
        this.stage = stage;
        this.authService = new AuthService();
        this.userService = new UserService();
        this.chatService = new ChatService();
        this.messageService = new MessageService();
    }

    public void start() {
        if (TokenStore.getInstance().hasAuthToken()) {
            showMainLayout();
        } else {
            showLogin();
        }
    }

    public void showLogin() {
        stopAllPolling();
        LoginView loginView = new LoginView();
        loginController = new LoginController(loginView, authService, this);

        setScene(loginView, "/styles/login.css");
    }

    public void showMainLayout() {
        stopAllPolling();

        Sidebar sidebar = new Sidebar();
        sidebar.setCurrentUser(getCurrentUser());
        mainLayout = new MainLayout(sidebar);

        chatListView = new ChatListView();
        chatListController = new ChatListController(chatListView, chatService, this);

        userListView = new UserListView();
        userListView.setCurrentUserId(getCurrentUser().getId());
        userListController = new UserListController(userListView, userService, this);

        sidebar.setOnChatsTab(() -> {
            mainLayout.setSidebarContent(chatListView);
            chatListController.startRefresh();
        });
        sidebar.setOnUsersTab(() -> {
            chatListController.stopRefresh();
            mainLayout.setSidebarContent(userListView);
            userListController.load();
        });
        sidebar.setOnNewChat(() -> sidebar.selectTab(Sidebar.Tab.USERS));
        sidebar.setOnSettings(this::showSettings);

        // Trigger initial tab
        sidebar.selectTab(Sidebar.Tab.CHATS);

        showWelcomeContent();

        setScene(mainLayout, "/styles/chat.css");
    }

    public void showSettings() {
        if (chatViewController != null) {
            chatViewController.stopPolling();
        }
        SettingsView settings = new SettingsView();
        settings.setOnLogout(this::logout);
        mainLayout.setMainContent(settings);
    }

    public void openChat(Chat chat) {
        if (chatViewController != null) {
            chatViewController.stopPolling();
        }
        ChatView chatView = new ChatView();
        User currentUser = getCurrentUser();
        chatViewController = new ChatViewController(chatView, messageService, chat, currentUser);
        mainLayout.setMainContent(chatView);
        chatViewController.startPolling();
    }

    public void startDmWith(User otherUser) {
        chatService.getOrCreateDmChat(otherUser.getId()).thenAcceptAsync(chat -> {
            openChat(chat);
            mainLayout.getSidebar().selectTab(Sidebar.Tab.CHATS);
        }, Platform::runLater);
    }

    private void showWelcomeContent() {
        VBox welcome = new VBox(10);
        welcome.getStyleClass().add("welcome-content");
        welcome.setAlignment(Pos.CENTER);

        Label title = new Label("Willkommen bei DevFlow");
        title.getStyleClass().add("welcome-title");

        Label subtitle = new Label("Wähle einen Chat aus der Seitenleiste oder starte eine neue Unterhaltung.");
        subtitle.getStyleClass().add("welcome-subtitle");
        subtitle.setWrapText(true);
        subtitle.setMaxWidth(460);

        welcome.getChildren().addAll(title, subtitle);
        StackPane wrapper = new StackPane(welcome);
        wrapper.getStyleClass().add("content-area");
        mainLayout.setMainContent(wrapper);
    }

    public void logout() {
        TokenStore.getInstance().clearAuthToken();
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
            chatListController.stopRefresh();
        }
        if (chatViewController != null) {
            chatViewController.stopPolling();
            chatViewController = null;
        }
    }

    private void setScene(javafx.scene.Parent root, String extraStylesheet) {
        if (scene != null) {
            ThemeManager.getInstance().unregisterScene(scene);
        }
        scene = new Scene(root, 1100, 740);
        scene.getStylesheets().add(getClass().getResource("/styles/app.css").toExternalForm());
        if (extraStylesheet != null) {
            scene.getStylesheets().add(getClass().getResource(extraStylesheet).toExternalForm());
        }
        ThemeManager.getInstance().registerScene(scene);
        stage.setScene(scene);
        stage.show();
    }

    public Stage getStage() { return stage; }
}
