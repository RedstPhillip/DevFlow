package com.devflow.view;

import com.devflow.config.ConnectionState;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.stage.Screen;
import javafx.stage.Stage;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.function.Consumer;

public class CustomTitleBar extends HBox {

    private double xOffset = 0;
    private double yOffset = 0;
    private double lastX, lastY, lastWidth, lastHeight;

    private final Label subtitleLabel;
    private final Label offlineLabel;
    /** Held so we can detach on {@link #dispose()} (logout/login cycles re-create the bar). */
    private final Consumer<Boolean> connectionListener;

    public CustomTitleBar(Stage stage, String title) {
        getStyleClass().add("titlebar");
        setAlignment(Pos.CENTER_LEFT);
        setPadding(new Insets(0, 0, 0, 14));

        Label brand = new Label(title);
        brand.getStyleClass().add("titlebar-brand");

        subtitleLabel = new Label("");
        subtitleLabel.getStyleClass().add("titlebar-subtitle");
        subtitleLabel.setPadding(new Insets(0, 0, 0, 12));

        // Disconnect indicator — visible only when offline so it grabs the
        // user's eye exactly when something is wrong, then disappears once
        // the next request succeeds. Tooltip explains the cause without
        // bloating the visible label.
        offlineLabel = new Label("\u2022 Offline");
        offlineLabel.getStyleClass().add("titlebar-offline");
        offlineLabel.setTooltip(new Tooltip("Keine Verbindung zum Server. Die App versucht es erneut, sobald wieder eine Anfrage laeuft."));
        offlineLabel.setPadding(new Insets(0, 0, 0, 12));
        boolean initiallyOnline = ConnectionState.getInstance().isOnline();
        offlineLabel.setVisible(!initiallyOnline);
        offlineLabel.setManaged(!initiallyOnline);
        connectionListener = online -> {
            offlineLabel.setVisible(!online);
            offlineLabel.setManaged(!online);
        };
        ConnectionState.getInstance().addListener(connectionListener);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button minBtn = buildIconButton(Feather.MINUS, "Minimieren");
        minBtn.setOnAction(e -> stage.setIconified(true));

        Button maxBtn = buildIconButton(Feather.SQUARE, "Maximieren");
        maxBtn.setOnAction(e -> toggleMaximize(stage, maxBtn));

        Button closeBtn = buildIconButton(Feather.X, "Schliessen");
        closeBtn.getStyleClass().add("titlebar-button-close");
        closeBtn.setOnAction(e -> stage.close());

        getChildren().addAll(brand, subtitleLabel, offlineLabel, spacer, minBtn, maxBtn, closeBtn);

        // Drag window
        setOnMousePressed(e -> {
            if (e.getY() > getHeight()) return;
            xOffset = e.getScreenX() - stage.getX();
            yOffset = e.getScreenY() - stage.getY();
        });
        setOnMouseDragged(e -> {
            if (stage.isMaximized() || isPseudoMaximized(stage)) {
                restore(stage, maxBtn);
                xOffset = e.getScreenX() - stage.getX() - stage.getWidth() / 2;
                yOffset = 18;
            }
            stage.setX(e.getScreenX() - xOffset);
            stage.setY(e.getScreenY() - yOffset);
        });
        setOnMouseClicked(e -> {
            if (e.getClickCount() == 2 && e.getTarget() == this) {
                toggleMaximize(stage, maxBtn);
            }
        });
    }

    public void setSubtitle(String text) {
        subtitleLabel.setText(text == null ? "" : text);
    }

    private Button buildIconButton(Feather icon, String tooltip) {
        FontIcon fontIcon = new FontIcon(icon);
        fontIcon.getStyleClass().add("titlebar-icon");
        Button btn = new Button();
        btn.setGraphic(fontIcon);
        btn.getStyleClass().add("titlebar-button");
        btn.setTooltip(new Tooltip(tooltip));
        btn.setFocusTraversable(false);
        return btn;
    }

    private void toggleMaximize(Stage stage, Button maxBtn) {
        if (isPseudoMaximized(stage)) {
            restore(stage, maxBtn);
        } else {
            lastX = stage.getX();
            lastY = stage.getY();
            lastWidth = stage.getWidth();
            lastHeight = stage.getHeight();
            var bounds = Screen.getPrimary().getVisualBounds();
            stage.setX(bounds.getMinX());
            stage.setY(bounds.getMinY());
            stage.setWidth(bounds.getWidth());
            stage.setHeight(bounds.getHeight());
            // Visual hint that the button now restores. Feather doesn't ship a
            // dedicated "windows-restore" glyph, so COPY (two stacked squares)
            // is the closest analogue and reads correctly at 14 px.
            ((FontIcon) maxBtn.getGraphic()).setIconCode(Feather.COPY);
        }
    }

    private void restore(Stage stage, Button maxBtn) {
        if (lastWidth > 0 && lastHeight > 0) {
            stage.setX(lastX);
            stage.setY(lastY);
            stage.setWidth(lastWidth);
            stage.setHeight(lastHeight);
        }
        ((FontIcon) maxBtn.getGraphic()).setIconCode(Feather.SQUARE);
    }

    private boolean isPseudoMaximized(Stage stage) {
        var bounds = Screen.getPrimary().getVisualBounds();
        return Math.abs(stage.getWidth() - bounds.getWidth()) < 2
                && Math.abs(stage.getHeight() - bounds.getHeight()) < 2;
    }

    /**
     * Detach the {@link ConnectionState} listener. Should be called when the
     * title bar is being replaced (login/logout cycle) so the singleton
     * doesn't accumulate stale listeners.
     */
    public void dispose() {
        ConnectionState.getInstance().removeListener(connectionListener);
    }
}
