package com.devflow.view;

import javafx.scene.Node;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public class MainLayout extends StackPane {

    private final CustomTitleBar titleBar;
    private final Sidebar sidebar;
    private final StackPane contentArea;
    private final VBox appRoot;

    public MainLayout(CustomTitleBar titleBar, Sidebar sidebar) {
        this.titleBar = titleBar;
        this.sidebar = sidebar;
        getStyleClass().add("main-layout");

        contentArea = new StackPane();
        contentArea.getStyleClass().add("content-area");

        BorderPane body = new BorderPane();
        body.setLeft(sidebar);
        body.setCenter(contentArea);

        appRoot = new VBox(titleBar, body);
        VBox.setVgrow(body, javafx.scene.layout.Priority.ALWAYS);

        getChildren().add(appRoot);
    }

    public void setSidebarListNode(Node node) {
        sidebar.setListNode(node);
    }

    public void setMainContent(Node node) {
        contentArea.getChildren().setAll(node);
    }

    public StackPane getModalHost() { return this; }
    public Node getBlurTarget() { return appRoot; }

    public Sidebar getSidebar() { return sidebar; }
    public CustomTitleBar getTitleBar() { return titleBar; }
    public StackPane getContentArea() { return contentArea; }
}
