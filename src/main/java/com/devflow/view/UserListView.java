package com.devflow.view;

import com.devflow.model.User;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.function.Consumer;

public class UserListView extends StackPane {

    private final ListView<User> listView;
    private final ObservableList<User> users;
    private final Label emptyLabel;
    private Consumer<User> onStartDm;
    private long currentUserId;

    public UserListView() {
        getStyleClass().add("user-list-panel");
        users = FXCollections.observableArrayList();

        listView = new ListView<>(users);
        listView.getStyleClass().add("list-view-clean");
        listView.setCellFactory(lv -> new UserListCell());

        emptyLabel = new Label("Keine Benutzer gefunden");
        emptyLabel.getStyleClass().add("empty-state-text");
        StackPane emptyState = new StackPane(emptyLabel);
        emptyState.getStyleClass().add("empty-state");
        emptyState.setPadding(new Insets(40));
        emptyState.visibleProperty().bind(javafx.beans.binding.Bindings.isEmpty(users));
        emptyState.managedProperty().bind(emptyState.visibleProperty());

        getChildren().addAll(listView, emptyState);
    }

    public void setUsers(List<User> userList) {
        users.setAll(userList.stream().filter(u -> u.getId() != currentUserId).toList());
    }

    public void setCurrentUserId(long id) {
        this.currentUserId = id;
    }

    public void setOnStartDm(Consumer<User> handler) {
        this.onStartDm = handler;
    }

    private class UserListCell extends ListCell<User> {
        @Override
        protected void updateItem(User user, boolean empty) {
            super.updateItem(user, empty);
            if (empty || user == null) {
                setGraphic(null);
                setText(null);
                return;
            }

            Avatar avatar = new Avatar(user.getUsername(), 36);

            Label nameLabel = new Label(user.getUsername());
            nameLabel.getStyleClass().add("user-cell-name");

            VBox textBox = new VBox(nameLabel);
            textBox.setAlignment(Pos.CENTER_LEFT);
            HBox.setHgrow(textBox, Priority.ALWAYS);

            Button dmButton = new Button("Nachricht");
            dmButton.getStyleClass().add("user-cell-btn");
            dmButton.setOnAction(e -> {
                if (onStartDm != null) {
                    onStartDm.accept(user);
                }
            });

            HBox cell = new HBox(12, avatar, textBox, dmButton);
            cell.setAlignment(Pos.CENTER_LEFT);
            cell.setPadding(new Insets(10, 14, 10, 14));

            setGraphic(cell);
            setText(null);
        }
    }
}
