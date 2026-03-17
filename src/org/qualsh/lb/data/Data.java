package org.qualsh.lb.data;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

import org.qualsh.lb.language.Language;
import org.qualsh.lb.station.Station;

public class Data {
	
	private static String dbPath;
	private static ArrayList<Language> languages = new ArrayList<Language>();
	private static ArrayList<Station> stations = new ArrayList<Station>();

	public Data() {
		// Check for a user-saved DB path in OS-level preferences (avoids chicken-and-egg with SQLite prefs)
		java.util.prefs.Preferences jPrefs = java.util.prefs.Preferences.userNodeForPackage(Data.class);
		String savedDbPath = jPrefs.get("db_path", null);
		if (savedDbPath != null && !savedDbPath.isBlank()) {
			setDbPath(savedDbPath);
		} else {
			// set default DB file path
			setDbPath(System.getProperty("user.home") + System.getProperty("file.separator") + "LB" + System.getProperty("file.separator") + "lb.db");
		}
		
		langList();
		
		stationsList();
		
		// create DB file if it doesn't exist
		File f = new File(getDbPath());
		if(!f.exists()) {
			try {
				f.getParentFile().mkdirs();
				f.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		establishTables();
	}
	
	private void langList() {
		getLanguages().add(new Language("ara", "Arabic"));
		getLanguages().add(new Language("zho", "Chinese"));
		getLanguages().add(new Language("nld", "Dutch/Flemish"));
		getLanguages().add(new Language("eng", "English"));
		getLanguages().add(new Language("fra", "French"));
		getLanguages().add(new Language("deu", "German"));
		getLanguages().add(new Language("ita", "Italian"));
		getLanguages().add(new Language("jpn", "Japanese"));
		getLanguages().add(new Language("kor", "Korean"));
		getLanguages().add(new Language("pol", "Polish"));
		getLanguages().add(new Language("por", "Portuguese"));
		getLanguages().add(new Language("rus", "Russian"));
		getLanguages().add(new Language("spa", "Spanish"));
		getLanguages().add(new Language("swa", "Swahili"));
	}

	private void stationsList() {
		getStations().add(new Station("volmet", "VOLMET"));
		getStations().add(new Station("mwara", "MWARA"));
		getStations().add(new Station("all_india_radio", "All India Radio"));
		getStations().add(new Station("china_radio_international", "China Radio International"));
		getStations().add(new Station("ewtn_wewn_", "EWTN (WEWN)"));
		getStations().add(new Station("globe_wireless_arinc_", "Globe Wireless (ARINC)"));
		getStations().add(new Station("radio_africa", "Radio Africa"));
		getStations().add(new Station("radio_australia", "Radio Australia"));
		getStations().add(new Station("radio_free_asia", "Radio Free Asia"));
		getStations().add(new Station("radio_habana_cuba", "Radio Habana Cuba"));
		getStations().add(new Station("radio_nacional_brasilia", "Radio Nacional Brasilia"));
		getStations().add(new Station("radio_new_zealand_international_rnzi_", "Radio New Zealand International"));
		getStations().add(new Station("radio_nikkei", "Radio Nikkei"));
		getStations().add(new Station("rri", "Radio Romania International"));
		getStations().add(new Station("shipcom_llc", "ShipCom LLC"));
		getStations().add(new Station("hfgcs", "USAF High Frequency Global Communications System (HFGCS)"));
		getStations().add(new Station("uscg", "United States Coast Guard (USCG)"));
		getStations().add(new Station("voice_of_america_voa_", "Voice of America"));
		getStations().add(new Station("voice_of_korea_kcbs_", "Voice of Korea (KCBS)"));
		getStations().add(new Station("voice_of_vietnam", "Voice of Vietnam"));
		getStations().add(new Station("whri", "WHRI"));
		getStations().add(new Station("wwcr", "WWCR"));
	}

	public static Connection getConnection() {
		Connection connection = null;
		
		try {
			Class.forName("org.sqlite.JDBC");
			try {
				connection = DriverManager.getConnection("jdbc:sqlite:" + getDbPath());
			} catch (SQLException e) {
				e.printStackTrace();
			}
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		
		return connection;
	}

	public static void establishTables() {
		establishLogsTable();
		migrateLogsAddCreatedAt();
		establishLanguagesTable();
		establishStationsTable();
		establishLocationsTable();
		establishPreferencesTable();
		establishPlacesTable();
	}

	private static void migrateLogsAddCreatedAt() {
		Connection conn = getConnection();
		try {
			Statement st = conn.createStatement();
			st.executeUpdate("ALTER TABLE logs ADD COLUMN created_at INTEGER");
			st.close();
		} catch (SQLException e) {
			// Column already exists — expected on existing databases
		} finally {
			try {
				conn.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}
	
	private static void establishPlacesTable() {
		Connection db = getConnection();
		Statement st = null;
		try {
			st = db.createStatement();
			st.executeUpdate("CREATE TABLE IF NOT EXISTS places (id INTEGER PRIMARY KEY NOT NULL, place_name TEXT, lat TEXT NOT NULL, lon TEXT NOT NULL)");
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
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
		
	}

	private static void establishPreferencesTable() {
		Connection conn = getConnection();
		try {
			Statement st = conn.createStatement();
			st.executeUpdate("CREATE TABLE IF NOT EXISTS preferences (id INTEGER PRIMARY KEY NOT NULL, pref_name TEXT NOT NULL, pref_value TEXT)");
			st.close();
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			try {
				conn.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}

	private static void establishLocationsTable() {
		Connection conn = getConnection();
		try {
			Statement st = conn.createStatement();
			st.executeUpdate("CREATE TABLE IF NOT EXISTS locations (id INTEGER PRIMARY KEY NOT NULL, station_id INTEGER, location TEXT NOT NULL, frequency TEXT NOT NULL, time_on TEXT, time_off TEXT, lat TEXT, lng TEXT, lang TEXT)");
			st.close();
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			try {
				conn.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}

	private static void establishStationsTable() {
		Connection conn = getConnection();
		try {
			Statement st = conn.createStatement();
			st.executeUpdate("CREATE TABLE IF NOT EXISTS stations (id INTEGER PRIMARY KEY NOT NULL, meta_name VARCHAR(25) NOT NULL, title VARCHAR(255) NOT NULL)");
			st.close();
			
			/**
			 * Alter table so that duplicate stations
			 * are not inserted, unique column is 'meta_name'
			 */
			setMetaNameUnique();
			
			insertStations();
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			try {
				conn.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}

	private static void setMetaNameUnique() {
		Connection conn = getConnection();
		
		Statement st;
		try {
			st = conn.createStatement();
			st.executeUpdate("CREATE UNIQUE INDEX IF NOT EXISTS unique_meta_name ON stations(meta_name)");
			st.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	private static void insertStations() {
		Connection conn = getConnection();
		ArrayList<Station> stns = getStations();
		
		for(Station stn : stns) {
			try {
				PreparedStatement ps = conn.prepareStatement("INSERT OR IGNORE INTO stations (meta_name, title) VALUES (?, ?)");
				ps.setString(1, stn.getMetaName());
				ps.setString(2, stn.getTitle());
				
				ps.execute();
				ps.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}

	private static void establishLanguagesTable() {
		Connection conn = getConnection();
		try {
			Statement st = conn.createStatement();
			st.executeUpdate("CREATE TABLE IF NOT EXISTS languages (id INTEGER PRIMARY KEY NOT NULL, iso TEXT NOT NULL UNIQUE, label TEXT NOT NULL UNIQUE)");
			st.close();
			insertLanguages();
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			try {
				conn.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}

	private static void insertLanguages() {
		Connection conn = getConnection();
		
		ArrayList<Language> langs = getLanguages();
		
		for(Language lang : langs) {
			try {
				PreparedStatement ps = conn.prepareStatement("INSERT OR IGNORE INTO languages (iso, label) VALUES (?, ?)");
				ps.setString(1, lang.getIso());
				ps.setString(2, lang.getLabel());
				
				ps.execute();
				ps.close();
			} catch (SQLException e) {
				e.printStackTrace();
			} finally {
				
			}
		}
	}

	private static void establishLogsTable() {
		Connection conn = getConnection();
		try {
			Statement st = conn.createStatement();
			st.executeUpdate("CREATE TABLE IF NOT EXISTS logs (id INTEGER PRIMARY KEY NOT NULL, freq NUMERIC NOT NULL, mode TEXT NOT NULL, dateon INTEGER NOT NULL, description TEXT NOT NULL, location INTEGER, my_place INTEGER, created_at INTEGER)");
			st.close();
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			try {
				conn.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}

	public static String getDbPath() {
		return dbPath;
	}

	public static void setDbPath(String dbPath) {
		Data.dbPath = dbPath;
	}

	public static ArrayList<Language> getLanguages() {
		return languages;
	}

	public void setLanguages(ArrayList<Language> languages) {
		Data.languages = languages;
	}

	public static ArrayList<Station> getStations() {
		return stations;
	}

	public static void setStations(ArrayList<Station> stations) {
		Data.stations = stations;
	}

}
