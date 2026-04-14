package com.devflow.view;

import com.devflow.model.Chat;
import com.devflow.model.User;
import com.devflow.util.DateFormatter;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.function.Consumer;

public class ChatListView extends StackPane {

    private final ListView<Chat> listView;
    private final ObservableList<Chat> chats;
    private final Label emptyLabel;
    private Consumer<Chat> onChatSelected;
    private long currentUserId;

    public ChatListView() {
        getStyleClass().add("chat-list-panel");
        chats = FXCollections.observableArrayList();

        listView = new ListView<>(chats);
        listView.getStyleClass().add("list-view-clean");
        listView.setCellFactory(lv -> new ChatListCell());

        listView.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> {
            if (selected != null && onChatSelected != null) {
                onChatSelected.accept(selected);
            }
        });

        emptyLabel = new Label("Noch keine Unterhaltungen");
        emptyLabel.getStyleClass().add("empty-state-text");
        StackPane emptyState = new StackPane(emptyLabel);
        emptyState.getStyleClass().add("empty-state");
        emptyState.setPadding(new Insets(40));
        emptyState.visibleProperty().bind(javafx.beans.binding.Bindings.isEmpty(chats));
        emptyState.managedProperty().bind(emptyState.visibleProperty());

        getChildren().addAll(listView, emptyState);
    }

    public void setChats(List<Chat> chatList) {
        Chat selected = listView.getSelectionModel().getSelectedItem();
        chats.setAll(chatList);
        if (selected != null) {
            for (Chat c : chatList) {
                if (c.getId() == selected.getId()) {
                    listView.getSelectionModel().select(c);
                    break;
                }
            }
        }
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

            Avatar avatar = new Avatar(name, 40);

            Label nameLabel = new Label(name);
            nameLabel.getStyleClass().add("chat-cell-name");

            Label previewLabel = new Label();
            previewLabel.getStyleClass().add("chat-cell-preview");
            if (chat.getLastMessage() != null) {
                String preview = chat.getLastMessage().getContent();
                if (preview.length() > 42) {
                    preview = preview.substring(0, 42) + "…";
                }
                previewLabel.setText(preview);
            } else {
                previewLabel.setText("Noch keine Nachrichten");
            }

            VBox textBox = new VBox(2, nameLabel, previewLabel);
            textBox.setAlignment(Pos.CENTER_LEFT);
            HBox.setHgrow(textBox, Priority.ALWAYS);

            Label timeLabel = new Label();
            timeLabel.getStyleClass().add("chat-cell-time");
            if (chat.getLastMessage() != null) {
                timeLabel.setText(DateFormatter.formatRelative(chat.getLastMessage().getCreatedAt()));
            }

            HBox cell = new HBox(12, avatar, textBox, timeLabel);
            cell.setAlignment(Pos.CENTER_LEFT);
            cell.setPadding(new Insets(10, 14, 10, 14));

            setGraphic(cell);
            setText(null);
        }
    }
}
