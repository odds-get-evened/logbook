package org.qualsh.lb.digital.decode;

/**
 * Holds the information extracted from a single decoded digital mode
 * transmission. Each result contains the decoded text, the callsign of
 * the transmitting station (when the mode includes it), a signal-to-noise
 * ratio reading, and the audio frequency offset at which the signal was
 * found relative to your dial frequency.
 */
public class DecodeResult {

	private int id;
	private String callsign;
	private String message;
	private float snrDb;
	private float frequencyOffsetHz;
	private int timestamp;
	private int digitalModeId;
	private boolean valid;

	/** Creates an empty decode result. */
	public DecodeResult() {}

	/**
	 * Creates a decode result with the core fields populated.
	 *
	 * @param callsign           the callsign extracted from the transmission,
	 *                           or an empty string if not present
	 * @param message            the full decoded text of the transmission
	 * @param snrDb              the signal-to-noise ratio in decibels
	 * @param frequencyOffsetHz  the audio offset in hertz from the dial frequency
	 * @param timestamp          the time the message was decoded, as a Unix timestamp
	 * @param digitalModeId      the identifier of the mode used to decode this result
	 */
	public DecodeResult(String callsign, String message, float snrDb,
			float frequencyOffsetHz, int timestamp, int digitalModeId) {
		this.callsign = callsign;
		this.message = message;
		this.snrDb = snrDb;
		this.frequencyOffsetHz = frequencyOffsetHz;
		this.timestamp = timestamp;
		this.digitalModeId = digitalModeId;
		this.valid = true;
	}

	/**
	 * Returns the unique identifier for this decode result within its session.
	 */
	public int getId() {
		return id;
	}

	/**
	 * Sets the unique identifier for this decode result.
	 */
	public void setId(int id) {
		this.id = id;
	}

	/**
	 * Returns the callsign of the station that sent this transmission, as
	 * extracted by the decoder. Returns an empty string if the mode does not
	 * embed a callsign or if none was found.
	 */
	public String getCallsign() {
		return callsign;
	}

	/**
	 * Sets the callsign associated with this decoded transmission.
	 */
	public void setCallsign(String callsign) {
		this.callsign = callsign;
	}

	/**
	 * Returns the complete decoded text of the transmission, for example
	 * "CQ W1AW FN31" or "W1AW G3XYZ -10".
	 */
	public String getMessage() {
		return message;
	}

	/**
	 * Sets the decoded message text for this result.
	 */
	public void setMessage(String message) {
		this.message = message;
	}

	/**
	 * Returns the signal-to-noise ratio of this transmission in decibels.
	 * Higher values indicate a stronger, cleaner signal.
	 */
	public float getSnrDb() {
		return snrDb;
	}

	/**
	 * Sets the signal-to-noise ratio in decibels for this decode result.
	 */
	public void setSnrDb(float snrDb) {
		this.snrDb = snrDb;
	}

	/**
	 * Returns the audio frequency offset in hertz at which this signal was
	 * found. Add this value to your dial frequency to find the signal's
	 * actual transmit frequency.
	 */
	public float getFrequencyOffsetHz() {
		return frequencyOffsetHz;
	}

	/**
	 * Sets the audio frequency offset in hertz for this decode result.
	 */
	public void setFrequencyOffsetHz(float frequencyOffsetHz) {
		this.frequencyOffsetHz = frequencyOffsetHz;
	}

	/**
	 * Returns the time at which this message was decoded, expressed as a
	 * Unix timestamp (seconds since 1970-01-01 UTC).
	 */
	public int getTimestamp() {
		return timestamp;
	}

	/**
	 * Sets the decode timestamp for this result.
	 */
	public void setTimestamp(int timestamp) {
		this.timestamp = timestamp;
	}

	/**
	 * Returns the identifier of the digital mode that produced this result.
	 */
	public int getDigitalModeId() {
		return digitalModeId;
	}

	/**
	 * Sets the digital mode identifier for this decode result.
	 */
	public void setDigitalModeId(int digitalModeId) {
		this.digitalModeId = digitalModeId;
	}

	/**
	 * Returns {@code true} if the decoder considers this result to be a
	 * complete and error-free decode. Results with {@code false} may contain
	 * partial or uncertain text.
	 */
	public boolean isValid() {
		return valid;
	}

	/**
	 * Marks this decode result as valid or invalid.
	 */
	public void setValid(boolean valid) {
		this.valid = valid;
	}

	/**
	 * Returns a short summary of this result showing the callsign and
	 * decoded message, suitable for display in a results list.
	 */
	@Override
	public String toString() {
		return "[" + callsign + "] " + message;
	}
}
