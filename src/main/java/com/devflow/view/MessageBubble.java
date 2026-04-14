package com.devflow.view;

import com.devflow.model.Message;
import com.devflow.util.DateFormatter;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class MessageBubble extends HBox {

    public MessageBubble(Message message, boolean isOwn) {
        setAlignment(isOwn ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        setPadding(new Insets(2, 16, 2, 16));

        Label content = new Label(message.getContent());
        content.setWrapText(true);
        content.setMaxWidth(400);

        Label time = new Label(DateFormatter.formatTime(message.getCreatedAt()));
        time.getStyleClass().add("bubble-time");

        VBox bubble = new VBox(4, content, time);
        bubble.setPadding(new Insets(8, 12, 8, 12));
        bubble.setMaxWidth(400);

        if (isOwn) {
            bubble.getStyleClass().add("bubble-own");
            content.getStyleClass().add("bubble-own-text");
        } else {
            bubble.getStyleClass().add("bubble-other");
            content.getStyleClass().add("bubble-other-text");
        }

        getChildren().add(bubble);
    }
}
