package com.devflow.view;

import atlantafx.base.theme.Styles;
import com.devflow.model.Chat;
import com.devflow.model.User;
import com.devflow.service.ChatService;
import com.devflow.service.UserService;
import com.devflow.util.Debouncer;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
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

public class GroupChatSettingsDialog extends VBox {

    private final ModalOverlay overlay;
    private final ChatService chatService;
    private final UserService userService;
    private final long currentUserId;
    private final Consumer<Chat> onChanged;
    private final StackPane host;
    private final Node blurTarget;

    private Chat chat;
    private final TextField nameField;
    private final ChoiceBox<String> policyChoice;
    private final ListView<User> memberList;
    private final ObservableList<User> members = FXCollections.observableArrayList();
    private final Button saveBtn;
    private final Button addMemberBtn;
    private final Button leaveBtn;
    private final Label statusLabel;

    public GroupChatSettingsDialog(StackPane host, Node blurTarget,
                                   ChatService chatService, UserService userService,
                                   Chat chat, long currentUserId, Consumer<Chat> onChanged) {
        this.chatService = chatService;
        this.userService = userService;
        this.chat = chat;
        this.currentUserId = currentUserId;
        this.onChanged = onChanged;
        this.host = host;
        this.blurTarget = blurTarget;

        getStyleClass().add("modal-card");
        // Phase 4 §8 + Polish-Pass §4: complex-modal width 560, padding 28, spacing 16.
        setMaxWidth(560);
        setPadding(new Insets(28));
        setSpacing(16);

        Label title = new Label("Gruppenchat-Einstellungen");
        title.getStyleClass().addAll("modal-title", "t-card-title", Styles.TITLE_4, Styles.TEXT_BOLD);
        ModalCloseButton closeBtn = new ModalCloseButton(null); // wired below once overlay exists
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        HBox header = new HBox(10, title, sp, closeBtn);
        header.setAlignment(Pos.CENTER_LEFT);

        boolean isOwner = chat.getOwnerId() != null && chat.getOwnerId() == currentUserId;

        // Name
        Label nameLbl = new Label("Name");
        nameLbl.getStyleClass().addAll("settings-label", "t-body-strong");
        nameField = new TextField(chat.getName() == null ? "" : chat.getName());
        nameField.getStyleClass().addAll("text-field", "t-input");
        nameField.setDisable(!isOwner);

        // Policy
        Label policyLbl = new Label("Wer kann Mitglieder hinzufügen?");
        policyLbl.getStyleClass().addAll("settings-label", "t-body-strong");
        policyChoice = new ChoiceBox<>();
        policyChoice.getItems().addAll("Nur Ersteller", "Alle Mitglieder");
        policyChoice.getSelectionModel().select(Chat.POLICY_ALL_MEMBERS.equalsIgnoreCase(chat.getMemberAddPolicy()) ? 1 : 0);
        policyChoice.setDisable(!isOwner);

        // Members
        Label membersLbl = new Label("Mitglieder (" + (chat.getParticipants() == null ? 0 : chat.getParticipants().size()) + ")");
        membersLbl.getStyleClass().addAll("settings-label", "t-body-strong");

        memberList = new ListView<>(members);
        memberList.getStyleClass().add("list-view-clean");
        memberList.setPrefHeight(220);
        memberList.setCellFactory(lv -> new MemberCell());
        if (chat.getParticipants() != null) members.setAll(chat.getParticipants());

        // Phase 3 P4: Add-Member uses the user-plus glyph + label so the
        // affordance is recognisable at a glance even before reading the text.
        org.kordamp.ikonli.javafx.FontIcon userPlusIcon =
                new org.kordamp.ikonli.javafx.FontIcon(org.kordamp.ikonli.feather.Feather.USER_PLUS);
        addMemberBtn = new Button("Mitglied hinzufügen", userPlusIcon);
        addMemberBtn.getStyleClass().add("button-secondary");
        boolean canAdd = chat.canAddMembers(currentUserId);
        addMemberBtn.setDisable(!canAdd);
        addMemberBtn.setOnAction(e -> openAddMember(host, blurTarget));

        HBox memberHeader = new HBox(10, membersLbl, new Region(), addMemberBtn);
        HBox.setHgrow(memberHeader.getChildren().get(1), Priority.ALWAYS);
        memberHeader.setAlignment(Pos.CENTER_LEFT);

        // Footer
        saveBtn = new Button("Änderungen speichern");
        saveBtn.getStyleClass().addAll("button-primary", "button-large");
        saveBtn.setDisable(!isOwner);
        saveBtn.setOnAction(e -> save());

        leaveBtn = new Button(isOwner ? "Gruppenchat auflösen" : "Gruppenchat verlassen");
        leaveBtn.getStyleClass().add("button-destructive");
        leaveBtn.setOnAction(e -> confirmLeave(isOwner));

        statusLabel = new Label("");
        statusLabel.getStyleClass().addAll("modal-subtitle", "t-caption", Styles.TEXT_MUTED);

        Region fSp = new Region();
        HBox.setHgrow(fSp, Priority.ALWAYS);
        // Phase 4 §8: destructive (leave/dissolve) on the left, primary (save) on the right.
        HBox footer = new HBox(8, leaveBtn, fSp, statusLabel, saveBtn);
        footer.setAlignment(Pos.CENTER_LEFT);

        getChildren().addAll(header, nameLbl, nameField, policyLbl, policyChoice,
                memberHeader, memberList, footer);

        overlay = new ModalOverlay(host, blurTarget, this);
        closeBtn.setOnAction(e -> overlay.close());
    }

