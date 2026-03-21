package org.qualsh.lb.view;

import org.qualsh.lb.App;
import org.qualsh.lb.data.LogsModel;
import org.qualsh.lb.digital.*;
import org.qualsh.lb.digital.RxMode;
import org.qualsh.lb.digital.WsjtxUdpListener.QsoLoggedMessage;
import org.qualsh.lb.digital.WsjtxUdpListener.StatusMessage;
import org.qualsh.lb.log.Log;
import org.qualsh.lb.util.Preferences;

import javax.sound.sampled.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.*;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * DigitalModesWindow – secondary JFrame for FT8 / FT4 / JS8Call operation.
 *
 * <h2>Layout overview</h2>
 * <pre>
 * ┌─────────────────────────────────────────────────────────────┐
 * │  [Mode: FT8 ▼]  [Freq: 14.074000 MHz]  [Set Band Defaults] │  ← toolbar
 * ├─────────────────────────────────────────────────────────────┤
 * │  Spectrum + Waterfall display (0–4 kHz audio band)          │  ← spectrum/waterfall
 * │  [● 1500 Hz ± 100 Hz]  [Tune to Selected]                  │  ← selection info
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
 * <h2>Spectrum / Waterfall</h2>
 * <p>Live audio from the capture device is processed via a real-time FFT and
 * displayed as a scrolling waterfall. Click or drag on the panel to select a
 * centre frequency and bandwidth. Use <b>Tune to Selected</b> to shift the
 * radio's dial so the selected audio frequency aligns to the mode's nominal
 * tone centre (e.g. 1500 Hz for FT8/FT4/JS8Call).
 *
 * <h2>Audio file playback</h2>
 * <p>Use <b>Upload Audio…</b> to open a pre-recorded WAV/MP3/OGG/FLAC file and
 * route it to the configured playback device so WSJT-X can decode it. The file
 * audio is also fed through the waterfall. A <b>Stop Playback</b> button
 * appears during playback.
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
    private JComboBox<RxMode>     comboRxMode;
    private JLabel lblFrequency;
    private JLabel lblRigStatus;
    private JButton btnPtt;
    private JLabel lblCaptureStatus;
    private JLabel lblPlaybackStatus;
    private JCheckBox chkAutoLog;

    // ── Spectrum / waterfall ──────────────────────────────────────────────────

    private SpectrumWaterfallPanel waterfallPanel;
    private JLabel lblSelectionInfo;
    private Consumer<byte[]> waterfallCaptureListener;

    /** Live WSJT-X decoded messages: Time | Call | Grid | dB | DT | Freq | Msg */
    private DefaultTableModel decodedModel;
    private JTable decodedTable;

    /** Auto-logged QSOs this session */
    private DefaultTableModel autoLogModel;
    private JTable autoLogTable;

    // ── References ────────────────────────────────────────────────────────────

    /** May be null if the main window hasn't been set up yet. */
    private final LogsModel logsModel;

    // ── Recording ─────────────────────────────────────────────────────────────

    private JButton btnRecord;
    private volatile boolean recording = false;
    private ByteArrayOutputStream recordingBuffer;
    private Consumer<byte[]> recordingListener;

    // ── File playback ─────────────────────────────────────────────────────────

    private volatile boolean playingFile    = false;
    private volatile boolean playbackPaused = false;
    private volatile boolean loopPlayback   = false;

    /** PCM byte array of the currently loaded audio file; null when no file is cached. */
    private volatile byte[] cachedAudioPcm  = null;
    private String cachedAudioFileName      = null;

    private JButton       btnPlayPause;
    private JToggleButton btnLoop;
    private JButton       btnReset;
    private JLabel        lblAudioFile;
    private JButton       btnStopPlayback;

    // ── Current dial frequency (kHz) for tune-to-selected ────────────────────

    private volatile double currentFreqKhz = 0.0;

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

        setupIcons();
        buildUI();
        wireListeners();
        loadPrefsAndStart();
    }

    // ── Icon setup ────────────────────────────────────────────────────────────

    private void setupIcons() {
        List<Image> images = new ArrayList<>();
        String[] sizes = {"/imgs/lb_16x16.png", "/imgs/lb_32x32.png",
                          "/imgs/lb_48x48.png", "/imgs/lb_128x128.png"};
        for (String path : sizes) {
            java.net.URL url = App.class.getResource(path);
            if (url != null) images.add(Toolkit.getDefaultToolkit().getImage(url));
        }
        if (!images.isEmpty()) setIconImages(images);
    }

    // ── UI construction ───────────────────────────────────────────────────────

    private void buildUI() {
        JPanel outer = new JPanel(new BorderLayout(0, 4));
        outer.setBorder(new EmptyBorder(8, 8, 8, 8));
        setContentPane(outer);

        outer.add(buildToolbar(), BorderLayout.NORTH);

        // Centre area: waterfall at top, decoded/autolog tables below
        JPanel centreArea = new JPanel(new BorderLayout(0, 4));
        centreArea.add(buildWaterfallSection(), BorderLayout.NORTH);
        centreArea.add(buildCentrePanel(),      BorderLayout.CENTER);

        outer.add(centreArea,      BorderLayout.CENTER);
        outer.add(buildStatusBar(), BorderLayout.SOUTH);
    }

    private JPanel buildWaterfallSection() {
        waterfallPanel = new SpectrumWaterfallPanel();
        waterfallPanel.setBorder(BorderFactory.createTitledBorder("Spectrum / Waterfall  (click to select frequency · drag to set bandwidth)"));

        // Set initial selection to the first mode's tone centre
        DigitalMode defaultMode = (DigitalMode) comboMode.getSelectedItem();
        if (defaultMode != null) {
            waterfallPanel.setSelection(defaultMode.getAudioToneCentreHz(), 200);
        }

        waterfallPanel.setSelectionListener((centerHz, bwHz) ->
            SwingUtilities.invokeLater(() -> updateSelectionLabel(centerHz, bwHz))
        );

        // Selection info bar – built after waterfallPanel exists so getRxMode() works
        lblSelectionInfo = new JLabel("");
        lblSelectionInfo.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
        updateSelectionLabel(waterfallPanel.getCenterFreqHz(), waterfallPanel.getBandwidthHz());

        JButton btnTune = new JButton("Tune to Selected");
        btnTune.setToolTipText(
                "Shift the radio dial so the selected audio frequency aligns to this mode's tone centre");
        btnTune.addActionListener(e -> onTuneToSelected());

        JPanel infoBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        infoBar.add(lblSelectionInfo);
        infoBar.add(btnTune);

        JPanel section = new JPanel(new BorderLayout(0, 2));
        section.add(infoBar,        BorderLayout.NORTH);
        section.add(waterfallPanel, BorderLayout.CENTER);
        return section;
    }

    private JPanel buildToolbar() {
        JPanel tb = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));

        tb.add(new JLabel("Mode:"));
        comboMode = new JComboBox<>(DigitalMode.values());
        comboMode.setToolTipText("Select digital mode");
        tb.add(comboMode);

        tb.add(Box.createHorizontalStrut(8));
        tb.add(new JLabel("RX:"));
        comboRxMode = new JComboBox<>(RxMode.values());
        comboRxMode.setSelectedItem(RxMode.USB);
        comboRxMode.setToolTipText("Receive mode — sets which side of the cursor the passband occupies");
        tb.add(comboRxMode);

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
        JPanel sb = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 3));

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

        sb.add(new JSeparator(JSeparator.VERTICAL));

        btnRecord = new JButton("\u25CF Record");
        btnRecord.setToolTipText("Record incoming audio to a WAV file");
        btnRecord.addActionListener(e -> onToggleRecording());
        sb.add(btnRecord);

        sb.add(new JSeparator(JSeparator.VERTICAL));

        // Audio file playback controls
        JButton btnUpload = new JButton("\u25B2 Upload\u2026");
        btnUpload.setToolTipText("Load an audio file for playback (replaces any cached file)");
        btnUpload.addActionListener(e -> onUploadAudio());
        sb.add(btnUpload);

        lblAudioFile = new JLabel("No file");
        lblAudioFile.setFont(lblAudioFile.getFont().deriveFont(11f));
        lblAudioFile.setForeground(Color.GRAY);
        sb.add(lblAudioFile);

        btnPlayPause = new JButton("\u25B6 Play");
        btnPlayPause.setToolTipText("Play / pause the cached audio file");
        btnPlayPause.setVisible(false);
        btnPlayPause.addActionListener(e -> onTogglePlayPause());
        sb.add(btnPlayPause);

        btnLoop = new JToggleButton("\u21BA Loop");
        btnLoop.setToolTipText("Toggle loop / one-shot playback");
        btnLoop.setVisible(false);
        btnLoop.addActionListener(e -> loopPlayback = btnLoop.isSelected());
        sb.add(btnLoop);

        btnStopPlayback = new JButton("\u25A0 Stop");
        btnStopPlayback.setToolTipText("Stop playback");
        btnStopPlayback.setForeground(Color.RED);
        btnStopPlayback.setVisible(false);
        btnStopPlayback.addActionListener(e -> {
            playingFile = false;
            playbackPaused = false;
        });
        sb.add(btnStopPlayback);

        btnReset = new JButton("\u2715 Reset");
        btnReset.setToolTipText("Clear the cached audio file");
        btnReset.setVisible(false);
        btnReset.addActionListener(e -> onResetAudio());
        sb.add(btnReset);

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

        // Mode change → update waterfall default selection to mode's tone centre
        comboMode.addActionListener(e -> {
            DigitalMode mode = (DigitalMode) comboMode.getSelectedItem();
            if (mode != null && waterfallPanel != null) {
                waterfallPanel.setSelection(mode.getAudioToneCentreHz(), 200);
                updateSelectionLabel(mode.getAudioToneCentreHz(), 200);
            }
        });

        // RX mode change → propagate to waterfall panel
        comboRxMode.addActionListener(e -> {
            RxMode rxm = (RxMode) comboRxMode.getSelectedItem();
            if (rxm != null && waterfallPanel != null) {
                waterfallPanel.setRxMode(rxm);
                // Label update will arrive via the selectionListener callback
            }
        });
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

        // Register waterfall listener for live capture audio (idempotent)
        if (waterfallCaptureListener == null && waterfallPanel != null) {
            waterfallCaptureListener = waterfallPanel::feedPcm;
            audio.addCaptureListener(waterfallCaptureListener);
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

    /**
     * Shift the radio's dial frequency so that the user's selected audio
     * frequency aligns to this mode's nominal tone centre.
     *
     * <p>For USB: RF = dial + audio. So to move a signal at {@code selectedHz}
     * to appear at {@code modeAudioCentreHz}, we adjust the dial by
     * {@code (selectedHz − modeAudioCentreHz)} kHz.
     *
     * <p>Example: selected = 1200 Hz, mode centre = 1500 Hz, dial = 14 074.000 kHz
     * → new dial = 14 074.000 + (1200 − 1500)/1000 = 14 073.700 kHz
     */
    private void onTuneToSelected() {
        DigitalMode mode = (DigitalMode) comboMode.getSelectedItem();
        if (mode == null || currentFreqKhz == 0.0) return;

        int selectedHz     = waterfallPanel.getCenterFreqHz();
        int modeAudioHz    = mode.getAudioToneCentreHz();
        double offsetKhz   = (selectedHz - modeAudioHz) / 1000.0;
        double newFreqKhz  = currentFreqKhz + offsetKhz;

        new Thread(() -> {
            RadioService svc = RadioService.getInstance();
            if (svc.isConnected()) {
                svc.setFrequencyKhz(newFreqKhz);
            }
            SwingUtilities.invokeLater(() -> updateFrequencyLabel(newFreqKhz));
        }, "DigitalModesWin-tuneToSelected").start();
    }

    private void onTogglePtt() {
        transmitting = !transmitting;
        new Thread(() -> {
            PttController.getInstance().setPtt(transmitting);
            final boolean tx = transmitting;
            SwingUtilities.invokeLater(() -> updatePttButton(tx));
        }, "DigitalModesWin-ptt").start();
    }

    private void onToggleRecording() {
        if (!recording) {
            // Start recording
            recordingBuffer = new ByteArrayOutputStream();
            recordingListener = chunk -> {
                synchronized (DigitalModesWindow.this) {
                    if (recording && recordingBuffer != null) {
                        try { recordingBuffer.write(chunk); } catch (IOException ignored) {}
                    }
                }
            };
            AudioRouter.getInstance().addCaptureListener(recordingListener);
            recording = true;
            btnRecord.setText("\u25A0 Stop Recording");
            btnRecord.setForeground(Color.RED);
        } else {
            // Stop recording and save file
            recording = false;
            AudioRouter.getInstance().removeCaptureListener(recordingListener);
            recordingListener = null;
            byte[] pcmData;
            synchronized (this) {
                pcmData = (recordingBuffer != null) ? recordingBuffer.toByteArray() : new byte[0];
                recordingBuffer = null;
            }
            btnRecord.setText("\u25CF Record");
            btnRecord.setForeground(null);
            if (pcmData.length == 0) {
                JOptionPane.showMessageDialog(this, "No audio was captured.", "Recording", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            JFileChooser fc = new JFileChooser();
            fc.setDialogTitle("Save Recording As");
            fc.setFileFilter(new FileNameExtensionFilter("WAV audio (*.wav)", "wav"));
            fc.setSelectedFile(new File("recording.wav"));
            if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                File f = fc.getSelectedFile();
                if (!f.getName().toLowerCase().endsWith(".wav")) f = new File(f.getPath() + ".wav");
                final byte[] data = pcmData;
                final File target = f;
                new Thread(() -> {
                    try (AudioInputStream ais = new AudioInputStream(
                            new ByteArrayInputStream(data),
                            AudioRouter.FORMAT,
                            data.length / AudioRouter.FORMAT.getFrameSize())) {
                        AudioSystem.write(ais, AudioFileFormat.Type.WAVE, target);
                        SwingUtilities.invokeLater(() ->
                            JOptionPane.showMessageDialog(DigitalModesWindow.this,
                                "Saved: " + target.getAbsolutePath(), "Recording Saved",
                                JOptionPane.INFORMATION_MESSAGE));
                    } catch (IOException ex) {
                        SwingUtilities.invokeLater(() ->
                            JOptionPane.showMessageDialog(DigitalModesWindow.this,
                                "Failed to save: " + ex.getMessage(), "Recording Error",
                                JOptionPane.ERROR_MESSAGE));
                    }
                }, "DigitalModesWin-saveWav").start();
            }
        }
    }

    private void onUploadAudio() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Load Audio File for Playback");
        fc.setFileFilter(new FileNameExtensionFilter(
                "Audio files (*.wav, *.mp3, *.ogg, *.flac)", "wav", "mp3", "ogg", "flac"));
        if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;

        File file = fc.getSelectedFile();

        // Stop any running playback so the load thread can safely replace the cache
        playingFile = false;
        playbackPaused = false;

        new Thread(() -> {
            try {
                AudioInputStream rawStream = AudioSystem.getAudioInputStream(file);
                AudioFormat srcFormat = rawStream.getFormat();
                AudioInputStream pcmStream;
                if (!srcFormat.matches(AudioRouter.FORMAT)) {
                    pcmStream = AudioSystem.getAudioInputStream(AudioRouter.FORMAT, rawStream);
                } else {
                    pcmStream = rawStream;
                }

                // Read entire file into memory so it can be replayed without re-uploading
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                byte[] buf = new byte[4096];
                int read;
                while ((read = pcmStream.read(buf)) != -1) {
                    baos.write(buf, 0, read);
                }
                pcmStream.close();

                final byte[] pcmData  = baos.toByteArray();
                final String fileName = file.getName();

                SwingUtilities.invokeLater(() -> {
                    cachedAudioPcm      = pcmData;
                    cachedAudioFileName = fileName;
                    updateAudioControls();
                    startPlayback();
                });
            } catch (UnsupportedAudioFileException | IOException ex) {
                SwingUtilities.invokeLater(() ->
                    JOptionPane.showMessageDialog(DigitalModesWindow.this,
                        "Could not read audio file:\n" + ex.getMessage(),
                        "Audio Load Error", JOptionPane.ERROR_MESSAGE));
            }
        }, "DigitalModesWin-loadAudio").start();
    }

    /** Start playback from the cached PCM buffer; no-op if already playing or nothing cached. */
    private void startPlayback() {
        final byte[] pcm = cachedAudioPcm;
        if (pcm == null || playingFile) return;

        playingFile = true;
        playbackPaused = false;
        updateAudioControls();

        new Thread(() -> {
            AudioRouter router = AudioRouter.getInstance();
            try {
                do {
                    InputStream src = new ByteArrayInputStream(pcm);
                    byte[] buf = new byte[AudioRouter.FORMAT.getFrameSize() * 512]; // ~5 ms chunks
                    int read;
                    while (playingFile) {
                        if (playbackPaused) {
                            try { Thread.sleep(50); } catch (InterruptedException ie) { break; }
                            continue;
                        }
                        read = src.read(buf);
                        if (read == -1) break; // end of stream
                        if (read > 0) {
                            byte[] chunk = new byte[read];
                            System.arraycopy(buf, 0, chunk, 0, read);
                            router.play(chunk);
                            if (waterfallPanel != null) waterfallPanel.feedPcm(chunk);
                        }
                    }
                    // If still "playing" (not stopped), check loop flag
                } while (playingFile && loopPlayback && cachedAudioPcm == pcm);
            } catch (java.io.IOException e) {
                // ByteArrayInputStream.read() should never throw, but satisfy the compiler
            } finally {
                playingFile = false;
                SwingUtilities.invokeLater(() -> {
                    playbackPaused = false;
                    updateAudioControls();
                });
            }
        }, "DigitalModesWin-playback").start();
    }

    private void onTogglePlayPause() {
        if (!playingFile) {
            startPlayback();
        } else {
            playbackPaused = !playbackPaused;
            updateAudioControls();
        }
    }

    private void onResetAudio() {
        playingFile = false;
        playbackPaused = false;
        cachedAudioPcm = null;
        cachedAudioFileName = null;
        updateAudioControls();
    }

    /** Refresh the audio playback control visibility/labels to match current state. Must be on EDT. */
    private void updateAudioControls() {
        boolean hasCached = (cachedAudioPcm != null);
        boolean isPlaying = playingFile;
        boolean isPaused  = playbackPaused;

        if (hasCached) {
            lblAudioFile.setText(cachedAudioFileName);
            lblAudioFile.setForeground(UIManager.getColor("Label.foreground"));
        } else {
            lblAudioFile.setText("No file");
            lblAudioFile.setForeground(Color.GRAY);
        }

        btnPlayPause.setVisible(hasCached);
        btnPlayPause.setText((isPlaying && !isPaused) ? "|| Pause" : "\u25B6 Play");

        btnLoop.setVisible(hasCached);

        btnStopPlayback.setVisible(isPlaying);
        btnReset.setVisible(hasCached && !isPlaying);

        revalidate();
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
        currentFreqKhz = freqKhz;
        lblFrequency.setText(String.format("%.6f MHz", freqKhz / 1000.0));
        updateRigStatus(true);
    }

    private void updateSelectionLabel(int centerHz, int bwHz) {
        RxMode rxm = (waterfallPanel != null) ? waterfallPanel.getRxMode() : RxMode.USB;
        int lo, hi;
        if (rxm.isUpperSide()) {
            lo = centerHz;
            hi = centerHz + bwHz;
        } else if (rxm.isLowerSide()) {
            lo = centerHz - bwHz;
            hi = centerHz;
        } else {
            lo = centerHz - bwHz / 2;
            hi = centerHz + bwHz / 2;
        }
        lblSelectionInfo.setText(rxm.getLabel() + "  BW: " + bwHz + " Hz  |  Centre: "
                + centerHz + " Hz  (" + lo + "\u2013" + hi + " Hz)");
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
            playingFile    = false;
            playbackPaused = false;
            if (waterfallCaptureListener != null) {
                AudioRouter.getInstance().removeCaptureListener(waterfallCaptureListener);
            }
            AudioRouter.getInstance().stopCapture();
            AudioRouter.getInstance().closePlayback();
            WsjtxUdpListener.getInstance().stop();
            PttController.getInstance().closePttPort();
        }, "DigitalModesWindow-shutdown"));
    }
}
