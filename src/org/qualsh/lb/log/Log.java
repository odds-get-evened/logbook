package org.qualsh.lb.log;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.qualsh.lb.data.Data;
import org.qualsh.lb.location.Location;
import org.qualsh.lb.place.Place;

public class Log {
	private int id;
	private int dateOn;
	private float frequency;
	private String mode;
	private String description;
	private int location;
	private int myPlace;
	private int createdAt;

	public Log() {
		
	}

	public int getDateOn() {
		return dateOn;
	}
	
	public String getDateOn(String format) {
		
		return null;
	}

	public void setDateOn(int dateOn) {
		this.dateOn = dateOn;
	}

	public float getFrequency() {
		return frequency;
	}

	public void setFrequency(float frequency) {
		this.frequency = frequency;
	}

	public String getMode() {
		return mode;
	}

	public void setMode(String mode) {
		this.mode = mode;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public boolean update() {
		Connection db = Data.getConnection();
		PreparedStatement ps = null;
		try {
			ps = db.prepareStatement("UPDATE logs "
					+ "SET freq = ?, "
					+ "mode = ?, "
					+ "dateon = ?, "
					+ "description = ?, "
					+ "location = ?, "
					+ "my_place = ? "
					+ "WHERE id = ?");
			ps.setFloat(1, getFrequency());
			ps.setString(2, getMode());
			ps.setInt(3, getDateOn());
			ps.setString(4, getDescription());
			
			if(this.getLocation() != 0) {
				ps.setInt(5, this.getLocation());
			} else {
				ps.setNull(5, Types.INTEGER);
			}
			
			if(this.getMyPlace() != 0) {
				ps.setInt(6, this.getMyPlace());
			} else {
				ps.setNull(6, Types.INTEGER);
			}
			
			ps.setInt(7, getId());
			
			ps.execute();
			
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			try {
				ps.close();
			} catch (SQLException e1) {
				e1.printStackTrace();
			}
			
			try {
				db.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		
		return false;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public int getLocation() {
		return location;
	}

	public void setLocation(int location) {
		this.location = location;
	}
	
	public Location getFullLocation() {
		Connection conn = Data.getConnection();
		
		if(this.getLocation() == 0) {
			return null;
		}
		
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = conn.prepareStatement("SELECT lang,lng,lat,time_off,time_on,frequency,location,station_id,id  FROM locations WHERE id = ?");
			ps.setInt(1, this.getLocation());
			rs = ps.executeQuery();
			
			Location loc = new Location();
			loc.setLanguage(rs.getString(Location.COLUMN_LANGUAGE));
			loc.setId(rs.getInt(Location.COLUMN_ID));
			loc.setLocationName(rs.getString(Location.COLUMN_LOCATION));
			loc.setStationId(rs.getInt(Location.COLUMN_STATIONID));
			loc.setStrFrequency(rs.getString(Location.COLUMN_FREQUENCY));
			loc.setStrLatitude(rs.getString(Location.COLUMN_LATITUDE));
			loc.setStrLongitude(rs.getString(Location.COLUMN_LONGITUDE));
			loc.setStrTimeOff(rs.getString(Location.COLUMN_TIMEOFF));
			loc.setStrTimeOn(rs.getString(Location.COLUMN_TIMEON));
			
			return loc;
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
			} catch (SQLException e1) {
				e1.printStackTrace();
			}
			
			try {
				conn.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		
		return null;
	}
	
	public Place getFullTxPlace() {
		if (this.getLocation() == 0) return null;
		return Place.getOne(this.getLocation());
	}

	public Place getFullMyPlace() {
		return Place.getOne(this.getMyPlace());
	}

	public int getMyPlace() {
		return myPlace;
	}

	public void setMyPlace(int myPlace) {
		this.myPlace = myPlace;
	}

	public int getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(int createdAt) {
		this.createdAt = createdAt;
	}
	
	public String toString() {
		return "{id:"+this.getId()+", frequency:"+this.getFrequency()+", "
				+ "dateon:"+this.getDateOn()+", "
				+ "mode:"+this.getMode()+", "
				+ "description:" +this.getDescription()+ ", "
				+ "location:"+this.getLocation()+ ", "
				+ "my_place:"+this.getMyPlace()+"}";
	}

	public boolean hasLocation() {
		if(this.getLocation() != 0) {
			return true;
		}
		
		return false;
	}

}
