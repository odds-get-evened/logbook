package org.qualsh.lb.dx;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Client for Telnet-based DX Clusters.
 *
 * <p>Connects to a DX cluster node over TCP (Telnet), logs in with a callsign,
 * and parses incoming DX spot lines in the standard format:
 * <pre>DX de &lt;spotter&gt;:   &lt;freq&gt;  &lt;callsign&gt;  &lt;comment&gt;  &lt;time&gt;Z</pre>
 *
 * <p>Registered {@link Consumer}&lt;{@link DXSpot}&gt; listeners are notified
 * on the reader thread whenever a new spot arrives; update the UI with
 * {@code SwingUtilities.invokeLater}.
 */
public class DXClusterClient {

    // Standard DX spot line pattern:
    // DX de KC9AAA:    14025.0  W1AW         599 cw                          0501Z
    private static final Pattern DX_SPOT_PATTERN = Pattern.compile(
        "DX de\\s+(\\S+?):\\s+([\\d.]+)\\s+(\\S+)\\s*(.*?)\\s+(\\d{4})Z\\s*$"
    );

    private volatile boolean connected = false;

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private Thread readerThread;

    private final List<Consumer<DXSpot>>   spotListeners   = new CopyOnWriteArrayList<>();
    private final List<Consumer<Boolean>>  statusListeners = new CopyOnWriteArrayList<>();
    private final List<Consumer<String>>   rawListeners    = new CopyOnWriteArrayList<>();

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Connect to the cluster and start the background reader.
     *
     * @param host     cluster hostname or IP
     * @param port     TCP port (typically 7300 or 23)
     * @param callsign callsign sent as the login credential
     * @return {@code true} if the socket connected successfully
     */
    public boolean connect(String host, int port, String callsign) {
        if (connected) disconnect();
        try {
            socket = new Socket(host, port);
            socket.setSoTimeout(0); // blocking reads
            out = new PrintWriter(socket.getOutputStream(), true);
            in  = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            connected = true;
            notifyStatusListeners(true);

            final String cs = (callsign != null) ? callsign.trim() : "";
            readerThread = new Thread(() -> {
                try {
                    // Give the cluster a moment to send its login banner
                    Thread.sleep(600);
                    if (!cs.isEmpty()) {
                        out.println(cs);
                    }
                    String line;
                    while (connected && (line = in.readLine()) != null) {
                        notifyRawListeners(line);
                        DXSpot spot = parseLine(line);
                        if (spot != null) {
                            notifySpotListeners(spot);
                        }
                    }
                } catch (InterruptedException ignored) {
                    // shutdown requested
                } catch (Exception e) {
                    if (connected) {
                        System.err.println("DXClusterClient: reader error – " + e.getMessage());
                    }
                } finally {
                    if (connected) {
                        disconnect();
                    }
                }
            }, "DXCluster-reader");
            readerThread.setDaemon(true);
            readerThread.start();

            return true;
        } catch (IOException e) {
            System.err.println("DXClusterClient: connect failed – " + e.getMessage());
            return false;
        }
    }

    /** Disconnect from the cluster and stop the reader thread. */
    public void disconnect() {
        connected = false;
        try { if (readerThread != null) readerThread.interrupt(); } catch (Exception ignored) {}
        try { if (in  != null) in.close();  } catch (Exception ignored) {}
        try { if (out != null) out.close(); } catch (Exception ignored) {}
        try { if (socket != null && !socket.isClosed()) socket.close(); } catch (Exception ignored) {}
        socket = null; out = null; in = null; readerThread = null;
        notifyStatusListeners(false);
    }

    public boolean isConnected() { return connected; }

    // ── Listener registration ─────────────────────────────────────────────────

    /** Called with each parsed {@link DXSpot} on the reader thread. */
    public void addSpotListener(Consumer<DXSpot> l)   { spotListeners.add(l); }
    public void removeSpotListener(Consumer<DXSpot> l) { spotListeners.remove(l); }

    /** Called with {@code true} on connect and {@code false} on disconnect. */
    public void addStatusListener(Consumer<Boolean> l)   { statusListeners.add(l); }
    public void removeStatusListener(Consumer<Boolean> l) { statusListeners.remove(l); }

    /** Called with every raw line received from the cluster. */
    public void addRawListener(Consumer<String> l)   { rawListeners.add(l); }
    public void removeRawListener(Consumer<String> l) { rawListeners.remove(l); }

    // ── Spot parsing ──────────────────────────────────────────────────────────

    DXSpot parseLine(String line) {
        if (line == null) return null;
        Matcher m = DX_SPOT_PATTERN.matcher(line);
        if (!m.find()) return null;
        try {
            String spotter  = m.group(1);
            double freq     = Double.parseDouble(m.group(2));
            String callsign = m.group(3);
            String comment  = m.group(4).trim();
            String time     = m.group(5) + "Z";
            return new DXSpot(time, freq, callsign, spotter, comment);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // ── Notification helpers ──────────────────────────────────────────────────

    private void notifySpotListeners(DXSpot spot) {
        for (Consumer<DXSpot> l : spotListeners) {
            try { l.accept(spot); } catch (Exception ignored) {}
        }
    }

    private void notifyStatusListeners(boolean conn) {
        for (Consumer<Boolean> l : statusListeners) {
            try { l.accept(conn); } catch (Exception ignored) {}
        }
    }

    private void notifyRawListeners(String line) {
        for (Consumer<String> l : rawListeners) {
            try { l.accept(line); } catch (Exception ignored) {}
        }
    }
}
