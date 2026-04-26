package com.devflow.view;

import atlantafx.base.theme.Styles;
import com.devflow.model.Workspace;
import com.devflow.service.WorkspaceService;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.function.Consumer;

/**
 * Two-phase modal:
 *
 * <ol>
 *   <li><b>Input:</b> name field + "Erstellen"/"Abbrechen". Submit calls
 *       {@link WorkspaceService#createWorkspace(String)}.</li>
 *   <li><b>Success:</b> same dialog instance, content swapped in-place.
 *       Shows the generated invite code read-only with a Copy button, plus a
 *       "Fertig" button that closes and fires {@code onWorkspaceCreated}.</li>
 * </ol>
 *
 * <p>The caller is expected to refresh the sidebar and set the new workspace
 * as current inside {@code onWorkspaceCreated}.</p>
 */
public class NewWorkspaceDialog extends VBox {

    private final ModalOverlay overlay;
    private final WorkspaceService workspaceService;
    private final Consumer<Workspace> onWorkspaceCreated;

    public NewWorkspaceDialog(StackPane host, Node blurTarget,
                              WorkspaceService workspaceService,
                              Consumer<Workspace> onWorkspaceCreated) {
        this.workspaceService = workspaceService;
        this.onWorkspaceCreated = onWorkspaceCreated;

        getStyleClass().add("modal-card");
        // Phase 4 §8 + Polish-Pass §4: width 480, padding 28, spacing 16.
        setMaxWidth(480);
        setPadding(new Insets(28));
        setSpacing(16);

        this.overlay = new ModalOverlay(host, blurTarget, this);

        showInputPhase();
    }

    public void show() {
        overlay.show();
    }

    // ── Phase 1: Name ────────────────────────────────────────────────────────

    private void showInputPhase() {
        Label title = new Label("Neuer Workspace");
        title.getStyleClass().addAll("modal-title", "t-card-title", Styles.TITLE_4, Styles.TEXT_BOLD);
        ModalCloseButton closeBtn = new ModalCloseButton(overlay::close);
        Region hSpacer = new Region();
        HBox.setHgrow(hSpacer, Priority.ALWAYS);
        HBox header = new HBox(10, title, hSpacer, closeBtn);
        header.setAlignment(Pos.CENTER_LEFT);

        Label subtitle = new Label("Wähle einen Namen für deinen neuen Workspace. Du kannst ihn später umbenennen.");
        subtitle.getStyleClass().addAll("modal-subtitle", "t-body", Styles.TEXT_MUTED);
        subtitle.setWrapText(true);

        Label nameLabel = new Label("Name");
        nameLabel.getStyleClass().addAll("settings-label", "t-body-strong");
        TextField nameField = new TextField();
        nameField.setPromptText("z.B. DevFlow Team");
        nameField.getStyleClass().addAll("text-field", "t-input");

        Label statusLabel = new Label("");
        statusLabel.getStyleClass().addAll("modal-subtitle", "t-caption", Styles.TEXT_MUTED);

        Button cancelBtn = new Button("Abbrechen");
        cancelBtn.getStyleClass().add("button-flat");
        cancelBtn.setOnAction(e -> overlay.close());

        Button submitBtn = new Button("Erstellen");
        submitBtn.getStyleClass().addAll("button-primary", "button-large");
        submitBtn.setDisable(true);

        // Enable submit as soon as the user typed at least one non-whitespace
        // character. 100-char cap mirrors the backend validation; typing past
        // that simply disables submit rather than trimming silently.
        nameField.textProperty().addListener((obs, old, val) -> {
            String trimmed = val == null ? "" : val.trim();
            submitBtn.setDisable(trimmed.isEmpty() || trimmed.length() > 100);
        });

        submitBtn.setOnAction(e -> {
            String name = nameField.getText().trim();
            if (name.isEmpty()) return;
            submitBtn.setDisable(true);
            cancelBtn.setDisable(true);
            statusLabel.setText("");
            workspaceService.createWorkspace(name)
                    .thenAcceptAsync(this::showSuccessPhase, Platform::runLater)
                    .exceptionally(ex -> {
                        Platform.runLater(() -> {
                            statusLabel.setText("Workspace konnte nicht erstellt werden.");
                            submitBtn.setDisable(false);
                            cancelBtn.setDisable(false);
                        });
                        return null;
                    });
        });

        Region fSpacer = new Region();
        HBox.setHgrow(fSpacer, Priority.ALWAYS);
        HBox footer = new HBox(8, statusLabel, fSpacer, cancelBtn, submitBtn);
        footer.setAlignment(Pos.CENTER_LEFT);

        getChildren().setAll(
                header,
                subtitle,
                new VBox(8, nameLabel, nameField),
                footer
        );
        Platform.runLater(nameField::requestFocus);
    }

