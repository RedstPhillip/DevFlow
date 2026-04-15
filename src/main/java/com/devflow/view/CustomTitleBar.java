package com.devflow.view;

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

public class CustomTitleBar extends HBox {

    private double xOffset = 0;
    private double yOffset = 0;
    private double lastX, lastY, lastWidth, lastHeight;

    private final Label subtitleLabel;

    public CustomTitleBar(Stage stage, String title) {
        getStyleClass().add("titlebar");
        setAlignment(Pos.CENTER_LEFT);
        setPadding(new Insets(0, 0, 0, 14));

        Label brand = new Label(title);
        brand.getStyleClass().add("titlebar-brand");

        subtitleLabel = new Label("");
        subtitleLabel.getStyleClass().add("titlebar-subtitle");
        subtitleLabel.setPadding(new Insets(0, 0, 0, 12));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button minBtn = buildButton("\u2013", "Minimieren");
        minBtn.setOnAction(e -> stage.setIconified(true));

        Button maxBtn = buildButton("\u25A1", "Maximieren");
        maxBtn.setOnAction(e -> toggleMaximize(stage, maxBtn));

        Button closeBtn = buildButton("\u2715", "Schliessen");
        closeBtn.getStyleClass().add("titlebar-button-close");
        closeBtn.setOnAction(e -> stage.close());

        getChildren().addAll(brand, subtitleLabel, spacer, minBtn, maxBtn, closeBtn);

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

    private Button buildButton(String text, String tooltip) {
        Button btn = new Button(text);
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
            maxBtn.setText("\u2750");
        }
    }

    private void restore(Stage stage, Button maxBtn) {
        if (lastWidth > 0 && lastHeight > 0) {
            stage.setX(lastX);
            stage.setY(lastY);
            stage.setWidth(lastWidth);
            stage.setHeight(lastHeight);
        }
        maxBtn.setText("\u25A1");
    }

    private boolean isPseudoMaximized(Stage stage) {
        var bounds = Screen.getPrimary().getVisualBounds();
        return Math.abs(stage.getWidth() - bounds.getWidth()) < 2
                && Math.abs(stage.getHeight() - bounds.getHeight()) < 2;
    }
}
