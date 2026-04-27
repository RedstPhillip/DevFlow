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

public class SettingsView extends ScrollPane {

    private final ToggleSwitch themeToggle;
    private final PasswordField patField;
    private final Button savePatButton;
    private final Button clearPatButton;
    private final Button logoutButton;
    private final Label patStatus;

    private final HBox wrapper;

    private Runnable onLogout;
    private Runnable onPatSaved;

    public SettingsView() {
        getStyleClass().add("settings-view");
        setFitToWidth(true);
        setHbarPolicy(ScrollBarPolicy.NEVER);

        // Phase 4 §3 + Polish-Pass §4: padding 24, between-section gap 32 ("Section-Break").
        // Each section wraps its (title, card) into a sub-VBox with a 12 gap so the
        // header sits tight to its card without the 32 break spilling inside.
        VBox root = new VBox(28);
        root.setPadding(new Insets(24, 24, 24, 24));
        root.setMaxWidth(940);

        Label headerTitle = new Label("Einstellungen");
        headerTitle.getStyleClass().add("settings-page-title");
        Label headerSubtitle = new Label("Client, Integrationen und Account für diesen Workspace.");
        headerSubtitle.getStyleClass().add("settings-page-subtitle");
        headerSubtitle.setWrapText(true);
        VBox pageHeader = new VBox(6, headerTitle, headerSubtitle);
        pageHeader.getStyleClass().add("settings-page-header");

        // ── Appearance ──
        Label appearanceTitle = sectionTitle("Erscheinungsbild");
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
        Label githubTitle = sectionTitle("GitHub");
        VBox githubCard = new VBox(14);
        githubCard.getStyleClass().add("settings-card");

        Label patLabel = new Label("Personal Access Token");
        patLabel.getStyleClass().addAll("settings-label", "t-card-title");

        Label patDesc = new Label("Token für private Repository-Updates. Wird lokal gespeichert.");
        patDesc.getStyleClass().addAll("settings-description", "t-body");
        patDesc.setWrapText(true);

        patField = new PasswordField();
        patField.getStyleClass().addAll("text-field", "t-input");
        patField.setPromptText("ghp_...");
        // SECURITY: never pre-fill the field with the stored token. Showing it (even masked)
        // hands the secret to any over-the-shoulder observer the moment they reveal the field
        // via DevTools / accessibility. Render a status row instead.

        savePatButton = new Button("Speichern");
        savePatButton.getStyleClass().add("button-primary");
        clearPatButton = new Button("Entfernen");
        clearPatButton.getStyleClass().add("button-secondary");

        patStatus = new Label();
        patStatus.getStyleClass().addAll("settings-description", "t-caption");
        refreshPatStatus();

        HBox patButtons = new HBox(10, savePatButton, clearPatButton);

        savePatButton.setOnAction(e -> {
            String val = patField.getText();
            if (val == null || val.isBlank()) {
                patStatus.setText("Token darf nicht leer sein.");
                return;
            }
            TokenStore.getInstance().setGithubPat(val.trim());
            patField.clear();
            refreshPatStatus();
            patStatus.setText("Token gespeichert.");
            if (onPatSaved != null) onPatSaved.run();
        });

        clearPatButton.setOnAction(e -> {
            TokenStore.getInstance().setGithubPat("");
            patField.clear();
            refreshPatStatus();
            patStatus.setText("Token entfernt.");
        });

        githubCard.getChildren().addAll(patLabel, patDesc, patField, patButtons, patStatus);

        // ── Account ──
        Label accountTitle = sectionTitle("Account");
        VBox accountCard = new VBox(14);
        accountCard.getStyleClass().add("settings-card");

        Label logoutLabel = new Label("Abmelden");
        logoutLabel.getStyleClass().addAll("settings-label", "t-card-title");

        Label logoutDesc = new Label("Meldet dich von diesem Gerät ab.");
        logoutDesc.getStyleClass().addAll("settings-description", "t-body");
        logoutDesc.setWrapText(true);

        logoutButton = new Button("Abmelden");
        logoutButton.getStyleClass().add("button-destructive");
        logoutButton.setOnAction(e -> { if (onLogout != null) onLogout.run(); });

        accountCard.getChildren().addAll(logoutLabel, logoutDesc, logoutButton);

        // ── About ──
        Label aboutTitle = sectionTitle("Über");
        VBox aboutCard = new VBox(6);
        aboutCard.getStyleClass().add("settings-card");
        Label version = new Label("DevFlow " + AppConfig.APP_VERSION);
        version.getStyleClass().addAll("settings-label", "t-card-title");
        Label about = new Label("Collaboration für Entwicklerteams.");
        about.getStyleClass().addAll("settings-version", "t-body");
        aboutCard.getChildren().addAll(version, about);

        // Polish-Pass §4: each section wraps its (title, card) into a 12-gap
        // VBox; the root VBox provides the larger 32 gap between sections.
        VBox appearanceSection = new VBox(12, appearanceTitle, appearanceCard);
        VBox githubSection     = new VBox(12, githubTitle,     githubCard);
        VBox accountSection    = new VBox(12, accountTitle,    accountCard);
        VBox aboutSection      = new VBox(12, aboutTitle,      aboutCard);
        root.getChildren().addAll(pageHeader, appearanceSection, githubSection, accountSection, aboutSection);

        wrapper = new HBox(root);
        wrapper.getStyleClass().add("settings-wrapper");
        wrapper.setAlignment(Pos.TOP_CENTER);
        HBox.setHgrow(root, Priority.ALWAYS);
        setContent(wrapper);
    }

    public void scrollToTop() {
        javafx.application.Platform.runLater(() -> setVvalue(0));
    }

    private void refreshPatStatus() {
        String existing = TokenStore.getInstance().getGithubPat();
        if (existing != null && !existing.isBlank()) {
            // Show only the token's prefix + suffix as a fingerprint, never the secret itself.
            String hint = existing.length() > 8
                    ? existing.substring(0, 4) + "…" + existing.substring(existing.length() - 4)
                    : "••••";
            patStatus.setText("Aktueller Token: " + hint);
            patField.setPromptText("Neuen Token eingeben, um zu ersetzen");
        } else {
            patStatus.setText("Kein Token gespeichert.");
            patField.setPromptText("ghp_…");
        }
    }

    private Label sectionTitle(String text) {
        Label l = new Label(text);
        l.getStyleClass().addAll("settings-section-title", "t-section-header");
        return l;
    }

    private HBox settingsRow(String title, String description, javafx.scene.Node control) {
        Label t = new Label(title);
        t.getStyleClass().addAll("settings-label", "t-card-title");
        Label d = new Label(description);
        d.getStyleClass().addAll("settings-description", "t-body");
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
