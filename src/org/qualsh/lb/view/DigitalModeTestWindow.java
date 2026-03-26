package org.qualsh.lb.view;

import org.qualsh.lb.digital.AudioRouter;
import org.qualsh.lb.digital.Bpsk31Decoder;

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
 * Digital mode decode window – BPSK31 (PSK31).
 *
 * <h2>Audio sources</h2>
 * <ul>
 *   <li><b>Live capture</b> – select a soundcard input (e.g., the USB audio
 *       CODEC from your radio interface) and click "Start Capture".</li>
 *   <li><b>Audio file</b> – click "Load Audio File…" to load a WAV/AU/AIFF
 *       recording of a PSK31 transmission.</li>
 * </ul>
 *
 * <h2>Tuning</h2>
 * <p>Click anywhere on the waterfall to set the carrier frequency, or type a
 * value directly into the "Carrier Hz" spinner.  PSK31 signals are typically
 * found between 500 Hz and 2500 Hz in the audio passband.
 *
 * <h2>Decoder</h2>
 * <p>The built-in {@link Bpsk31Decoder} performs:
 * <ol>
 *   <li>IQ mixing to baseband at the selected carrier frequency.</li>
 *   <li>Single-pole IIR low-pass filtering (~47 Hz cutoff).</li>
 *   <li>Integrate-and-dump over one 31.25-baud symbol (1536 samples at 48 kHz).</li>
 *   <li>Differential BPSK bit decision.</li>
 *   <li>PSK31 Varicode decoding to ASCII text.</li>
 * </ol>
 */
public class DigitalModeTestWindow extends JFrame {

    private static final long serialVersionUID = 2L;

    /** Default PSK31 carrier frequency (Hz). */
    private static final int DEFAULT_CARRIER_HZ = 1000;
    /** Waterfall passband width for PSK31 (Hz). */
    private static final int BPSK31_BW_HZ = 100;

    // ── UI components ──────────────────────────────────────────────────────────

    private final JComboBox<String> audioInCombo;
    private final JButton           startStopBtn;
    private final JButton           loadFileBtn;
    private final JSpinner          carrierSpinner;

    private final SpectrumWaterfallPanel waterfallPanel;

    private final JTextArea rxOutput;
    private final JLabel    statusLabel;

    // ── State ─────────────────────────────────────────────────────────────────

    private boolean capturing    = false;
    private Thread  filePlayThread = null;

    /** Stored reference so we can un-register the same instance from AudioRouter. */
    private final java.util.function.Consumer<byte[]> pcmListener = this::feedPcm;

    // ── Decoder ───────────────────────────────────────────────────────────────

    private final Bpsk31Decoder decoder;

    // ── Construction ──────────────────────────────────────────────────────────