    public void show() { overlay.show(); }

    private void save() {
        saveBtn.setDisable(true);
        String name = nameField.getText().trim();
        String policy = policyChoice.getSelectionModel().getSelectedIndex() == 1
                ? Chat.POLICY_ALL_MEMBERS : Chat.POLICY_OWNER_ONLY;
        chatService.updateGroupChat(chat.getId(), name, policy)
                .thenAcceptAsync(updated -> {
                    chat = updated;
                    onChanged.accept(updated);
                    statusLabel.setText("Gespeichert.");
                    saveBtn.setDisable(false);
                }, Platform::runLater)
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        statusLabel.setText("Fehler beim Speichern.");
                        saveBtn.setDisable(false);
                    });
                    return null;
                });
    }

    private void confirmLeave(boolean isOwner) {
        String title = isOwner ? "Gruppenchat auflösen?" : "Gruppenchat verlassen?";
        String msg = isOwner
                ? "Diese Aktion löscht den Gruppenchat für alle Mitglieder. Sie kann nicht rückgängig gemacht werden."
                : "Du verlierst den Zugriff auf bisherige Nachrichten dieses Gruppenchats.";
        String ok = isOwner ? "Auflösen" : "Verlassen";
        new ConfirmDialog(host, blurTarget, title, msg, ok, true, this::leave).show();
    }

    private void leave() {
        leaveBtn.setDisable(true);
        chatService.leaveGroupChat(chat.getId())
                .thenRunAsync(() -> {
                    onChanged.accept(null);
                    overlay.close();
                }, Platform::runLater)
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        statusLabel.setText("Aktion fehlgeschlagen.");
                        leaveBtn.setDisable(false);
                    });
                    return null;
                });
    }

    private void removeMember(User u) {
        chatService.removeMember(chat.getId(), u.getId())
                .thenRunAsync(() -> {
                    members.remove(u);
                    if (chat.getParticipants() != null) chat.getParticipants().remove(u);
                    onChanged.accept(chat);
                }, Platform::runLater)
                .exceptionally(ex -> {
                    Platform.runLater(() -> statusLabel.setText("Entfernen fehlgeschlagen."));
                    return null;
                });
    }

    private void openAddMember(StackPane host, Node blurTarget) {
        Set<Long> existingIds = new HashSet<>();
        for (User u : members) existingIds.add(u.getId());

        AddMemberPicker picker = new AddMemberPicker(host, blurTarget, userService, existingIds, user -> {
            chatService.addMember(chat.getId(), user.getId())
                    .thenAcceptAsync(updated -> {
                        chat = updated;
                        if (updated.getParticipants() != null) {
                            members.setAll(updated.getParticipants());
                        }
                        onChanged.accept(updated);
                    }, Platform::runLater)
                    .exceptionally(ex -> {
                        Platform.runLater(() -> statusLabel.setText("Hinzufügen fehlgeschlagen."));
                        return null;
                    });
        });
        picker.show();
    }

    private class MemberCell extends ListCell<User> {
        @Override
        protected void updateItem(User user, boolean empty) {
            super.updateItem(user, empty);
            if (empty || user == null) { setGraphic(null); setText(null); return; }
            Avatar avatar = new Avatar(user.getUsername(), 32);
            Label name = new Label(user.getUsername());
            name.getStyleClass().add("user-cell-name");
            boolean isOwner = chat.getOwnerId() != null && chat.getOwnerId() == user.getId();
            Label sub = new Label(isOwner ? "Administrator" : (user.isOnline() ? "Online" : "Offline"));
            sub.getStyleClass().add(isOwner ? "chat-header-status-online" : "user-cell-sub");
            VBox textBox = new VBox(1, name, sub);
            textBox.setAlignment(Pos.CENTER_LEFT);
            HBox.setHgrow(textBox, Priority.ALWAYS);

            Node trailing;
            boolean canRemove = chat.getOwnerId() != null && chat.getOwnerId() == currentUserId
                    && user.getId() != currentUserId;
            if (canRemove) {
                Button rm = new Button("Entfernen");
                rm.getStyleClass().add("user-cell-btn");
                rm.setOnAction(e -> removeMember(user));
                trailing = rm;
            } else {
                Label tag = new Label(user.getId() == currentUserId ? "Du" : "");
                tag.getStyleClass().add("user-cell-sub");
                trailing = tag;
            }

            HBox cell = new HBox(12, avatar, textBox, trailing);
            cell.setAlignment(Pos.CENTER_LEFT);
            cell.setPadding(new Insets(8, 12, 8, 12));
            setGraphic(cell);
            setText(null);
        }
    }

    /** Small sub-dialog for picking a user to add. */
    private static class AddMemberPicker extends VBox {
        private final ModalOverlay pickerOverlay;
        AddMemberPicker(StackPane host, Node blurTarget, UserService userService,
                        Set<Long> existingIds, Consumer<User> onPick) {
            getStyleClass().add("modal-card");
            // Phase 4 §8 + Polish-Pass §4: width 480, padding 28, spacing 16.
            setMaxWidth(480);
            setPadding(new Insets(28));
            setSpacing(16);

            Label title = new Label("Mitglied hinzufügen");
            title.getStyleClass().addAll("modal-title", "t-card-title", Styles.TITLE_4, Styles.TEXT_BOLD);
            ModalCloseButton close = new ModalCloseButton(null); // wired below
            Region sp = new Region();
            HBox.setHgrow(sp, Priority.ALWAYS);
            HBox header = new HBox(10, title, sp, close);
            header.setAlignment(Pos.CENTER_LEFT);

            TextField search = new TextField();
            search.setPromptText("Benutzer suchen…");
            search.getStyleClass().add("text-field");

            ObservableList<User> items = FXCollections.observableArrayList();
            ListView<User> list = new ListView<>(items);
            list.getStyleClass().add("list-view-clean");
            list.setPrefHeight(280);
            list.setCellFactory(lv -> new ListCell<>() {
                @Override
                protected void updateItem(User u, boolean empty) {
                    super.updateItem(u, empty);
                    if (empty || u == null) { setGraphic(null); setText(null); return; }
                    Avatar a = new Avatar(u.getUsername(), 28);
                    Label n = new Label(u.getUsername());
                    n.getStyleClass().add("user-cell-name");
                    HBox.setHgrow(n, Priority.ALWAYS);
                    n.setMaxWidth(Double.MAX_VALUE);
                    Label add = new Label("+ Hinzufügen");
                    add.getStyleClass().add("chat-header-status-online");
                    HBox row = new HBox(10, a, n, add);
                    row.setAlignment(Pos.CENTER_LEFT);
                    row.setPadding(new Insets(6, 10, 6, 10));
                    row.setOnMouseClicked(e -> {
                        onPick.accept(u);
                        pickerOverlay.close();
                    });
                    setGraphic(row);
                }
            });

            getChildren().addAll(header, search, list);
            pickerOverlay = new ModalOverlay(host, blurTarget, this);
            close.setOnAction(e -> pickerOverlay.close());

            Runnable load = () -> {
                String q = search.getText();
                var fut = (q == null || q.isBlank()) ? userService.listUsers() : userService.searchUsers(q);
                fut.thenAcceptAsync(all -> {
                    List<User> filtered = new ArrayList<>();
                    for (User u : all) if (!existingIds.contains(u.getId())) filtered.add(u);
                    items.setAll(filtered);
                }, Platform::runLater).exceptionally(ex -> null);
            };
            Debouncer debouncer = new Debouncer(280);
            search.textProperty().addListener((o, a, b) -> debouncer.schedule(load));
            load.run();
        }
        void show() { pickerOverlay.show(); }
    }
}
