package com.devflow.view;

import com.devflow.model.User;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public class Sidebar extends VBox {

    public enum Tab { CHATS, USERS }

    private final Button chatsTab;
    private final Button usersTab;
    private final StackPane contentArea;
    private final Button newChatButton;
    private final Button settingsButton;
    private final Avatar userAvatar;
    private final Label usernameLabel;
    private final Label statusLabel;

    private Runnable onChatsTab;
    private Runnable onUsersTab;
    private Runnable onNewChat;
    private Runnable onSettings;

    private Tab activeTab;

    public Sidebar() {
        getStyleClass().add("sidebar");
        setPrefWidth(300);
        setMinWidth(260);
        setMaxWidth(360);

        // ── Header ──
        Label brand = new Label("DevFlow");
        brand.getStyleClass().add("sidebar-brand");

        newChatButton = new Button("+");
        newChatButton.getStyleClass().add("sidebar-new-chat-btn");
        newChatButton.setTooltip(new Tooltip("Neuer Chat"));
        newChatButton.setOnAction(e -> { if (onNewChat != null) onNewChat.run(); });

        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);

        HBox header = new HBox(10, brand, headerSpacer, newChatButton);
        header.getStyleClass().add("sidebar-header");
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(14, 16, 14, 16));

        // ── Tabs ──
        chatsTab = buildTab("Chats", () -> selectTab(Tab.CHATS));
        usersTab = buildTab("Benutzer", () -> selectTab(Tab.USERS));
        HBox.setHgrow(chatsTab, Priority.ALWAYS);
        HBox.setHgrow(usersTab, Priority.ALWAYS);
        chatsTab.setMaxWidth(Double.MAX_VALUE);
        usersTab.setMaxWidth(Double.MAX_VALUE);

        HBox tabBar = new HBox(chatsTab, usersTab);
        tabBar.getStyleClass().add("sidebar-tab-bar");
        tabBar.setPadding(new Insets(0, 12, 0, 12));

        // ── Content ──
        contentArea = new StackPane();
        contentArea.getStyleClass().add("sidebar-content");
        VBox.setVgrow(contentArea, Priority.ALWAYS);

        // ── User bar ──
        userAvatar = new Avatar("?", 36);

        usernameLabel = new Label("–");
        usernameLabel.getStyleClass().add("sidebar-username");
        statusLabel = new Label("Online");
        statusLabel.getStyleClass().add("sidebar-status");

        VBox userInfo = new VBox(1, usernameLabel, statusLabel);
        userInfo.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(userInfo, Priority.ALWAYS);

        settingsButton = new Button("\u2699");
        settingsButton.getStyleClass().add("sidebar-settings-btn");
        settingsButton.setTooltip(new Tooltip("Einstellungen"));
        settingsButton.setOnAction(e -> { if (onSettings != null) onSettings.run(); });

        HBox userBar = new HBox(10, userAvatar, userInfo, settingsButton);
        userBar.getStyleClass().add("sidebar-user-bar");
        userBar.setAlignment(Pos.CENTER_LEFT);
        userBar.setPadding(new Insets(12, 14, 12, 14));

        getChildren().addAll(header, tabBar, contentArea, userBar);
        selectTab(Tab.CHATS);
    }

    private Button buildTab(String label, Runnable action) {
        Button btn = new Button(label);
        btn.getStyleClass().add("sidebar-tab");
        btn.setOnAction(e -> action.run());
        return btn;
    }

    public void selectTab(Tab tab) {
        activeTab = tab;
        chatsTab.getStyleClass().remove("sidebar-tab-active");
        usersTab.getStyleClass().remove("sidebar-tab-active");
        if (tab == Tab.CHATS) {
            chatsTab.getStyleClass().add("sidebar-tab-active");
            if (onChatsTab != null) onChatsTab.run();
        } else {
            usersTab.getStyleClass().add("sidebar-tab-active");
            if (onUsersTab != null) onUsersTab.run();
        }
    }

    public void setContent(Node node) {
        contentArea.getChildren().setAll(node);
    }

    public void setCurrentUser(User user) {
        if (user == null) {
            usernameLabel.setText("Gast");
            userAvatar.setName("?");
        } else {
            usernameLabel.setText(user.getUsername());
            userAvatar.setName(user.getUsername());
        }
    }

    public Tab getActiveTab() { return activeTab; }

    public void setOnChatsTab(Runnable r) { this.onChatsTab = r; }
    public void setOnUsersTab(Runnable r) { this.onUsersTab = r; }
    public void setOnNewChat(Runnable r) { this.onNewChat = r; }
    public void setOnSettings(Runnable r) { this.onSettings = r; }
}
