package org.qualsh.lb.rig;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import com.fazecast.jSerialComm.SerialPort;

import org.qualsh.lb.util.Preferences;

/**
 * RigController – CAT (Computer-Aided Transceiver) integration.
 *
 * <p>Supports two connection modes:
 * <ul>
 *   <li><b>RIGCTLD</b> – connects to a running hamlib {@code rigctld} daemon over TCP
 *       (default: localhost:4532). This is the recommended path because it supports
 *       200+ radio models without any radio-specific code here.</li>
 *   <li><b>SERIAL</b> – opens a serial/USB port directly and speaks a simple CAT
 *       dialect (Yaesu CAT II/III, Kenwood CAT, or Icom CI-V).</li>
 * </ul>
 *
 * <p>Once connected a background thread polls the radio every 2 seconds and
 * notifies registered {@link Consumer}&lt;Double&gt; listeners with the VFO-A
 * frequency in <em>kHz</em>.
 */
public class RigController {

    // ── Connection-type enum ──────────────────────────────────────────────────

    public enum ConnectionType {
        RIGCTLD("rigctld (hamlib network daemon)"),
        SERIAL("Direct serial / USB CAT");

        private final String label;
        ConnectionType(String label) { this.label = label; }
        public String getLabel() { return label; }
        @Override public String toString() { return label; }
    }

    // ── Serial protocol enum ──────────────────────────────────────────────────

    public enum SerialProtocol {
        YAESU("Yaesu CAT II/III"),
        KENWOOD("Kenwood CAT"),
        ICOM("Icom CI-V");

        private final String label;
        SerialProtocol(String label) { this.label = label; }
        public String getLabel() { return label; }
        @Override public String toString() { return label; }
    }

    // ── Singleton ─────────────────────────────────────────────────────────────

    private static RigController instance;

    public static synchronized RigController getInstance() {
        if (instance == null) {
            instance = new RigController();
        }
        return instance;
    }

    // ── State ─────────────────────────────────────────────────────────────────

    private volatile boolean connected = false;

    // rigctld
    private Socket rigctldSocket;
    private PrintWriter rigctldOut;
    private BufferedReader rigctldIn;

    // serial
    private SerialPort serialPort;

    // polling
    private ScheduledExecutorService scheduler;
    private ScheduledFuture<?> pollFuture;

    // listeners
    private final List<Consumer<Double>> freqListeners = new CopyOnWriteArrayList<>();
    private final List<Consumer<Boolean>> statusListeners = new CopyOnWriteArrayList<>();

    private RigController() {}

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Connect using the settings stored in {@link Preferences}.
     *
     * @return {@code true} on success
     */
    public synchronized boolean connect() {
        if (connected) disconnect();

        String typeStr = Preferences.getOne(Preferences.PREF_CAT_TYPE);
        ConnectionType type = (typeStr != null && typeStr.equals("SERIAL"))
                ? ConnectionType.SERIAL : ConnectionType.RIGCTLD;

        try {
            if (type == ConnectionType.RIGCTLD) {
                return connectRigctld();
            } else {
                return connectSerial();
            }
        } catch (Exception e) {
            System.err.println("RigController: connect failed – " + e.getMessage());
            return false;
        }
    }

    /**
     * Connect to rigctld with explicit host and port (used by the settings dialog
     * "Test" button without saving prefs first).
     */
    public synchronized boolean connectRigctld(String host, int port) {
        if (connected) disconnect();
        try {
            rigctldSocket = new Socket(host, port);
            rigctldSocket.setSoTimeout(3000);
            rigctldOut = new PrintWriter(rigctldSocket.getOutputStream(), true);
            rigctldIn  = new BufferedReader(new InputStreamReader(rigctldSocket.getInputStream()));
            connected = true;
            notifyStatusListeners(true);
            startPolling();
            return true;
        } catch (IOException e) {
            System.err.println("RigController: rigctld connect failed – " + e.getMessage());
            return false;
        }
    }

