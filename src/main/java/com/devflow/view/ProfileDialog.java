package com.devflow.view;

import atlantafx.base.theme.Styles;
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
        // Phase 4 §8 + Polish-Pass §4: width 480, padding 28, spacing 16.
        setMaxWidth(480);
        setPadding(new Insets(28));
        setSpacing(16);
        setAlignment(Pos.TOP_LEFT);

        Label title = new Label("Profil");
        title.getStyleClass().addAll("modal-title", "t-card-title", Styles.TITLE_4, Styles.TEXT_BOLD);
        ModalCloseButton closeBtn = new ModalCloseButton(null); // wired below once overlay exists
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        HBox header = new HBox(10, title, sp, closeBtn);
        header.setAlignment(Pos.CENTER_LEFT);

        Avatar avatar = new Avatar(currentUser.getUsername(), 72);
        HBox avatarRow = new HBox(avatar);
        avatarRow.setAlignment(Pos.CENTER);
        avatarRow.setPadding(new Insets(8, 0, 8, 0));

        Label hint = new Label("Dein Avatar wird aus deinem Benutzernamen generiert.");
        hint.getStyleClass().addAll("modal-subtitle", "t-caption", Styles.TEXT_MUTED);
        hint.setWrapText(true);

        Label nameLbl = new Label("Benutzername");
        nameLbl.getStyleClass().addAll("settings-label", "t-body-strong");
        TextField nameField = new TextField(currentUser.getUsername());
        nameField.getStyleClass().addAll("text-field", "t-input");

        Label status = new Label("");
        status.getStyleClass().addAll("modal-subtitle", "t-caption", Styles.TEXT_MUTED);

        Button save = new Button("Speichern");
        save.getStyleClass().addAll("button-primary", "button-large");
        // Phase 4: Profil-Modal exposes an explicit Abbrechen button next to
        // Speichern so the destructive escape path matches every other dialog
        // in the app (Cancel-left / Primary-right pattern). Flat-button keeps
        // "one Primary per modal".
        Button cancel = new Button("Abbrechen");
        cancel.getStyleClass().add("button-flat");
        Region fSp = new Region();
        HBox.setHgrow(fSp, Priority.ALWAYS);
        HBox footer = new HBox(8, status, fSp, cancel, save);
        footer.setAlignment(Pos.CENTER_LEFT);

        getChildren().addAll(header, avatarRow, hint, nameLbl, nameField, footer);

        overlay = new ModalOverlay(host, blurTarget, this);
        closeBtn.setOnAction(e -> overlay.close());
        cancel.setOnAction(e -> overlay.close());

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
