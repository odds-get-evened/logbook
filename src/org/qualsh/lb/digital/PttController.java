package org.qualsh.lb.digital;

import com.fazecast.jSerialComm.SerialPort;
import org.qualsh.lb.util.Preferences;

/**
 * PttController – hardware and software PTT (Push-To-Talk) for digital modes.
 *
 * <h2>PTT Methods</h2>
 * <ul>
 *   <li><b>VOX</b> – no action; the radio keys itself on audio power.</li>
 *   <li><b>RTS</b> – raise/lower the RTS pin on a serial/USB port.
 *       Many interfaces (SignaLink, home-brew) use this.</li>
 *   <li><b>DTR</b> – same but using the DTR pin instead of RTS.
 *       Some interfaces (e.g. Digirig) wire PTT to DTR.</li>
 *   <li><b>CAT</b> – delegate to {@link RadioService#setPtt(boolean)} which
 *       sends the appropriate protocol command (Yaesu TX1;, Icom 0x1C…).</li>
 * </ul>
 *
 * <h2>Typical wiring</h2>
 * <pre>
 *   DB-9 pin 7 = RTS  →  PTT input of rig interface
 *   DB-9 pin 4 = DTR  →  PTT input of rig interface (alternative)
 *   Ground (pin 5)    →  PTT ground
 * </pre>
 *
 * <p>jSerialComm exposes {@code setRTS()} / {@code clearRTS()} and
 * {@code setDTR()} / {@code clearDTR()} which map to OS-level TIOCMSET ioctls
 * on Linux and SetCommState on Windows, making them nearly instantaneous.
 *
 * <h2>PTT Port vs CAT Port</h2>
 * <p>The PTT serial port may be the <em>same</em> port as the CAT port, or a
 * <em>different</em> USB-serial adapter dedicated to the interface.  Both are
 * supported; configure the PTT port via
 * {@link Preferences#PREF_DIGITAL_PTT_PORT}.
 */
public class PttController {

    public enum PttMethod {
        VOX("VOX (audio-activated)"),
        RTS("RTS pin"),
        DTR("DTR pin"),
        CAT("CAT command (software)");

        private final String label;
        PttMethod(String label) { this.label = label; }
        public String getLabel() { return label; }
        @Override public String toString() { return label; }
    }

    // ── Singleton ─────────────────────────────────────────────────────────────

    private static PttController instance;

    public static synchronized PttController getInstance() {
        if (instance == null) instance = new PttController();
        return instance;
    }

    private PttController() {}

    // ── State ─────────────────────────────────────────────────────────────────

    /** Dedicated PTT serial port (separate from the CAT port). May be null. */
    private SerialPort pttPort;
    private volatile boolean transmitting = false;

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Open the dedicated PTT serial port if the configured method is RTS or DTR
     * and the PTT port is different from the CAT port.
     *
     * <p>Call this once after the settings are loaded. It is safe to call again
     * after the port name changes.
     *
     * @return {@code true} if the port was opened successfully or no port is needed
     */
    public synchronized boolean openPttPort() {
        closePttPort();

        PttMethod method = getConfiguredMethod();
        if (method != PttMethod.RTS && method != PttMethod.DTR) return true;

        String portName = Preferences.getOne(Preferences.PREF_DIGITAL_PTT_PORT);
        if (portName == null || portName.isEmpty()) {
            // Reuse the CAT port – the lines can still be toggled on the already-open port
            // via RigController's serialPort field. In this case we don't open separately.
            return true;
        }

        // Open a dedicated port just for pin control.
        // Baud rate is irrelevant for RTS/DTR-only usage; 9600 is fine.
        SerialPort port = SerialPort.getCommPort(portName);
        port.setBaudRate(9600);
        port.setNumDataBits(8);
        port.setNumStopBits(SerialPort.ONE_STOP_BIT);
        port.setParity(SerialPort.NO_PARITY);
        if (!port.openPort()) {
            System.err.println("PttController: cannot open PTT port " + portName);
            return false;
        }
        pttPort = port;
        // Ensure we start in RX state (pins deasserted)
        clearPins(pttPort);
        return true;
    }

