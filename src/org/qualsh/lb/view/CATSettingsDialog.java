package org.qualsh.lb.view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;

import org.qualsh.lb.rig.RigController;
import org.qualsh.lb.rig.RigController.ConnectionType;
import org.qualsh.lb.rig.RigController.SerialProtocol;
import org.qualsh.lb.util.Preferences;

/**
 * Modal dialog for configuring CAT (Computer-Aided Transceiver) rig control.
 *
 * <p>Users choose between:
 * <ul>
 *   <li><b>rigctld</b> – host + port of a running hamlib rigctld daemon.</li>
 *   <li><b>Direct serial</b> – serial/USB port, baud rate, and radio protocol
 *       (Yaesu, Kenwood, Icom CI-V).</li>
 * </ul>
 */
public class CATSettingsDialog extends JDialog {

    private static final long serialVersionUID = 1L;

    private static final int[] BAUD_RATES = { 1200, 2400, 4800, 9600, 19200, 38400, 57600, 115200 };

    // rigctld widgets
    private JTextField textHost;
    private JSpinner spinnerPort;

    // serial widgets
    private JComboBox<String> comboSerialPort;
    private JComboBox<String> comboBaud;
    private JComboBox<SerialProtocol> comboProtocol;
    private JTextField textIcomAddress;
    private JLabel lblIcomAddress;

    // type selection
    private JRadioButton radioRigctld;
    private JRadioButton radioSerial;

    // status
    private JLabel lblStatus;

    // panels to show/hide
    private JPanel rigctldPanel;
    private JPanel serialPanel;

    public CATSettingsDialog(JFrame owner) {
        super(owner, "CAT / Rig Control Settings", true);
        setMinimumSize(new Dimension(440, 400));
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        setSize((int)(screen.getWidth() * 0.32), (int)(screen.getHeight() * 0.45));
        setLocationRelativeTo(owner);

        JPanel outer = new JPanel(new BorderLayout(0, 6));
        outer.setBorder(new EmptyBorder(8, 8, 8, 8));
        getContentPane().add(outer, BorderLayout.CENTER);

        // ── Connection type ───────────────────────────────────────────────────
        JPanel typePanel = new JPanel(new GridBagLayout());
        typePanel.setBorder(new CompoundBorder(
                new TitledBorder(new EtchedBorder(), "Connection Type"),
                new EmptyBorder(4, 6, 4, 6)));
        outer.add(typePanel, BorderLayout.NORTH);

        radioRigctld = new JRadioButton("rigctld (hamlib network daemon)  – recommended");
        radioSerial  = new JRadioButton("Direct serial / USB CAT");
        ButtonGroup bg = new ButtonGroup();
        bg.add(radioRigctld);
        bg.add(radioSerial);
        radioRigctld.setSelected(true);

        GridBagConstraints gc = new GridBagConstraints();
        gc.anchor = GridBagConstraints.WEST; gc.insets = new Insets(2, 0, 2, 0);
        gc.gridx = 0; gc.gridy = 0; typePanel.add(radioRigctld, gc);
        gc.gridy = 1;                typePanel.add(radioSerial, gc);

        JLabel lblTypeNote = new JLabel(
                "<html><i>rigctld supports 200+ radios. Run <tt>rigctld -m &lt;model&gt; -r &lt;port&gt;</tt> first.</i></html>");
        lblTypeNote.setFont(lblTypeNote.getFont().deriveFont(10f));
        gc.gridy = 2; gc.insets = new Insets(2, 20, 2, 0);
        typePanel.add(lblTypeNote, gc);

        // ── Centre panel (rigctld + serial stacked, toggled) ──────────────────
        JPanel centrePanel = new JPanel(new BorderLayout(0, 6));
        outer.add(centrePanel, BorderLayout.CENTER);

        // rigctld settings
        rigctldPanel = new JPanel(new GridBagLayout());
        rigctldPanel.setBorder(new CompoundBorder(
                new TitledBorder(new EtchedBorder(), "rigctld Settings"),
                new EmptyBorder(6, 8, 6, 8)));
        centrePanel.add(rigctldPanel, BorderLayout.NORTH);
        buildRigctldPanel();

        // serial settings
        serialPanel = new JPanel(new GridBagLayout());
        serialPanel.setBorder(new CompoundBorder(
                new TitledBorder(new EtchedBorder(), "Serial / USB Settings"),
                new EmptyBorder(6, 8, 6, 8)));
        centrePanel.add(serialPanel, BorderLayout.CENTER);
        buildSerialPanel();

        // ── Status bar ────────────────────────────────────────────────────────
        lblStatus = new JLabel(" ");
        lblStatus.setFont(new Font("Tahoma", Font.PLAIN, 11));
        lblStatus.setBorder(new EmptyBorder(2, 4, 2, 4));
        outer.add(lblStatus, BorderLayout.SOUTH);

        // ── Button row ────────────────────────────────────────────────────────
        JPanel btnRow = new JPanel();
        getContentPane().add(btnRow, BorderLayout.SOUTH);

        JButton btnTest = new JButton("Test Connection");
        btnTest.addActionListener(e -> onTest());
        btnRow.add(btnTest);

        JButton btnSave = new JButton("Save");
        btnSave.addActionListener(e -> { onSave(); dispose(); });
        btnRow.add(btnSave);

        JButton btnCancel = new JButton("Cancel");
        btnCancel.addActionListener(e -> dispose());
        btnRow.add(btnCancel);

        // Disconnect button
        JButton btnDisconnect = new JButton("Disconnect");
        btnDisconnect.addActionListener(e -> {
            RigController.getInstance().disconnect();
            setStatus("Disconnected.", Color.DARK_GRAY);
        });
        btnRow.add(btnDisconnect);

        // ── Toggle panel visibility on radio change ───────────────────────────
        ActionListener toggleListener = e -> updatePanelVisibility();
        radioRigctld.addActionListener(toggleListener);
        radioSerial.addActionListener(toggleListener);

        // ── Populate fields from saved prefs ──────────────────────────────────
        loadPrefs();
        updatePanelVisibility();
    }

