package com.devflow.view;

import com.devflow.model.Chat;
import com.devflow.util.DateFormatter;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
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
    private final FilteredList<Chat> filtered;
    private final Label emptyLabel;
    private Consumer<Chat> onChatSelected;
    private long currentUserId;
    private String filterText = "";
    private boolean suppressSelectionEvents = false;

    public ChatListView() {
        getStyleClass().add("chat-list-panel");
        chats = FXCollections.observableArrayList();
        filtered = new FilteredList<>(chats, c -> true);

        listView = new ListView<>(filtered);
        listView.getStyleClass().add("list-view-clean");
        listView.setCellFactory(lv -> new ChatListCell());

        listView.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> {
            if (suppressSelectionEvents) return;
            if (selected != null && onChatSelected != null) {
                onChatSelected.accept(selected);
            }
        });

        emptyLabel = new Label("Noch keine Unterhaltungen");
        emptyLabel.getStyleClass().add("empty-state-text");
        StackPane emptyState = new StackPane(emptyLabel);
        emptyState.getStyleClass().add("empty-state");
        emptyState.setPadding(new Insets(40));
        emptyState.visibleProperty().bind(javafx.beans.binding.Bindings.isEmpty(filtered));
        emptyState.managedProperty().bind(emptyState.visibleProperty());

        getChildren().addAll(listView, emptyState);
    }

    public void setChats(List<Chat> chatList) {
        Chat selected = listView.getSelectionModel().getSelectedItem();
        suppressSelectionEvents = true;
        try {
            chats.setAll(chatList);
            if (selected != null) {
                for (Chat c : chatList) {
                    if (c.getId() == selected.getId()) {
                        listView.getSelectionModel().select(c);
                        break;
                    }
                }
            }
        } finally {
            suppressSelectionEvents = false;
        }
    }

    public void clearSelection() {
        suppressSelectionEvents = true;
        try {
            listView.getSelectionModel().clearSelection();
        } finally {
            suppressSelectionEvents = false;
        }
    }

    public void setFilter(String text) {
        this.filterText = text == null ? "" : text.trim().toLowerCase();
        if (filterText.isEmpty()) {
            filtered.setPredicate(c -> true);
        } else {
            filtered.setPredicate(c ->
                    c.getDisplayName(currentUserId).toLowerCase().contains(filterText));
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

            String name = chat.getDisplayName(currentUserId);

            Avatar avatar = new Avatar(chat.isGroup() ? "# " + name : name, 40);

            Label nameLabel = new Label((chat.isGroup() ? "\u2023 " : "") + name);
            nameLabel.getStyleClass().add("chat-cell-name");

            Label previewLabel = new Label();
            previewLabel.getStyleClass().add("chat-cell-preview");
            if (chat.getLastMessage() != null) {
                String preview = chat.getLastMessage().getContent();
                if (preview.length() > 42) preview = preview.substring(0, 42) + "\u2026";
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
