package org.qualsh.lb.data;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

import javax.swing.AbstractListModel;

import org.qualsh.lb.place.Place;

public class ViewPlacesModel extends AbstractListModel<Place> {

	private static final long serialVersionUID = 2837461927364810293L;
	private ArrayList<Place> data = new ArrayList<Place>();

	public int getSize() {
		return data.size();
	}

	public Place getElementAt(int index) {
		return data.get(index);
	}

	public void setAllPlaces() {
		this.data.clear();

		Connection conn = Data.getConnection();
		Statement st = null;
		ResultSet rs = null;
		try {
			st = conn.createStatement();
			rs = st.executeQuery("SELECT id, place_name, lat, lon FROM places ORDER BY place_name ASC");

			while (rs.next()) {
				Place place = new Place();
				place.setId(rs.getInt(Place.COL_ID));
				place.setPlaceName(rs.getString(Place.COL_PLACENAME));
				place.setLatitude(rs.getString(Place.COL_LAT));
				place.setLongitude(rs.getString(Place.COL_LON));
				data.add(place);
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

	public ArrayList<Place> getData() {
		return data;
	}

}
