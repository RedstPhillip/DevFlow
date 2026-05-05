package com.devflow.view;

import java.util.ArrayList;
import java.util.function.Consumer;

import com.devflow.config.GroupState;
import com.devflow.config.WorkspaceState;
import com.devflow.model.Group;
import com.devflow.model.Workspace;
import com.devflow.model.WorkspaceMember;
import com.devflow.service.GroupService;
import com.devflow.service.WorkspaceService;

import atlantafx.base.theme.Styles;
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
import javafx.scene.control.Tooltip;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * Minimal workspace-settings dialog opened from the gear icon on the sidebar
 * switcher. Four actions cover the 2b MVP:
 *
 * <ul>
 *   <li><b>Rename</b> — owner-only; sends a PUT and replaces the local
 *       {@link Workspace} instance via the {@code onChanged} callback.</li>
 *   <li><b>Invite-Code</b> — always readable, copy button mirrors the
 *       pattern in {@code NewWorkspaceDialog}. Not editable; the backend
 *       does not expose a regenerate endpoint in 2b.</li>
 *   <li><b>Kick</b> — one button per other member, owner-only.</li>
 *   <li><b>Leave</b> — the current user removes themselves. Hidden for the
 *       personal workspace (cannot be left) and for owners of non-personal
 *       workspaces (backend would 409; we surface the constraint in the UI
 *       rather than as a toast).</li>
 * </ul>
 *
 * <p>All HTTP errors are surfaced inline on the status label rather than
 * bubbled — this is a best-effort settings dialog, not a critical flow.</p>
 */
public class WorkspaceSettingsDialog extends VBox {

    private final ModalOverlay overlay;
    private final WorkspaceService workspaceService;
    private final long currentUserId;

    /**
     * Fired with the latest {@link Workspace} after a successful change that
     * mutates visible fields (rename). Callers typically push it into
     * {@link WorkspaceState} and refresh the sidebar cache.
     */
    private final Consumer<Workspace> onChanged;
    /**
     * Fired after a successful leave. The caller is expected to switch away
     * to another workspace (typically personal) and refresh the sidebar.
     */
    private final Runnable onLeft;

    private Workspace workspace;            // mutable — replaced after rename
    private final Label titleLabel;
    private final TextField nameField;
    private final Button renameBtn;
    private final TextField codeField;
    private final ObservableList<WorkspaceMember> members = FXCollections.observableArrayList();
    private final ListView<WorkspaceMember> memberList;
    private final Label statusLabel;
    private final Button leaveBtn;

    /**
     * Phase 2c — group (folder) management. Owner-only and hidden for the
     * personal workspace (no group chats possible there). The inline list
     * lets the owner create / rename / delete folders without leaving the
     * dialog; each CRUD op re-fetches so the Sidebar tree picks up the
     * change via {@link GroupState}.
     */
    private final GroupService groupService = new GroupService();
    private final ObservableList<Group> groups = FXCollections.observableArrayList();
    private final ListView<Group> groupList;
    private final TextField newGroupField;
    private final Button addGroupBtn;
    private final VBox groupsSection;

