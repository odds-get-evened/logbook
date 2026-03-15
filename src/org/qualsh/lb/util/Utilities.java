package org.qualsh.lb.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.qualsh.lb.util.Geocode;

public class Utilities {

	public Utilities() {}
	
	public static Object geocode(String q) throws MalformedURLException, IOException, org.json.simple.parser.ParseException {
		URL url = new URL("http://nominatim.openstreetmap.org/search?q="+ URLEncoder.encode(q, "UTF-8") +"&format=json&limit=1&addressdetails=1");
		
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setRequestMethod("GET");
		conn.setRequestProperty("User-Agent", "Mozilla/5.0");
				
		BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
		String inputLine;
		StringBuffer response = new StringBuffer();
		
		while((inputLine = in.readLine()) != null) {
			response.append(inputLine);
		}
		in.close();
		
		JSONParser parser = new JSONParser();
		Object obj = parser.parse(response.toString());
		JSONArray ary = (JSONArray) obj;
		JSONObject place = (JSONObject) ary.get(0);
		
		JSONObject address = (JSONObject) place.get("address");
		double dblLat = (double) Math.round(Double.parseDouble((String) place.get("lat")) * 1000) / 1000;
		double dblLon = (double) Math.round(Double.parseDouble((String) place.get("lon")) * 1000) / 1000;
		Geocode gc = new Geocode(String.valueOf(dblLat), String.valueOf(dblLon), String.valueOf(address.get("city")), String.valueOf(address.get("state")));
		
		return gc;
	}
	
	public static boolean isValidDate(String dateToValidate, String format) {
		
		if(dateToValidate == null) {
			return false;
		}
		
		SimpleDateFormat sdf = new SimpleDateFormat(format);
		sdf.setLenient(false);
		
		try {
			sdf.parse(dateToValidate);
			return true;
		} catch (ParseException e) {
			return false;
		}
	}
	
	public static String unixTimestampToString(int timestamp, String format) {
		Calendar cal = new GregorianCalendar();
		cal.setTimeInMillis(((long) timestamp * 1000));
		SimpleDateFormat sdf = new SimpleDateFormat(format);
		Date date = cal.getTime();
		
		return sdf.format(date);
	}
	
	public static int stringToUnixTimeStamp(String date, String format) {
		SimpleDateFormat sdf = new SimpleDateFormat(format);
		
		int secs = 0;
		
		try {
			Date parsedDate = sdf.parse(date);
			long ms = parsedDate.getTime();
			secs = (int) (ms/1000);
		} catch (ParseException e) {}
		
		return secs;
	}

}
