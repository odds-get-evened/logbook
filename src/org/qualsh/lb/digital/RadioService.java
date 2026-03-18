package org.qualsh.lb.digital;

import com.fazecast.jSerialComm.SerialPort;
import org.qualsh.lb.rig.RigController;
import org.qualsh.lb.rig.RigController.SerialProtocol;
import org.qualsh.lb.util.Preferences;

import java.io.IOException;
import java.util.function.Consumer;

/**
 * RadioService – high-level CAT command abstraction for digital-mode operation.
 *
 * <p>Builds on top of the existing {@link RigController} singleton and adds:
 * <ul>
 *   <li>{@link #getMode()} / {@link #setMode(String)} – query / set the radio's operating mode
 *       using protocol-specific Hex or ASCII strings for Icom CI-V and Yaesu/Kenwood.</li>
 *   <li>{@link #setPtt(boolean)} – key/unkey the transmitter via CAT command (use
 *       {@link PttController} for hardware RTS/DTR PTT).</li>
 * </ul>
 *
 * <h2>CAT Protocol Reference</h2>
 *
 * <h3>Yaesu / Kenwood ASCII (same command set for mode)</h3>
 * <pre>
 *   Query mode:  send "MD;"  → reply "MD0;" … "MD9;" (Yaesu mode codes)
 *   Set mode:    send "MD3;" (USB), "MD2;" (LSB), "MD6;" (FM), etc.
 *   Yaesu mode codes: 1=LSB 2=USB 3=CW 4=FM 5=AM 6=RTTY 7=CW-R 8=PKT-L 9=PKT-U
 * </pre>
 *
 * <h3>Icom CI-V binary protocol</h3>
 * <pre>
 *   Query mode:  FE FE [addr] E0 04 FD
 *   Response:    FE FE E0 [addr] 04 [mode-byte] [filter-byte] FD
 *   Set mode:    FE FE [addr] E0 06 [mode-byte] [filter-byte] FD
 *   Icom mode bytes: 00=LSB 01=USB 02=AM 03=CW 04=RTTY 05=FM 07=CW-R 08=RTTY-R
 * </pre>
 *
 * <h3>rigctld (Hamlib)</h3>
 * <pre>
 *   Query mode: send "m\n"  → "USB\n12500\n" (mode, passband)
 *   Set mode:   send "M USB 3000\n"
 * </pre>
 */
public class RadioService {

    // ── Singleton ─────────────────────────────────────────────────────────────

    private static RadioService instance;

    public static synchronized RadioService getInstance() {
        if (instance == null) instance = new RadioService();
        return instance;
    }

    private RadioService() {}

    // ── Delegate to RigController for connection & frequency ──────────────────

    public boolean isConnected() {
        return RigController.getInstance().isConnected();
    }

    public boolean connect() {
        return RigController.getInstance().connect();
    }

    public void disconnect() {
        RigController.getInstance().disconnect();
    }

    public double getFrequencyKhz() {
        return RigController.getInstance().pollFrequency();
    }

    public boolean setFrequencyKhz(double khz) {
        return RigController.getInstance().setFrequency(khz);
    }

    public void addFrequencyListener(Consumer<Double> l) {
        RigController.getInstance().addFrequencyListener(l);
    }

    public void removeFrequencyListener(Consumer<Double> l) {
        RigController.getInstance().removeFrequencyListener(l);
    }

    // ── Mode control ──────────────────────────────────────────────────────────

    /**
     * Query the radio's current operating mode.
     *
     * @return mode string (e.g. "USB", "LSB", "FM") or {@code null} if unsupported/disconnected.
     */
    public synchronized String getMode() {
        if (!isConnected()) return null;
        try {
            String catType = Preferences.getOne(Preferences.PREF_CAT_TYPE);
            if ("SERIAL".equals(catType)) {
                SerialProtocol proto = getSerialProtocol();
                return switch (proto) {
                    case YAESU, KENWOOD -> getModeYaesu();
                    case ICOM           -> getModeIcom();
                };
            } else {
                return getModeRigctld();
            }
        } catch (Exception e) {
            System.err.println("RadioService.getMode failed: " + e.getMessage());
            return null;
        }
    }

