package org.qualsh.lb.view;

import java.awt.Dimension;
import java.awt.Toolkit;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;

import java.awt.BorderLayout;
import java.awt.GridBagLayout;

import javax.swing.border.EmptyBorder;
import javax.swing.JLabel;

import org.qualsh.lb.App;

import java.awt.Desktop;
import java.awt.GridBagConstraints;
import java.awt.Font;
import java.awt.Insets;
import java.time.Year;

import org.jdesktop.swingx.JXLabel;

import javax.swing.JButton;

import java.awt.Color;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

public class AboutDialog extends JDialog {
	
	private static final long serialVersionUID = -1467349046428637201L;

	public AboutDialog(JFrame frame) {
		super(frame);
		
		this.setMinimumSize(new Dimension(300, 200));
		this.setTitle("About the Logbook");
		this.setModal(true);
		
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		int winW = (int) (screenSize.getWidth() * 0.3);
		int winH = (int) (screenSize.getHeight() * 0.25);
		setSize(winW, winH);
		
		int winX = (int) ((screenSize.getWidth() - getSize().getWidth()) / 2);
		int winY = (int) ((screenSize.getHeight() - getSize().getHeight()) / 2);
		setLocation(winX, winY);
		
		JPanel panel = new JPanel();
		panel.setBorder(new EmptyBorder(10, 10, 10, 10));
		getContentPane().add(panel, BorderLayout.CENTER);
		GridBagLayout gbl_panel = new GridBagLayout();
		gbl_panel.columnWidths = new int[]{0, 0};
		gbl_panel.rowHeights = new int[]{0, 0, 0, 0, 0};
		gbl_panel.columnWeights = new double[]{1.0, Double.MIN_VALUE};
		gbl_panel.rowWeights = new double[]{0.0, 1.0, 0.0, 0.0, Double.MIN_VALUE};
		panel.setLayout(gbl_panel);
		
		JLabel lblLogbook = new JLabel("Logbook " + App.VERSION);
		lblLogbook.setFont(new Font("Tahoma", Font.BOLD, 14));
		GridBagConstraints gbc_lblLogbook = new GridBagConstraints();
		gbc_lblLogbook.anchor = GridBagConstraints.WEST;
		gbc_lblLogbook.insets = new Insets(0, 0, 5, 0);
		gbc_lblLogbook.gridx = 0;
		gbc_lblLogbook.gridy = 0;
		panel.add(lblLogbook, gbc_lblLogbook);
		
		JXLabel labelDesc = new JXLabel();
		labelDesc.setLineWrap(true);
		labelDesc.setText("The Logbook is a resource for shortwave radio listeners to keep track of all their own heard radio broadcasts.");
		GridBagConstraints gbc_labelDesc = new GridBagConstraints();
		gbc_labelDesc.insets = new Insets(0, 0, 5, 0);
		gbc_labelDesc.fill = GridBagConstraints.BOTH;
		gbc_labelDesc.gridx = 0;
		gbc_labelDesc.gridy = 1;
		panel.add(labelDesc, gbc_labelDesc);
		
		JLabel lblCopyright = new JLabel("\u00a9" + Year.now().getValue() + " Chris Walsh, KJ6BBS");
		GridBagConstraints gbc_lblCopyright = new GridBagConstraints();
		gbc_lblCopyright.insets = new Insets(0, 0, 5, 0);
		gbc_lblCopyright.anchor = GridBagConstraints.WEST;
		gbc_lblCopyright.gridx = 0;
		gbc_lblCopyright.gridy = 2;
		panel.add(lblCopyright, gbc_lblCopyright);
		
		JButton btnGitHub = new JButton("https://github.com/odds-get-evened/logbook");
		btnGitHub.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				try {
					URI uri = new URI("https://github.com/odds-get-evened/logbook");
					Desktop dt = Desktop.getDesktop();
					try {
						dt.browse(uri);
					} catch (IOException e1) {
						e1.printStackTrace();
					}
				} catch (URISyntaxException e1) {
					e1.printStackTrace();
				}
			}
		});
		btnGitHub.setBorderPainted(false);
		btnGitHub.setForeground(Color.BLUE);
		GridBagConstraints gbc_btnGitHub = new GridBagConstraints();
		gbc_btnGitHub.insets = new Insets(0, 0, 5, 0);
		gbc_btnGitHub.gridx = 0;
		gbc_btnGitHub.gridy = 3;
		panel.add(btnGitHub, gbc_btnGitHub);
		
		
	}

}
