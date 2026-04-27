package com.devflow.view;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;

public class ChatView extends VBox {

    private final StackPane avatarHost;
    private final Label headerName;
    private final Label headerStatus;
    private final VBox messagesBox;
    private final ScrollPane scrollPane;
    private final TextArea inputField;
    private final Button sendButton;
    private final Button infoButton;
    private final Button aboutTab;
    private final Button membersTab;
    private final Button integrationsTab;
    private final Label inspectorMark;
    private final Label inspectorTitle;
    private final Label inspectorSubtitle;
    private final Label inspectorWorkspace;
    private final Label inspectorType;
    private final Label inspectorMembers;
    private final VBox aboutPane;
    private final VBox membersPane;
    private final VBox integrationsPane;

    public ChatView() {
        getStyleClass().add("chat-view");

        avatarHost = new StackPane();
        avatarHost.setMinSize(40, 40);
        avatarHost.setPrefSize(40, 40);
        avatarHost.setMaxSize(40, 40);

        headerName = new Label();
        headerName.getStyleClass().add("chat-header-name");

        headerStatus = new Label();
        headerStatus.getStyleClass().add("chat-header-status");

        VBox headerText = new VBox(2, headerName, headerStatus);
        headerText.setAlignment(Pos.CENTER_LEFT);
        headerText.setMinWidth(0);
        HBox.setHgrow(headerText, Priority.ALWAYS);
        headerName.setMaxWidth(Double.MAX_VALUE);
        headerName.setTextOverrun(OverrunStyle.ELLIPSIS);
        headerStatus.setMaxWidth(Double.MAX_VALUE);
        headerStatus.setTextOverrun(OverrunStyle.ELLIPSIS);

        infoButton = buildHeaderToolButton(Feather.SLIDERS, "Unterhaltung verwalten");
        infoButton.setVisible(false);
        infoButton.setManaged(false);

        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);

