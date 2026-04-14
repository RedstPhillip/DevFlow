package com.devflow.view;

import javafx.scene.Node;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;

public class MainLayout extends BorderPane {

    private final ActivityBar activityBar;
    private final StackPane sidePanel;
    private final StackPane contentArea;

    public MainLayout(ActivityBar activityBar) {
        this.activityBar = activityBar;
        getStyleClass().add("main-layout");

        sidePanel = new StackPane();
        sidePanel.getStyleClass().add("side-panel");
        sidePanel.setPrefWidth(280);
        sidePanel.setMinWidth(280);
        sidePanel.setMaxWidth(280);

        contentArea = new StackPane();
        contentArea.getStyleClass().add("content-area");

        BorderPane centerPane = new BorderPane();
        centerPane.setLeft(sidePanel);
        centerPane.setCenter(contentArea);

        setLeft(activityBar);
        setCenter(centerPane);
    }

    public void setSideContent(Node node) {
        sidePanel.getChildren().setAll(node);
    }

    public void setMainContent(Node node) {
        contentArea.getChildren().setAll(node);
    }

    public void clearMainContent() {
        contentArea.getChildren().clear();
    }

    public StackPane getSidePanel() { return sidePanel; }
    public StackPane getContentArea() { return contentArea; }
    public ActivityBar getActivityBar() { return activityBar; }
}