    // ── Phase 2: Invite code ─────────────────────────────────────────────────

    private void showSuccessPhase(Workspace created) {
        Label title = new Label("Workspace erstellt");
        title.getStyleClass().addAll("modal-title", "t-card-title", Styles.TITLE_4, Styles.TEXT_BOLD);
        ModalCloseButton closeBtn = new ModalCloseButton(() -> finishAndClose(created));
        Region hSpacer = new Region();
        HBox.setHgrow(hSpacer, Priority.ALWAYS);
        HBox header = new HBox(10, title, hSpacer, closeBtn);
        header.setAlignment(Pos.CENTER_LEFT);

        Label subtitle = new Label(
                "\"" + created.getName() + "\" wurde angelegt. Teile den Einladungs-Code mit deinem Team, "
                        + "damit sie beitreten können."
        );
        subtitle.getStyleClass().addAll("modal-subtitle", "t-body", Styles.TEXT_MUTED);
        subtitle.setWrapText(true);

        Label codeLabel = new Label("Einladungs-Code");
        codeLabel.getStyleClass().addAll("settings-label", "t-body-strong");

        TextField codeField = new TextField(created.getInviteCode());
        codeField.setEditable(false);
        // Visual hint that this is a code, not editable text.
        codeField.getStyleClass().addAll("text-field", "invite-code-field");

        Button copyBtn = new Button("Kopieren");
        copyBtn.getStyleClass().add("button-secondary");
        copyBtn.setMinHeight(36);
        copyBtn.setPrefHeight(36);
        copyBtn.setOnAction(e -> {
            Clipboard cb = Clipboard.getSystemClipboard();
            ClipboardContent c = new ClipboardContent();
            c.putString(created.getInviteCode());
            cb.setContent(c);
            copyBtn.setText("Kopiert ✓");
            // Reset label after a short delay so the user can copy again.
            Platform.runLater(() -> new Thread(() -> {
                try { Thread.sleep(1500); } catch (InterruptedException ignored) {}
                Platform.runLater(() -> copyBtn.setText("Kopieren"));
            }).start());
        });

        HBox codeRow = new HBox(8, codeField, copyBtn);
        HBox.setHgrow(codeField, Priority.ALWAYS);
        codeRow.setAlignment(Pos.CENTER_LEFT);

        Button doneBtn = new Button("Fertig");
        doneBtn.getStyleClass().addAll("button-primary", "button-large");
        doneBtn.setOnAction(e -> finishAndClose(created));

        Region fSpacer = new Region();
        HBox.setHgrow(fSpacer, Priority.ALWAYS);
        HBox footer = new HBox(8, fSpacer, doneBtn);
        footer.setAlignment(Pos.CENTER_LEFT);

        getChildren().setAll(
                header,
                subtitle,
                new VBox(8, codeLabel, codeRow),
                footer
        );
    }

    private void finishAndClose(Workspace created) {
        overlay.close();
        if (onWorkspaceCreated != null) onWorkspaceCreated.accept(created);
    }
}
