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
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.function.Consumer;

public class UserListView extends VBox {

    private final ListView<User> listView;
    private final ObservableList<User> users;
    private Consumer<User> onStartDm;

    public UserListView() {
        getStyleClass().add("user-list-panel");
        users = FXCollections.observableArrayList();

        Label header = new Label("Benutzer");
        header.getStyleClass().add("panel-header");
        header.setPadding(new Insets(12, 16, 12, 16));

        listView = new ListView<>(users);
        listView.getStyleClass().add("user-list");
        listView.setCellFactory(lv -> new UserListCell());
        VBox.setVgrow(listView, Priority.ALWAYS);

        getChildren().addAll(header, listView);
    }

    public void setUsers(List<User> userList) {
        users.setAll(userList);
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

            Label nameLabel = new Label(user.getUsername());
            nameLabel.getStyleClass().add("user-cell-name");
            HBox.setHgrow(nameLabel, Priority.ALWAYS);
            nameLabel.setMaxWidth(Double.MAX_VALUE);

            Button dmButton = new Button("Nachricht");
            dmButton.getStyleClass().add("button-primary");
            dmButton.setStyle("-fx-font-size: 11px; -fx-padding: 4 10;");
            dmButton.setOnAction(e -> {
                if (onStartDm != null) {
                    onStartDm.accept(user);
                }
            });

            HBox cell = new HBox(10, nameLabel, dmButton);
            cell.setAlignment(Pos.CENTER_LEFT);
            cell.setPadding(new Insets(8, 16, 8, 16));

            setGraphic(cell);
            setText(null);
        }
    }
}
