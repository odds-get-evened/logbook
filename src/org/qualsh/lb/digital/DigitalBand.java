package org.qualsh.lb.digital;

/**
 * Describes an amateur radio band and the portions of that band allocated
 * for digital mode operation. Each band entry records its common name, the
 * overall frequency limits, and a list of dial frequencies most widely used
 * for digital contacts on that band.
 */
public class DigitalBand {

	private int id;
	private String name;
	private String metersBand;
	private float lowerFrequencyMhz;
	private float upperFrequencyMhz;
	private String commonDigitalFrequencies;

	public static final String COLUMN_ID                       = "id";
	public static final String COLUMN_NAME                     = "name";
	public static final String COLUMN_METERS_BAND              = "meters_band";
	public static final String COLUMN_LOWER_FREQUENCY_MHZ      = "lower_frequency_mhz";
	public static final String COLUMN_UPPER_FREQUENCY_MHZ      = "upper_frequency_mhz";
	public static final String COLUMN_COMMON_DIGITAL_FREQUENCIES = "common_digital_frequencies";

	/** Creates an empty band record. */
	public DigitalBand() {}

	/**
	 * Creates a band record with a display name, metre-band label, and
	 * its frequency limits.
	 *
	 * @param name              the human-readable band name, such as "20 Metres"
	 * @param metersBand        the short label, such as "20m"
	 * @param lowerFrequencyMhz the lower edge of the band in megahertz
	 * @param upperFrequencyMhz the upper edge of the band in megahertz
	 */
	public DigitalBand(String name, String metersBand,
			float lowerFrequencyMhz, float upperFrequencyMhz) {
		this.name = name;
		this.metersBand = metersBand;
		this.lowerFrequencyMhz = lowerFrequencyMhz;
		this.upperFrequencyMhz = upperFrequencyMhz;
	}

	/**
	 * Returns the unique database identifier for this band record.
	 */
	public int getId() {
		return id;
	}

	/**
	 * Sets the unique database identifier for this band record.
	 */
	public void setId(int id) {
		this.id = id;
	}

	/**
	 * Returns the full display name of this band, for example "20 Metres"
	 * or "40 Metres".
	 */
	public String getName() {
		return name;
	}

	/**
	 * Sets the display name for this band.
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Returns the short metre-band label used in log entries and drop-down
	 * menus, for example "20m" or "40m".
	 */
	public String getMetersBand() {
		return metersBand;
	}

	/**
	 * Sets the short metre-band label for this band.
	 */
	public void setMetersBand(String metersBand) {
		this.metersBand = metersBand;
	}

	/**
	 * Returns the lower frequency edge of this band in megahertz.
	 */
	public float getLowerFrequencyMhz() {
		return lowerFrequencyMhz;
	}

	/**
	 * Sets the lower frequency edge of this band in megahertz.
	 */
	public void setLowerFrequencyMhz(float lowerFrequencyMhz) {
		this.lowerFrequencyMhz = lowerFrequencyMhz;
	}

	/**
	 * Returns the upper frequency edge of this band in megahertz.
	 */
	public float getUpperFrequencyMhz() {
		return upperFrequencyMhz;
	}

	/**
	 * Sets the upper frequency edge of this band in megahertz.
	 */
	public void setUpperFrequencyMhz(float upperFrequencyMhz) {
		this.upperFrequencyMhz = upperFrequencyMhz;
	}

	/**
	 * Returns a comma-separated list of the most commonly used dial
	 * frequencies for digital modes on this band, for example
	 * "14.074, 14.076, 14.090". These are offered as quick-select
	 * options when logging a new contact.
	 */
	public String getCommonDigitalFrequencies() {
		return commonDigitalFrequencies;
	}

	/**
	 * Sets the comma-separated list of common digital dial frequencies
	 * for this band.
	 */
	public void setCommonDigitalFrequencies(String commonDigitalFrequencies) {
		this.commonDigitalFrequencies = commonDigitalFrequencies;
	}

	/**
	 * Returns the metre-band label for this band, which is the value shown
	 * in band selection controls throughout the application.
	 */
	@Override
	public String toString() {
		return metersBand;
	}
}
