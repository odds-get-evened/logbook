package org.qualsh.lb.view;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.border.EmptyBorder;

import org.qualsh.lb.data.LogsModel;

public class LogsPanel extends JPanel {
	
	private LogsTable logsTable;

	/**
	 *
	 */
	private static final long serialVersionUID = 4888587192397257257L;
	private JTextField textSearchLogs;

	private JButton btnReset;
			
	public LogsPanel() {
		
		setBorder(new EmptyBorder(0, 0, 0, 0));
		setLayout(new BorderLayout(0, 0));
		
		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setBorder(new EmptyBorder(1, 1, 1, 1));
		add(scrollPane, BorderLayout.CENTER);
		
		final LogsModel lm = new LogsModel();
		logsTable = new LogsTable(lm);
		logsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		scrollPane.setViewportView(logsTable);
		
		JPanel panel = new JPanel();
		panel.setBorder(new EmptyBorder(5, 5, 5, 5));
		add(panel, BorderLayout.NORTH);
		GridBagLayout gbl_panel = new GridBagLayout();
		gbl_panel.columnWidths = new int[]{64, 281, 0, 64, 0};
		gbl_panel.rowHeights = new int[]{23, 0};
		gbl_panel.columnWeights = new double[]{0.0, 1.0, 0.0, 0.0, Double.MIN_VALUE};
		gbl_panel.rowWeights = new double[]{0.0, Double.MIN_VALUE};
		panel.setLayout(gbl_panel);
		
		JLabel lblSearchLogs = new JLabel("Search Logs");
		GridBagConstraints gbc_lblSearchLogs = new GridBagConstraints();
		gbc_lblSearchLogs.anchor = GridBagConstraints.WEST;
		gbc_lblSearchLogs.fill = GridBagConstraints.VERTICAL;
		gbc_lblSearchLogs.insets = new Insets(0, 0, 0, 5);
		gbc_lblSearchLogs.gridx = 0;
		gbc_lblSearchLogs.gridy = 0;
		panel.add(lblSearchLogs, gbc_lblSearchLogs);
		
		btnReset = new JButton("Reset");
		btnReset.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				LogsModel lm = (LogsModel) getLogsTable().getModel();
				lm.getData().clear();
				lm.setData(lm.getLogList());

				getLogsTable().updateUI();
				textSearchLogs.setText("");
			}

		});
		
		textSearchLogs = new JTextField();
		textSearchLogs.addKeyListener(new KeyListener() {

			public void keyTyped(KeyEvent e) {}

			public void keyPressed(KeyEvent e) {}

			public void keyReleased(KeyEvent e) {
				JTextField tf = (JTextField) e.getSource();
				
				searchLogs(tf.getText());
			}
			
		});
		GridBagConstraints gbc_textSearchLogs = new GridBagConstraints();
		gbc_textSearchLogs.fill = GridBagConstraints.BOTH;
		gbc_textSearchLogs.insets = new Insets(0, 0, 0, 5);
		gbc_textSearchLogs.gridx = 1;
		gbc_textSearchLogs.gridy = 0;
		panel.add(textSearchLogs, gbc_textSearchLogs);
		textSearchLogs.setColumns(10);
		
		JButton btnThisHour = new JButton("This Hour");
		btnThisHour.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				LogsModel lm = (LogsModel) LogsPanel.this.getLogsTable().getModel();
				lm.getData().clear();
				lm.getThisHour();

				LogsPanel.this.getLogsTable().updateUI();
			}
		});
		GridBagConstraints gbc_btnThisHour = new GridBagConstraints();
		gbc_btnThisHour.insets = new Insets(0, 0, 0, 5);
		gbc_btnThisHour.gridx = 2;
		gbc_btnThisHour.gridy = 0;
		panel.add(btnThisHour, gbc_btnThisHour);
		GridBagConstraints gbc_btnReset = new GridBagConstraints();
		gbc_btnReset.fill = GridBagConstraints.BOTH;
		gbc_btnReset.gridx = 3;
		gbc_btnReset.gridy = 0;
		panel.add(btnReset, gbc_btnReset);
	}

	private void searchLogs(String text) {
		LogsModel lm = (LogsModel) getLogsTable().getModel();
		lm.getData().clear();
		lm.searchAll(text);
		getLogsTable().updateUI();
	}

	public LogsPanel(LayoutManager layout) {
		super(layout);
	}

	public LogsPanel(boolean isDoubleBuffered) {
		super(isDoubleBuffered);
	}

	public LogsPanel(LayoutManager layout, boolean isDoubleBuffered) {
		super(layout, isDoubleBuffered);
	}

	public LogsTable getLogsTable() {
		return logsTable;
	}

	public void setLogsTable(LogsTable logsTable) {
		this.logsTable = logsTable;
	}

}

