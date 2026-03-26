package org.qualsh.lb.data;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;

import org.qualsh.lb.digital.DigitalContact;
import org.qualsh.lb.util.Utilities;

/**
 * Provides the data behind the digital contacts table displayed in the
 * Digital Modes panel. Each row in the table corresponds to one
 * {@link DigitalContact} stored in your logbook. The model supports
 * searching, sorting, inserting, updating, and deleting contact entries.
 */
public class DigitalContactsModel extends AbstractTableModel implements TableModelListener {

	private static final long serialVersionUID = 7812345098234560123L;

	private String[] columns;
	private ArrayList<DigitalContact> data;

	/** Column index for the contact date. */
	public static final int COLUMN_NUM_DATE_ON              = 0;
	/** Column index for the UTC start time. */
	public static final int COLUMN_NUM_TIME_ON              = 1;
	/** Column index for the contacted callsign. */
	public static final int COLUMN_NUM_CALLSIGN             = 2;
	/** Column index for the band. */
	public static final int COLUMN_NUM_BAND                 = 3;
	/** Column index for the dial frequency. */
	public static final int COLUMN_NUM_FREQUENCY_MHZ        = 4;
	/** Column index for the digital mode name. */
	public static final int COLUMN_NUM_MODE                 = 5;
	/** Column index for the signal report sent. */
	public static final int COLUMN_NUM_SIGNAL_REPORT_SENT   = 6;
	/** Column index for the signal report received. */
	public static final int COLUMN_NUM_SIGNAL_REPORT_RECEIVED = 7;
	/** Column index for free-text notes. */
	public static final int COLUMN_NUM_NOTES                = 8;

	/**
	 * Creates the model and loads all digital contacts from the database,
	 * sorted from newest to oldest.
	 */
	public DigitalContactsModel() {
		columns = new String[]{
			"Date", "Time (UTC)", "Callsign", "Band",
			"Frequency (MHz)", "Mode", "Sent", "Received", "Notes"
		};
		setData(loadAll());
		addTableModelListener(this);
	}

