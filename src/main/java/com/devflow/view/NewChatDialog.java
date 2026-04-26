package com.devflow.view;

import atlantafx.base.theme.Styles;
import com.devflow.config.GroupState;
import com.devflow.config.WorkspaceState;
import com.devflow.model.Chat;
import com.devflow.model.Group;
import com.devflow.model.User;
import com.devflow.model.Workspace;
import com.devflow.service.ChatService;
import com.devflow.service.UserService;
import com.devflow.service.WorkspaceService;
import com.devflow.util.Debouncer;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public class NewChatDialog extends VBox {

    private enum Mode { DM, GROUP_CHAT }

    private final ModalOverlay overlay;
    private final UserService userService;
    private final ChatService chatService;
    private final WorkspaceService workspaceService;
    private final long currentUserId;
    private final Consumer<Chat> onChatCreated;

    /**
     * Full workspace-member list kept client-side. Loaded once when the user
     * switches to the GROUP_CHAT tab and filtered in-memory by the search
     * field; we deliberately do NOT refetch on every keystroke — member lists
     * are small (workspaces cap at ~50 for the 2b MVP) so one GET per tab
     * open is cheaper than talking to the server per search edit.
     */
    private final List<User> workspaceMembers = new ArrayList<>();

    private Mode mode = Mode.DM;
    private final TextField searchField;
    private final TextField groupChatNameField;
    private final ComboBox<Group> groupFolderCombo;
    private final ListView<User> userListView;
    private final ObservableList<User> users = FXCollections.observableArrayList();
    private final FlowPane selectedChips;
    private final Set<User> selected = new HashSet<>();
    private final Button primaryButton;
    private final Label statusLabel;

    private final Button dmTab;
    private final Button groupChatTab;
    private final VBox groupChatNameBox;
    private final VBox groupFolderBox;

    /**
     * Sentinel "no folder → Allgemein" row at the top of the dropdown. We use
     * a Group instance with id {@code 0} rather than a {@code null} row so the
     * ComboBox cell factory stays simple (no null check per render) and so
     * users see an explicit label for the default choice.
     */
    private static final long ALLGEMEIN_SENTINEL_ID = 0L;

    public NewChatDialog(StackPane host, Node blurTarget,
                         UserService userService, ChatService chatService,
                         WorkspaceService workspaceService,
                         long currentUserId, Consumer<Chat> onChatCreated) {
        this.userService = userService;
        this.chatService = chatService;
        this.workspaceService = workspaceService;
        this.currentUserId = currentUserId;
        this.onChatCreated = onChatCreated;

        getStyleClass().add("modal-card");
        // Phase 4 §8 + Polish-Pass §4: standard modal width 480, padding 28
        // (Polish-Pass bump from 24 — leicht generaler), form-field-spacing 16.
        setMaxWidth(480);
        setMaxHeight(620);
        setPadding(new Insets(28));
        setSpacing(16);

        // ── Header ──
        Label title = new Label("Neue Unterhaltung");
        title.getStyleClass().addAll("modal-title", "t-card-title", Styles.TITLE_4, Styles.TEXT_BOLD);

        ModalCloseButton closeBtn = new ModalCloseButton(null); // wired below once overlay exists

        Region hSpacer = new Region();
        HBox.setHgrow(hSpacer, Priority.ALWAYS);
        HBox header = new HBox(10, title, hSpacer, closeBtn);
        header.setAlignment(Pos.CENTER_LEFT);

        // ── Tabs ──
        dmTab = tabButton("Direktnachricht", () -> setMode(Mode.DM));
        groupChatTab = tabButton("Gruppenchat", () -> setMode(Mode.GROUP_CHAT));
        HBox tabBar = new HBox(dmTab, groupChatTab);
        tabBar.getStyleClass().add("modal-tab-bar");

        // ── Group-chat name (only visible in GROUP_CHAT mode) ──
        groupChatNameField = new TextField();
        groupChatNameField.setPromptText("Name des Gruppenchats");
        groupChatNameField.getStyleClass().add("text-field");

        Label groupChatNameLabel = new Label("Name");
        groupChatNameLabel.getStyleClass().addAll("settings-label", "t-body-strong");
        groupChatNameBox = new VBox(6, groupChatNameLabel, groupChatNameField);
        groupChatNameBox.setVisible(false);
        groupChatNameBox.setManaged(false);

        // ── Group-folder dropdown (Phase 2c) ──
        groupFolderCombo = new ComboBox<>();
        groupFolderCombo.setMaxWidth(Double.MAX_VALUE);
        groupFolderCombo.setTooltip(new Tooltip("Ordner, in dem der Gruppenchat abgelegt wird"));
        groupFolderCombo.setCellFactory(lv -> new GroupFolderCell());
        groupFolderCombo.setButtonCell(new GroupFolderCell());
        Label groupFolderLabel = new Label("Ordner");
        groupFolderLabel.getStyleClass().addAll("settings-label", "t-body-strong");
        groupFolderBox = new VBox(6, groupFolderLabel, groupFolderCombo);
        groupFolderBox.setVisible(false);
        groupFolderBox.setManaged(false);

        // ── Selected chips (only in GROUP_CHAT mode) ──
        selectedChips = new FlowPane(6, 6);
        selectedChips.setVisible(false);
        selectedChips.setManaged(false);

        // ── Search ──
        searchField = new TextField();
        searchField.setPromptText("Benutzer suchen…");
        searchField.getStyleClass().add("text-field");

        // ── User list ──
        userListView = new ListView<>(users);
        userListView.getStyleClass().add("list-view-clean");
        userListView.setPrefHeight(280);
        userListView.setCellFactory(lv -> new UserCell());
        VBox.setVgrow(userListView, Priority.ALWAYS);

        statusLabel = new Label("");
        statusLabel.getStyleClass().addAll("modal-subtitle", "t-caption", Styles.TEXT_MUTED);

        // Phase 4 §8: footer = right-aligned Cancel(Flat) + Primary(Large 36 h),
        // --space-2 (8) gap. Cancel is wired below, after the overlay reference exists.
        Button cancelButton = new Button("Abbrechen");
        cancelButton.getStyleClass().add("button-flat");

        primaryButton = new Button("Chat öffnen");
        primaryButton.getStyleClass().addAll("button-primary", "button-large");
        primaryButton.setDisable(true);

        Region fSpacer = new Region();
        HBox.setHgrow(fSpacer, Priority.ALWAYS);
        HBox footer = new HBox(8, statusLabel, fSpacer, cancelButton, primaryButton);
        footer.setAlignment(Pos.CENTER_LEFT);

        getChildren().addAll(header, tabBar, groupChatNameBox, groupFolderBox, selectedChips, searchField, userListView, footer);

        // ── Overlay ──
        overlay = new ModalOverlay(host, blurTarget, this);

        // ── Events ──
        closeBtn.setOnAction(e -> overlay.close());
        cancelButton.setOnAction(e -> overlay.close());
        Debouncer searchDebouncer = new Debouncer(280);
        searchField.textProperty().addListener((obs, old, val) -> {
            if (mode == Mode.GROUP_CHAT) {
                // Workspace members are cached client-side; filtering doesn't
                // need debouncing, but we debounce anyway for symmetry so the
                // list doesn't jitter on fast typing.
                searchDebouncer.schedule(() -> Platform.runLater(() -> applyWorkspaceSearchFilter(val)));
            } else {
                searchDebouncer.schedule(() -> loadUsers(val));
            }
        });

        primaryButton.setOnAction(e -> submit());
        groupChatNameField.textProperty().addListener((obs, old, val) -> updatePrimary());

        setMode(Mode.DM);
        loadUsers("");
    }

    public void show() {
        overlay.show();
        Platform.runLater(searchField::requestFocus);
    }

    private Button tabButton(String label, Runnable action) {
        Button b = new Button(label);
        b.getStyleClass().add("modal-tab");
        b.setOnAction(e -> action.run());
        b.setFocusTraversable(false);
        return b;
    }

    private void setMode(Mode newMode) {
        this.mode = newMode;
        dmTab.getStyleClass().remove("modal-tab-active");
        groupChatTab.getStyleClass().remove("modal-tab-active");
        if (newMode == Mode.DM) {
            dmTab.getStyleClass().add("modal-tab-active");
            groupChatNameBox.setVisible(false);
            groupChatNameBox.setManaged(false);
            groupFolderBox.setVisible(false);
            groupFolderBox.setManaged(false);
            selectedChips.setVisible(false);
            selectedChips.setManaged(false);
            primaryButton.setText("Chat öffnen");
            selected.clear();
            refreshChips();
            // Reload full user directory for DM search.
            loadUsers(searchField.getText());
        } else {
            groupChatTab.getStyleClass().add("modal-tab-active");
            groupChatNameBox.setVisible(true);
            groupChatNameBox.setManaged(true);
            groupFolderBox.setVisible(true);
            groupFolderBox.setManaged(true);
            selectedChips.setVisible(true);
            selectedChips.setManaged(true);
            primaryButton.setText("Gruppenchat erstellen");
            // Fetch workspace members on every tab entry so members who were
            // added after the dialog opened show up. Cheap: member lists are
            // capped at ~50 in the 2b MVP.
            loadWorkspaceMembers();
            // Repopulate the folder dropdown from the freshest GroupState
            // snapshot. No HTTP here — the Sidebar owns the group list and
            // pushes updates through GroupState.
            populateGroupFolderOptions();
        }
        userListView.refresh();
        updatePrimary();
    }

    /**
     * Fetch members of the currently-active workspace and re-apply the
     * current search filter so the transition feels immediate. On failure
     * show an inline status rather than an alert — the user can still cancel
     * or retry by switching tabs.
     */
    private void loadWorkspaceMembers() {
        Workspace ws = WorkspaceState.getInstance().getCurrent();
        if (ws == null || ws.getId() <= 0) {
            workspaceMembers.clear();
            users.clear();
            statusLabel.setText("Kein Workspace ausgewählt.");
            return;
        }
        statusLabel.setText("");
        workspaceService.getMembers(ws.getId())
                .thenAcceptAsync(members -> {
                    workspaceMembers.clear();
                    for (var m : members) {
                        if (m.getUserId() == currentUserId) continue; // exclude self
                        workspaceMembers.add(m.toUser());
                    }
                    applyWorkspaceSearchFilter(searchField.getText());
                }, Platform::runLater)
                .exceptionally(ex -> {
                    Platform.runLater(() -> statusLabel.setText("Mitglieder konnten nicht geladen werden."));
                    return null;
                });
    }

    /**
     * Refill the folder ComboBox from {@link GroupState}. Always prepends the
     * "Allgemein" sentinel (id = 0) and selects it by default so users who
     * ignore the dropdown get the same behaviour as pre-2c (chat lands in the
     * implicit "Allgemein" section).
     */
    private void populateGroupFolderOptions() {
        Group allgemein = new Group();
        allgemein.setId(ALLGEMEIN_SENTINEL_ID);
        allgemein.setName("Allgemein");
        List<Group> options = new ArrayList<>();
        options.add(allgemein);
        options.addAll(GroupState.getInstance().getGroups());
        groupFolderCombo.getItems().setAll(options);
        groupFolderCombo.getSelectionModel().select(allgemein);
    }

    /** Substring match on username, case-insensitive. Empty query = show all. */
    private void applyWorkspaceSearchFilter(String query) {
        String q = query == null ? "" : query.trim().toLowerCase();
        if (q.isEmpty()) {
            users.setAll(workspaceMembers);
            return;
        }
        List<User> filtered = new ArrayList<>();
        for (User u : workspaceMembers) {
            if (u.getUsername() != null && u.getUsername().toLowerCase().contains(q)) {
                filtered.add(u);
            }
        }
        users.setAll(filtered);
    }

    private void loadUsers(String query) {
        var future = (query == null || query.isBlank())
                ? userService.listUsers()
                : userService.searchUsers(query);

        future.thenAcceptAsync(list -> {
            List<User> filtered = new ArrayList<>();
            for (User u : list) {
                if (u.getId() != currentUserId) filtered.add(u);
            }
            users.setAll(filtered);
        }, Platform::runLater)
        .exceptionally(ex -> {
            Platform.runLater(() -> statusLabel.setText("Benutzer konnten nicht geladen werden."));
            return null;
        });
    }

    private void toggleSelect(User user) {
        // Defensive: never allow the current user to be selected, even if they
        // somehow slipped through the list filter.
        if (user == null || user.getId() == currentUserId) return;
        if (mode == Mode.DM) {
            // Directly open DM
            primaryButton.setDisable(true);
            chatService.getOrCreateDmChat(user.getId())
                    .thenAcceptAsync(chat -> {
                        overlay.close();
                        onChatCreated.accept(chat);
                    }, Platform::runLater)
                    .exceptionally(ex -> {
                        Platform.runLater(() -> {
                            statusLabel.setText("Chat konnte nicht geöffnet werden.");
                            primaryButton.setDisable(false);
                        });
                        return null;
                    });
            return;
        }
        if (selected.contains(user)) selected.remove(user);
        else selected.add(user);
        refreshChips();
        userListView.refresh();
        updatePrimary();
    }

    private void refreshChips() {
        selectedChips.getChildren().clear();
        for (User u : selected) {
            Label lbl = new Label(u.getUsername());
            lbl.getStyleClass().add("chip");
            org.kordamp.ikonli.javafx.FontIcon xIcon = new org.kordamp.ikonli.javafx.FontIcon(org.kordamp.ikonli.feather.Feather.X);
            xIcon.getStyleClass().add("chip-close-icon");
            Button x = new Button();
            x.setGraphic(xIcon);
            x.getStyleClass().add("chip-close");
            x.setOnAction(e -> {
                selected.remove(u);
                refreshChips();
                userListView.refresh();
                updatePrimary();
            });
            HBox chip = new HBox(4, lbl, x);
            chip.setAlignment(Pos.CENTER_LEFT);
            selectedChips.getChildren().add(chip);
        }
    }

    private void updatePrimary() {
        if (mode == Mode.DM) {
            primaryButton.setDisable(true); // DM opens on click in list
            return;
        }
        boolean ok = selected.size() >= 2 && !groupChatNameField.getText().trim().isEmpty();
        primaryButton.setDisable(!ok);
    }

    private void submit() {
        if (mode != Mode.GROUP_CHAT) return;
        String name = groupChatNameField.getText().trim();
        if (name.isEmpty() || selected.size() < 2) return;
        primaryButton.setDisable(true);
        List<Long> ids = selected.stream()
                .map(User::getId)
                .filter(id -> id != currentUserId)
                .toList();
        Workspace ws = WorkspaceState.getInstance().getCurrent();
        if (ws == null || ws.getId() <= 0) {
            statusLabel.setText("Kein Workspace ausgewählt.");
            primaryButton.setDisable(false);
            return;
        }
        // Translate the sentinel "Allgemein" row (id = 0) back to a null
        // groupId — the backend treats null as "no folder" / implicit
        // Allgemein. Any other row carries a real workspace_groups.id.
        Group selectedFolder = groupFolderCombo.getValue();
        Long groupId = (selectedFolder == null || selectedFolder.getId() == ALLGEMEIN_SENTINEL_ID)
                ? null
                : selectedFolder.getId();
        chatService.createGroupChat(name, ids, Chat.POLICY_OWNER_ONLY, ws.getId(), groupId)
                .thenAcceptAsync(chat -> {
                    overlay.close();
                    onChatCreated.accept(chat);
                }, Platform::runLater)
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        statusLabel.setText("Gruppenchat konnte nicht erstellt werden.");
                        primaryButton.setDisable(false);
                    });
                    return null;
                });
    }

    /**
     * Renders a {@link Group} row in the folder dropdown. The sentinel
     * "Allgemein" row (id = 0) is styled as muted italic so it reads as a
     * default / fallback rather than a real folder, while real groups render
     * with plain foreground text.
     */
    private static class GroupFolderCell extends ListCell<Group> {
        @Override
        protected void updateItem(Group group, boolean empty) {
            super.updateItem(group, empty);
            getStyleClass().remove("group-folder-sentinel");
            if (empty || group == null) {
                setText(null);
                setGraphic(null);
                return;
            }
            setText(group.getName());
            if (group.getId() == ALLGEMEIN_SENTINEL_ID) {
                // Muted italic hint; mirrors the treatment the sidebar uses
                // for the implicit Allgemein bucket. Cell instances are
                // recycled across rows, so we explicitly remove the class
                // above before deciding whether to re-add it.
                getStyleClass().add("group-folder-sentinel");
            }
            setGraphic(null);
        }
    }

    private class UserCell extends ListCell<User> {
        @Override
        protected void updateItem(User user, boolean empty) {
            super.updateItem(user, empty);
            if (empty || user == null) {
                setGraphic(null);
                setText(null);
                return;
            }
            Avatar avatar = new Avatar(user.getUsername(), 32);
            Label name = new Label(user.getUsername());
            name.getStyleClass().add("user-cell-name");
            Label sub = new Label(user.isOnline() ? "Online" : "Offline");
            sub.getStyleClass().add(user.isOnline() ? "chat-header-status-online" : "user-cell-sub");
            VBox textBox = new VBox(1, name, sub);
            textBox.setAlignment(Pos.CENTER_LEFT);
            HBox.setHgrow(textBox, Priority.ALWAYS);

            Node trailing;
            if (mode == Mode.GROUP_CHAT) {
                Label tag = new Label(selected.contains(user) ? "\u2713 Ausgewählt" : "Hinzufügen");
                tag.getStyleClass().add(selected.contains(user) ? "chat-header-status-online" : "user-cell-sub");
                trailing = tag;
            } else {
                Label tag = new Label("Öffnen \u2192");
                tag.getStyleClass().add("user-cell-sub");
                trailing = tag;
            }

            HBox cell = new HBox(12, avatar, textBox, trailing);
            cell.setAlignment(Pos.CENTER_LEFT);
            cell.setPadding(new Insets(8, 12, 8, 12));
            cell.setOnMouseClicked(e -> toggleSelect(user));

            setGraphic(cell);
            setText(null);
        }
    }
}