    // ── Panel builders ────────────────────────────────────────────────────────

    private void buildRigctldPanel() {
        GridBagConstraints lc = labelConstraints();
        GridBagConstraints fc = fieldConstraints();

        lc.gridy = fc.gridy = 0;
        rigctldPanel.add(bold("Host"), lc);
        textHost = new JTextField("localhost", 16);
        rigctldPanel.add(textHost, fc);

        lc.gridy = fc.gridy = 1;
        rigctldPanel.add(bold("Port"), lc);
        spinnerPort = new JSpinner(new SpinnerNumberModel(4532, 1, 65535, 1));
        ((JSpinner.DefaultEditor) spinnerPort.getEditor()).getTextField().setColumns(6);
        rigctldPanel.add(spinnerPort, fc);
    }

    private void buildSerialPanel() {
        GridBagConstraints lc = labelConstraints();
        GridBagConstraints fc = fieldConstraints();

        lc.gridy = fc.gridy = 0;
        serialPanel.add(bold("Port"), lc);
        String[] ports = RigController.availableSerialPorts();
        comboSerialPort = new JComboBox<>(ports.length > 0 ? ports : new String[]{"(none detected)"});
        comboSerialPort.setEditable(true); // allow typing a path manually
        serialPanel.add(comboSerialPort, fc);

        lc.gridy = fc.gridy = 1;
        serialPanel.add(bold("Baud Rate"), lc);
        String[] baudStrs = new String[BAUD_RATES.length];
        for (int i = 0; i < BAUD_RATES.length; i++) baudStrs[i] = String.valueOf(BAUD_RATES[i]);
        comboBaud = new JComboBox<>(baudStrs);
        comboBaud.setSelectedItem("9600");
        serialPanel.add(comboBaud, fc);

        lc.gridy = fc.gridy = 2;
        serialPanel.add(bold("Protocol"), lc);
        comboProtocol = new JComboBox<>(SerialProtocol.values());
        comboProtocol.addActionListener(e -> lblIcomAddress.setVisible(
                comboProtocol.getSelectedItem() == SerialProtocol.ICOM));
        serialPanel.add(comboProtocol, fc);

        lc.gridy = fc.gridy = 3;
        lblIcomAddress = new JLabel("CI-V Address (hex)");
        lblIcomAddress.setFont(lblIcomAddress.getFont().deriveFont(Font.BOLD));
        serialPanel.add(lblIcomAddress, lc);
        textIcomAddress = new JTextField("A4", 6);
        serialPanel.add(textIcomAddress, fc);
        lblIcomAddress.setVisible(false);
        textIcomAddress.setVisible(false);
    }

    // ── Actions ───────────────────────────────────────────────────────────────

