package org.qualsh.lb.digital;

import java.util.function.Consumer;

/**
 * Abstraction over an external digital-mode decoder application
 * (WSJT-X, JS8Call, Fldigi, VARA, …).
 *
 * <p>Implementations communicate with the decoder app via whatever
 * protocol it supports (UDP for WSJT-X/JS8Call, XML-RPC for Fldigi,
 * etc.) and translate incoming events into common record types.
 *
 * <p>All listener callbacks are fired from background threads;
 * UI code must dispatch to the EDT via {@code SwingUtilities.invokeLater}.
 */
public interface DigitalModeBackend {

    /** Human-readable name of this backend, e.g. {@code "WSJT-X"} or {@code "Fldigi"}. */
    String name();

    /**
     * Start listening / polling.  For UDP backends {@code port} is the
     * UDP port to bind; for HTTP-based backends it is the TCP port of the
     * remote API server (e.g. 7362 for Fldigi XML-RPC).
     *
     * @return {@code true} if the backend started successfully
     */
    boolean start(int port);

    /** Stop listening / polling and free resources. */
    void stop();

    /** @return {@code true} if the backend is currently active */
    boolean isRunning();

    // ── Listener registration ─────────────────────────────────────────────────

    void addDecodedLineListener(Consumer<DecodedLine> l);
    void removeDecodedLineListener(Consumer<DecodedLine> l);

    void addQsoLoggedListener(Consumer<AutoLogEvent> l);
    void removeQsoLoggedListener(Consumer<AutoLogEvent> l);

    void addStatusListener(Consumer<StatusEvent> l);
    void removeStatusListener(Consumer<StatusEvent> l);
}