    public WorkspaceSettingsDialog(StackPane host, Node blurTarget,
                                   WorkspaceService workspaceService,
                                   Workspace workspace,
                                   long currentUserId,
                                   Consumer<Workspace> onChanged,
                                   Runnable onLeft) {
        this.workspaceService = workspaceService;
        this.workspace = workspace;
        this.currentUserId = currentUserId;
        this.onChanged = onChanged;
        this.onLeft = onLeft;

        getStyleClass().add("modal-card");
        // Phase 4 §8 + Polish-Pass §4: complex-modal width 560, padding 28, spacing 16.
        setMaxWidth(560);
        setMaxHeight(620);
        setPadding(new Insets(28));
        setSpacing(16);

        // Initialise the overlay first so the close-button lambda can safely
        // reference it. JLS definite-assignment rules for final fields forbid
        // referencing a blank final from a lambda body before its assignment
        // in the constructor, even though the lambda executes later at runtime.
        this.overlay = new ModalOverlay(host, blurTarget, this);

        // ── Header ──
        titleLabel = new Label("Workspace-Einstellungen");
        titleLabel.getStyleClass().addAll("modal-title", "t-card-title", Styles.TITLE_4, Styles.TEXT_BOLD);
        ModalCloseButton closeBtn = new ModalCloseButton(overlay::close);
        Region hSpacer = new Region();
        HBox.setHgrow(hSpacer, Priority.ALWAYS);
        HBox header = new HBox(10, titleLabel, hSpacer, closeBtn);
        header.setAlignment(Pos.CENTER_LEFT);

        // ── Rename row ──
        Label nameLabel = new Label("Name");
        nameLabel.getStyleClass().addAll("settings-label", "t-body-strong");
        nameField = new TextField(workspace.getName());
        nameField.getStyleClass().addAll("text-field", "t-input");
        renameBtn = new Button("Speichern");
        renameBtn.getStyleClass().addAll("button-primary");
        renameBtn.setDisable(true);
        nameField.textProperty().addListener((obs, old, val) -> {
            String trimmed = val == null ? "" : val.trim();
            boolean changed = !trimmed.equals(workspace.getName());
            renameBtn.setDisable(trimmed.isEmpty() || trimmed.length() > 100 || !changed);
        });
        renameBtn.setOnAction(e -> doRename());
        HBox nameRow = new HBox(8, nameField, renameBtn);
        HBox.setHgrow(nameField, Priority.ALWAYS);

        // Only the owner may rename; plain disabled state reads as "you don't
        // have permission" rather than an explicit message, which feels less
        // accusatory for the common case (plain member opening settings).
        if (!workspace.isOwner()) {
            nameField.setEditable(false);
            renameBtn.setVisible(false);
            renameBtn.setManaged(false);
        }

        // ── Invite-Code row ──
        Label codeLabel = new Label("Einladungs-Code");
        codeLabel.getStyleClass().addAll("settings-label", "t-body-strong");
        codeField = new TextField(workspace.getInviteCode() != null ? workspace.getInviteCode() : "");
        codeField.setEditable(false);
        // Unified with NewWorkspaceDialog/JoinWorkspaceDialog (16px) — the
        // 15px outlier here was incidental, not a deliberate sub-variant.
        codeField.getStyleClass().addAll("text-field", "invite-code-field");
        Button copyBtn = new Button("Kopieren");
        copyBtn.getStyleClass().add("button-secondary");
        copyBtn.setOnAction(e -> {
            Clipboard cb = Clipboard.getSystemClipboard();
            ClipboardContent c = new ClipboardContent();
            c.putString(codeField.getText());
            cb.setContent(c);
            copyBtn.setText("Kopiert \u2713");
            Platform.runLater(() -> new Thread(() -> {
                try { Thread.sleep(1500); } catch (InterruptedException ignored) {}
                Platform.runLater(() -> copyBtn.setText("Kopieren"));
            }).start());
        });
        HBox codeRow = new HBox(8, codeField, copyBtn);
        HBox.setHgrow(codeField, Priority.ALWAYS);

        // Personal workspaces have no invite code — hide the row entirely
        // rather than showing an empty field, which would be confusing.
        boolean hasCode = !workspace.isPersonal()
                && workspace.getInviteCode() != null
                && !workspace.getInviteCode().isEmpty();
        VBox codeBox = new VBox(6, codeLabel, codeRow);
        codeBox.setVisible(hasCode);
        codeBox.setManaged(hasCode);

        // ── Member list ──
        Label membersLabel = new Label("Mitglieder");
        membersLabel.getStyleClass().addAll("settings-label", "t-body-strong");
        memberList = new ListView<>(members);
        memberList.getStyleClass().add("list-view-clean");
        memberList.setPrefHeight(220);
        memberList.setCellFactory(lv -> new MemberCell());
        VBox.setVgrow(memberList, Priority.ALWAYS);

        // ── Groups (Phase 2c) ──
        Label groupsLabel = new Label("Ordner");
        groupsLabel.getStyleClass().addAll("settings-label", "t-body-strong");
        newGroupField = new TextField();
        newGroupField.setPromptText("Neuer Ordner…");
        newGroupField.getStyleClass().addAll("text-field", "t-input");
        // Phase 3 P4: text-only "+" replaced with a Feather PLUS icon-only
        // button so it reads as an affordance rather than punctuation. The
        // tooltip carries the meaning; AtlantaFX BUTTON_ICON keeps the
        // padding tight and matches the rest of the icon-button language.
        org.kordamp.ikonli.javafx.FontIcon addGroupIcon =
                new org.kordamp.ikonli.javafx.FontIcon(org.kordamp.ikonli.feather.Feather.PLUS);
        addGroupBtn = new Button();
        addGroupBtn.setGraphic(addGroupIcon);
        addGroupBtn.getStyleClass().addAll("button-primary", Styles.BUTTON_ICON);
        addGroupBtn.setTooltip(new Tooltip("Ordner hinzufügen"));
        addGroupBtn.setDisable(true);
        addGroupBtn.setOnAction(e -> doCreateGroup());
        newGroupField.textProperty().addListener((obs, old, val) -> {
            String trimmed = val == null ? "" : val.trim();
            addGroupBtn.setDisable(trimmed.isEmpty() || trimmed.length() > 100);
        });
        newGroupField.setOnAction(e -> { if (!addGroupBtn.isDisabled()) doCreateGroup(); });
        HBox addRow = new HBox(8, newGroupField, addGroupBtn);
        HBox.setHgrow(newGroupField, Priority.ALWAYS);

        groupList = new ListView<>(groups);
        groupList.getStyleClass().add("list-view-clean");
        groupList.setPrefHeight(160);
        groupList.setCellFactory(lv -> new GroupCell());

        groupsSection = new VBox(6, groupsLabel, addRow, groupList);
        // Groups only make sense in multi-user workspaces; hide for personal
        // and for non-owner members (who can't mutate them anyway).
        boolean canManageGroups = false;
        groupsSection.setVisible(canManageGroups);
        groupsSection.setManaged(canManageGroups);

        // ── Footer ──
        statusLabel = new Label("");
        statusLabel.getStyleClass().addAll("modal-subtitle", "t-caption", Styles.TEXT_MUTED);
        statusLabel.setWrapText(true);

        leaveBtn = new Button("Workspace verlassen");
        // Phase 4 §8: destructive action in the footer is rendered as the
        // outlined danger variant, separated to the left from the primary axis.
        leaveBtn.getStyleClass().add("button-destructive");
        leaveBtn.setOnAction(e -> doLeave());
        // Personal workspace cannot be left; owners of non-personal can't
        // either (would dissolve the workspace — out of 2b scope).
        boolean canLeave = !workspace.isPersonal() && !workspace.isOwner();
        leaveBtn.setVisible(canLeave);
        leaveBtn.setManaged(canLeave);

        Region fSpacer = new Region();
        HBox.setHgrow(fSpacer, Priority.ALWAYS);
        // Phase 4 §8: destructive action sits on the left, separated from the
        // primary axis. Status text occupies the gap so transient feedback
        // stays anchored to the action that produced it.
        HBox footer = new HBox(8, leaveBtn, fSpacer, statusLabel);
        footer.setAlignment(Pos.CENTER_LEFT);

        getChildren().addAll(
                header,
                new VBox(8, nameLabel, nameRow),
                codeBox,
                new VBox(8, membersLabel, memberList),
                groupsSection,
                footer
        );

        loadMembers();
        if (canManageGroups) loadGroups();
    }

