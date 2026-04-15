package com.devflow.view;

import com.devflow.model.Message;
import com.devflow.model.User;
import com.devflow.util.DateFormatter;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

public class MessageBubble extends HBox {

    public MessageBubble(Message message, boolean isOwn, User sender, boolean showSender) {
        setAlignment(isOwn ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        setPadding(new Insets(3, 14, 3, 14));

        Label content = new Label(message.getContent());
        content.setWrapText(true);
        content.setMaxWidth(440);

        VBox bubble = new VBox(content);
        bubble.setPadding(new Insets(8, 12, 8, 12));
        bubble.setMaxWidth(440);

        if (isOwn) {
            bubble.getStyleClass().add("bubble-own");
            content.getStyleClass().add("bubble-own-text");
        } else {
            bubble.getStyleClass().add("bubble-other");
            content.getStyleClass().add("bubble-other-text");
        }

        // Time label outside the bubble
        Label time = new Label(DateFormatter.formatTime(message.getCreatedAt()));
        time.getStyleClass().add("bubble-time");

        VBox column = new VBox(2);
        column.setAlignment(isOwn ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);

        if (showSender && !isOwn && sender != null) {
            Label senderLabel = new Label(sender.getUsername());
            senderLabel.getStyleClass().add("message-sender");
            column.getChildren().add(senderLabel);
        }

        column.getChildren().addAll(bubble, time);

        // Context menu
        ContextMenu menu = new ContextMenu();
        MenuItem copy = new MenuItem("Kopieren");
        copy.setOnAction(e -> {
            Clipboard cb = Clipboard.getSystemClipboard();
            ClipboardContent c = new ClipboardContent();
            c.putString(message.getContent());
            cb.setContent(c);
        });
        MenuItem info = new MenuItem("Zeitstempel: " + DateFormatter.formatFull(message.getCreatedAt()));
        info.setDisable(true);
        menu.getItems().addAll(copy, info);

        bubble.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.SECONDARY) {
                menu.show(bubble, e.getScreenX(), e.getScreenY());
            }
        });

        if (isOwn) {
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            getChildren().addAll(spacer, column);
        } else {
            getChildren().add(column);
        }
    }

    // Backwards-compatible convenience constructor
    public MessageBubble(Message message, boolean isOwn) {
        this(message, isOwn, null, false);
    }
}
