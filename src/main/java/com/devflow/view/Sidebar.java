package com.devflow.view;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;

import com.devflow.config.WorkspaceState;
import com.devflow.model.User;
import com.devflow.model.Workspace;
import com.devflow.service.WorkspaceService;

import javafx.application.Platform;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;

/**
 * Phase 4 — Linear-style sidebar.
 *
 * <p>Old hierarchy was {@code HBox(activity-rail, panel)} — the rail held the
 * Chats/Einstellungen/Profil items as a side rail. Phase 4 collapses the
 * activity-rail into the panel itself: this class is now a single
 * {@link VBox} (240 px fixed) with five regions stacked top-to-bottom:</p>
 *
 * <ol>
 *   <li>Workspace switcher (44 h, hover row, click → ContextMenu).</li>
 *   <li>1 px subtle divider.</li>
 *   <li>Primary nav (Chats / Einstellungen).</li>
 *   <li>Section content (chat-list <em>or</em> settings sub-nav).</li>
 *   <li>User bar at the bottom (56 h, click → ProfileDialog).</li>
 * </ol>
 *
 * <p>The Workspace ContextMenu now also carries the per-workspace
 * "Einstellungen" entry as the last item before "Neu/Beitreten" — the
 * Gear-Button next to the chevron is gone (spec §11). Pin/Star markers are
 * removed from the header but kept in the dropdown rows because the spec only
 * forbids them in the header (Q-tweak: dropdown-marker is useful, header is
 * noise).</p>
 */
public class Sidebar extends VBox {

    /**
     * Logical content modes the sidebar can show. Names kept as "RailKey" for
     * source compatibility with controllers that reference them — the
     * activity-rail itself no longer exists.
     */
    public enum RailKey { CHATS, SETTINGS }

    private static final int SIDEBAR_WIDTH = 230;

    // Primary nav
    private final Button chatsNavItem;
    private final Button settingsFooterButton;
    private RailKey activeKey = RailKey.CHATS;

    // Section content
    private final StackPane sectionContentHost;
    private final VBox chatsSection;
    private final Label brand;          // section header label "CHATS"/"EINSTELLUNGEN"
    private final Button newChatButton; // plus-button in the chats section header
    private final TextField searchField;
    private final StackPane listHost;
    private Node chatListContent;

    // User bar
    private final Avatar userAvatar;
    private final Label usernameLabel;
    private final Label statusLabel;
    private final HBox userBar;
    private final HBox profileSummary;

    // Workspace switcher
    private final WorkspaceService workspaceService = new WorkspaceService();
    private final HBox workspaceSwitcher;
    private final Label workspaceName;
    private final HBox workspaceErrorBanner;
    private final Label workspaceErrorLabel;
    private List<Workspace> workspacesCache = new ArrayList<>();

    // Callbacks
    private Runnable onChatsClick;
    private Runnable onSettingsClick;
    private Runnable onNewChat;
    private Runnable onProfileClick;
    private Runnable onNewWorkspace;
    private Runnable onJoinWorkspace;
    private Runnable onWorkspaceSettings;
    private Consumer<String> onSearch;

    // External listener tracked so we can detach it on disposal.
    private final Consumer<Workspace> workspaceStateListener;