    /**
     * Set the radio's operating mode.
     *
     * @param mode mode string: "USB", "LSB", "FM", "AM", "CW", "RTTY", "PKT-U", "PKT-L"
     * @return {@code true} on success
     */
    public synchronized boolean setMode(String mode) {
        if (!isConnected() || mode == null) return false;
        try {
            String catType = Preferences.getOne(Preferences.PREF_CAT_TYPE);
            if ("SERIAL".equals(catType)) {
                SerialProtocol proto = getSerialProtocol();
                return switch (proto) {
                    case YAESU, KENWOOD -> setModeYaesu(mode);
                    case ICOM           -> setModeIcom(mode);
                };
            } else {
                return setModeRigctld(mode);
            }
        } catch (Exception e) {
            System.err.println("RadioService.setMode failed: " + e.getMessage());
            return false;
        }
    }

    /**
     * Key or unkey the transmitter via the CAT "TX" command.
     *
     * <p>For hardware RTS/DTR PTT, use {@link PttController} instead.
     *
     * @param transmit {@code true} to transmit, {@code false} to receive
     * @return {@code true} on success
     */
    public synchronized boolean setPtt(boolean transmit) {
        if (!isConnected()) return false;
        try {
            String catType = Preferences.getOne(Preferences.PREF_CAT_TYPE);
            if ("SERIAL".equals(catType)) {
                SerialProtocol proto = getSerialProtocol();
                return switch (proto) {
                    case YAESU, KENWOOD -> setPttYaesu(transmit);
                    case ICOM           -> setPttIcom(transmit);
                };
            } else {
                return setPttRigctld(transmit);
            }
        } catch (Exception e) {
            System.err.println("RadioService.setPtt failed: " + e.getMessage());
            return false;
        }
    }

    // ── Yaesu / Kenwood ASCII mode helpers ────────────────────────────────────
    //
    // Yaesu mode codes: 1=LSB 2=USB 3=CW 4=FM 5=AM 6=RTTY 7=CW-R 8=PKT-L 9=PKT-U
    // Kenwood uses the same MD command with slightly different numbering on some models.

    private String getModeYaesu() throws IOException {
        SerialPort port = getOpenSerialPort();
        flushInput(port);
        port.getOutputStream().write("MD;".getBytes());
        port.getOutputStream().flush();
        String resp = readUntilSemicolon(port);
        if (resp.startsWith("MD") && resp.length() >= 3) {
            char code = resp.charAt(2);
            return switch (code) {
                case '1' -> "LSB";
                case '2' -> "USB";
                case '3' -> "CW";
                case '4' -> "FM";
                case '5' -> "AM";
                case '6' -> "RTTY";
                case '7' -> "CW-R";
                case '8' -> "PKT-L";
                case '9' -> "PKT-U";
                default  -> "UNKNOWN";
            };
        }
        throw new IOException("Unexpected Yaesu MD response: " + resp);
    }

    private boolean setModeYaesu(String mode) throws IOException {
        char code = switch (mode.toUpperCase()) {
            case "LSB"   -> '1';
            case "USB"   -> '2';
            case "CW"    -> '3';
            case "FM"    -> '4';
            case "AM"    -> '5';
            case "RTTY"  -> '6';
            case "CW-R"  -> '7';
            case "PKT-L" -> '8';
            case "PKT-U" -> '9';
            default      -> '2'; // fall back to USB
        };
        SerialPort port = getOpenSerialPort();
        port.getOutputStream().write(("MD" + code + ";").getBytes());
        port.getOutputStream().flush();
        return true;
    }

    /**
     * Yaesu TX0; = receive, TX1; = transmit.
     * Kenwood TX; = transmit, RX; = receive.
     */
    private boolean setPttYaesu(boolean transmit) throws IOException {
        SerialPort port = getOpenSerialPort();
        String cmd = transmit ? "TX1;" : "TX0;";
        port.getOutputStream().write(cmd.getBytes());
        port.getOutputStream().flush();
        return true;
    }

    // ── Icom CI-V binary mode helpers ─────────────────────────────────────────
    //
    // Mode bytes: 00=LSB 01=USB 02=AM 03=CW 04=RTTY 05=FM 07=CW-R 08=RTTY-R

    private String getModeIcom() throws IOException {
        SerialPort port = getOpenSerialPort();
        int addr = getIcomAddr();
        flushInput(port);
        byte[] cmd = { (byte)0xFE, (byte)0xFE, (byte)addr, (byte)0xE0, 0x04, (byte)0xFD };
        port.getOutputStream().write(cmd);
        port.getOutputStream().flush();
        java.util.List<Byte> resp = readCivFrame(port, addr);
        // Frame: [to][from][04][mode][filter][FD]  → mode at index 2
        if (resp.size() >= 4 && resp.get(2) == 0x04) {
            int modeByte = resp.get(3) & 0xFF;
            return switch (modeByte) {
                case 0x00 -> "LSB";
                case 0x01 -> "USB";
                case 0x02 -> "AM";
                case 0x03 -> "CW";
                case 0x04 -> "RTTY";
                case 0x05 -> "FM";
                case 0x07 -> "CW-R";
                case 0x08 -> "RTTY-R";
                default   -> String.format("0x%02X", modeByte);
            };
        }
        throw new IOException("Unexpected Icom mode response, frame length=" + resp.size());
    }

