package com.devflow.controller;

import com.devflow.service.UserService;
import com.devflow.view.UserListView;
import javafx.application.Platform;

public class UserListController {

    private final UserListView view;
    private final UserService userService;
    private final MainController mainController;

    public UserListController(UserListView view, UserService userService, MainController mainController) {
        this.view = view;
        this.userService = userService;
        this.mainController = mainController;

        view.setOnStartDm(mainController::startDmWith);
        load();
    }

    public void load() {
        userService.listUsers()
                .thenAcceptAsync(users -> view.setUsers(users), Platform::runLater)
                .exceptionally(ex -> {
                    System.err.println("Failed to load users: " + ex.getMessage());
                    return null;
                });
    }
}
