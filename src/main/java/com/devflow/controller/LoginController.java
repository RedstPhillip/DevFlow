package com.devflow.controller;

import com.devflow.service.AuthService;
import com.devflow.view.LoginView;
import javafx.application.Platform;

public class LoginController {

    private final LoginView view;
    private final AuthService authService;
    private final MainController mainController;

    public LoginController(LoginView view, AuthService authService, MainController mainController) {
        this.view = view;
        this.authService = authService;
        this.mainController = mainController;
        bindEvents();
    }

    private void bindEvents() {
        view.getLoginButton().setOnAction(e -> handleLogin());
        view.getRegisterButton().setOnAction(e -> handleRegister());

        view.getPasswordField().setOnAction(e -> handleLogin());
    }

    private void handleLogin() {
        String username = view.getUsernameField().getText().trim();
        String password = view.getPasswordField().getText();

        if (!validate(username, password)) return;

        view.clearError();
        view.setLoading(true);

        authService.login(username, password)
                .thenAcceptAsync(token -> {
                    mainController.showMainLayout();
                }, Platform::runLater)
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        view.setLoading(false);
                        view.showError(extractMessage(ex));
                    });
                    return null;
                });
    }

    private void handleRegister() {
        String username = view.getUsernameField().getText().trim();
        String password = view.getPasswordField().getText();

        if (!validate(username, password)) return;

        view.clearError();
        view.setLoading(true);

        authService.register(username, password)
                .thenAcceptAsync(token -> {
                    mainController.showMainLayout();
                }, Platform::runLater)
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        view.setLoading(false);
                        view.showError(extractMessage(ex));
                    });
                    return null;
                });
    }

    private boolean validate(String username, String password) {
        if (username.isEmpty()) {
            view.showError("Benutzername darf nicht leer sein");
            return false;
        }
        if (password.isEmpty()) {
            view.showError("Passwort darf nicht leer sein");
            return false;
        }
        if (password.length() < 4) {
            view.showError("Passwort muss mindestens 4 Zeichen lang sein");
            return false;
        }
        return true;
    }

    private String extractMessage(Throwable ex) {
        Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
        String msg = cause.getMessage();
        if (msg != null && !msg.isBlank()) return msg;
        return "Verbindung zum Server fehlgeschlagen";
    }
}
