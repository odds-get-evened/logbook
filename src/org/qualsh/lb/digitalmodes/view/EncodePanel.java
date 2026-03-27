package org.qualsh.lb.digitalmodes.view;

import org.qualsh.lb.digital.DigitalMode;
import org.qualsh.lb.digitalmodes.audio.AudioBuffer;
import org.qualsh.lb.digitalmodes.audio.PlaybackController;
import org.qualsh.lb.digitalmodes.encoder.Bpsk31Encoder;
import org.qualsh.lb.digitalmodes.encoder.Encoder;
import org.qualsh.lb.digitalmodes.encoder.Encoder.EncoderException;
import org.qualsh.lb.digitalmodes.encoder.Ft8Encoder;
import org.qualsh.lb.digitalmodes.encoder.MfskEncoder;
import org.qualsh.lb.digitalmodes.encoder.OliviaEncoder;
import org.qualsh.lb.digitalmodes.encoder.PacketEncoder;
import org.qualsh.lb.digitalmodes.encoder.RttyEncoder;
import org.qualsh.lb.digitalmodes.encoder.WsprEncoder;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The transmit panel for digital modes, displayed on the Encode tab of the
 * Digital Modes window.
 *
 * <p>EncodePanel gives you a full message-composition and transmission
 * workflow in one place:
 * <ol>
 *   <li>Type your message in the text area.</li>
 *   <li>Pick a digital mode from the drop-down.</li>
 *   <li>Click <em>Encode</em> to convert the text to audio.</li>
 *   <li>Optionally click <em>Preview Audio</em> to hear what will be
 *       transmitted through your speakers.</li>
 *   <li>Click <em>Transmit</em> when you are ready to go on air (requires
 *       a connected rig configured in Preferences).</li>
 * </ol>
 *
 * <p>The character counter below the text area tracks how many characters you
 * have typed against the limit imposed by the selected mode. FT8 and WSPR
 * allow a maximum of 13 characters; all other modes treat message length as
 * unlimited.
 *
 * <p>Call {@link #setOperatorCallsign(String)} and
 * {@link #setOperatorGridSquare(String)} before encoding to ensure your
 * station details are embedded in the transmitted signal.
 */
public class EncodePanel extends JPanel {

    private static final Color ERROR_COLOR      = new Color(200, 50, 50);
    private static final Color SUCCESS_COLOR    = new Color(50, 180, 80);
    private static final Color PANEL_BACKGROUND = new Color(25, 25, 25);
    private static final Color TEXT_COLOR       = new Color(200, 200, 200);

    private final JTextArea          messageInputArea;
    private final JComboBox<DigitalMode> modeComboBox;
    private final JButton            encodeButton;
    private final JButton            previewButton;
    private final JButton            transmitButton;
    private final JButton            clearButton;
    private final JLabel             statusLabel;
    private final JLabel             charCountLabel;
    private final JProgressBar       encodeProgressBar;

    private AudioBuffer        encodedBuffer;
    private PlaybackController previewController;

    private final Map<DigitalMode, Encoder> encoders;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    /**
     * Creates a new {@code EncodePanel} with all controls initialised and ready
     * for use. The panel uses a dark background and a top-to-bottom
     * GridBagLayout.
     */
    public EncodePanel() {
        setBackground(PANEL_BACKGROUND);
        setLayout(new GridBagLayout());

        // --- Build encoders map ---
        encoders = new LinkedHashMap<>();
        Encoder[] allEncoders = {
            new Ft8Encoder(),
            new WsprEncoder(),
            new Bpsk31Encoder(),
            new RttyEncoder(),
            new MfskEncoder(),
            new OliviaEncoder(),
            new PacketEncoder()
        };
        for (Encoder enc : allEncoders) {
            encoders.put(enc.getMode(), enc);
        }

        // --- Combo box ---
        modeComboBox = new JComboBox<>();
        for (DigitalMode mode : encoders.keySet()) {
            modeComboBox.addItem(mode);
        }
        modeComboBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(
                    JList<?> list, Object value, int index,
                    boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof DigitalMode) {
                    String display = ((DigitalMode) value).getName();
                    setText(display != null ? display : value.toString());
                }
                return this;
            }
        });

        // --- Text area ---
        messageInputArea = new JTextArea(6, 40);
        messageInputArea.setLineWrap(true);
        messageInputArea.setWrapStyleWord(true);

        // --- Buttons ---
        encodeButton   = new JButton("Encode");
        previewButton  = new JButton("Preview Audio");
        transmitButton = new JButton("Transmit");
        clearButton    = new JButton("Clear");

        previewButton.setEnabled(false);
        transmitButton.setEnabled(false);

        // --- Status / progress ---
        statusLabel = new JLabel(" ");
        statusLabel.setHorizontalAlignment(SwingConstants.CENTER);
        statusLabel.setForeground(TEXT_COLOR);

        charCountLabel = new JLabel("0 / \u221e chars");
        charCountLabel.setForeground(TEXT_COLOR);

        encodeProgressBar = new JProgressBar();
        encodeProgressBar.setIndeterminate(true);
        encodeProgressBar.setVisible(false);

        // --- Layout ---
        buildLayout();

        // --- Listeners ---
        attachListeners();
    }

    // -------------------------------------------------------------------------
    // Layout
    // -------------------------------------------------------------------------

    private void buildLayout() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets  = new Insets(4, 8, 4, 8);
        gbc.fill    = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        int row = 0;

        // Header
        JLabel header = new JLabel("Compose Message");
        header.setFont(header.getFont().deriveFont(Font.BOLD, 14f));
        header.setForeground(TEXT_COLOR);
        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 4;
        add(header, gbc);
        row++;

        // Mode selector row
        JLabel modeLabel = new JLabel("Mode:");
        modeLabel.setForeground(TEXT_COLOR);
        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 1; gbc.weightx = 0;
        add(modeLabel, gbc);
        gbc.gridx = 1; gbc.gridwidth = 3; gbc.weightx = 1.0;
        add(modeComboBox, gbc);
        row++;

        // Message label
        JLabel messageLabel = new JLabel("Message:");
        messageLabel.setForeground(TEXT_COLOR);
        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 4; gbc.weightx = 1.0;
        add(messageLabel, gbc);
        row++;

        // Message scroll pane
        JScrollPane scrollPane = new JScrollPane(messageInputArea);
        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 4;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1.0;
        add(scrollPane, gbc);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weighty = 0;
        row++;

        // Character count — right-aligned
        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 4;
        gbc.anchor = GridBagConstraints.EAST;
        add(charCountLabel, gbc);
        gbc.anchor = GridBagConstraints.WEST;
        row++;

        // Button row
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        buttonPanel.setBackground(PANEL_BACKGROUND);
        buttonPanel.add(encodeButton);
        buttonPanel.add(previewButton);
        buttonPanel.add(transmitButton);
        buttonPanel.add(clearButton);
        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 4;
        add(buttonPanel, gbc);
        row++;

        // Progress bar
        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 4;
        add(encodeProgressBar, gbc);
        row++;

        // Status label
        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 4;
        gbc.anchor = GridBagConstraints.CENTER;
        add(statusLabel, gbc);
    }

    // -------------------------------------------------------------------------
    // Listeners
    // -------------------------------------------------------------------------

    private void attachListeners() {
        encodeButton.addActionListener(e -> onEncodeClicked());
        previewButton.addActionListener(e -> onPreviewClicked());
        transmitButton.addActionListener(e -> onTransmitClicked());
        clearButton.addActionListener(e -> onClearClicked());

        modeComboBox.addActionListener(e -> updateCharCount());

        messageInputArea.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e)  { updateCharCount(); }
            @Override public void removeUpdate(DocumentEvent e)  { updateCharCount(); }
            @Override public void changedUpdate(DocumentEvent e) { updateCharCount(); }
        });
    }

    // -------------------------------------------------------------------------
    // Action handlers
    // -------------------------------------------------------------------------

    private void onEncodeClicked() {
        DigitalMode selectedMode = (DigitalMode) modeComboBox.getSelectedItem();
        String text = messageInputArea.getText().trim();

        encodeProgressBar.setVisible(true);
        encodeButton.setEnabled(false);
        statusLabel.setText(" ");
        statusLabel.setForeground(TEXT_COLOR);

        SwingWorker<AudioBuffer, Void> worker = new SwingWorker<AudioBuffer, Void>() {
            @Override
            protected AudioBuffer doInBackground() throws EncoderException {
                Encoder encoder = encoders.get(selectedMode);
                return encoder.encode(text, selectedMode);
            }

            @Override
            protected void done() {
                encodeProgressBar.setVisible(false);
                encodeButton.setEnabled(true);
                try {
                    encodedBuffer = get();
                    previewButton.setEnabled(true);
                    transmitButton.setEnabled(true);
                    statusLabel.setText("Encoded successfully \u2014 ready to preview or transmit");
                    statusLabel.setForeground(SUCCESS_COLOR);
                } catch (java.util.concurrent.ExecutionException ex) {
                    Throwable cause = ex.getCause();
                    String msg = (cause != null) ? cause.getMessage() : ex.getMessage();
                    statusLabel.setText(msg);
                    statusLabel.setForeground(ERROR_COLOR);
                    previewButton.setEnabled(false);
                    transmitButton.setEnabled(false);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    statusLabel.setText("Encoding interrupted.");
                    statusLabel.setForeground(ERROR_COLOR);
                    previewButton.setEnabled(false);
                    transmitButton.setEnabled(false);
                }
            }
        };
        worker.execute();
    }

    private void onPreviewClicked() {
        if (encodedBuffer == null || encodedBuffer.isEmpty()) {
            return;
        }
        previewController = new PlaybackController(encodedBuffer);
        previewController.play();
        statusLabel.setText("Playing preview...");
        statusLabel.setForeground(TEXT_COLOR);
    }

    private void onTransmitClicked() {
        statusLabel.setText(
            "Transmit not yet implemented \u2014 connect a rig in Preferences to enable transmit");
        statusLabel.setForeground(ERROR_COLOR);
        transmitButton.setEnabled(false);
    }

    private void onClearClicked() {
        messageInputArea.setText("");
        statusLabel.setText(" ");
        statusLabel.setForeground(TEXT_COLOR);
        encodedBuffer = null;
        previewButton.setEnabled(false);
        transmitButton.setEnabled(false);
        updateCharCount();
    }

    // -------------------------------------------------------------------------
    // Char count
    // -------------------------------------------------------------------------

    private void updateCharCount() {
        int length = messageInputArea.getText().length();
        DigitalMode mode = (DigitalMode) modeComboBox.getSelectedItem();

        int limit = charLimitFor(mode);
        String limitStr = (limit > 0) ? String.valueOf(limit) : "\u221e";
        charCountLabel.setText(length + " / " + limitStr + " chars");

        if (limit > 0 && length > limit) {
            charCountLabel.setForeground(ERROR_COLOR);
        } else {
            charCountLabel.setForeground(TEXT_COLOR);
        }
    }

    /**
     * Returns the maximum character count for the given mode, or 0 to indicate
     * no limit.
     */
    private int charLimitFor(DigitalMode mode) {
        if (mode == null) {
            return 0;
        }
        String abbr = mode.getAbbreviation();
        if ("FT8".equalsIgnoreCase(abbr) || "WSPR".equalsIgnoreCase(abbr)) {
            return 13;
        }
        return 0;
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Sets the operator callsign on every encoder managed by this panel.
     *
     * <p>The callsign is embedded in each transmitted signal so that receiving
     * stations can identify you. You must configure your callsign before
     * clicking <em>Encode</em>. Open <em>Preferences &rarr; Station</em> to
     * set your callsign application-wide; this method is called automatically
     * when those preferences are saved.
     *
     * @param callsign your licensed amateur radio callsign, for example
     *                 {@code "W1AW"}
     */
    public void setOperatorCallsign(String callsign) {
        for (Encoder encoder : encoders.values()) {
            if (encoder instanceof Ft8Encoder)    ((Ft8Encoder)    encoder).setOperatorCallsign(callsign);
            else if (encoder instanceof WsprEncoder)   ((WsprEncoder)   encoder).setOperatorCallsign(callsign);
            else if (encoder instanceof Bpsk31Encoder) ((Bpsk31Encoder) encoder).setOperatorCallsign(callsign);
            else if (encoder instanceof RttyEncoder)   ((RttyEncoder)   encoder).setOperatorCallsign(callsign);
            else if (encoder instanceof MfskEncoder)   ((MfskEncoder)   encoder).setOperatorCallsign(callsign);
            else if (encoder instanceof OliviaEncoder) ((OliviaEncoder) encoder).setOperatorCallsign(callsign);
            else if (encoder instanceof PacketEncoder) ((PacketEncoder) encoder).setOperatorCallsign(callsign);
        }
    }

    /**
     * Sets the Maidenhead grid square on the WSPR encoder.
     *
     * <p>WSPR transmissions always include your four-character grid locator
     * (for example {@code "FN42"}) so that receiving stations can calculate
     * propagation paths. This value is only used by the WSPR encoder; all other
     * modes ignore it. Open <em>Preferences &rarr; Station</em> to configure
     * your grid square application-wide.
     *
     * @param grid your four-character Maidenhead grid locator
     */
    public void setOperatorGridSquare(String grid) {
        for (Encoder encoder : encoders.values()) {
            if (encoder instanceof WsprEncoder) {
                ((WsprEncoder) encoder).setGridSquare(grid);
            }
        }
    }

    /**
     * Returns the digital mode currently selected in the mode drop-down.
     *
     * @return the selected {@link DigitalMode}; never {@code null} as long as
     *         at least one mode has been loaded
     */
    public DigitalMode getSelectedMode() {
        return (DigitalMode) modeComboBox.getSelectedItem();
    }

    /**
     * Programmatically selects a digital mode in the mode drop-down.
     *
     * <p>Use this to restore a previously chosen mode when the window is
     * reopened, or to pre-select the mode that matches a log entry the user is
     * reviewing. If {@code mode} is not present in the combo box this method
     * has no effect.
     *
     * @param mode the {@link DigitalMode} to select
     */
    public void setSelectedMode(DigitalMode mode) {
        modeComboBox.setSelectedItem(mode);
    }
}
