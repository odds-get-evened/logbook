package org.qualsh.lb.digitalmodes.view;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;

/**
 * The audio control toolbar at the top of the Digital Modes window.
 *
 * <p>The toolbar has six buttons — Upload WAV, Record, Stop Recording, Play, Pause, Stop —
 * plus a Loop toggle and a status label. Register an {@link AudioControlListener} with
 * {@link #setAudioControlListener(AudioControlListener)} to connect these buttons to the rest
 * of the application.
 *
 * @author Logbook Development Team
 * @version 1.0
 */
public class AudioControlPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    // -------------------------------------------------------------------------
    // Listener interface
    // -------------------------------------------------------------------------

    /**
     * Notified when you press a button on the {@link AudioControlPanel}.
     *
     * <p>Register an implementation with
     * {@link AudioControlPanel#setAudioControlListener(AudioControlListener)} to connect the
     * toolbar buttons to the rest of the application.
     *
     * @author Logbook Development Team
     * @version 1.0
     */
    public interface AudioControlListener {

        /**
         * Called after you select a WAV file to load using the Upload WAV button.
         *
         * @param file the WAV file you selected; never {@code null}
         */
        void onFileUploaded(File file);

        /**
         * Called when you choose a save location and start recording using the Record button.
         *
         * @param outputFile the file where the recording will be saved; never {@code null}
         */
        void onRecordStarted(File outputFile);

        /**
         * Called when you press Stop Recording to end the current recording session.
         */
        void onRecordStopped();

        /**
         * Called when you press Play to start or resume audio playback.
         */
        void onPlayClicked();

        /**
         * Called when you press Pause to pause playback at the current position.
         */
        void onPauseClicked();

        /**
         * Called when you press Stop to halt playback and return to the beginning.
         */
        void onStopClicked();

        /**
         * Called when you toggle the Loop button on or off.
         *
         * @param looping {@code true} if loop playback is now enabled; {@code false} if turned off
         */
        void onLoopToggled(boolean looping);
    }

    // -------------------------------------------------------------------------
    // Fields
    // -------------------------------------------------------------------------

    private final JButton       uploadButton;
    private final JButton       recordButton;
    private final JButton       stopRecordButton;
    private final JButton       playButton;
    private final JButton       pauseButton;
    private final JButton       stopButton;
    private final JToggleButton loopButton;
    private final JLabel        statusLabel;

    private AudioControlListener listener;

    // -------------------------------------------------------------------------
    // Construction
    // -------------------------------------------------------------------------

    /**
     * Creates a new audio control toolbar with all buttons ready to use. The Stop Recording
     * button is initially disabled since there is no active recording to stop.
     */
    public AudioControlPanel() {
        setLayout(new FlowLayout(FlowLayout.LEFT, 6, 4));

        uploadButton     = new JButton("Upload WAV");
        recordButton     = new JButton("Record");
        stopRecordButton = new JButton("Stop Recording");
        playButton       = new JButton("\u25B6 Play");
        pauseButton      = new JButton("\u23F8 Pause");
        stopButton       = new JButton("\u23F9 Stop");
        loopButton       = new JToggleButton("\u27F3 Loop");
        statusLabel      = new JLabel("No file loaded");

        stopRecordButton.setEnabled(false);

        add(uploadButton);
        add(recordButton);
        add(stopRecordButton);
        add(new JSeparator(SwingConstants.VERTICAL));
        add(playButton);
        add(pauseButton);
        add(stopButton);
        add(loopButton);
        add(new JSeparator(SwingConstants.VERTICAL));
        add(statusLabel);

        attachListeners();
    }

    // -------------------------------------------------------------------------
    // Internal listeners
    // -------------------------------------------------------------------------

    private void attachListeners() {

        uploadButton.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("Select WAV File");
            chooser.setFileFilter(new FileNameExtensionFilter("WAV Audio Files (*.wav)", "wav"));
            int result = chooser.showOpenDialog(AudioControlPanel.this);
            if (result == JFileChooser.APPROVE_OPTION) {
                File file = chooser.getSelectedFile();
                statusLabel.setText(file.getName());
                if (listener != null) {
                    listener.onFileUploaded(file);
                }
            }
        });

        recordButton.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("Save Recording As");
            chooser.setFileFilter(new FileNameExtensionFilter("WAV Audio Files (*.wav)", "wav"));
            chooser.setSelectedFile(new File(System.getProperty("user.home"), "recording.wav"));
            int result = chooser.showSaveDialog(AudioControlPanel.this);
            if (result == JFileChooser.APPROVE_OPTION) {
                File file = chooser.getSelectedFile();
                if (!file.getName().toLowerCase().endsWith(".wav")) {
                    file = new File(file.getAbsolutePath() + ".wav");
                }
                recordButton.setEnabled(false);
                stopRecordButton.setEnabled(true);
                statusLabel.setText("Recording...");
                if (listener != null) {
                    listener.onRecordStarted(file);
                }
            }
        });

        stopRecordButton.addActionListener(e -> {
            recordButton.setEnabled(true);
            stopRecordButton.setEnabled(false);
            statusLabel.setText("Recording saved");
            if (listener != null) {
                listener.onRecordStopped();
            }
        });

        playButton.addActionListener(e -> {
            if (listener != null) {
                listener.onPlayClicked();
            }
        });

        pauseButton.addActionListener(e -> {
            if (listener != null) {
                listener.onPauseClicked();
            }
        });

        stopButton.addActionListener(e -> {
            if (listener != null) {
                listener.onStopClicked();
            }
        });

        loopButton.addActionListener(e -> {
            if (listener != null) {
                listener.onLoopToggled(loopButton.isSelected());
            }
        });
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Registers the listener that receives button events from this panel. Pass {@code null}
     * to remove any existing listener.
     *
     * @param listener the listener to register, or {@code null} to deregister
     */
    public void setAudioControlListener(AudioControlListener listener) {
        this.listener = listener;
    }

    /**
     * Returns the status label so external code can update its text, for example after a
     * file finishes loading.
     *
     * @return the status label; never {@code null}
     */
    public JLabel getStatusLabel() {
        return statusLabel;
    }
}
