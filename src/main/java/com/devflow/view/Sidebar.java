package com.devflow.view;

import com.devflow.model.User;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public class Sidebar extends HBox {

    public enum RailKey { CHATS, SETTINGS }

    // Rail
    private final VBox rail;
    private final Button toggleButton;
    private final Button chatsItem;
    private final Button settingsItem;
    private final Button profileItem;
    private RailKey activeKey = RailKey.CHATS;
    private boolean expanded = true;

    // Panel (chat list)
    private final VBox panel;
    private final Label brand;
    private final Button newChatButton;
    private final TextField searchField;
    private final StackPane listHost;
    private final Avatar userAvatar;
    private final Label usernameLabel;
    private final Label statusLabel;
    private final Button userSettingsBtn;

    private Runnable onChatsClick;
    private Runnable onSettingsClick;
    private Runnable onNewChat;
    private Runnable onProfileClick;
    private java.util.function.Consumer<String> onSearch;

    public Sidebar() {
        getStyleClass().add("main-layout");

        // ── Rail ──
        rail = new VBox(6);
        rail.getStyleClass().add("activity-rail");
        rail.setPadding(new Insets(10, 8, 10, 8));

        toggleButton = new Button("\u2630");
        toggleButton.getStyleClass().add("rail-toggle");
        toggleButton.setTooltip(new Tooltip("Seitenleiste ein-/ausklappen"));
        toggleButton.setFocusTraversable(false);
        toggleButton.setOnAction(e -> setExpanded(!expanded));

        chatsItem = buildRailItem("\uD83D\uDCAC", "Chats");
        chatsItem.setOnAction(e -> activate(RailKey.CHATS));

        settingsItem = buildRailItem("\u2699", "Einstellungen");
        settingsItem.setOnAction(e -> activate(RailKey.SETTINGS));

        Region grow = new Region();
        VBox.setVgrow(grow, Priority.ALWAYS);

        profileItem = buildRailItem("\u263A", "Profil");
        profileItem.setOnAction(e -> { if (onProfileClick != null) onProfileClick.run(); });

        rail.getChildren().addAll(toggleButton, chatsItem, settingsItem, grow, profileItem);

        // ── Panel ──
        panel = new VBox();
        panel.getStyleClass().add("sidebar");
        panel.setPrefWidth(300);
        panel.setMinWidth(260);
        panel.setMaxWidth(360);

        brand = new Label("Chats");
        brand.getStyleClass().add("sidebar-brand");

        newChatButton = new Button("+");
        newChatButton.getStyleClass().add("sidebar-new-chat-btn");
        newChatButton.setTooltip(new Tooltip("Neuer Chat"));
        newChatButton.setFocusTraversable(false);
        newChatButton.setOnAction(e -> { if (onNewChat != null) onNewChat.run(); });

        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);
        HBox header = new HBox(10, brand, headerSpacer, newChatButton);
        header.getStyleClass().add("sidebar-header");
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(14, 14, 12, 16));

        searchField = new TextField();
        searchField.setPromptText("Chats durchsuchen…");
        searchField.getStyleClass().add("sidebar-search-field");
        VBox searchWrap = new VBox(searchField);
        searchWrap.setPadding(new Insets(0, 12, 10, 12));
        searchField.textProperty().addListener((obs, old, val) -> {
            if (onSearch != null) onSearch.accept(val);
        });

        listHost = new StackPane();
        VBox.setVgrow(listHost, Priority.ALWAYS);

        userAvatar = new Avatar("?", 36);
        userAvatar.getStyleClass().add("avatar-clickable");
        userAvatar.setOnMouseClicked(e -> { if (onProfileClick != null) onProfileClick.run(); });

        usernameLabel = new Label("–");
        usernameLabel.getStyleClass().add("sidebar-username");
        statusLabel = new Label("Online");
        statusLabel.getStyleClass().add("sidebar-status");
        VBox userInfo = new VBox(1, usernameLabel, statusLabel);
        userInfo.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(userInfo, Priority.ALWAYS);

        userSettingsBtn = new Button("\u2699");
        userSettingsBtn.getStyleClass().add("sidebar-settings-btn");
        userSettingsBtn.setTooltip(new Tooltip("Einstellungen"));
        userSettingsBtn.setFocusTraversable(false);
        userSettingsBtn.setOnAction(e -> activate(RailKey.SETTINGS));

        HBox userBar = new HBox(10, userAvatar, userInfo, userSettingsBtn);
        userBar.getStyleClass().add("sidebar-user-bar");
        userBar.setAlignment(Pos.CENTER_LEFT);
        userBar.setPadding(new Insets(12, 14, 12, 14));

        panel.getChildren().addAll(header, searchWrap, listHost, userBar);

        getChildren().addAll(rail, panel);

        setExpanded(true);
        updateActiveStyles();
    }

    private Button buildRailItem(String icon, String label) {
        Button b = new Button();
        b.getStyleClass().add("rail-item");
        Label iconLbl = new Label(icon);
        iconLbl.getStyleClass().add("rail-icon");
        Label textLbl = new Label(label);
        textLbl.getStyleClass().add("label");
        HBox content = new HBox(12, iconLbl, textLbl);
        content.setAlignment(Pos.CENTER_LEFT);
        b.setGraphic(content);
        b.setTooltip(new Tooltip(label));
        b.setMaxWidth(Double.MAX_VALUE);
        b.setFocusTraversable(false);
        b.getProperties().put("text", textLbl);
        return b;
    }

    private void activate(RailKey key) {
        this.activeKey = key;
        updateActiveStyles();
        if (key == RailKey.CHATS && onChatsClick != null) onChatsClick.run();
        if (key == RailKey.SETTINGS && onSettingsClick != null) onSettingsClick.run();
    }

    private void updateActiveStyles() {
        chatsItem.getStyleClass().remove("rail-item-active");
        settingsItem.getStyleClass().remove("rail-item-active");
        if (activeKey == RailKey.CHATS) chatsItem.getStyleClass().add("rail-item-active");
        if (activeKey == RailKey.SETTINGS) settingsItem.getStyleClass().add("rail-item-active");
    }

    public void setExpanded(boolean expanded) {
        this.expanded = expanded;
        double width = expanded ? 196 : 60;
        rail.setPrefWidth(width);
        rail.setMinWidth(width);
        rail.setMaxWidth(width);
        for (Node n : new Node[] { chatsItem, settingsItem, profileItem }) {
            Label text = (Label) ((Button) n).getProperties().get("text");
            if (text != null) {
                text.setVisible(expanded);
                text.setManaged(expanded);
            }
        }
        toggleButton.setText(expanded ? "\u276E" : "\u2630");
    }

    public void setActive(RailKey key) {
        this.activeKey = key;
        updateActiveStyles();
    }

    public void setListNode(Node node) {
        listHost.getChildren().setAll(node);
    }

    public void setBrand(String text) { brand.setText(text); }

    public void setCurrentUser(User user) {
        if (user == null) {
            usernameLabel.setText("Gast");
            userAvatar.setName("?");
        } else {
            usernameLabel.setText(user.getUsername());
            userAvatar.setName(user.getUsername());
        }
    }

    public TextField getSearchField() { return searchField; }

    public void setOnChatsClick(Runnable r) { this.onChatsClick = r; }
    public void setOnSettingsClick(Runnable r) { this.onSettingsClick = r; }
    public void setOnNewChat(Runnable r) { this.onNewChat = r; }
    public void setOnProfileClick(Runnable r) { this.onProfileClick = r; }
    public void setOnSearch(java.util.function.Consumer<String> c) { this.onSearch = c; }
}
