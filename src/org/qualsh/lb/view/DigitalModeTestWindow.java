package org.qualsh.lb.view;

import org.qualsh.lb.digital.AudioRouter;
import org.qualsh.lb.digital.DigitalMode;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;

/**
 * DigitalModeTestWindow – prototype workbench for developing and testing
 * digital mode encoding/decoding from a live or file audio source.
 *
 * <h2>Purpose</h2>
 * <p>This window is the foundation for a full-fledged multi-mode digital
 * mode operator.  Right now it provides:
 * <ul>
 *   <li>Audio source selection: live capture device or WAV file</li>
 *   <li>Mode selection (FT8, FT4, PSK31, RTTY, …)</li>
 *   <li>Real-time spectrum / waterfall display</li>
 *   <li>RX decoded-output pane (to be wired to a real decoder)</li>
 *   <li>TX compose pane (to be wired to a real encoder)</li>
 * </ul>
 *
 * <h2>Layout</h2>
 * <pre>
 * ┌──────────────────────────────────────────────────────────────────┐
 * │  [Mode ▼]  [Audio In ▼]  [▶ Start Capture]  [Load File…]        │ ← toolbar
 * ├─────────────────────────────┬────────────────────────────────────┤
 * │  Spectrum / Waterfall       │  RX — Decoded Output               │
 * │  (live FFT from audio in)   │  (scrolling text; hook decoder in) │
 * │                             │                                    │
 * ├─────────────────────────────┴────────────────────────────────────┤
 * │  TX — Compose                                  [Encode & Send]   │
 * ├──────────────────────────────────────────────────────────────────┤
 * │  Status bar                                                      │
 * └──────────────────────────────────────────────────────────────────┘
 * </pre>
 */
public class DigitalModeTestWindow extends JFrame {

    private static final long serialVersionUID = 1L;

    // ── UI components ──────────────────────────────────────────────────────────

    private final JComboBox<DigitalMode> modeCombo;
    private final JComboBox<String>      audioInCombo;
    private final JButton                startStopBtn;
    private final JButton                loadFileBtn;

    private final SpectrumWaterfallPanel waterfallPanel;

    private final JTextArea rxOutput;
    private final JTextArea txCompose;
    private final JButton   encodeSendBtn;

    private final JLabel statusLabel;

    // ── State ─────────────────────────────────────────────────────────────────

    private boolean capturing = false;
    private Thread  filePlayThread = null;
    /** Stored reference so we can un-register the same instance from AudioRouter. */
    private final java.util.function.Consumer<byte[]> pcmListener = this::feedPcm;

    // ── Construction ──────────────────────────────────────────────────────────

    public DigitalModeTestWindow(Frame owner) {
        super("Digital Mode Test");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setPreferredSize(new Dimension(1000, 700));
        setMinimumSize(new Dimension(800, 550));

        // ── Mode + audio toolbar ───────────────────────────────────────────────

        modeCombo   = new JComboBox<>(DigitalMode.values());
        audioInCombo = new JComboBox<>(AudioRouter.availableCaptureDevices());
        if (audioInCombo.getItemCount() == 0) audioInCombo.addItem("(no devices found)");
        audioInCombo.insertItemAt("System Default", 0);
        audioInCombo.setSelectedIndex(0);

        startStopBtn = new JButton("▶  Start Capture");
        loadFileBtn  = new JButton("Load Audio File…");

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        toolbar.add(new JLabel("Mode:"));
        toolbar.add(modeCombo);
        toolbar.add(new JLabel("Audio In:"));
        toolbar.add(audioInCombo);
        toolbar.add(startStopBtn);
        toolbar.add(new JSeparator(SwingConstants.VERTICAL));
        toolbar.add(loadFileBtn);

        // ── Waterfall ──────────────────────────────────────────────────────────

        waterfallPanel = new SpectrumWaterfallPanel();
        waterfallPanel.setPreferredSize(new Dimension(500, 300));
        waterfallPanel.setMinimumSize(new Dimension(200, 150));
        JPanel waterfallWrapper = new JPanel(new BorderLayout());
        waterfallWrapper.setBorder(new TitledBorder("Spectrum / Waterfall"));
        waterfallWrapper.add(waterfallPanel, BorderLayout.CENTER);

        // ── RX pane ────────────────────────────────────────────────────────────

        rxOutput = new JTextArea();
        rxOutput.setEditable(false);
        rxOutput.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        rxOutput.setLineWrap(true);
        rxOutput.setWrapStyleWord(false);
        rxOutput.setText("— RX decoder not yet connected —\n" +
                "Wire your decoder's output to appendRx() on this panel.\n");
        JScrollPane rxScroll = new JScrollPane(rxOutput);
        rxScroll.setPreferredSize(new Dimension(400, 300));
        JPanel rxWrapper = new JPanel(new BorderLayout());
        rxWrapper.setBorder(new TitledBorder("RX — Decoded Output"));
        rxWrapper.add(rxScroll, BorderLayout.CENTER);

        // ── Centre split: waterfall | RX ──────────────────────────────────────

        JSplitPane centreSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                waterfallWrapper, rxWrapper);
        centreSplit.setResizeWeight(0.55);
        centreSplit.setBorder(new EmptyBorder(0, 0, 0, 0));

