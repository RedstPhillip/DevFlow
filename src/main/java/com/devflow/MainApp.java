package com.devflow;

import com.devflow.config.AppConfig;
import com.devflow.config.ThemeManager;
import com.devflow.config.TokenStore;
import com.devflow.controller.MainController;
import com.devflow.service.UpdateService;
import com.devflow.view.UpdateDialog;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;

public class MainApp extends Application {

    @Override
    public void start(Stage stage) {
        ThemeManager.getInstance().applyInitial();

        stage.setTitle("DevFlow " + AppConfig.APP_VERSION);
        stage.setMinWidth(900);
        stage.setMinHeight(600);

        MainController mainController = new MainController(stage);
        mainController.start();

        checkForUpdates(stage);
    }

    private void checkForUpdates(Stage stage) {
        String pat = TokenStore.getInstance().getGithubPat();
        if (pat == null || pat.isBlank()) {
            return;
        }

        UpdateService updateService = new UpdateService();
        updateService.checkForUpdate()
                .thenAcceptAsync(updateInfo -> {
                    if (updateInfo != null) {
                        UpdateDialog dialog = new UpdateDialog(stage, updateInfo, updateService);
                        dialog.showAndWait();
                    }
                }, Platform::runLater)
                .exceptionally(ex -> {
                    System.err.println("Update check failed: " + ex.getMessage());
                    return null;
                });
    }

    public static void main(String[] args) {
        launch(args);
    }
}
