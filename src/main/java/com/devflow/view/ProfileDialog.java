package com.devflow.view;

import com.devflow.model.User;
import com.devflow.service.UserService;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.function.Consumer;

public class ProfileDialog extends VBox {

    private final ModalOverlay overlay;

    public ProfileDialog(StackPane host, Node blurTarget,
                         UserService userService, User currentUser,
                         Consumer<User> onUpdated) {
        getStyleClass().add("modal-card");
        setMaxWidth(420);
        setPadding(new Insets(20, 22, 18, 22));
        setSpacing(14);
        setAlignment(Pos.TOP_LEFT);

        Label title = new Label("Profil");
        title.getStyleClass().add("modal-title");
        Button closeBtn = new Button("\u2715");
        closeBtn.getStyleClass().add("modal-close-btn");
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        HBox header = new HBox(10, title, sp, closeBtn);
        header.setAlignment(Pos.CENTER_LEFT);

        Avatar avatar = new Avatar(currentUser.getUsername(), 72);
        HBox avatarRow = new HBox(avatar);
        avatarRow.setAlignment(Pos.CENTER);
        avatarRow.setPadding(new Insets(8, 0, 8, 0));

        Label hint = new Label("Dein Avatar wird aus deinem Benutzernamen generiert.");
        hint.getStyleClass().add("modal-subtitle");
        hint.setWrapText(true);

        Label nameLbl = new Label("Benutzername");
        nameLbl.getStyleClass().add("settings-label");
        TextField nameField = new TextField(currentUser.getUsername());
        nameField.getStyleClass().add("text-field");

        Label status = new Label("");
        status.getStyleClass().add("modal-subtitle");

        Button save = new Button("Speichern");
        save.getStyleClass().add("button-primary");
        Region fSp = new Region();
        HBox.setHgrow(fSp, Priority.ALWAYS);
        HBox footer = new HBox(10, status, fSp, save);
        footer.setAlignment(Pos.CENTER_LEFT);

        getChildren().addAll(header, avatarRow, hint, nameLbl, nameField, footer);

        overlay = new ModalOverlay(host, blurTarget, this);
        closeBtn.setOnAction(e -> overlay.close());

        nameField.textProperty().addListener((o, old, val) -> avatar.setName(val));

        save.setOnAction(e -> {
            String newName = nameField.getText().trim();
            if (newName.isEmpty() || newName.equals(currentUser.getUsername())) {
                overlay.close();
                return;
            }
            save.setDisable(true);
            userService.updateProfile(newName, null)
                    .thenAcceptAsync(updated -> {
                        onUpdated.accept(updated);
                        overlay.close();
                    }, Platform::runLater)
                    .exceptionally(ex -> {
                        Platform.runLater(() -> {
                            status.setText("Konnte nicht gespeichert werden.");
                            save.setDisable(false);
                        });
                        return null;
                    });
        });
    }

    public void show() { overlay.show(); }
}