        // ── TX pane ────────────────────────────────────────────────────────────

        txCompose = new JTextArea(3, 40);
        txCompose.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        txCompose.setLineWrap(true);
        txCompose.setWrapStyleWord(true);
        txCompose.setToolTipText("Type your message here.  Encoding is not yet implemented.");
        JScrollPane txScroll = new JScrollPane(txCompose);

        encodeSendBtn = new JButton("Encode & Send");
        encodeSendBtn.setEnabled(false); // not yet implemented
        encodeSendBtn.setToolTipText("TX encoding not yet implemented");

        JPanel txBottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 4));
        txBottom.add(new JLabel("(TX encoder not yet implemented)"));
        txBottom.add(encodeSendBtn);

        JPanel txWrapper = new JPanel(new BorderLayout(4, 4));
        txWrapper.setBorder(new TitledBorder("TX — Compose"));
        txWrapper.add(txScroll, BorderLayout.CENTER);
        txWrapper.add(txBottom, BorderLayout.SOUTH);

        // ── Status bar ─────────────────────────────────────────────────────────

        statusLabel = new JLabel("Ready");
        statusLabel.setBorder(new EmptyBorder(2, 6, 2, 6));

        // ── Root layout ────────────────────────────────────────────────────────

        JPanel root = new JPanel(new BorderLayout(4, 4));
        root.setBorder(new EmptyBorder(4, 4, 4, 4));
        root.add(toolbar,    BorderLayout.NORTH);
        root.add(centreSplit, BorderLayout.CENTER);
        root.add(txWrapper,  BorderLayout.SOUTH);

        JPanel statusBar = new JPanel(new BorderLayout());
        statusBar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0,
                UIManager.getColor("Separator.foreground")));
        statusBar.add(statusLabel, BorderLayout.WEST);

        setLayout(new BorderLayout());
        add(root,      BorderLayout.CENTER);
        add(statusBar, BorderLayout.SOUTH);

        // ── Wire actions ──────────────────────────────────────────────────────

        startStopBtn.addActionListener(e -> toggleCapture());
        loadFileBtn.addActionListener(e -> chooseAndPlayFile());

        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) { shutdown(); }
        });

        pack();
        setLocationRelativeTo(owner);
    }

    // ── Capture control ───────────────────────────────────────────────────────

    private void toggleCapture() {
        if (capturing) {
            stopCapture();
        } else {
            startCapture();
        }
    }

    private void startCapture() {
        String deviceName = audioInCombo.getSelectedIndex() == 0
                ? null
                : (String) audioInCombo.getSelectedItem();

        AudioRouter ar = AudioRouter.getInstance();
        ar.addCaptureListener(pcmListener);

        boolean ok = ar.startCapture(deviceName);
        if (!ok) {
            JOptionPane.showMessageDialog(this,
                    "Could not open audio device:\n" + audioInCombo.getSelectedItem(),
                    "Audio Error", JOptionPane.ERROR_MESSAGE);
            ar.removeCaptureListener(pcmListener);
            return;
        }

        capturing = true;
        startStopBtn.setText("■  Stop Capture");
        audioInCombo.setEnabled(false);
        setStatus("Capturing from: " + (deviceName != null ? deviceName : "System Default"));
    }

    private void stopCapture() {
        AudioRouter ar = AudioRouter.getInstance();
        ar.removeCaptureListener(pcmListener);
        ar.stopCapture();

        capturing = false;
        startStopBtn.setText("▶  Start Capture");
        audioInCombo.setEnabled(true);
        setStatus("Capture stopped");
    }

    // ── File playback ─────────────────────────────────────────────────────────

    private void chooseAndPlayFile() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Load Audio File");
        fc.setFileFilter(new FileNameExtensionFilter(
                "Audio files (*.wav, *.au, *.aiff)", "wav", "au", "aiff"));
        if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;

        File f = fc.getSelectedFile();
        stopFilePlay();

        filePlayThread = new Thread(() -> playFile(f), "DigitalModeTest-fileplay");
        filePlayThread.setDaemon(true);
        filePlayThread.start();
    }

    private void playFile(File f) {
        SwingUtilities.invokeLater(() -> {
            loadFileBtn.setEnabled(false);
            setStatus("Playing file: " + f.getName());
        });

        try (AudioInputStream raw = AudioSystem.getAudioInputStream(f)) {
            AudioFormat srcFmt = raw.getFormat();
            AudioFormat tgtFmt = AudioRouter.FORMAT;

            AudioInputStream converted = AudioSystem.isConversionSupported(tgtFmt, srcFmt)
                    ? AudioSystem.getAudioInputStream(tgtFmt, raw)
                    : raw; // best-effort if conversion not available

            byte[] buf = new byte[2048];
            int read;
            while ((read = converted.read(buf)) > 0 && !Thread.currentThread().isInterrupted()) {
                byte[] chunk = new byte[read];
                System.arraycopy(buf, 0, chunk, 0, read);
                feedPcm(chunk);
                // throttle to approximate real-time playback
                long sleepMs = (long) (chunk.length * 1000.0
                        / (tgtFmt.getSampleRate() * tgtFmt.getFrameSize()));
                if (sleepMs > 0) Thread.sleep(sleepMs);
            }
        } catch (UnsupportedAudioFileException e) {
            SwingUtilities.invokeLater(() ->
                    JOptionPane.showMessageDialog(this,
                            "Unsupported audio format:\n" + e.getMessage(),
                            "File Error", JOptionPane.ERROR_MESSAGE));
        } catch (IOException | InterruptedException e) {
            // interrupted = stopped intentionally
        } finally {
            SwingUtilities.invokeLater(() -> {
                loadFileBtn.setEnabled(true);
                setStatus("File playback finished");
            });
        }
    }

    private void stopFilePlay() {
        if (filePlayThread != null && filePlayThread.isAlive()) {
            filePlayThread.interrupt();
            filePlayThread = null;
        }
    }

    // ── PCM pipeline ──────────────────────────────────────────────────────────

    /**
     * Entry point for raw PCM audio (48 kHz / 16-bit / mono).
     * Currently feeds the waterfall display.
     * Wire additional consumers here as decoders are developed.
     *
     * @param pcm raw PCM bytes in {@link AudioRouter#FORMAT}
     */
    private void feedPcm(byte[] pcm) {
        // 1. Feed the spectrum / waterfall
        waterfallPanel.feedPcm(pcm);

        // 2. TODO: feed decoder(s) – e.g. FT8, PSK31, RTTY, …
        //    decoderChain.feed(pcm);
    }

    // ── Public API for external decoder wiring ────────────────────────────────

    /**
     * Append a decoded line to the RX output pane.
     * Safe to call from any thread.
     *
     * @param line decoded text to display
     */
    public void appendRx(String line) {
        SwingUtilities.invokeLater(() -> {
            rxOutput.append(line + "\n");
            rxOutput.setCaretPosition(rxOutput.getDocument().getLength());
        });
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    private void shutdown() {
        stopFilePlay();
        if (capturing) stopCapture();
    }

    // ── Utilities ─────────────────────────────────────────────────────────────

    private void setStatus(String msg) {
        SwingUtilities.invokeLater(() -> statusLabel.setText(msg));
    }
}
