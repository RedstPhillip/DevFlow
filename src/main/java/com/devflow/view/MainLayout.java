package com.devflow.view;

import javafx.beans.value.ChangeListener;
import javafx.scene.Node;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Screen;
import javafx.stage.Stage;

public class MainLayout extends StackPane {

    private final CustomTitleBar titleBar;
    private final Sidebar sidebar;
    private final StackPane contentArea;
    private final VBox frame;

    public MainLayout(CustomTitleBar titleBar, Sidebar sidebar, Stage stage) {
        this.titleBar = titleBar;
        this.sidebar = sidebar;
        getStyleClass().add("main-layout");

        contentArea = new StackPane();
        contentArea.getStyleClass().add("content-area");

        BorderPane body = new BorderPane();
        body.setLeft(sidebar);
        body.setCenter(contentArea);

        frame = new VBox(titleBar, body);
        frame.getStyleClass().add("window-frame");
        VBox.setVgrow(body, Priority.ALWAYS);

        getChildren().add(frame);

        attachMaximizeListener(stage);
    }

    private void attachMaximizeListener(Stage stage) {
        ChangeListener<Number> listener = (obs, old, val) -> updateMaximizedState(stage);
        stage.xProperty().addListener(listener);
        stage.yProperty().addListener(listener);
        stage.widthProperty().addListener(listener);
        stage.heightProperty().addListener(listener);
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

    public StackPane getModalHost() { return this; }
    public Node getBlurTarget() { return frame; }

    public Sidebar getSidebar() { return sidebar; }
    public CustomTitleBar getTitleBar() { return titleBar; }
    public StackPane getContentArea() { return contentArea; }
    public VBox getWindowFrame() { return frame; }
}
