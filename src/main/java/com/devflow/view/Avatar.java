package com.devflow.view;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Paint;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;

/**
 * Phase 4 — Linear-style avatar.
 *
 * <p>Old avatar used 10 solid hex fills picked deterministically from the
 * username hash. Phase 4 swaps the solid fill for a 135° {@link LinearGradient}
 * across two stops, picked from one of six accent-aligned pairs. The diagonal
 * (top-left → bottom-right) gives a subtle "lit from above" feel that reads as
 * a small chip rather than a flat circle.</p>
 *
 * <p>Initials are rendered in {@code -text-primary} (light grey) instead of
 * pure white, with a soft drop-shadow so the text stays legible across all
 * gradient combinations. Both colour and shadow live in {@code .avatar-initials}
 * — see {@code app.css}.</p>
 */
public class Avatar extends StackPane {

    /**
     * Six gradient pairs aligned with the design-system accent palette
     * (Q7 — Linear-Indigo first, then status colours, then two complementary
     * tints). Hash-mod-6 picks the pair so the same username always hits the
     * same gradient.
     */
    private static final String[][] PALETTE = {
            {"#5e6ad2", "#8b8bd9"}, // accent-indigo
            {"#4cb782", "#6dc99a"}, // success-green
            {"#e5484d", "#f06a6e"}, // danger-red
            {"#ffb224", "#ffc555"}, // warning-amber
            {"#d36ad2", "#e08be0"}, // magenta
            {"#6ad2c5", "#8de0d5"}  // cyan-mint
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
        circle.setFill(gradientFor(safeName));

        initials = new Label(initialsOf(safeName));
        // Static colour + weight + drop-shadow live in CSS (.avatar-initials);
        // the font size scales with the avatar diameter so it can't be hard-coded.
        initials.getStyleClass().add("avatar-initials");
        initials.setFont(Font.font(size * 0.42));

        getChildren().addAll(circle, initials);
    }

    public void setName(String name) {
        String safeName = (name == null || name.isBlank()) ? "?" : name;
        circle.setFill(gradientFor(safeName));
        initials.setText(initialsOf(safeName));
    }

    private static String initialsOf(String name) {
        String[] parts = name.trim().split("\\s+");
        if (parts.length >= 2 && !parts[0].isEmpty() && !parts[1].isEmpty()) {
            return ("" + parts[0].charAt(0) + parts[1].charAt(0)).toUpperCase();
        }
        return name.substring(0, Math.min(2, name.length())).toUpperCase();
    }

    /**
     * Pick a gradient deterministically from the username so the same user
     * keeps the same avatar across sessions. The 135° diagonal is realised
     * via {@link LinearGradient#LinearGradient(double, double, double, double,
     * boolean, CycleMethod, Stop...)} with proportional coords from
     * (0,0) (top-left) to (1,1) (bottom-right).
     */
    private static Paint gradientFor(String name) {
        int hash = 0;
        for (int i = 0; i < name.length(); i++) {
            hash = name.charAt(i) + ((hash << 5) - hash);
        }
        String[] pair = PALETTE[Math.abs(hash) % PALETTE.length];
        Color c1 = Color.web(pair[0]);
        Color c2 = Color.web(pair[1]);
        return new LinearGradient(
                0, 0, 1, 1,
                true,
                CycleMethod.NO_CYCLE,
                new Stop(0, c1),
                new Stop(1, c2)
        );
    }
}
