package org.qualsh.lb.digital.encode;

import java.io.IOException;

import org.qualsh.lb.digital.DigitalMode;

/**
 * Defines the contract for a digital mode encoder. An encoder takes a
 * text message and the parameters from an {@link EncodeRequest} and
 * produces a raw PCM audio byte array ready to be sent to your
 * transmitter via a sound card interface.
 *
 * <p>Select the correct encoder for the mode you intend to transmit —
 * for example, use an FT8 encoder when operating on the FT8 sub-band.
 * Pass the returned audio bytes to your sound-card output to begin
 * transmitting.</p>
 */
public interface Encoder {

	/**
	 * Encodes the message contained in {@code request} into a raw PCM
	 * audio byte array at the sample rate and audio offset specified in
	 * the request. Send the returned bytes to your sound card to transmit
	 * the message.
	 *
	 * @param request the encoding parameters including the message text,
	 *                sample rate, and audio frequency offset
	 * @return a byte array of raw PCM audio samples representing the
	 *         encoded transmission
	 * @throws IOException if the audio data cannot be generated
	 */
	byte[] encode(EncodeRequest request) throws IOException;

	/**
	 * Returns the digital mode that this encoder produces transmissions for.
	 * Make sure this matches the mode you have selected on your radio before
	 * transmitting.
	 */
	DigitalMode getSupportedMode();

	/**
	 * Returns the audio sample rate in hertz that this encoder uses when
	 * generating audio. Configure your sound card output to the same rate
	 * to avoid pitch errors in the transmitted signal.
	 */
	int getSampleRateHz();
}