    public DigitalModeTestWindow(Frame owner) {
        super("BPSK31 Decoder");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setPreferredSize(new Dimension(1000, 680));
        setMinimumSize(new Dimension(800, 520));

        // ── Decoder ────────────────────────────────────────────────────────────
        decoder = new Bpsk31Decoder(this::appendRx);

        // ── Toolbar ────────────────────────────────────────────────────────────

        audioInCombo = new JComboBox<>(AudioRouter.availableCaptureDevices());
        if (audioInCombo.getItemCount() == 0) audioInCombo.addItem("(no devices found)");
        audioInCombo.insertItemAt("System Default", 0);
        audioInCombo.setSelectedIndex(0);

        startStopBtn = new JButton("▶  Start Capture");
        loadFileBtn  = new JButton("Load Audio File…");

        SpinnerNumberModel carrierModel = new SpinnerNumberModel(
                DEFAULT_CARRIER_HZ, 100, 3500, 10);
        carrierSpinner = new JSpinner(carrierModel);
        carrierSpinner.setPreferredSize(new Dimension(80, carrierSpinner.getPreferredSize().height));
        carrierSpinner.setToolTipText(
                "PSK31 carrier frequency in Hz (click waterfall to tune)");

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        toolbar.add(new JLabel("Mode: BPSK31"));
        toolbar.add(new JSeparator(SwingConstants.VERTICAL));
        toolbar.add(new JLabel("Audio In:"));
        toolbar.add(audioInCombo);
        toolbar.add(startStopBtn);
        toolbar.add(new JSeparator(SwingConstants.VERTICAL));
        toolbar.add(loadFileBtn);
        toolbar.add(new JSeparator(SwingConstants.VERTICAL));
        toolbar.add(new JLabel("Carrier Hz:"));
        toolbar.add(carrierSpinner);

        // ── Waterfall ──────────────────────────────────────────────────────────

        waterfallPanel = new SpectrumWaterfallPanel();
        waterfallPanel.setPreferredSize(new Dimension(500, 280));
        waterfallPanel.setMinimumSize(new Dimension(200, 140));

        // Set symmetric passband centred at the carrier (appropriate for BPSK31)
        waterfallPanel.setRxMode(org.qualsh.lb.digital.RxMode.CW);
        waterfallPanel.setSelection(DEFAULT_CARRIER_HZ, BPSK31_BW_HZ);

        // Forward bandpass-filtered audio to the decoder
        waterfallPanel.setFilteredOutputListener(decoder::feed);

        // Keep decoder tuned to whatever the user selects on the waterfall
        waterfallPanel.setSelectionListener((centerHz, bwHz) -> {
            decoder.setCarrierHz(centerHz);
            SwingUtilities.invokeLater(() ->
                    carrierSpinner.setValue(centerHz));
        });

        JPanel waterfallWrapper = new JPanel(new BorderLayout());
        waterfallWrapper.setBorder(new TitledBorder("Spectrum / Waterfall — click to tune"));
        waterfallWrapper.add(waterfallPanel, BorderLayout.CENTER);

        // ── RX pane ────────────────────────────────────────────────────────────

        rxOutput = new JTextArea();
        rxOutput.setEditable(false);
        rxOutput.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        rxOutput.setLineWrap(true);
        rxOutput.setWrapStyleWord(false);
        rxOutput.setText("");
        JScrollPane rxScroll = new JScrollPane(rxOutput);
        rxScroll.setPreferredSize(new Dimension(420, 280));

        JPanel clearRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 2));
        JButton clearBtn = new JButton("Clear");
        clearBtn.addActionListener(e -> {
            rxOutput.setText("");
            decoder.reset();
        });
        clearRow.add(clearBtn);

        JPanel rxWrapper = new JPanel(new BorderLayout());
        rxWrapper.setBorder(new TitledBorder("RX — Decoded BPSK31 Text"));
        rxWrapper.add(rxScroll,  BorderLayout.CENTER);
        rxWrapper.add(clearRow, BorderLayout.SOUTH);

        // ── Centre split: waterfall | RX ──────────────────────────────────────

        JSplitPane centreSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                waterfallWrapper, rxWrapper);
        centreSplit.setResizeWeight(0.55);
        centreSplit.setBorder(new EmptyBorder(0, 0, 0, 0));

        // ── Status bar ─────────────────────────────────────────────────────────

        statusLabel = new JLabel("Ready — load an audio file or start live capture");
        statusLabel.setBorder(new EmptyBorder(2, 6, 2, 6));

        // ── Root layout ────────────────────────────────────────────────────────

        JPanel root = new JPanel(new BorderLayout(4, 4));
        root.setBorder(new EmptyBorder(4, 4, 4, 4));
        root.add(toolbar,     BorderLayout.NORTH);
        root.add(centreSplit, BorderLayout.CENTER);

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

        carrierSpinner.addChangeListener(e -> {
            int hz = ((Number) carrierSpinner.getValue()).intValue();
            decoder.setCarrierHz(hz);
            waterfallPanel.setSelection(hz, BPSK31_BW_HZ);
        });

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

        decoder.reset();
        capturing = true;
        startStopBtn.setText("■  Stop Capture");
        audioInCombo.setEnabled(false);
        loadFileBtn.setEnabled(false);
        setStatus("Capturing: " + (deviceName != null ? deviceName : "System Default")
                + "  |  Carrier: " + decoder.getCarrierHz() + " Hz");
    }

    private void stopCapture() {
        AudioRouter ar = AudioRouter.getInstance();
        ar.removeCaptureListener(pcmListener);
        ar.stopCapture();

        capturing = false;
        startStopBtn.setText("▶  Start Capture");
        audioInCombo.setEnabled(true);
        loadFileBtn.setEnabled(true);
        setStatus("Capture stopped");
    }

    // ── File playback ─────────────────────────────────────────────────────────

    private void chooseAndPlayFile() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Load PSK31 Audio File");
        fc.setFileFilter(new FileNameExtensionFilter(
                "Audio files (*.wav, *.au, *.aiff)", "wav", "au", "aiff"));
        if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;

        File f = fc.getSelectedFile();
        stopFilePlay();
        decoder.reset();

        filePlayThread = new Thread(() -> playFile(f), "DigitalModeTest-fileplay");
        filePlayThread.setDaemon(true);
        filePlayThread.start();
    }

    private void playFile(File f) {
        SwingUtilities.invokeLater(() -> {
            loadFileBtn.setEnabled(false);
            startStopBtn.setEnabled(false);
            setStatus("Decoding file: " + f.getName()
                    + "  |  Carrier: " + decoder.getCarrierHz() + " Hz");
        });

        try (AudioInputStream raw = AudioSystem.getAudioInputStream(f)) {
            AudioFormat srcFmt = raw.getFormat();
            AudioFormat tgtFmt = AudioRouter.FORMAT;

            AudioInputStream converted = AudioSystem.isConversionSupported(tgtFmt, srcFmt)
                    ? AudioSystem.getAudioInputStream(tgtFmt, raw)
                    : raw;

            byte[] buf = new byte[4096];
            int read;
            while ((read = converted.read(buf)) > 0
                    && !Thread.currentThread().isInterrupted()) {
                byte[] chunk = new byte[read];
                System.arraycopy(buf, 0, chunk, 0, read);
                feedPcm(chunk);
                // Throttle to approximate real-time so the waterfall scrolls visibly
                long sleepMs = (long) (chunk.length * 1000.0
                        / (tgtFmt.getSampleRate() * tgtFmt.getFrameSize()));
                if (sleepMs > 0) {
                    Thread.sleep(sleepMs);
                }
            }
        } catch (UnsupportedAudioFileException e) {
            SwingUtilities.invokeLater(() ->
                    JOptionPane.showMessageDialog(this,
                            "Unsupported audio format:\n" + e.getMessage(),
                            "File Error", JOptionPane.ERROR_MESSAGE));
        } catch (IOException | InterruptedException e) {
            // Interrupted = user stopped playback or window closed – expected
        } finally {
            SwingUtilities.invokeLater(() -> {
                loadFileBtn.setEnabled(true);
                startStopBtn.setEnabled(true);
                setStatus("File decode finished");
            });
        }
    }

    private void stopFilePlay() {
        if (filePlayThread != null && filePlayThread.isAlive()) {
            filePlayThread.interrupt();
            try { filePlayThread.join(500); } catch (InterruptedException ignored) {}
            filePlayThread = null;
        }
    }

    // ── PCM pipeline ──────────────────────────────────────────────────────────

    /**
     * Entry point for all raw PCM audio (48 kHz / 16-bit / mono).
     * Feeds the waterfall display; the waterfall then forwards bandpass-filtered
     * audio to the decoder via its {@code filteredOutputListener}.
     */
    private void feedPcm(byte[] pcm) {
        waterfallPanel.feedPcm(pcm);
    }

    // ── Decoded text output ───────────────────────────────────────────────────

    /**
     * Append decoded text to the RX pane.  Safe to call from any thread.
     */
    public void appendRx(String text) {
        SwingUtilities.invokeLater(() -> {
            rxOutput.append(text);
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
