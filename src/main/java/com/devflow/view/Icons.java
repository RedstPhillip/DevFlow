package com.devflow.view;

import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.shape.SVGPath;

/**
 * Central icon registry. Uses SVG paths (feather-style 24x24 stroke icons) so
 * glyphs render consistently across Windows/macOS/Linux without depending on
 * system emoji or symbol fonts. All icons are designed on a 24x24 grid.
 *
 * Icons are returned as {@link Node} ready to be placed into a graphic slot.
 * Styling (color, stroke width) is controlled by the CSS class {@code .df-icon}.
 */
public final class Icons {

    private Icons() {}

    // Feather-style stroke paths (24x24).
    // Source shapes chosen to match: chat bubble, cog, user silhouette,
    // sun/palette, github mark, user-circle, info-circle, menu, chevron.
    private static final String MESSAGE_SQUARE =
            "M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z";

    private static final String SETTINGS =
            "M12 15.5A3.5 3.5 0 1 1 15.5 12 3.5 3.5 0 0 1 12 15.5z " +
            "M19.43 12.98a7.7 7.7 0 0 0 0-1.96l2.11-1.65a.5.5 0 0 0 .12-.64l-2-3.46a.5.5 0 0 0-.61-.22l-2.49 1a7.6 7.6 0 0 0-1.7-.98l-.38-2.65A.5.5 0 0 0 14 2h-4a.5.5 0 0 0-.5.42l-.38 2.65a7.6 7.6 0 0 0-1.7.98l-2.49-1a.5.5 0 0 0-.61.22l-2 3.46a.5.5 0 0 0 .12.64l2.11 1.65a7.7 7.7 0 0 0 0 1.96L2.44 14.63a.5.5 0 0 0-.12.64l2 3.46a.5.5 0 0 0 .61.22l2.49-1a7.6 7.6 0 0 0 1.7.98l.38 2.65A.5.5 0 0 0 10 22h4a.5.5 0 0 0 .5-.42l.38-2.65a7.6 7.6 0 0 0 1.7-.98l2.49 1a.5.5 0 0 0 .61-.22l2-3.46a.5.5 0 0 0-.12-.64z";

    private static final String USER =
            "M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2 " +
            "M16 7a4 4 0 1 1-8 0 4 4 0 0 1 8 0z";

    private static final String PALETTE =
            "M12 2a10 10 0 1 0 0 20h1a2 2 0 0 0 2-2 2 2 0 0 0-.5-1.3 2 2 0 0 1-.5-1.2 2 2 0 0 1 2-2H18a4 4 0 0 0 4-4 10 10 0 0 0-10-10z " +
            "M6.5 12a1.5 1.5 0 1 0 0-3 1.5 1.5 0 0 0 0 3z " +
            "M9.5 7.5a1.5 1.5 0 1 0 0-3 1.5 1.5 0 0 0 0 3z " +
            "M14.5 7.5a1.5 1.5 0 1 0 0-3 1.5 1.5 0 0 0 0 3z " +
            "M17.5 12a1.5 1.5 0 1 0 0-3 1.5 1.5 0 0 0 0 3z";

    private static final String GITHUB =
            "M9 19c-5 1.5-5-2.5-7-3m14 6v-3.87a3.37 3.37 0 0 0-.94-2.61c3.14-.35 6.44-1.54 6.44-7A5.44 5.44 0 0 0 20 4.77 5.07 5.07 0 0 0 19.91 1S18.73.65 16 2.48a13.38 13.38 0 0 0-7 0C6.27.65 5.09 1 5.09 1A5.07 5.07 0 0 0 5 4.77a5.44 5.44 0 0 0-1.5 3.78c0 5.42 3.3 6.61 6.44 7A3.37 3.37 0 0 0 9 18.13V22";

    private static final String USER_CIRCLE =
            "M12 2a10 10 0 1 0 10 10A10 10 0 0 0 12 2z " +
            "M12 6a3.5 3.5 0 1 1-3.5 3.5A3.5 3.5 0 0 1 12 6z " +
            "M5.5 18.4a8 8 0 0 1 13 0 8 8 0 0 1-13 0z";

    private static final String INFO =
            "M12 2a10 10 0 1 0 10 10A10 10 0 0 0 12 2z " +
            "M12 10v6 " +
            "M12 7.5a.75.75 0 1 1 0 1.5.75.75 0 0 1 0-1.5z";

    private static final String MENU =
            "M3 6h18 M3 12h18 M3 18h18";

    private static final String CHEVRON_LEFT =
            "M15 6l-6 6 6 6";

    private static final String PLUS =
            "M12 5v14 M5 12h14";

    public static Node messageSquare() { return build(MESSAGE_SQUARE); }
    public static Node settings()      { return build(SETTINGS); }
    public static Node user()          { return build(USER); }
    public static Node palette()       { return build(PALETTE); }
    public static Node github()        { return build(GITHUB); }
    public static Node userCircle()    { return build(USER_CIRCLE); }
    public static Node info()          { return build(INFO); }
    public static Node menu()          { return build(MENU); }
    public static Node chevronLeft()   { return build(CHEVRON_LEFT); }
    public static Node plus()          { return build(PLUS); }

    private static Node build(String path) {
        SVGPath p = new SVGPath();
        p.setContent(path);
        p.getStyleClass().add("df-icon");
        // Wrapping in a Group keeps layout bounds tight and lets CSS target
        // the inner SVGPath without StackPane baseline quirks.
        Group g = new Group(p);
        g.getStyleClass().add("df-icon-wrap");
        return g;
    }
}
