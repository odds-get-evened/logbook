package org.qualsh.lb.view;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;

import org.qualsh.lb.util.Preferences;

/**
 * Modal dialog for configuring the DX Cluster connection.
 */
public class DXClusterSettingsDialog extends JDialog {

    private static final long serialVersionUID = 1L;

    private JTextField textHost;
    private JSpinner   spinnerPort;
    private JTextField textCallsign;

    public DXClusterSettingsDialog(JFrame owner) {
        super(owner, "DX Cluster Settings", true);
        setMinimumSize(new Dimension(380, 240));
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        setSize((int) (screen.getWidth() * 0.28), (int) (screen.getHeight() * 0.30));
        setLocationRelativeTo(owner);

        JPanel outerPanel = new JPanel(new BorderLayout(0, 5));
        outerPanel.setBorder(new EmptyBorder(8, 8, 8, 8));
        getContentPane().add(outerPanel, BorderLayout.CENTER);

        // ── Settings panel ────────────────────────────────────────────────────
        JPanel settingsPanel = new JPanel();
        settingsPanel.setBorder(new CompoundBorder(
                new TitledBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null),
                        "DX Cluster Connection", TitledBorder.LEADING, TitledBorder.TOP, null, null),
                new EmptyBorder(8, 8, 8, 8)));
        outerPanel.add(settingsPanel, BorderLayout.CENTER);

        GridBagLayout gbl = new GridBagLayout();
        gbl.columnWidths  = new int[]    { 0, 0 };
        gbl.rowHeights    = new int[]    { 0, 0, 0, 0 };
        gbl.columnWeights = new double[] { 0.0, 1.0 };
        gbl.rowWeights    = new double[] { 0.0, 0.0, 0.0, Double.MIN_VALUE };
        settingsPanel.setLayout(gbl);

        // Host
        JLabel lblHost = new JLabel("Host");
        lblHost.setFont(new Font("Tahoma", Font.BOLD, 11));
        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(0, 0, 6, 8);
        c.gridx = 0; c.gridy = 0;
        settingsPanel.add(lblHost, c);

        textHost = new JTextField();
        c = new GridBagConstraints();
        c.fill   = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(0, 0, 6, 0);
        c.gridx  = 1; c.gridy = 0;
        settingsPanel.add(textHost, c);

        // Port
        JLabel lblPort = new JLabel("Port");
        lblPort.setFont(new Font("Tahoma", Font.BOLD, 11));
        c = new GridBagConstraints();
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(0, 0, 6, 8);
        c.gridx  = 0; c.gridy = 1;
        settingsPanel.add(lblPort, c);

        spinnerPort = new JSpinner(new SpinnerNumberModel(7300, 1, 65535, 1));
        c = new GridBagConstraints();
        c.fill   = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(0, 0, 6, 0);
        c.gridx  = 1; c.gridy = 1;
        settingsPanel.add(spinnerPort, c);

        // Callsign (login)
        JLabel lblCallsign = new JLabel("Callsign");
        lblCallsign.setFont(new Font("Tahoma", Font.BOLD, 11));
        c = new GridBagConstraints();
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(0, 0, 6, 8);
        c.gridx  = 0; c.gridy = 2;
        settingsPanel.add(lblCallsign, c);

        textCallsign = new JTextField();
        c = new GridBagConstraints();
        c.fill   = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(0, 0, 6, 0);
        c.gridx  = 1; c.gridy = 2;
        settingsPanel.add(textCallsign, c);

        // Note
        JLabel lblNote = new JLabel("Common DX cluster ports: 7300 (DX Spider), 23 (AR-Cluster)");
        lblNote.setFont(new Font("Tahoma", Font.ITALIC, 10));
        c = new GridBagConstraints();
        c.gridwidth = 2;
        c.anchor    = GridBagConstraints.WEST;
        c.insets    = new Insets(0, 0, 0, 0);
        c.gridx     = 0; c.gridy = 3;
        settingsPanel.add(lblNote, c);

        // ── Save / Cancel ─────────────────────────────────────────────────────
        JPanel btnPanel = new JPanel();
        outerPanel.add(btnPanel, BorderLayout.SOUTH);

        JButton btnSave = new JButton("Save");
        btnSave.addActionListener(e -> { save(); dispose(); });
        btnPanel.add(btnSave);

        JButton btnCancel = new JButton("Cancel");
        btnCancel.addActionListener(e -> dispose());
        btnPanel.add(btnCancel);

        loadSettings();
    }

    private void loadSettings() {
        String host      = Preferences.getOne(Preferences.PREF_DX_CLUSTER_HOST);
        String portStr   = Preferences.getOne(Preferences.PREF_DX_CLUSTER_PORT);
        String callsign  = Preferences.getOne(Preferences.PREF_DX_CLUSTER_CALLSIGN);

        if (host     != null) textHost.setText(host);
        if (portStr  != null) {
            try { spinnerPort.setValue(Integer.parseInt(portStr)); } catch (NumberFormatException ignored) {}
        }
        if (callsign != null) textCallsign.setText(callsign);
    }

    private void save() {
        String host = textHost.getText().trim();
        if (!host.isEmpty()) Preferences.save(Preferences.PREF_DX_CLUSTER_HOST, host);
        Preferences.save(Preferences.PREF_DX_CLUSTER_PORT, String.valueOf(spinnerPort.getValue()));
        String cs = textCallsign.getText().trim();
        if (!cs.isEmpty()) Preferences.save(Preferences.PREF_DX_CLUSTER_CALLSIGN, cs);
    }
}
