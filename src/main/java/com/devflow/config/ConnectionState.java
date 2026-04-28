package com.devflow.config;

import javafx.application.Platform;

import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Consumer;

/**
 * Process-wide flag for "we have a working connection to the backend".
 *
 * <p>{@link com.devflow.service.HttpService} flips it to {@code false} on
 * any I/O failure (network unreachable, connection refused, timeout) and
 * back to {@code true} as soon as a request returns any response — even an
 * HTTP error like 4xx/5xx counts as "online", because it proves the server
 * is reachable. This deliberately ignores HTTP status codes; the indicator
 * is about <em>reachability</em>, not authorization.</p>
 *
 * <p>Listeners always fire on the FX thread so view code can mutate the UI
 * without {@code Platform.runLater}. The class is thread-safe via a volatile
 * flag and a {@link CopyOnWriteArraySet} of listeners.</p>
 */
public class ConnectionState {

    private static final ConnectionState INSTANCE = new ConnectionState();

    /** Optimistic default — until we attempt a request we don't know either way. */
    private volatile boolean online = true;
    private volatile long lastOnlineAtMs = System.currentTimeMillis();
    private final CopyOnWriteArraySet<Consumer<Boolean>> listeners = new CopyOnWriteArraySet<>();

    private ConnectionState() {}

    public static ConnectionState getInstance() { return INSTANCE; }

    public boolean isOnline() { return online; }

    public void markOnline()  {
        lastOnlineAtMs = System.currentTimeMillis();
        setState(true);
    }
    public void markOffline() { setState(false); }

    public void markOfflineIfNoOnlineSince(long startedAtMs) {
        if (lastOnlineAtMs > startedAtMs) return;
        setState(false);
    }

    private void setState(boolean newValue) {
        // Same-state transitions are noise; skip the notify to avoid
        // pointless UI churn on every successful request.
        if (online == newValue) return;
        online = newValue;
        notifyOnFx(newValue);
    }

    public void addListener(Consumer<Boolean> l) { if (l != null) listeners.add(l); }
    public void removeListener(Consumer<Boolean> l) { listeners.remove(l); }

    private void notifyOnFx(boolean snapshot) {
        Runnable fire = () -> {
            for (Consumer<Boolean> l : listeners) {
                try { l.accept(snapshot); } catch (RuntimeException ex) {
                    System.err.println("ConnectionState listener threw: " + ex.getMessage());
                }
            }
        };
        if (Platform.isFxApplicationThread()) fire.run(); else Platform.runLater(fire);
    }
}