    /**
     * Connect to a serial port with explicit parameters (used by the settings
     * dialog "Test" button).
     */
    public synchronized boolean connectSerial(String portName, int baudRate, SerialProtocol protocol) {
        if (connected) disconnect();
        SerialPort port = SerialPort.getCommPort(portName);
        port.setBaudRate(baudRate);
        port.setNumDataBits(8);
        port.setNumStopBits(SerialPort.ONE_STOP_BIT);
        port.setParity(SerialPort.NO_PARITY);
        port.setComPortTimeouts(SerialPort.TIMEOUT_READ_BLOCKING, 3000, 0);
        if (!port.openPort()) {
            System.err.println("RigController: could not open serial port " + portName);
            return false;
        }
        serialPort = port;
        connected = true;
        notifyStatusListeners(true);
        startPolling();
        return true;
    }

    /** Disconnect and stop polling. */
    public synchronized void disconnect() {
        stopPolling();
        closeRigctld();
        closeSerial();
        connected = false;
        notifyStatusListeners(false);
    }

    public boolean isConnected() {
        return connected;
    }

    /**
     * Query the radio once and return the VFO-A frequency in kHz,
     * or {@code -1} if the query fails.
     */
    public double pollFrequency() {
        try {
            if (rigctldSocket != null && !rigctldSocket.isClosed()) {
                return pollRigctldFrequency();
            } else if (serialPort != null && serialPort.isOpen()) {
                String typeStr = Preferences.getOne(Preferences.PREF_CAT_TYPE);
                String protoStr = Preferences.getOne(Preferences.PREF_CAT_SERIAL_PROTOCOL);
                SerialProtocol proto = parseProtocol(protoStr);
                return pollSerialFrequency(proto);
            }
        } catch (Exception e) {
            System.err.println("RigController: poll failed – " + e.getMessage());
            disconnect();
        }
        return -1;
    }

    // ── Listener registration ─────────────────────────────────────────────────

    /** Register a listener that receives VFO-A frequency in kHz on every poll. */
    public void addFrequencyListener(Consumer<Double> listener) {
        freqListeners.add(listener);
    }

    public void removeFrequencyListener(Consumer<Double> listener) {
        freqListeners.remove(listener);
    }

    /** Register a listener that receives {@code true} on connect, {@code false} on disconnect. */
    public void addStatusListener(Consumer<Boolean> listener) {
        statusListeners.add(listener);
    }

    public void removeStatusListener(Consumer<Boolean> listener) {
        statusListeners.remove(listener);
    }

    // ── rigctld helpers ───────────────────────────────────────────────────────

    private boolean connectRigctld() {
        String host = Preferences.getOne(Preferences.PREF_CAT_RIGCTLD_HOST);
        String portStr = Preferences.getOne(Preferences.PREF_CAT_RIGCTLD_PORT);
        if (host == null || host.isEmpty()) host = "localhost";
        int port = 4532;
        if (portStr != null && !portStr.isEmpty()) {
            try { port = Integer.parseInt(portStr); } catch (NumberFormatException ignored) {}
        }
        return connectRigctld(host, port);
    }

    private double pollRigctldFrequency() throws IOException {
        // rigctld "get_freq" command: send "f\n", response is frequency in Hz
        rigctldOut.println("f");
        String line = rigctldIn.readLine();
        if (line == null) throw new IOException("rigctld returned null");
        line = line.trim();
        // Some rigctld versions reply "RPRT -11" on error
        if (line.startsWith("RPRT")) throw new IOException("rigctld error: " + line);
        double hz = Double.parseDouble(line);
        return hz / 1000.0; // Hz → kHz
    }

    private void closeRigctld() {
        try { if (rigctldIn  != null) rigctldIn.close();  } catch (IOException ignored) {}
        try { if (rigctldOut != null) rigctldOut.close(); } catch (Exception ignored) {}
        try { if (rigctldSocket != null && !rigctldSocket.isClosed()) rigctldSocket.close(); }
        catch (IOException ignored) {}
        rigctldSocket = null;
        rigctldOut = null;
        rigctldIn  = null;
    }

