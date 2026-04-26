package com.devflow.view;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

/**
 * Polish-Pass — DevFlow brand mark.
 *
 * <p>A small rounded square carrying the gradient brand colour and the "DF"
 * wordmark in white. Used in two places:</p>
 *
 * <ul>
 *   <li><b>Sidebar workspace switcher</b> — 22 px, sits left of the workspace
 *       name. Anchors the brand identity inside the app shell.</li>
 *   <li><b>Login card brand row</b> — 28 px, paired with the "DevFlow"
 *       wordmark before any form fields. Welcomes the user before the
 *       sign-in copy starts.</li>
 * </ul>
 *
 * <p>Lives in its own class (<em>not</em> as a static factory on {@link Avatar})
 * because the responsibility is distinct: avatars are user-initial-driven and
 * vary by hash; the brand mark is a fixed asset whose only parameter is its
 * physical size. Q7 accepted this separation explicitly.</p>
 *
 * <p>Both visual properties — corner radius and inner font size — scale with
 * the requested {@code size} so the same component reads consistent across
 * the 22 / 28 variants. Radius ≈ 22.7 % of size (5 px at size 22, 6.4 px at
 * size 28). Font ≈ 41 % of size (9 px at 22, 11 px at 28).</p>
 */
public class BrandMark extends StackPane {

    /** Gradient stop 0 — accent. */
    private static final Color GRADIENT_START = Color.web("#4d7cfe");
    /** Gradient stop 1 — accent-hover (subtle 135° lift). */
    private static final Color GRADIENT_END   = Color.web("#6491ff");

    public BrandMark(double size) {
        setMinSize(size, size);
        setPrefSize(size, size);
        setMaxSize(size, size);
        setAlignment(Pos.CENTER);
        getStyleClass().add("brand-mark");

        double radius = size * 0.227;
        Rectangle bg = new Rectangle(size, size);
        bg.setArcWidth(radius * 2);
        bg.setArcHeight(radius * 2);
        // 135° diagonal — same direction as the avatar gradient so the brand
        // mark and user avatars feel like part of one family.
        bg.setFill(new LinearGradient(
                0, 0, 1, 1,
                true,
                CycleMethod.NO_CYCLE,
                new Stop(0, GRADIENT_START),
                new Stop(1, GRADIENT_END)
        ));

        Label text = new Label("DF");
        text.getStyleClass().add("brand-mark-text");
        // Font size hard-coded here (not in CSS) because it scales with the
        // mark size — same pattern Avatar uses for its initials.
        text.setFont(Font.font("System", FontWeight.BOLD, size * 0.41));

        getChildren().addAll(bg, text);
    }
}
