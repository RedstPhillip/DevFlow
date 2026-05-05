package com.devflow.view;

import com.devflow.platform.PlatformWindowStyle;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.stage.Screen;
import javafx.stage.Stage;

public class MainLayout extends StackPane {

    private final CustomTitleBar titleBar;
    private final Sidebar sidebar;
    private final StackPane contentArea;
    private final StackPane modalLayer;
    private final VBox frame;

    /**
     * Held refs so the four Stage-property listeners we register in
     * {@link #attachMaximizeListener(Stage)} can be detached on
     * {@link #dispose()}. The {@link Stage} is the JavaFX singleton window — it
     * outlives MainLayout instances across logout/login cycles, so without
     * explicit removal every cycle leaks four listeners forever.
     */
    private final Stage stage;
    private ChangeListener<Number> maximizeListener;

    public MainLayout(CustomTitleBar titleBar, Sidebar sidebar, Stage stage) {
        this.titleBar = titleBar;
        this.sidebar = sidebar;
        this.stage = stage;
        getStyleClass().add("main-layout");

        contentArea = new StackPane();
        contentArea.getStyleClass().add("content-area");
        Rectangle contentClip = new Rectangle();
        contentClip.widthProperty().bind(contentArea.widthProperty());
        contentClip.heightProperty().bind(contentArea.heightProperty());
        contentArea.setClip(contentClip);

        BorderPane body = new BorderPane();
        body.setLeft(sidebar);
        body.setCenter(contentArea);

        frame = new VBox(titleBar, body);
        frame.getStyleClass().add("window-frame");
        if (PlatformWindowStyle.usesOpaqueFramelessWindow()) {
            getStyleClass().add("native-frameless");
            frame.getStyleClass().add("native-frameless");
        }
        VBox.setVgrow(body, Priority.ALWAYS);

        modalLayer = new StackPane();
        modalLayer.getStyleClass().add("modal-layer");
        modalLayer.setPickOnBounds(false);
        modalLayer.setMouseTransparent(true);
        modalLayer.setMinSize(0, 0);
        modalLayer.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        modalLayer.prefWidthProperty().bind(widthProperty());
        modalLayer.prefHeightProperty().bind(heightProperty());
        StackPane.setAlignment(modalLayer, Pos.CENTER);

        getChildren().addAll(frame, modalLayer);

        attachMaximizeListener(stage);
    }

    private void attachMaximizeListener(Stage stage) {
        maximizeListener = (obs, old, val) -> updateMaximizedState(stage);
        stage.xProperty().addListener(maximizeListener);
        stage.yProperty().addListener(maximizeListener);
        stage.widthProperty().addListener(maximizeListener);
        stage.heightProperty().addListener(maximizeListener);
        // Apply once the stage has dimensions
        javafx.application.Platform.runLater(() -> updateMaximizedState(stage));
    }

    private void updateMaximizedState(Stage stage) {
        if (stage == null) return;
        var bounds = Screen.getPrimary().getVisualBounds();
        boolean maximized = Math.abs(stage.getWidth() - bounds.getWidth()) < 2
                && Math.abs(stage.getHeight() - bounds.getHeight()) < 2
                && Math.abs(stage.getX() - bounds.getMinX()) < 2
                && Math.abs(stage.getY() - bounds.getMinY()) < 2;
        setMaximizedStyle(maximized);
    }

    public void setMaximizedStyle(boolean maximized) {
        if (maximized) {
            if (!getStyleClass().contains("maximized")) getStyleClass().add("maximized");
            if (!frame.getStyleClass().contains("maximized")) frame.getStyleClass().add("maximized");
        } else {
            getStyleClass().remove("maximized");
            frame.getStyleClass().remove("maximized");
        }
    }

    public void setSidebarListNode(Node node) {
        sidebar.setListNode(node);
    }

    public void setMainContent(Node node) {
        contentArea.getChildren().setAll(node);
    }

    public StackPane getModalHost() { return modalLayer; }
    public Node getBlurTarget() { return frame; }

    public Sidebar getSidebar() { return sidebar; }
    public CustomTitleBar getTitleBar() { return titleBar; }
    public StackPane getContentArea() { return contentArea; }
    public VBox getWindowFrame() { return frame; }

    /**
     * Detach the four Stage-property listeners this layout registered during
     * construction. Must be called when the layout is being replaced (e.g.
     * logout → login cycle) — the Stage is long-lived and would otherwise
     * accumulate one listener quad per cycle.
     */
    public void dispose() {
        if (maximizeListener != null) {
            stage.xProperty().removeListener(maximizeListener);
            stage.yProperty().removeListener(maximizeListener);
            stage.widthProperty().removeListener(maximizeListener);
            stage.heightProperty().removeListener(maximizeListener);
            maximizeListener = null;
        }
        // Cascade: the title bar holds a ConnectionState listener that
        // would otherwise survive a logout/login cycle on the singleton.
        if (titleBar != null) titleBar.dispose();
    }
}