    public Sidebar() {
        getStyleClass().add("sidebar");
        setPrefWidth(SIDEBAR_WIDTH);
        setMinWidth(SIDEBAR_WIDTH);
        setMaxWidth(SIDEBAR_WIDTH);

        // ── 1) Workspace switcher (Polish-Pass §3: BrandMark + name + chevron). ──
        // Spec: 46 h row, 0/14 padding, 9 gap, name in 14/500, chevron 11 px.
        BrandMark sidebarBrand = new BrandMark(22);
        workspaceName = new Label("—");
        workspaceName.getStyleClass().add("workspace-switcher-name");
        FontIcon workspaceChev = new FontIcon(Feather.CHEVRON_DOWN);
        workspaceChev.getStyleClass().add("workspace-switcher-chevron");
        Region wsSpacer = new Region();
        HBox.setHgrow(wsSpacer, Priority.ALWAYS);
        workspaceSwitcher = new HBox(9, sidebarBrand, workspaceName, wsSpacer, workspaceChev);
        workspaceSwitcher.getStyleClass().add("workspace-switcher");
        workspaceSwitcher.setAlignment(Pos.CENTER_LEFT);
        workspaceSwitcher.setPadding(new Insets(0, 14, 0, 14));
        workspaceSwitcher.setMinHeight(46);
        workspaceSwitcher.setPrefHeight(46);
        workspaceSwitcher.setMaxHeight(46);
        workspaceSwitcher.setOnMouseClicked(e -> showWorkspaceMenu());

        // Error banner shown if listWorkspaces() fails on mount/refresh.
        workspaceErrorLabel = new Label("Workspaces konnten nicht geladen werden — bitte Verbindung prüfen.");
        workspaceErrorLabel.getStyleClass().addAll("workspace-error-label", "t-caption");
        workspaceErrorLabel.setWrapText(true);
        workspaceErrorBanner = new HBox(workspaceErrorLabel);
        workspaceErrorBanner.getStyleClass().add("workspace-error-banner");
        workspaceErrorBanner.setPadding(new Insets(6, 12, 6, 12));
        workspaceErrorBanner.setVisible(false);
        workspaceErrorBanner.setManaged(false);

        // ── 2) Subtle divider between switcher and primary nav. ──
        Region dividerTop = sidebarDivider();

        // ── 3) Primary nav: active modules plus disabled future slots. ──
        chatsNavItem = buildNavItem(Feather.MESSAGE_SQUARE, "Chats", false);
        chatsNavItem.setOnAction(e -> activate(RailKey.CHATS));
        VBox primaryNav = new VBox(2,
                chatsNavItem,
                buildDisabledNavItem(Feather.CODE, "Code"),
                buildDisabledNavItem(Feather.GITHUB, "GitHub"),
                buildDisabledNavItem(Feather.FILE_TEXT, "Dateien")
        );
        primaryNav.getStyleClass().add("sidebar-module-nav");
        primaryNav.setPadding(new Insets(10, 10, 10, 10));

        // ── 4) Section content. ──
        // The sidebar stays global. Settings sub-navigation belongs to the
        // settings view itself, not to a second nested rail.
        brand = new Label("CHATS");
        brand.getStyleClass().addAll("sidebar-section-header-label", "t-section-header");

        // Plus button shown in the Chats section header only.
        FontIcon plusIcon = new FontIcon(Feather.PLUS);
        plusIcon.getStyleClass().add("sidebar-new-chat-icon");
        newChatButton = new Button();
        newChatButton.setGraphic(plusIcon);
        newChatButton.getStyleClass().addAll("button-flat", "sidebar-new-chat-btn", "button-compact");
        newChatButton.setTooltip(new Tooltip("Neuer Chat"));
        newChatButton.setFocusTraversable(false);
        newChatButton.setOnAction(e -> { if (onNewChat != null) onNewChat.run(); });

        Region brandSpacer = new Region();
        HBox.setHgrow(brandSpacer, Priority.ALWAYS);
        HBox sectionHeader = new HBox(8, brand, brandSpacer, newChatButton);
        sectionHeader.getStyleClass().add("sidebar-section-header");
        sectionHeader.setAlignment(Pos.CENTER_LEFT);
        // Polish-Pass §4: 14/14/6 — slightly more breathing room on top.
        sectionHeader.setPadding(new Insets(14, 14, 6, 14));

        // Search field (kept compact at 32 h per Q15 — deliberately one tier
        // below the standard 36 h form-input scale).
        searchField = new TextField();
        searchField.setPromptText("Chats durchsuchen…");
        searchField.getStyleClass().add("sidebar-search-field");
        VBox searchWrap = new VBox(searchField);
        searchWrap.setPadding(new Insets(0, 12, 8, 12));
        searchField.textProperty().addListener((obs, old, val) -> {
            if (onSearch != null) onSearch.accept(val);
        });

        listHost = new StackPane();
        VBox.setVgrow(listHost, Priority.ALWAYS);

        chatsSection = new VBox(sectionHeader, searchWrap, listHost);
        chatsSection.getStyleClass().add("sidebar-section");

        sectionContentHost = new StackPane(chatsSection);
        VBox.setVgrow(sectionContentHost, Priority.ALWAYS);

        // ── 5) User bar (whole row clickable → ProfileDialog). ──
        userAvatar = new Avatar("?", 32);
        userAvatar.getStyleClass().add("avatar-clickable");
        usernameLabel = new Label("–");
        usernameLabel.getStyleClass().addAll("sidebar-username", "t-body-strong");
        Circle presenceDot = new Circle(4);
        presenceDot.getStyleClass().add("sidebar-presence-online");
        statusLabel = new Label("Online");
        statusLabel.getStyleClass().addAll("sidebar-status", "t-caption");
        HBox statusRow = new HBox(6, presenceDot, statusLabel);
        statusRow.setAlignment(Pos.CENTER_LEFT);
        VBox userInfo = new VBox(2, usernameLabel, statusRow);
        userInfo.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(userInfo, Priority.ALWAYS);

        profileSummary = new HBox(10, userAvatar, userInfo);
        profileSummary.getStyleClass().add("sidebar-profile-summary");
        profileSummary.setAlignment(Pos.CENTER_LEFT);
        profileSummary.setMinWidth(0);
        HBox.setHgrow(profileSummary, Priority.ALWAYS);
        profileSummary.setOnMouseClicked(e -> { if (onProfileClick != null) onProfileClick.run(); });

        FontIcon settingsIcon = new FontIcon(Feather.SETTINGS);
        settingsIcon.getStyleClass().add("sidebar-footer-settings-icon");
        settingsFooterButton = new Button();
        settingsFooterButton.setGraphic(settingsIcon);
        settingsFooterButton.getStyleClass().addAll("button-flat", "sidebar-footer-settings-btn");
        settingsFooterButton.setTooltip(new Tooltip("Einstellungen"));
        settingsFooterButton.setFocusTraversable(false);
        settingsFooterButton.setOnAction(e -> activate(RailKey.SETTINGS));

        userBar = new HBox(8, profileSummary, settingsFooterButton);
        userBar.getStyleClass().add("sidebar-user-bar");
        userBar.setAlignment(Pos.CENTER_LEFT);
        userBar.setPadding(new Insets(8, 10, 8, 10));
        userBar.setMinHeight(56);
        userBar.setPrefHeight(56);
        userBar.setMaxHeight(56);

        // Bottom divider above user-bar (mirrors the top divider visual rhythm).
        Region dividerBottom = sidebarDivider();

        getChildren().addAll(
                workspaceSwitcher,
                workspaceErrorBanner,
                dividerTop,
                primaryNav,
                sectionContentHost,
                dividerBottom,
                userBar
        );

        applyActiveSection();
        updateActiveStyles();

        // React to workspace switches (any source: menu click, dialog success,
        // programmatic). Always hop to FX before touching the view.
        workspaceStateListener = ws -> Platform.runLater(() -> applyCurrentWorkspaceToHeader(ws));
        WorkspaceState.getInstance().addListener(workspaceStateListener);
        applyCurrentWorkspaceToHeader(WorkspaceState.getInstance().getCurrent());

        refreshWorkspacesInternal();
    }

