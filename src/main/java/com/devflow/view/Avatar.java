package com.devflow.view;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

public class Avatar extends StackPane {

    // Tight palette around the app's azure primary — cohesive rather than rainbow.
    // All hues are muted/darker so white initials stay legible.
    private static final String[] PALETTE = {
            "#2b7fff", // primary azure
            "#1f6de8", // azure press
            "#5a93ff", // azure light
            "#3f6ad0", // indigo
            "#6a73b8", // slate blue
            "#3d6a99", // steel
            "#4a7c74", // teal-slate
            "#5f6b7a", // cool grey
            "#6a5c8a", // muted violet
            "#874d6d"  // muted rose
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
