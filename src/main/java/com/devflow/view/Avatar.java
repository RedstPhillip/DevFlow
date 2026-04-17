package com.devflow.view;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

public class Avatar extends StackPane {

    // Harmonised palette — slightly muted vs. saturated marketing colours so that
    // a list of ten avatars side-by-side reads as one cohesive group, not a rainbow.
    private static final String[] PALETTE = {
            "#5e6ad2", // indigo (matches primary)
            "#7c6fdc", // violet
            "#9b6dd0", // purple
            "#c160a8", // pink
            "#d96775", // rose
            "#d68a4a", // amber
            "#5fa472", // green
            "#4ea1b8", // teal
            "#5b8ed6", // sky
            "#6a73b8"  // slate-blue
    };

    private final Circle circle;
    private final Label initials;

    public Avatar(String name, double size) {
        setMinSize(size, size);
        setPrefSize(size, size);
        setMaxSize(size, size);
        setAlignment(Pos.CENTER);

        String safeName = (name == null || name.isBlank()) ? "?" : name;
        circle = new Circle(size / 2.0);
        circle.setFill(Color.web(colorFor(safeName)));

        initials = new Label(initialsOf(safeName));
        initials.setStyle("-fx-text-fill: white; -fx-font-weight: 700; -fx-font-size: " + (size * 0.42) + "px;");

        getChildren().addAll(circle, initials);
    }

    public void setName(String name) {
        String safeName = (name == null || name.isBlank()) ? "?" : name;
        circle.setFill(Color.web(colorFor(safeName)));
        initials.setText(initialsOf(safeName));
    }

    private static String initialsOf(String name) {
        String[] parts = name.trim().split("\\s+");
        if (parts.length >= 2 && !parts[0].isEmpty() && !parts[1].isEmpty()) {
            return ("" + parts[0].charAt(0) + parts[1].charAt(0)).toUpperCase();
        }
        return name.substring(0, Math.min(2, name.length())).toUpperCase();
    }

    private static String colorFor(String name) {
        int hash = 0;
        for (int i = 0; i < name.length(); i++) {
            hash = name.charAt(i) + ((hash << 5) - hash);
        }
        return PALETTE[Math.abs(hash) % PALETTE.length];
    }
}
