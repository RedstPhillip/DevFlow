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
import javafx.scene.shape.Circle;

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
    private final VBox searchWrap;
    private final StackPane listHost;
    private final Avatar userAvatar;
    private final Label usernameLabel;
    private final Label statusLabel;
    private final Button userSettingsBtn;

    // Settings-mode navigation (shown in panel when SETTINGS is active)
    private final VBox settingsNav;
    private Node chatListContent;

    private Runnable onChatsClick;
    private Runnable onSettingsClick;
    private Runnable onNewChat;
    private Runnable onProfileClick;
    private java.util.function.Consumer<String> onSearch;
    private java.util.function.Consumer<String> onSettingsSection;
    private String activeSettingsSection;

    public Sidebar() {
        getStyleClass().add("sidebar-root");

        // ── Rail ──
        rail = new VBox(6);
        rail.getStyleClass().add("activity-rail");
        rail.setPadding(new Insets(10, 8, 10, 8));

        toggleButton = new Button();
        toggleButton.setGraphic(Icons.menu());
        toggleButton.getStyleClass().add("rail-toggle");
        toggleButton.setTooltip(new Tooltip("Seitenleiste ein-/ausklappen"));
        toggleButton.setFocusTraversable(false);
        toggleButton.setOnAction(e -> setExpanded(!expanded));

        // SVG line-art icons render identically on every platform.
        chatsItem = buildRailItem(Icons.messageSquare(), "Chats");
        chatsItem.setOnAction(e -> activate(RailKey.CHATS));

        settingsItem = buildRailItem(Icons.settings(), "Einstellungen");
        settingsItem.setOnAction(e -> activate(RailKey.SETTINGS));

        Region grow = new Region();
        VBox.setVgrow(grow, Priority.ALWAYS);

        profileItem = buildRailItem(Icons.user(), "Profil");
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

        newChatButton = new Button();
        newChatButton.setGraphic(Icons.plus());
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
        searchWrap = new VBox(searchField);
        searchWrap.setPadding(new Insets(0, 12, 10, 12));
        searchField.textProperty().addListener((obs, old, val) -> {
            if (onSearch != null) onSearch.accept(val);
        });

        listHost = new StackPane();
        VBox.setVgrow(listHost, Priority.ALWAYS);

        // Settings navigation panel (hidden by default; shown in SETTINGS mode)
        settingsNav = new VBox(2);
        settingsNav.getStyleClass().add("settings-nav");
        settingsNav.setPadding(new Insets(4, 8, 8, 8));
        settingsNav.getChildren().addAll(
                buildSettingsNavItem("appearance", Icons.palette(),    "Erscheinungsbild"),
                buildSettingsNavItem("github",     Icons.github(),     "GitHub Integration"),
                buildSettingsNavItem("account",    Icons.userCircle(), "Account"),
                buildSettingsNavItem("about",      Icons.info(),       "Über")
        );

        userAvatar = new Avatar("?", 36);
        userAvatar.getStyleClass().add("avatar-clickable");
        userAvatar.setOnMouseClicked(e -> { if (onProfileClick != null) onProfileClick.run(); });

        usernameLabel = new Label("–");
        usernameLabel.getStyleClass().add("sidebar-username");
        Circle presenceDot = new Circle(4);
        presenceDot.getStyleClass().add("sidebar-presence-online");
        statusLabel = new Label("Online");
        statusLabel.getStyleClass().add("sidebar-status");
        HBox statusRow = new HBox(6, presenceDot, statusLabel);
        statusRow.setAlignment(Pos.CENTER_LEFT);
        VBox userInfo = new VBox(2, usernameLabel, statusRow);
        userInfo.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(userInfo, Priority.ALWAYS);

        // Settings access lives in the rail — no second cog here.
        userSettingsBtn = null;

        HBox userBar = new HBox(10, userAvatar, userInfo);
        userBar.getStyleClass().add("sidebar-user-bar");
        userBar.setAlignment(Pos.CENTER_LEFT);
        userBar.setPadding(new Insets(12, 14, 12, 14));

        panel.getChildren().addAll(header, searchWrap, listHost, userBar);

        getChildren().addAll(rail, panel);

        setExpanded(true);
        updateActiveStyles();
        applyPanelMode();
    }

    private Button buildSettingsNavItem(String key, Node icon, String label) {
        Button b = new Button();
        b.getStyleClass().add("settings-nav-item");
        StackPane iconHost = new StackPane(icon);
        iconHost.getStyleClass().add("settings-nav-icon");
        iconHost.setMinSize(18, 18);
        iconHost.setPrefSize(18, 18);
        Label textLbl = new Label(label);
        textLbl.getStyleClass().add("settings-nav-label");
        HBox content = new HBox(12, iconHost, textLbl);
        content.setAlignment(Pos.CENTER_LEFT);
        b.setGraphic(content);
        b.setMaxWidth(Double.MAX_VALUE);
        b.setFocusTraversable(false);
        b.getProperties().put("key", key);
        b.setOnAction(e -> {
            activeSettingsSection = key;
            updateSettingsNavStyles();
            if (onSettingsSection != null) onSettingsSection.accept(key);
        });
        return b;
    }

    private void updateSettingsNavStyles() {
        for (Node n : settingsNav.getChildren()) {
            if (!(n instanceof Button b)) continue;
            b.getStyleClass().remove("settings-nav-item-active");
            Object k = b.getProperties().get("key");
            if (k != null && k.equals(activeSettingsSection)) {
                b.getStyleClass().add("settings-nav-item-active");
            }
        }
    }

    private void applyPanelMode() {
        boolean isSettings = activeKey == RailKey.SETTINGS;
        brand.setText(isSettings ? "Einstellungen" : "Chats");
        newChatButton.setVisible(!isSettings);
        newChatButton.setManaged(!isSettings);
        searchWrap.setVisible(!isSettings);
        searchWrap.setManaged(!isSettings);
        if (isSettings) {
            if (activeSettingsSection == null) activeSettingsSection = "appearance";
            updateSettingsNavStyles();
            listHost.getChildren().setAll(settingsNav);
        } else if (chatListContent != null) {
            listHost.getChildren().setAll(chatListContent);
        } else {
            listHost.getChildren().clear();
        }
    }

    private Button buildRailItem(Node icon, String label) {
        Button b = new Button();
        b.getStyleClass().add("rail-item");
        StackPane iconHost = new StackPane(icon);
        iconHost.getStyleClass().add("rail-icon");
        iconHost.setMinSize(20, 20);
        iconHost.setPrefSize(20, 20);
        Label textLbl = new Label(label);
        textLbl.getStyleClass().add("label");
        HBox content = new HBox(12, iconHost, textLbl);
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
        applyPanelMode();
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
        toggleButton.setGraphic(expanded ? Icons.chevronLeft() : Icons.menu());
    }

    public void setActive(RailKey key) {
        this.activeKey = key;
        updateActiveStyles();
        applyPanelMode();
    }

    public void setListNode(Node node) {
        this.chatListContent = node;
        if (activeKey != RailKey.SETTINGS) {
            listHost.getChildren().setAll(node);
        }
    }

    public void setActiveSettingsSection(String key) {
        this.activeSettingsSection = key;
        updateSettingsNavStyles();
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
    public void setOnSettingsSection(java.util.function.Consumer<String> c) { this.onSettingsSection = c; }
}
