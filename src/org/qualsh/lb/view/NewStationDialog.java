package org.qualsh.lb.view;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Toolkit;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.JPanel;

import java.awt.BorderLayout;
import java.awt.GridBagLayout;

import javax.swing.border.EmptyBorder;
import javax.swing.JLabel;

import java.awt.GridBagConstraints;

import javax.swing.JTextField;

import java.awt.Insets;

import javax.swing.JButton;

import org.qualsh.lb.data.Data;
import org.qualsh.lb.data.ViewStationsModel;
import org.qualsh.lb.station.Station;
import org.qualsh.lb.util.FormError;

import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;

public class NewStationDialog extends JDialog {

	private static final long serialVersionUID = 3651515815756685914L;
	private JTextField textStationName;
	private LogInteraction logInteraction;
	private JPanel errorPanel;

	public NewStationDialog(JFrame frame) {
		super(frame);
		
		this.setMinimumSize(new Dimension(250, 125));
		this.setTitle("New Station");
		this.setModal(true);
		
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		int winW = (int) (screenSize.getWidth() * 0.25);
		int winH = (int) (screenSize.getHeight() * 0.3);
		setSize(winW, winH);
		
		int winX = (int) ((screenSize.getWidth() - getSize().getWidth()) / 2);
		int winY = (int) ((screenSize.getHeight() - getSize().getHeight()) / 2);
		setLocation(winX, winY);
		
		JPanel panel = new JPanel();
		panel.setBorder(new EmptyBorder(10, 10, 10, 10));
		getContentPane().add(panel, BorderLayout.CENTER);
		GridBagLayout gbl_panel = new GridBagLayout();
		gbl_panel.columnWidths = new int[]{0, 0, 0};
		gbl_panel.rowHeights = new int[]{12, 0, 0, 0};
		gbl_panel.columnWeights = new double[]{0.0, 1.0, Double.MIN_VALUE};
		gbl_panel.rowWeights = new double[]{0.0, 0.0, 0.0, Double.MIN_VALUE};
		panel.setLayout(gbl_panel);
		
		errorPanel = new JPanel();
		GridBagConstraints gbc_errorPanel = new GridBagConstraints();
		gbc_errorPanel.insets = new Insets(0, 0, 5, 0);
		gbc_errorPanel.fill = GridBagConstraints.BOTH;
		gbc_errorPanel.gridx = 1;
		gbc_errorPanel.gridy = 0;
		panel.add(errorPanel, gbc_errorPanel);
		
		JLabel lblName = new JLabel("Name");
		lblName.setFont(new Font("Tahoma", Font.BOLD, 11));
		GridBagConstraints gbc_lblName = new GridBagConstraints();
		gbc_lblName.insets = new Insets(0, 0, 5, 5);
		gbc_lblName.anchor = GridBagConstraints.EAST;
		gbc_lblName.gridx = 0;
		gbc_lblName.gridy = 1;
		panel.add(lblName, gbc_lblName);
		
		textStationName = new JTextField();
		GridBagConstraints gbc_textStationName = new GridBagConstraints();
		gbc_textStationName.insets = new Insets(0, 0, 5, 0);
		gbc_textStationName.fill = GridBagConstraints.HORIZONTAL;
		gbc_textStationName.gridx = 1;
		gbc_textStationName.gridy = 1;
		panel.add(textStationName, gbc_textStationName);
		textStationName.setColumns(10);
		
		JPanel panel_1 = new JPanel();
		GridBagConstraints gbc_panel_1 = new GridBagConstraints();
		gbc_panel_1.fill = GridBagConstraints.BOTH;
		gbc_panel_1.gridx = 1;
		gbc_panel_1.gridy = 2;
		panel.add(panel_1, gbc_panel_1);
		
		JButton btnOk = new JButton("OK");
		btnOk.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				NewStationDialog.this.process();
				NewStationDialog.this.textStationName.setText("");
				NewStationDialog.this.setVisible(false);
			}
			
		});
		panel_1.add(btnOk);
		
		JButton btnCancel = new JButton("Cancel");
		btnCancel.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				NewStationDialog.this.reset();
				NewStationDialog.this.textStationName.setText("");
				NewStationDialog.this.setVisible(false);
			}
			
		});
		panel_1.add(btnCancel);
		
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (ClassNotFoundException e1) {
			e1.printStackTrace();
		} catch (InstantiationException e1) {
			e1.printStackTrace();
		} catch (IllegalAccessException e1) {
			e1.printStackTrace();
		} catch (UnsupportedLookAndFeelException e1) {
			e1.printStackTrace();
		}
		
		this.pack();
	}
	
	private ArrayList<FormError> getErrors() {
		ArrayList<FormError> errors = new ArrayList<FormError>();
		
		if(textStationName.getText().isEmpty()) {
			errors.add(new FormError(textStationName, "Station name is required."));
		}
		
		return errors;
	}
	
	private void process() {
		ArrayList<FormError> errors = getErrors();
		
		getErrorPanel().removeAll();
		getErrorPanel().setVisible(false);
		
		if(errors.isEmpty()) {
			Connection conn = Data.getConnection();
			
			Station stn = new Station();
			stn.setMetaName(this.textStationName.getText().replaceAll("[\\W\\s]+", "_").toLowerCase());
			stn.setTitle(this.textStationName.getText());
			
			PreparedStatement ps = null;
			try {
				ps = conn.prepareStatement("INSERT INTO stations (meta_name, title) VALUES (?, ?)");
				ps.setString(1, stn.getMetaName());
				ps.setString(2, stn.getTitle());
				ps.execute();
			} catch (SQLException e) {
				e.printStackTrace();
			} finally {
				try {
					ps.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
				
				try {
					conn.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
			
			this.reset();
			this.setVisible(false);
		} else {
			getErrorPanel().setVisible(true);
			for(FormError fe : errors) {
				
				JLabel errLabel = new JLabel(fe.getMessage());
				errLabel.setFont(new Font("Tahoma", Font.BOLD, 11));
				errLabel.setForeground(Color.RED);
				getErrorPanel().add(errLabel);
			}
		}
		
	}
	
	private void reset() {
		this.textStationName.setText("");
	}

	public void setLogInteraction(LogInteraction logInteraction) {
		this.logInteraction = logInteraction;
	}

	public LogInteraction getLogInteraction() {
		return logInteraction;
	}

	public JPanel getErrorPanel() {
		return errorPanel;
	}

	public void setErrorPanel(JPanel errorPanel) {
		this.errorPanel = errorPanel;
	}

}
