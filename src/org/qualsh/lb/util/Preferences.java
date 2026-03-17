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
