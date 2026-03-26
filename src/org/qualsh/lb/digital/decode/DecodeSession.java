package org.qualsh.lb.digital.decode;

import java.util.ArrayList;

/**
 * Tracks a single continuous decoding session — the period between pressing
 * "Start Decoding" and "Stop Decoding" in the Digital Modes panel. A session
 * records which audio device and dial frequency were in use, when the session
 * ran, and every {@link DecodeResult} received during that time.
 *
 * <p>Sessions are created automatically by a {@link Decoder} when decoding
 * starts and are available for review in the session history even after
 * decoding has stopped.</p>
 */
public class DecodeSession {

	private String sessionId;
	private int digitalModeId;
	private int startTime;
	private int endTime;
	private String audioDeviceName;
	private float dialFrequencyMhz;
	private ArrayList<DecodeResult> results;

	/** Creates an empty decode session. */
	public DecodeSession() {
		this.results = new ArrayList<DecodeResult>();
	}

	/**
	 * Creates a decode session with the core configuration fields set.
	 *
	 * @param sessionId        a unique identifier for this session, used to
	 *                         distinguish it from other sessions in the history
	 * @param digitalModeId    the identifier of the digital mode being decoded
	 * @param audioDeviceName  the name of the audio input device in use
	 * @param dialFrequencyMhz the dial frequency of the radio in megahertz
	 * @param startTime        the time decoding began, as a Unix timestamp
	 */
	public DecodeSession(String sessionId, int digitalModeId,
			String audioDeviceName, float dialFrequencyMhz, int startTime) {
		this.sessionId = sessionId;
		this.digitalModeId = digitalModeId;
		this.audioDeviceName = audioDeviceName;
		this.dialFrequencyMhz = dialFrequencyMhz;
		this.startTime = startTime;
		this.results = new ArrayList<DecodeResult>();
	}

	/**
	 * Adds a newly decoded message to this session's result list. This is
	 * called automatically as the decoder processes incoming audio.
	 *
	 * @param result the decode result to append to this session
	 */
	public void addResult(DecodeResult result) {
		results.add(result);
	}

	/**
	 * Returns the number of messages successfully decoded during this session.
	 */
	public int getResultCount() {
		return results.size();
	}

	/**
	 * Returns the unique identifier that distinguishes this session from
	 * others in the decode history.
	 */
	public String getSessionId() {
		return sessionId;
	}

	/**
	 * Sets the unique session identifier.
	 */
	public void setSessionId(String sessionId) {
		this.sessionId = sessionId;
	}

	/**
	 * Returns the identifier of the digital mode that was active during
	 * this session.
	 */
	public int getDigitalModeId() {
		return digitalModeId;
	}

	/**
	 * Sets the digital mode identifier for this session.
	 */
	public void setDigitalModeId(int digitalModeId) {
		this.digitalModeId = digitalModeId;
	}

	/**
	 * Returns the time at which this decoding session started, as a Unix
	 * timestamp (seconds since 1970-01-01 UTC).
	 */
	public int getStartTime() {
		return startTime;
	}

	/**
	 * Sets the start time for this session.
	 */
	public void setStartTime(int startTime) {
		this.startTime = startTime;
	}

	/**
	 * Returns the time at which this decoding session ended, as a Unix
	 * timestamp, or {@code 0} if the session is still active.
	 */
	public int getEndTime() {
		return endTime;
	}

	/**
	 * Records when this decoding session ended.
	 */
	public void setEndTime(int endTime) {
		this.endTime = endTime;
	}

	/**
	 * Returns the name of the audio input device used during this session,
	 * as it appears in your system's sound settings.
	 */
	public String getAudioDeviceName() {
		return audioDeviceName;
	}

	/**
	 * Sets the audio input device name for this session.
	 */
	public void setAudioDeviceName(String audioDeviceName) {
		this.audioDeviceName = audioDeviceName;
	}

	/**
	 * Returns the radio dial frequency in megahertz that was set when this
	 * session started.
	 */
	public float getDialFrequencyMhz() {
		return dialFrequencyMhz;
	}

	/**
	 * Sets the radio dial frequency in megahertz for this session.
	 */
	public void setDialFrequencyMhz(float dialFrequencyMhz) {
		this.dialFrequencyMhz = dialFrequencyMhz;
	}

	/**
	 * Returns all decode results collected during this session. The list is
	 * in the order the messages were received.
	 */
	public ArrayList<DecodeResult> getResults() {
		return results;
	}

	/**
	 * Replaces the entire result list for this session.
	 */
	public void setResults(ArrayList<DecodeResult> results) {
		this.results = results;
	}

	/**
	 * Returns a summary of this session showing its identifier and how many
	 * messages were decoded.
	 */
	@Override
	public String toString() {
		return "Session " + sessionId + " (" + results.size() + " decoded)";
	}
}
