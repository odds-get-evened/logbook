package org.qualsh.lb.view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.table.AbstractTableModel;

import org.qualsh.lb.dx.DXClusterClient;
import org.qualsh.lb.dx.DXSpot;
import org.qualsh.lb.rig.RigController;
import org.qualsh.lb.util.Preferences;

/**
 * Panel showing real-time DX cluster spots in a table.
 *
 * <p>Connects to a Telnet-based DX cluster and displays incoming spots.
 * Double-clicking a spot populates the frequency field in the log entry
 * form and, if the CAT rig is connected, commands the radio to that frequency.
 */
public class DXClusterPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private static final int MAX_SPOTS = 300;

    private final DXClusterClient client     = new DXClusterClient();
    private final SpotsTableModel tableModel = new SpotsTableModel();

    private JTable  table;
    private JButton btnConnect;
    private JLabel  lblStatus;

    private LogInteraction logInteraction;

    public DXClusterPanel() {
        setLayout(new BorderLayout(0, 4));
        setBorder(new EmptyBorder(5, 5, 5, 5));

        // ── Top bar ───────────────────────────────────────────────────────────
        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));

        btnConnect = new JButton("Connect");
        btnConnect.addActionListener(e -> onConnectToggle());
        topBar.add(btnConnect);

        lblStatus = new JLabel("\u25CF"); // ● filled circle
        lblStatus.setForeground(Color.LIGHT_GRAY);
        lblStatus.setToolTipText("DX Cluster: not connected");
        topBar.add(lblStatus);

        JButton btnSettings = new JButton("Settings\u2026");
        btnSettings.addActionListener(e -> {
            Window win = SwingUtilities.getWindowAncestor(DXClusterPanel.this);
            DXClusterSettingsDialog dlg = new DXClusterSettingsDialog(
                    win instanceof JFrame ? (JFrame) win : null);
            dlg.setVisible(true);
        });
        topBar.add(btnSettings);

        JLabel lblHint = new JLabel("Double-click a spot to tune the radio and populate the entry form.");
        lblHint.setFont(lblHint.getFont().deriveFont(Font.ITALIC, 10f));
        topBar.add(lblHint);

        add(topBar, BorderLayout.NORTH);

        // ── Spots table ───────────────────────────────────────────────────────
        table = new JTable(tableModel);
        table.setFillsViewportHeight(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);

        // Column preferred widths
        table.getColumnModel().getColumn(0).setPreferredWidth(55);   // Time
        table.getColumnModel().getColumn(0).setMaxWidth(65);
        table.getColumnModel().getColumn(1).setPreferredWidth(80);   // Freq
        table.getColumnModel().getColumn(1).setMaxWidth(100);
        table.getColumnModel().getColumn(2).setPreferredWidth(95);   // Callsign
        table.getColumnModel().getColumn(2).setMaxWidth(120);
        table.getColumnModel().getColumn(3).setPreferredWidth(95);   // Spotter
        table.getColumnModel().getColumn(3).setMaxWidth(120);
        // column 4 (Comment) gets remaining space via AUTO_RESIZE_LAST_COLUMN

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int row = table.getSelectedRow();
                    if (row >= 0) {
                        onSpotDoubleClicked(tableModel.getSpot(row));
                    }
                }
            }
        });

        add(new JScrollPane(table), BorderLayout.CENTER);

        // ── Wire up client listeners ──────────────────────────────────────────
        client.addStatusListener(conn -> SwingUtilities.invokeLater(() -> {
            if (conn) {
                lblStatus.setForeground(new Color(0, 160, 0));
                lblStatus.setToolTipText("DX Cluster: connected");
                btnConnect.setText("Disconnect");
            } else {
                lblStatus.setForeground(Color.LIGHT_GRAY);
                lblStatus.setToolTipText("DX Cluster: not connected");
                btnConnect.setText("Connect");
            }
        }));

        client.addSpotListener(spot -> SwingUtilities.invokeLater(() -> tableModel.addSpot(spot)));
    }

    // ── Actions ───────────────────────────────────────────────────────────────

    private void onConnectToggle() {
        if (client.isConnected()) {
            client.disconnect();
            return;
        }

        String host     = Preferences.getOne(Preferences.PREF_DX_CLUSTER_HOST);
        String portStr  = Preferences.getOne(Preferences.PREF_DX_CLUSTER_PORT);
        String callsign = Preferences.getOne(Preferences.PREF_DX_CLUSTER_CALLSIGN);

        if (host == null || host.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "No DX Cluster host configured.\nClick Settings\u2026 to set up the connection.",
                    "DX Cluster", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int port = 7300;
        if (portStr != null && !portStr.isEmpty()) {
            try { port = Integer.parseInt(portStr); } catch (NumberFormatException ignored) {}
        }

        final String fHost     = host;
        final int    fPort     = port;
        final String fCallsign = callsign;

        btnConnect.setEnabled(false);
        new Thread(() -> {
            boolean ok = client.connect(fHost, fPort, fCallsign);
            SwingUtilities.invokeLater(() -> {
                btnConnect.setEnabled(true);
                if (!ok) {
                    lblStatus.setForeground(Color.RED);
                    lblStatus.setToolTipText("DX Cluster: connection failed");
                    JOptionPane.showMessageDialog(DXClusterPanel.this,
                            "Could not connect to DX Cluster at " + fHost + ":" + fPort + "\n"
                            + "Check your settings.",
                            "DX Cluster Connection Failed",
                            JOptionPane.WARNING_MESSAGE);
                }
            });
        }, "DXCluster-connect").start();
    }

    private void onSpotDoubleClicked(DXSpot spot) {
        double freq = spot.getFrequency();

        // Format: drop trailing zeros (e.g. 14025.0 → "14025", 14025.5 → "14025.5")
        String freqStr;
        if (freq == Math.floor(freq)) {
            freqStr = String.valueOf((long) freq);
        } else {
            freqStr = String.format("%.3f", freq).replaceAll("0+$", "");
        }

        // Populate log entry form and switch to Entry tab
        if (logInteraction != null) {
            logInteraction.getTextFrequency().setText(freqStr);
            logInteraction.getTabbedPane().setSelectedIndex(1);
        }

        // Command the radio if CAT is connected
        RigController rig = RigController.getInstance();
        if (rig.isConnected()) {
            new Thread(() -> rig.setFrequency(freq), "DXCluster-setFreq").start();
        }
    }

    // ── Wiring ────────────────────────────────────────────────────────────────

    public void setLogInteraction(LogInteraction logInteraction) {
        this.logInteraction = logInteraction;
    }

    /** Disconnect from the cluster (called on app close). */
    public void shutdown() {
        if (client.isConnected()) client.disconnect();
    }

    // ── Table model ───────────────────────────────────────────────────────────

    private static class SpotsTableModel extends AbstractTableModel {

        private static final long serialVersionUID = 1L;
        private static final String[] COLUMNS = { "Time", "Freq (kHz)", "Callsign", "Spotter", "Comment" };

        private final List<DXSpot> spots = new ArrayList<>();

        void addSpot(DXSpot spot) {
            spots.add(0, spot); // newest first
            if (spots.size() > MAX_SPOTS) {
                spots.remove(spots.size() - 1);
            }
            fireTableDataChanged();
        }

        DXSpot getSpot(int row) {
            return spots.get(row);
        }

        @Override public int    getRowCount()    { return spots.size(); }
        @Override public int    getColumnCount() { return COLUMNS.length; }
        @Override public String getColumnName(int col) { return COLUMNS[col]; }

        @Override
        public Class<?> getColumnClass(int col) {
            return (col == 1) ? Double.class : String.class;
        }

        @Override
        public Object getValueAt(int row, int col) {
            DXSpot s = spots.get(row);
            switch (col) {
                case 0: return s.getTime();
                case 1: return s.getFrequency();
                case 2: return s.getCallsign();
                case 3: return s.getSpotter();
                case 4: return s.getComment();
                default: return "";
            }
        }
    }
}
