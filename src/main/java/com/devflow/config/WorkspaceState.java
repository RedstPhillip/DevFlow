package com.devflow.config;

import com.devflow.model.Workspace;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Consumer;

/**
 * Process-wide holder for the currently-active workspace.
 *
 * <p>Initialised by {@code MainController} after login with the user's
 * "Persoenlich" workspace (resolved from
 * {@link com.devflow.service.WorkspaceService#listWorkspaces()}). Mutated
 * when the user clicks a workspace in the sidebar switcher, or after
 * create/join flows.</p>
 *
 * <p>Singleton + plain getters, matching the existing {@link TokenStore}
 * style. A listener set is exposed so {@code ChatListController} (and the
 * sidebar header label) can react to switches without polling.</p>
 *
 * <p><b>Threading:</b> {@link #setCurrent(Workspace)} notifies listeners
 * synchronously on the calling thread. Listeners that touch the JavaFX scene
 * graph are responsible for hopping to the application thread themselves
 * (e.g. {@code Platform.runLater}). Notification errors from one listener
 * must not stop others from being invoked.</p>
 */
public class WorkspaceState {

    private static final WorkspaceState INSTANCE = new WorkspaceState();

    private volatile Workspace current;
    private final Set<Consumer<Workspace>> listeners = new CopyOnWriteArraySet<>();

    private WorkspaceState() {}

    public static WorkspaceState getInstance() {
        return INSTANCE;
    }

    public Workspace getCurrent() {
        return current;
    }

    /**
     * Set the current workspace and notify every registered listener.
     * Pass {@code null} on logout to clear state.
     */
    public void setCurrent(Workspace workspace) {
        this.current = workspace;
        for (Consumer<Workspace> listener : listeners) {
            try {
                listener.accept(workspace);
            } catch (RuntimeException ex) {
                // Swallow so one broken listener cannot starve the others.
                // Kept visible on stderr so bugs in listeners remain diagnosable.
                System.err.println("WorkspaceState listener threw: " + ex.getMessage());
            }
        }
    }

    public void addListener(Consumer<Workspace> listener) {
        if (listener != null) listeners.add(listener);
    }

    public void removeListener(Consumer<Workspace> listener) {
        listeners.remove(listener);
    }
}
