package com.devflow.view;

import com.devflow.model.Chat;
import com.devflow.model.User;
import com.devflow.util.DateFormatter;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.function.Consumer;

public class ChatListView extends VBox {

    private final ListView<Chat> listView;
    private final ObservableList<Chat> chats;
    private Consumer<Chat> onChatSelected;
    private long currentUserId;

    public ChatListView() {
        getStyleClass().add("chat-list-panel");
        chats = FXCollections.observableArrayList();

        Label header = new Label("Chats");
        header.getStyleClass().add("panel-header");
        header.setPadding(new Insets(12, 16, 12, 16));

        listView = new ListView<>(chats);
        listView.getStyleClass().add("chat-list");
        listView.setCellFactory(lv -> new ChatListCell());
        VBox.setVgrow(listView, Priority.ALWAYS);

        listView.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> {
            if (selected != null && onChatSelected != null) {
                onChatSelected.accept(selected);
            }
        });

        getChildren().addAll(header, listView);
    }

    public void setChats(List<Chat> chatList) {
        chats.setAll(chatList);
    }

    public void setOnChatSelected(Consumer<Chat> handler) {
        this.onChatSelected = handler;
    }

    public void setCurrentUserId(long userId) {
        this.currentUserId = userId;
    }

    private class ChatListCell extends ListCell<Chat> {
        @Override
        protected void updateItem(Chat chat, boolean empty) {
            super.updateItem(chat, empty);
            if (empty || chat == null) {
                setGraphic(null);
                setText(null);
                return;
            }

            User other = chat.getOtherParticipant(currentUserId);
            String name = other != null ? other.getUsername() : "Unbekannt";

            Label nameLabel = new Label(name);
            nameLabel.getStyleClass().add("chat-cell-name");

            Label previewLabel = new Label();
            previewLabel.getStyleClass().add("chat-cell-preview");
            if (chat.getLastMessage() != null) {
                String preview = chat.getLastMessage().getContent();
                if (preview.length() > 40) {
                    preview = preview.substring(0, 40) + "...";
                }
                previewLabel.setText(preview);
            }

            VBox textBox = new VBox(2, nameLabel, previewLabel);
            HBox.setHgrow(textBox, Priority.ALWAYS);

            Label timeLabel = new Label();
            timeLabel.getStyleClass().add("chat-cell-time");
            if (chat.getLastMessage() != null) {
                timeLabel.setText(DateFormatter.formatDate(chat.getLastMessage().getCreatedAt()));
            }

            HBox cell = new HBox(10, textBox, timeLabel);
            cell.getStyleClass().add("chat-cell");
            cell.setPadding(new Insets(10, 16, 10, 16));

            setGraphic(cell);
            setText(null);
        }
    }
}
