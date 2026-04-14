package com.devflow.controller;

import com.devflow.config.TokenStore;
import com.devflow.model.Chat;
import com.devflow.model.User;
import com.devflow.service.AuthService;
import com.devflow.service.ChatService;
import com.devflow.service.MessageService;
import com.devflow.service.UserService;
import com.devflow.view.ActivityBar;
import com.devflow.view.ChatListView;
import com.devflow.view.ChatView;
import com.devflow.view.LoginView;
import com.devflow.view.MainLayout;
import com.devflow.view.UserListView;
import javafx.application.Platform;
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

        scene = new Scene(loginView, 980, 700);
        scene.getStylesheets().add(getClass().getResource("/styles/app.css").toExternalForm());
        scene.getStylesheets().add(getClass().getResource("/styles/login.css").toExternalForm());
        stage.setScene(scene);
    }

    public void showMainLayout() {
        stopAllPolling();

        ActivityBar activityBar = new ActivityBar(this::onNavigate);
        mainLayout = new MainLayout(activityBar);

        ChatListView chatListView = new ChatListView();
        chatListController = new ChatListController(chatListView, chatService, this);

        UserListView userListView = new UserListView();
        userListController = new UserListController(userListView, userService, this);

        showWelcomeContent();
        mainLayout.setSideContent(chatListView);
        chatListController.startRefresh();

        scene = new Scene(mainLayout, 980, 700);
        scene.getStylesheets().add(getClass().getResource("/styles/app.css").toExternalForm());
        scene.getStylesheets().add(getClass().getResource("/styles/chat.css").toExternalForm());
        stage.setScene(scene);
    }

    private void onNavigate(String target) {
        switch (target) {
            case "chats" -> {
                ChatListView chatListView = new ChatListView();
                chatListController = new ChatListController(chatListView, chatService, this);
                mainLayout.setSideContent(chatListView);
                chatListController.startRefresh();
            }
            case "users" -> {
                UserListView userListView = new UserListView();
                userListController = new UserListController(userListView, userService, this);
                mainLayout.setSideContent(userListView);
                userListController.load();
            }
            case "settings" -> {
                mainLayout.setSideContent(createSettingsPanel());
            }
        }
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
            mainLayout.getActivityBar().selectChats();
        }, Platform::runLater);
    }

    private void showWelcomeContent() {
        VBox welcome = new VBox(12);
        welcome.getStyleClass().add("welcome-content");

        Label title = new Label("DevFlow");
        title.getStyleClass().add("welcome-title");

        Label subtitle = new Label("Waehle einen Chat oder starte eine neue Unterhaltung");
        subtitle.getStyleClass().add("muted");

        welcome.getChildren().addAll(title, subtitle);
        StackPane wrapper = new StackPane(welcome);
        wrapper.getStyleClass().add("content-area");
        mainLayout.setMainContent(wrapper);
    }

    private VBox createSettingsPanel() {
        VBox settings = new VBox(12);
        settings.getStyleClass().add("settings-panel");
        Label title = new Label("Einstellungen");
        title.getStyleClass().add("section-title");
        settings.getChildren().add(title);
        return settings;
    }

    public void logout() {
        TokenStore.getInstance().clearAuthToken();
        showLogin();
    }

    public User getCurrentUser() {
        if (TokenStore.getInstance().hasAuthToken() && TokenStore.getInstance().getAuthToken().getUser() != null) {
            return TokenStore.getInstance().getAuthToken().getUser();
        }
        return new User(0, "Unknown", null);
    }

    private void stopAllPolling() {
        if (chatListController != null) {
            chatListController.stopRefresh();
        }
        if (chatViewController != null) {
            chatViewController.stopPolling();
        }
    }

    public Stage getStage() { return stage; }
}
