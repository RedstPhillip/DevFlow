package com.devflow.view;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class ChatView extends VBox {

    private final Label headerName;
    private final VBox messagesBox;
    private final ScrollPane scrollPane;
    private final TextField inputField;
    private final Button sendButton;

    public ChatView() {
        getStyleClass().add("chat-view");

        headerName = new Label();
        headerName.getStyleClass().add("chat-header-name");

        HBox header = new HBox(10, headerName);
        header.getStyleClass().add("chat-header");
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(12, 16, 12, 16));

        messagesBox = new VBox(4);
        messagesBox.getStyleClass().add("messages-box");
        messagesBox.setPadding(new Insets(8));

        scrollPane = new ScrollPane(messagesBox);
        scrollPane.getStyleClass().add("messages-scroll");
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        messagesBox.heightProperty().addListener((obs, old, val) -> {
            scrollPane.setVvalue(1.0);
        });

        inputField = new TextField();
        inputField.setPromptText("Nachricht schreiben...");
        inputField.getStyleClass().add("chat-input");
        HBox.setHgrow(inputField, Priority.ALWAYS);

        sendButton = new Button("Senden");
        sendButton.getStyleClass().add("button-primary");

        HBox inputBar = new HBox(10, inputField, sendButton);
        inputBar.getStyleClass().add("chat-input-bar");
        inputBar.setAlignment(Pos.CENTER);
        inputBar.setPadding(new Insets(12, 16, 12, 16));

        getChildren().addAll(header, scrollPane, inputBar);
    }

    public Label getHeaderName() { return headerName; }
    public VBox getMessagesBox() { return messagesBox; }
    public ScrollPane getScrollPane() { return scrollPane; }
    public TextField getInputField() { return inputField; }
    public Button getSendButton() { return sendButton; }
}
