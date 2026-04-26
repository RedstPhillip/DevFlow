package com.devflow.view;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.javafx.FontIcon;

/**
 * Reusable empty-state widget — used by the main welcome panel, the chat list
 * sidebar, and chat-view "no messages yet" placeholders.
 *
 * <p>Layout: large muted Feather glyph, primary title, multi-line subtitle,
 * optional action button. The component owns its CSS classes only — colour
 * and spacing come from {@code .empty-state-*} rules in {@code app.css}.</p>
 *
 * <p>Phase 3 P2 introduced this component so the three "nothing here yet"
 * surfaces stop diverging visually. Before, each call site hand-rolled a
 * {@code VBox} with one {@code Label} and slightly different spacing — they
 * looked subtly different which read as inconsistency rather than depth.</p>
 */
public class EmptyState extends VBox {

    private final FontIcon glyph;
    private final Label titleLabel;
    private final Label subtitleLabel;
    private Button actionButton;

    public EmptyState(Ikon icon, String title, String subtitle) {
        getStyleClass().add("empty-state-card");
        setAlignment(Pos.CENTER);
        setSpacing(10);
        setPadding(new Insets(24, 20, 24, 20));

        glyph = new FontIcon(icon);
        glyph.getStyleClass().add("empty-state-glyph");
        StackPane glyphHost = new StackPane(glyph);
        glyphHost.getStyleClass().add("empty-state-glyph-host");

        titleLabel = new Label(title);
        titleLabel.getStyleClass().add("empty-state-title");
        titleLabel.setWrapText(true);

        subtitleLabel = new Label(subtitle);
        subtitleLabel.getStyleClass().add("empty-state-subtitle");
        subtitleLabel.setWrapText(true);
        subtitleLabel.setMaxWidth(420);

        getChildren().addAll(glyphHost, titleLabel, subtitleLabel);
    }

    /**
     * Attach a primary action button below the subtitle. Calling this twice
     * replaces the previous action — useful when reusing the same EmptyState
     * across screens with different CTAs (we don't actually do that today,
     * but the affordance keeps the API symmetric with similar widgets).
     */
    public EmptyState withAction(String label, Runnable handler) {
        if (actionButton != null) {
            getChildren().remove(actionButton);
        }
        actionButton = new Button(label);
        actionButton.getStyleClass().addAll("button", "button-primary", "empty-state-action");
        actionButton.setOnAction(e -> {
            if (handler != null) handler.run();
        });
        // Push the button slightly down for breathing room.
        VBox.setMargin(actionButton, new Insets(8, 0, 0, 0));
        getChildren().add(actionButton);
        return this;
    }

    public void setTitleText(String title) { titleLabel.setText(title); }
    public void setSubtitleText(String subtitle) { subtitleLabel.setText(subtitle); }
}
