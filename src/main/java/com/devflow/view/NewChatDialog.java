package com.devflow.view;

import com.devflow.model.Chat;
import com.devflow.model.User;
import com.devflow.service.ChatService;
import com.devflow.service.UserService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
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

    private enum Mode { DM, GROUP }

    private final ModalOverlay overlay;
    private final UserService userService;
    private final ChatService chatService;
    private final long currentUserId;
    private final Consumer<Chat> onChatCreated;

    private Mode mode = Mode.DM;
    private final TextField searchField;
    private final TextField groupNameField;
    private final ListView<User> userListView;
    private final ObservableList<User> users = FXCollections.observableArrayList();
    private final FlowPane selectedChips;
    private final Set<User> selected = new HashSet<>();
    private final Button primaryButton;
    private final Label statusLabel;

    private final Button dmTab;
    private final Button groupTab;
    private final VBox groupNameBox;

    public NewChatDialog(StackPane host, Node blurTarget,
                         UserService userService, ChatService chatService,
                         long currentUserId, Consumer<Chat> onChatCreated) {
        this.userService = userService;
        this.chatService = chatService;
        this.currentUserId = currentUserId;
        this.onChatCreated = onChatCreated;

        getStyleClass().add("modal-card");
        setMaxWidth(520);
        setMaxHeight(620);
        setPadding(new Insets(20, 22, 18, 22));
        setSpacing(14);

        // ── Header ──
        Label title = new Label("Neue Unterhaltung");
        title.getStyleClass().add("modal-title");

        Button closeBtn = new Button("\u2715");
        closeBtn.getStyleClass().add("modal-close-btn");

        Region hSpacer = new Region();
        HBox.setHgrow(hSpacer, Priority.ALWAYS);
        HBox header = new HBox(10, title, hSpacer, closeBtn);
        header.setAlignment(Pos.CENTER_LEFT);

        // ── Tabs ──
        dmTab = tabButton("Direktnachricht", () -> setMode(Mode.DM));
        groupTab = tabButton("Gruppe", () -> setMode(Mode.GROUP));
        HBox tabBar = new HBox(dmTab, groupTab);
        tabBar.getStyleClass().add("modal-tab-bar");

        // ── Group name (only visible in GROUP mode) ──
        groupNameField = new TextField();
        groupNameField.setPromptText("Gruppenname");
        groupNameField.getStyleClass().add("text-field");

        Label groupNameLabel = new Label("Name");
        groupNameLabel.getStyleClass().add("settings-label");
        groupNameBox = new VBox(6, groupNameLabel, groupNameField);
        groupNameBox.setVisible(false);
        groupNameBox.setManaged(false);

        // ── Selected chips (only in GROUP mode) ──
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
        statusLabel.getStyleClass().add("modal-subtitle");

        primaryButton = new Button("Chat öffnen");
        primaryButton.getStyleClass().add("button-primary");
        primaryButton.setDisable(true);

        Region fSpacer = new Region();
        HBox.setHgrow(fSpacer, Priority.ALWAYS);
        HBox footer = new HBox(10, statusLabel, fSpacer, primaryButton);
        footer.setAlignment(Pos.CENTER_LEFT);

        getChildren().addAll(header, tabBar, groupNameBox, selectedChips, searchField, userListView, footer);

        // ── Overlay ──
        overlay = new ModalOverlay(host, blurTarget, this);

        // ── Events ──
        closeBtn.setOnAction(e -> overlay.close());
        searchField.textProperty().addListener((obs, old, val) -> loadUsers(val));

        primaryButton.setOnAction(e -> submit());
        groupNameField.textProperty().addListener((obs, old, val) -> updatePrimary());

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
        groupTab.getStyleClass().remove("modal-tab-active");
        if (newMode == Mode.DM) {
            dmTab.getStyleClass().add("modal-tab-active");
            groupNameBox.setVisible(false);
            groupNameBox.setManaged(false);
            selectedChips.setVisible(false);
            selectedChips.setManaged(false);
            primaryButton.setText("Chat öffnen");
            selected.clear();
            refreshChips();
        } else {
            groupTab.getStyleClass().add("modal-tab-active");
            groupNameBox.setVisible(true);
            groupNameBox.setManaged(true);
            selectedChips.setVisible(true);
            selectedChips.setManaged(true);
            primaryButton.setText("Gruppe erstellen");
        }
        userListView.refresh();
        updatePrimary();
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
            Button x = new Button("\u2715");
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
        boolean ok = selected.size() >= 2 && !groupNameField.getText().trim().isEmpty();
        primaryButton.setDisable(!ok);
    }

    private void submit() {
        if (mode != Mode.GROUP) return;
        String name = groupNameField.getText().trim();
        if (name.isEmpty() || selected.size() < 2) return;
        primaryButton.setDisable(true);
        List<Long> ids = selected.stream()
                .map(User::getId)
                .filter(id -> id != currentUserId)
                .toList();
        chatService.createGroup(name, ids, Chat.POLICY_OWNER_ONLY)
                .thenAcceptAsync(chat -> {
                    overlay.close();
                    onChatCreated.accept(chat);
                }, Platform::runLater)
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        statusLabel.setText("Gruppe konnte nicht erstellt werden.");
                        primaryButton.setDisable(false);
                    });
                    return null;
                });
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
            if (mode == Mode.GROUP) {
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
