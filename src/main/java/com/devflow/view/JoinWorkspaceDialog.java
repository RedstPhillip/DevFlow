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
import javafx.scene.control.TextFormatter;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.function.Consumer;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;

/**
 * Single-phase modal that joins an existing workspace by invite code.
 *
 * <p>Input is constrained at source: {@link TextFormatter} auto-uppercases and
 * drops any character that is not {@code [A-Za-z0-9]}, and caps length at 8.
 * That matches the backend's 8-char alphanumeric code contract and prevents
 * the user ever submitting something the server will refuse for format
 * reasons — so 400 responses here always mean "code not in the table" rather
 * than "the client let a bad shape through".</p>
 *
 * <p>Error mapping (status → user-facing German):
 * <ul>
 *   <li>404 → "Invite-Code nicht gefunden."</li>
 *   <li>409 → "Du bist bereits Mitglied dieses Workspaces."</li>
 *   <li>others → backend body, surfaced raw.</li>
 * </ul>
 * On success the overlay closes and {@code onWorkspaceJoined} fires so the
 * caller can refresh the sidebar and set the joined workspace as current.</p>
 */
public class JoinWorkspaceDialog extends VBox {

    private static final Pattern ALNUM_8 = Pattern.compile("[A-Z0-9]{0,8}");

    private final ModalOverlay overlay;
    private final WorkspaceService workspaceService;
    private final Consumer<Workspace> onWorkspaceJoined;

    public JoinWorkspaceDialog(StackPane host, Node blurTarget,
                               WorkspaceService workspaceService,
                               Consumer<Workspace> onWorkspaceJoined) {
        this.workspaceService = workspaceService;
        this.onWorkspaceJoined = onWorkspaceJoined;

        getStyleClass().add("modal-card");
        // Phase 4 §8 + Polish-Pass §4: width 480, padding 28, spacing 16.
        setMaxWidth(480);
        setPadding(new Insets(28));
        setSpacing(16);

        this.overlay = new ModalOverlay(host, blurTarget, this);

        buildUi();
    }

    public void show() {
        overlay.show();
    }

    private void buildUi() {
        Label title = new Label("Workspace beitreten");
        title.getStyleClass().addAll("modal-title", "t-card-title", Styles.TITLE_4, Styles.TEXT_BOLD);
        ModalCloseButton closeBtn = new ModalCloseButton(overlay::close);
        Region hSpacer = new Region();
        HBox.setHgrow(hSpacer, Priority.ALWAYS);
        HBox header = new HBox(10, title, hSpacer, closeBtn);
        header.setAlignment(Pos.CENTER_LEFT);

        Label subtitle = new Label(
                "Gib den 8-stelligen Einladungs-Code ein, den du von einem Workspace-Mitglied erhalten hast."
        );
        subtitle.getStyleClass().addAll("modal-subtitle", "t-body", Styles.TEXT_MUTED);
        subtitle.setWrapText(true);

        Label codeLabel = new Label("Einladungs-Code");
        codeLabel.getStyleClass().addAll("settings-label", "t-body-strong");

        TextField codeField = new TextField();
        codeField.setPromptText("z.B. 7F3K9XQ2");
        codeField.getStyleClass().addAll("text-field", "invite-code-field");

        // Auto-uppercase + drop non-alphanumerics + hard-cap length at 8 so the
        // user can paste codes copied with surrounding whitespace or in
        // lower-case without us caring. Invalid keystrokes are silently
        // rejected rather than shown-then-cleared.
        UnaryOperator<TextFormatter.Change> filter = change -> {
            String text = change.getControlNewText().toUpperCase();
            if (!ALNUM_8.matcher(text).matches()) return null;
            if (change.isContentChange()) {
                change.setText(change.getText().toUpperCase().replaceAll("[^A-Z0-9]", ""));
            }
            return change;
        };
        codeField.setTextFormatter(new TextFormatter<>(filter));

        Label statusLabel = new Label("");
        statusLabel.getStyleClass().addAll("modal-subtitle", "t-caption", Styles.TEXT_MUTED);
        statusLabel.setWrapText(true);

        Button cancelBtn = new Button("Abbrechen");
        cancelBtn.getStyleClass().add("button-flat");
        cancelBtn.setOnAction(e -> overlay.close());

        Button submitBtn = new Button("Beitreten");
        submitBtn.getStyleClass().addAll("button-primary", "button-large");
        submitBtn.setDisable(true);

        codeField.textProperty().addListener((obs, old, val) -> {
            String v = val == null ? "" : val;
            submitBtn.setDisable(v.length() != 8);
            // Clear stale error as soon as the user edits — avoids the jarring
            // "Invite-Code nicht gefunden" sticking around while they retype.
            if (!statusLabel.getText().isEmpty()) statusLabel.setText("");
        });

        submitBtn.setOnAction(e -> {
            String code = codeField.getText();
            if (code == null || code.length() != 8) return;
            submitBtn.setDisable(true);
            cancelBtn.setDisable(true);
            statusLabel.setText("");
            workspaceService.joinWorkspace(code)
                    .thenAcceptAsync(joined -> {
                        overlay.close();
                        if (onWorkspaceJoined != null) onWorkspaceJoined.accept(joined);
                    }, Platform::runLater)
                    .exceptionally(ex -> {
                        Platform.runLater(() -> {
                            statusLabel.setText(mapError(ex));
                            submitBtn.setDisable(false);
                            cancelBtn.setDisable(false);
                            codeField.requestFocus();
                            codeField.selectAll();
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
                new VBox(8, codeLabel, codeField),
                footer
        );
        Platform.runLater(codeField::requestFocus);
    }

    /**
     * Map WorkspaceService's HTTP-shaped exceptions to German UI copy. The
     * service prepends "HTTP &lt;status&gt;" so we can branch on that prefix
     * without regex; anything we don't recognise falls through to the raw
     * backend body (which itself tends to be a JSON {@code {"error":"..."}}
     * shape the user can at least read).
     */
    private static String mapError(Throwable ex) {
        Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
        String msg = cause.getMessage() == null ? "" : cause.getMessage();
        if (msg.startsWith("HTTP 404")) {
            return "Invite-Code nicht gefunden.";
        }
        if (msg.startsWith("HTTP 409")) {
            return "Du bist bereits Mitglied dieses Workspaces.";
        }
        if (msg.startsWith("HTTP 400")) {
            return "Ungültiger Invite-Code.";
        }
        if (msg.startsWith("HTTP 403")) {
            return "Zugriff verweigert.";
        }
        // Generic fallback: strip the "HTTP ... on join workspace: " prefix
        // if present so the user sees only the backend's message text.
        int colon = msg.indexOf(": ");
        if (msg.startsWith("HTTP ") && colon >= 0 && colon + 2 < msg.length()) {
            String body = msg.substring(colon + 2).trim();
            if (!body.isEmpty()) return body;
        }
        return "Beitritt fehlgeschlagen. Bitte versuche es erneut.";
    }
}
