package com.devflow;

import atlantafx.base.theme.PrimerDark;
import com.devflow.config.AppConfig;
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
        Application.setUserAgentStylesheet(new PrimerDark().getUserAgentStylesheet());

        stage.setTitle("DevFlow " + AppConfig.APP_VERSION);
        stage.setMinWidth(800);
        stage.setMinHeight(500);

        MainController mainController = new MainController(stage);

        checkForUpdates(stage, () -> mainController.start());
    }

    private void checkForUpdates(Stage stage, Runnable onComplete) {
        String pat = TokenStore.getInstance().getGithubPat();

        if (pat == null || pat.isBlank()) {
            onComplete.run();
            return;
        }

        UpdateService updateService = new UpdateService();
        updateService.checkForUpdate()
                .thenAcceptAsync(updateInfo -> {
                    onComplete.run();
                    if (updateInfo != null) {
                        UpdateDialog dialog = new UpdateDialog(stage, updateInfo, updateService);
                        dialog.showAndWait();
                    }
                }, Platform::runLater)
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        System.err.println("Update check failed: " + ex.getMessage());
                        onComplete.run();
                    });
                    return null;
                });
    }

    public static void main(String[] args) {
        launch(args);
    }
}