    public void show() {
        overlay.show();
    }

    private void loadMembers() {
        workspaceService.getMembers(workspace.getId())
                .thenAcceptAsync(list -> {
                    members.setAll(list);
                }, Platform::runLater)
                .exceptionally(ex -> {
                    Platform.runLater(() -> statusLabel.setText("Mitglieder konnten nicht geladen werden."));
                    return null;
                });
    }

    private void doRename() {
        String newName = nameField.getText().trim();
        if (newName.isEmpty() || newName.equals(workspace.getName())) return;
        renameBtn.setDisable(true);
        statusLabel.setText("");
        workspaceService.renameWorkspace(workspace.getId(), newName)
                .thenAcceptAsync(updated -> {
                    // Adopt the server copy — it may also include refreshed
                    // memberCount/createdAt that we want the caller to see.
                    this.workspace = updated;
                    titleLabel.setText("Workspace-Einstellungen");
                    nameField.setText(updated.getName());
                    statusLabel.setText("Gespeichert.");
                    if (onChanged != null) onChanged.accept(updated);
                }, Platform::runLater)
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        statusLabel.setText("Umbenennen fehlgeschlagen.");
                        renameBtn.setDisable(false);
                    });
                    return null;
                });
    }

    private void doLeave() {
        leaveBtn.setDisable(true);
        statusLabel.setText("");
        // Backend uses the same DELETE /members/{userId} endpoint for both
        // kick and self-leave; the 2b contract accepts own id as leave.
        workspaceService.removeMember(workspace.getId(), currentUserId)
                .thenAcceptAsync(v -> {
                    overlay.close();
                    if (onLeft != null) onLeft.run();
                }, Platform::runLater)
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        statusLabel.setText("Verlassen fehlgeschlagen.");
                        leaveBtn.setDisable(false);
                    });
                    return null;
                });
    }

    private void doKick(WorkspaceMember member) {
        statusLabel.setText("");
        workspaceService.removeMember(workspace.getId(), member.getUserId())
                .thenAcceptAsync(v -> {
                    members.remove(member);
                }, Platform::runLater)
                .exceptionally(ex -> {
                    Platform.runLater(() -> statusLabel.setText("Entfernen fehlgeschlagen."));
                    return null;
                });
    }

    private void loadGroups() {
        groupService.listGroups(workspace.getId())
                .thenAcceptAsync(list -> groups.setAll(list), Platform::runLater)
                .exceptionally(ex -> {
                    Platform.runLater(() -> statusLabel.setText("Ordner konnten nicht geladen werden."));
                    return null;
                });
    }

    private void doCreateGroup() {
        String name = newGroupField.getText().trim();
        if (name.isEmpty()) return;
        addGroupBtn.setDisable(true);
        newGroupField.setDisable(true);
        statusLabel.setText("");
        groupService.createGroup(workspace.getId(), name)
                .thenAcceptAsync(created -> {
                    groups.add(created);
                    newGroupField.clear();
                    newGroupField.setDisable(false);
                    // Push into GroupState so the sidebar tree updates
                    // without waiting for the next 10 s chat-list poll.
                    GroupState.getInstance().refreshInPlace(new ArrayList<>(groups));
                }, Platform::runLater)
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        statusLabel.setText("Ordner konnte nicht erstellt werden.");
                        newGroupField.setDisable(false);
                        addGroupBtn.setDisable(false);
                    });
                    return null;
                });
    }

    private void doRenameGroup(Group group, String newName, Runnable onDone) {
        statusLabel.setText("");
        groupService.renameGroup(workspace.getId(), group.getId(), newName)
                .thenAcceptAsync(updated -> {
                    int idx = groups.indexOf(group);
                    if (idx >= 0) groups.set(idx, updated);
                    GroupState.getInstance().refreshInPlace(new ArrayList<>(groups));
                    onDone.run();
                }, Platform::runLater)
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        statusLabel.setText("Umbenennen fehlgeschlagen.");
                        onDone.run();
                    });
                    return null;
                });
    }

    private void doDeleteGroup(Group group) {
        statusLabel.setText("");
        groupService.deleteGroup(workspace.getId(), group.getId())
                .thenAcceptAsync(v -> {
                    groups.remove(group);
                    // Chats previously in this folder fall back to "Allgemein"
                    // in the sidebar — the backend nulls chats.group_id on
                    // delete, so a sidebar refresh would show them there.
                    GroupState.getInstance().refreshInPlace(new ArrayList<>(groups));
                }, Platform::runLater)
                .exceptionally(ex -> {
                    Platform.runLater(() -> statusLabel.setText("Ordner konnte nicht gelöscht werden."));
                    return null;
                });
    }

    /**
     * Row renderer for the group list. Normal view shows name + Rename +
     * Delete buttons. Clicking Rename swaps in an inline TextField; Enter
     * saves, Esc cancels. Delete uses a two-step confirm (first click
     * turns the button red and asks to confirm).
     */
    private class GroupCell extends ListCell<Group> {
        @Override
        protected void updateItem(Group group, boolean empty) {
            super.updateItem(group, empty);
            if (empty || group == null) {
                setGraphic(null);
                setText(null);
                return;
            }
            setGraphic(buildView(group));
            setText(null);
        }

        private HBox buildView(Group group) {
            Label name = new Label(group.getName());
            name.getStyleClass().add("user-cell-name");
            HBox.setHgrow(name, Priority.ALWAYS);
            name.setMaxWidth(Double.MAX_VALUE);

            // Phase 3 P4: rename/delete are always-visible icon-only buttons
            // (Edit_2 / Trash_2). Tooltip carries the verb. Two-step confirm
            // for delete now signals via destructive accent + tooltip swap
            // instead of a "Wirklich?" text change.
            org.kordamp.ikonli.javafx.FontIcon editIcon =
                    new org.kordamp.ikonli.javafx.FontIcon(org.kordamp.ikonli.feather.Feather.EDIT_2);
            Button renameBtn = new Button();
            renameBtn.setGraphic(editIcon);
            renameBtn.getStyleClass().addAll("button-secondary", Styles.BUTTON_ICON);
            Tooltip renameTip = new Tooltip("Umbenennen");
            renameBtn.setTooltip(renameTip);

            org.kordamp.ikonli.javafx.FontIcon trashIcon =
                    new org.kordamp.ikonli.javafx.FontIcon(org.kordamp.ikonli.feather.Feather.TRASH_2);
            Button deleteBtn = new Button();
            deleteBtn.setGraphic(trashIcon);
            deleteBtn.getStyleClass().addAll("button-secondary", Styles.BUTTON_ICON);
            Tooltip deleteTip = new Tooltip("Löschen");
            deleteBtn.setTooltip(deleteTip);
            // Marker property tracks whether the button is in confirm state.
            deleteBtn.getProperties().put("confirm", false);

            renameBtn.setOnAction(e -> setGraphic(buildEditor(group)));
            deleteBtn.setOnAction(e -> {
                boolean inConfirm = Boolean.TRUE.equals(deleteBtn.getProperties().get("confirm"));
                if (inConfirm) {
                    doDeleteGroup(group);
                } else {
                    deleteBtn.getProperties().put("confirm", true);
                    deleteBtn.getStyleClass().add("button-destructive");
                    deleteTip.setText("Wirklich löschen? Erneut klicken zum Bestätigen.");
                    new Thread(() -> {
                        try { Thread.sleep(2000); } catch (InterruptedException ignored) {}
                        Platform.runLater(() -> {
                            deleteBtn.getProperties().put("confirm", false);
                            deleteBtn.getStyleClass().remove("button-destructive");
                            deleteTip.setText("Löschen");
                        });
                    }, "group-delete-confirm-timeout").start();
                }
            });

            HBox row = new HBox(8, name, renameBtn, deleteBtn);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(6, 10, 6, 10));
            return row;
        }

        private HBox buildEditor(Group group) {
            TextField edit = new TextField(group.getName());
            edit.getStyleClass().add("text-field");
            HBox.setHgrow(edit, Priority.ALWAYS);

            Button save = new Button("Speichern");
            save.getStyleClass().add("button-primary");
            Button cancel = new Button("Abbrechen");
            cancel.getStyleClass().add("button-secondary");

            Runnable commit = () -> {
                String val = edit.getText() == null ? "" : edit.getText().trim();
                if (val.isEmpty() || val.length() > 100 || val.equals(group.getName())) {
                    // No-op cancel when the name hasn't really changed —
                    // avoids a needless PUT and the flicker of a "saved" toast.
                    setGraphic(buildView(group));
                    return;
                }
                save.setDisable(true);
                cancel.setDisable(true);
                doRenameGroup(group, val, () -> setGraphic(buildView(group)));
            };
            save.setOnAction(e -> commit.run());
            edit.setOnAction(e -> commit.run());
            cancel.setOnAction(e -> setGraphic(buildView(group)));
            edit.setOnKeyPressed(e -> {
                if (e.getCode() == javafx.scene.input.KeyCode.ESCAPE) setGraphic(buildView(group));
            });

            HBox row = new HBox(8, edit, save, cancel);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(6, 10, 6, 10));
            Platform.runLater(edit::requestFocus);
            return row;
        }
    }

    /** Row renderer: avatar + name + role badge + optional kick button. */
    private class MemberCell extends ListCell<WorkspaceMember> {
        @Override
        protected void updateItem(WorkspaceMember m, boolean empty) {
            super.updateItem(m, empty);
            if (empty || m == null) {
                setGraphic(null);
                setText(null);
                return;
            }
            Avatar avatar = new Avatar(m.getUsername(), 30);
            Label name = new Label(m.getUsername());
            name.getStyleClass().add("user-cell-name");
            Label role = new Label(m.isOwner() ? "Owner" : "Mitglied");
            role.getStyleClass().add(m.isOwner() ? "chat-header-status-online" : "user-cell-sub");
            VBox textBox = new VBox(1, name, role);
            textBox.setAlignment(Pos.CENTER_LEFT);
            HBox.setHgrow(textBox, Priority.ALWAYS);

            HBox row = new HBox(10, avatar, textBox);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(6, 10, 6, 10));

            // Kick button: owner-only, never on self, never on the owner row
            // (owners can't be kicked — would orphan the workspace).
            if (workspace.isOwner() && m.getUserId() != currentUserId && !m.isOwner()) {
                Button kickBtn = new Button("Entfernen");
                kickBtn.getStyleClass().add("button-secondary");
                kickBtn.setOnAction(e -> doKick(m));
                row.getChildren().add(kickBtn);
            }

            setGraphic(row);
            setText(null);
        }
    }
}
