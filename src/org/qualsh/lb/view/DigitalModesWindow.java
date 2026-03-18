package org.qualsh.lb.view;

import org.qualsh.lb.data.LogsModel;
import org.qualsh.lb.digital.*;
import org.qualsh.lb.digital.WsjtxUdpListener.QsoLoggedMessage;
import org.qualsh.lb.digital.WsjtxUdpListener.StatusMessage;
import org.qualsh.lb.log.Log;
import org.qualsh.lb.util.Preferences;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * DigitalModesWindow – secondary JFrame for FT8 / FT4 / JS8Call operation.
 *
 * <h2>Layout overview</h2>
 * <pre>
 * ┌─────────────────────────────────────────────────────────────┐
 * │  [Mode: FT8 ▼]  [Freq: 14.074000 MHz]  [Set Band Defaults] │  ← toolbar
 * ├─────────────────────────────────────────────────────────────┤
 * │  Decoded Messages (from WSJT-X UDP)                         │  ← live table
 * │  Time    Call     Grid   dB   DT   Freq   Message           │
 * │  …                                                          │
 * ├─────────────────────────────────────────────────────────────┤
 * │  [PTT: RX ●]  Audio: ● Capture  ● Playback   [Settings…]   │  ← status bar
 * ├─────────────────────────────────────────────────────────────┤
 * │  Auto-log: [✓ Auto-log QSOs]  Recent auto-logs table        │
 * └─────────────────────────────────────────────────────────────┘
 * </pre>
 *
 * <h2>WSJT-X integration</h2>
 * <p>This window listens on the configured UDP port for WSJT-X broadcasts.
 * Each decoded line from a WSJT-X Status message is displayed in the table.
 * When WSJT-X sends a <b>QSO-Logged (Type 5)</b> message, and auto-log is
 * enabled, a {@link Log} entry is automatically inserted into the database.
 *
 * <h2>Concurrency</h2>
 * <ul>
 *   <li>Frequency polling: delegated to {@link org.qualsh.lb.rig.RigController}'s
 *       {@code ScheduledExecutorService} (runs every 2 s on "RigController-poll" thread).
 *       The listener callback posts a UI update via {@link SwingUtilities#invokeLater}.</li>
 *   <li>UDP listener: runs on "WsjtxUdpListener" daemon thread; callbacks are similarly
 *       dispatched to the EDT via {@code invokeLater}.</li>
 *   <li>Audio capture: runs on "AudioRouter-capture" daemon thread; raw PCM bytes
 *       are forwarded to WSJT-X via loopback audio – no EDT involvement.</li>
 *   <li>The Swing EDT is <em>never</em> blocked by serial I/O or audio.</li>
 * </ul>
 */
public class DigitalModesWindow extends JFrame {

    private static final long serialVersionUID = 1L;

    private static final DateTimeFormatter TIME_FMT =
            DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault());

    // ── UI components ─────────────────────────────────────────────────────────

    private JComboBox<DigitalMode> comboMode;
    private JLabel lblFrequency;
    private JLabel lblRigStatus;
    private JButton btnPtt;
    private JLabel lblCaptureStatus;
    private JLabel lblPlaybackStatus;
    private JCheckBox chkAutoLog;

    /** Live WSJT-X decoded messages: Time | Call | Grid | dB | DT | Freq | Msg */
    private DefaultTableModel decodedModel;
    private JTable decodedTable;

    /** Auto-logged QSOs this session */
    private DefaultTableModel autoLogModel;
    private JTable autoLogTable;

    // ── References ────────────────────────────────────────────────────────────

    /** May be null if the main window hasn't been set up yet. */
    private final LogsModel logsModel;

    // ── Polling ───────────────────────────────────────────────────────────────

    private volatile boolean transmitting = false;

    // ── Constructor ───────────────────────────────────────────────────────────

    public DigitalModesWindow(JFrame owner, LogsModel logsModel) {
        super("Digital Modes");
        this.logsModel = logsModel;

        setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        setMinimumSize(new Dimension(700, 550));
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        setSize((int)(screen.getWidth() * 0.52), (int)(screen.getHeight() * 0.65));

        // Position to the right of the main window if possible
        if (owner != null) {
            Point ownerLoc  = owner.getLocation();
            Dimension ownerSz = owner.getSize();
            int x = ownerLoc.x + ownerSz.width + 8;
            int y = ownerLoc.y;
            if (x + getWidth() > screen.getWidth()) x = Math.max(0, (int)(screen.getWidth() - getWidth()));
            setLocation(x, y);
        } else {
            setLocationRelativeTo(null);
        }

        buildUI();
        wireListeners();
        loadPrefsAndStart();
    }

    // ── UI construction ───────────────────────────────────────────────────────

    private void buildUI() {
        JPanel outer = new JPanel(new BorderLayout(0, 4));
        outer.setBorder(new EmptyBorder(8, 8, 8, 8));
        setContentPane(outer);

        outer.add(buildToolbar(),     BorderLayout.NORTH);
        outer.add(buildCentrePanel(), BorderLayout.CENTER);
        outer.add(buildStatusBar(),   BorderLayout.SOUTH);
    }

    private JPanel buildToolbar() {
        JPanel tb = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));

        tb.add(new JLabel("Mode:"));
        comboMode = new JComboBox<>(DigitalMode.values());
        comboMode.setToolTipText("Select digital mode");
        tb.add(comboMode);

        tb.add(Box.createHorizontalStrut(16));
        tb.add(new JLabel("VFO-A:"));
        lblFrequency = new JLabel("----.--- MHz");
        lblFrequency.setFont(new Font(Font.MONOSPACED, Font.BOLD, 14));
        tb.add(lblFrequency);

        JButton btnSetFreq = new JButton("Set Band Default");
        btnSetFreq.setToolTipText("Tune radio to this mode's default 20 m frequency");
        btnSetFreq.addActionListener(e -> onSetDefaultFrequency());
        tb.add(btnSetFreq);

        tb.add(Box.createHorizontalStrut(16));
        lblRigStatus = new JLabel("Rig: disconnected");
        lblRigStatus.setForeground(Color.GRAY);
        tb.add(lblRigStatus);

        JButton btnSettings = new JButton("Settings\u2026");
        btnSettings.addActionListener(e -> {
            DigitalModesSettingsDialog dlg = new DigitalModesSettingsDialog(this);
            dlg.setVisible(true);
            // Re-apply after dialog closes
            loadPrefsAndStart();
        });
        tb.add(btnSettings);

        return tb;
    }

    private JSplitPane buildCentrePanel() {
        // ── Top: decoded messages ──────────────────────────────────────────────
        String[] decodedCols = { "UTC", "Call", "Grid", "dB", "DT", "Freq (Hz)", "Message" };
        decodedModel = new DefaultTableModel(decodedCols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        decodedTable = new JTable(decodedModel);
        decodedTable.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        decodedTable.getColumnModel().getColumn(0).setPreferredWidth(60);
        decodedTable.getColumnModel().getColumn(1).setPreferredWidth(80);
        decodedTable.getColumnModel().getColumn(2).setPreferredWidth(55);
        decodedTable.getColumnModel().getColumn(3).setPreferredWidth(40);
        decodedTable.getColumnModel().getColumn(4).setPreferredWidth(40);
        decodedTable.getColumnModel().getColumn(5).setPreferredWidth(65);
        decodedTable.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        decodedTable.setRowHeight(18);
        JScrollPane spDecoded = new JScrollPane(decodedTable);
        spDecoded.setBorder(new TitledBorder("Decoded Messages (from WSJT-X / JS8Call)"));
        spDecoded.setPreferredSize(new Dimension(0, 200));

        // ── Bottom: auto-logged QSOs ───────────────────────────────────────────
        String[] autoLogCols = { "Time", "Call", "Grid", "Freq (kHz)", "Mode", "RST Snt", "RST Rcv", "Notes" };
        autoLogModel = new DefaultTableModel(autoLogCols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        autoLogTable = new JTable(autoLogModel);
        autoLogTable.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        autoLogTable.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        autoLogTable.setRowHeight(18);
        JScrollPane spAutoLog = new JScrollPane(autoLogTable);

        JPanel autoLogPanel = new JPanel(new BorderLayout());
        autoLogPanel.setBorder(new TitledBorder("Auto-logged QSOs (this session)"));

        JPanel autoLogToolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        chkAutoLog = new JCheckBox("Auto-log completed QSOs from WSJT-X", true);
        chkAutoLog.setToolTipText(
                "When enabled, every QSO logged in WSJT-X is automatically added to the Logbook");
        autoLogToolbar.add(chkAutoLog);
        autoLogPanel.add(autoLogToolbar, BorderLayout.NORTH);
        autoLogPanel.add(spAutoLog, BorderLayout.CENTER);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, spDecoded, autoLogPanel);
        split.setResizeWeight(0.6);
        split.setBorder(null);
        return split;
    }

    private JPanel buildStatusBar() {
        JPanel sb = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 3));

        btnPtt = new JButton("PTT: RX");
        btnPtt.setForeground(new Color(0, 120, 0));
        btnPtt.setFocusable(false);
        btnPtt.addActionListener(e -> onTogglePtt());
        sb.add(btnPtt);

        sb.add(new JSeparator(JSeparator.VERTICAL));

        lblCaptureStatus = statusDot("Capture");
        lblPlaybackStatus = statusDot("Playback");
        sb.add(lblCaptureStatus);
        sb.add(lblPlaybackStatus);

        return sb;
    }

    /** Small coloured dot + label for audio status. */
    private JLabel statusDot(String name) {
        JLabel l = new JLabel("\u25CF " + name + ": off");
        l.setForeground(Color.GRAY);
        l.setFont(l.getFont().deriveFont(11f));
        return l;
    }

    // ── Listener wiring ───────────────────────────────────────────────────────

    private void wireListeners() {
        // Frequency updates from RigController → EDT → UI label
        RadioService.getInstance().addFrequencyListener(freqKhz ->
            SwingUtilities.invokeLater(() -> updateFrequencyLabel(freqKhz))
        );

        // WSJT-X Status (live decoded lines)
        WsjtxUdpListener.getInstance().addStatusListener(msg ->
            SwingUtilities.invokeLater(() -> handleWsjtxStatus(msg))
        );

        // WSJT-X QSO-Logged (Type 5) → auto-log
        WsjtxUdpListener.getInstance().addQsoLoggedListener(msg ->
            SwingUtilities.invokeLater(() -> handleQsoLogged(msg))
        );
    }

    // ── Start-up ──────────────────────────────────────────────────────────────

    private void loadPrefsAndStart() {
        // Start WSJT-X UDP listener
        startWsjtxListener();

        // Start audio routing
        startAudio();

        // Open PTT port
        PttController.getInstance().openPttPort();

        // Connect to rig if settings are available
        if (!RadioService.getInstance().isConnected()) {
            new Thread(() -> {
                boolean ok = RadioService.getInstance().connect();
                SwingUtilities.invokeLater(() -> updateRigStatus(ok));
            }, "DigitalModesWin-connect").start();
        } else {
            updateRigStatus(true);
        }
    }

    private void startWsjtxListener() {
        WsjtxUdpListener udp = WsjtxUdpListener.getInstance();

        // Parse WSJT-X port
        String wsjtxPortStr = Preferences.getOne(Preferences.PREF_DIGITAL_WSJTX_UDP_PORT);
        int wsjtxPort = WsjtxUdpListener.DEFAULT_PORT;
        if (wsjtxPortStr != null && !wsjtxPortStr.isEmpty()) {
            try { wsjtxPort = Integer.parseInt(wsjtxPortStr); } catch (NumberFormatException ignored) {}
        }

        if (!udp.isRunning()) {
            boolean ok = udp.start(wsjtxPort);
            if (!ok) {
                // Try JS8Call port as fallback
                String js8PortStr = Preferences.getOne(Preferences.PREF_DIGITAL_JS8CALL_UDP_PORT);
                int js8Port = WsjtxUdpListener.JS8CALL_PORT;
                try { js8Port = Integer.parseInt(js8PortStr); } catch (Exception ignored) {}
                udp.start(js8Port);
            }
        }
    }

    private void startAudio() {
        AudioRouter audio = AudioRouter.getInstance();

        String capDev  = Preferences.getOne(Preferences.PREF_DIGITAL_CAPTURE_DEVICE);
        String playDev = Preferences.getOne(Preferences.PREF_DIGITAL_PLAYBACK_DEVICE);

        // Capture
        if (!audio.isCapturing()) {
            boolean ok = audio.startCapture(capDev);
            updateCaptureStatus(ok);
        } else {
            updateCaptureStatus(true);
        }

        // Playback
        if (!audio.isPlaybackOpen()) {
            boolean ok = audio.openPlayback(playDev);
            updatePlaybackStatus(ok);
        } else {
            updatePlaybackStatus(true);
        }
    }

    // ── Event handlers ────────────────────────────────────────────────────────

    private void onSetDefaultFrequency() {
        DigitalMode mode = (DigitalMode) comboMode.getSelectedItem();
        if (mode == null) return;

        // Set radio mode to USB first, then frequency
        new Thread(() -> {
            RadioService svc = RadioService.getInstance();
            if (svc.isConnected()) {
                svc.setMode(mode.getCatMode());
                svc.setFrequencyKhz(mode.getDefaultFreqKhz());
            }
            SwingUtilities.invokeLater(() ->
                updateFrequencyLabel(mode.getDefaultFreqKhz()));
        }, "DigitalModesWin-setFreq").start();
    }

    private void onTogglePtt() {
        transmitting = !transmitting;
        new Thread(() -> {
            PttController.getInstance().setPtt(transmitting);
            final boolean tx = transmitting;
            SwingUtilities.invokeLater(() -> updatePttButton(tx));
        }, "DigitalModesWin-ptt").start();
    }

    // ── WSJT-X callbacks ──────────────────────────────────────────────────────

    /**
     * A WSJT-X Status message arrived. Used mainly to update the frequency
     * display (WSJT-X tracks the radio dial independently via its own CAT).
     *
     * <p>WSJT-X does not broadcast individual decoded lines via UDP Status;
     * decoded lines appear in the WSJT-X window itself. The Status message
     * gives us the current dial frequency and transmit state so we can keep
     * the window in sync even when the user operates WSJT-X directly.
     */
    private void handleWsjtxStatus(StatusMessage msg) {
        if (msg.dialFreqHz() > 0) {
            updateFrequencyLabel(msg.dialFreqHz() / 1000.0);
        }
        updatePttButton(msg.transmitting());
        transmitting = msg.transmitting();
    }

    /**
     * WSJT-X logged a QSO (Type 5). Auto-create a {@link Log} entry if enabled.
     *
     * <p>The description string is formatted as:
     * <pre>  [FT8] W1AW EM72 RST: -10/-12  WSJT-X auto</pre>
     */
    private void handleQsoLogged(QsoLoggedMessage msg) {
        // Always add to the session table
        String timeStr = msg.dateOn() != null
                ? msg.dateOn().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                  + " " + formatSecondsOfDay(msg.timeOnSecOfDay())
                : "?";

        autoLogModel.insertRow(0, new Object[]{
                timeStr,
                msg.dxCall(),
                msg.dxGrid(),
                String.format("%.3f", msg.dialFreqKhz()),
                msg.mode(),
                msg.rstSent(),
                msg.rstRcvd(),
                buildDescription(msg)
        });
        // Keep at most 200 rows
        while (autoLogModel.getRowCount() > 200) {
            autoLogModel.removeRow(autoLogModel.getRowCount() - 1);
        }

        // Auto-log to the main logbook if enabled and model is available
        if (!chkAutoLog.isSelected() || logsModel == null) return;

        Log log = new Log();
        log.setFrequency((float) msg.dialFreqKhz());
        log.setMode(msg.mode() != null ? msg.mode() : "FT8");
        log.setDescription(buildDescription(msg));

        // dateOn as Unix timestamp
        if (msg.dateOn() != null) {
            long epochSec = msg.dateOn().atStartOfDay(ZoneId.systemDefault()).toEpochSecond()
                          + msg.timeOnSecOfDay();
            log.setDateOn((int) epochSec);
        } else {
            log.setDateOn((int)(System.currentTimeMillis() / 1000L));
        }

        // myPlace from global preferences
        String myPlaceStr = Preferences.getOne(Preferences.PREF_NAME_MY_PLACE);
        if (myPlaceStr != null && !myPlaceStr.isEmpty()) {
            try { log.setMyPlace(Integer.parseInt(myPlaceStr)); } catch (NumberFormatException ignored) {}
        }

        logsModel.insert(log);
    }

    private String buildDescription(QsoLoggedMessage msg) {
        StringBuilder sb = new StringBuilder();
        sb.append("[").append(msg.mode() != null ? msg.mode() : "DIGI").append("] ");
        if (msg.dxCall() != null && !msg.dxCall().isEmpty()) sb.append(msg.dxCall()).append(" ");
        if (msg.dxGrid() != null && !msg.dxGrid().isEmpty()) sb.append(msg.dxGrid()).append(" ");
        if (msg.rstSent() != null && !msg.rstSent().isEmpty()
                && msg.rstRcvd() != null && !msg.rstRcvd().isEmpty()) {
            sb.append("RST: ").append(msg.rstSent()).append("/").append(msg.rstRcvd()).append(" ");
        }
        if (msg.name() != null && !msg.name().isEmpty()) sb.append(msg.name()).append(" ");
        if (msg.comments() != null && !msg.comments().isEmpty()) sb.append(msg.comments()).append(" ");
        sb.append("(WSJT-X auto)");
        return sb.toString().trim();
    }

    // ── UI update helpers (call only from EDT) ─────────────────────────────────

    private void updateFrequencyLabel(double freqKhz) {
        lblFrequency.setText(String.format("%.6f MHz", freqKhz / 1000.0));
        updateRigStatus(true);
    }

    private void updateRigStatus(boolean connected) {
        if (connected) {
            lblRigStatus.setText("Rig: connected");
            lblRigStatus.setForeground(new Color(0, 130, 0));
        } else {
            lblRigStatus.setText("Rig: disconnected");
            lblRigStatus.setForeground(Color.GRAY);
        }
    }

    private void updatePttButton(boolean tx) {
        if (tx) {
            btnPtt.setText("PTT: TX");
            btnPtt.setForeground(Color.RED);
        } else {
            btnPtt.setText("PTT: RX");
            btnPtt.setForeground(new Color(0, 120, 0));
        }
    }

    private void updateCaptureStatus(boolean active) {
        lblCaptureStatus.setText("\u25CF Capture: " + (active ? "on" : "off"));
        lblCaptureStatus.setForeground(active ? new Color(0, 130, 0) : Color.GRAY);
    }

    private void updatePlaybackStatus(boolean active) {
        lblPlaybackStatus.setText("\u25CF Playback: " + (active ? "on" : "off"));
        lblPlaybackStatus.setForeground(active ? new Color(0, 130, 0) : Color.GRAY);
    }

    // ── Utilities ─────────────────────────────────────────────────────────────

    private static String formatSecondsOfDay(int sec) {
        int h = sec / 3600;
        int m = (sec % 3600) / 60;
        int s = sec % 60;
        return String.format("%02d:%02d:%02d", h, m, s);
    }

    /** Cleanly stop background services when the window is hidden. */
    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        if (!visible) {
            // Keep audio and UDP running so background logging continues.
            // Stop only when the application exits (via shutdown hook).
        }
    }

    /** Register a JVM shutdown hook to stop audio + UDP. */
    public void registerShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            AudioRouter.getInstance().stopCapture();
            AudioRouter.getInstance().closePlayback();
            WsjtxUdpListener.getInstance().stop();
            PttController.getInstance().closePttPort();
        }, "DigitalModesWindow-shutdown"));
    }
}
