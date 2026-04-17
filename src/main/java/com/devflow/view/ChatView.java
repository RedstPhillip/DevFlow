package com.devflow.view;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public class ChatView extends VBox {

    private final StackPane avatarHost;
    private final Label headerName;
    private final Label headerStatus;
    private final VBox messagesBox;
    private final ScrollPane scrollPane;
    private final TextField inputField;
    private final Button sendButton;
    private final Button infoButton;

    public ChatView() {
        getStyleClass().add("chat-view");

        avatarHost = new StackPane();
        avatarHost.setMinSize(40, 40);
        avatarHost.setPrefSize(40, 40);
        avatarHost.setMaxSize(40, 40);

        headerName = new Label();
        headerName.getStyleClass().add("chat-header-name");

        headerStatus = new Label();
        headerStatus.getStyleClass().add("chat-header-status");

        VBox headerText = new VBox(1, headerName, headerStatus);
        headerText.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(headerText, Priority.ALWAYS);

        infoButton = new Button();
        infoButton.setGraphic(Icons.settings());
        infoButton.getStyleClass().add("chat-header-btn");
        infoButton.setTooltip(new Tooltip("Gruppen-Einstellungen"));
        infoButton.setVisible(false);
        infoButton.setManaged(false);

        HBox header = new HBox(12, avatarHost, headerText, infoButton);
        header.getStyleClass().add("chat-header");
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(10, 16, 10, 16));

        messagesBox = new VBox(2);
        messagesBox.getStyleClass().add("messages-box");
        messagesBox.setPadding(new Insets(12, 8, 12, 8));

        scrollPane = new ScrollPane(messagesBox);
        scrollPane.getStyleClass().add("messages-scroll");
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        // Smart auto-scroll: only follow new content if the user is already near the bottom.
        // This preserves the user's reading position when scrolling back through history.
        messagesBox.heightProperty().addListener((obs, oldH, newH) -> {
            double v = scrollPane.getVvalue();
            // Treat "near the bottom" as v >= 0.92, OR the very first paint (oldH ~= 0).
            if (oldH == null || oldH.doubleValue() < 1 || v >= 0.92) {
                scrollPane.setVvalue(1.0);
            }
        });

        inputField = new TextField();
        inputField.setPromptText("Nachricht schreiben…");
        inputField.getStyleClass().add("chat-input");
        HBox.setHgrow(inputField, Priority.ALWAYS);

        sendButton = new Button("Senden");
        sendButton.getStyleClass().add("chat-send-btn");

        HBox inputBar = new HBox(10, inputField, sendButton);
        inputBar.getStyleClass().add("chat-input-bar");
        inputBar.setAlignment(Pos.CENTER);
        inputBar.setPadding(new Insets(12, 16, 12, 16));

        getChildren().addAll(header, scrollPane, inputBar);
    }

    /** Force-scroll to the newest message. Use after sending an own message. */
    public void scrollToBottom() {
        // Double-pump: the first runLater lets the newly-added bubble become part
        // of the layout; the nested one runs after the scroll-pane recomputes its
        // vmax, so setVvalue(1.0) actually lands at the true bottom.
        javafx.application.Platform.runLater(() ->
                javafx.application.Platform.runLater(() -> scrollPane.setVvalue(1.0)));
    }

    public StackPane getAvatarHost() { return avatarHost; }
    public Label getHeaderName() { return headerName; }
    public Label getHeaderStatus() { return headerStatus; }
    public VBox getMessagesBox() { return messagesBox; }
    public ScrollPane getScrollPane() { return scrollPane; }
    public TextField getInputField() { return inputField; }
    public Button getSendButton() { return sendButton; }
    public Button getInfoButton() { return infoButton; }
}
