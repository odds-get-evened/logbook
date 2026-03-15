package org.qualsh.lb.util;

public class Geocode {
	private String latitude;
	private String longitude;
	private String city;
	private String state;

	public Geocode() {

	}

	public Geocode(String lat, String lon, String city, String state) {
		this.setCity(city);
		this.setLatitude(lat);
		this.setLongitude(lon);
		this.setState(state);
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

	public String getCity() {
		return city;
	}

	public void setCity(String city) {
		this.city = city;
	}

	public String getState() {
		return state;
	}

	public void setState(String state) {
		this.state = state;
	}

}
