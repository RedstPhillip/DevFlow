package com.devflow.view;

import javafx.animation.FadeTransition;
import javafx.scene.Node;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

public class ModalOverlay extends StackPane {

    private static final Duration FADE_IN = Duration.millis(140);
    private static final Duration FADE_OUT = Duration.millis(110);

    private final StackPane host;
    private final Node blurTarget;
    // 8 px blur is ~4x cheaper to rerender than 16 px on JavaFX/Prism but still
    // gives the focus-cue we want when a dialog opens. Pair with -df-overlay scrim.
    private final GaussianBlur blur = new GaussianBlur(8);

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
        // Subtle fade-in so the dialog feels intentional rather than popping in.
        setOpacity(0);
        FadeTransition fade = new FadeTransition(FADE_IN, this);
        fade.setFromValue(0);
        fade.setToValue(1);
        fade.play();
        requestFocus();
    }

    public void close() {
        FadeTransition fade = new FadeTransition(FADE_OUT, this);
        fade.setFromValue(getOpacity());
        fade.setToValue(0);
        fade.setOnFinished(e -> {
            if (blurTarget != null) blurTarget.setEffect(null);
            host.getChildren().remove(this);
        });
        fade.play();
    }
}
