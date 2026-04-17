package com.devflow.view;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public class LoginView extends StackPane {

    private final TextField usernameField;
    private final PasswordField passwordField;
    private final Button loginButton;
    private final Button registerButton;
    private final Label errorLabel;

    public LoginView() {
        getStyleClass().add("login-root");

        VBox card = new VBox(18);
        card.getStyleClass().add("login-card");
        card.setMaxWidth(400);
        card.setAlignment(Pos.TOP_LEFT);

        Label title = new Label("Willkommen bei DevFlow");
        title.getStyleClass().add("login-title");

        Label subtitle = new Label("Melde dich an oder erstelle einen neuen Account, um mit deinem Team zu chatten.");
        subtitle.getStyleClass().add("login-subtitle");
        subtitle.setWrapText(true);

        usernameField = new TextField();
        usernameField.setPromptText("Benutzername");
        usernameField.getStyleClass().add("login-field");

        passwordField = new PasswordField();
        passwordField.setPromptText("Passwort");
        passwordField.getStyleClass().add("login-field");

        VBox fields = new VBox(10, usernameField, passwordField);

        loginButton = new Button("Anmelden");
        loginButton.getStyleClass().addAll("button-primary", "login-button");
        loginButton.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(loginButton, javafx.scene.layout.Priority.ALWAYS);

        registerButton = new Button("Registrieren");
        registerButton.getStyleClass().addAll("button-secondary", "login-button");
        registerButton.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(registerButton, javafx.scene.layout.Priority.ALWAYS);

        HBox buttons = new HBox(10, loginButton, registerButton);
        buttons.setAlignment(Pos.CENTER);

        errorLabel = new Label();
        errorLabel.getStyleClass().add("login-error");
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
        errorLabel.setWrapText(true);

        Label footer = new Label("Bei Problemen wende dich an dein IT-Team.");
        footer.getStyleClass().add("login-footer");

        card.getChildren().addAll(title, subtitle, fields, errorLabel, buttons, footer);
        card.setPadding(new Insets(30, 28, 28, 28));

        setAlignment(Pos.CENTER);
        setPadding(new Insets(24));
        getChildren().add(card);

        // Focus username on first paint so the user can start typing immediately.
        javafx.application.Platform.runLater(usernameField::requestFocus);
    }

    public TextField getUsernameField() { return usernameField; }
    public PasswordField getPasswordField() { return passwordField; }
    public Button getLoginButton() { return loginButton; }
    public Button getRegisterButton() { return registerButton; }

    public void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    public void clearError() {
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }

    public void setLoading(boolean loading) {
        loginButton.setDisable(loading);
        registerButton.setDisable(loading);
        usernameField.setDisable(loading);
        passwordField.setDisable(loading);
        loginButton.setText(loading ? "..." : "Anmelden");
    }
}
