package org.qualsh.lb.digitalmodes;

import org.qualsh.lb.digital.DigitalMode;
import org.qualsh.lb.digital.decode.DecodeResult;
import org.qualsh.lb.digitalmodes.audio.AudioBuffer;
import org.qualsh.lb.digitalmodes.audio.PlaybackController;
import org.qualsh.lb.digitalmodes.audio.RigAudioSource;
import org.qualsh.lb.digitalmodes.audio.WavFileSource;
import org.qualsh.lb.digitalmodes.decoder.Bpsk31Decoder;
import org.qualsh.lb.digitalmodes.decoder.Decoder;
import org.qualsh.lb.digitalmodes.decoder.Ft8Decoder;
import org.qualsh.lb.digitalmodes.decoder.MfskDecoder;
import org.qualsh.lb.digitalmodes.decoder.OliviaDecoder;
import org.qualsh.lb.digitalmodes.decoder.PacketDecoder;
import org.qualsh.lb.digitalmodes.decoder.RttyDecoder;
import org.qualsh.lb.digitalmodes.decoder.WsprDecoder;
import org.qualsh.lb.digitalmodes.spectrum.FFTPanel;
import org.qualsh.lb.digitalmodes.spectrum.FrequencySelector;
import org.qualsh.lb.digitalmodes.spectrum.WaterfallPanel;
import org.qualsh.lb.digitalmodes.view.AudioControlPanel;
import org.qualsh.lb.digitalmodes.view.DecodeLogModel;
import org.qualsh.lb.digitalmodes.view.DecodeLogTable;
import org.qualsh.lb.digitalmodes.view.DecodeTextArea;
import org.qualsh.lb.digitalmodes.view.EncodePanel;
import org.qualsh.lb.digitalmodes.view.ModeSelectionPanel;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The Digital Modes window where you decode and encode amateur-radio digital signals.
 *
 * <p>Open this window from the main Logbook menu. It contains a live frequency spectrum and
 * waterfall display, a mode selector, audio transport controls, a scrolling decode output
 * area, a decode history log, and a message compose panel. You can continue using the main
 * logbook while this window is open — it runs independently.
 *
 * <p>Call {@link #openWindow()} to show the window and {@link #closeWindow()} to hide it and
 * stop all background decoding. Connect a radio rig via {@link #setRigAudioSource(RigAudioSource)}
 * to stream live audio directly into the decoder.
 *
 * @author Logbook Development Team
 * @version 1.0
 */
public class DigitalModesWindow extends JDialog {

    private static final long serialVersionUID = 1L;

    private static final String WINDOW_TITLE      = "Digital Modes";
    private static final int    DECODE_INTERVAL_MS = 500;

    // -------------------------------------------------------------------------
    // Sub-panels
    // -------------------------------------------------------------------------

    private FFTPanel           fftPanel;
    private WaterfallPanel     waterfallPanel;
    private FrequencySelector  frequencySelector;
    private AudioControlPanel  audioControlPanel;
    private ModeSelectionPanel modeSelectionPanel;
    private DecodeTextArea     decodeTextArea;
    private DecodeLogTable     decodeLogTable;
    private DecodeLogModel     decodeLogModel;
    private EncodePanel        encodePanel;
    private JTabbedPane        mainTabbedPane;
    private JPanel             spectrumPanel;
    private JPanel             decodeOutputPanel;

    // -------------------------------------------------------------------------
    // Audio infrastructure
    // -------------------------------------------------------------------------

    private AudioBuffer        sharedBuffer;
    private PlaybackController playbackController;
    private WavFileSource      wavFileSource;
    private RigAudioSource     rigAudioSource;

    // -------------------------------------------------------------------------
    // Decode infrastructure
    // -------------------------------------------------------------------------

    private Map<DigitalMode, Decoder> decoders;
    private Timer                     decodeTimer;
    private DigitalMode               currentMode;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    /**
     * Creates a new Digital Modes window attached to the given parent frame.
     *
     * <p>The window is sized to fit the screen and centred on it. Call {@link #openWindow()}
     * to make it visible.
     *
     * @param parent the owner frame; must not be {@code null}
     */
    public DigitalModesWindow(JFrame parent) {
        super(parent, WINDOW_TITLE, false);

        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        int w = (int) (screen.getWidth()  * 0.75);
        int h = (int) (screen.getHeight() * 0.85);
        setSize(w, h);

        int x = (int) ((screen.getWidth()  - w) / 2);
        int y = (int) ((screen.getHeight() - h) / 2);
        setLocation(x, y);

        setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);

        initComponents();
        initLayout();
        initListeners();
        initDecoders();
        initDecodeTimer();

        currentMode = modeSelectionPanel.getSelectedMode();
    }

    // -------------------------------------------------------------------------
    // Initialisation methods
    // -------------------------------------------------------------------------

    /**
     * Instantiates all sub-components and shared infrastructure objects.
     */
    private void initComponents() {
        sharedBuffer       = new AudioBuffer();
        wavFileSource      = new WavFileSource();
        playbackController = new PlaybackController(sharedBuffer);

        decodeLogModel = new DecodeLogModel();
        decodeLogTable = new DecodeLogTable(decodeLogModel);

        fftPanel = new FFTPanel();
        fftPanel.setBuffer(sharedBuffer);
        fftPanel.setPlaybackController(playbackController);

        waterfallPanel = new WaterfallPanel();
        waterfallPanel.setBuffer(sharedBuffer);
        waterfallPanel.setPlaybackController(playbackController);

        frequencySelector  = new FrequencySelector();
        audioControlPanel  = new AudioControlPanel();
        modeSelectionPanel = new ModeSelectionPanel();
        decodeTextArea     = new DecodeTextArea();
        encodePanel        = new EncodePanel();
        mainTabbedPane     = new JTabbedPane();
    }

    /**
     * Assembles all sub-components into the window's content pane.
     *
     * <p>Layout:
     * <ul>
     *   <li><strong>NORTH</strong> — a spectrum panel stacking a
     *       {@link JLayeredPane} (FFT + transparent frequency selector) above
     *       the waterfall display.</li>
     *   <li><strong>CENTER</strong> — a {@link JTabbedPane} with a
     *       <em>Decode</em> tab (split between the live decode text area and
     *       the decode history table) and an <em>Encode</em> tab.</li>
     *   <li><strong>SOUTH</strong> — a row containing the audio controls on
     *       the left and the mode selector on the right.</li>
     * </ul>
     */
    private void initLayout() {
        Container contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());

        // --- NORTH: spectrum area ---
        spectrumPanel = new JPanel(new BorderLayout());

        // Layered pane: fftPanel on the bottom, frequencySelector overlaid
        JLayeredPane layeredPane = new JLayeredPane() {
            @Override
            public void doLayout() {
                int lw = getWidth();
                int lh = getHeight();
                for (Component c : getComponents()) {
                    c.setBounds(0, 0, lw, lh);
                }
            }

            @Override
            public Dimension getPreferredSize() {
                return fftPanel.getPreferredSize();
            }
        };
        layeredPane.add(fftPanel,          JLayeredPane.DEFAULT_LAYER);
        layeredPane.add(frequencySelector, JLayeredPane.PALETTE_LAYER);

        spectrumPanel.add(layeredPane,    BorderLayout.NORTH);
        spectrumPanel.add(waterfallPanel, BorderLayout.CENTER);
        contentPane.add(spectrumPanel, BorderLayout.NORTH);

        // --- CENTER: tabbed pane ---

        // Decode tab
        JScrollPane decodeTextScroll = new JScrollPane(decodeTextArea);
        decodeTextScroll.setPreferredSize(new Dimension(decodeTextScroll.getPreferredSize().width, 180));

        JScrollPane decodeTableScroll = new JScrollPane(decodeLogTable);

        JSplitPane decodeSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                decodeTextScroll, decodeTableScroll);
        decodeSplit.setDividerLocation(200);
        decodeSplit.setResizeWeight(0.3);

        // Encode tab
        JScrollPane encodeScroll = new JScrollPane(encodePanel);

        mainTabbedPane.addTab("Decode", decodeSplit);
        mainTabbedPane.addTab("Encode", encodeScroll);
        contentPane.add(mainTabbedPane, BorderLayout.CENTER);

        // --- SOUTH: controls ---
        JPanel southPanel = new JPanel(new BorderLayout());
        southPanel.add(audioControlPanel,  BorderLayout.WEST);
        southPanel.add(modeSelectionPanel, BorderLayout.EAST);
        contentPane.add(southPanel, BorderLayout.SOUTH);
    }

    /**
     * Wires all inter-component event listeners.
     *
     * <p>Connections established:
     * <ul>
     *   <li>Audio buffer changes refresh the spectrum panels, stop playback,
     *       and log a status message.</li>
     *   <li>Frequency selector changes are echoed to the decode text area.</li>
     *   <li>Mode selection changes update the frequency markers, the shared
     *       buffer's associated mode, and the active decoder.</li>
     *   <li>Audio control button clicks are forwarded to the appropriate
     *       audio infrastructure objects.</li>
     *   <li>Decode log row selections display the full decoded text as a
     *       tooltip on the table.</li>
     * </ul>
     */
    private void initListeners() {

        // --- Shared buffer listener ---
        sharedBuffer.addListener(buffer -> {
            decodeTextArea.appendText("--- Audio buffer updated ---\n");
            playbackController.stop();
        });

        // --- Frequency selector listener ---
        frequencySelector.addListener(new FrequencySelector.FrequencySelectorListener() {
            @Override
            public void onCenterFrequencyChanged(double hz) {
                decodeTextArea.appendText("Center frequency: " + String.format("%.1f", hz) + " Hz\n");
            }

            @Override
            public void onBandwidthChanged(double hz) {
                decodeTextArea.appendText("Bandwidth: " + String.format("%.1f", hz) + " Hz\n");
            }
        });

        // --- Mode selection listener ---
        modeSelectionPanel.setModeChangeListener(mode -> {
            currentMode = mode;
            frequencySelector.applyModeProfile(mode);
            sharedBuffer.setAssociatedMode(mode);
            String displayName = mode.getName() != null ? mode.getName() : mode.getAbbreviation();
            decodeTextArea.appendText("Mode changed to: " + displayName + "\n");
        });

        // --- Audio control listener ---
        audioControlPanel.setAudioControlListener(new AudioControlPanel.AudioControlListener() {

            @Override
            public void onFileUploaded(File file) {
                decodeTextArea.appendText("--- Loading: " + file.getName() + " ---\n");
                SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
                    @Override
                    protected Void doInBackground() throws Exception {
                        wavFileSource.loadFile(file);
                        return null;
                    }

                    @Override
                    protected void done() {
                        try {
                            get();
                            AudioBuffer fb = wavFileSource.getBuffer();
                            sharedBuffer.load(fb.getSamples(), fb.getSampleRate());
                            decodeTextArea.appendText("--- File loaded: " + file.getName() + " ---\n");
                            audioControlPanel.getStatusLabel().setText(file.getName());
                        } catch (Exception ex) {
                            decodeTextArea.appendText("--- Load error: " + ex.getMessage() + " ---\n");
                        }
                    }
                };
                worker.execute();
            }

            @Override
            public void onRecordStarted(File outputFile) {
                try {
                    wavFileSource.startRecording(outputFile);
                    decodeTextArea.appendText("--- Recording started ---\n");
                } catch (Exception ex) {
                    decodeTextArea.appendText("--- Record error: " + ex.getMessage() + " ---\n");
                }
            }

            @Override
            public void onRecordStopped() {
                SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
                    @Override
                    protected Void doInBackground() throws Exception {
                        wavFileSource.stopRecording();
                        return null;
                    }

                    @Override
                    protected void done() {
                        try {
                            get();
                            AudioBuffer fb = wavFileSource.getBuffer();
                            sharedBuffer.load(fb.getSamples(), fb.getSampleRate());
                            decodeTextArea.appendText("--- Recording loaded ---\n");
                        } catch (Exception ex) {
                            decodeTextArea.appendText("--- Stop record error: " + ex.getMessage() + " ---\n");
                        }
                    }
                };
                worker.execute();
            }

            @Override
            public void onPlayClicked() {
                playbackController.play();
            }

            @Override
            public void onPauseClicked() {
                playbackController.pause();
            }

            @Override
            public void onStopClicked() {
                playbackController.stop();
            }

            @Override
            public void onLoopToggled(boolean looping) {
                playbackController.setLooping(looping);
            }
        });

        // --- Decode log table selection listener ---
        decodeLogTable.setDecodeResultSelectionListener(result -> {
            String tooltip = "<html><b>" + result.getCallsign() + "</b> — " + result.getMessage() + "</html>";
            decodeLogTable.setToolTipText(tooltip);
        });
    }

    /**
     * Instantiates one decoder for each supported digital mode and registers
     * them in the {@link #decoders} map, keyed by the decoder's associated
     * {@link DigitalMode}.
     */
    private void initDecoders() {
        decoders = new LinkedHashMap<>();

        Ft8Decoder    ft8    = new Ft8Decoder();
        WsprDecoder   wspr   = new WsprDecoder();
        Bpsk31Decoder bpsk   = new Bpsk31Decoder();
        RttyDecoder   rtty   = new RttyDecoder();
        MfskDecoder   mfsk   = new MfskDecoder();
        OliviaDecoder olivia = new OliviaDecoder();
        PacketDecoder packet = new PacketDecoder();

        decoders.put(ft8.getMode(),    new Decoder() {
            public List<DecodeResult> decode(AudioBuffer b) { return ft8.decode(b);    }
            public DigitalMode getMode()                    { return ft8.getMode();    }
        });
        decoders.put(wspr.getMode(),   new Decoder() {
            public List<DecodeResult> decode(AudioBuffer b) { return wspr.decode(b);   }
            public DigitalMode getMode()                    { return wspr.getMode();   }
        });
        decoders.put(bpsk.getMode(),   new Decoder() {
            public List<DecodeResult> decode(AudioBuffer b) { return bpsk.decode(b);   }
            public DigitalMode getMode()                    { return bpsk.getMode();   }
        });
        decoders.put(rtty.getMode(),   new Decoder() {
            public List<DecodeResult> decode(AudioBuffer b) { return rtty.decode(b);   }
            public DigitalMode getMode()                    { return rtty.getMode();   }
        });
        decoders.put(mfsk.getMode(),   new Decoder() {
            public List<DecodeResult> decode(AudioBuffer b) { return mfsk.decode(b);   }
            public DigitalMode getMode()                    { return mfsk.getMode();   }
        });
        decoders.put(olivia.getMode(), new Decoder() {
            public List<DecodeResult> decode(AudioBuffer b) { return olivia.decode(b); }
            public DigitalMode getMode()                    { return olivia.getMode(); }
        });
        decoders.put(packet.getMode(), new Decoder() {
            public List<DecodeResult> decode(AudioBuffer b) { return packet.decode(b); }
            public DigitalMode getMode()                    { return packet.getMode(); }
        });
    }

    /**
     * Creates and starts the periodic decode timer.
     *
     * <p>The timer fires every {@value #DECODE_INTERVAL_MS} milliseconds and
     * calls {@link #runDecode()} to attempt a decode of the current audio buffer
     * content using the active decoder.
     */
    private void initDecodeTimer() {
        decodeTimer = new Timer(DECODE_INTERVAL_MS, e -> runDecode());
        decodeTimer.start();
    }

    // -------------------------------------------------------------------------
    // Decode loop
    // -------------------------------------------------------------------------

    /**
     * Runs a single decode pass against the current audio buffer.
     *
     * <p>If the shared buffer is empty, or no decoder is registered for the
     * currently selected mode, this method returns immediately. Otherwise the
     * decode is performed asynchronously via a {@link SwingWorker}, and each
     * {@link DecodeResult} in the result list is appended to both the live
     * text area and the history table on the EDT.
     */
    private void runDecode() {
        if (sharedBuffer.isEmpty()) {
            return;
        }
        Decoder decoder = findDecoder();
        if (decoder == null) {
            return;
        }

        SwingWorker<List<DecodeResult>, Void> worker =
                new SwingWorker<List<DecodeResult>, Void>() {
            @Override
            protected List<DecodeResult> doInBackground() {
                return decoder.decode(sharedBuffer);
            }

            @Override
            protected void done() {
                try {
                    List<DecodeResult> results = get();
                    for (DecodeResult result : results) {
                        decodeTextArea.appendDecodeResult(result);
                        decodeLogTable.addDecodeResult(result);
                    }
                } catch (Exception ex) {
                    // Decode errors are non-fatal; silently discard.
                }
            }
        };
        worker.execute();
    }

    /**
     * Finds the decoder whose mode abbreviation matches the currently selected
     * mode, using case-insensitive comparison.
     *
     * @return the matching {@link Decoder}, or {@code null} if none found
     */
    private Decoder findDecoder() {
        if (currentMode == null) {
            return null;
        }
        String target = currentMode.getAbbreviation();
        for (Map.Entry<DigitalMode, Decoder> entry : decoders.entrySet()) {
            if (entry.getKey().getAbbreviation().equalsIgnoreCase(target)) {
                return entry.getValue();
            }
        }
        return null;
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Connects a radio rig audio source so that live rig audio feeds into the decoder and
     * spectrum display.
     *
     * <p>The rig source is not started automatically — call {@link RigAudioSource#start()} on
     * it when you are ready to begin receiving audio.
     *
     * @param source the rig audio source to connect; must not be {@code null}
     */
    public void setRigAudioSource(RigAudioSource source) {
        this.rigAudioSource = source;
        source.getBuffer().addListener(buffer ->
                sharedBuffer.load(buffer.getSamples(), buffer.getSampleRate()));
    }

    /**
     * Makes the Digital Modes window visible and starts a new session.
     *
     * <p>A "Digital Modes Ready" banner appears in the decode output area each time the window
     * opens so you can easily see when a new session begins.
     */
    public void openWindow() {
        setVisible(true);
        requestFocus();
        decodeTextArea.appendText("--- Digital Modes Ready ---\n");
    }

    /**
     * Hides the Digital Modes window and stops all background decoding and playback.
     *
     * <p>The window is hidden rather than closed, so all decode history and settings are
     * preserved for the next time you call {@link #openWindow()}.
     */
    public void closeWindow() {
        if (decodeTimer != null) {
            decodeTimer.stop();
        }
        playbackController.stop();
        if (rigAudioSource != null && rigAudioSource.isActive()) {
            rigAudioSource.stop();
        }
        setVisible(false);
    }
}
