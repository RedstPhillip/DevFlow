package com.devflow.util;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;

/**
 * Coalesces rapid invocations into a single delayed run on the JavaFX thread.
 *
 * <p>Useful for search-as-you-type fields where each keystroke would otherwise
 * fire an HTTP request. Reusable across the upcoming File Sharing / Code Editor
 * modules wherever input is bursty.
 */
public class Debouncer {

    private final Timeline timeline;
    private Runnable pending;

    public Debouncer(int delayMs) {
        this.timeline = new Timeline(new KeyFrame(Duration.millis(delayMs), e -> {
            if (pending != null) {
                pending.run();
            }
        }));
        this.timeline.setCycleCount(1);
    }

    /** Schedule {@code action} to run after the configured delay, replacing any earlier pending action. */
    public void schedule(Runnable action) {
        this.pending = action;
        timeline.stop();
        timeline.playFromStart();
    }

    /** Cancel any pending action without firing it. */
    public void cancel() {
        timeline.stop();
        pending = null;
    }
}
