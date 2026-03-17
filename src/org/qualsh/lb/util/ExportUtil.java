package org.qualsh.lb.util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import org.qualsh.lb.data.LogsModel;
import org.qualsh.lb.location.Location;
import org.qualsh.lb.log.Log;
import org.qualsh.lb.place.Place;

/**
 * Utility for exporting log entries to CSV, JSON, and ADIF formats.
 */
public class ExportUtil {

	private ExportUtil() {}

	// ── CSV ──────────────────────────────────────────────────────────────────

	public static void exportCsv(File file) throws IOException {
		ArrayList<Log> logs = new LogsModel().getLogList();
		StringBuilder sb = new StringBuilder();
		sb.append("Date,Time (UTC),Frequency (kHz),Mode,RX Location,Description,TX Location\n");
		for (Log log : logs) {
			sb.append(csvEscape(Utilities.unixTimestampToString(log.getDateOn(), "MM/dd/yyyy"))).append(',');
			sb.append(csvEscape(Utilities.unixTimestampToString(log.getDateOn(), "HH:mm"))).append(',');
			sb.append(csvEscape(String.valueOf(log.getFrequency()))).append(',');
			sb.append(csvEscape(log.getMode())).append(',');
			Place rxPlace = log.getFullMyPlace();
			sb.append(csvEscape(rxPlace != null ? rxPlace.getPlaceName() : "")).append(',');
			sb.append(csvEscape(log.getDescription())).append(',');
			Location txLoc = log.getFullLocation();
			sb.append(csvEscape(txLoc != null ? txLoc.getLocationName() : "")).append('\n');
		}
		try (FileWriter fw = new FileWriter(file)) {
			fw.write(sb.toString());
		}
	}

