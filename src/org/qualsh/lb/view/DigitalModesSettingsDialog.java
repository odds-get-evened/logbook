package org.qualsh.lb.view;

import org.qualsh.lb.digital.AudioRouter;
import org.qualsh.lb.digital.PttController;
import org.qualsh.lb.digital.PttController.PttMethod;
import org.qualsh.lb.digital.WsjtxUdpListener;
import org.qualsh.lb.rig.RigController;
import org.qualsh.lb.util.Preferences;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;

/**
 * Settings dialog for digital-mode operation.
 *
 * <p>Covers three concerns:
 * <ol>
 *   <li><b>PTT</b> – method (VOX / RTS / DTR / CAT) and dedicated PTT port.</li>
 *   <li><b>Audio</b> – capture device (Radio → App) and playback device (App → Radio).</li>
 *   <li><b>WSJT-X / JS8Call integration</b> – UDP port for QSO-logged messages.</li>
 * </ol>
 */
public class DigitalModesSettingsDialog extends JDialog {

    private static final long serialVersionUID = 1L;

    // PTT widgets
    private JComboBox<PttMethod> comboPttMethod;
    private JComboBox<String>    comboPttPort;
    private JLabel               lblPttPort;

    // Audio widgets
    private JComboBox<String> comboCaptureDevice;
    private JComboBox<String> comboPlaybackDevice;

    // WSJT-X UDP
    private JSpinner spinnerWsjtxPort;
    private JSpinner spinnerJs8callPort;

    // Status
    private JLabel lblStatus;

    public DigitalModesSettingsDialog(JFrame owner) {
        super(owner, "Digital Modes Settings", true);
        setMinimumSize(new Dimension(480, 460));
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        setSize((int)(screen.getWidth() * 0.35), (int)(screen.getHeight() * 0.55));
        setLocationRelativeTo(owner);

        JPanel outer = new JPanel(new BorderLayout(0, 8));
        outer.setBorder(new EmptyBorder(10, 10, 10, 10));
        getContentPane().add(outer, BorderLayout.CENTER);

        JPanel centre = new JPanel();
        centre.setLayout(new BoxLayout(centre, BoxLayout.Y_AXIS));
        outer.add(centre, BorderLayout.CENTER);

        centre.add(buildPttPanel());
        centre.add(Box.createVerticalStrut(6));
        centre.add(buildAudioPanel());
        centre.add(Box.createVerticalStrut(6));
        centre.add(buildWsjtxPanel());

        lblStatus = new JLabel(" ");
        lblStatus.setBorder(new EmptyBorder(2, 4, 2, 4));
        outer.add(lblStatus, BorderLayout.SOUTH);

        // Button row
        JPanel btnRow = new JPanel();
        getContentPane().add(btnRow, BorderLayout.SOUTH);

        JButton btnSave = new JButton("Save & Apply");
        btnSave.addActionListener(e -> { onSave(); dispose(); });
        btnRow.add(btnSave);

        JButton btnCancel = new JButton("Cancel");
        btnCancel.addActionListener(e -> dispose());
        btnRow.add(btnCancel);

        loadPrefs();
    }

    // ── Panel builders ────────────────────────────────────────────────────────

