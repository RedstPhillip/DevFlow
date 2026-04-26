package com.devflow.view;

import javafx.scene.control.Button;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;

/**
 * Standard close-button used in the top-right corner of every modal card.
 *
 * <p>Centralised here so all six dialogs share one glyph (Feather X), one
 * size, and one CSS hook ({@code .modal-close-btn}). Earlier each dialog
 * inlined a {@code Button("\u2715")} with its own style class — the unicode
 * "✕" rendered with whatever fallback font the platform happened to pick,
 * which led to subtle weight/size differences across machines. The Feather
 * glyph is a font icon: identical pixels everywhere.</p>
 *
 * <p>Construction signature mirrors a plain {@code Button} so call-sites
 * stay readable; supply the {@code Runnable} you want fired on click — the
 * caller almost always wants to close the surrounding {@code ModalOverlay}.</p>
 */
public class ModalCloseButton extends Button {

    public ModalCloseButton(Runnable onClose) {
        FontIcon icon = new FontIcon(Feather.X);
        icon.getStyleClass().add("modal-close-icon");
        setGraphic(icon);
        getStyleClass().add("modal-close-btn");
        setFocusTraversable(false);
        if (onClose != null) setOnAction(e -> onClose.run());
    }
}
