package com.devflow.view;

import javafx.scene.Node;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.StackPane;

public class ModalOverlay extends StackPane {

    private final StackPane host;
    private final Node blurTarget;
    private final GaussianBlur blur = new GaussianBlur(16);

    public ModalOverlay(StackPane host, Node blurTarget, Node content) {
        this.host = host;
        this.blurTarget = blurTarget;

        getStyleClass().add("modal-overlay");
        setPickOnBounds(true);
        getChildren().add(content);

        // Click outside content closes
        setOnMouseClicked(e -> {
            if (e.getTarget() == this) close();
        });

        setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE) close();
        });
    }

    public void show() {
        if (blurTarget != null) blurTarget.setEffect(blur);
        if (!host.getChildren().contains(this)) host.getChildren().add(this);
        requestFocus();
    }

    public void close() {
        if (blurTarget != null) blurTarget.setEffect(null);
        host.getChildren().remove(this);
    }
}
