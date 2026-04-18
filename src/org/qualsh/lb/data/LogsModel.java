package org.qualsh.lb.data;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Calendar;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;

import org.qualsh.lb.log.Log;
import org.qualsh.lb.place.Place;
import org.qualsh.lb.util.Utilities;

public class LogsModel extends AbstractTableModel implements TableModelListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4033126324157573519L;
	
	private String[] columns;
	private ArrayList<Log> data;

	public static final int COLUMN_NUM_DATEON = 0;
	public static final int COLUMN_NUM_FREQUENCY = 2;
	public static final int COLUMN_NUM_MODE = 3;
	public static final int COLUMN_NUM_DESC = 5;
	public static final int COLUMN_NUM_MYPLACE = 4;
	public static final int COLUMN_NUM_TIMEON = 1;
	public static final int COLUMN_NUM_LOCATION = 6;

	public LogsModel() {
		columns = new String[]{"Date", "Time (UTC)", "Frequency (kHz)", "Mode", "RX Loc.", "Description", "TX Location"};
		
		setData(getLogList());
		
		addTableModelListener(this);
	}
	
	public ArrayList<Log> getLogList() {
		ArrayList<Log> logList = new ArrayList<Log>();
		
		Connection db = Data.getConnection();
		Statement st = null;
		ResultSet rs = null;
		try {
			st = db.createStatement();
			rs = st.executeQuery("SELECT id, freq, mode, dateon, description, location, my_place, created_at FROM logs ORDER BY dateon DESC");
			while(rs.next()) {
				Log log = new Log();
				log.setId(rs.getInt("id"));
				log.setDateOn(rs.getInt("dateon"));
				log.setDescription(rs.getString("description"));
				log.setFrequency(rs.getFloat("freq"));
				log.setMode(rs.getString("mode"));
				log.setLocation(rs.getInt("location"));
				log.setMyPlace(rs.getInt("my_place"));
				log.setCreatedAt(rs.getInt("created_at"));

				logList.add(log);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			try {
				if (rs != null) rs.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}

			try {
				if (st != null) st.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}

			try {
				db.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}

		return logList;
	}

	public void searchAll(String query) {
		ArrayList<Log> results = new ArrayList<Log>();
		Connection conn = Data.getConnection();
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = conn.prepareStatement("SELECT id, freq, mode, dateon, description, location, my_place, created_at FROM logs WHERE freq LIKE ? OR description LIKE ? ORDER BY dateon DESC");
			ps.setString(1, "%" + query.trim()  + "%");
			ps.setString(2, "%" + query.trim() + "%");

			rs = ps.executeQuery();
			while(rs.next()) {
				Log log = new Log();
				log.setId(rs.getInt("id"));
				log.setDateOn(rs.getInt("dateon"));
				log.setDescription(rs.getString("description"));
				log.setFrequency(rs.getFloat("freq"));
				log.setMode(rs.getString("mode"));
				log.setLocation(rs.getInt("location"));
				log.setMyPlace(rs.getInt("my_place"));
				log.setCreatedAt(rs.getInt("created_at"));

				results.add(log);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			try {
				if (rs != null) rs.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}

			try {
				if (ps != null) ps.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}

			try {
				conn.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		setData(results);
		fireTableDataChanged();
	}
	
	public ArrayList<Log> getLogList(String order, String sort) {
		ArrayList<Log> logList = new ArrayList<Log>();
		
		Connection conn = Data.getConnection();
		Statement st = null;
		ResultSet rs = null;
		try {
			st = conn.createStatement();
			rs = st.executeQuery("SELECT id, freq, mode, dateon, description, location, my_place, created_at FROM logs ORDER BY " + order + " " + sort);
			while(rs.next()) {
				Log log = new Log();
				log.setId(rs.getInt("id"));
				log.setDateOn(rs.getInt("dateon"));
				log.setDescription(rs.getString("description"));
				log.setFrequency(rs.getFloat("freq"));
				log.setMode(rs.getString("mode"));
				log.setLocation(rs.getInt("location"));
				log.setMyPlace(rs.getInt("my_place"));
				log.setCreatedAt(rs.getInt("created_at"));

				logList.add(log);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			try {
				if (rs != null) rs.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}

			try {
				if (st != null) st.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}

			try {
				conn.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}

		return logList;
	}
	
	public ArrayList<Log> getLogsByLocation(int id) {
		ArrayList<Log> logList = new ArrayList<Log>();
		
		Connection conn = Data.getConnection();
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = conn.prepareStatement("SELECT id, freq, mode, dateon, description, location, my_place, created_at FROM logs WHERE location LIKE ? ORDER BY dateon DESC");
			ps.setInt(1, id);

			rs = ps.executeQuery();

			while(rs.next()) {
				Log log = new Log();
				log.setId(rs.getInt("id"));
				log.setDateOn(rs.getInt("dateon"));
				log.setDescription(rs.getString("description"));
				log.setFrequency(rs.getFloat("freq"));
				log.setMode(rs.getString("mode"));
				log.setLocation(rs.getInt("location"));
				log.setMyPlace(rs.getInt("my_place"));
				log.setCreatedAt(rs.getInt("created_at"));

				logList.add(log);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			try {
				if (rs != null) rs.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}

			try {
				if (ps != null) ps.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}

			try {
				conn.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}

		return logList;
	}

	public ArrayList<Log> getLogsByMyPlace(int id) {
		ArrayList<Log> logList = new ArrayList<Log>();

		Connection conn = Data.getConnection();
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = conn.prepareStatement("SELECT id, freq, mode, dateon, description, location, my_place, created_at FROM logs WHERE my_place = ? ORDER BY dateon DESC");
			ps.setInt(1, id);

			rs = ps.executeQuery();

			while (rs.next()) {
				Log log = new Log();
				log.setId(rs.getInt("id"));
				log.setDateOn(rs.getInt("dateon"));
				log.setDescription(rs.getString("description"));
				log.setFrequency(rs.getFloat("freq"));
				log.setMode(rs.getString("mode"));
				log.setLocation(rs.getInt("location"));
				log.setMyPlace(rs.getInt("my_place"));
				log.setCreatedAt(rs.getInt("created_at"));
				logList.add(log);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			try {
				if (rs != null) rs.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
			try {
				if (ps != null) ps.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
			try {
				conn.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}

		return logList;
	}

	public int getRowCount() {
		return data.size();
	}

	public int getColumnCount() {
		return columns.length;
	}

	public Object getValueAt(int rowIndex, int columnIndex) {
		if (rowIndex < 0 || rowIndex >= data.size()) return null;
		Log log = data.get(rowIndex);
		
		switch(columnIndex) {
			default:
				return null;
			case COLUMN_NUM_DATEON:
				return Utilities.unixTimestampToString(log.getDateOn(), "MM/dd/yyyy");
			case COLUMN_NUM_TIMEON:
				return Utilities.unixTimestampToString(log.getDateOn(), "HH:mm");
			case COLUMN_NUM_FREQUENCY:
				return String.valueOf(log.getFrequency());
			case COLUMN_NUM_MODE:
				return log.getMode();
			case COLUMN_NUM_DESC:
				return log.getDescription();
			case COLUMN_NUM_MYPLACE:
				Place place = log.getFullMyPlace();
				if(place != null) {
					return place.getPlaceName();
				} else {
					return "";
				}
			case COLUMN_NUM_LOCATION:
				Place txPlace = log.getFullTxPlace();
				return txPlace != null ? txPlace.getPlaceName() : "";
		}
	}

	public void setData(ArrayList<Log> logs) {
		this.setLogs(logs);
	}

	public ArrayList<Log> getLogs() {
		return data;
	}

	public void setLogs(ArrayList<Log> logs) {
		this.data = logs;
	}

	@Override
	public String getColumnName(int pCol) {
		String str = columns[pCol];
		
		return str;
	}
	
	@Override
	public boolean isCellEditable(int row, int col) {
		return false;
	}
	
	@Override
	public void setValueAt(Object value, int row, int col) {
		String dateStr = getValueAt(row, COLUMN_NUM_DATEON).toString() + " " + getValueAt(row, COLUMN_NUM_TIMEON).toString();
		data.get(row).setDateOn(Utilities.stringToUnixTimeStamp(dateStr, "MM/dd/yyyy HH:mm"));
		
		switch(col) {
			default:;
			case COLUMN_NUM_DATEON:
				//data.get(row).setDateOn(Integer.valueOf((String) value));
			case COLUMN_NUM_FREQUENCY:
				data.get(row).setFrequency(Float.valueOf((String) value));
			case COLUMN_NUM_MODE:
				data.get(row).setMode((String) value);
			case COLUMN_NUM_DESC:
				data.get(row).setDescription((String) value);
		}
		
		updateRow(row, col);
	}

	public void updateRow(int row, int col) {
		data.get(row).update();
		
		fireTableCellUpdated(row, col);
	}
	
	public void insert(Log log) {
		Connection db = Data.getConnection();
		PreparedStatement st = null;
		try {
			st = db.prepareStatement("INSERT INTO logs (freq, mode, dateon, description, location, my_place, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)");
			st.setFloat(1, log.getFrequency());
			st.setString(2, log.getMode());
			st.setInt(3, log.getDateOn());
			st.setString(4, log.getDescription());

			// set station location
			if(log.getLocation() == 0) {
				st.setNull(5, Types.INTEGER);
			} else {
				st.setInt(5, log.getLocation());
			}

			// add current user location
			if(log.getMyPlace() != 0) {
				st.setInt(6, log.getMyPlace());
			} else {
				st.setNull(6, Types.INTEGER);
			}

			// record when the log entry was created
			st.setInt(7, (int) (System.currentTimeMillis() / 1000L));

			st.execute();
			setData(getLogList());
			fireTableDataChanged();

		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			try {
				if (st != null) st.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}

			try {
				db.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}

	public void delete(Log log) {
		Connection db = Data.getConnection();
		PreparedStatement ps = null;
		try {
			ps = db.prepareStatement("DELETE FROM logs WHERE id = ?");
			ps.setInt(1, log.getId());
			
			ps.execute();
			setData(getLogList());
			fireTableDataChanged();
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			try {
				if (ps != null) ps.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}

			try {
				db.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}

	public void tableChanged(TableModelEvent e) {
		// no-op: data is refreshed before fireTableDataChanged() in each mutating operation
	}
	
	public ArrayList<Log> getData() {
		return data;
	}

	public void update(Log log) {
		Connection conn = Data.getConnection();
		PreparedStatement ps = null;
		try {
			ps = conn.prepareStatement("UPDATE logs SET freq = ?, mode = ?, dateon = ?, description = ?, location = ?, my_place = ? WHERE id = ?");
			ps.setFloat(1, log.getFrequency());
			ps.setString(2, log.getMode());
			ps.setInt(3, log.getDateOn());
			ps.setString(4, log.getDescription());
			ps.setInt(7, log.getId());
			
			if(log.getLocation() == 0) {
				ps.setNull(5, Types.INTEGER);
			} else {
				ps.setInt(5, log.getLocation());
			}
			
			if(log.getMyPlace() != 0) {
				ps.setInt(6, log.getMyPlace());
			} else {
				ps.setNull(6, Types.INTEGER);
			}
			
			ps.execute();
			setData(getLogList());
			fireTableDataChanged();

			ps.close();
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

	public void getThisHour() {
		Calendar now = Calendar.getInstance();
		int hour = now.get(Calendar.HOUR_OF_DAY);
		ArrayList<Log> results = new ArrayList<Log>();

		Connection db = Data.getConnection();
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = db.prepareStatement("SELECT id, freq, mode, dateon, description, location, my_place, created_at "
					+ "FROM logs "
					+ "WHERE CAST(STRFTIME('%H', datetime(dateon, 'unixepoch')) AS INTEGER) = ?");
			ps.setInt(1, hour);

			rs = ps.executeQuery();

			while(rs.next()) {
				Log log = new Log();
				log.setId(rs.getInt("id"));
				log.setDateOn(rs.getInt("dateon"));
				log.setDescription(rs.getString("description"));
				log.setFrequency(rs.getFloat("freq"));
				log.setMode(rs.getString("mode"));
				log.setLocation(rs.getInt("location"));
				log.setMyPlace(rs.getInt("my_place"));
				log.setCreatedAt(rs.getInt("created_at"));

				results.add(log);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			try {
				if (rs != null) rs.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}

			try {
				if (ps != null) ps.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}

			try {
				db.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		setData(results);
		fireTableDataChanged();
	}

}