	/**
	 * Loads every digital contact from the database, ordered by date with
	 * the most recent contact first. Returns the full list so the table
	 * can be refreshed after a change.
	 */
	public ArrayList<DigitalContact> loadAll() {
		ArrayList<DigitalContact> list = new ArrayList<DigitalContact>();
		Connection db = Data.getConnection();
		Statement st = null;
		ResultSet rs = null;
		try {
			st = db.createStatement();
			rs = st.executeQuery(
				"SELECT id, callsign, frequency_mhz, digital_mode_id, "
				+ "signal_report_sent, signal_report_received, notes, "
				+ "date_on, time_on, time_off, my_callsign, band, "
				+ "tx_power_watts, grid, location_id, created_at "
				+ "FROM digital_contacts ORDER BY date_on DESC");
			while (rs.next()) {
				DigitalContact c = new DigitalContact();
				c.setId(rs.getInt(DigitalContact.COLUMN_ID));
				c.setCallsign(rs.getString(DigitalContact.COLUMN_CALLSIGN));
				c.setFrequencyMhz(rs.getFloat(DigitalContact.COLUMN_FREQUENCY_MHZ));
				c.setDigitalModeId(rs.getInt(DigitalContact.COLUMN_DIGITAL_MODE_ID));
				c.setSignalReportSent(rs.getString(DigitalContact.COLUMN_SIGNAL_REPORT_SENT));
				c.setSignalReportReceived(rs.getString(DigitalContact.COLUMN_SIGNAL_REPORT_RECEIVED));
				c.setNotes(rs.getString(DigitalContact.COLUMN_NOTES));
				c.setDateOn(rs.getInt(DigitalContact.COLUMN_DATE_ON));
				c.setTimeOn(rs.getString(DigitalContact.COLUMN_TIME_ON));
				c.setTimeOff(rs.getString(DigitalContact.COLUMN_TIME_OFF));
				c.setMyCallsign(rs.getString(DigitalContact.COLUMN_MY_CALLSIGN));
				c.setBand(rs.getString(DigitalContact.COLUMN_BAND));
				c.setTxPowerWatts(rs.getFloat(DigitalContact.COLUMN_TX_POWER_WATTS));
				c.setGrid(rs.getString(DigitalContact.COLUMN_GRID));
				c.setLocationId(rs.getInt(DigitalContact.COLUMN_LOCATION_ID));
				c.setCreatedAt(rs.getInt(DigitalContact.COLUMN_CREATED_AT));
				list.add(c);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			try { if (rs != null) rs.close(); } catch (SQLException e) { e.printStackTrace(); }
			try { if (st != null) st.close(); } catch (SQLException e) { e.printStackTrace(); }
			try { db.close(); } catch (SQLException e) { e.printStackTrace(); }
		}
		return list;
	}

	/**
	 * Filters the displayed contacts to those whose callsign or notes contain
	 * the given search text. The table updates immediately with the matching
	 * results.
	 *
	 * @param query the text to search for across callsign and notes fields
	 */
	public void search(String query) {
		ArrayList<DigitalContact> results = new ArrayList<DigitalContact>();
		Connection conn = Data.getConnection();
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = conn.prepareStatement(
				"SELECT id, callsign, frequency_mhz, digital_mode_id, "
				+ "signal_report_sent, signal_report_received, notes, "
				+ "date_on, time_on, time_off, my_callsign, band, "
				+ "tx_power_watts, grid, location_id, created_at "
				+ "FROM digital_contacts "
				+ "WHERE callsign LIKE ? OR notes LIKE ? "
				+ "ORDER BY date_on DESC");
			ps.setString(1, "%" + query.trim() + "%");
			ps.setString(2, "%" + query.trim() + "%");
			rs = ps.executeQuery();
			while (rs.next()) {
				DigitalContact c = new DigitalContact();
				c.setId(rs.getInt(DigitalContact.COLUMN_ID));
				c.setCallsign(rs.getString(DigitalContact.COLUMN_CALLSIGN));
				c.setFrequencyMhz(rs.getFloat(DigitalContact.COLUMN_FREQUENCY_MHZ));
				c.setDigitalModeId(rs.getInt(DigitalContact.COLUMN_DIGITAL_MODE_ID));
				c.setSignalReportSent(rs.getString(DigitalContact.COLUMN_SIGNAL_REPORT_SENT));
				c.setSignalReportReceived(rs.getString(DigitalContact.COLUMN_SIGNAL_REPORT_RECEIVED));
				c.setNotes(rs.getString(DigitalContact.COLUMN_NOTES));
				c.setDateOn(rs.getInt(DigitalContact.COLUMN_DATE_ON));
				c.setTimeOn(rs.getString(DigitalContact.COLUMN_TIME_ON));
				c.setTimeOff(rs.getString(DigitalContact.COLUMN_TIME_OFF));
				c.setMyCallsign(rs.getString(DigitalContact.COLUMN_MY_CALLSIGN));
				c.setBand(rs.getString(DigitalContact.COLUMN_BAND));
				c.setTxPowerWatts(rs.getFloat(DigitalContact.COLUMN_TX_POWER_WATTS));
				c.setGrid(rs.getString(DigitalContact.COLUMN_GRID));
				c.setLocationId(rs.getInt(DigitalContact.COLUMN_LOCATION_ID));
				c.setCreatedAt(rs.getInt(DigitalContact.COLUMN_CREATED_AT));
				results.add(c);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			try { if (rs != null) rs.close(); } catch (SQLException e) { e.printStackTrace(); }
			try { if (ps != null) ps.close(); } catch (SQLException e) { e.printStackTrace(); }
			try { conn.close(); } catch (SQLException e) { e.printStackTrace(); }
		}
		setData(results);
		fireTableDataChanged();
	}

	/**
	 * Saves a new digital contact to the database and refreshes the table
	 * so the new entry appears immediately at the top of the list.
	 *
	 * @param contact the completed contact record to save
	 */
	public void insert(DigitalContact contact) {
		Connection db = Data.getConnection();
		PreparedStatement ps = null;
		try {
			ps = db.prepareStatement(
				"INSERT INTO digital_contacts "
				+ "(callsign, frequency_mhz, digital_mode_id, signal_report_sent, "
				+ "signal_report_received, notes, date_on, time_on, time_off, "
				+ "my_callsign, band, tx_power_watts, grid, location_id, created_at) "
				+ "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
			ps.setString(1, contact.getCallsign());
			ps.setFloat(2, contact.getFrequencyMhz());
			ps.setInt(3, contact.getDigitalModeId());
			ps.setString(4, contact.getSignalReportSent());
			ps.setString(5, contact.getSignalReportReceived());
			ps.setString(6, contact.getNotes());
			ps.setInt(7, contact.getDateOn());
			ps.setString(8, contact.getTimeOn());
			ps.setString(9, contact.getTimeOff());
			ps.setString(10, contact.getMyCallsign());
			ps.setString(11, contact.getBand());
			ps.setFloat(12, contact.getTxPowerWatts());
			ps.setString(13, contact.getGrid());
			if (contact.getLocationId() != 0) {
				ps.setInt(14, contact.getLocationId());
			} else {
				ps.setNull(14, Types.INTEGER);
			}
			ps.setInt(15, (int) (System.currentTimeMillis() / 1000L));
			ps.execute();
			setData(loadAll());
			fireTableDataChanged();
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			try { if (ps != null) ps.close(); } catch (SQLException e) { e.printStackTrace(); }
			try { db.close(); } catch (SQLException e) { e.printStackTrace(); }
		}
	}

	/**
	 * Deletes the given contact from the database and removes it from the
	 * table display.
	 *
	 * @param contact the contact to delete from your logbook
	 */
	public void delete(DigitalContact contact) {
		Connection db = Data.getConnection();
		PreparedStatement ps = null;
		try {
			ps = db.prepareStatement("DELETE FROM digital_contacts WHERE id = ?");
			ps.setInt(1, contact.getId());
			ps.execute();
			setData(loadAll());
			fireTableDataChanged();
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			try { if (ps != null) ps.close(); } catch (SQLException e) { e.printStackTrace(); }
			try { db.close(); } catch (SQLException e) { e.printStackTrace(); }
		}
	}

	/**
	 * Returns the {@link DigitalContact} displayed in the given table row,
	 * allowing callers to retrieve the full record when the user selects
	 * a row for editing or deletion.
	 *
	 * @param rowIndex the zero-based row index in the table
	 * @return the contact at that row, or {@code null} if the index is out of range
	 */
	public DigitalContact getContactAt(int rowIndex) {
		if (rowIndex < 0 || rowIndex >= data.size()) return null;
		return data.get(rowIndex);
	}

	/**
	 * Reloads all contacts from the database and refreshes every row in
	 * the table. Use this after external changes to the data.
	 */
	public void refresh() {
		setData(loadAll());
		fireTableDataChanged();
	}

	@Override
	public int getRowCount() {
		return data == null ? 0 : data.size();
	}

	@Override
	public int getColumnCount() {
		return columns.length;
	}

	@Override
	public String getColumnName(int col) {
		return columns[col];
	}

	@Override
	public boolean isCellEditable(int row, int col) {
		return false;
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		if (data == null || rowIndex < 0 || rowIndex >= data.size()) return null;
		DigitalContact c = data.get(rowIndex);
		switch (columnIndex) {
			case COLUMN_NUM_DATE_ON:
				return Utilities.unixTimestampToString(c.getDateOn(), "MM/dd/yyyy");
			case COLUMN_NUM_TIME_ON:
				return c.getTimeOn();
			case COLUMN_NUM_CALLSIGN:
				return c.getCallsign();
			case COLUMN_NUM_BAND:
				return c.getBand();
			case COLUMN_NUM_FREQUENCY_MHZ:
				return String.valueOf(c.getFrequencyMhz());
			case COLUMN_NUM_MODE:
				return String.valueOf(c.getDigitalModeId());
			case COLUMN_NUM_SIGNAL_REPORT_SENT:
				return c.getSignalReportSent();
			case COLUMN_NUM_SIGNAL_REPORT_RECEIVED:
				return c.getSignalReportReceived();
			case COLUMN_NUM_NOTES:
				return c.getNotes();
			default:
				return null;
		}
	}

	/**
	 * Returns the full list of contacts currently displayed in the table.
	 */
	public ArrayList<DigitalContact> getData() {
		return data;
	}

	/**
	 * Replaces the entire data set used by the table. The display is not
	 * automatically refreshed; call {@link #fireTableDataChanged()} after
	 * this method if an immediate update is needed.
	 */
	public void setData(ArrayList<DigitalContact> contacts) {
		this.data = contacts;
	}

	@Override
	public void tableChanged(TableModelEvent e) {
		// no-op: data is refreshed before fireTableDataChanged() in each mutating operation
	}
}