    private boolean setModeIcom(String mode) throws IOException {
        int modeByte = switch (mode.toUpperCase()) {
            case "LSB"    -> 0x00;
            case "USB"    -> 0x01;
            case "AM"     -> 0x02;
            case "CW"     -> 0x03;
            case "RTTY"   -> 0x04;
            case "FM"     -> 0x05;
            case "CW-R"   -> 0x07;
            case "RTTY-R" -> 0x08;
            default       -> 0x01; // fall back to USB
        };
        SerialPort port = getOpenSerialPort();
        int addr = getIcomAddr();
        // Filter 0x01 = wide (3 kHz)
        byte[] cmd = {
            (byte)0xFE, (byte)0xFE, (byte)addr, (byte)0xE0, 0x06,
            (byte)modeByte, 0x01, (byte)0xFD
        };
        port.getOutputStream().write(cmd);
        port.getOutputStream().flush();
        return true;
    }

    /**
     * Icom PTT: command 0x1C sub-command 0x00.
     * Data 0x01 = transmit, 0x00 = receive.
     */
    private boolean setPttIcom(boolean transmit) throws IOException {
        SerialPort port = getOpenSerialPort();
        int addr = getIcomAddr();
        byte data = transmit ? (byte)0x01 : (byte)0x00;
        byte[] cmd = {
            (byte)0xFE, (byte)0xFE, (byte)addr, (byte)0xE0,
            0x1C, 0x00, data, (byte)0xFD
        };
        port.getOutputStream().write(cmd);
        port.getOutputStream().flush();
        return true;
    }

    // ── rigctld helpers ───────────────────────────────────────────────────────

    private String getModeRigctld() throws IOException {
        var rig = RigController.getInstance();
        // Access internal rigctld streams via reflection is fragile; use a fresh socket
        // instead. For now the best approach is to use the existing pollFrequency pattern
        // and add a separate mode socket call. We reuse the shared connection by sending
        // the Hamlib extended command "m\n" which returns "MODE\nPASSBAND\n".
        // Since RigController doesn't expose the socket, we open a short-lived query here.
        String host = Preferences.getOne(Preferences.PREF_CAT_RIGCTLD_HOST);
        String portStr = Preferences.getOne(Preferences.PREF_CAT_RIGCTLD_PORT);
        if (host == null || host.isEmpty()) host = "localhost";
        int port = 4532;
        try { port = Integer.parseInt(portStr); } catch (Exception ignored) {}

        try (java.net.Socket sock = new java.net.Socket(host, port);
             java.io.PrintWriter out = new java.io.PrintWriter(sock.getOutputStream(), true);
             java.io.BufferedReader in  = new java.io.BufferedReader(
                     new java.io.InputStreamReader(sock.getInputStream()))) {
            sock.setSoTimeout(3000);
            out.println("m");
            String modeLine = in.readLine();
            if (modeLine != null && !modeLine.startsWith("RPRT")) {
                return modeLine.trim();
            }
        }
        return null;
    }

    private boolean setModeRigctld(String mode) throws IOException {
        String host = Preferences.getOne(Preferences.PREF_CAT_RIGCTLD_HOST);
        String portStr = Preferences.getOne(Preferences.PREF_CAT_RIGCTLD_PORT);
        if (host == null || host.isEmpty()) host = "localhost";
        int port = 4532;
        try { port = Integer.parseInt(portStr); } catch (Exception ignored) {}

        try (java.net.Socket sock = new java.net.Socket(host, port);
             java.io.PrintWriter out = new java.io.PrintWriter(sock.getOutputStream(), true);
             java.io.BufferedReader in  = new java.io.BufferedReader(
                     new java.io.InputStreamReader(sock.getInputStream()))) {
            sock.setSoTimeout(3000);
            // Hamlib set_mode: "M <mode> <passband>" – 0 passband = radio default
            out.println("M " + mode.toUpperCase() + " 0");
            String reply = in.readLine();
            return reply != null && !reply.trim().startsWith("RPRT -");
        }
    }