    /** Close the dedicated PTT port, driving the radio back to receive first. */
    public synchronized void closePttPort() {
        if (pttPort != null && pttPort.isOpen()) {
            clearPins(pttPort);
            pttPort.closePort();
        }
        pttPort = null;
    }

    /**
     * Activate or deactivate PTT according to the configured method.
     *
     * @param transmit {@code true} to key the transmitter, {@code false} to return to receive
     * @return {@code true} on success
     */
    public synchronized boolean setPtt(boolean transmit) {
        PttMethod method = getConfiguredMethod();
        boolean ok = switch (method) {
            case VOX -> true;  // nothing to do; audio presence keys the radio
            case RTS -> setRts(transmit);
            case DTR -> setDtr(transmit);
            case CAT -> RadioService.getInstance().setPtt(transmit);
        };
        if (ok) transmitting = transmit;
        return ok;
    }

    public boolean isTransmitting() { return transmitting; }

    // ── Pin control ───────────────────────────────────────────────────────────

    /**
     * Set RTS high (transmit) or low (receive).
     *
     * <p>jSerialComm API:
     * <pre>
     *   port.setRTS()   → assert RTS high  (logic 1, positive voltage ~+10 V)
     *   port.clearRTS() → deassert RTS low (logic 0, negative voltage ~−10 V)
     * </pre>
     * Some interfaces are active-low; invert the logic if needed.
     */
    private boolean setRts(boolean assert_) {
        SerialPort port = resolvePort();
        if (port == null) return false;
        if (assert_) port.setRTS(); else port.clearRTS();
        return true;
    }

    /**
     * Set DTR high (transmit) or low (receive).
     *
     * <pre>
     *   port.setDTR()   → assert DTR high
     *   port.clearDTR() → deassert DTR low
     * </pre>
     */
    private boolean setDtr(boolean assert_) {
        SerialPort port = resolvePort();
        if (port == null) return false;
        if (assert_) port.setDTR(); else port.clearDTR();
        return true;
    }

    private void clearPins(SerialPort port) {
        port.clearRTS();
        port.clearDTR();
    }

    /**
     * Resolve which serial port to use for pin manipulation.
     *
     * <p>Preference order:
     * <ol>
     *   <li>The dedicated PTT port opened by {@link #openPttPort()}</li>
     *   <li>A fresh port object pointing at the CAT port name
     *       (works when the OS allows multiple handles to the same device,
     *        e.g. Linux {@code /dev/ttyUSB0})</li>
     * </ol>
     */
    private SerialPort resolvePort() {
        if (pttPort != null && pttPort.isOpen()) return pttPort;

        // Fall back to CAT port
        String portName = Preferences.getOne(Preferences.PREF_CAT_SERIAL_PORT);
        if (portName == null || portName.isEmpty()) {
            System.err.println("PttController: no port configured for PTT");
            return null;
        }
        SerialPort port = SerialPort.getCommPort(portName);
        port.setBaudRate(9600);
        port.setNumDataBits(8);
        port.setNumStopBits(SerialPort.ONE_STOP_BIT);
        port.setParity(SerialPort.NO_PARITY);
        if (!port.openPort()) {
            System.err.println("PttController: cannot open fallback CAT port for PTT: " + portName);
            return null;
        }
        // Cache so we don't re-open every call
        pttPort = port;
        return pttPort;
    }

    // ── Utilities ─────────────────────────────────────────────────────────────

    private static PttMethod getConfiguredMethod() {
        String s = Preferences.getOne(Preferences.PREF_DIGITAL_PTT_METHOD);
        if (s == null) return PttMethod.VOX;
        try { return PttMethod.valueOf(s); } catch (IllegalArgumentException e) { return PttMethod.VOX; }
    }
}
