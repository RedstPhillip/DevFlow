package com.devflow.view;

import atlantafx.base.theme.Styles;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * Reusable confirmation modal: title, message, optional destructive accent.
 *
 * <pre>
 * new ConfirmDialog(host, blurTarget,
 *         "Gruppenchat aufl\u00f6sen?",
 *         "Diese Aktion l\u00f6scht den Gruppenchat f\u00fcr alle Mitglieder. Sie kann nicht r\u00fcckg\u00e4ngig gemacht werden.",
 *         "Aufl\u00f6sen", true, this::doLeave).show();
 * </pre>
 *
 * Used by group-chat settings today; will be reused by file-delete / repo-disconnect
 * actions in later modules.
 */
public class ConfirmDialog extends VBox {

    private final ModalOverlay overlay;

    public ConfirmDialog(StackPane host, Node blurTarget,
                         String title, String message,
                         String confirmLabel, boolean destructive,
                         Runnable onConfirm) {
        getStyleClass().add("modal-card");
        // Phase 4 Q11 + Polish-Pass §4: narrow 400 (Yes/No only), padding 28.
        setMaxWidth(400);
        setPadding(new Insets(28));
        setSpacing(16);

        Label titleLbl = new Label(title);
        // AtlantaFX TITLE_4/TEXT_BOLD stay; .t-card-title overrides their
        // sizing per Phase 4 spec (14/600 instead of 16/normal).
        titleLbl.getStyleClass().addAll("modal-title", "t-card-title", Styles.TITLE_4, Styles.TEXT_BOLD);

        Label msgLbl = new Label(message);
        msgLbl.getStyleClass().addAll("modal-subtitle", "t-body", Styles.TEXT_MUTED);
        msgLbl.setWrapText(true);

        Button cancel = new Button("Abbrechen");
        cancel.getStyleClass().add("button-flat");

        Button confirm = new Button(confirmLabel);
        confirm.getStyleClass().addAll(destructive ? "button-destructive" : "button-primary", "button-large");
        confirm.setDefaultButton(!destructive);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox footer = new HBox(8, spacer, cancel, confirm);
        footer.setAlignment(Pos.CENTER_RIGHT);

        getChildren().addAll(titleLbl, msgLbl, footer);

        overlay = new ModalOverlay(host, blurTarget, this);

        cancel.setOnAction(e -> overlay.close());
        confirm.setOnAction(e -> {
            overlay.close();
            if (onConfirm != null) onConfirm.run();
        });
    }

    public void show() { overlay.show(); }
}
