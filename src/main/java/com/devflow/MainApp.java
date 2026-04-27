package com.devflow;

import com.devflow.config.AppConfig;
import com.devflow.config.ThemeManager;
import com.devflow.config.TokenStore;
import com.devflow.controller.MainController;
import com.devflow.service.UpdateService;
import com.devflow.view.UpdateDialog;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class MainApp extends Application {

    @Override
    public void start(Stage stage) {
        loadFonts();
        ThemeManager.getInstance().applyInitial();

        stage.initStyle(StageStyle.TRANSPARENT);
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
                        dialog.show();
                        dialog.toFront();
                    }
                }, Platform::runLater)
                .exceptionally(ex -> {
                    System.err.println("Update check failed: " + ex.getMessage());
                    return null;
                });
    }

    private void loadFonts() {
        String[] fonts = {
                "/fonts/Roboto-Regular.ttf",
                "/fonts/Roboto-Medium.ttf",
                "/fonts/Roboto-Bold.ttf"
        };
        for (String path : fonts) {
            try (var in = getClass().getResourceAsStream(path)) {
                if (in == null) {
                    System.err.println("Font resource not found: " + path);
                    continue;
                }
                Font f = Font.loadFont(in, 13);
                if (f == null) {
                    System.err.println("Font.loadFont returned null for " + path);
                } else {
                    System.out.println("Loaded font: " + f.getName() + " (family=" + f.getFamily() + ")");
                }
            } catch (Exception e) {
                System.err.println("Failed to load font " + path + ": " + e.getMessage());
            }
        }
    }

    public static void main(String[] args) {
        // Crisper text rendering on Windows/JavaFX
        System.setProperty("prism.lcdtext", "true");
        launch(args);
    }
}
