package com.devflow;

import atlantafx.base.theme.PrimerDark;
import com.devflow.config.AppConfig;
import com.devflow.config.TokenStore;
import com.devflow.controller.MainController;
import com.devflow.service.UpdateService;
import com.devflow.view.UpdateDialog;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

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
            showPatDialogIfNeeded(stage);
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

    private void showPatDialogIfNeeded(Stage owner) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.NONE);
        dialog.initOwner(owner);
        dialog.initStyle(StageStyle.UNDECORATED);
        dialog.setTitle("GitHub PAT");

        VBox root = new VBox(12);
        root.getStyleClass().add("update-dialog");
        root.setPadding(new Insets(24));
        root.setMaxWidth(420);

        Label title = new Label("GitHub Personal Access Token");
        title.getStyleClass().add("section-title");

        Label info = new Label("Optional: Fuer automatische Updates von der privaten Repo einen PAT eingeben (repo scope).");
        info.getStyleClass().add("muted");
        info.setWrapText(true);

        PasswordField patField = new PasswordField();
        patField.setPromptText("ghp_...");

        Button saveButton = new Button("Speichern");
        saveButton.getStyleClass().add("button-primary");
        saveButton.setOnAction(e -> {
            String val = patField.getText().trim();
            if (!val.isBlank()) {
                TokenStore.getInstance().setGithubPat(val);
            }
            dialog.close();
        });

        Button skipButton = new Button("Ueberspringen");
        skipButton.getStyleClass().add("button-secondary");
        skipButton.setOnAction(e -> dialog.close());

        HBox buttons = new HBox(10, saveButton, skipButton);
        buttons.setAlignment(Pos.CENTER_RIGHT);

        root.getChildren().addAll(title, info, patField, buttons);

        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("/styles/app.css").toExternalForm());
        dialog.setScene(scene);
        dialog.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
