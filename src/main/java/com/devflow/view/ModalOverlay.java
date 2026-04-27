package com.devflow.view;

import javafx.animation.FadeTransition;
import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Region;
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
        setAlignment(Pos.CENTER);
        setPadding(new Insets(24));
        setPickOnBounds(true);
        setMinSize(0, 0);
        setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        minWidthProperty().bind(host.widthProperty());
        minHeightProperty().bind(host.heightProperty());
        prefWidthProperty().bind(host.widthProperty());
        prefHeightProperty().bind(host.heightProperty());

        StackPane centeredContent = new StackPane(content);
        centeredContent.setAlignment(Pos.CENTER);
        centeredContent.setPickOnBounds(false);

        ScrollPane shell = new ScrollPane(centeredContent);
        shell.getStyleClass().add("modal-scroll-shell");
        // Keep dialog at its preferred size; only use scroll when it would overflow.
        shell.setFitToWidth(false);
        shell.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        shell.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        shell.setPannable(true);
        shell.maxWidthProperty().bind(Bindings.max(260, host.widthProperty().subtract(48)));
        shell.maxHeightProperty().bind(Bindings.max(220, host.heightProperty().subtract(48)));
        // Center content inside the viewport while preserving preferred-size layout.
        shell.viewportBoundsProperty().addListener((obs, oldB, b) -> {
            centeredContent.setMinWidth(Math.max(0, b.getWidth()));
            centeredContent.setMinHeight(Math.max(0, b.getHeight()));
        });

        if (content instanceof Region region && region.getMaxHeight() == Double.MAX_VALUE) {
            region.setMaxHeight(Region.USE_PREF_SIZE);
        }
        if (content instanceof Region region) {
            // Prevent StackPane from stretching dialog content to viewport size.
            region.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
            double maxWidth = region.getMaxWidth();
            double viewportWidth = maxWidth > 0 && maxWidth < Double.MAX_VALUE
                    ? maxWidth
                    : Math.max(320, region.prefWidth(-1));
            double maxHeight = region.getMaxHeight();
            double viewportHeight = maxHeight > 0 && maxHeight < Double.MAX_VALUE
                    ? maxHeight
                    : Math.max(160, region.prefHeight(viewportWidth));
            shell.setPrefViewportWidth(viewportWidth);
            shell.setPrefViewportHeight(viewportHeight);
        }

        getChildren().add(shell);
        StackPane.setAlignment(shell, Pos.CENTER);

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
        host.setPickOnBounds(true);
        host.setMouseTransparent(false);
        host.toFront();
        if (!host.getChildren().contains(this)) {
            host.getChildren().add(this);
            StackPane.setAlignment(this, Pos.CENTER);
        }
        toFront();
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
            host.getChildren().remove(this);
            if (host.getChildren().isEmpty()) {
                if (blurTarget != null) blurTarget.setEffect(null);
                host.setPickOnBounds(false);
                host.setMouseTransparent(true);
            } else {
                host.getChildren().get(host.getChildren().size() - 1).toFront();
            }
        });
        fade.play();
    }
}
