package com.devflow.view;

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
 *         "Gruppe aufl\u00f6sen?",
 *         "Diese Aktion l\u00f6scht die Gruppe f\u00fcr alle Mitglieder. Sie kann nicht r\u00fcckg\u00e4ngig gemacht werden.",
 *         "Aufl\u00f6sen", true, this::doLeave).show();
 * </pre>
 *
 * Used by group settings today; will be reused by file-delete / repo-disconnect
 * actions in later modules.
 */
public class ConfirmDialog extends VBox {

    private final ModalOverlay overlay;

    public ConfirmDialog(StackPane host, Node blurTarget,
                         String title, String message,
                         String confirmLabel, boolean destructive,
                         Runnable onConfirm) {
        getStyleClass().add("modal-card");
        setMaxWidth(420);
        setPadding(new Insets(22, 24, 18, 24));
        setSpacing(14);

        Label titleLbl = new Label(title);
        titleLbl.getStyleClass().add("modal-title");

        Label msgLbl = new Label(message);
        msgLbl.getStyleClass().add("modal-subtitle");
        msgLbl.setWrapText(true);

        Button cancel = new Button("Abbrechen");
        cancel.getStyleClass().add("button-secondary");

        Button confirm = new Button(confirmLabel);
        confirm.getStyleClass().add(destructive ? "button-destructive" : "button-primary");
        confirm.setDefaultButton(!destructive);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox footer = new HBox(10, spacer, cancel, confirm);
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