    // ── Serial CAT helpers ────────────────────────────────────────────────────

    private boolean connectSerial() {
        String portName = Preferences.getOne(Preferences.PREF_CAT_SERIAL_PORT);
        String baudStr  = Preferences.getOne(Preferences.PREF_CAT_SERIAL_BAUD);
        String protoStr = Preferences.getOne(Preferences.PREF_CAT_SERIAL_PROTOCOL);
        if (portName == null || portName.isEmpty()) {
            System.err.println("RigController: no serial port configured");
            return false;
        }
        int baud = 9600;
        if (baudStr != null && !baudStr.isEmpty()) {
            try { baud = Integer.parseInt(baudStr); } catch (NumberFormatException ignored) {}
        }
        SerialProtocol proto = parseProtocol(protoStr);
        return connectSerial(portName, baud, proto);
    }

    private double pollSerialFrequency(SerialProtocol proto) throws IOException {
        switch (proto) {
            case YAESU:   return pollYaesu();
            case KENWOOD: return pollKenwood();
            case ICOM:    return pollIcom();
            default:      return pollYaesu();
        }
    }

    /**
     * Yaesu CAT II/III: send {@code FA;}, response {@code FA00014225000;} (11-digit Hz).
     */
    private double pollYaesu() throws IOException {
        flushSerialInput();
        serialPort.getOutputStream().write("FA;".getBytes());
        serialPort.getOutputStream().flush();
        // Read until ';'
        StringBuilder sb = new StringBuilder();
        byte[] buf = new byte[1];
        long deadline = System.currentTimeMillis() + 2000;
        while (System.currentTimeMillis() < deadline) {
            if (serialPort.bytesAvailable() > 0) {
                serialPort.readBytes(buf, 1);
                sb.append((char) buf[0]);
                if (buf[0] == ';') break;
            }
        }
        String resp = sb.toString().trim();
        // Expected: "FA" + 11-digit frequency in Hz + ";"
        if (resp.startsWith("FA") && resp.endsWith(";") && resp.length() >= 13) {
            long hz = Long.parseLong(resp.substring(2, resp.length() - 1));
            return hz / 1000.0;
        }
        throw new IOException("Unexpected Yaesu response: " + resp);
    }

    /**
     * Kenwood CAT: send {@code IF;}, parse 11-digit frequency from the response.
     * Response format: {@code IF} + 11-digit-freq + remaining fields + {@code ;}.
     */
    private double pollKenwood() throws IOException {
        flushSerialInput();
        serialPort.getOutputStream().write("IF;".getBytes());
        serialPort.getOutputStream().flush();
        StringBuilder sb = new StringBuilder();
        byte[] buf = new byte[1];
        long deadline = System.currentTimeMillis() + 2000;
        while (System.currentTimeMillis() < deadline) {
            if (serialPort.bytesAvailable() > 0) {
                serialPort.readBytes(buf, 1);
                sb.append((char) buf[0]);
                if (buf[0] == ';') break;
            }
        }
        String resp = sb.toString().trim();
        if (resp.startsWith("IF") && resp.length() >= 13) {
            long hz = Long.parseLong(resp.substring(2, 13));
            return hz / 1000.0;
        }
        throw new IOException("Unexpected Kenwood response: " + resp);
    }

