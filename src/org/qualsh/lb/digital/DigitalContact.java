package org.qualsh.lb.digital;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

import org.qualsh.lb.data.Data;

/**
 * Represents a single logged contact made using a digital mode. A digital
 * contact records the callsign of the station you worked, the frequency and
 * mode used, signal reports exchanged, your Maidenhead grid locator, and the
 * date and time of the contact in UTC.
 */
public class DigitalContact {

	private int id;
	private String callsign;
	private float frequencyMhz;
	private int digitalModeId;
	private String signalReportSent;
	private String signalReportReceived;
	private String notes;
	private int dateOn;
	private String timeOn;
	private String timeOff;
	private String myCallsign;
	private String band;
	private float txPowerWatts;
	private String grid;
	private int locationId;
	private int createdAt;

	public static final String COLUMN_ID                    = "id";
	public static final String COLUMN_CALLSIGN              = "callsign";
	public static final String COLUMN_FREQUENCY_MHZ         = "frequency_mhz";
	public static final String COLUMN_DIGITAL_MODE_ID       = "digital_mode_id";
	public static final String COLUMN_SIGNAL_REPORT_SENT    = "signal_report_sent";
	public static final String COLUMN_SIGNAL_REPORT_RECEIVED = "signal_report_received";
	public static final String COLUMN_NOTES                 = "notes";
	public static final String COLUMN_DATE_ON               = "date_on";
	public static final String COLUMN_TIME_ON               = "time_on";
	public static final String COLUMN_TIME_OFF              = "time_off";
	public static final String COLUMN_MY_CALLSIGN           = "my_callsign";
	public static final String COLUMN_BAND                  = "band";
	public static final String COLUMN_TX_POWER_WATTS        = "tx_power_watts";
	public static final String COLUMN_GRID                  = "grid";
	public static final String COLUMN_LOCATION_ID           = "location_id";
	public static final String COLUMN_CREATED_AT            = "created_at";

	/** Creates an empty digital contact record. */
	public DigitalContact() {}

	/**
	 * Creates a digital contact pre-filled with the minimum fields needed
	 * to save a log entry.
	 *
	 * @param callsign      the callsign of the station you contacted
	 * @param frequencyMhz  the operating frequency in megahertz
	 * @param digitalModeId the identifier of the digital mode used
	 * @param dateOn        the date of the contact as a Unix timestamp
	 */
	public DigitalContact(String callsign, float frequencyMhz, int digitalModeId, int dateOn) {
		this.callsign = callsign;
		this.frequencyMhz = frequencyMhz;
		this.digitalModeId = digitalModeId;
		this.dateOn = dateOn;
	}

	/**
	 * Saves any changes made to this contact back to the logbook database.
	 * Returns {@code true} if the update succeeded.
	 */
	public boolean update() {
		Connection db = Data.getConnection();
		PreparedStatement ps = null;
		try {
			ps = db.prepareStatement(
				"UPDATE digital_contacts SET "
				+ "callsign = ?, frequency_mhz = ?, digital_mode_id = ?, "
				+ "signal_report_sent = ?, signal_report_received = ?, notes = ?, "
				+ "date_on = ?, time_on = ?, time_off = ?, my_callsign = ?, "
				+ "band = ?, tx_power_watts = ?, grid = ?, location_id = ? "
				+ "WHERE id = ?");
			ps.setString(1, callsign);
			ps.setFloat(2, frequencyMhz);
			ps.setInt(3, digitalModeId);
			ps.setString(4, signalReportSent);
			ps.setString(5, signalReportReceived);
			ps.setString(6, notes);
			ps.setInt(7, dateOn);
			ps.setString(8, timeOn);
			ps.setString(9, timeOff);
			ps.setString(10, myCallsign);
			ps.setString(11, band);
			ps.setFloat(12, txPowerWatts);
			ps.setString(13, grid);
			if (locationId != 0) {
				ps.setInt(14, locationId);
			} else {
				ps.setNull(14, Types.INTEGER);
			}
			ps.setInt(15, id);
			ps.execute();
			return true;
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			try { if (ps != null) ps.close(); } catch (SQLException e) { e.printStackTrace(); }
			try { db.close(); } catch (SQLException e) { e.printStackTrace(); }
		}
		return false;
	}

	/**
	 * Returns the unique database identifier for this digital contact.
	 */
	public int getId() {
		return id;
	}

	/**
	 * Sets the unique database identifier for this digital contact.
	 */
	public void setId(int id) {
		this.id = id;
	}

	/**
	 * Returns the callsign of the station you contacted, for example "W1AW"
	 * or "G3XYZ".
	 */
	public String getCallsign() {
		return callsign;
	}

	/**
	 * Sets the callsign of the station you contacted.
	 */
	public void setCallsign(String callsign) {
		this.callsign = callsign;
	}

