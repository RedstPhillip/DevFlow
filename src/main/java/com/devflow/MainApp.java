package com.devflow;

import atlantafx.base.theme.PrimerDark;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class MainApp extends Application {

    @Override
    public void start(Stage stage) {
        Application.setUserAgentStylesheet(new PrimerDark().getUserAgentStylesheet());

        VBox content = new VBox(20);
        content.getStyleClass().addAll("app-root", "showcase-content");
        content.setPadding(new Insets(24));

        Label title = new Label("DevFlow Theme Showcase");
        title.getStyleClass().add("title");

        Label subtitle = new Label("JavaFX + AtlantaFX mit allen Theme-Rollen im direkten Vergleich");
        subtitle.getStyleClass().add("muted");

        FlowPane tokens = new FlowPane(12, 12);
        tokens.getStyleClass().add("token-grid");
        tokens.getChildren().addAll(
            colorToken("Background", "#0a0a0a", "surface-background", "text-foreground"),
            colorToken("Foreground", "#fafafa", "surface-foreground", "text-background"),
            colorToken("Card", "#171717", "surface-card", "text-card-foreground"),
            colorToken("Popover", "#262626", "surface-popover", "text-popover-foreground"),
            colorToken("Primary", "#2b7fff", "surface-primary", "text-primary-foreground"),
            colorToken("Secondary", "#262626", "surface-secondary", "text-secondary-foreground"),
            colorToken("Muted", "#262626", "surface-muted", "text-muted-foreground"),
            colorToken("Accent", "#404040", "surface-accent", "text-accent-foreground"),
            colorToken("Destructive", "#ff6467", "surface-destructive", "text-destructive-foreground"),
            colorToken("Border", "#282828", "surface-border", "text-foreground"),
            colorToken("Input", "#343434", "surface-input", "text-foreground"),
            colorToken("Ring", "#737373", "surface-ring", "text-background")
        );

        VBox componentDemo = createComponentDemo();
        VBox contextDemo = createContextDemo();

        content.getChildren().addAll(
            title,
            subtitle,
            new Separator(),
            section("Farbrollen", "Alle definierten Theme-Werte als Tokens", tokens),
            section("Komponenten", "Buttons, Inputs und Zustandsfarben", componentDemo),
            section("Kontextflaechen", "Card- und Popover-Oberflaechen in Kombination", contextDemo)
        );

        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.getStyleClass().add("showcase-scroll");
        scrollPane.setFitToWidth(true);

        Scene scene = new Scene(scrollPane, 980, 700);
        scene.getStylesheets().add(getClass().getResource("/styles/app.css").toExternalForm());

        stage.setTitle("DevFlow UI");
        stage.setScene(scene);
        stage.show();
    }

    private VBox createComponentDemo() {
        TextField input = new TextField();
        input.setPromptText("Input-Feld mit Ring-Farbe bei Fokus");
        input.setMaxWidth(320);

        Button primaryButton = new Button("Primary Action");
        primaryButton.getStyleClass().add("button-primary");

        Button secondaryButton = new Button("Secondary");
        secondaryButton.getStyleClass().add("button-secondary");

        Button destructiveButton = new Button("Delete");
        destructiveButton.getStyleClass().add("button-destructive");

        HBox actions = new HBox(10, primaryButton, secondaryButton, destructiveButton);

        Label accentBadge = new Label("Accent Badge");
        accentBadge.getStyleClass().addAll("badge", "surface-accent", "text-accent-foreground");

        Label mutedBadge = new Label("Muted Info");
        mutedBadge.getStyleClass().addAll("badge", "surface-muted", "text-muted-foreground");

        HBox badges = new HBox(8, accentBadge, mutedBadge);

        VBox card = new VBox(14,
            new Label("Interaktionsdemo"),
            input,
            actions,
            badges
        );
        card.getStyleClass().add("card");
        card.setPadding(new Insets(16));
        return card;
    }

    private VBox createContextDemo() {
        Label cardLabel = new Label("Card Context");
        cardLabel.getStyleClass().add("text-card-foreground");

        Label cardText = new Label("Diese Flaeche nutzt Card + Card Foreground.");
        cardText.getStyleClass().add("muted");

        VBox cardSurface = new VBox(6, cardLabel, cardText);
        cardSurface.getStyleClass().addAll("context-box", "surface-card");

        Label popoverLabel = new Label("Popover Preview");
        popoverLabel.getStyleClass().add("text-popover-foreground");

        Label popoverText = new Label("Popover + Border fuer Overlay-artige Inhalte.");
        popoverText.getStyleClass().add("text-popover-foreground");

        VBox popoverSurface = new VBox(6, popoverLabel, popoverText);
        popoverSurface.getStyleClass().addAll("context-box", "surface-popover");

        HBox row = new HBox(12, cardSurface, popoverSurface);
        HBox.setHgrow(cardSurface, Priority.ALWAYS);
        HBox.setHgrow(popoverSurface, Priority.ALWAYS);
        return new VBox(row);
    }

    private VBox section(String heading, String description, Node content) {
        Label sectionTitle = new Label(heading);
        sectionTitle.getStyleClass().add("section-title");

        Label sectionDescription = new Label(description);
        sectionDescription.getStyleClass().add("muted");

        VBox box = new VBox(12, sectionTitle, sectionDescription, content);
        box.getStyleClass().add("section");
        return box;
    }

    private VBox colorToken(String role, String hex, String surfaceClass, String textClass) {
        Label roleLabel = new Label(role);
        roleLabel.getStyleClass().addAll("token-title", textClass);

        Label hexLabel = new Label(hex);
        hexLabel.getStyleClass().addAll("token-hex", textClass);

        VBox token = new VBox(8, roleLabel, hexLabel);
        token.getStyleClass().addAll("token", surfaceClass);
        token.setPadding(new Insets(14));
        token.setPrefWidth(180);
        return token;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
