package com.devflow.view;

import com.devflow.config.ConnectionState;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.util.function.Consumer;

public class CustomTitleBar extends HBox {

    private double xOffset = 0;
    private double yOffset = 0;
    private double lastX, lastY, lastWidth, lastHeight;
    private boolean snapped = false;

    private final Label subtitleLabel;
    private final Label offlineLabel;
    private final Object nativeChrome;
    private final ChangeListener<Boolean> maximizeListener;
    private final Stage stage;
    /** Held so we can detach on {@link #dispose()} (logout/login cycles re-create the bar). */
    private final Consumer<Boolean> connectionListener;

    public CustomTitleBar(Stage stage, String title) {
        this.stage = stage;
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

        Button minBtn = buildIconButton("minimize", "Minimieren");
        minBtn.setOnAction(e -> stage.setIconified(true));

        Button maxBtn = buildIconButton("maximize", "Maximieren");
        maxBtn.setOnAction(e -> toggleMaximize(stage, maxBtn));

        Button closeBtn = buildIconButton("close", "Schliessen");
        closeBtn.getStyleClass().add("titlebar-button-close");
        closeBtn.setOnAction(e -> stage.close());

        getChildren().addAll(brand, subtitleLabel, offlineLabel, spacer, minBtn, maxBtn, closeBtn);
        maximizeListener = (obs, wasMaximized, isMaximized) -> setMaximizeIcon(maxBtn, isMaximized);
        stage.maximizedProperty().addListener(maximizeListener);
        nativeChrome = installNativeChrome(stage, minBtn, maxBtn, closeBtn);
    }

    private void installManualDrag(Stage stage, Button maxBtn) {
        // Drag window
        setOnMousePressed(e -> {
            if (e.getY() > getHeight()) return;
            xOffset = e.getScreenX() - stage.getX();
            yOffset = e.getScreenY() - stage.getY();
        });
        setOnMouseDragged(e -> {
            if (stage.isMaximized() || isPseudoMaximized(stage) || snapped) {
                restore(stage, maxBtn);
                xOffset = e.getScreenX() - stage.getX() - stage.getWidth() / 2;
                yOffset = 18;
            }
            stage.setX(e.getScreenX() - xOffset);
            stage.setY(e.getScreenY() - yOffset);
        });
        setOnMouseReleased(e -> snapIfNeeded(stage, maxBtn, e.getScreenX(), e.getScreenY()));
        setOnMouseClicked(e -> {
            if (e.getClickCount() == 2 && e.getTarget() == this) {
                toggleMaximize(stage, maxBtn);
            }
        });
    }

    public void setSubtitle(String text) {
        subtitleLabel.setText(text == null ? "" : text);
    }

    private Button buildIconButton(String icon, String tooltip) {
        Button btn = new Button();
        btn.setGraphic(createWindowIcon(icon));
        btn.getStyleClass().add("titlebar-button");
        btn.setTooltip(new Tooltip(tooltip));
        btn.setFocusTraversable(false);
        btn.setMinSize(46, 30);
        btn.setPrefSize(46, 30);
        btn.setMaxSize(46, 30);
        return btn;
    }

    private Node createWindowIcon(String icon) {
        StackPane box = new StackPane();
        box.getStyleClass().addAll("titlebar-icon", "titlebar-icon-" + icon);
        box.setMinSize(14, 14);
        box.setPrefSize(14, 14);
        box.setMaxSize(14, 14);

        if ("minimize".equals(icon)) {
            Region line = new Region();
            line.getStyleClass().add("titlebar-icon-line");
            box.getChildren().add(line);
        } else if ("maximize".equals(icon)) {
            Region square = new Region();
            square.getStyleClass().add("titlebar-icon-square-shape");
            box.getChildren().add(square);
        } else if ("restore".equals(icon)) {
            Region back = new Region();
            back.getStyleClass().addAll("titlebar-icon-square-shape", "titlebar-icon-restore-back");
            Region front = new Region();
            front.getStyleClass().addAll("titlebar-icon-square-shape", "titlebar-icon-restore-front");
            box.getChildren().addAll(back, front);
        } else if ("close".equals(icon)) {
            Region first = new Region();
            first.getStyleClass().addAll("titlebar-icon-line", "titlebar-icon-close-line");
            first.setRotate(45);
            Region second = new Region();
            second.getStyleClass().addAll("titlebar-icon-line", "titlebar-icon-close-line");
            second.setRotate(-45);
            box.getChildren().addAll(first, second);
        }
        return box;
    }

    private void setMaximizeIcon(Button maxBtn, boolean restore) {
        maxBtn.setGraphic(createWindowIcon(restore ? "restore" : "maximize"));
    }

