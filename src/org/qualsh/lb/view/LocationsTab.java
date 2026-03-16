package org.qualsh.lb.view;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.UIDefaults;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;

import org.qualsh.lb.data.Data;
import org.qualsh.lb.data.LogsModel;
import org.qualsh.lb.data.ViewLocationsModel;
import org.qualsh.lb.data.ViewStationsModel;
import org.qualsh.lb.location.Location;
import org.qualsh.lb.station.Station;
import org.qualsh.lb.view.MapPanel;

public class LocationsTab extends JPanel {

	private static final long serialVersionUID = -7174045351909746608L;
	private JList<Station> stationList;
	private JList<Location> locationList;
	private LogsTable logsTable;
	private JButton btnDeleteLocation;
	private JButton btnDeleteStation;
	private MapPanel mapPanel;

	public LocationsTab() {
		setLayout(new BorderLayout(0, 0));
		
		JSplitPane splitPane = new JSplitPane();
		add(splitPane, BorderLayout.CENTER);
		
		JPanel stationsPanel = new JPanel();
		stationsPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		splitPane.setLeftComponent(stationsPanel);
		final ViewStationsModel stationsModel = new ViewStationsModel();
		stationsModel.setAllStations();
		stationsPanel.setLayout(new BorderLayout(0, 0));
		
		JPanel panel = new JPanel();
		stationsPanel.add(panel, BorderLayout.NORTH);
		panel.setLayout(new BorderLayout(0, 0));
		
		JLabel lblStations = new JLabel("Stations");
		lblStations.setFont(new Font("Tahoma", Font.BOLD, 12));
		panel.add(lblStations, BorderLayout.NORTH);
		
		JLabel lblSelectStation = new JLabel("Select a station below to list its locations.");
		lblSelectStation.setFont(new Font("Tahoma", Font.PLAIN, 12));
		panel.add(lblSelectStation, BorderLayout.SOUTH);
		
		JPanel panel_2 = new JPanel();
		stationsPanel.add(panel_2, BorderLayout.SOUTH);
		
		btnDeleteStation = new JButton("Delete");
		btnDeleteStation.setEnabled(false);
		btnDeleteStation.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				int dialogBtn = JOptionPane.YES_NO_OPTION;
				int conf = JOptionPane.showConfirmDialog(null, "Select Yes to confirm, and No to cancel. All related locations will be removed as well.", "Warning", dialogBtn);
				
				if(conf == JOptionPane.YES_OPTION) {
					int stationId = stationList.getSelectedValue().getId();
					deleteStation(stationId);
					// reset stations list
					ViewStationsModel stationModel = (ViewStationsModel) getStationList().getModel();
					stationModel.setAllStations();
					getStationList().setSelectedValue(null, true);
					btnDeleteStation.setEnabled(false);
					// reset locations list
					ViewLocationsModel locationModel = (ViewLocationsModel) getLocationList().getModel();
					locationModel.setAllLocations();
					getLocationList().setSelectedValue(null, true);
					btnDeleteLocation.setEnabled(false);
					// reset logs table
					LogsModel logModel = (LogsModel) getLogsTable().getModel();
					logModel.setData(logModel.getLogList());
					logModel.fireTableDataChanged();
					getLogsTable().updateUI();
				}
				
			}
			
		});
		panel_2.add(btnDeleteStation);
		
		JScrollPane scrollPane_1 = new JScrollPane();
		stationsPanel.add(scrollPane_1, BorderLayout.CENTER);
		
		stationList = new JList<Station>();
		scrollPane_1.setViewportView(stationList);
		stationList.setCellRenderer(new StationListCellRenderer());
		stationList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		stationList.setModel(stationsModel);
		stationList.addMouseListener(new MouseListener() {

			public void mouseClicked(MouseEvent e) {
				if(stationsModel.getSize() > 0) {
					ViewLocationsModel locModel = (ViewLocationsModel) getLocationList().getModel();
					locModel.setLocationsByStation(stationList.getSelectedValue().getId());
					getLocationList().clearSelection();
					btnDeleteStation.setEnabled(true);
				}
			}

			public void mousePressed(MouseEvent e) {}

			public void mouseReleased(MouseEvent e) {}

			public void mouseEntered(MouseEvent e) {}

			public void mouseExited(MouseEvent e) {}
			
		});
		
		JPanel locationsPanel = new JPanel();
		locationsPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		splitPane.setRightComponent(locationsPanel);
		final ViewLocationsModel locationsModel = new ViewLocationsModel();
		locationsModel.setAllLocations();
		locationsPanel.setLayout(new BorderLayout(0, 0));
		
		JPanel panel_3 = new JPanel();
		locationsPanel.add(panel_3, BorderLayout.SOUTH);
		
		btnDeleteLocation = new JButton("Delete");
		btnDeleteLocation.setEnabled(false);
		btnDeleteLocation.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				int dialogBtn = JOptionPane.YES_NO_OPTION;
				int confirm = JOptionPane.showConfirmDialog(null, "Select Yes to confirm, and No to cancel.", "Warning", dialogBtn);
				
				if(confirm == JOptionPane.YES_OPTION) {
					int locationId = getLocationList().getSelectedValue().getId();
					deleteLocation(locationId);
					// reset locations list
					ViewLocationsModel locationModel = (ViewLocationsModel) getLocationList().getModel();
					locationModel.setAllLocations();
					getLocationList().setSelectedValue(null, true);
					btnDeleteLocation.setEnabled(false);
					// reset log table
					LogsModel logModel = (LogsModel) getLogsTable().getModel();
					logModel.setData(logModel.getLogList());
					logModel.fireTableDataChanged();
					getLogsTable().updateUI();
				}
			}
			
		});
		panel_3.add(btnDeleteLocation);
		
		JPanel panel_1 = new JPanel();
		locationsPanel.add(panel_1, BorderLayout.NORTH);
		panel_1.setLayout(new BorderLayout(0, 0));
		
		JLabel lblLocations = new JLabel("Locations");
		lblLocations.setFont(new Font("Tahoma", Font.BOLD, 12));
		panel_1.add(lblLocations, BorderLayout.NORTH);
		
		JLabel lblSelectLocation = new JLabel("Select a location below to list its log entries.");
		lblSelectLocation.setFont(new Font("Tahoma", Font.PLAIN, 12));
		panel_1.add(lblSelectLocation, BorderLayout.SOUTH);
		
		JScrollPane scrollPane = new JScrollPane();
		locationsPanel.add(scrollPane, BorderLayout.CENTER);
		
		locationList = new JList<Location>();
		locationList.setVisibleRowCount(4);
		scrollPane.setViewportView(locationList);
		locationList.setCellRenderer(new LocationListCellRenderer());
		locationList.setModel(locationsModel);
		locationList.addMouseListener(new MouseListener() {

			public void mouseClicked(MouseEvent e) {
				if(locationsModel.getSize() > 0) {
					Location selected = locationList.getSelectedValue();
					LogsModel logModel = (LogsModel) getLogsTable().getModel();
					logModel.setData(logModel.getLogsByLocation(selected.getId()));
					logModel.fireTableDataChanged();
					getLogsTable().updateUI();
					getLogsTable().clearSelection();
					btnDeleteLocation.setEnabled(true);
					// Pan map to the selected location if it has coordinates
					if (mapPanel != null) {
						try {
							String latStr = selected.getStrLatitude();
							String lonStr = selected.getStrLongitude();
							if (latStr != null && !latStr.isBlank() && lonStr != null && !lonStr.isBlank()) {
								mapPanel.panToLocation(Double.parseDouble(latStr.trim()), Double.parseDouble(lonStr.trim()));
							}
						} catch (NumberFormatException ignored) {}
					}
				}
			}

			public void mousePressed(MouseEvent e) {}

			public void mouseReleased(MouseEvent e) {}

			public void mouseEntered(MouseEvent e) {}

			public void mouseExited(MouseEvent e) {}

		});
		
	}

	protected void deleteLocation(int locationId) {
		this.unsetLogLocation(locationId, Data.getConnection());
		
		Connection conn = Data.getConnection();
		
		PreparedStatement ps = null;
		try {
			ps = conn.prepareStatement("DELETE FROM locations WHERE id = ?");
			ps.setInt(1, locationId);
			
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
		
	}

	public JList<Station> getStationList() {
		return stationList;
	}

	public void setStationList(JList<Station> stationList) {
		this.stationList = stationList;
	}

	public JList<Location> getLocationList() {
		return locationList;
	}

	public void setLocationList(JList<Location> locationList) {
		this.locationList = locationList;
	}

	public void setLogsTable(LogsTable logTable) {
		this.logsTable = logTable;
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
	
	public class StationListCellRenderer implements ListCellRenderer<Station> {

		public Component getListCellRendererComponent(
				JList<? extends Station> list, Station value, int index,
				boolean isSelected, boolean cellHasFocus) {
			
			JPanel pnl = new JPanel();
			pnl.setLayout(new BorderLayout());
			pnl.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
			
			JLabel lblTitle = new JLabel(value.getTitle());
			lblTitle.setFont(new Font("Tahoma", Font.BOLD, 12));
			pnl.add(lblTitle, BorderLayout.CENTER);
			
			UIDefaults defs = UIManager.getDefaults();
			if(cellHasFocus || isSelected) {
				pnl.setBackground(defs.getColor("List.selectionBackground"));
				lblTitle.setForeground(defs.getColor("List.selectionForeground"));
			} else {
				pnl.setBackground(defs.getColor("List.background"));
				lblTitle.setForeground(defs.getColor("List.foreground"));
			}
			
			return pnl;
		}
		
	}
	
	public class LocationListCellRenderer implements ListCellRenderer<Location> {

		public Component getListCellRendererComponent(
				JList<? extends Location> list, Location value, int index,
				boolean isSelected, boolean cellHasFocus) {
			
			JPanel pnl = new JPanel();
			pnl.setLayout(new BorderLayout());
			pnl.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
			
			JLabel lblName = new JLabel(value.getLocationName());
			lblName.setFont(new Font("Tahoma", Font.BOLD, 12));
			pnl.add(lblName, BorderLayout.CENTER);
			
			JPanel metaPanel = new JPanel();
			metaPanel.setLayout(new BorderLayout());
			pnl.add(metaPanel, BorderLayout.SOUTH);
			
			JLabel lblFreq = new JLabel(value.getStrFrequency() + " kHz");
			lblFreq.setFont(new Font("Tahoma", Font.PLAIN, 12));
			metaPanel.add(lblFreq, BorderLayout.CENTER);
			
			JLabel lblTimes = new JLabel();
			lblTimes.setFont(new Font("Tahoma", Font.PLAIN, 12));
			StringBuilder sbTimes = new StringBuilder();
			sbTimes.append(value.getStrTimeOn());
			sbTimes.append(" to ");
			sbTimes.append(value.getStrTimeOff());
			lblTimes.setText(sbTimes.toString());
			metaPanel.add(lblTimes, BorderLayout.SOUTH);
			
			UIDefaults defs = UIManager.getDefaults();
			if(cellHasFocus || isSelected) {
				pnl.setBackground(defs.getColor("List.selectionBackground"));
				metaPanel.setBackground(defs.getColor("List.selectionBackground"));
				lblName.setForeground(defs.getColor("List.selectionForeground"));
				lblTimes.setForeground(defs.getColor("List.selectionForeground"));
				lblFreq.setForeground(defs.getColor("List.selectionForeground"));
			} else {
				pnl.setBackground(defs.getColor("List.background"));
				metaPanel.setBackground(defs.getColor("List.background"));
				lblName.setForeground(defs.getColor("List.foreground"));
				lblTimes.setForeground(defs.getColor("List.foreground"));
				lblFreq.setForeground(defs.getColor("List.foreground"));
			}
			
			return pnl;
			
		}
		
	}

	public void deleteStation(int id) {		
		Connection conn = Data.getConnection();
		try {
			conn.setAutoCommit(false);
		} catch (SQLException e1) {
			e1.printStackTrace();
		}
		
		PreparedStatement ps = null;
		try {
			ps = conn.prepareStatement("DELETE FROM stations WHERE id = ?");
			ps.setInt(1, id);
			
			ps.execute();
			ps.close();
			
			deleteLocationsByStation(id, conn);
						
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			try {
				ps.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}

	private void deleteLocationsByStation(int stationId, Connection conn) {
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = conn.prepareStatement("SELECT id FROM locations WHERE station_id = ?");
			ps.setInt(1, stationId);
			rs = ps.executeQuery();
			
			while(rs.next()) {
				unsetLogLocation(rs.getInt("id"), conn);
			}
			
			deleteLocationByStation(stationId, conn);
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			try {
				rs.close();
			} catch (SQLException e1) {
				e1.printStackTrace();
			}
			
			try {
				ps.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		
		try {
			conn.commit();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		try {
			conn.setAutoCommit(true);
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		try {
			conn.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	private void deleteLocationByStation(int stationId, Connection conn) {
		PreparedStatement ps = null;
		try {
			ps = conn.prepareStatement("DELETE FROM locations WHERE station_id = ?");
			ps.setInt(1, stationId);
			
			ps.execute();
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			try {
				ps.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}

	private void unsetLogLocation(int locationId, Connection conn) {
		PreparedStatement ps = null;
		try {
			ps = conn.prepareStatement("UPDATE logs SET location = ? WHERE location = ?");
			ps.setNull(1, Types.INTEGER);
			ps.setInt(2, locationId);
			
			ps.execute();
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			try {
				ps.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}
	
}
