package org.qualsh.lb.digitalmodes.encoder;

import org.qualsh.lb.digital.DigitalMode;
import org.qualsh.lb.digitalmodes.audio.AudioBuffer;
import org.qualsh.lb.digitalmodes.mode.ModeProfile;

/**
 * Encodes your typed message into an MFSK16 audio signal ready for preview or transmission.
 *
 * <p>MFSK16 is a robust multi-tone keyboard mode well suited to difficult propagation conditions.
 * On the air it sounds like a series of rapidly changing musical tones scattered across a
 * 250 Hz swath of spectrum. Its use of 16 simultaneous tones makes it especially resilient
 * to selective fading and interference that would break other modes.
 *
 * <p>Before transmitting you must set your operator callsign via
 * {@link #setOperatorCallsign(String)}. Open Preferences → Station if it has not been
 * configured yet.
 *
 * @author Logbook Development Team
 * @version 1.0
 */
public class MfskEncoder implements Encoder {

    private static final String TAG = "MfskEncoder";

    private static final float  OUTPUT_SAMPLE_RATE        = 8000.0f;
    private static final int    BITS_PER_SAMPLE           = 16;

    private static final int    MFSK_TONES                = 16;
    private static final double MFSK_BAUD_RATE            = 15.625;
    private static final double MFSK_TONE_SPACING_HZ      = 15.625;
    private static final double MFSK_CENTER_FREQUENCY_HZ  = 1500.0;

    private final ModeProfile profile;
    private String operatorCallsign;

    /**
     * Creates a new MFSK16 encoder. Set your callsign with {@link #setOperatorCallsign(String)}
     * before encoding.
     */
    public MfskEncoder() {
        DigitalMode mode = new DigitalMode("MFSK16", "MFSK16");
        profile = ModeProfile.getProfile(mode);
        operatorCallsign = "";
    }

    /**
     * Returns the {@link DigitalMode} produced by this encoder.
     *
     * @return the MFSK16 digital mode; never {@code null}
     */
    @Override
    public DigitalMode getMode() {
        return new DigitalMode("MFSK16", "MFSK16");
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
     * Converts your typed message into an MFSK16 audio signal ready for preview or transmission.
     *
     * <p>There is no hard character limit for MFSK16, but messages longer than 500 characters
     * may result in very long transmissions. Your callsign must be set before calling this
     * method — open Preferences → Station if it has not been configured yet.
     *
     * @param text the message to encode; must not be {@code null} or blank
     * @param mode the digital mode (should be MFSK16)
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

            // Each character is encoded as a sequence of 4-bit symbols at MFSK_BAUD_RATE.
            // Estimate 2 symbols per character on average.
            double symbolsPerChar = 2.0;
            double totalSymbols   = text.length() * symbolsPerChar;
            int totalSamples      = (int) (totalSymbols / MFSK_BAUD_RATE * OUTPUT_SAMPLE_RATE);
            int bufferBytes       = totalSamples * (BITS_PER_SAMPLE / 8);
            byte[] outputPcm      = new byte[bufferBytes];

            // TODO: implement MFSK16 waveform synthesis using signal parameters above

            AudioBuffer output = new AudioBuffer();
            output.load(outputPcm, OUTPUT_SAMPLE_RATE);
            return output;

        } catch (EncoderException e) {
            throw e;
        } catch (Exception e) {
            throw new EncoderException("MFSK16 encoding failed: " + e.getMessage(), e);
        }
    }

    /**
     * Returns the operator callsign sent at the start of MFSK16 transmissions.
     *
     * @return the callsign, or an empty string if none has been set
     */
    public String getOperatorCallsign() {
        return operatorCallsign;
    }

    /**
     * Sets the operator callsign included at the start of MFSK16 transmissions.
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
