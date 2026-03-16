package org.qualsh.lb.place;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.qualsh.lb.data.Data;

public class Place {
	private int id;
	private String placeName;
	private String latitude;
	private String longitude;
	
	public static final String COL_PLACENAME = "place_name";
	public static final String COL_LAT = "lat";
	public static final String COL_LON = "lon";
	public static final String COL_ID = "id";

	public Place() {
		
	}
	
	public static void delete(int id) {
		Connection db = Data.getConnection();
		PreparedStatement ps = null;
		try {
			ps = db.prepareStatement("DELETE FROM places WHERE id LIKE ?");
			ps.setInt(1, id);
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
	
	public static Place getOne(int id) {
		Place place = new Place();
		
		Connection db = Data.getConnection();
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = db.prepareStatement("SELECT id, place_name, lat, lon"
					+ " FROM places"
					+ " WHERE id LIKE ? LIMIT 0,1");
			ps.setInt(1, id);
			rs = ps.executeQuery();
			
			if(rs.next()) {
				place.setId(rs.getInt(Place.COL_ID));
				place.setPlaceName(rs.getString(Place.COL_PLACENAME));
				place.setLatitude(rs.getString(Place.COL_LAT));
				place.setLongitude(rs.getString(Place.COL_LON));
				
				return place;
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
	
	public boolean update() {
		Connection db = Data.getConnection();
		PreparedStatement ps = null;
		try {
			ps = db.prepareStatement("UPDATE places SET place_name = ?, lat = ?, lon = ? WHERE id = ?");
			ps.setString(1, this.getPlaceName());
			ps.setString(2, this.getLatitude());
			ps.setString(3, this.getLongitude());
			ps.setInt(4, this.getId());
			ps.execute();
			return true;
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
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

	public int insert() {
		Connection db = Data.getConnection();
		try {
			PreparedStatement ps = db.prepareStatement("INSERT INTO places ("
					+ "place_name, lat, lon) VALUES ("
					+ "?, ?, ?)");
			ps.setString(1, this.getPlaceName());
			ps.setString(2, this.getLatitude());
			ps.setString(3, this.getLongitude());
			
			ps.execute();
			ps.close();
			db.close();
			
			return this.getLastId();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		return 0;
	}

	private int getLastId() {
		Connection db = Data.getConnection();
		
		Statement st = null;
		ResultSet rs = null;
		try {
			st = db.createStatement();
			rs = st.executeQuery("SELECT id FROM places ORDER BY id DESC LIMIT 0,1");
			return rs.getInt("id");
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			try {
				rs.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
			
			try {
				st.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
			
			try {
				db.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		
		return 0;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getPlaceName() {
		return placeName;
	}

	public void setPlaceName(String placeName) {
		this.placeName = placeName;
	}

	public String getLatitude() {
		return latitude;
	}

	public void setLatitude(String latitude) {
		this.latitude = latitude;
	}

	public String getLongitude() {
		return longitude;
	}

	public void setLongitude(String longitude) {
		this.longitude = longitude;
	}

}
