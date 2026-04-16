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
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

public class MessageBubble extends HBox {

    private static final double MAX_BUBBLE_WIDTH = 460;

    public MessageBubble(Message message, boolean isOwn, User sender, boolean showSender) {
        setAlignment(isOwn ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        setPadding(new Insets(3, 14, 3, 14));

        // TextFlow + Text wraps mid-word for unbreakable strings (long URLs, code), where Label would clip.
        Text text = new Text(message.getContent() == null ? "" : message.getContent());
        text.getStyleClass().add(isOwn ? "bubble-own-text" : "bubble-other-text");

        TextFlow flow = new TextFlow(text);
        flow.setMaxWidth(MAX_BUBBLE_WIDTH);
        flow.setPrefWidth(Region.USE_COMPUTED_SIZE);

        VBox bubble = new VBox(flow);
        bubble.setMaxWidth(MAX_BUBBLE_WIDTH);
        bubble.getStyleClass().add(isOwn ? "bubble-own" : "bubble-other");

        Label time = new Label(DateFormatter.formatTime(message.getCreatedAt()));
        time.getStyleClass().add("bubble-time");

        VBox column = new VBox(2);
        column.setAlignment(isOwn ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        column.setMaxWidth(MAX_BUBBLE_WIDTH);

        if (showSender && !isOwn && sender != null) {
            Label senderLabel = new Label(sender.getUsername());
            senderLabel.getStyleClass().add("message-sender");
            column.getChildren().add(senderLabel);
        }

        column.getChildren().addAll(bubble, time);

        // Context menu — copy / show full timestamp
        ContextMenu menu = new ContextMenu();
        MenuItem copy = new MenuItem("Kopieren");
        copy.setOnAction(e -> {
            Clipboard cb = Clipboard.getSystemClipboard();
            ClipboardContent c = new ClipboardContent();
            c.putString(message.getContent() == null ? "" : message.getContent());
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
