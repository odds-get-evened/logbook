package org.qualsh.lb.view;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.UIDefaults;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.qualsh.lb.data.LogsModel;
import org.qualsh.lb.data.ViewPlacesModel;
import org.qualsh.lb.place.Place;

/**
 * Locations tab — manages the user's own station locations (Places).
 * Users can add, edit, and delete their custom locations. Selecting a
 * location pans the map to it and filters the log table to entries recorded
 * from that location.
 */
public class LocationsTab extends JPanel {

	private static final long serialVersionUID = -7174045351909746608L;

	private JList<Place> placeList;
	private ViewPlacesModel placesModel;
	private LogsTable logsTable;
	private MapPanel mapPanel;
	private JButton btnEdit;
	private JButton btnDelete;

	public LocationsTab() {
		setLayout(new BorderLayout(0, 0));

		// --- Header ---
		JPanel headerPanel = new JPanel(new BorderLayout(0, 4));
		headerPanel.setBorder(new EmptyBorder(8, 8, 4, 8));
		add(headerPanel, BorderLayout.NORTH);

		JLabel lblTitle = new JLabel("My Station Locations");
		lblTitle.setFont(new Font("Tahoma", Font.BOLD, 13));
		headerPanel.add(lblTitle, BorderLayout.NORTH);

		JLabel lblHint = new JLabel("Add your own listening locations. Select one to filter the log.");
		lblHint.setFont(new Font("Tahoma", Font.PLAIN, 11));
		headerPanel.add(lblHint, BorderLayout.SOUTH);

		// --- List ---
		placesModel = new ViewPlacesModel();
		placesModel.setAllPlaces();

		placeList = new JList<>(placesModel);
		placeList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		placeList.setCellRenderer(new PlaceCellRenderer());

		JScrollPane scrollPane = new JScrollPane(placeList);
		scrollPane.setBorder(new EmptyBorder(0, 8, 0, 8));
		add(scrollPane, BorderLayout.CENTER);

		// --- Button bar ---
		JPanel btnBar = new JPanel(new GridBagLayout());
		btnBar.setBorder(new EmptyBorder(6, 8, 8, 8));
		add(btnBar, BorderLayout.SOUTH);

		GridBagConstraints gbc = new GridBagConstraints();
		gbc.insets = new Insets(0, 0, 0, 4);

		JButton btnAdd = new JButton("Add");
		gbc.gridx = 0;
		gbc.gridy = 0;
		btnBar.add(btnAdd, gbc);

		btnEdit = new JButton("Edit");
		btnEdit.setEnabled(false);
		gbc.gridx = 1;
		btnBar.add(btnEdit, gbc);

		btnDelete = new JButton("Delete");
		btnDelete.setEnabled(false);
		gbc.gridx = 2;
		gbc.insets = new Insets(0, 0, 0, 0);
		btnBar.add(btnDelete, gbc);

		// --- Listeners ---

		placeList.addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent e) {
				if (e.getValueIsAdjusting()) return;
				Place selected = placeList.getSelectedValue();
				boolean hasSelection = selected != null;
				btnEdit.setEnabled(hasSelection);
				btnDelete.setEnabled(hasSelection);

				if (hasSelection) {
					// Filter logs table by this place
					if (logsTable != null) {
						LogsModel logModel = (LogsModel) logsTable.getModel();
						logModel.setData(logModel.getLogsByMyPlace(selected.getId()));
						logModel.fireTableDataChanged();
						logsTable.clearSelection();
					}
					// Pan map to the location
					if (mapPanel != null) {
						try {
							String lat = selected.getLatitude();
							String lon = selected.getLongitude();
							if (lat != null && !lat.isBlank() && lon != null && !lon.isBlank()) {
								mapPanel.panToLocation(Double.parseDouble(lat.trim()), Double.parseDouble(lon.trim()));
							}
						} catch (NumberFormatException ignored) {}
					}
				}
			}
		});

		// Show all logs again on deselect via double-click on empty area
		placeList.addMouseListener(new MouseListener() {
			public void mouseClicked(MouseEvent e) {
				int index = placeList.locationToIndex(e.getPoint());
				if (index < 0 || !placeList.getCellBounds(index, index).contains(e.getPoint())) {
					placeList.clearSelection();
					if (logsTable != null) {
						LogsModel logModel = (LogsModel) logsTable.getModel();
						logModel.setData(logModel.getLogList());
						logModel.fireTableDataChanged();
					}
				}
			}
			public void mousePressed(MouseEvent e) {}
			public void mouseReleased(MouseEvent e) {}
			public void mouseEntered(MouseEvent e) {}
			public void mouseExited(MouseEvent e) {}
		});

		btnAdd.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JFrame owner = (JFrame) javax.swing.SwingUtilities.getWindowAncestor(LocationsTab.this);
				PlaceDialog dlg = new PlaceDialog(owner, null);
				dlg.setVisible(true);
				if (dlg.getResultPlace() != null) {
					refreshList();
				}
			}
		});

		btnEdit.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Place selected = placeList.getSelectedValue();
				if (selected == null) return;
				JFrame owner = (JFrame) javax.swing.SwingUtilities.getWindowAncestor(LocationsTab.this);
				PlaceDialog dlg = new PlaceDialog(owner, selected);
				dlg.setVisible(true);
				if (dlg.getResultPlace() != null) {
					refreshList();
				}
			}
		});

		btnDelete.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Place selected = placeList.getSelectedValue();
				if (selected == null) return;
				int confirm = JOptionPane.showConfirmDialog(
					LocationsTab.this,
					"Delete \"" + selected.getPlaceName() + "\"?\nLog entries using this location will be unlinked.",
					"Confirm Delete",
					JOptionPane.YES_NO_OPTION
				);
				if (confirm == JOptionPane.YES_OPTION) {
					Place.delete(selected.getId());
					placeList.clearSelection();
					refreshList();
					// Reset log table to show all
					if (logsTable != null) {
						LogsModel logModel = (LogsModel) logsTable.getModel();
						logModel.setData(logModel.getLogList());
						logModel.fireTableDataChanged();
					}
				}
			}
		});
	}

	/** Reload the places list from the database. */
	public void refreshList() {
		placesModel.setAllPlaces();
		placeList.updateUI();
	}

	public void setLogsTable(LogsTable logsTable) {
		this.logsTable = logsTable;
	}

	public LogsTable getLogsTable() {
		return logsTable;
	}

	public void setMapPanel(MapPanel mapPanel) {
		this.mapPanel = mapPanel;
	}

	public MapPanel getMapPanel() {
		return mapPanel;
	}

	public JList<Place> getPlaceList() {
		return placeList;
	}

	// ---------------------------------------------------------------
	// Cell renderer
	// ---------------------------------------------------------------

	private static class PlaceCellRenderer implements ListCellRenderer<Place> {
		public Component getListCellRendererComponent(
				JList<? extends Place> list, Place value, int index,
				boolean isSelected, boolean cellHasFocus) {

			JPanel pnl = new JPanel(new BorderLayout(0, 2));
			pnl.setBorder(BorderFactory.createEmptyBorder(5, 8, 5, 8));

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