    private void toggleMaximize(Stage stage, Button maxBtn) {
        if (isNativeChromeInstalled()) {
            stage.setMaximized(!stage.isMaximized());
            return;
        }

        if (isPseudoMaximized(stage)) {
            restore(stage, maxBtn);
        } else {
            rememberBounds(stage);
            var bounds = getScreenForStage(stage).getVisualBounds();
            stage.setX(bounds.getMinX());
            stage.setY(bounds.getMinY());
            stage.setWidth(bounds.getWidth());
            stage.setHeight(bounds.getHeight());
            snapped = false;
            setMaximizeIcon(maxBtn, true);
        }
    }

    private void restore(Stage stage, Button maxBtn) {
        if (lastWidth > 0 && lastHeight > 0) {
            stage.setX(lastX);
            stage.setY(lastY);
            stage.setWidth(lastWidth);
            stage.setHeight(lastHeight);
        }
        snapped = false;
        setMaximizeIcon(maxBtn, false);
    }

    private void rememberBounds(Stage stage) {
        if (!isPseudoMaximized(stage) && !snapped) {
            lastX = stage.getX();
            lastY = stage.getY();
            lastWidth = stage.getWidth();
            lastHeight = stage.getHeight();
        }
    }

    private void snapIfNeeded(Stage stage, Button maxBtn, double screenX, double screenY) {
        var screen = Screen.getScreensForRectangle(screenX, screenY, 1, 1)
                .stream()
                .findFirst()
                .orElse(Screen.getPrimary());
        var bounds = screen.getVisualBounds();
        double threshold = 14;

        if (screenY <= bounds.getMinY() + threshold) {
            if (!isPseudoMaximized(stage)) rememberBounds(stage);
            stage.setX(bounds.getMinX());
            stage.setY(bounds.getMinY());
            stage.setWidth(bounds.getWidth());
            stage.setHeight(bounds.getHeight());
            snapped = false;
            setMaximizeIcon(maxBtn, true);
            return;
        }

        boolean left = screenX <= bounds.getMinX() + threshold;
        boolean right = screenX >= bounds.getMaxX() - threshold;
        if (!left && !right) return;

        rememberBounds(stage);
        stage.setX(left ? bounds.getMinX() : bounds.getMinX() + bounds.getWidth() / 2.0);
        stage.setY(bounds.getMinY());
        stage.setWidth(bounds.getWidth() / 2.0);
        stage.setHeight(bounds.getHeight());
        snapped = true;
        setMaximizeIcon(maxBtn, false);
    }

    private boolean isPseudoMaximized(Stage stage) {
        var bounds = getScreenForStage(stage).getVisualBounds();
        return Math.abs(stage.getX() - bounds.getMinX()) < 2
                && Math.abs(stage.getY() - bounds.getMinY()) < 2
                && Math.abs(stage.getWidth() - bounds.getWidth()) < 2
                && Math.abs(stage.getHeight() - bounds.getHeight()) < 2;
    }

    private Screen getScreenForStage(Stage stage) {
        double centerX = stage.getX() + stage.getWidth() / 2.0;
        double centerY = stage.getY() + stage.getHeight() / 2.0;
        return Screen.getScreensForRectangle(centerX, centerY, 1, 1)
                .stream()
                .findFirst()
                .orElse(Screen.getPrimary());
    }

    /**
     * Detach the {@link ConnectionState} listener. Should be called when the
     * title bar is being replaced (login/logout cycle) so the singleton
     * doesn't accumulate stale listeners.
     */
    public void dispose() {
        if (nativeChrome != null) {
            disposeNativeChrome();
        }
        stage.maximizedProperty().removeListener(maximizeListener);
        ConnectionState.getInstance().removeListener(connectionListener);
    }

    private Object installNativeChrome(Stage stage, Button minBtn, Button maxBtn, Button closeBtn) {
        try {
            Class<?> chromeClass = Class.forName("com.devflow.platform.WindowsWindowChrome");
            return chromeClass.getMethod(
                    "install",
                    Stage.class,
                    javafx.scene.layout.Region.class,
                    Button.class,
                    Button.class,
                    Button.class,
                    Runnable.class
            ).invoke(null, stage, this, minBtn, maxBtn, closeBtn, (Runnable) () -> installManualDrag(stage, maxBtn));
        } catch (ReflectiveOperationException | LinkageError ex) {
            System.err.println("Native Windows chrome unavailable, falling back to JavaFX drag: " + ex.getMessage());
            installManualDrag(stage, maxBtn);
            return null;
        }
    }

    private boolean isNativeChromeInstalled() {
        if (nativeChrome == null) return false;
        try {
            return Boolean.TRUE.equals(nativeChrome.getClass().getMethod("isNativeInstalled").invoke(nativeChrome));
        } catch (ReflectiveOperationException | LinkageError ex) {
            return false;
        }
    }

    private void disposeNativeChrome() {
        try {
            nativeChrome.getClass().getMethod("dispose").invoke(nativeChrome);
        } catch (ReflectiveOperationException | LinkageError ex) {
            System.err.println("Failed to dispose native Windows chrome: " + ex.getMessage());
        }
    }
}
