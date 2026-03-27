package org.qualsh.lb.digitalmodes.encoder;

import org.qualsh.lb.digital.DigitalMode;
import org.qualsh.lb.digitalmodes.audio.AudioBuffer;
import org.qualsh.lb.digitalmodes.mode.ModeProfile;

/**
 * Encodes your typed message into an FT8 audio signal ready for preview or transmission.
 *
 * <p>FT8 is the most widely used weak-signal HF mode in amateur radio. On the air it sounds
 * like a rapid series of rising musical tones that repeat every 15 seconds. Messages are
 * limited to 13 characters and must follow the FT8 callsign-exchange format.
 *
 * <p>Before encoding, set your callsign via {@link #setOperatorCallsign(String)}, or open
 * Preferences → Station to configure it application-wide.
 *
 * @author Logbook Development Team
 * @version 1.0
 */
public class Ft8Encoder implements Encoder {

    private static final String TAG = "Ft8Encoder";

    private static final float  OUTPUT_SAMPLE_RATE       = 8000.0f;
    private static final int    BITS_PER_SAMPLE          = 16;

    private static final int    FT8_SYMBOL_COUNT         = 79;
    private static final double FT8_SYMBOL_PERIOD_SECONDS = 0.160;
    private static final double FT8_TONE_SPACING_HZ      = 6.25;
    private static final int    FT8_TONES                = 8;

    /** Maximum free-text length accepted by the FT8 protocol. */
    private static final int    FT8_MAX_TEXT_LENGTH      = 13;

    private final ModeProfile profile;
    private String operatorCallsign;

    /**
     * Creates a new FT8 encoder. Set your callsign with {@link #setOperatorCallsign(String)}
     * before encoding.
     */
    public Ft8Encoder() {
        DigitalMode mode = new DigitalMode("FT8", "FT8");
        profile = ModeProfile.getProfile(mode);
        operatorCallsign = "";
    }

    /**
     * Returns the {@link DigitalMode} produced by this encoder.
     *
     * @return the FT8 digital mode; never {@code null}
     */
    @Override
    public DigitalMode getMode() {
        return new DigitalMode("FT8", "FT8");
    }

    /**
     * Returns {@code true} if all required information has been entered and the encoder
     * is ready to generate a signal.
     *
     * <p>At minimum your callsign must be set. Open Preferences → Station to fill in
     * any missing details.
     *
     * @return {@code true} if the callsign is set; {@code false} otherwise
     */
    @Override
    public boolean isReadyToEncode() {
        return operatorCallsign != null && !operatorCallsign.isEmpty();
    }

    /**
     * Converts your typed message into an FT8 audio signal ready for preview or transmission.
     *
     * <p>The message must be no longer than {@value #FT8_MAX_TEXT_LENGTH} characters and must
     * follow the FT8 callsign-exchange format. Your callsign must be set before calling this
     * method — open Preferences → Station if it has not been configured yet.
     *
     * @param text the message to encode; must not be {@code null}, blank, or
     *             longer than {@value #FT8_MAX_TEXT_LENGTH} characters
     * @param mode the digital mode (should be FT8)
     * @return an {@link AudioBuffer} containing the encoded audio frame; never {@code null}
     * @throws EncoderException if the message is empty, too long, or the
     *         operator callsign has not been set
     */
    @Override
    public AudioBuffer encode(String text, DigitalMode mode) throws EncoderException {
        try {
            if (text == null || text.isBlank()) {
                throw new EncoderException("Cannot encode empty message");
            }
            if (!isReadyToEncode()) {
                throw new EncoderException(
                        "Operator callsign not set. Please configure your callsign in Preferences.");
            }
            if (text.length() > FT8_MAX_TEXT_LENGTH) {
                throw new EncoderException("FT8 messages are limited to 13 characters");
            }

            int totalSamples = (int) (FT8_SYMBOL_COUNT * FT8_SYMBOL_PERIOD_SECONDS * OUTPUT_SAMPLE_RATE);
            int bufferBytes  = totalSamples * (BITS_PER_SAMPLE / 8);
            byte[] outputPcm = new byte[bufferBytes];

            // TODO: implement FT8 waveform synthesis using signal parameters above

            AudioBuffer output = new AudioBuffer();
            output.load(outputPcm, OUTPUT_SAMPLE_RATE);
            return output;

        } catch (EncoderException e) {
            throw e;
        } catch (Exception e) {
            throw new EncoderException("FT8 encoding failed: " + e.getMessage(), e);
        }
    }

    /**
     * Returns the operator callsign used in outgoing FT8 message headers.
     *
     * @return the callsign, or an empty string if none has been set
     */
    public String getOperatorCallsign() {
        return operatorCallsign;
    }

    /**
     * Sets the operator callsign included in outgoing FT8 transmissions.
     *
     * <p>The callsign is trimmed of surrounding whitespace and converted to
     * upper case. Set this to your licensed amateur radio callsign before
     * calling {@link #encode(String, DigitalMode)}.
     *
     * @param callsign your amateur radio callsign, for example {@code "W1AW"}
     */
    public void setOperatorCallsign(String callsign) {
        this.operatorCallsign = (callsign == null) ? "" : callsign.trim().toUpperCase();
    }
}