    // ── Builders ─────────────────────────────────────────────────────────

    /** Subtle 1 px line separator used at the section boundaries. */
    private static Region sidebarDivider() {
        Region r = new Region();
        r.getStyleClass().add("sidebar-section-divider");
        r.setMinHeight(1);
        r.setPrefHeight(1);
        r.setMaxHeight(1);
        return r;
    }

    /**
     * Primary-nav button factory. Used both for the top-level nav (Chats /
     * Einstellungen) and — with {@code indented = true} — for the four
     * settings sub-tabs that appear when SETTINGS is the active mode.
     */
    private Button buildNavItem(Ikon glyph, String label, boolean indented) {
        FontIcon icon = new FontIcon(glyph);
        icon.getStyleClass().add("sidebar-nav-fonticon");
        Label textLbl = new Label(label);
        textLbl.getStyleClass().addAll("sidebar-nav-label", "t-body-strong");
        HBox content = new HBox(10, icon, textLbl);
        content.setAlignment(Pos.CENTER_LEFT);
        Button b = new Button();
        b.setGraphic(content);
        b.getStyleClass().add("sidebar-nav-item");
        if (indented) b.getStyleClass().add("indented");
        b.setMaxWidth(Double.MAX_VALUE);
        b.setFocusTraversable(false);
        return b;
    }

    private Button buildDisabledNavItem(Ikon glyph, String label) {
        Button b = buildNavItem(glyph, label, false);
        b.getStyleClass().add("sidebar-nav-item-disabled");
        b.setDisable(true);
        b.setTooltip(new Tooltip(label + " wird als Modul vorbereitet"));
        return b;
    }

    // ── Active-state plumbing ────────────────────────────────────────────

