package org.qualsh.lb.digitalmodes.encoder;

import org.qualsh.lb.digital.DigitalMode;
import org.qualsh.lb.digitalmodes.audio.AudioBuffer;

/**
 * The shared contract that all digital-mode encoders in the application follow.
 *
 * <p>Each encoder handles one specific digital mode — FT8, WSPR, BPSK31, and so on —
 * and knows how to turn your typed message into an audio signal ready for transmission.
 * On the Encode tab, type your message, choose a mode, and press Encode — the correct
 * encoder is selected automatically.
 *
 * <p>Before encoding, check {@link #isReadyToEncode()} to confirm your callsign and
 * any other required details are configured. Open Preferences → Station to set your
 * callsign application-wide.
 *
 * @author Logbook Development Team
 * @version 1.0
 */
public interface Encoder {

    /**
     * Converts your typed message into an audio signal ready for preview or transmission.
     *
     * <p>The returned audio buffer can be previewed through your speakers or sent to your
     * radio. An {@link EncoderException} is thrown if your message is empty, too long for
     * the selected mode, or if required station details are missing.
     *
     * @param text the message to encode; must not be {@code null} or blank
     * @param mode the digital mode to use for encoding
     * @return an audio buffer containing the encoded signal; never {@code null}
     * @throws EncoderException if the message is invalid or required configuration is missing
     */
    AudioBuffer encode(String text, DigitalMode mode) throws EncoderException;

    /**
     * Returns the digital mode that this encoder produces.
     *
     * @return the digital mode handled by this encoder; never {@code null}
     */
    DigitalMode getMode();

    /**
     * Returns {@code true} if all required information has been entered and the encoder
     * is ready to generate a signal.
     *
     * <p>At minimum your callsign must be set. WSPR also requires a grid square, and
     * Packet requires a destination callsign. Open Preferences → Station to fill in
     * any missing details.
     *
     * @return {@code true} if the encoder is ready; {@code false} if required details are missing
     */
    boolean isReadyToEncode();

    // -------------------------------------------------------------------------
    // Nested exception
    // -------------------------------------------------------------------------

    /**
     * Reported when the encoder cannot produce audio for a given message.
     *
     * <p>Common causes include an empty message, a message that is too long for the selected
     * mode, or a missing callsign or grid square. The error message explains what needs to be
     * fixed in plain English and is shown directly in the Encode panel status area.
     *
     * @author Logbook Development Team
     * @version 1.0
     */
    class EncoderException extends Exception {

        /**
         * Creates an encoder error with a plain-English explanation.
         *
         * @param message a description of why encoding failed
         */
        public EncoderException(String message) {
            super(message);
        }

        /**
         * Creates an encoder error with a plain-English explanation and the underlying cause.
         *
         * @param message a description of why encoding failed
         * @param cause   the original exception that triggered this failure
         */
        public EncoderException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
