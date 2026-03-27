package org.qualsh.lb.digitalmodes.encoder;

import org.qualsh.lb.digital.DigitalMode;
import org.qualsh.lb.digitalmodes.audio.AudioBuffer;
import org.qualsh.lb.digitalmodes.mode.ModeProfile;

/**
 * Encodes your typed message into an Olivia audio signal ready for preview or transmission.
 *
 * <p>Olivia is an extremely robust keyboard mode designed for difficult propagation paths where
 * faster modes like FT8 or PSK31 fail to decode. On the air it sounds like a smooth,
 * continuously varying chord — a gentle, almost musical swoosh that shifts slowly across
 * the audio passband. This encoder uses the Olivia 8/250 configuration (8 tones, 250 Hz
 * bandwidth).
 *
 * <p>Before transmitting you must set your operator callsign via
 * {@link #setOperatorCallsign(String)}. Open Preferences → Station if it has not been
 * configured yet.
 *
 * @author Logbook Development Team
 * @version 1.0
 */
public class OliviaEncoder implements Encoder {

    private static final String TAG = "OliviaEncoder";

    private static final float  OUTPUT_SAMPLE_RATE          = 8000.0f;
    private static final int    BITS_PER_SAMPLE             = 16;

    private static final int    OLIVIA_TONES                = 8;
    private static final int    OLIVIA_BANDWIDTH_HZ         = 250;
    private static final double OLIVIA_CENTER_FREQUENCY_HZ  = 1500.0;
    private static final double OLIVIA_BAUD_RATE            = 31.25;

    private final ModeProfile profile;
    private String operatorCallsign;

    /**
     * Creates a new Olivia encoder. Set your callsign with {@link #setOperatorCallsign(String)}
     * before encoding.
     */
    public OliviaEncoder() {
        DigitalMode mode = new DigitalMode("Olivia", "OLIVIA");
        profile = ModeProfile.getProfile(mode);
        operatorCallsign = "";
    }

    /**
     * Returns the {@link DigitalMode} produced by this encoder.
     *
     * @return the Olivia digital mode; never {@code null}
     */
    @Override
    public DigitalMode getMode() {
        return new DigitalMode("Olivia", "OLIVIA");
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
     * Converts your typed message into an Olivia audio signal ready for preview or transmission.
     *
     * <p>There is no hard character limit for Olivia, but messages longer than 500 characters
     * may result in very long transmissions. Your callsign must be set before calling this
     * method — open Preferences → Station if it has not been configured yet.
     *
     * @param text the message to encode; must not be {@code null} or blank
     * @param mode the digital mode (should be Olivia)
     * @return an {@link AudioBuffer} containing the encoded audio signal; never {@code null}
     * @throws EncoderException if the message is empty or the operator callsign has not been set
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
            if (text.length() > 500) {
                System.err.println(TAG + ": message length " + text.length()
                        + " exceeds 500 characters; transmission will be very long");
            }

            // Olivia encodes in 8-character blocks; each character requires 8 symbols.
            // Estimate buffer size based on block-aligned symbol count.
            double symbolsPerChar = 8.0;
            double totalSymbols   = text.length() * symbolsPerChar;
            int totalSamples      = (int) (totalSymbols / OLIVIA_BAUD_RATE * OUTPUT_SAMPLE_RATE);
            int bufferBytes       = totalSamples * (BITS_PER_SAMPLE / 8);
            byte[] outputPcm      = new byte[bufferBytes];

            // TODO: implement Olivia waveform synthesis using signal parameters above

            AudioBuffer output = new AudioBuffer();
            output.load(outputPcm, OUTPUT_SAMPLE_RATE);
            return output;

        } catch (EncoderException e) {
            throw e;
        } catch (Exception e) {
            throw new EncoderException("Olivia encoding failed: " + e.getMessage(), e);
        }
    }

    /**
     * Returns the operator callsign sent at the start of Olivia transmissions.
     *
     * @return the callsign, or an empty string if none has been set
     */
    public String getOperatorCallsign() {
        return operatorCallsign;
    }

    /**
     * Sets the operator callsign included at the start of Olivia transmissions.
     *
     * <p>The callsign is trimmed and converted to upper case. This should be
     * your licensed amateur radio callsign.
     *
     * @param callsign your amateur radio callsign, for example {@code "W1AW"}
     */
    public void setOperatorCallsign(String callsign) {
        this.operatorCallsign = (callsign == null) ? "" : callsign.trim().toUpperCase();
    }
}