    /**
     * Icom CI-V: send read-frequency command {@code FE FE [addr] E0 03 FD},
     * parse binary response.
     */
    private double pollIcom() throws IOException {
        String addrStr = Preferences.getOne(Preferences.PREF_CAT_ICOM_ADDRESS);
        int addr = 0xA4; // common default (IC-7300)
        if (addrStr != null && !addrStr.isEmpty()) {
            try { addr = Integer.parseInt(addrStr, 16); } catch (NumberFormatException ignored) {}
        }
        flushSerialInput();
        byte[] cmd = { (byte)0xFE, (byte)0xFE, (byte)addr, (byte)0xE0, 0x03, (byte)0xFD };
        serialPort.getOutputStream().write(cmd);
        serialPort.getOutputStream().flush();

        // Read until 0xFD terminator
        byte[] buf = new byte[1];
        java.util.List<Byte> resp = new java.util.ArrayList<>();
        long deadline = System.currentTimeMillis() + 2000;
        boolean inFrame = false;
        int preambleCount = 0;
        while (System.currentTimeMillis() < deadline) {
            if (serialPort.bytesAvailable() > 0) {
                serialPort.readBytes(buf, 1);
                byte b = buf[0];
                if (!inFrame && b == (byte)0xFE) {
                    preambleCount++;
                    if (preambleCount >= 2) inFrame = true;
                } else if (inFrame) {
                    resp.add(b);
                    if (b == (byte)0xFD) break;
                }
            }
        }
        // Response after preamble: [to][from][03][b0][b1][b2][b3][b4][FD]
        // BCD-encoded frequency, 5 bytes, LSB first
        if (resp.size() >= 8 && resp.get(1) == (byte)addr && resp.get(2) == 0x03) {
            long hz = decodeCivBcd(resp.subList(3, 8));
            return hz / 1000.0;
        }
        throw new IOException("Unexpected Icom CI-V response length: " + resp.size());
    }

    /** Decode 5-byte BCD-packed CI-V frequency (LSB first) to Hz. */
    private long decodeCivBcd(java.util.List<Byte> bytes) {
        long freq = 0;
        long mul = 1;
        for (Byte b : bytes) {
            int lo = b & 0x0F;
            int hi = (b >> 4) & 0x0F;
            freq += lo * mul;  mul *= 10;
            freq += hi * mul;  mul *= 10;
        }
        return freq;
    }

    private void flushSerialInput() {
        byte[] buf = new byte[256];
        int available = serialPort.bytesAvailable();
        while (available > 0) {
            int toRead = Math.min(available, buf.length);
            serialPort.readBytes(buf, toRead);
            available = serialPort.bytesAvailable();
        }
    }

    private void closeSerial() {
        if (serialPort != null && serialPort.isOpen()) {
            serialPort.closePort();
        }
        serialPort = null;
    }

    // ── Polling ───────────────────────────────────────────────────────────────

    private void startPolling() {
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "RigController-poll");
            t.setDaemon(true);
            return t;
        });
        pollFuture = scheduler.scheduleAtFixedRate(() -> {
            double freqKhz = pollFrequency();
            if (freqKhz > 0) {
                notifyFreqListeners(freqKhz);
            }
        }, 0, 2, TimeUnit.SECONDS);
    }

    private void stopPolling() {
        if (pollFuture != null) { pollFuture.cancel(true); pollFuture = null; }
        if (scheduler != null) { scheduler.shutdownNow(); scheduler = null; }
    }

    // ── Notification helpers ──────────────────────────────────────────────────

    private void notifyFreqListeners(double freqKhz) {
        for (Consumer<Double> l : freqListeners) {
            try { l.accept(freqKhz); } catch (Exception ignored) {}
        }
    }

    private void notifyStatusListeners(boolean isConnected) {
        for (Consumer<Boolean> l : statusListeners) {
            try { l.accept(isConnected); } catch (Exception ignored) {}
        }
    }

    // ── Utility ───────────────────────────────────────────────────────────────

    private static SerialProtocol parseProtocol(String s) {
        if (s == null) return SerialProtocol.YAESU;
        try { return SerialProtocol.valueOf(s); } catch (IllegalArgumentException e) { return SerialProtocol.YAESU; }
    }

    /** Return available serial port names, or an empty array if none. */
    public static String[] availableSerialPorts() {
        SerialPort[] ports = SerialPort.getCommPorts();
        String[] names = new String[ports.length];
        for (int i = 0; i < ports.length; i++) {
            names[i] = ports[i].getSystemPortName();
        }
        return names;
    }
}
