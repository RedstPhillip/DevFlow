package com.devflow.view;

import javafx.scene.Node;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;

public class MainLayout extends BorderPane {

    private final Sidebar sidebar;
    private final StackPane contentArea;

    public MainLayout(Sidebar sidebar) {
        this.sidebar = sidebar;
        getStyleClass().add("main-layout");

        contentArea = new StackPane();
        contentArea.getStyleClass().add("content-area");

        setLeft(sidebar);
        setCenter(contentArea);
    }

    public void setSidebarContent(Node node) {
        sidebar.setContent(node);
    }

    public void setMainContent(Node node) {
        contentArea.getChildren().setAll(node);
    }

    public Sidebar getSidebar() { return sidebar; }
    public StackPane getContentArea() { return contentArea; }
}
