package com.devflow.view;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;

import com.devflow.model.Chat;
import com.devflow.model.Group;
import com.devflow.util.DateFormatter;

import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * Sectioned chat list shown inside the sidebar.
 *
 * <p>Since Phase 2c the structure is:
 * <pre>
 *   [section header  Direktnachrichten]
 *   [ ListView of DM chats              ]
 *   [section header  Gruppenchats]
 *   [   sub-section  Allgemein     ]   ← chats with groupId = null
 *   [   sub-section  &lt;Group A&gt;   ]   ← one per Group, ordered by sortOrder
 *   [   sub-section  &lt;Group B&gt;   ]
 *   [ empty-state label when everything empty ]
 * </pre>
 *
 * <p>The sub-section tree is rebuilt whenever {@link #setGroups(List)} fires
 * (workspace switch or group CRUD) or when {@link #setChats(List)} re-partitions
 * chats. "Allgemein" catches {@code groupId == null} and also any chat whose
 * {@code groupId} points at a group that isn't in the current cache (stale
 * references survive to the implicit bucket rather than vanishing).</p>
 *
 * <p>Selection is unified across the DM list and every sub-section list via a
 * shared {@code suppressSelectionEvents} guard — exactly one chat is active
 * across the whole tree.</p>
 */
public class ChatListView extends StackPane {

    /** Sentinel used as the bucket key for "Allgemein" (group_id = null). */
    private static final long ALLGEMEIN_KEY = -1L;

    // ── DM section ────────────────────────────────────────────────────────
    private final ObservableList<Chat> dmChats = FXCollections.observableArrayList();
    private final FilteredList<Chat> dmFiltered = new FilteredList<>(dmChats, c -> true);
    private final ListView<Chat> dmList = new ListView<>(dmFiltered);
    private final Label dmCount = new Label();
    // Phase 3 P1.3: chevrons migrated from unicode glyphs (▾/▸) to Feather
    // FontIcons so they share -fx-icon-color with the rest of the icon set.
    private final FontIcon dmChevron = new FontIcon(Feather.CHEVRON_DOWN);

    // ── Group-chats umbrella ──────────────────────────────────────────────
    /** Group chats are workspace-scoped and shown as one flat list. */
    private final ObservableList<Chat> groupChats = FXCollections.observableArrayList();
    private final FilteredList<Chat> groupFiltered = new FilteredList<>(groupChats, c -> true);
    private final ListView<Chat> groupList = new ListView<>(groupFiltered);
    /** Raw un-partitioned list retained for older helper methods kept below. */
    private final List<Chat> allGroupChats = new ArrayList<>();
    /** Snapshot of current workspace's groups (from GroupState). */
    private List<Group> currentGroups = Collections.emptyList();
    /** Host that contains the sub-section nodes in render order. */
    private final VBox groupSectionsHost = new VBox();
    /** Umbrella "Gruppenchats" outer section (header + groupSectionsHost). */
    private final VBox groupChatsSection;
    private final Node workspaceDivider;
    private final Label groupChatsCount = new Label();
    private final FontIcon groupChatsChevron = new FontIcon(Feather.CHEVRON_DOWN);
    /** Per-bucket cached sub-section state, keyed by group-id or ALLGEMEIN_KEY. */
    private final Map<Long, SubSection> subSections = new LinkedHashMap<>();

    private final Label emptyLabel;

    private Consumer<Chat> onChatSelected;
    private long currentUserId;
    private String filterText = "";
    /**
     * Active workspace id. {@code 0} = bootstrap (no filter), {@code -1} =
     * placeholder (hide all group chats), positive = filter to matching.
     * DMs are workspace-agnostic and unaffected.
     */
    private long currentWorkspaceId = 0;
    private long desiredSelectedChatId = -1;
    private boolean suppressSelectionEvents = false;
    private boolean loaded = false;

    public ChatListView() {
        getStyleClass().add("chat-list-panel");

        // DM section
        Node dmHeader = buildSectionHeader("DMs · alle Workspaces", dmChevron, dmCount,
                () -> toggleListVisibility(dmList, dmChevron), false);
        dmList.getStyleClass().addAll("list-view-clean", "chat-list-inner");
        dmList.setCellFactory(lv -> new ChatListCell(false));
        dmList.setFocusTraversable(false);
        bindListSelection(dmList);
        dmList.prefHeightProperty().bind(Bindings.size(dmFiltered).multiply(56));
        dmList.managedProperty().bind(dmList.visibleProperty());
        VBox dmSection = new VBox(dmHeader, dmList);
        dmSection.getStyleClass().add("chat-list-dm-section");

        // Group chats: flat per-workspace list. Folder labels like "Allgemein"
        // were removed because they added noise without changing behaviour.
        workspaceDivider = buildWorkspaceDivider();
        Node groupChatsHeader = buildSectionHeader("Gruppenchats", groupChatsChevron, groupChatsCount,
                () -> toggleListVisibility(groupList, groupChatsChevron), false);
        groupList.getStyleClass().addAll("list-view-clean", "chat-list-inner");
        groupList.setCellFactory(lv -> new ChatListCell(false));
        groupList.setFocusTraversable(false);
        bindListSelection(groupList);
        groupList.prefHeightProperty().bind(Bindings.size(groupFiltered).multiply(56));
        groupList.managedProperty().bind(groupList.visibleProperty());
        groupChatsSection = new VBox(groupChatsHeader, groupList);
        updateWorkspaceSectionVisibility();

        VBox content = new VBox(dmSection, workspaceDivider, groupChatsSection);
        content.getStyleClass().add("chat-list-content");

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.getStyleClass().add("chat-list-scroll");

        // Phase 3 P2: same EmptyState language as the welcome panel and the
        // chat-view "no messages yet" placeholder — large muted glyph + text.
        // We keep the Label as a field because setChats()/setFilter() still
        // rewrite the title between "Loading", "No chats yet", and "No matches"
        // states; the icon stays MESSAGE_SQUARE in all of them.
        emptyLabel = new Label("Lade Unterhaltungen \u2026");
        emptyLabel.getStyleClass().add("empty-state-title");
        emptyLabel.setWrapText(true);
        FontIcon emptyGlyph = new FontIcon(Feather.INBOX);
        emptyGlyph.getStyleClass().add("empty-state-glyph");
        StackPane glyphHost = new StackPane(emptyGlyph);
        glyphHost.getStyleClass().add("empty-state-glyph-host");
        VBox emptyState = new VBox(8, glyphHost, emptyLabel);
        emptyState.getStyleClass().addAll("empty-state", "empty-state-card", "sidebar-empty-state");
        emptyState.setAlignment(Pos.CENTER);
        emptyState.setPadding(new Insets(12, 18, 8, 18));
        emptyState.visibleProperty().bind(
                Bindings.size(dmFiltered).isEqualTo(0)
                        .and(Bindings.size(groupFiltered).isEqualTo(0)));
        emptyState.managedProperty().bind(emptyState.visibleProperty());

        getChildren().addAll(scroll, emptyState);
    }

    // ── Section header factory ─────────────────────────────────────────────

    private HBox buildWorkspaceDivider() {
        Region line = new Region();
        line.getStyleClass().add("chat-list-workspace-divider-line");
        Label label = new Label("Workspace");
        label.getStyleClass().add("chat-list-workspace-divider-label");
        Region line2 = new Region();
        line2.getStyleClass().add("chat-list-workspace-divider-line");
        HBox.setHgrow(line, Priority.ALWAYS);
        HBox.setHgrow(line2, Priority.ALWAYS);
        HBox divider = new HBox(8, line, label, line2);
        divider.getStyleClass().add("chat-list-workspace-divider");
        divider.setAlignment(Pos.CENTER);
        return divider;
    }

    private HBox buildSectionHeader(String title, FontIcon chevron, Label count, Runnable onToggle, boolean sub) {
        Label name = new Label(title);
        name.getStyleClass().add(sub ? "chat-list-subsection-title" : "chat-list-section-title");
        boolean dmHeader = title != null && title.startsWith("DMs");
        if (dmHeader) {
            name.setText("Direktnachrichten");
            name.getStyleClass().add("chat-list-dm-title");
        }
        name.setMinWidth(0);
        name.setMaxWidth(Double.MAX_VALUE);
        name.setTextOverrun(OverrunStyle.ELLIPSIS);
        chevron.getStyleClass().add("chat-list-section-chevron");
        count.getStyleClass().add("chat-list-section-count");
        Region spacer = new Region();
        HBox.setHgrow(name, Priority.ALWAYS);
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox header = new HBox(6, chevron, name, spacer, count);
        header.getStyleClass().add(sub ? "chat-list-subsection-header" : "chat-list-section-header");
        if (dmHeader) {
            Label badge = new Label("GLOBAL");
            badge.getStyleClass().add("chat-list-global-badge");
            header.getStyleClass().add("chat-list-dm-header");
            header.getChildren().add(2, badge);
        }
        header.setAlignment(Pos.CENTER_LEFT);
        // Sub-sections get extra left padding so the chevron aligns under the
        // umbrella's title text — reads as a tree in a single glance.
        header.setPadding(sub ? new Insets(4, 12, 4, 22) : new Insets(6, 12, 6, 10));
        header.setOnMouseClicked(e -> onToggle.run());
        return header;
    }

    private void toggleListVisibility(ListView<?> list, FontIcon chevron) {
        boolean nowVisible = !list.isVisible();
        list.setVisible(nowVisible);
        chevron.setIconCode(nowVisible ? Feather.CHEVRON_DOWN : Feather.CHEVRON_RIGHT);
    }

    private void toggleNodeVisibility(Node node, FontIcon chevron) {
        boolean nowVisible = !node.isVisible();
        node.setVisible(nowVisible);
        chevron.setIconCode(nowVisible ? Feather.CHEVRON_DOWN : Feather.CHEVRON_RIGHT);
    }

    /** Hook every list's selection into the unified callback + peer clearing. */
    private void bindListSelection(ListView<Chat> list) {
        list.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> {
            if (suppressSelectionEvents) return;
            if (selected == null) return;
            // Clear selection on every other list so at most one row is active.
            suppressSelectionEvents = true;
            try {
                if (dmList != list) dmList.getSelectionModel().clearSelection();
                if (groupList != list) groupList.getSelectionModel().clearSelection();
                for (SubSection s : subSections.values()) {
                    if (s.list != list) s.list.getSelectionModel().clearSelection();
                }
            } finally {
                suppressSelectionEvents = false;
            }
            desiredSelectedChatId = selected.getId();
            if (onChatSelected != null) onChatSelected.accept(selected);
        });
    }

    // ── Public API ─────────────────────────────────────────────────────────

    public void setChats(List<Chat> chatList) {
        // Remember current selection (across all lists) so we can restore it.
        Chat previouslySelected = currentSelection();
        long restoreId = previouslySelected != null ? previouslySelected.getId() : desiredSelectedChatId;

        List<Chat> dms = new ArrayList<>();
        List<Chat> groups = new ArrayList<>();
        for (Chat c : chatList) {
            if (c.isGroupChat()) groups.add(c);
            else dms.add(c);
        }

        suppressSelectionEvents = true;
        try {
            dmChats.setAll(dms);
            groupChats.setAll(groups);
            restoreSelectionById(restoreId);
        } finally {
            suppressSelectionEvents = false;
        }

        refreshCounts();
        loaded = true;
        emptyLabel.setText(filterText.isEmpty()
                ? "Noch keine Unterhaltungen.\nKlicke + um eine neue zu starten."
                : "Keine Treffer.");
    }

    /**
     * Push a new groups snapshot. Rebuilds the sub-section tree, preserving
     * existing sub-sections (so collapse state survives across refreshes) and
     * dropping any whose group no longer exists. Called by ChatListController
     * on GroupState changes.
     */
    public void setGroups(List<Group> groups) {
        this.currentGroups = groups != null ? groups : Collections.emptyList();
        refreshCounts();
    }

    public void clearSelection() {
        suppressSelectionEvents = true;
        try {
            dmList.getSelectionModel().clearSelection();
            groupList.getSelectionModel().clearSelection();
            for (SubSection s : subSections.values()) {
                s.list.getSelectionModel().clearSelection();
            }
        } finally {
            suppressSelectionEvents = false;
        }
        desiredSelectedChatId = -1;
    }

    public void setFilter(String text) {
        this.filterText = text == null ? "" : text.trim().toLowerCase();
        applyPredicates();
        if (loaded) {
            emptyLabel.setText(filterText.isEmpty()
                    ? "Noch keine Unterhaltungen.\nKlicke + um eine neue zu starten."
                    : "Keine Treffer.");
        }
    }

    public void setCurrentWorkspaceId(long workspaceId) {
        this.currentWorkspaceId = workspaceId;
        updateWorkspaceSectionVisibility();
        applyPredicates();
    }

    public void setOnChatSelected(Consumer<Chat> handler) {
        this.onChatSelected = handler;
    }

    public void setCurrentUserId(long userId) {
        this.currentUserId = userId;
    }

    public void selectChat(Chat chat) {
        desiredSelectedChatId = chat == null ? -1 : chat.getId();
        restoreSelectionById(desiredSelectedChatId);
    }

    // ── Internals ──────────────────────────────────────────────────────────

    /**
     * Rebuild the ordered list of sub-sections to match current groups.
     * Existing sub-sections are reused (preserves their expanded state); any
     * section whose key is no longer needed is removed.
     */
    private void rebuildGroupSections() {
        // Desired order: Allgemein first, then each group by its position in
        // the currentGroups list (already sorted server-side by sort_order).
        List<Long> desiredKeys = new ArrayList<>();
        desiredKeys.add(ALLGEMEIN_KEY);
        for (Group g : currentGroups) desiredKeys.add(g.getId());

        // Drop sub-sections that are no longer in the desired set.
        subSections.keySet().retainAll(desiredKeys);

        // Add any missing sub-sections. Ordering is enforced below when we
        // repopulate groupSectionsHost's children list.
        for (Long key : desiredKeys) {
            subSections.computeIfAbsent(key, k -> createSubSection());
        }

        // Repopulate host in desired order. For each sub-section we add its
        // header, then its list view.
        groupSectionsHost.getChildren().clear();
        for (Long key : desiredKeys) {
            SubSection s = subSections.get(key);
            s.label.setText(labelFor(key));
            groupSectionsHost.getChildren().add(s.container);
        }
    }

    private String labelFor(long key) {
        if (key == ALLGEMEIN_KEY) return "Allgemein";
        for (Group g : currentGroups) if (g.getId() == key) return g.getName();
        // Stale key that slipped through — render neutral rather than crash.
        return "Gruppe";
    }

    private SubSection createSubSection() {
        SubSection s = new SubSection();
        s.chats = FXCollections.observableArrayList();
        s.filtered = new FilteredList<>(s.chats, c -> matchesText(c) && matchesWorkspace(c));
        s.list = new ListView<>(s.filtered);
        s.list.getStyleClass().addAll("list-view-clean", "chat-list-inner");
        s.list.setCellFactory(lv -> new ChatListCell(true));
        s.list.setFocusTraversable(false);
        s.list.prefHeightProperty().bind(Bindings.size(s.filtered).multiply(56));
        s.list.managedProperty().bind(s.list.visibleProperty());
        bindListSelection(s.list);

        s.chevron = new FontIcon(Feather.CHEVRON_DOWN);
        s.count = new Label();
        s.label = new Label();
        s.label.getStyleClass().add("chat-list-subsection-title");
        s.label.setMinWidth(0);
        s.label.setMaxWidth(Double.MAX_VALUE);
        s.label.setTextOverrun(OverrunStyle.ELLIPSIS);
        s.chevron.getStyleClass().add("chat-list-section-chevron");
        s.count.getStyleClass().add("chat-list-section-count");
        Region spacer = new Region();
        HBox.setHgrow(s.label, Priority.ALWAYS);
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox header = new HBox(6, s.chevron, s.label, spacer, s.count);
        header.getStyleClass().add("chat-list-subsection-header");
        header.setAlignment(Pos.CENTER_LEFT);
        // Polish-Pass §4: sub-section row height ≈ 28 (was ≈ 24).
        header.setPadding(new Insets(6, 12, 6, 22));
        header.setOnMouseClicked(e -> toggleListVisibility(s.list, s.chevron));

        // Hide the entire sub-section when it has no chats matching the
        // current filter. Otherwise we'd show "Frontend (0)" for every group
        // that happens to be empty, creating a lot of noise in tree form.
        s.container = new VBox(header, s.list);
        s.container.visibleProperty().bind(Bindings.size(s.filtered).greaterThan(0));
        s.container.managedProperty().bind(s.container.visibleProperty());
        return s;
    }

    /** Re-bucket {@link #allGroupChats} into per-group lists. */
    private void repartitionGroupChats() {
        // Clear existing buckets first — setChats may have shuffled items
        // between groups (e.g. a chat was moved to a different folder).
        for (SubSection s : subSections.values()) s.chats.clear();

        for (Chat c : allGroupChats) {
            Long gid = c.getGroupId();
            long key = (gid == null) ? ALLGEMEIN_KEY : gid;
            SubSection s = subSections.get(key);
            if (s == null) {
                // groupId references a group not in the current cache (deleted
                // or not yet loaded) — fall back to Allgemein so the chat is
                // still reachable.
                s = subSections.get(ALLGEMEIN_KEY);
            }
            if (s != null) s.chats.add(c);
        }
    }

    private void applyPredicates() {
        dmFiltered.setPredicate(this::matchesText);
        groupFiltered.setPredicate(c -> matchesText(c) && matchesWorkspace(c));
        for (SubSection s : subSections.values()) {
            s.filtered.setPredicate(c -> matchesText(c) && matchesWorkspace(c));
        }
        refreshCounts();
    }

    private void updateWorkspaceSectionVisibility() {
        boolean hasWorkspace = currentWorkspaceId > 0;
        if (groupChatsSection != null) {
            groupChatsSection.setVisible(hasWorkspace);
            groupChatsSection.setManaged(hasWorkspace);
        }
        if (workspaceDivider != null) {
            workspaceDivider.setVisible(hasWorkspace);
            workspaceDivider.setManaged(hasWorkspace);
        }
    }

    private void refreshCounts() {
        dmCount.setText(String.valueOf(dmFiltered.size()));
        int total = groupFiltered.size();
        for (SubSection s : subSections.values()) {
            int n = s.filtered.size();
            s.count.setText(String.valueOf(n));
            total += n;
        }
        groupChatsCount.setText(String.valueOf(total));
    }

    private boolean matchesText(Chat c) {
        if (filterText.isEmpty()) return true;
        return c.getDisplayName(currentUserId).toLowerCase().contains(filterText);
    }

    private boolean matchesWorkspace(Chat c) {
        if (currentWorkspaceId == 0) return true;
        if (currentWorkspaceId < 0) return false;
        Long wsId = c.getWorkspaceId();
        return wsId != null && wsId == currentWorkspaceId;
    }

    private Chat currentSelection() {
        Chat sel = dmList.getSelectionModel().getSelectedItem();
        if (sel != null) return sel;
        sel = groupList.getSelectionModel().getSelectedItem();
        if (sel != null) return sel;
        for (SubSection s : subSections.values()) {
            Chat c = s.list.getSelectionModel().getSelectedItem();
            if (c != null) return c;
        }
        return null;
    }

    private void restoreSelection(Chat previouslySelected) {
        if (previouslySelected == null) return;
        restoreSelectionById(previouslySelected.getId());
    }

    private void restoreSelectionById(long id) {
        if (id <= 0) return;
        for (Chat c : groupChats) {
            if (c.getId() == id && matchesWorkspace(c)) {
                groupList.getSelectionModel().select(c);
                return;
            }
        }
        for (SubSection s : subSections.values()) {
            for (Chat c : s.chats) {
                if (c.getId() == id) {
                    s.list.getSelectionModel().select(c);
                    return;
                }
            }
        }
        for (Chat c : dmChats) {
            if (c.getId() == id) {
                dmList.getSelectionModel().select(c);
                return;
            }
        }
    }

    private boolean isAllGroupSubSectionsEmpty() {
        for (SubSection s : subSections.values()) {
            if (!s.filtered.isEmpty()) return false;
        }
        return true;
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

    // ── Helper types ───────────────────────────────────────────────────────

    /** Per-bucket state. Grouped into a tiny class so the map stays readable. */
    private static class SubSection {
        VBox container;
        Label label;
        FontIcon chevron;
        Label count;
        ObservableList<Chat> chats;
        FilteredList<Chat> filtered;
        ListView<Chat> list;
    }

    /**
     * Recyclable cell renderer.
     *
     * <p>Earlier versions allocated a fresh node tree (Avatar + 3 labels +
     * VBox + HBox) on every {@code updateItem} call. JavaFX recycles
     * ListCell instances aggressively while scrolling — for a list of N
     * chats only a handful of cells actually exist physically — so the old
     * code threw away dozens of node sub-trees per scroll tick. That
     * thrashed the GC, occasionally caused styling flicker (newly-built
     * labels start without their CSS pseudo-classes applied), and made
     * scrolling visibly stutter on slower machines.</p>
     *
     * <p>Now the entire node graph is built once in the constructor and
     * {@code updateItem} only mutates text + avatar identity. Recycle-safe.</p>
     */
    private class ChatListCell extends ListCell<Chat> {
        private final Avatar avatar = new Avatar("?", 36);
        private final Label nameLabel = new Label();
        private final Label previewLabel = new Label();
        private final Label timeLabel = new Label();
        private final HBox row;

        ChatListCell(boolean indented) {
            nameLabel.getStyleClass().add("chat-cell-name");
            previewLabel.getStyleClass().add("chat-cell-preview");
            timeLabel.getStyleClass().add("chat-cell-time");
            nameLabel.setMinWidth(0);
            nameLabel.setMaxWidth(Double.MAX_VALUE);
            nameLabel.setTextOverrun(OverrunStyle.ELLIPSIS);
            previewLabel.setMinWidth(0);
            previewLabel.setMaxWidth(Double.MAX_VALUE);
            previewLabel.setTextOverrun(OverrunStyle.ELLIPSIS);
            timeLabel.setMinWidth(Region.USE_PREF_SIZE);
            timeLabel.setTextOverrun(OverrunStyle.CLIP);
            VBox textBox = new VBox(2, nameLabel, previewLabel);
            textBox.setAlignment(Pos.CENTER_LEFT);
            textBox.setMinWidth(0);
            textBox.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(textBox, Priority.ALWAYS);
            row = new HBox(10, avatar, textBox, timeLabel);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setMinWidth(0);
            row.setMaxWidth(Double.MAX_VALUE);
            // Group chats sit one level deeper in the folder tree; DMs keep
            // the tighter left edge so previews do not get squeezed.
            row.setPadding(indented ? new Insets(7, 12, 7, 32) : new Insets(7, 12, 7, 12));
        }

        @Override
        protected void updateItem(Chat chat, boolean empty) {
            super.updateItem(chat, empty);
            if (empty || chat == null) {
                // Clear the graphic AND wipe text on the cached labels —
                // otherwise a stale name briefly flashes when the cell
                // recycles for a different (or null) row before the next
                // updateItem fires with real data.
                setGraphic(null);
                setText(null);
                nameLabel.setText("");
                previewLabel.setText("");
                timeLabel.setText("");
                return;
            }
            String name = chat.getDisplayName(currentUserId);
            avatar.setName(chat.isGroupChat() ? "# " + name : name);
            nameLabel.setText(name);
            if (chat.getLastMessage() != null) {
                previewLabel.setText(truncatePreview(chat.getLastMessage().getContent(), 38));
                timeLabel.setText(DateFormatter.formatRelative(chat.getLastMessage().getCreatedAt()));
            } else {
                previewLabel.setText("Noch keine Nachrichten");
                timeLabel.setText("");
            }
            setGraphic(row);
            setText(null);
        }
    }
}
