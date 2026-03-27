package org.qualsh.lb.digitalmodes.view;

import org.qualsh.lb.digital.DigitalMode;
import org.qualsh.lb.digitalmodes.mode.ModeProfile;

import javax.swing.*;
import java.awt.*;

/**
 * A compact selection panel that lets the operator choose which digital mode
 * the decoder and encoder should use.
 *
 * <p>The panel contains a labelled drop-down ({@link JComboBox}) pre-populated
 * with every supported digital mode, and a description label that updates
 * automatically to show the selected mode's key signal characteristics
 * (bandwidth, modulation family, and typical use case).
 *
 * <p>Register a {@link ModeChangeListener} with
 * {@link #setModeChangeListener(ModeChangeListener)} to be notified whenever
 * the operator changes the selected mode. The listener fires on the Swing Event
 * Dispatch Thread.
 */
public class ModeSelectionPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    // -------------------------------------------------------------------------
    // Listener interface
    // -------------------------------------------------------------------------

    /**
     * Callback interface for components that need to react when the operator
     * selects a different digital mode.
     */
    public interface ModeChangeListener {

        /**
         * Called on the Swing Event Dispatch Thread whenever the selected mode
         * changes.
         *
         * @param mode the newly selected {@link DigitalMode}; never
         *             {@code null}
         */
        void onModeChanged(DigitalMode mode);
    }

    // -------------------------------------------------------------------------
    // Fields
    // -------------------------------------------------------------------------

    private final JComboBox<DigitalMode> modeComboBox;
    private final JLabel modeDescriptionLabel;
    private ModeChangeListener listener;

    // -------------------------------------------------------------------------
    // Supported modes
    // -------------------------------------------------------------------------

    /** The ordered list of digital modes shown in the combo box. */
    private static final DigitalMode[] SUPPORTED_MODES = {
        new DigitalMode("FT8",    "FT8"),
        new DigitalMode("WSPR",   "WSPR"),
        new DigitalMode("BPSK31", "BPSK31"),
        new DigitalMode("RTTY",   "RTTY"),
        new DigitalMode("MFSK16", "MFSK16"),
        new DigitalMode("Olivia", "Olivia"),
        new DigitalMode("Packet", "Packet"),
    };

    // -------------------------------------------------------------------------
    // Construction
    // -------------------------------------------------------------------------

    /**
     * Creates a new {@code ModeSelectionPanel} with FT8 selected by default.
     *
     * <p>The panel uses a {@link BorderLayout}: the mode label and combo box
     * are placed in {@code WEST} and the description label occupies
     * {@code CENTER}. The description label wraps text and has a maximum width
     * of 200 pixels.
     */
    public ModeSelectionPanel() {
        setLayout(new BorderLayout(8, 0));

        // West: "Mode: " label + combo box
        JPanel westPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        westPanel.add(new JLabel("Mode: "));

        modeComboBox = new JComboBox<>();
        for (DigitalMode mode : SUPPORTED_MODES) {
            modeComboBox.addItem(mode);
        }
        modeComboBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(
                    JList<?> list, Object value, int index,
                    boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof DigitalMode) {
                    String name = ((DigitalMode) value).getName();
                    setText(name != null ? name : value.toString());
                }
                return this;
            }
        });
        westPanel.add(modeComboBox);
        add(westPanel, BorderLayout.WEST);

        // Center: mode description label
        modeDescriptionLabel = new JLabel();
        modeDescriptionLabel.setMaximumSize(new Dimension(200, Integer.MAX_VALUE));
        modeDescriptionLabel.setPreferredSize(new Dimension(200, modeDescriptionLabel.getPreferredSize().height));
        add(modeDescriptionLabel, BorderLayout.CENTER);

        // Initialise description with the default selection
        updateDescription((DigitalMode) modeComboBox.getSelectedItem());

        // Combo box change listener
        modeComboBox.addActionListener(e -> {
            DigitalMode selected = (DigitalMode) modeComboBox.getSelectedItem();
            if (selected != null) {
                updateDescription(selected);
                if (listener != null) {
                    listener.onModeChanged(selected);
                }
            }
        });
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Updates the description label to reflect the given mode's characteristics.
     */
    private void updateDescription(DigitalMode mode) {
        modeDescriptionLabel.setText(buildModeDescription(mode));
    }

    /**
     * Builds a human-readable description string for a mode.
     * Uses ModeProfile for bandwidth and a short hard-coded summary per mode.
     */
    private String buildModeDescription(DigitalMode mode) {
        if (mode == null) {
            return "";
        }
        ModeProfile profile = ModeProfile.getProfile(mode);
        String bw = String.format("%.0f Hz", profile.getDefaultBandwidthHz());
        String abbrev = mode.getAbbreviation() == null ? "" : mode.getAbbreviation();
        switch (abbrev) {
            case "FT8":    return "Weak-signal 8-FSK, 15 s slots. BW: " + bw;
            case "WSPR":   return "Propagation beacon, 2 min slots. BW: " + bw;
            case "BPSK31": return "BPSK keyboard mode, 31.25 baud. BW: " + bw;
            case "RTTY":   return "FSK teletype, 45.45 baud ITA2. BW: " + bw;
            case "MFSK16": return "16-tone MFSK, selective-fade robust. BW: " + bw;
            case "Olivia": return "MFSK+FEC, extreme weak-signal mode. BW: " + bw;
            case "Packet": return "AX.25 packet / APRS, 1200 baud AFSK. BW: " + bw;
            default:       return mode.getName() + ". BW: " + bw;
        }
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Registers a listener to be notified whenever the operator selects a
     * different digital mode.
     *
     * <p>Passing {@code null} removes any previously registered listener.
     *
     * @param listener the listener to register, or {@code null} to deregister
     */
    public void setModeChangeListener(ModeChangeListener listener) {
        this.listener = listener;
    }

    /**
     * Returns the digital mode currently selected in the combo box.
     *
     * @return the selected {@link DigitalMode}; never {@code null} as long as
     *         the combo box is populated
     */
    public DigitalMode getSelectedMode() {
        return (DigitalMode) modeComboBox.getSelectedItem();
    }

    /**
     * Programmatically selects a digital mode in the combo box.
     *
     * <p>The selection is matched by abbreviation so that equivalent
     * {@link DigitalMode} instances that differ only in object identity can
     * still be found. If no matching mode is present in the list this method
     * has no effect.
     *
     * @param mode the {@link DigitalMode} to select; ignored if {@code null}
     */
    public void setSelectedMode(DigitalMode mode) {
        if (mode == null) {
            return;
        }
        for (int i = 0; i < modeComboBox.getItemCount(); i++) {
            DigitalMode item = modeComboBox.getItemAt(i);
            if (item.getAbbreviation().equalsIgnoreCase(mode.getAbbreviation())) {
                modeComboBox.setSelectedIndex(i);
                return;
            }
        }
    }
}