    /** Set which section ({@link RailKey#CHATS} or {@link RailKey#SETTINGS}) is shown. */
    public void setActive(RailKey key) {
        this.activeKey = key;
        updateActiveStyles();
        applyActiveSection();
    }

    private void activate(RailKey key) {
        setActive(key);
        if (key == RailKey.CHATS && onChatsClick != null) onChatsClick.run();
        if (key == RailKey.SETTINGS && onSettingsClick != null) onSettingsClick.run();
    }

    private void updateActiveStyles() {
        chatsNavItem.getStyleClass().remove("sidebar-nav-item-active");
        settingsFooterButton.getStyleClass().remove("sidebar-footer-settings-btn-active");
        if (activeKey == RailKey.CHATS) chatsNavItem.getStyleClass().add("sidebar-nav-item-active");
        if (activeKey == RailKey.SETTINGS) settingsFooterButton.getStyleClass().add("sidebar-footer-settings-btn-active");
    }

    private void applyActiveSection() {
        if (chatListContent != null) {
            listHost.getChildren().setAll(chatListContent);
        } else {
            listHost.getChildren().clear();
        }
        sectionContentHost.getChildren().setAll(chatsSection);
    }

    // ── External wiring (controller-side) ────────────────────────────────

    public void setListNode(Node node) {
        this.chatListContent = node;
        listHost.getChildren().setAll(node);
    }

    public TextField getSearchField() { return searchField; }

    public void setCurrentUser(User user) {
        if (user == null) {
            usernameLabel.setText("Gast");
            userAvatar.setName("?");
        } else {
            usernameLabel.setText(user.getUsername());
            userAvatar.setName(user.getUsername());
        }
    }

    public void setOnChatsClick(Runnable r)            { this.onChatsClick = r; }
    public void setOnSettingsClick(Runnable r)         { this.onSettingsClick = r; }
    public void setOnNewChat(Runnable r)               { this.onNewChat = r; }
    public void setOnProfileClick(Runnable r)          { this.onProfileClick = r; }
    public void setOnSearch(Consumer<String> c)        { this.onSearch = c; }

    public void setWorkspaceActions(Runnable onNewWorkspace, Runnable onJoinWorkspace) {
        this.onNewWorkspace = onNewWorkspace;
        this.onJoinWorkspace = onJoinWorkspace;
    }

    public void setOnWorkspaceSettings(Runnable onWorkspaceSettings) {
        this.onWorkspaceSettings = onWorkspaceSettings;
    }

    // ── Workspace switcher ───────────────────────────────────────────────

    public void refreshWorkspaces() {
        refreshWorkspacesInternal();
    }

    private void refreshWorkspacesInternal() {
        workspaceService.listWorkspaces()
                .whenComplete((list, err) -> Platform.runLater(() -> {
                    if (err != null) {
                        showWorkspaceError(true);
                        if (WorkspaceState.getInstance().getCurrent() == null) {
                            WorkspaceState.getInstance().setCurrent(placeholderWorkspace());
                        }
                        return;
                    }
                    showWorkspaceError(false);
                    workspacesCache = list != null ? list : new ArrayList<>();
                    Workspace current = WorkspaceState.getInstance().getCurrent();
                        Long currentId = current != null ? current.getId() : null;
                        boolean stillValid = currentId != null && workspacesCache.stream()
                            .anyMatch(w -> w.getId() == currentId);
                    if (!stillValid) {
                        Workspace personal = workspacesCache.stream()
                                .filter(Workspace::isPersonal)
                                .findFirst()
                                .orElse(workspacesCache.isEmpty() ? placeholderWorkspace() : workspacesCache.get(0));
                        WorkspaceState.getInstance().setCurrent(personal);
                    } else {
                        Workspace refreshed = workspacesCache.stream()
                            .filter(w -> currentId != null && w.getId() == currentId)
                                .findFirst()
                                .orElse(current);
                        WorkspaceState.getInstance().setCurrent(refreshed);
                    }
                }));
    }