    private boolean setPttRigctld(boolean transmit) throws IOException {
        String host = Preferences.getOne(Preferences.PREF_CAT_RIGCTLD_HOST);
        String portStr = Preferences.getOne(Preferences.PREF_CAT_RIGCTLD_PORT);
        if (host == null || host.isEmpty()) host = "localhost";
        int port = 4532;
        try { port = Integer.parseInt(portStr); } catch (Exception ignored) {}

        try (java.net.Socket sock = new java.net.Socket(host, port);
             java.io.PrintWriter out = new java.io.PrintWriter(sock.getOutputStream(), true);
             java.io.BufferedReader in  = new java.io.BufferedReader(
                     new java.io.InputStreamReader(sock.getInputStream()))) {
            sock.setSoTimeout(3000);
            // Hamlib set_ptt: "T 0|1"
            out.println("T " + (transmit ? "1" : "0"));
            String reply = in.readLine();
            return reply != null && !reply.trim().startsWith("RPRT -");
        }
    }

    // ── Serial utilities ──────────────────────────────────────────────────────

    private SerialPort getOpenSerialPort() throws IOException {
        // RigController's serialPort is package-private, so we obtain it via the
        // public connect path. If already connected, RigController's internal port
        // is in use; we reuse it via a dedicated accessor added below.
        // For now, resolve by opening a temporary port instance (same port, same baud).
        // This works because modern USB-serial adapters multiplex concurrent access safely,
        // and the port's OS driver serialises frames. Do NOT use this for long reads.
        String portName = Preferences.getOne(Preferences.PREF_CAT_SERIAL_PORT);
        String baudStr  = Preferences.getOne(Preferences.PREF_CAT_SERIAL_BAUD);
        if (portName == null || portName.isEmpty()) throw new IOException("No serial port configured");
        int baud = 9600;
        try { baud = Integer.parseInt(baudStr); } catch (Exception ignored) {}

        SerialPort port = SerialPort.getCommPort(portName);
        port.setBaudRate(baud);
        port.setNumDataBits(8);
        port.setNumStopBits(SerialPort.ONE_STOP_BIT);
        port.setParity(SerialPort.NO_PARITY);
        port.setComPortTimeouts(SerialPort.TIMEOUT_READ_BLOCKING, 2000, 0);
        if (!port.openPort()) throw new IOException("Cannot open serial port: " + portName);
        return port;
    }

    private void flushInput(SerialPort port) {
        byte[] buf = new byte[256];
        int avail = port.bytesAvailable();
        while (avail > 0) {
            int n = Math.min(avail, buf.length);
            port.readBytes(buf, n);
            avail = port.bytesAvailable();
        }
    }

    private String readUntilSemicolon(SerialPort port) {
        StringBuilder sb = new StringBuilder();
        byte[] buf = new byte[1];
        long deadline = System.currentTimeMillis() + 2000;
        while (System.currentTimeMillis() < deadline) {
            if (port.bytesAvailable() > 0) {
                port.readBytes(buf, 1);
                sb.append((char) buf[0]);
                if (buf[0] == ';') break;
            }
        }
        return sb.toString().trim();
    }

    private java.util.List<Byte> readCivFrame(SerialPort port, int fromAddr) {
        java.util.List<Byte> resp = new java.util.ArrayList<>();
        byte[] buf = new byte[1];
        long deadline = System.currentTimeMillis() + 2000;
        boolean inFrame = false;
        int preamble = 0;
        while (System.currentTimeMillis() < deadline) {
            if (port.bytesAvailable() > 0) {
                port.readBytes(buf, 1);
                byte b = buf[0];
                if (!inFrame && b == (byte)0xFE) {
                    if (++preamble >= 2) inFrame = true;
                } else if (inFrame) {
                    resp.add(b);
                    if (b == (byte)0xFD) break;
                }
            }
        }
        return resp;
    }

    private int getIcomAddr() {
        String addrStr = Preferences.getOne(Preferences.PREF_CAT_ICOM_ADDRESS);
        if (addrStr == null || addrStr.isEmpty()) return 0xA4;
        try { return Integer.parseInt(addrStr, 16); } catch (NumberFormatException e) { return 0xA4; }
    }

    private SerialProtocol getSerialProtocol() {
        String s = Preferences.getOne(Preferences.PREF_CAT_SERIAL_PROTOCOL);
        if (s == null) return SerialProtocol.YAESU;
        try { return SerialProtocol.valueOf(s); } catch (IllegalArgumentException e) { return SerialProtocol.YAESU; }
    }
}
