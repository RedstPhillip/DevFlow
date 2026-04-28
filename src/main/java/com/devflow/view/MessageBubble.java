package com.devflow.view;

import com.devflow.model.Message;
import com.devflow.model.User;
import com.devflow.util.DateFormatter;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.OverrunStyle;
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

    private static final double MAX_MESSAGE_WIDTH = 720;

    public MessageBubble(Message message, boolean isOwn, User sender, boolean showSender) {
        getStyleClass().add("message-row");
        setAlignment(Pos.TOP_LEFT);
        setPadding(new Insets(6, 22, 6, 22));

        String senderName = sender != null ? sender.getUsername() : (isOwn ? "Du" : "Unbekannt");
        Avatar avatar = new Avatar(senderName, 34);
        avatar.getStyleClass().add("message-avatar");

        Label name = new Label(senderName);
        name.getStyleClass().add("message-sender");
        name.setMinWidth(0);
        name.setMaxWidth(260);
        name.setTextOverrun(OverrunStyle.ELLIPSIS);

        Label time = new Label(DateFormatter.formatTime(message.getCreatedAt()));
        time.getStyleClass().add("bubble-time");
        time.setMinWidth(Region.USE_PREF_SIZE);

        HBox meta = new HBox(8, name, time);
        meta.setAlignment(Pos.BASELINE_LEFT);
        meta.setMinWidth(0);
        HBox.setHgrow(name, Priority.ALWAYS);

        Text text = new Text(message.getContent() == null ? "" : message.getContent());
        text.getStyleClass().add("message-text");

        TextFlow flow = new TextFlow(text);
        flow.getStyleClass().add("message-text-flow");
        flow.setMinWidth(0);
        flow.setMaxWidth(MAX_MESSAGE_WIDTH);
        flow.setPrefWidth(Region.USE_COMPUTED_SIZE);
        text.wrappingWidthProperty().bind(flow.maxWidthProperty().subtract(1));
        widthProperty().addListener((obs, oldWidth, newWidth) -> {
            double available = Math.max(160, newWidth.doubleValue() - 96);
            flow.setMaxWidth(Math.min(MAX_MESSAGE_WIDTH, available));
        });

        VBox content = new VBox(3, meta, flow);
        content.getStyleClass().add("message-content");
        HBox.setHgrow(content, Priority.ALWAYS);

        getChildren().addAll(avatar, content);

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

        setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.SECONDARY) {
                menu.show(this, e.getScreenX(), e.getScreenY());
            }
        });
    }

    public MessageBubble(Message message, boolean isOwn) {
        this(message, isOwn, null, false);
    }
}