    private void onTest() {
        setStatus("Testing…", Color.DARK_GRAY);
        new Thread(() -> {
            boolean ok;
            if (radioRigctld.isSelected()) {
                String host = textHost.getText().trim();
                int port = (Integer) spinnerPort.getValue();
                ok = RigController.getInstance().connectRigctld(host, port);
            } else {
                String portName = comboSerialPort.getSelectedItem().toString().trim();
                int baud = Integer.parseInt(comboBaud.getSelectedItem().toString());
                SerialProtocol proto = (SerialProtocol) comboProtocol.getSelectedItem();
                ok = RigController.getInstance().connectSerial(portName, baud, proto);
            }
            final boolean connected = ok;
            SwingUtilities.invokeLater(() -> {
                if (connected) {
                    setStatus("Connected successfully!", new Color(0, 130, 0));
                } else {
                    setStatus("Connection failed. Check settings and try again.", Color.RED);
                }
            });
        }, "CATSettingsDlg-test").start();
    }

    private void onSave() {
        String type = radioRigctld.isSelected() ? "RIGCTLD" : "SERIAL";
        Preferences.save(Preferences.PREF_CAT_TYPE, type);
        Preferences.save(Preferences.PREF_CAT_RIGCTLD_HOST, textHost.getText().trim());
        Preferences.save(Preferences.PREF_CAT_RIGCTLD_PORT, spinnerPort.getValue().toString());
        Preferences.save(Preferences.PREF_CAT_SERIAL_PORT,
                comboSerialPort.getSelectedItem() != null
                        ? comboSerialPort.getSelectedItem().toString().trim() : "");
        Preferences.save(Preferences.PREF_CAT_SERIAL_BAUD,
                comboBaud.getSelectedItem().toString());
        SerialProtocol proto = (SerialProtocol) comboProtocol.getSelectedItem();
        Preferences.save(Preferences.PREF_CAT_SERIAL_PROTOCOL,
                proto != null ? proto.name() : SerialProtocol.YAESU.name());
        Preferences.save(Preferences.PREF_CAT_ICOM_ADDRESS, textIcomAddress.getText().trim());
    }

    private void loadPrefs() {
        String type     = Preferences.getOne(Preferences.PREF_CAT_TYPE);
        String host     = Preferences.getOne(Preferences.PREF_CAT_RIGCTLD_HOST);
        String port     = Preferences.getOne(Preferences.PREF_CAT_RIGCTLD_PORT);
        String serPort  = Preferences.getOne(Preferences.PREF_CAT_SERIAL_PORT);
        String baud     = Preferences.getOne(Preferences.PREF_CAT_SERIAL_BAUD);
        String proto    = Preferences.getOne(Preferences.PREF_CAT_SERIAL_PROTOCOL);
        String icomAddr = Preferences.getOne(Preferences.PREF_CAT_ICOM_ADDRESS);

        if ("SERIAL".equals(type)) {
            radioSerial.setSelected(true);
        } else {
            radioRigctld.setSelected(true);
        }
        if (host != null && !host.isEmpty()) textHost.setText(host);
        if (port != null && !port.isEmpty()) {
            try { spinnerPort.setValue(Integer.parseInt(port)); } catch (NumberFormatException ignored) {}
        }
        if (serPort != null && !serPort.isEmpty()) comboSerialPort.setSelectedItem(serPort);
        if (baud   != null && !baud.isEmpty())   comboBaud.setSelectedItem(baud);
        if (proto  != null && !proto.isEmpty()) {
            try {
                comboProtocol.setSelectedItem(SerialProtocol.valueOf(proto));
            } catch (IllegalArgumentException ignored) {}
        }
        if (icomAddr != null) textIcomAddress.setText(icomAddr);

        // Show CI-V address only when Icom is selected
        lblIcomAddress.setVisible(comboProtocol.getSelectedItem() == SerialProtocol.ICOM);
        textIcomAddress.setVisible(comboProtocol.getSelectedItem() == SerialProtocol.ICOM);
    }

    private void updatePanelVisibility() {
        boolean isRigctld = radioRigctld.isSelected();
        rigctldPanel.setVisible(isRigctld);
        serialPanel.setVisible(!isRigctld);
        revalidate();
        repaint();
    }

    private void setStatus(String text, Color color) {
        lblStatus.setText(text);
        lblStatus.setForeground(color);
    }

    // ── Layout helpers ────────────────────────────────────────────────────────

    private static JLabel bold(String text) {
        JLabel l = new JLabel(text);
        l.setFont(l.getFont().deriveFont(Font.BOLD));
        return l;
    }

    private static GridBagConstraints labelConstraints() {
        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0;
        gc.anchor = GridBagConstraints.EAST;
        gc.insets = new Insets(3, 0, 3, 8);
        return gc;
    }

    private static GridBagConstraints fieldConstraints() {
        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 1;
        gc.anchor = GridBagConstraints.WEST;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 1.0;
        gc.insets = new Insets(3, 0, 3, 0);
        return gc;
    }
}
