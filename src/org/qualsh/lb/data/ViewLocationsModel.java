package org.qualsh.lb.data;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

import javax.swing.AbstractListModel;

import org.qualsh.lb.location.Location;

public class ViewLocationsModel extends AbstractListModel<Location> {

	private static final long serialVersionUID = -5946662304220936668L;
	private ArrayList<Location> data = new ArrayList<Location>();

	public int getSize() {
		return data.size();
	}

	public Location getElementAt(int index) {
		return data.get(index);
	}
	
	public void setAllLocations() {
		this.data.clear();
		
		Connection conn = Data.getConnection();
		Statement st = null;
		ResultSet rs = null;
		try {
			st = conn.createStatement();
			rs = st.executeQuery("SELECT lang, lng, lat, time_off, time_on, frequency, location, station_id, id FROM locations ORDER BY location ASC");
			
			while(rs.next()) {
				Location loc = new Location();
				loc.setId(rs.getInt(Location.COLUMN_ID));
				loc.setLanguage(rs.getString(Location.COLUMN_LANGUAGE));
				loc.setLocationName(rs.getString(Location.COLUMN_LOCATION));
				loc.setStationId(rs.getInt(Location.COLUMN_STATIONID));
				loc.setStrFrequency(rs.getString(Location.COLUMN_FREQUENCY));
				loc.setStrLatitude(rs.getString(Location.COLUMN_LATITUDE));
				loc.setStrLongitude(rs.getString(Location.COLUMN_LONGITUDE));
				loc.setStrTimeOff(rs.getString(Location.COLUMN_TIMEOFF));
				loc.setStrTimeOn(rs.getString(Location.COLUMN_TIMEON));
				
				data.add(loc);
			}
			
			this.fireContentsChanged(this, 0, getSize());
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
	}

	public void setLocationsByStation(int id) {
		this.data.clear();
		
		Connection conn = Data.getConnection();
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = conn.prepareStatement("SELECT lang, lng, lat, time_off, time_on, frequency, location, station_id, id FROM locations WHERE station_id = ? ORDER BY location ASC");
			ps.setInt(1, id);
			
			rs = ps.executeQuery();
			
			while(rs.next()) {
				Location loc = new Location();
				loc.setId(rs.getInt(Location.COLUMN_ID));
				loc.setLanguage(rs.getString(Location.COLUMN_LANGUAGE));
				loc.setLocationName(rs.getString(Location.COLUMN_LOCATION));
				loc.setStationId(rs.getInt(Location.COLUMN_STATIONID));
				loc.setStrFrequency(rs.getString(Location.COLUMN_FREQUENCY));
				loc.setStrLatitude(rs.getString(Location.COLUMN_LATITUDE));
				loc.setStrLongitude(rs.getString(Location.COLUMN_LONGITUDE));
				loc.setStrTimeOff(rs.getString(Location.COLUMN_TIMEOFF));
				loc.setStrTimeOn(rs.getString(Location.COLUMN_TIMEON));
				
				data.add(loc);
			}
			
			this.fireContentsChanged(this, 0, getSize());
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
	}

	public ArrayList<Location> getData() {
		return data;
	}

	public void setData(ArrayList<Location> data) {
		this.data = data;
	}

}
