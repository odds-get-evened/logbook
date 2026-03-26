package org.qualsh.lb.digital;

/**
 * Represents a digital transmission mode available for logging contacts,
 * such as FT8, PSK31, or RTTY. Each mode carries a display name, a short
 * abbreviation used in log entries, and technical characteristics that
 * describe how the signal occupies spectrum.
 */
public class DigitalMode {

	private int id;
	private String name;
	private String abbreviation;
	private int bandwidthHz;
	private String modulationType;
	private String encoding;
	private boolean builtIn;

	public static final String COLUMN_ID             = "id";
	public static final String COLUMN_NAME           = "name";
	public static final String COLUMN_ABBREVIATION   = "abbreviation";
	public static final String COLUMN_BANDWIDTH_HZ   = "bandwidth_hz";
	public static final String COLUMN_MODULATION_TYPE = "modulation_type";
	public static final String COLUMN_ENCODING       = "encoding";
	public static final String COLUMN_BUILT_IN       = "built_in";

	/** Creates an empty digital mode record. */
	public DigitalMode() {}

	/**
	 * Creates a digital mode with the given display name and abbreviation.
	 *
	 * @param name         the full display name, such as "Phase Shift Keying 31"
	 * @param abbreviation the short label used in log entries, such as "PSK31"
	 */
	public DigitalMode(String name, String abbreviation) {
		this.name = name;
		this.abbreviation = abbreviation;
	}

	/**
	 * Returns the unique identifier for this digital mode as stored in the database.
	 */
	public int getId() {
		return id;
	}

	/**
	 * Sets the unique database identifier for this digital mode.
	 */
	public void setId(int id) {
		this.id = id;
	}

	/**
	 * Returns the full display name of this mode, for example "Olivia 8/500"
	 * or "Frequency Shift Keying 31".
	 */
	public String getName() {
		return name;
	}

	/**
	 * Sets the full display name for this digital mode.
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Returns the short abbreviation used in your log entries, such as "FT8",
	 * "PSK31", or "RTTY".
	 */
	public String getAbbreviation() {
		return abbreviation;
	}

	/**
	 * Sets the short abbreviation for this digital mode.
	 */
	public void setAbbreviation(String abbreviation) {
		this.abbreviation = abbreviation;
	}

	/**
	 * Returns the typical occupied bandwidth of this mode measured in hertz.
	 * Use this to check whether a mode fits within a given sub-band.
	 */
	public int getBandwidthHz() {
		return bandwidthHz;
	}

	/**
	 * Sets the typical occupied bandwidth in hertz for this digital mode.
	 */
	public void setBandwidthHz(int bandwidthHz) {
		this.bandwidthHz = bandwidthHz;
	}

	/**
	 * Returns the modulation family used by this mode, such as "PSK", "FSK",
	 * or "MFSK". Useful when filtering modes by modulation type.
	 */
	public String getModulationType() {
		return modulationType;
	}

	/**
	 * Sets the modulation type identifier for this digital mode.
	 */
	public void setModulationType(String modulationType) {
		this.modulationType = modulationType;
	}

	/**
	 * Returns the character encoding or protocol name associated with this
	 * mode, such as "Varicode", "ASCII", or "ITA2".
	 */
	public String getEncoding() {
		return encoding;
	}

	/**
	 * Sets the character encoding or protocol name for this digital mode.
	 */
	public void setEncoding(String encoding) {
		this.encoding = encoding;
	}

	/**
	 * Returns {@code true} if this mode is one of the built-in modes that
	 * ship with the application. Built-in modes cannot be deleted, but their
	 * display names can be customised.
	 */
	public boolean isBuiltIn() {
		return builtIn;
	}

	/**
	 * Marks this digital mode as a built-in (protected) entry.
	 */
	public void setBuiltIn(boolean builtIn) {
		this.builtIn = builtIn;
	}

	/**
	 * Returns the abbreviation of this digital mode, which is the label
	 * shown throughout the application in tables, drop-downs, and reports.
	 */
	@Override
	public String toString() {
		return abbreviation;
	}
}
