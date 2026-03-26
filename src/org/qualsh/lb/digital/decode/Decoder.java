package org.qualsh.lb.digital.decode;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.qualsh.lb.digital.DigitalMode;

/**
 * Defines the contract for a digital mode decoder. A decoder listens to
 * an incoming audio stream and produces {@link DecodeResult} objects each
 * time it successfully identifies a message or contact in the received signal.
 *
 * <p>To use a decoder, call {@link #startDecoding(InputStream)} with an
 * audio stream from your sound card, then retrieve results through the
 * active {@link DecodeSession}. Call {@link #stopDecoding()} when you are
 * finished to release the audio device.</p>
 */
public interface Decoder {

	/**
	 * Begins listening for digital mode signals on the provided audio stream
	 * and returns a session object that you can use to retrieve decoded
	 * messages. The stream typically comes from a sound card connected to
	 * your receiver's audio output.
	 *
	 * @param audioStream the raw PCM audio stream to decode
	 * @return a {@link DecodeSession} representing this active decoding session
	 * @throws IOException if the audio stream cannot be opened or read
	 */
	DecodeSession startDecoding(InputStream audioStream) throws IOException;

	/**
	 * Stops the active decoding session and releases the audio stream.
	 * Any messages that were being decoded when this method is called are
	 * discarded.
	 */
	void stopDecoding();

	/**
	 * Returns {@code true} if this decoder is currently active and
	 * processing incoming audio.
	 */
	boolean isDecoding();

	/**
	 * Returns the digital mode that this decoder is designed to receive.
	 * For example, an FT8 decoder returns the FT8 {@link DigitalMode} entry.
	 */
	DigitalMode getSupportedMode();

	/**
	 * Returns all messages that have been decoded since the current session
	 * started. The list grows as new transmissions are received; call this
	 * method repeatedly to check for new arrivals.
	 */
	List<DecodeResult> getResults();
}
