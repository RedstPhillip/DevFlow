package com.devflow.view;

import com.devflow.config.AppConfig;
import com.devflow.service.UpdateService;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class UpdateDialog extends Stage {

    private final ProgressBar progressBar;
    private final Button updateButton;
    private final Button skipButton;
    private final Label statusLabel;

    public UpdateDialog(Stage owner, UpdateService.UpdateInfo info, UpdateService updateService) {
        initModality(Modality.APPLICATION_MODAL);
        initOwner(owner);
        initStyle(StageStyle.UNDECORATED);

        VBox root = new VBox(14);
        root.getStyleClass().add("update-dialog");
        root.setPadding(new Insets(24));
        root.setMaxWidth(480);

        Label title = new Label("Update verfuegbar");
        title.getStyleClass().add("section-title");

        Label versionLabel = new Label("Aktuelle Version: " + AppConfig.APP_VERSION
                + "  \u2192  Neue Version: " + info.version);
        versionLabel.getStyleClass().add("muted");

        TextArea notes = new TextArea(info.releaseNotes != null ? info.releaseNotes : "");
        notes.setEditable(false);
        notes.setWrapText(true);
        notes.setPrefHeight(120);
        notes.getStyleClass().add("update-notes");

        progressBar = new ProgressBar(0);
        progressBar.setMaxWidth(Double.MAX_VALUE);
        progressBar.setVisible(false);
        progressBar.setManaged(false);

        statusLabel = new Label();
        statusLabel.getStyleClass().add("muted");
        statusLabel.setVisible(false);

        updateButton = new Button("Jetzt updaten");
        updateButton.getStyleClass().add("button-primary");
        if (info.downloadUrl == null) {
            updateButton.setDisable(true);
            statusLabel.setText("Kein JAR-Asset im Release gefunden");
            statusLabel.setVisible(true);
        }

        skipButton = new Button("Ueberspringen");
        skipButton.getStyleClass().add("button-secondary");
        skipButton.setOnAction(e -> close());

        updateButton.setOnAction(e -> {
            updateButton.setDisable(true);
            skipButton.setDisable(true);
            progressBar.setVisible(true);
            progressBar.setManaged(true);
            progressBar.setProgress(-1);
            statusLabel.setText("Downloading...");
            statusLabel.setVisible(true);

            updateService.downloadUpdate(info.downloadUrl)
                    .thenAcceptAsync(jarPath -> {
                        statusLabel.setText("Update wird angewendet...");
                        try {
                            updateService.applyUpdate(jarPath);
                            Platform.exit();
                            System.exit(0);
                        } catch (Exception ex) {
                            statusLabel.setText("Update fehlgeschlagen: " + ex.getMessage());
                            updateButton.setDisable(false);
                            skipButton.setDisable(false);
                        }
                    }, Platform::runLater)
                    .exceptionally(ex -> {
                        Platform.runLater(() -> {
                            statusLabel.setText("Download fehlgeschlagen: " + ex.getMessage());
                            progressBar.setVisible(false);
                            progressBar.setManaged(false);
                            updateButton.setDisable(false);
                            skipButton.setDisable(false);
                        });
                        return null;
                    });
        });

        HBox buttons = new HBox(10, updateButton, skipButton);
        buttons.setAlignment(Pos.CENTER_RIGHT);

        root.getChildren().addAll(title, versionLabel, notes, progressBar, statusLabel, buttons);

        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("/styles/app.css").toExternalForm());
        setScene(scene);
        setTitle("DevFlow Update");
    }
}
