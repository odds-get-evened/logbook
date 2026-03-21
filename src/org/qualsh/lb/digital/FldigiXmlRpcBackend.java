package org.qualsh.lb.digital;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Fldigi XML-RPC backend — implements {@link DigitalModeBackend} by polling
 * Fldigi's built-in XML-RPC API server (default port 7362).
 *
 * <h2>How it works</h2>
 * <p>Fldigi exposes an XML-RPC server at {@code http://host:port/RPC2}.
 * This backend polls it at 1 Hz to:
 * <ul>
 *   <li>Read dial frequency ({@code rig.get_frequency}) and modem name
 *       ({@code modem.get_name}) → fire {@link StatusEvent}</li>
 *   <li>Read newly received text ({@code rx.get_data}) → fire {@link DecodedLine}
 *       for each non-empty line</li>
 *   <li>Detect a logged QSO via {@code log.get_call} changing from non-empty
 *       to empty after a {@code log.submit} → fire {@link AutoLogEvent}</li>
 * </ul>
 *
 * <h2>Integration with Fldigi</h2>
 * Enable Fldigi's XML-RPC server under Configure → Misc → XML-RPC.
 * Default: {@code localhost:7362}.
 */
public class FldigiXmlRpcBackend implements DigitalModeBackend {

    public static final int DEFAULT_PORT = 7362;
    public static final String DEFAULT_HOST = "localhost";

    // ── Singleton ─────────────────────────────────────────────────────────────

    private static FldigiXmlRpcBackend instance;

    public static synchronized FldigiXmlRpcBackend getInstance() {
        if (instance == null) instance = new FldigiXmlRpcBackend();
        return instance;
    }

    private FldigiXmlRpcBackend() {}

    // ── State ─────────────────────────────────────────────────────────────────

    private String rpcUrl;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private ScheduledExecutorService poller;

    /** Last call sign seen in the Fldigi log; used to detect a new QSO being submitted. */
    private volatile String lastLogCall = "";
    /** Last modem name seen; used to build StatusEvent.mode. */
    private volatile String lastModem = "";

    private final List<Consumer<StatusEvent>>  statusListeners      = new ArrayList<>();
    private final List<Consumer<AutoLogEvent>> qsoLoggedListeners   = new ArrayList<>();
    private final List<Consumer<DecodedLine>>  decodedLineListeners = new ArrayList<>();

    // ── DigitalModeBackend ────────────────────────────────────────────────────

    @Override
    public String name() { return "Fldigi"; }

    /**
     * Start polling Fldigi XML-RPC at {@code host:port}.
     * The {@code port} parameter is the TCP port; the host is set separately
     * via {@link #setHost(String)} (defaults to {@code localhost}).
     */
    @Override
    public synchronized boolean start(int port) {
        stop();
        rpcUrl = "http://" + host + ":" + port + "/RPC2";
        running.set(true);
        lastLogCall = "";
        lastModem   = "";

        poller = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "FldigiXmlRpcBackend-poller");
            t.setDaemon(true);
            return t;
        });
        poller.scheduleAtFixedRate(this::poll, 0, 1, TimeUnit.SECONDS);
        System.out.println("FldigiXmlRpcBackend: polling " + rpcUrl);
        return true;
    }

    @Override
    public synchronized void stop() {
        running.set(false);
        if (poller != null) {
            poller.shutdownNow();
            poller = null;
        }
    }

    @Override
    public boolean isRunning() { return running.get(); }

    // ── Host configuration ────────────────────────────────────────────────────

    private String host = DEFAULT_HOST;

    public void setHost(String host) { this.host = (host != null && !host.isBlank()) ? host : DEFAULT_HOST; }
    public String getHost() { return host; }

    // ── Listener registration ─────────────────────────────────────────────────

    @Override public void addStatusListener(Consumer<StatusEvent> l)      { synchronized (statusListeners)      { statusListeners.add(l); } }
    @Override public void removeStatusListener(Consumer<StatusEvent> l)   { synchronized (statusListeners)      { statusListeners.remove(l); } }
    @Override public void addQsoLoggedListener(Consumer<AutoLogEvent> l)  { synchronized (qsoLoggedListeners)   { qsoLoggedListeners.add(l); } }
    @Override public void removeQsoLoggedListener(Consumer<AutoLogEvent> l){ synchronized (qsoLoggedListeners)  { qsoLoggedListeners.remove(l); } }
    @Override public void addDecodedLineListener(Consumer<DecodedLine> l) { synchronized (decodedLineListeners) { decodedLineListeners.add(l); } }
    @Override public void removeDecodedLineListener(Consumer<DecodedLine> l){ synchronized (decodedLineListeners){ decodedLineListeners.remove(l); } }

    // ── Polling ───────────────────────────────────────────────────────────────

    private void poll() {
        if (!running.get()) return;
        try {
            pollStatus();
            pollRxData();
            pollLogState();
        } catch (Exception e) {
            // Fldigi not running or unreachable — silent failure is expected
        }
    }

    private void pollStatus() throws Exception {
        double freqHz = callDouble("rig.get_frequency");
        String modem  = callString("modem.get_name");
        lastModem = modem != null ? modem : lastModem;

        StatusEvent evt = new StatusEvent(freqHz, lastModem, false);
        List<Consumer<StatusEvent>> snapshot;
        synchronized (statusListeners) { snapshot = new ArrayList<>(statusListeners); }
        for (Consumer<StatusEvent> l : snapshot) {
            try { l.accept(evt); } catch (Exception ignored) {}
        }
    }

    private void pollRxData() throws Exception {
        String raw = callString("rx.get_data");
        if (raw == null || raw.isBlank()) return;

        // Emit each non-empty line as a DecodedLine
        LocalTime now = LocalTime.now();
        List<Consumer<DecodedLine>> snapshot;
        synchronized (decodedLineListeners) { snapshot = new ArrayList<>(decodedLineListeners); }
        if (snapshot.isEmpty()) return;

        for (String line : raw.split("\n")) {
            line = line.trim();
            if (line.isEmpty()) continue;
            DecodedLine dl = new DecodedLine(now, null, null, null, null, null, line);
            for (Consumer<DecodedLine> l : snapshot) {
                try { l.accept(dl); } catch (Exception ignored) {}
            }
        }
    }

    private void pollLogState() throws Exception {
        String call = callString("log.get_call");
        if (call == null) call = "";

        // Fldigi clears log.get_call after a QSO is submitted; detect the transition
        // non-empty → empty as a "QSO was just logged" signal
        if (!lastLogCall.isEmpty() && call.isEmpty()) {
            fireQsoLogged();
        }
        lastLogCall = call;
    }

    private void fireQsoLogged() {
        try {
            String dxCall  = callString("log.get_call");
            String dxGrid  = callString("log.get_qth");
            double freqHz  = callDouble("rig.get_frequency");
            String rstSent = callString("log.get_rst_out");
            String rstRcvd = callString("log.get_rst_in");
            String name    = callString("log.get_name");
            String notes   = callString("log.get_notes");

            AutoLogEvent evt = new AutoLogEvent(
                    LocalDate.now(), localTimeSec(),
                    dxCall != null ? dxCall : "",
                    dxGrid != null ? dxGrid : "",
                    freqHz / 1000.0,
                    lastModem,
                    rstSent != null ? rstSent : "",
                    rstRcvd != null ? rstRcvd : "",
                    "", // txPower not available via XML-RPC
                    notes != null ? notes : "",
                    name != null ? name : "",
                    "", "", "", // myCall, myGrid, propMode
                    name()
            );

            List<Consumer<AutoLogEvent>> snapshot;
            synchronized (qsoLoggedListeners) { snapshot = new ArrayList<>(qsoLoggedListeners); }
            for (Consumer<AutoLogEvent> l : snapshot) {
                try { l.accept(evt); } catch (Exception ignored) {}
            }
        } catch (Exception e) {
            System.err.println("FldigiXmlRpcBackend: QSO log fire error – " + e.getMessage());
        }
    }

    // ── XML-RPC helpers ───────────────────────────────────────────────────────

    private static final int TIMEOUT_MS = 800;

    /** Call an XML-RPC method that returns a string value. Returns null on failure. */
    private String callString(String method) {
        try {
            String response = rpcCall(method);
            return extractString(response);
        } catch (Exception e) {
            return null;
        }
    }

    /** Call an XML-RPC method that returns a double value. Returns 0.0 on failure. */
    private double callDouble(String method) {
        try {
            String response = rpcCall(method);
            String val = extractString(response);
            if (val == null || val.isBlank()) return 0.0;
            return Double.parseDouble(val.trim());
        } catch (Exception e) {
            return 0.0;
        }
    }

    /** POST an XML-RPC call and return the raw response body. */
    private String rpcCall(String method) throws Exception {
        String body = "<?xml version=\"1.0\"?><methodCall><methodName>"
                + method + "</methodName><params/></methodCall>";
        byte[] bodyBytes = body.getBytes(StandardCharsets.UTF_8);

        URL url = new URL(rpcUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "text/xml; charset=UTF-8");
        conn.setRequestProperty("Content-Length", String.valueOf(bodyBytes.length));
        conn.setDoOutput(true);
        conn.setConnectTimeout(TIMEOUT_MS);
        conn.setReadTimeout(TIMEOUT_MS);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(bodyBytes);
        }

        byte[] resp = conn.getInputStream().readAllBytes();
        return new String(resp, StandardCharsets.UTF_8);
    }

    /**
     * Extract the first value string from an XML-RPC response.
     * Handles {@code <string>}, {@code <double>}, {@code <int>}, and {@code <i4>} elements.
     */
    private static String extractString(String xml) throws Exception {
        if (xml == null || xml.isBlank()) return null;
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc = db.parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));

        for (String tag : new String[]{"string", "double", "int", "i4"}) {
            NodeList nodes = doc.getElementsByTagName(tag);
            if (nodes.getLength() > 0) {
                return nodes.item(0).getTextContent();
            }
        }
        return null;
    }

    private static int localTimeSec() {
        LocalTime t = LocalTime.now();
        return t.toSecondOfDay();
    }
}
