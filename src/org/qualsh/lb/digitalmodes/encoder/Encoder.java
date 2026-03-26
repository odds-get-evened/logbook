package org.qualsh.lb.digitalmodes.encoder;

import org.qualsh.lb.digital.DigitalMode;
import org.qualsh.lb.digitalmodes.audio.AudioBuffer;

/**
 * Contract for digital mode encoders that convert typed text into audio signals
 * ready for radio transmission.
 *
 * <p>Each encoder handles one specific digital mode (FT8, WSPR, PSK31, and so on)
 * and knows how to turn a plain-text message into a block of 16-bit mono PCM audio
 * that can be sent directly to the radio's microphone input. The Transmit tab in the
 * application lets you compose a message, pick a digital mode, and press <em>Transmit</em>
 * — the appropriate encoder is selected automatically and produces the audio that goes
 * on air.
 *
 * <p>Before calling {@link #encode(String, DigitalMode)}, check
 * {@link #isReadyToEncode()} to confirm that any required operator details
 * (callsign, Maidenhead grid square, etc.) have been configured. If the encoder is
 * not ready, the encode call will throw an {@link EncoderException} explaining what
 * information is missing.
 *
 * <p>Usage example:
 * <pre>
 *     Encoder encoder = new Ft8Encoder();
 *     encoder.setOperatorCallsign("W1AW");
 *     if (encoder.isReadyToEncode()) {
 *         AudioBuffer audio = encoder.encode("CQ DX W1AW FN31", encoder.getMode());
 *         // send audio to the radio
 *     }
 * </pre>
 */
public interface Encoder {

    /**
     * Encodes the supplied text message into a block of audio suitable for
     * radio transmission in the given digital mode.
     *
     * <p>The returned {@link AudioBuffer} contains 16-bit signed mono PCM audio
     * at 8 000 Hz. Pass this buffer to the transmit pipeline to put the signal
     * on air. The method verifies that the encoder is ready and that the message
     * text meets the constraints of the chosen mode (length limits, character
     * set, etc.) before generating audio.
     *
     * @param text the message to transmit; must not be {@code null} or blank
     * @param mode the digital mode to use for encoding
     * @return an {@link AudioBuffer} containing the encoded audio; never
     *         {@code null}
     * @throws EncoderException if the message is empty or invalid, if required
     *         operator details have not been configured, or if a signal
     *         generation error occurs
     */
    AudioBuffer encode(String text, DigitalMode mode) throws EncoderException;

    /**
     * Returns the {@link DigitalMode} that this encoder produces.
     *
     * <p>Use this to label the outgoing audio or to record the mode in your
     * log entry after a successful transmission.
     *
     * @return the digital mode handled by this encoder; never {@code null}
     */
    DigitalMode getMode();

    /**
     * Returns {@code true} when the encoder has all the information it needs
     * to produce a valid transmission.
     *
     * <p>At minimum this means your operator callsign has been set. Some modes
     * require additional data — for example WSPR also needs a Maidenhead grid
     * square, and Packet requires a destination callsign. Open
     * <em>Preferences &rarr; Station</em> to fill in any missing details.
     *
     * @return {@code true} if the encoder is ready; {@code false} if required
     *         configuration is missing
     */
    boolean isReadyToEncode();

    // -------------------------------------------------------------------------
    // Nested exception
    // -------------------------------------------------------------------------

    /**
     * Thrown when an {@link Encoder} cannot produce audio for a given message.
     *
     * <p>Common causes include an empty message, a message that exceeds the
     * maximum length allowed by the mode, or missing operator configuration
     * such as a callsign or grid square. Read the exception message for a
     * plain-English explanation that can be shown directly in the user
     * interface.
     */
    class EncoderException extends Exception {

        /**
         * Creates an {@code EncoderException} with the supplied description.
         *
         * @param message a human-readable explanation of why encoding failed
         */
        public EncoderException(String message) {
            super(message);
        }

        /**
         * Creates an {@code EncoderException} wrapping an underlying cause.
         *
         * @param message a human-readable explanation of why encoding failed
         * @param cause   the original exception that triggered this failure
         */
        public EncoderException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
