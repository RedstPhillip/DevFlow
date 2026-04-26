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
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
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
        initStyle(StageStyle.TRANSPARENT);

        VBox root = new VBox(16);
        root.getStyleClass().add("update-dialog");
        // Phase 4 §8 + Polish-Pass §4: width 480, padding 28, spacing 16.
        root.setPadding(new Insets(28));
        root.setMaxWidth(480);

        Label title = new Label("Update verfügbar");
        title.getStyleClass().addAll("section-title", "t-card-title");

        ModalCloseButton closeBtn = new ModalCloseButton(this::close);

        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);
        HBox header = new HBox(8, title, headerSpacer, closeBtn);
        header.setAlignment(Pos.CENTER_LEFT);

        Label versionLabel = new Label("Aktuelle Version: " + AppConfig.APP_VERSION
                + "  \u2192  Neue Version: " + info.version);
        versionLabel.getStyleClass().addAll("muted", "t-body");

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
        statusLabel.getStyleClass().addAll("muted", "t-caption");
        statusLabel.setVisible(false);

        updateButton = new Button("Jetzt updaten");
        updateButton.getStyleClass().addAll("button-primary", "button-large");
        if (info.downloadUrl == null) {
            updateButton.setDisable(true);
            statusLabel.setText("Kein JAR-Asset im Release gefunden");
            statusLabel.setVisible(true);
        }

        skipButton = new Button("Überspringen");
        skipButton.getStyleClass().add("button-flat");
        skipButton.setOnAction(e -> close());

        updateButton.setOnAction(e -> {
            updateButton.setDisable(true);
            skipButton.setDisable(true);
            closeBtn.setDisable(true);
            progressBar.setVisible(true);
            progressBar.setManaged(true);
            progressBar.setProgress(-1);
            statusLabel.setText("Download läuft …");
            statusLabel.setVisible(true);

            updateService.downloadUpdate(info.downloadUrl)
                    .thenAcceptAsync(jarPath -> {
                        statusLabel.setText("Update wird angewendet …");
                        try {
                            updateService.applyUpdate(jarPath);
                            Platform.exit();
                            System.exit(0);
                        } catch (Exception ex) {
                            statusLabel.setText("Update fehlgeschlagen: " + ex.getMessage());
                            updateButton.setDisable(false);
                            skipButton.setDisable(false);
                            closeBtn.setDisable(false);
                        }
                    }, Platform::runLater)
                    .exceptionally(ex -> {
                        Platform.runLater(() -> {
                            statusLabel.setText("Download fehlgeschlagen: " + ex.getMessage());
                            progressBar.setVisible(false);
                            progressBar.setManaged(false);
                            updateButton.setDisable(false);
                            skipButton.setDisable(false);
                            closeBtn.setDisable(false);
                        });
                        return null;
                    });
        });

        Region btnSpacer = new Region();
        HBox.setHgrow(btnSpacer, Priority.ALWAYS);
        HBox buttons = new HBox(8, statusLabel, btnSpacer, skipButton, updateButton);
        buttons.setAlignment(Pos.CENTER_LEFT);

        root.getChildren().addAll(header, versionLabel, notes, progressBar, buttons);

        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);
        scene.getStylesheets().add(getClass().getResource("/styles/app.css").toExternalForm());
        // ESC closes when the update isn't actively downloading.
        scene.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE && !updateButton.isDisabled()) close();
        });
        setScene(scene);
        setTitle("DevFlow Update");
    }
}