        HBox header = new HBox(12, avatarHost, headerText, headerSpacer, infoButton);
        header.getStyleClass().add("chat-header");
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(10, 18, 10, 18));

        messagesBox = new VBox(2);
        messagesBox.getStyleClass().add("messages-box");

        scrollPane = new ScrollPane(messagesBox);
        scrollPane.getStyleClass().add("messages-scroll");
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        HBox.setHgrow(scrollPane, Priority.ALWAYS);

        inspectorMark = new Label("#");
        inspectorMark.getStyleClass().add("chat-inspector-mark");
        StackPane inspectorMarkHost = new StackPane(inspectorMark);
        inspectorMarkHost.getStyleClass().add("chat-inspector-mark-host");

        inspectorTitle = new Label();
        inspectorTitle.getStyleClass().add("chat-inspector-title");
        inspectorTitle.setWrapText(true);

        inspectorSubtitle = new Label();
        inspectorSubtitle.getStyleClass().add("chat-inspector-subtitle");
        inspectorSubtitle.setWrapText(true);

        VBox inspectorIdentity = new VBox(8, inspectorMarkHost, inspectorTitle, inspectorSubtitle);
        inspectorIdentity.getStyleClass().add("chat-inspector-identity");
        inspectorIdentity.setAlignment(Pos.CENTER);

        Label propertiesTitle = new Label("EIGENSCHAFTEN");
        propertiesTitle.getStyleClass().add("chat-inspector-section-title");

        inspectorWorkspace = new Label();
        inspectorWorkspace.getStyleClass().add("chat-inspector-property-value");
        inspectorType = new Label();
        inspectorType.getStyleClass().add("chat-inspector-property-value");
        inspectorMembers = new Label();
        inspectorMembers.getStyleClass().add("chat-inspector-property-value");

        VBox properties = new VBox(12,
                propertiesTitle,
                buildInspectorProperty("Workspace", inspectorWorkspace),
                buildInspectorProperty("Typ", inspectorType),
                buildInspectorProperty("Mitglieder", inspectorMembers)
        );
        properties.getStyleClass().add("chat-inspector-properties");

        aboutPane = new VBox(18, inspectorIdentity, properties, buildInspectorEmptyPanel(
                Feather.PAPERCLIP,
                "Geteilte Dateien",
                "Noch keine Dateien geteilt."
        ));
        aboutPane.getStyleClass().add("chat-inspector-pane");

        membersPane = new VBox(12, buildInspectorEmptyPanel(
                Feather.USERS,
                "Mitglieder",
                "Mitgliederverwaltung wird angezeigt, sobald der Chat Daten liefert."
        ));
        membersPane.getStyleClass().add("chat-inspector-pane");

        integrationsPane = new VBox(12, buildInspectorEmptyPanel(
                Feather.GIT_BRANCH,
                "Integrationen",
                "Keine Integration mit diesem Channel verbunden."
        ));
        integrationsPane.getStyleClass().add("chat-inspector-pane");

        StackPane inspectorPaneHost = new StackPane(aboutPane, membersPane, integrationsPane);
        VBox.setVgrow(inspectorPaneHost, Priority.ALWAYS);

        aboutTab = buildInspectorTab("Über", "about");
        membersTab = buildInspectorTab("Mitglieder", "members");
        integrationsTab = buildInspectorTab("Integrationen", "integrations");
        HBox inspectorTabs = new HBox(4, aboutTab, membersTab, integrationsTab);
        inspectorTabs.getStyleClass().add("chat-inspector-tabs");
        inspectorTabs.setMinWidth(0);
        for (Button tab : new Button[] { aboutTab, membersTab, integrationsTab }) {
            tab.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(tab, Priority.ALWAYS);
        }

        VBox inspector = new VBox(16, inspectorTabs, inspectorPaneHost);
        inspector.getStyleClass().add("chat-inspector");
        inspector.setPadding(new Insets(20, 18, 18, 18));
        inspector.setMinWidth(336);
        inspector.setPrefWidth(352);
        inspector.setMaxWidth(380);

        inspector.setVisible(false);
        inspector.setManaged(false);
        showInspectorTab("about");

        messagesBox.heightProperty().addListener((obs, oldH, newH) -> {
            double v = scrollPane.getVvalue();
            if (oldH == null || oldH.doubleValue() < 1 || v >= 0.92) {
                scrollPane.setVvalue(1.0);
            }
        });

        inputField = new TextArea();
        inputField.setPromptText("Nachricht schreiben...");
        inputField.getStyleClass().add("chat-input");
        inputField.setWrapText(true);
        inputField.setPrefRowCount(1);
        inputField.setMinHeight(38);
        inputField.setPrefHeight(38);
        inputField.setMaxHeight(92);
        inputField.textProperty().addListener((obs, oldText, newText) -> {
            int lines = Math.min(4, Math.max(1, (newText == null ? "" : newText).split("\\R", -1).length));
            inputField.setPrefRowCount(lines);
            inputField.setPrefHeight(30 + lines * 18);
        });
        HBox.setHgrow(inputField, Priority.ALWAYS);

        FontIcon sendIcon = new FontIcon(Feather.SEND);
        sendIcon.getStyleClass().add("chat-send-icon");
        sendButton = new Button("Senden", sendIcon);
        sendButton.getStyleClass().add("chat-send-btn");

        HBox composerToolbar = new HBox(6,
                buildComposerAction(Feather.CODE, "Code", "`"),
                buildComposerAction(Feather.AT_SIGN, "Mention", "@"),
                buildComposerAction(Feather.LINK, "Link", "https://"),
                buildComposerAction(Feather.HASH, "Referenz", "#")
        );
        composerToolbar.getStyleClass().add("chat-composer-toolbar");

        HBox inputRow = new HBox(10, inputField, sendButton);
        inputRow.getStyleClass().add("chat-composer-input-row");
        inputRow.setAlignment(Pos.BOTTOM_CENTER);

        VBox inputBar = new VBox(8, composerToolbar, inputRow);
        inputBar.getStyleClass().add("chat-input-bar");
        inputBar.setPadding(new Insets(10, 12, 12, 12));
        inputBar.setMinWidth(0);
        inputBar.setMaxWidth(980);

        StackPane composerShell = new StackPane(inputBar);
        composerShell.getStyleClass().add("chat-composer-shell");
        composerShell.setMinWidth(0);
        StackPane.setAlignment(inputBar, Pos.CENTER);

        VBox chatColumn = new VBox(scrollPane, composerShell);
        chatColumn.getStyleClass().add("chat-column");
        chatColumn.setMinWidth(0);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        HBox.setHgrow(chatColumn, Priority.ALWAYS);

        HBox conversationBody = new HBox(chatColumn, inspector);
        conversationBody.getStyleClass().add("chat-body");
        VBox.setVgrow(conversationBody, Priority.ALWAYS);
        conversationBody.widthProperty().addListener((obs, oldWidth, newWidth) -> {
            boolean showInspector = newWidth.doubleValue() >= 900;
            inspector.setVisible(showInspector);
            inspector.setManaged(showInspector);
        });

        getChildren().addAll(header, conversationBody);
    }

    private Button buildHeaderToolButton(Feather icon, String tooltip) {
        FontIcon glyph = new FontIcon(icon);
        glyph.getStyleClass().add("chat-header-btn-icon");
        Button button = new Button();
        button.setGraphic(glyph);
        button.getStyleClass().add("chat-header-btn");
        button.setTooltip(new Tooltip(tooltip));
        button.setFocusTraversable(false);
        return button;
    }

    private Button buildComposerAction(Feather icon, String tooltip, String insertText) {
        FontIcon glyph = new FontIcon(icon);
        glyph.getStyleClass().add("chat-composer-btn-icon");
        Button button = new Button();
        button.setGraphic(glyph);
        button.getStyleClass().add("chat-composer-btn");
        button.setTooltip(new Tooltip(tooltip));
        button.setFocusTraversable(false);
        button.setOnAction(e -> insertAtCaret(insertText));
        return button;
    }

    private void insertAtCaret(String text) {
        int position = Math.max(0, inputField.getCaretPosition());
        inputField.insertText(position, text);
        inputField.requestFocus();
        inputField.positionCaret(position + text.length());
    }

    private Button buildInspectorTab(String label, String key) {
        Button tab = new Button(label);
        tab.getStyleClass().add("chat-inspector-tab");
        tab.setFocusTraversable(false);
        tab.setMinWidth(0);
        tab.setOnAction(e -> showInspectorTab(key));
        return tab;
    }

    private void showInspectorTab(String key) {
        aboutPane.setVisible("about".equals(key));
        aboutPane.setManaged("about".equals(key));
        membersPane.setVisible("members".equals(key));
        membersPane.setManaged("members".equals(key));
        integrationsPane.setVisible("integrations".equals(key));
        integrationsPane.setManaged("integrations".equals(key));

        aboutTab.getStyleClass().remove("chat-inspector-tab-active");
        membersTab.getStyleClass().remove("chat-inspector-tab-active");
        integrationsTab.getStyleClass().remove("chat-inspector-tab-active");
        switch (key) {
            case "members" -> membersTab.getStyleClass().add("chat-inspector-tab-active");
            case "integrations" -> integrationsTab.getStyleClass().add("chat-inspector-tab-active");
            default -> aboutTab.getStyleClass().add("chat-inspector-tab-active");
        }
    }

    private VBox buildInspectorEmptyPanel(Feather icon, String title, String detail) {
        FontIcon glyph = new FontIcon(icon);
        glyph.getStyleClass().add("chat-inspector-empty-icon");
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("chat-inspector-empty-title");
        Label detailLabel = new Label(detail);
        detailLabel.getStyleClass().add("chat-inspector-empty-detail");
        detailLabel.setWrapText(true);
        VBox panel = new VBox(8, glyph, titleLabel, detailLabel);
        panel.getStyleClass().add("chat-inspector-empty-panel");
        return panel;
    }

    private HBox buildInspectorProperty(String label, Label value) {
        Label key = new Label(label);
        key.getStyleClass().add("chat-inspector-property-label");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox row = new HBox(10, key, spacer, value);
        row.getStyleClass().add("chat-inspector-property-row");
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    public void setContextInfo(String title, String type, String members, String workspace) {
        String safeTitle = title == null || title.isBlank() ? "Unterhaltung" : title;
        String normalizedType = type == null || type.isBlank() ? "Unterhaltung" : type;
        inspectorTitle.setText(safeTitle);
        inspectorWorkspace.setText(workspace == null || workspace.isBlank() ? "-" : workspace);
        inspectorType.setText(normalizedType);
        inspectorMembers.setText(members == null || members.isBlank() ? "-" : members);
        boolean direct = normalizedType.toLowerCase().contains("direkt");
        inspectorMark.setText(direct ? "@" : "#");
        inspectorSubtitle.setText(direct ? "Direktnachricht" : "Channel für Team-Kontext");
    }

    public void scrollToBottom() {
        javafx.application.Platform.runLater(() ->
                javafx.application.Platform.runLater(() -> scrollPane.setVvalue(1.0)));
    }

    public StackPane getAvatarHost() { return avatarHost; }
    public Label getHeaderName() { return headerName; }
    public Label getHeaderStatus() { return headerStatus; }
    public VBox getMessagesBox() { return messagesBox; }
    public ScrollPane getScrollPane() { return scrollPane; }
    public TextArea getInputField() { return inputField; }
    public Button getSendButton() { return sendButton; }
    public Button getInfoButton() { return infoButton; }
}
