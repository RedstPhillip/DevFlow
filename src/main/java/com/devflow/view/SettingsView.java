package com.devflow.view;

import com.devflow.config.AppConfig;
import com.devflow.config.ThemeManager;
import com.devflow.config.TokenStore;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.controlsfx.control.ToggleSwitch;

import java.util.HashMap;
import java.util.Map;

public class SettingsView extends ScrollPane {

    private final ToggleSwitch themeToggle;
    private final PasswordField patField;
    private final Button savePatButton;
    private final Button clearPatButton;
    private final Button logoutButton;
    private final Label patStatus;

    private final Map<String, javafx.scene.Node> sectionAnchors = new HashMap<>();
    private final HBox wrapper;

    private Runnable onLogout;
    private Runnable onPatSaved;

    public SettingsView() {
        getStyleClass().add("settings-view");
        setFitToWidth(true);
        setHbarPolicy(ScrollBarPolicy.NEVER);

        VBox root = new VBox(24);
        root.setPadding(new Insets(32, 40, 32, 40));
        root.setMaxWidth(720);

        Label header = new Label("Einstellungen");
        header.getStyleClass().add("settings-header");

        // ── Appearance ──
        Label appearanceTitle = sectionTitle("ERSCHEINUNGSBILD");
        sectionAnchors.put("appearance", appearanceTitle);
        VBox appearanceCard = new VBox(0);
        appearanceCard.getStyleClass().add("settings-card");

        themeToggle = new ToggleSwitch();
        themeToggle.setSelected(ThemeManager.getInstance().isDark());

        HBox themeRow = settingsRow(
                "Dunkles Design",
                "Wechsle zwischen hellem und dunklem Modus.",
                themeToggle
        );
        appearanceCard.getChildren().add(themeRow);

        themeToggle.selectedProperty().addListener((obs, old, selected) -> {
            ThemeManager.getInstance().setTheme(
                    selected ? ThemeManager.Theme.DARK : ThemeManager.Theme.LIGHT);
        });

        // ── GitHub ──
        Label githubTitle = sectionTitle("GITHUB INTEGRATION");
        sectionAnchors.put("github", githubTitle);
        VBox githubCard = new VBox(14);
        githubCard.getStyleClass().add("settings-card");

        Label patLabel = new Label("Personal Access Token");
        patLabel.getStyleClass().add("settings-label");

        Label patDesc = new Label("Wird benötigt um Updates aus dem privaten Repository zu beziehen. Der Token wird lokal gespeichert.");
        patDesc.getStyleClass().add("settings-description");
        patDesc.setWrapText(true);

        patField = new PasswordField();
        patField.setPromptText("ghp_...");
        String existing = TokenStore.getInstance().getGithubPat();
        if (existing != null && !existing.isBlank()) {
            patField.setText(existing);
        }

        savePatButton = new Button("Speichern");
        savePatButton.getStyleClass().add("button-primary");
        clearPatButton = new Button("Entfernen");
        clearPatButton.getStyleClass().add("button-secondary");

        patStatus = new Label();
        patStatus.getStyleClass().add("settings-description");

        HBox patButtons = new HBox(10, savePatButton, clearPatButton);

        savePatButton.setOnAction(e -> {
            String val = patField.getText();
            if (val == null || val.isBlank()) {
                patStatus.setText("Token darf nicht leer sein.");
                return;
            }
            TokenStore.getInstance().setGithubPat(val.trim());
            patStatus.setText("Token gespeichert.");
            if (onPatSaved != null) onPatSaved.run();
        });

        clearPatButton.setOnAction(e -> {
            TokenStore.getInstance().setGithubPat("");
            patField.clear();
            patStatus.setText("Token entfernt.");
        });

        githubCard.getChildren().addAll(patLabel, patDesc, patField, patButtons, patStatus);

        // ── Account ──
        Label accountTitle = sectionTitle("ACCOUNT");
        sectionAnchors.put("account", accountTitle);
        VBox accountCard = new VBox(14);
        accountCard.getStyleClass().add("settings-card");

        Label logoutLabel = new Label("Abmelden");
        logoutLabel.getStyleClass().add("settings-label");

        Label logoutDesc = new Label("Meldet dich von diesem Gerät ab. Deine Unterhaltungen bleiben erhalten.");
        logoutDesc.getStyleClass().add("settings-description");
        logoutDesc.setWrapText(true);

        logoutButton = new Button("Abmelden");
        logoutButton.getStyleClass().add("button-destructive");
        logoutButton.setOnAction(e -> { if (onLogout != null) onLogout.run(); });

        accountCard.getChildren().addAll(logoutLabel, logoutDesc, logoutButton);

        // ── About ──
        Label aboutTitle = sectionTitle("ÜBER");
        sectionAnchors.put("about", aboutTitle);
        VBox aboutCard = new VBox(6);
        aboutCard.getStyleClass().add("settings-card");
        Label version = new Label("DevFlow " + AppConfig.APP_VERSION);
        version.getStyleClass().add("settings-label");
        Label about = new Label("Enterprise-Chat für Teams.");
        about.getStyleClass().add("settings-version");
        aboutCard.getChildren().addAll(version, about);

        root.getChildren().addAll(
                header,
                appearanceTitle, appearanceCard,
                githubTitle, githubCard,
                accountTitle, accountCard,
                aboutTitle, aboutCard
        );

        wrapper = new HBox(root);
        wrapper.setAlignment(Pos.TOP_CENTER);
        HBox.setHgrow(root, Priority.ALWAYS);
        setContent(wrapper);
    }

    public void scrollToSection(String key) {
        javafx.scene.Node target = sectionAnchors.get(key);
        if (target == null) return;
        // Defer to next pulse so layout is up to date
        javafx.application.Platform.runLater(() -> {
            double contentHeight = wrapper.getBoundsInLocal().getHeight();
            double viewportHeight = getViewportBounds().getHeight();
            double maxScroll = contentHeight - viewportHeight;
            if (maxScroll <= 0) { setVvalue(0); return; }
            javafx.geometry.Bounds tgt = target.localToScene(target.getBoundsInLocal());
            javafx.geometry.Bounds con = wrapper.localToScene(wrapper.getBoundsInLocal());
            double yInContent = tgt.getMinY() - con.getMinY() - 16; // small offset
            setVvalue(Math.min(1, Math.max(0, yInContent / maxScroll)));
        });
    }

    private Label sectionTitle(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("settings-section-title");
        return l;
    }

    private HBox settingsRow(String title, String description, javafx.scene.Node control) {
        Label t = new Label(title);
        t.getStyleClass().add("settings-label");
        Label d = new Label(description);
        d.getStyleClass().add("settings-description");
        d.setWrapText(true);
        VBox text = new VBox(4, t, d);
        HBox.setHgrow(text, Priority.ALWAYS);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox row = new HBox(16, text, spacer, control);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(6, 0, 6, 0));
        return row;
    }

    public void setOnLogout(Runnable r) { this.onLogout = r; }
    public void setOnPatSaved(Runnable r) { this.onPatSaved = r; }
}
