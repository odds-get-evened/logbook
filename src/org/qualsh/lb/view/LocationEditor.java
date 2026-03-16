package org.qualsh.lb.view;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.UIDefaults;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.qualsh.lb.data.ViewPlacesModel;
import org.qualsh.lb.place.Place;

public class LocationEditor extends JDialog {

	private static final long serialVersionUID = -4456464571681368014L;

	private JList<Place> placeList;
	private ViewPlacesModel placesModel;
	private Place selectedPlace = null;
	private JButton btnAssign;

	public LocationEditor(JFrame frame) {
		super(frame, "Locations", true);

		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		int winW = (int) (screenSize.getWidth() * 0.25);
		int winH = (int) (screenSize.getHeight() * 0.3);
		setSize(Math.max(winW, 300), Math.max(winH, 400));
		setMinimumSize(new Dimension(300, 400));
		setLocation(
			(int) ((screenSize.getWidth() - getWidth()) / 2),
			(int) ((screenSize.getHeight() - getHeight()) / 2)
		);

		JPanel root = new JPanel(new BorderLayout(0, 0));
		root.setBorder(new EmptyBorder(10, 10, 8, 10));
		getContentPane().add(root, BorderLayout.CENTER);

		// Search field row
		JPanel searchRow = new JPanel(new GridBagLayout());
		searchRow.setBorder(new EmptyBorder(0, 0, 6, 0));
		root.add(searchRow, BorderLayout.NORTH);

		JLabel lblSearch = new JLabel("Search");
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.insets = new Insets(0, 0, 0, 6);
		gbc.anchor = GridBagConstraints.WEST;
		searchRow.add(lblSearch, gbc);

		JTextField txtSearch = new JTextField();
		gbc = new GridBagConstraints();
		gbc.gridx = 1;
		gbc.gridy = 0;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weightx = 1.0;
		searchRow.add(txtSearch, gbc);

		// Places list
		placesModel = new ViewPlacesModel();
		placesModel.setAllPlaces();

		placeList = new JList<>(placesModel);
		placeList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		placeList.setCellRenderer(new PlaceCellRenderer());

		JScrollPane scrollPane = new JScrollPane(placeList);
		root.add(scrollPane, BorderLayout.CENTER);

		// Buttons
		JPanel btnPanel = new JPanel(new GridBagLayout());
		btnPanel.setBorder(new EmptyBorder(6, 0, 0, 0));
		root.add(btnPanel, BorderLayout.SOUTH);

		btnAssign = new JButton("Assign");
		btnAssign.setEnabled(false);
		GridBagConstraints gbcBtn = new GridBagConstraints();
		gbcBtn.gridx = 0;
		gbcBtn.gridy = 0;
		gbcBtn.weightx = 1.0;
		gbcBtn.anchor = GridBagConstraints.EAST;
		gbcBtn.insets = new Insets(0, 0, 0, 4);
		btnPanel.add(btnAssign, gbcBtn);

		JButton btnClose = new JButton("Close");
		gbcBtn = new GridBagConstraints();
		gbcBtn.gridx = 1;
		gbcBtn.gridy = 0;
		btnPanel.add(btnClose, gbcBtn);

		// Listeners
		placeList.addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent e) {
				btnAssign.setEnabled(placeList.getSelectedValue() != null);
			}
		});

		placeList.addMouseListener(new MouseListener() {
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() == 2 && placeList.getSelectedValue() != null) {
					selectedPlace = placeList.getSelectedValue();
					LocationEditor.this.setVisible(false);
				}
			}
			public void mousePressed(MouseEvent e) {}
			public void mouseReleased(MouseEvent e) {}
			public void mouseEntered(MouseEvent e) {}
			public void mouseExited(MouseEvent e) {}
		});

		btnAssign.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				selectedPlace = placeList.getSelectedValue();
				LocationEditor.this.setVisible(false);
			}
		});

		btnClose.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				LocationEditor.this.setVisible(false);
			}
		});

		// Real-time search filter
		txtSearch.getDocument().addDocumentListener(new DocumentListener() {
			public void insertUpdate(DocumentEvent e) { filter(txtSearch.getText()); }
			public void removeUpdate(DocumentEvent e) { filter(txtSearch.getText()); }
			public void changedUpdate(DocumentEvent e) { filter(txtSearch.getText()); }
		});
	}

	private void filter(String query) {
		String q = query.trim().toLowerCase();
		placesModel.setAllPlaces();
		if (q.isEmpty()) {
			placeList.updateUI();
			return;
		}
		java.util.ArrayList<Place> all = new java.util.ArrayList<>(placesModel.getData());
		placesModel.getData().clear();
		for (Place p : all) {
			if (p.getPlaceName() != null && p.getPlaceName().toLowerCase().contains(q)) {
				placesModel.getData().add(p);
			}
		}
		placeList.updateUI();
		btnAssign.setEnabled(false);
	}

	/** Returns the Place the user selected, or null if cancelled. */
	public Place getSelectedPlace() {
		return selectedPlace;
	}

	private static class PlaceCellRenderer implements ListCellRenderer<Place> {
		public Component getListCellRendererComponent(
				JList<? extends Place> list, Place value, int index,
				boolean isSelected, boolean cellHasFocus) {

			JPanel pnl = new JPanel(new BorderLayout(0, 2));
			pnl.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));

			JLabel lblName = new JLabel(value.getPlaceName());
			lblName.setFont(new Font("Tahoma", Font.BOLD, 12));
			pnl.add(lblName, BorderLayout.NORTH);

			String lat = value.getLatitude();
			String lon = value.getLongitude();
			String coords = (lat != null && lon != null && !lat.isBlank() && !lon.isBlank())
				? lat + ", " + lon : "No coordinates";
			JLabel lblCoords = new JLabel(coords);
			lblCoords.setFont(new Font("Tahoma", Font.PLAIN, 11));
			pnl.add(lblCoords, BorderLayout.SOUTH);

			UIDefaults defs = UIManager.getDefaults();
			if (isSelected || cellHasFocus) {
				pnl.setBackground(defs.getColor("List.selectionBackground"));
				lblName.setForeground(defs.getColor("List.selectionForeground"));
				lblCoords.setForeground(defs.getColor("List.selectionForeground"));
			} else {
				pnl.setBackground(defs.getColor("List.background"));
				lblName.setForeground(defs.getColor("List.foreground"));
				lblCoords.setForeground(defs.getColor("List.foreground"));
			}

			return pnl;
		}
	}
}
