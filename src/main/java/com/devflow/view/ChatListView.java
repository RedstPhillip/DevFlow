package com.devflow.view;

import com.devflow.model.Chat;
import com.devflow.util.DateFormatter;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Sectioned chat list shown inside the sidebar.
 *
 * Layout is a single VBox stacked into a ScrollPane:
 *   [section header  Direktnachrichten  (chevron ▾)]
 *   [ ListView of DM chats                        ]
 *   [section header  Gruppen           (chevron ▾)]
 *   [ ListView of group chats                     ]
 *   [ empty-state label when both lists are empty ]
 *
 * Each section is collapsible via its header. The section headers mimic a
 * folder-style tree (VS Code's Explorer). Selection is unified across both
 * inner lists so only one chat is ever highlighted at a time.
 */
public class ChatListView extends StackPane {

    private final ObservableList<Chat> dmChats   = FXCollections.observableArrayList();
    private final ObservableList<Chat> groupChats = FXCollections.observableArrayList();
    private final FilteredList<Chat> dmFiltered   = new FilteredList<>(dmChats, c -> true);
    private final FilteredList<Chat> groupFiltered = new FilteredList<>(groupChats, c -> true);

    private final ListView<Chat> dmList   = new ListView<>(dmFiltered);
    private final ListView<Chat> groupList = new ListView<>(groupFiltered);

    private final Label dmCount    = new Label();
    private final Label groupCount = new Label();
    private final Label dmChevron  = new Label("\u25BE"); // ▾
    private final Label groupChevron = new Label("\u25BE");

    private final VBox dmSection;
    private final VBox groupSection;
    private final Label emptyLabel;

    private Consumer<Chat> onChatSelected;
    private long currentUserId;
    private String filterText = "";
    private boolean suppressSelectionEvents = false;
    private boolean loaded = false;

    public ChatListView() {
        getStyleClass().add("chat-list-panel");

        // DM section ────────────────────────────
        Node dmHeader = buildSectionHeader("Direktnachrichten", dmChevron, dmCount,
                () -> toggleSection(dmList, dmChevron));
        dmList.getStyleClass().addAll("list-view-clean", "chat-list-inner");
        dmList.setCellFactory(lv -> new ChatListCell());
        dmList.setFocusTraversable(false);
        bindUnifiedSelection(dmList, groupList);
        dmList.prefHeightProperty().bind(
                javafx.beans.binding.Bindings.size(dmFiltered).multiply(56));
        dmList.managedProperty().bind(dmList.visibleProperty());
        dmSection = new VBox(dmHeader, dmList);

        // Group section ──────────────────────────
        Node groupHeader = buildSectionHeader("Gruppen", groupChevron, groupCount,
                () -> toggleSection(groupList, groupChevron));
        groupList.getStyleClass().addAll("list-view-clean", "chat-list-inner");
        groupList.setCellFactory(lv -> new ChatListCell());
        groupList.setFocusTraversable(false);
        bindUnifiedSelection(groupList, dmList);
        groupList.prefHeightProperty().bind(
                javafx.beans.binding.Bindings.size(groupFiltered).multiply(56));
        groupList.managedProperty().bind(groupList.visibleProperty());
        groupSection = new VBox(groupHeader, groupList);

        VBox content = new VBox(dmSection, groupSection);
        content.getStyleClass().add("chat-list-content");

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.getStyleClass().add("chat-list-scroll");

        // Empty state shown when both sections are empty.
        emptyLabel = new Label("Lade Unterhaltungen \u2026");
        emptyLabel.getStyleClass().add("empty-state-text");
        StackPane emptyState = new StackPane(emptyLabel);
        emptyState.getStyleClass().add("empty-state");
        emptyState.setPadding(new Insets(40));
        emptyState.visibleProperty().bind(
                javafx.beans.binding.Bindings.isEmpty(dmFiltered)
                        .and(javafx.beans.binding.Bindings.isEmpty(groupFiltered)));
        emptyState.managedProperty().bind(emptyState.visibleProperty());

        getChildren().addAll(scroll, emptyState);
    }

    private HBox buildSectionHeader(String title, Label chevron, Label count, Runnable onToggle) {
        Label name = new Label(title);
        name.getStyleClass().add("chat-list-section-title");
        chevron.getStyleClass().add("chat-list-section-chevron");
        count.getStyleClass().add("chat-list-section-count");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox header = new HBox(6, chevron, name, spacer, count);
        header.getStyleClass().add("chat-list-section-header");
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(6, 12, 6, 10));
        header.setOnMouseClicked(e -> onToggle.run());
        return header;
    }

    private void toggleSection(ListView<?> list, Label chevron) {
        boolean nowVisible = !list.isVisible();
        list.setVisible(nowVisible);
        chevron.setText(nowVisible ? "\u25BE" : "\u25B8"); // ▾ expanded, ▸ collapsed
    }

    /** Selecting in one inner list clears selection in the other, so at most one chat is active. */
    private void bindUnifiedSelection(ListView<Chat> own, ListView<Chat> peer) {
        own.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> {
            if (suppressSelectionEvents) return;
            if (selected != null) {
                if (peer.getSelectionModel().getSelectedItem() != null) {
                    suppressSelectionEvents = true;
                    try { peer.getSelectionModel().clearSelection(); }
                    finally { suppressSelectionEvents = false; }
                }
                if (onChatSelected != null) onChatSelected.accept(selected);
            }
        });
    }

    public void setChats(List<Chat> chatList) {
        // Remember current selection so we can restore across a refresh.
        Chat selectedDm    = dmList.getSelectionModel().getSelectedItem();
        Chat selectedGroup = groupList.getSelectionModel().getSelectedItem();
        Chat previouslySelected = selectedDm != null ? selectedDm : selectedGroup;

        List<Chat> dms = new ArrayList<>();
        List<Chat> groups = new ArrayList<>();
        for (Chat c : chatList) {
            if (c.isGroup()) groups.add(c); else dms.add(c);
        }

        suppressSelectionEvents = true;
        try {
            dmChats.setAll(dms);
            groupChats.setAll(groups);
            if (previouslySelected != null) {
                if (previouslySelected.isGroup()) {
                    for (Chat c : groups) {
                        if (c.getId() == previouslySelected.getId()) {
                            groupList.getSelectionModel().select(c);
                            break;
                        }
                    }
                } else {
                    for (Chat c : dms) {
                        if (c.getId() == previouslySelected.getId()) {
                            dmList.getSelectionModel().select(c);
                            break;
                        }
                    }
                }
            }
        } finally {
            suppressSelectionEvents = false;
        }

        dmCount.setText(String.valueOf(dms.size()));
        groupCount.setText(String.valueOf(groups.size()));

        loaded = true;
        emptyLabel.setText(filterText.isEmpty()
                ? "Noch keine Unterhaltungen.\nKlicke + um eine neue zu starten."
                : "Keine Treffer.");
    }

    public void clearSelection() {
        suppressSelectionEvents = true;
        try {
            dmList.getSelectionModel().clearSelection();
            groupList.getSelectionModel().clearSelection();
        } finally {
            suppressSelectionEvents = false;
        }
    }

    public void setFilter(String text) {
        this.filterText = text == null ? "" : text.trim().toLowerCase();
        if (filterText.isEmpty()) {
            dmFiltered.setPredicate(c -> true);
            groupFiltered.setPredicate(c -> true);
        } else {
            dmFiltered.setPredicate(c ->
                    c.getDisplayName(currentUserId).toLowerCase().contains(filterText));
            groupFiltered.setPredicate(c ->
                    c.getDisplayName(currentUserId).toLowerCase().contains(filterText));
        }
        if (loaded) {
            emptyLabel.setText(filterText.isEmpty()
                    ? "Noch keine Unterhaltungen.\nKlicke + um eine neue zu starten."
                    : "Keine Treffer.");
        }
    }

    public void setOnChatSelected(Consumer<Chat> handler) {
        this.onChatSelected = handler;
    }

    public void setCurrentUserId(long userId) {
        this.currentUserId = userId;
    }

    /** Truncate at the last whitespace boundary inside maxLen so we never split words mid-character. */
    private static String truncatePreview(String text, int maxLen) {
        if (text == null) return "";
        String flat = text.replaceAll("\\s+", " ").trim();
        if (flat.length() <= maxLen) return flat;
        int cut = flat.lastIndexOf(' ', maxLen);
        if (cut < maxLen / 2) cut = maxLen;
        return flat.substring(0, cut) + "\u2026";
    }

    private class ChatListCell extends ListCell<Chat> {
        @Override
        protected void updateItem(Chat chat, boolean empty) {
            super.updateItem(chat, empty);
            if (empty || chat == null) {
                setGraphic(null);
                setText(null);
                return;
            }

            String name = chat.getDisplayName(currentUserId);

            Avatar avatar = new Avatar(chat.isGroup() ? "# " + name : name, 36);

            Label nameLabel = new Label(name);
            nameLabel.getStyleClass().add("chat-cell-name");

            Label previewLabel = new Label();
            previewLabel.getStyleClass().add("chat-cell-preview");
            if (chat.getLastMessage() != null) {
                previewLabel.setText(truncatePreview(chat.getLastMessage().getContent(), 38));
            } else {
                previewLabel.setText("Noch keine Nachrichten");
            }

            VBox textBox = new VBox(2, nameLabel, previewLabel);
            textBox.setAlignment(Pos.CENTER_LEFT);
            HBox.setHgrow(textBox, Priority.ALWAYS);

            Label timeLabel = new Label();
            timeLabel.getStyleClass().add("chat-cell-time");
            if (chat.getLastMessage() != null) {
                timeLabel.setText(DateFormatter.formatRelative(chat.getLastMessage().getCreatedAt()));
            }

            HBox cell = new HBox(10, avatar, textBox, timeLabel);
            cell.setAlignment(Pos.CENTER_LEFT);
            // Cells already live inside a tree-like section; left-padding mimics
            // the indentation of a child row in a VS Code-style explorer.
            cell.setPadding(new Insets(7, 12, 7, 22));

            setGraphic(cell);
            setText(null);
        }
    }
}
