package com.devflow.view;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.function.Consumer;

public class ActivityBar extends VBox {

    private Button chatButton;
    private Button usersButton;
    private Button settingsButton;
    private Button activeButton;

    public ActivityBar(Consumer<String> onNavigate) {
        getStyleClass().add("activity-bar");
        setAlignment(Pos.TOP_CENTER);
        setPadding(new Insets(8, 0, 8, 0));
        setSpacing(4);
        setPrefWidth(48);
        setMinWidth(48);
        setMaxWidth(48);

        chatButton = createIconButton("\u2261", "Chats", () -> onNavigate.accept("chats"));
        usersButton = createIconButton("\u263A", "Benutzer", () -> onNavigate.accept("users"));

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        settingsButton = createIconButton("\u2731", "Einstellungen", () -> onNavigate.accept("settings"));

        getChildren().addAll(chatButton, usersButton, spacer, settingsButton);

        setActive(chatButton);
    }

    private Button createIconButton(String icon, String tooltip, Runnable action) {
        Button btn = new Button(icon);
        btn.getStyleClass().add("activity-bar-button");
        btn.setTooltip(new Tooltip(tooltip));
        btn.setOnAction(e -> {
            setActive(btn);
            action.run();
        });
        return btn;
    }

    private void setActive(Button btn) {
        if (activeButton != null) {
            activeButton.getStyleClass().remove("activity-bar-button-active");
        }
        activeButton = btn;
        btn.getStyleClass().add("activity-bar-button-active");
    }

    public void selectChats() { chatButton.fire(); }
    public void selectUsers() { usersButton.fire(); }
}