	/**
	 * Returns the dial frequency in megahertz at which this contact was made.
	 */
	public float getFrequencyMhz() {
		return frequencyMhz;
	}

	/**
	 * Sets the operating frequency in megahertz for this contact.
	 */
	public void setFrequencyMhz(float frequencyMhz) {
		this.frequencyMhz = frequencyMhz;
	}

	/**
	 * Returns the identifier of the digital mode used for this contact.
	 * Use this value to look up the full mode details in your mode list.
	 */
	public int getDigitalModeId() {
		return digitalModeId;
	}

	/**
	 * Sets which digital mode was used for this contact.
	 */
	public void setDigitalModeId(int digitalModeId) {
		this.digitalModeId = digitalModeId;
	}

	/**
	 * Returns the signal report you sent to the other station, for example
	 * "-10" for an FT8 dB report or "599" for a traditional RST report.
	 */
	public String getSignalReportSent() {
		return signalReportSent;
	}

	/**
	 * Sets the signal report you sent to the other station.
	 */
	public void setSignalReportSent(String signalReportSent) {
		this.signalReportSent = signalReportSent;
	}

	/**
	 * Returns the signal report you received from the other station.
	 */
	public String getSignalReportReceived() {
		return signalReportReceived;
	}

	/**
	 * Sets the signal report you received from the other station.
	 */
	public void setSignalReportReceived(String signalReportReceived) {
		this.signalReportReceived = signalReportReceived;
	}

	/**
	 * Returns any free-text notes you saved with this contact.
	 */
	public String getNotes() {
		return notes;
	}

	/**
	 * Sets free-text notes for this contact.
	 */
	public void setNotes(String notes) {
		this.notes = notes;
	}

	/**
	 * Returns the date of this contact as a Unix timestamp (seconds since
	 * 1970-01-01 UTC).
	 */
	public int getDateOn() {
		return dateOn;
	}

	/**
	 * Sets the date of this contact as a Unix timestamp.
	 */
	public void setDateOn(int dateOn) {
		this.dateOn = dateOn;
	}

	/**
	 * Returns the UTC time the contact started, in HH:MM format.
	 */
	public String getTimeOn() {
		return timeOn;
	}

	/**
	 * Sets the UTC start time for this contact.
	 */
	public void setTimeOn(String timeOn) {
		this.timeOn = timeOn;
	}

	/**
	 * Returns the UTC time the contact ended, in HH:MM format.
	 */
	public String getTimeOff() {
		return timeOff;
	}

	/**
	 * Sets the UTC end time for this contact.
	 */
	public void setTimeOff(String timeOff) {
		this.timeOff = timeOff;
	}

	/**
	 * Returns your own callsign as used during this contact.
	 */
	public String getMyCallsign() {
		return myCallsign;
	}

	/**
	 * Sets your callsign for this contact (useful when operating portable
	 * or with a special event callsign).
	 */
	public void setMyCallsign(String myCallsign) {
		this.myCallsign = myCallsign;
	}

	/**
	 * Returns the amateur band on which this contact took place, for example
	 * "40m" or "20m".
	 */
	public String getBand() {
		return band;
	}

	/**
	 * Sets the amateur band for this contact.
	 */
	public void setBand(String band) {
		this.band = band;
	}

	/**
	 * Returns the transmit power used during this contact, in watts.
	 */
	public float getTxPowerWatts() {
		return txPowerWatts;
	}

	/**
	 * Sets the transmit power in watts for this contact.
	 */
	public void setTxPowerWatts(float txPowerWatts) {
		this.txPowerWatts = txPowerWatts;
	}

	/**
	 * Returns your Maidenhead grid locator at the time of this contact,
	 * for example "FN31" or "IO91".
	 */
	public String getGrid() {
		return grid;
	}

	/**
	 * Sets your Maidenhead grid locator for this contact.
	 */
	public void setGrid(String grid) {
		this.grid = grid;
	}

	/**
	 * Returns the identifier of the saved location linked to this contact,
	 * or {@code 0} if no location has been associated.
	 */
	public int getLocationId() {
		return locationId;
	}

	/**
	 * Links this contact to a saved location by its database identifier.
	 */
	public void setLocationId(int locationId) {
		this.locationId = locationId;
	}

	/**
	 * Returns the timestamp recording when this contact entry was first
	 * created in the logbook, as a Unix timestamp.
	 */
	public int getCreatedAt() {
		return createdAt;
	}

	/**
	 * Sets the creation timestamp for this contact entry.
	 */
	public void setCreatedAt(int createdAt) {
		this.createdAt = createdAt;
	}

	/**
	 * Returns a brief summary of this contact showing the callsign, band,
	 * and mode identifier, suitable for display in lists and tables.
	 */
	@Override
	public String toString() {
		return callsign + " [" + band + ", mode " + digitalModeId + "]";
	}
}