	private static String csvEscape(String value) {
		if (value == null) return "";
		if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
			return "\"" + value.replace("\"", "\"\"") + "\"";
		}
		return value;
	}

	// ── JSON ─────────────────────────────────────────────────────────────────

	public static void exportJson(File file) throws IOException {
		ArrayList<Log> logs = new LogsModel().getLogList();
		StringBuilder sb = new StringBuilder();
		sb.append("[\n");
		for (int i = 0; i < logs.size(); i++) {
			Log log = logs.get(i);
			Place rxPlace = log.getFullMyPlace();
			Location txLoc = log.getFullLocation();
			sb.append("  {\n");
			sb.append("    \"id\": ").append(log.getId()).append(",\n");
			sb.append("    \"date\": \"").append(jsonEscape(Utilities.unixTimestampToString(log.getDateOn(), "MM/dd/yyyy"))).append("\",\n");
			sb.append("    \"time_utc\": \"").append(jsonEscape(Utilities.unixTimestampToString(log.getDateOn(), "HH:mm"))).append("\",\n");
			sb.append("    \"frequency_khz\": ").append(log.getFrequency()).append(",\n");
			sb.append("    \"mode\": \"").append(jsonEscape(log.getMode())).append("\",\n");
			sb.append("    \"description\": \"").append(jsonEscape(log.getDescription())).append("\",\n");
			if (rxPlace != null) {
				sb.append("    \"rx_location\": {\n");
				sb.append("      \"name\": \"").append(jsonEscape(rxPlace.getPlaceName())).append("\",\n");
				sb.append("      \"lat\": \"").append(jsonEscape(rxPlace.getLatitude())).append("\",\n");
				sb.append("      \"lon\": \"").append(jsonEscape(rxPlace.getLongitude())).append("\"\n");
				sb.append("    },\n");
			} else {
				sb.append("    \"rx_location\": null,\n");
			}
			if (txLoc != null) {
				sb.append("    \"tx_location\": {\n");
				sb.append("      \"name\": \"").append(jsonEscape(txLoc.getLocationName())).append("\",\n");
				sb.append("      \"lat\": \"").append(jsonEscape(txLoc.getStrLatitude())).append("\",\n");
				sb.append("      \"lon\": \"").append(jsonEscape(txLoc.getStrLongitude())).append("\",\n");
				sb.append("      \"frequency\": \"").append(jsonEscape(txLoc.getStrFrequency())).append("\",\n");
				sb.append("      \"time_on\": \"").append(jsonEscape(txLoc.getStrTimeOn())).append("\",\n");
				sb.append("      \"time_off\": \"").append(jsonEscape(txLoc.getStrTimeOff())).append("\",\n");
				sb.append("      \"language\": \"").append(jsonEscape(txLoc.getLanguage())).append("\"\n");
				sb.append("    }\n");
			} else {
				sb.append("    \"tx_location\": null\n");
			}
			sb.append("  }");
			if (i < logs.size() - 1) sb.append(',');
			sb.append('\n');
		}
		sb.append("]\n");
		try (FileWriter fw = new FileWriter(file)) {
			fw.write(sb.toString());
		}
	}

	private static String jsonEscape(String value) {
		if (value == null) return "";
		return value
			.replace("\\", "\\\\")
			.replace("\"", "\\\"")
			.replace("\n", "\\n")
			.replace("\r", "\\r")
			.replace("\t", "\\t");
	}

	// ── ADIF ─────────────────────────────────────────────────────────────────

	/**
	 * Exports logs in ADIF (Amateur Data Interchange Format) 3.1.4.
	 * Maps radio log fields to the closest ADIF equivalents.
	 */
	public static void exportAdif(File file) throws IOException {
		ArrayList<Log> logs = new LogsModel().getLogList();
		StringBuilder sb = new StringBuilder();

		// ADIF header
		sb.append("ADIF Export from LogBook\n");
		sb.append("<ADIF_VER:5>3.1.4 ");
		sb.append("<PROGRAMID:7>LogBook ");
		sb.append("<PROGRAMVERSION:3>1.0 ");
		sb.append("<EOH>\n\n");

		for (Log log : logs) {
			Place rxPlace = log.getFullMyPlace();
			Location txLoc = log.getFullLocation();

			// FREQ in MHz (ADIF uses MHz, log stores kHz)
			double freqMhz = log.getFrequency() / 1000.0;
			String freqStr = String.format("%.6f", freqMhz);
			sb.append(adifField("FREQ", freqStr));

			// MODE
			sb.append(adifField("MODE", mapMode(log.getMode())));

			// QSO_DATE: YYYYMMDD
			String qsoDate = Utilities.unixTimestampToString(log.getDateOn(), "yyyyMMdd");
			sb.append(adifField("QSO_DATE", qsoDate));

			// TIME_ON: HHMM
			String timeOn = Utilities.unixTimestampToString(log.getDateOn(), "HHmm");
			sb.append(adifField("TIME_ON", timeOn));

			// COMMENT: description
			if (log.getDescription() != null && !log.getDescription().isBlank()) {
				sb.append(adifField("COMMENT", log.getDescription()));
			}

			// TX location fields
			if (txLoc != null) {
				// STATION_CALLSIGN: TX station name
				String txName = txLoc.getLocationName();
				if (txName != null && !txName.isBlank()) {
					sb.append(adifField("STATION_CALLSIGN", txName));
				}
				// QTH: TX location name
				sb.append(adifField("QTH", txLoc.getLocationName() != null ? txLoc.getLocationName() : ""));
				// TX coordinates
				if (txLoc.getStrLatitude() != null && !txLoc.getStrLatitude().isBlank()) {
					sb.append(adifField("LAT", decimalToAdifLatLon(txLoc.getStrLatitude(), true)));
				}
				if (txLoc.getStrLongitude() != null && !txLoc.getStrLongitude().isBlank()) {
					sb.append(adifField("LON", decimalToAdifLatLon(txLoc.getStrLongitude(), false)));
				}
				// Language
				if (txLoc.getLanguage() != null && !txLoc.getLanguage().isBlank()) {
					sb.append(adifField("NOTES", "Language: " + txLoc.getLanguage()));
				}
			}

			// RX location fields
			if (rxPlace != null) {
				sb.append(adifField("MY_CITY", rxPlace.getPlaceName() != null ? rxPlace.getPlaceName() : ""));
				if (rxPlace.getLatitude() != null && !rxPlace.getLatitude().isBlank()) {
					sb.append(adifField("MY_LAT", decimalToAdifLatLon(rxPlace.getLatitude(), true)));
				}
				if (rxPlace.getLongitude() != null && !rxPlace.getLongitude().isBlank()) {
					sb.append(adifField("MY_LON", decimalToAdifLatLon(rxPlace.getLongitude(), false)));
				}
			}

			sb.append("<EOR>\n\n");
		}

		try (FileWriter fw = new FileWriter(file)) {
			fw.write(sb.toString());
		}
	}

	/**
	 * Encodes a single ADIF field: {@code <FIELDNAME:length>value}.
	 */
	private static String adifField(String name, String value) {
		if (value == null) value = "";
		return "<" + name + ":" + value.length() + ">" + value + " ";
	}

	/**
	 * Maps application mode strings to standard ADIF MODE values.
	 */
	private static String mapMode(String mode) {
		if (mode == null) return "AM";
		switch (mode.toUpperCase()) {
			case "AM":   return "AM";
			case "SSB":  return "SSB";
			case "USB":  return "USB";
			case "LSB":  return "LSB";
			case "CW":   return "CW";
			case "FM":   return "FM";
			case "RTTY": return "RTTY";
			case "PSK":  return "PSK31";
			case "FT8":  return "FT8";
			case "WSPR": return "WSPR";
			default:     return mode;
		}
	}

	/**
	 * Converts a decimal degree coordinate string to ADIF format.
	 * ADIF LAT/LON format: {@code XDDD MM.MMM} where X is N/S/E/W.
	 *
	 * @param decimalDeg decimal degrees string
	 * @param isLatitude true for latitude, false for longitude
	 */
	private static String decimalToAdifLatLon(String decimalDeg, boolean isLatitude) {
		try {
			double val = Double.parseDouble(decimalDeg.trim());
			char dir;
			if (isLatitude) {
				dir = val >= 0 ? 'N' : 'S';
			} else {
				dir = val >= 0 ? 'E' : 'W';
			}
			val = Math.abs(val);
			int degrees = (int) val;
			double minutes = (val - degrees) * 60.0;
			return String.format("%c%03d %06.3f", dir, degrees, minutes);
		} catch (NumberFormatException e) {
			return decimalDeg;
		}
	}
}
