package org.qualsh.lb.util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.qualsh.lb.data.Data;
import org.qualsh.lb.place.Place;


public class Preferences {

	public static final String PREF_NAME_MY_PLACE = "my_place";
	public static final String PREF_NAME_THEME = "ui_theme";
	public static final String PREF_NAME_DB_PATH = "db_path";
	public static final String PREF_NAME_DEFAULT_PLACE = "default_place";

	// CAT / rig control
	public static final String PREF_CAT_TYPE               = "cat_type";               // RIGCTLD | SERIAL
	public static final String PREF_CAT_RIGCTLD_HOST       = "cat_rigctld_host";
	public static final String PREF_CAT_RIGCTLD_PORT       = "cat_rigctld_port";
	public static final String PREF_CAT_SERIAL_PORT        = "cat_serial_port";
	public static final String PREF_CAT_SERIAL_BAUD        = "cat_serial_baud";
	public static final String PREF_CAT_SERIAL_PROTOCOL    = "cat_serial_protocol";    // YAESU | KENWOOD | ICOM
	public static final String PREF_CAT_ICOM_ADDRESS       = "cat_icom_address";       // hex string e.g. "A4"

	// DX Cluster
	public static final String PREF_DX_CLUSTER_HOST        = "dx_cluster_host";
	public static final String PREF_DX_CLUSTER_PORT        = "dx_cluster_port";        // default 7300
	public static final String PREF_DX_CLUSTER_CALLSIGN    = "dx_cluster_callsign";    // login callsign

	// Digital modes
	public static final String PREF_DIGITAL_PTT_METHOD        = "digital_ptt_method";        // VOX | RTS | DTR | CAT
	public static final String PREF_DIGITAL_PTT_PORT          = "digital_ptt_port";          // serial port for RTS/DTR PTT
	public static final String PREF_DIGITAL_CAPTURE_DEVICE    = "digital_capture_device";    // javax.sound mixer name
	public static final String PREF_DIGITAL_PLAYBACK_DEVICE   = "digital_playback_device";   // javax.sound mixer name
	public static final String PREF_DIGITAL_WSJTX_UDP_PORT    = "digital_wsjtx_udp_port";    // default 2237
	public static final String PREF_DIGITAL_JS8CALL_UDP_PORT  = "digital_js8call_udp_port";  // default 2242

	public Preferences() {
		
	}

	public static void save(String name, String value) {
		Connection db = Data.getConnection();
		PreparedStatement ps = null;
		try {
			ps = db.prepareStatement("INSERT OR REPLACE INTO preferences ("
					+ "id, pref_name, pref_value) VALUES ("
					+ "(SELECT id FROM preferences WHERE pref_name = ?), ?, ?)");
			ps.setString(1, name);
			ps.setString(2, name);
			ps.setString(3, value);
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
				db.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}

	public static void saveMyPlace(Place place) {
		Connection db = Data.getConnection();
		PreparedStatement ps = null;
		try {
			ps = db.prepareStatement("INSERT OR REPLACE INTO preferences ("
					+ "id, pref_name, pref_value) VALUES ("
					+ "(SELECT id FROM preferences WHERE pref_name = ?), ?, ?)");
			ps.setString(1, PREF_NAME_MY_PLACE);
			ps.setString(2, PREF_NAME_MY_PLACE);
			ps.setString(3, String.valueOf(place.getId()));
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
				db.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}

	public static void remove(String preferenceName) {
		Connection db = Data.getConnection();
		PreparedStatement ps = null;
		try {
			ps = db.prepareStatement("DELETE FROM preferences WHERE pref_name LIKE ?");
			ps.setString(1, preferenceName);
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
				db.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}
	
	public static String getOne(String preferenceName) {
		Connection db = Data.getConnection();
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = db.prepareStatement("SELECT * FROM preferences WHERE pref_name LIKE ?");
			ps.setString(1, preferenceName);
			rs = ps.executeQuery();
			if(rs.next()) {
				return rs.getString("pref_value");
			} else {
				return null;
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			try {
				rs.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
			
			try {
				ps.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
			
			try {
				db.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		
		return null;
	}
}
