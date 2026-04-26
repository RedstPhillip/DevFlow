package com.devflow.config;

import com.devflow.model.Group;
import com.devflow.service.GroupService;
import javafx.application.Platform;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Consumer;

/**
 * Process-wide cache of the current workspace's groups.
 *
 * <p>Unlike {@link WorkspaceState}, which just holds a single value, this
 * caches the most recently loaded group list and exposes it synchronously to
 * callers who need to render a sidebar/tree without another round-trip. The
 * cache is scoped to a single {@code workspaceId} — switching workspaces
 * invalidates it.</p>
 *
 * <p>Controller code calls {@link #loadFor(long)} on workspace switch /
 * group-CRUD and then reads {@link #getGroups()} whenever it needs the
 * up-to-date list. Listeners fire on the FX thread so UI code can update
 * without {@code Platform.runLater}.</p>
 */
public class GroupState {

    private static final GroupState INSTANCE = new GroupState();

    private final GroupService groupService = new GroupService();
    private volatile long currentWorkspaceId = 0;
    private volatile List<Group> groups = Collections.emptyList();
    private final CopyOnWriteArraySet<Consumer<List<Group>>> listeners = new CopyOnWriteArraySet<>();
    /** Retained so callers can subscribe to load errors separately from success. */
    private final CopyOnWriteArrayList<Consumer<Throwable>> errorListeners = new CopyOnWriteArrayList<>();

    private GroupState() {}

    public static GroupState getInstance() { return INSTANCE; }

    public long getCurrentWorkspaceId() { return currentWorkspaceId; }

    /** Immutable snapshot. Safe to iterate without locking. */
    public List<Group> getGroups() { return groups; }

    /**
     * Find a group by id in the current cache. Returns {@code null} if not
     * cached (e.g. stale UI holding an id from before a refresh).
     */
    public Group findById(long groupId) {
        for (Group g : groups) {
            if (g.getId() == groupId) return g;
        }
        return null;
    }

    /**
     * Fetch groups for {@code workspaceId} and replace the cache. On success
     * notifies all listeners on the FX thread. On failure notifies error
     * listeners (also on FX thread) and leaves the cache untouched so UI
     * doesn't blank out during a transient network blip.
     */
    public void loadFor(long workspaceId) {
        if (workspaceId <= 0) {
            // Placeholder workspace — clear cache so stale groups don't leak.
            this.currentWorkspaceId = workspaceId;
            this.groups = Collections.emptyList();
            notifyOnFx(Collections.emptyList());
            return;
        }
        this.currentWorkspaceId = workspaceId;
        groupService.listGroups(workspaceId)
                .whenComplete((list, err) -> {
                    // Protect against a fast workspace-switch race: if the
                    // user already moved on by the time this response lands,
                    // ignore it rather than overwrite a newer cache.
                    if (this.currentWorkspaceId != workspaceId) return;
                    if (err != null) {
                        Platform.runLater(() -> {
                            for (var l : errorListeners) {
                                try { l.accept(err); } catch (RuntimeException ignored) {}
                            }
                        });
                        return;
                    }
                    this.groups = list != null ? List.copyOf(list) : Collections.emptyList();
                    notifyOnFx(this.groups);
                });
    }

    /**
     * Push a locally-known update (e.g. after a create/rename/delete) without
     * another GET. Re-runs {@link #loadFor(long)} is also fine; this is the
     * fast path for dialogs that already received a fresh {@link Group}.
     */
    public void refreshInPlace(List<Group> newGroups) {
        this.groups = newGroups != null ? List.copyOf(newGroups) : Collections.emptyList();
        notifyOnFx(this.groups);
    }

    private void notifyOnFx(List<Group> snapshot) {
        Runnable fire = () -> {
            for (Consumer<List<Group>> l : listeners) {
                try { l.accept(snapshot); } catch (RuntimeException ex) {
                    System.err.println("GroupState listener threw: " + ex.getMessage());
                }
            }
        };
        if (Platform.isFxApplicationThread()) fire.run(); else Platform.runLater(fire);
    }

    public void addListener(Consumer<List<Group>> l) { if (l != null) listeners.add(l); }
    public void removeListener(Consumer<List<Group>> l) { listeners.remove(l); }

    public void addErrorListener(Consumer<Throwable> l) { if (l != null) errorListeners.add(l); }
    public void removeErrorListener(Consumer<Throwable> l) { errorListeners.remove(l); }
}
