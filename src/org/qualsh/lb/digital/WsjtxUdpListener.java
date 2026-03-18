package org.qualsh.lb.digital;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * WsjtxUdpListener – receive WSJT-X UDP broadcast messages and fire callbacks
 * so the application can auto-log completed QSOs.
 *
 * <h2>Background</h2>
 * <p>WSJT-X (and by extension FT8, FT4, JS8Call) broadcasts real-time status
 * over a UDP multicast/unicast stream on port 2237 (default).  The protocol is
 * described in the WSJT-X source file {@code Network/NetworkMessage.hpp}.
 *
 * <h2>Message types used here</h2>
 * <pre>
 *   Type 0  – Heartbeat    (id="WSJT-X", max-schema, version, revision)
 *   Type 1  – Status       (dial-freq, mode, DX-call, report, TX-mode, transmitting…)
 *   Type 5  – QSO-Logged   (date-off, time-off, DX-call, DX-grid, freq,
 *                            mode, rst-sent, rst-rcvd, tx-power, comments, name, …)
 *   Type 12 – ADIF-Logged  (raw ADIF string for the logged QSO)
 * </pre>
 *
 * <p>This listener decodes <b>Type 1 (Status)</b> for live display updates and
 * <b>Type 5 (QSO-Logged)</b> for auto-creating a log entry.
 *
 * <h2>Wire format</h2>
 * All multi-byte integers are big-endian.  Strings are length-prefixed:
 * {@code uint32 length} followed by {@code length} UTF-8 bytes (or 0xFFFFFFFF = null).
 * Datetimes are a {@code uint64} Julian day number + {@code uint32} ms-of-day.
 * Booleans are a single byte (0x00/0x01).
 *
 * <h2>Integration with WSJT-X / JS8Call</h2>
 * Configure the external app to send UDP to 127.0.0.1:2237 (default).
 * JS8Call uses port 2242 by default; override via {@link #DEFAULT_PORT}.
 */
public class WsjtxUdpListener {

    // WSJT-X magic number and default port
    private static final int MAGIC  = 0xADBCCBDA;
    public  static final int DEFAULT_PORT = 2237;
    // JS8Call uses 2242; user can configure this
    public  static final int JS8CALL_PORT = 2242;

    // Message type constants
    private static final int MSG_HEARTBEAT = 0;
    private static final int MSG_STATUS    = 1;
    private static final int MSG_QSO_LOGGED = 5;

    // ── Singleton ─────────────────────────────────────────────────────────────

    private static WsjtxUdpListener instance;

    public static synchronized WsjtxUdpListener getInstance() {
        if (instance == null) instance = new WsjtxUdpListener();
        return instance;
    }

    private WsjtxUdpListener() {}

    // ── State ─────────────────────────────────────────────────────────────────

    private DatagramSocket socket;
    private Thread listenerThread;
    private final AtomicBoolean running = new AtomicBoolean(false);

    private final List<Consumer<StatusMessage>>    statusListeners    = new ArrayList<>();
    private final List<Consumer<QsoLoggedMessage>> qsoLoggedListeners = new ArrayList<>();

    // ── Start / stop ──────────────────────────────────────────────────────────

    /**
     * Start listening on the given UDP port.
     *
     * @param port UDP port (use {@link #DEFAULT_PORT} for WSJT-X, {@link #JS8CALL_PORT} for JS8Call)
     * @return {@code true} if the socket was bound successfully
     */
    public synchronized boolean start(int port) {
        stop();
        try {
            socket = new DatagramSocket(port);
            socket.setSoTimeout(500); // allow periodic shutdown check
            running.set(true);

            listenerThread = new Thread(() -> {
                byte[] buf = new byte[4096];
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                while (running.get()) {
                    try {
                        socket.receive(packet);
                        handlePacket(packet.getData(), packet.getLength());
                    } catch (java.net.SocketTimeoutException ignored) {
                        // normal; loop to check running flag
                    } catch (Exception e) {
                        if (running.get()) {
                            System.err.println("WsjtxUdpListener: " + e.getMessage());
                        }
                    }
                }
            }, "WsjtxUdpListener");
            listenerThread.setDaemon(true);
            listenerThread.start();
            System.out.println("WsjtxUdpListener: listening on UDP port " + port);
            return true;
        } catch (Exception e) {
            System.err.println("WsjtxUdpListener: cannot bind port " + port + " – " + e.getMessage());
            return false;
        }
    }

    /** Stop the listener and close the socket. */
    public synchronized void stop() {
        running.set(false);
        if (socket != null) { socket.close(); socket = null; }
        if (listenerThread != null) {
            try { listenerThread.join(1000); } catch (InterruptedException ignored) {}
            listenerThread = null;
        }
    }

    public boolean isRunning() { return running.get(); }

    // ── Listener registration ─────────────────────────────────────────────────

    /** Receive live status updates (frequency, mode, transmitting flag). */
    public void addStatusListener(Consumer<StatusMessage> l) {
        synchronized (statusListeners) { statusListeners.add(l); }
    }

    public void removeStatusListener(Consumer<StatusMessage> l) {
        synchronized (statusListeners) { statusListeners.remove(l); }
    }

    /**
     * Receive a callback each time WSJT-X logs a QSO (Type 5 message).
     *
     * <p>This is the hook for <b>auto-log creation</b>: when a QSO is logged
     * in WSJT-X, the application receives the full QSO data and can create
     * a {@link org.qualsh.lb.log.Log} entry automatically.
     */
    public void addQsoLoggedListener(Consumer<QsoLoggedMessage> l) {
        synchronized (qsoLoggedListeners) { qsoLoggedListeners.add(l); }
    }

    public void removeQsoLoggedListener(Consumer<QsoLoggedMessage> l) {
        synchronized (qsoLoggedListeners) { qsoLoggedListeners.remove(l); }
    }

    // ── Packet decoding ───────────────────────────────────────────────────────

    private void handlePacket(byte[] data, int length) {
        ByteBuffer buf = ByteBuffer.wrap(data, 0, length).order(ByteOrder.BIG_ENDIAN);
        if (buf.remaining() < 8) return;

        int magic = buf.getInt();
        if (magic != MAGIC) return; // not a WSJT-X packet

        int schemaVersion = buf.getInt();
        int messageType   = buf.getInt();

        switch (messageType) {
            case MSG_STATUS     -> handleStatus(buf);
            case MSG_QSO_LOGGED -> handleQsoLogged(buf);
            // MSG_HEARTBEAT and others are silently ignored
        }
    }

    private void handleStatus(ByteBuffer buf) {
        try {
            String id        = readUtf8(buf);
            long   dialFreqHz = buf.getLong();  // Hz
            String mode      = readUtf8(buf);
            String dxCall    = readUtf8(buf);
            String report    = readUtf8(buf);
            String txMode    = readUtf8(buf);
            boolean txEnabled  = buf.get() != 0;
            boolean transmitting = buf.get() != 0;

            StatusMessage msg = new StatusMessage(id, dialFreqHz, mode, dxCall,
                    report, txMode, txEnabled, transmitting);

            List<Consumer<StatusMessage>> snapshot;
            synchronized (statusListeners) { snapshot = new ArrayList<>(statusListeners); }
            for (Consumer<StatusMessage> l : snapshot) {
                try { l.accept(msg); } catch (Exception ignored) {}
            }
        } catch (Exception e) {
            System.err.println("WsjtxUdpListener: status decode error – " + e.getMessage());
        }
    }

    private void handleQsoLogged(ByteBuffer buf) {
        try {
            String id          = readUtf8(buf);
            long   dateOffJD   = buf.getLong();  // Julian day
            int    timeOffMs   = buf.getInt();   // ms of day
            String dxCall      = readUtf8(buf);
            String dxGrid      = readUtf8(buf);
            long   dialFreqHz  = buf.getLong();
            String mode        = readUtf8(buf);
            String rstSent     = readUtf8(buf);
            String rstRcvd     = readUtf8(buf);
            String txPower     = readUtf8(buf);
            String comments    = readUtf8(buf);
            String name        = readUtf8(buf);
            long   dateOnJD    = buf.getLong();
            int    timeOnMs    = buf.getInt();
            String operatorCall = buf.hasRemaining() ? readUtf8(buf) : "";
            String myCall       = buf.hasRemaining() ? readUtf8(buf) : "";
            String myGrid       = buf.hasRemaining() ? readUtf8(buf) : "";
            String exchangeSent = buf.hasRemaining() ? readUtf8(buf) : "";
            String exchangeRcvd = buf.hasRemaining() ? readUtf8(buf) : "";
            String propMode     = buf.hasRemaining() ? readUtf8(buf) : "";

            QsoLoggedMessage msg = new QsoLoggedMessage(
                    id, julianToLocalDate(dateOnJD), timeOnMs / 1000,
                    dxCall, dxGrid, dialFreqHz / 1000.0, mode,
                    rstSent, rstRcvd, txPower, comments, name,
                    myCall, myGrid, propMode
            );

            List<Consumer<QsoLoggedMessage>> snapshot;
            synchronized (qsoLoggedListeners) { snapshot = new ArrayList<>(qsoLoggedListeners); }
            for (Consumer<QsoLoggedMessage> l : snapshot) {
                try { l.accept(msg); } catch (Exception ignored) {}
            }
        } catch (Exception e) {
            System.err.println("WsjtxUdpListener: QSO-logged decode error – " + e.getMessage());
        }
    }

    // ── Wire-format helpers ───────────────────────────────────────────────────

    /**
     * Read a WSJT-X length-prefixed UTF-8 string.
     * A length of 0xFFFFFFFF means null/empty.
     */
    private static String readUtf8(ByteBuffer buf) {
        int len = buf.getInt();
        if (len == 0xFFFFFFFF || len == 0 || len < 0) return "";
        byte[] bytes = new byte[len];
        buf.get(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    /**
     * Convert a Julian Day Number to a {@link java.time.LocalDate}.
     * Julian day 2299161 = 1582-10-15 (Gregorian reform).
     */
    private static java.time.LocalDate julianToLocalDate(long jdn) {
        // Algorithm from https://en.wikipedia.org/wiki/Julian_day#Julian_day_number_calculation
        long l = jdn + 68569;
        long n = (4 * l) / 146097;
        l = l - (146097 * n + 3) / 4;
        long i = (4000 * (l + 1)) / 1461001;
        l = l - (1461 * i) / 4 + 31;
        long j = (80 * l) / 2447;
        int day   = (int)(l - (2447 * j) / 80);
        l = j / 11;
        int month = (int)(j + 2 - 12 * l);
        int year  = (int)(100 * (n - 49) + i + l);
        return java.time.LocalDate.of(year, month, day);
    }

    // ── Data transfer objects ─────────────────────────────────────────────────

    /**
     * Live status snapshot from WSJT-X (Message Type 1).
     */
    public record StatusMessage(
            String  id,
            long    dialFreqHz,
            String  mode,
            String  dxCall,
            String  report,
            String  txMode,
            boolean txEnabled,
            boolean transmitting
    ) {
        public double dialFreqKhz() { return dialFreqHz / 1000.0; }
    }

    /**
     * Completed QSO data from WSJT-X (Message Type 5).
     *
     * <p>This is handed directly to the auto-log callback in
     * {@link org.qualsh.lb.view.DigitalModesWindow}.
     */
    public record QsoLoggedMessage(
            String              instanceId,
            java.time.LocalDate dateOn,
            int                 timeOnSecOfDay,
            String              dxCall,
            String              dxGrid,
            double              dialFreqKhz,
            String              mode,
            String              rstSent,
            String              rstRcvd,
            String              txPower,
            String              comments,
            String              name,
            String              myCall,
            String              myGrid,
            String              propMode
    ) {}
}
