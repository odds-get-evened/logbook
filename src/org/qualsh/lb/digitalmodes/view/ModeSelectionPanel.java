package org.qualsh.lb.digitalmodes.view;

import org.qualsh.lb.digital.DigitalMode;
import org.qualsh.lb.digitalmodes.mode.ModeProfile;

import javax.swing.*;
import java.awt.*;

/**
 * The mode selector drop-down panel at the top of the Digital Modes window.
 *
 * <p>The panel contains a "Mode:" label and a drop-down list of every supported digital mode.
 * A short description of the selected mode — including its typical bandwidth — is shown next
 * to the drop-down. Register a {@link ModeChangeListener} with
 * {@link #setModeChangeListener(ModeChangeListener)} to be notified whenever you pick a
 * different mode.
 *
 * @author Logbook Development Team
 * @version 1.0
 */
public class ModeSelectionPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    // -------------------------------------------------------------------------
    // Listener interface
    // -------------------------------------------------------------------------

    /**
     * Notified when you select a different digital mode from the drop-down.
     *
     * @author Logbook Development Team
     * @version 1.0
     */
    public interface ModeChangeListener {

        /**
         * Called whenever you select a different mode from the drop-down.
         *
         * @param mode the newly selected mode; never {@code null}
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
     * Creates a new mode selection panel with FT8 selected by default.
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
     * Registers a listener to be notified when you pick a different digital mode. Pass
     * {@code null} to remove any existing listener.
     *
     * @param listener the listener to register, or {@code null} to deregister
     */
    public void setModeChangeListener(ModeChangeListener listener) {
        this.listener = listener;
    }

    /**
     * Returns the digital mode currently selected in the drop-down.
     *
     * @return the selected mode; never {@code null} as long as at least one mode is available
     */
    public DigitalMode getSelectedMode() {
        return (DigitalMode) modeComboBox.getSelectedItem();
    }

    /**
     * Selects a digital mode in the drop-down. If the given mode is not available, this method
     * has no effect.
     *
     * @param mode the mode to select; ignored if {@code null}
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