    private JPanel buildPttPanel() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(new CompoundBorder(
                new TitledBorder(new EtchedBorder(), "PTT (Push-To-Talk)"),
                new EmptyBorder(6, 8, 6, 8)));
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));

        GridBagConstraints lc = lc();
        GridBagConstraints fc = fc();

        lc.gridy = fc.gridy = 0;
        p.add(bold("Method"), lc);
        comboPttMethod = new JComboBox<>(PttMethod.values());
        comboPttMethod.addActionListener(e -> updatePttPortVisibility());
        p.add(comboPttMethod, fc);

        lc.gridy = fc.gridy = 1;
        lblPttPort = bold("PTT Port");
        p.add(lblPttPort, lc);
        String[] ports = RigController.availableSerialPorts();
        comboPttPort = new JComboBox<>(ports.length > 0 ? ports : new String[]{"(none)"});
        comboPttPort.setEditable(true);
        p.add(comboPttPort, fc);

        JLabel note = new JLabel(
                "<html><i>RTS/DTR: PTT port may be the same as CAT port or a separate adapter.</i></html>");
        note.setFont(note.getFont().deriveFont(10f));
        GridBagConstraints nc = fc();
        nc.gridy = 2; nc.gridx = 0; nc.gridwidth = 2;
        nc.insets = new Insets(2, 0, 0, 0);
        p.add(note, nc);

        return p;
    }

    private JPanel buildAudioPanel() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(new CompoundBorder(
                new TitledBorder(new EtchedBorder(), "Audio Routing"),
                new EmptyBorder(6, 8, 6, 8)));
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));

        GridBagConstraints lc = lc();
        GridBagConstraints fc = fc();

        lc.gridy = fc.gridy = 0;
        p.add(bold("Capture (Radio → App)"), lc);
        String[] captureDevs = AudioRouter.availableCaptureDevices();
        comboCaptureDevice = new JComboBox<>(captureDevs.length > 0
                ? captureDevs : new String[]{"(system default)"});
        comboCaptureDevice.setEditable(false);
        p.add(comboCaptureDevice, fc);

        lc.gridy = fc.gridy = 1;
        p.add(bold("Playback (App → Radio)"), lc);
        String[] playDevs = AudioRouter.availablePlaybackDevices();
        comboPlaybackDevice = new JComboBox<>(playDevs.length > 0
                ? playDevs : new String[]{"(system default)"});
        comboPlaybackDevice.setEditable(false);
        p.add(comboPlaybackDevice, fc);

        JLabel note = new JLabel(
                "<html><i>Select the USB Audio CODEC / soundcard wired to your radio interface.</i></html>");
        note.setFont(note.getFont().deriveFont(10f));
        GridBagConstraints nc = fc();
        nc.gridy = 2; nc.gridx = 0; nc.gridwidth = 2;
        nc.insets = new Insets(2, 0, 0, 0);
        p.add(note, nc);

        return p;
    }

    private JPanel buildWsjtxPanel() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(new CompoundBorder(
                new TitledBorder(new EtchedBorder(), "WSJT-X / JS8Call Integration (UDP)"),
                new EmptyBorder(6, 8, 6, 8)));
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 140));

        GridBagConstraints lc = lc();
        GridBagConstraints fc = fc();

        lc.gridy = fc.gridy = 0;
        p.add(bold("WSJT-X UDP port"), lc);
        spinnerWsjtxPort = new JSpinner(new SpinnerNumberModel(
                WsjtxUdpListener.DEFAULT_PORT, 1024, 65535, 1));
        ((JSpinner.DefaultEditor) spinnerWsjtxPort.getEditor()).getTextField().setColumns(6);
        p.add(spinnerWsjtxPort, fc);

        lc.gridy = fc.gridy = 1;
        p.add(bold("JS8Call UDP port"), lc);
        spinnerJs8callPort = new JSpinner(new SpinnerNumberModel(
                WsjtxUdpListener.JS8CALL_PORT, 1024, 65535, 1));
        ((JSpinner.DefaultEditor) spinnerJs8callPort.getEditor()).getTextField().setColumns(6);
        p.add(spinnerJs8callPort, fc);

        JLabel note = new JLabel(
                "<html><i>In WSJT-X: File → Settings → Reporting → UDP Server port.</i></html>");
        note.setFont(note.getFont().deriveFont(10f));
        GridBagConstraints nc = fc();
        nc.gridy = 2; nc.gridx = 0; nc.gridwidth = 2;
        nc.insets = new Insets(2, 0, 0, 0);
        p.add(note, nc);

        return p;
    }

    // ── Actions ───────────────────────────────────────────────────────────────

    private void onSave() {
        PttMethod method = (PttMethod) comboPttMethod.getSelectedItem();
        Preferences.save(Preferences.PREF_DIGITAL_PTT_METHOD,
                method != null ? method.name() : PttMethod.VOX.name());

        Object pttPortObj = comboPttPort.getSelectedItem();
        Preferences.save(Preferences.PREF_DIGITAL_PTT_PORT,
                pttPortObj != null ? pttPortObj.toString().trim() : "");

        Object captureObj = comboCaptureDevice.getSelectedItem();
        Preferences.save(Preferences.PREF_DIGITAL_CAPTURE_DEVICE,
                captureObj != null ? captureObj.toString() : "");

        Object playbackObj = comboPlaybackDevice.getSelectedItem();
        Preferences.save(Preferences.PREF_DIGITAL_PLAYBACK_DEVICE,
                playbackObj != null ? playbackObj.toString() : "");

        Preferences.save(Preferences.PREF_DIGITAL_WSJTX_UDP_PORT,
                spinnerWsjtxPort.getValue().toString());
        Preferences.save(Preferences.PREF_DIGITAL_JS8CALL_UDP_PORT,
                spinnerJs8callPort.getValue().toString());

        // Apply PTT port immediately
        PttController.getInstance().closePttPort();
        PttController.getInstance().openPttPort();
    }

    private void loadPrefs() {
        String pttMethod = Preferences.getOne(Preferences.PREF_DIGITAL_PTT_METHOD);
        if (pttMethod != null) {
            try { comboPttMethod.setSelectedItem(PttMethod.valueOf(pttMethod)); }
            catch (IllegalArgumentException ignored) {}
        }

        String pttPort = Preferences.getOne(Preferences.PREF_DIGITAL_PTT_PORT);
        if (pttPort != null && !pttPort.isEmpty()) comboPttPort.setSelectedItem(pttPort);

        String capDev = Preferences.getOne(Preferences.PREF_DIGITAL_CAPTURE_DEVICE);
        if (capDev != null && !capDev.isEmpty()) comboCaptureDevice.setSelectedItem(capDev);

        String playDev = Preferences.getOne(Preferences.PREF_DIGITAL_PLAYBACK_DEVICE);
        if (playDev != null && !playDev.isEmpty()) comboPlaybackDevice.setSelectedItem(playDev);

        String wsjtxPort = Preferences.getOne(Preferences.PREF_DIGITAL_WSJTX_UDP_PORT);
        if (wsjtxPort != null && !wsjtxPort.isEmpty()) {
            try { spinnerWsjtxPort.setValue(Integer.parseInt(wsjtxPort)); }
            catch (NumberFormatException ignored) {}
        }

        String js8Port = Preferences.getOne(Preferences.PREF_DIGITAL_JS8CALL_UDP_PORT);
        if (js8Port != null && !js8Port.isEmpty()) {
            try { spinnerJs8callPort.setValue(Integer.parseInt(js8Port)); }
            catch (NumberFormatException ignored) {}
        }

        updatePttPortVisibility();
    }

    private void updatePttPortVisibility() {
        PttMethod m = (PttMethod) comboPttMethod.getSelectedItem();
        boolean showPort = m == PttMethod.RTS || m == PttMethod.DTR;
        lblPttPort.setVisible(showPort);
        comboPttPort.setVisible(showPort);
        revalidate();
        repaint();
    }

    // ── Layout helpers ────────────────────────────────────────────────────────

    private static JLabel bold(String text) {
        JLabel l = new JLabel(text);
        l.setFont(l.getFont().deriveFont(Font.BOLD));
        return l;
    }

    private static GridBagConstraints lc() {
        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx  = 0;
        gc.anchor = GridBagConstraints.EAST;
        gc.insets = new Insets(3, 0, 3, 8);
        return gc;
    }

    private static GridBagConstraints fc() {
        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx   = 1;
        gc.anchor  = GridBagConstraints.WEST;
        gc.fill    = GridBagConstraints.HORIZONTAL;
        gc.weightx = 1.0;
        gc.insets  = new Insets(3, 0, 3, 0);
        return gc;
    }
}
