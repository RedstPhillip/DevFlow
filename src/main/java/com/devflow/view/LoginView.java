package com.devflow.view;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
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
        card.setMaxHeight(Region.USE_PREF_SIZE);
        card.setAlignment(Pos.TOP_LEFT);

        Label title = new Label("Willkommen bei DevFlow");
        title.getStyleClass().addAll("login-title", "t-page-title");

        Label subtitle = new Label("Melde dich an oder erstelle einen neuen Account, um mit deinem Team zu chatten.");
        subtitle.getStyleClass().addAll("login-subtitle", "t-body");
        subtitle.setWrapText(true);

        usernameField = new TextField();
        usernameField.setPromptText("Benutzername");
        usernameField.getStyleClass().addAll("login-field", "t-input");

        passwordField = new PasswordField();
        passwordField.setPromptText("Passwort");
        passwordField.getStyleClass().addAll("login-field", "t-input");

        VBox fields = new VBox(10, usernameField, passwordField);

        loginButton = new Button("Anmelden");
        loginButton.getStyleClass().addAll("button-primary", "button-large", "login-button");
        // Phase 4 §7: Primary stays at min-width 120 so it's visually dominant
        // without growing edge-to-edge. The flat secondary next to it is
        // intentionally smaller — spec forbids two equally-sized buttons.
        loginButton.setPrefWidth(120);

        registerButton = new Button("Registrieren");
        registerButton.getStyleClass().addAll("button-flat", "button-large", "login-button");

        HBox buttons = new HBox(8, loginButton, registerButton);
        buttons.setAlignment(Pos.CENTER_LEFT);

        errorLabel = new Label();
        errorLabel.getStyleClass().addAll("login-error", "t-caption");
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
        errorLabel.setWrapText(true);

        Label footer = new Label("Bei Problemen wende dich an dein IT-Team.");
        footer.getStyleClass().addAll("login-footer", "t-caption");

        card.getChildren().addAll(title, subtitle, fields, errorLabel, buttons, footer);
        // Phase 4 §7: card padding --space-6 (32) all around.
        card.setPadding(new Insets(32, 32, 32, 32));

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
