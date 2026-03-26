package org.qualsh.lb.digital.encode;

/**
 * Carries all the parameters needed to encode a single digital mode
 * transmission. Fill in the message text, choose the audio settings that
 * match your sound card, then pass this object to an {@link Encoder} to
 * generate the audio that will be transmitted.
 */
public class EncodeRequest {

	private String message;
	private int digitalModeId;
	private int sampleRateHz;
	private float dialFrequencyMhz;
	private float audioOffsetHz;
	private int txPowerPercent;

	/** Creates an empty encode request. */
	public EncodeRequest() {}

	/**
	 * Creates an encode request with the essential parameters required
	 * to produce a transmission.
	 *
	 * @param message          the text to transmit, such as "CQ W1AW FN31"
	 * @param digitalModeId    the identifier of the digital mode to use
	 * @param sampleRateHz     the audio sample rate of your sound card output,
	 *                         typically 44100 or 48000
	 * @param dialFrequencyMhz the radio dial frequency in megahertz
	 * @param audioOffsetHz    the audio tone offset in hertz within the
	 *                         receiver passband, typically 1000–2500 Hz
	 */
	public EncodeRequest(String message, int digitalModeId, int sampleRateHz,
			float dialFrequencyMhz, float audioOffsetHz) {
		this.message = message;
		this.digitalModeId = digitalModeId;
		this.sampleRateHz = sampleRateHz;
		this.dialFrequencyMhz = dialFrequencyMhz;
		this.audioOffsetHz = audioOffsetHz;
		this.txPowerPercent = 100;
	}

	/**
	 * Returns the text message that will be encoded and transmitted.
	 */
	public String getMessage() {
		return message;
	}

	/**
	 * Sets the text message to transmit. Keep the message within the
	 * character limit of the chosen digital mode.
	 */
	public void setMessage(String message) {
		this.message = message;
	}

	/**
	 * Returns the identifier of the digital mode to use for encoding.
	 */
	public int getDigitalModeId() {
		return digitalModeId;
	}

	/**
	 * Sets the digital mode identifier for this encoding request.
	 */
	public void setDigitalModeId(int digitalModeId) {
		this.digitalModeId = digitalModeId;
	}

	/**
	 * Returns the audio sample rate in hertz that the encoder will use.
	 * This must match the sample rate configured in your operating system's
	 * sound card output settings.
	 */
	public int getSampleRateHz() {
		return sampleRateHz;
	}

	/**
	 * Sets the audio sample rate in hertz for the encoder output.
	 */
	public void setSampleRateHz(int sampleRateHz) {
		this.sampleRateHz = sampleRateHz;
	}

	/**
	 * Returns the radio dial frequency in megahertz that is set on your
	 * transceiver while this transmission takes place.
	 */
	public float getDialFrequencyMhz() {
		return dialFrequencyMhz;
	}

	/**
	 * Sets the radio dial frequency in megahertz for this transmission.
	 */
	public void setDialFrequencyMhz(float dialFrequencyMhz) {
		this.dialFrequencyMhz = dialFrequencyMhz;
	}

	/**
	 * Returns the audio tone offset in hertz. This is the position within
	 * the receiver passband where the signal will appear on the waterfall,
	 * typically between 500 Hz and 3000 Hz.
	 */
	public float getAudioOffsetHz() {
		return audioOffsetHz;
	}

	/**
	 * Sets the audio tone offset in hertz for this transmission.
	 */
	public void setAudioOffsetHz(float audioOffsetHz) {
		this.audioOffsetHz = audioOffsetHz;
	}

	/**
	 * Returns the transmit power level as a percentage of the encoder's
	 * maximum audio output level. Reduce this value if your transmitter
	 * shows signs of over-drive or splatter.
	 */
	public int getTxPowerPercent() {
		return txPowerPercent;
	}

	/**
	 * Sets the transmit power percentage (0–100) for this encoding request.
	 */
	public void setTxPowerPercent(int txPowerPercent) {
		this.txPowerPercent = txPowerPercent;
	}

	/**
	 * Returns a summary of this encode request showing the mode identifier
	 * and the message to be transmitted.
	 */
	@Override
	public String toString() {
		return "EncodeRequest[mode=" + digitalModeId + ", msg=" + message + "]";
	}
}