    /**
     * Open the workspace dropdown. Order: workspace rows → separator →
     * "Workspace-Einstellungen" (Q-tweak: per-workspace settings live in the
     * dropdown now, not as a header gear) → "Neuer Workspace" / "Workspace
     * beitreten". The Settings entry is disabled when the active workspace is
     * the placeholder ({@code id <= 0}).
     */
    private void showWorkspaceMenu() {
        ContextMenu menu = new ContextMenu();
        menu.getStyleClass().add("workspace-menu");
        menu.setAutoFix(true);
        menu.setAutoHide(true);

        Workspace current = WorkspaceState.getInstance().getCurrent();
        long currentId = current != null ? current.getId() : -1L;

        if (workspacesCache.isEmpty()) {
            MenuItem empty = new MenuItem("Keine Workspaces verfügbar");
            empty.setDisable(true);
            menu.getItems().add(empty);
        } else {
            for (Workspace ws : workspacesCache) {
                MenuItem item = buildWorkspaceMenuItem(ws, ws.getId() == currentId);
                menu.getItems().add(item);
            }
        }

        menu.getItems().add(new SeparatorMenuItem());

        // Per-workspace Einstellungen — disabled while the placeholder is active.
        MenuItem settingsItem = buildIconMenuItem(Feather.SETTINGS, "Workspace-Einstellungen");
        settingsItem.setDisable(current == null || current.getId() <= 0);
        settingsItem.setOnAction(e -> { if (onWorkspaceSettings != null) onWorkspaceSettings.run(); });

        MenuItem newItem  = buildIconMenuItem(Feather.PLUS,      "Neuer Workspace");
        newItem.setOnAction(e -> { if (onNewWorkspace != null) onNewWorkspace.run(); });

        MenuItem joinItem = buildIconMenuItem(Feather.LOG_IN,    "Workspace beitreten");
        joinItem.setOnAction(e -> { if (onJoinWorkspace != null) onJoinWorkspace.run(); });

        menu.getItems().addAll(settingsItem, newItem, joinItem);

        Bounds bounds = workspaceSwitcher.localToScreen(workspaceSwitcher.getBoundsInLocal());
        if (bounds == null) return;
        menu.show(workspaceSwitcher, bounds.getMinX() + 8, bounds.getMaxY() + 4);
    }

    private MenuItem buildIconMenuItem(Ikon glyph, String label) {
        FontIcon ic = new FontIcon(glyph);
        ic.getStyleClass().add("workspace-menu-action-icon");
        Label name = new Label(label);
        name.getStyleClass().addAll("workspace-menu-name", "t-body");
        name.setMaxWidth(144);
        name.setTextOverrun(OverrunStyle.ELLIPSIS);
        HBox row = new HBox(8, ic, name);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setMinWidth(190);
        CustomMenuItem item = new CustomMenuItem(row, true);
        item.setHideOnClick(true);
        return item;
    }

    private MenuItem buildWorkspaceMenuItem(Workspace ws, boolean isCurrent) {
        // Pin/Star marker stays in the dropdown row only — useful as a "this is
        // the protected personal workspace" cue. Spec §11 forbids it in the
        // header, not in the menu.
        StackPane pinHost = new StackPane();
        pinHost.setMinWidth(18);
        pinHost.setPrefWidth(18);
        if (ws.isPersonal()) {
            FontIcon pin = new FontIcon(Feather.STAR);
            pin.getStyleClass().add("workspace-menu-pin");
            pinHost.getChildren().add(pin);
        }
        Label name = new Label(ws.getName());
        name.getStyleClass().addAll("workspace-menu-name", "t-body");
        name.setMaxWidth(134);
        name.setTextOverrun(OverrunStyle.ELLIPSIS);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        StackPane checkHost = new StackPane();
        checkHost.setMinWidth(14);
        checkHost.setPrefWidth(14);
        if (isCurrent) {
            FontIcon check = new FontIcon(Feather.CHECK);
            check.getStyleClass().add("workspace-menu-check");
            checkHost.getChildren().add(check);
        }
        HBox row = new HBox(8, pinHost, name, spacer, checkHost);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setMinWidth(190);

        CustomMenuItem item = new CustomMenuItem(row, true);
        item.setHideOnClick(true);
        item.setOnAction(e -> WorkspaceState.getInstance().setCurrent(ws));
        return item;
    }

    private void applyCurrentWorkspaceToHeader(Workspace ws) {
        if (ws == null) {
            workspaceName.setText("—");
            return;
        }
        workspaceName.setText(ws.getName());
    }

    private void showWorkspaceError(boolean visible) {
        workspaceErrorBanner.setVisible(visible);
        workspaceErrorBanner.setManaged(visible);
    }

    private static Workspace placeholderWorkspace() {
        Workspace ws = new Workspace();
        ws.setId(-1);
        ws.setName("—");
        ws.setPersonal(false);
        return ws;
    }
}
