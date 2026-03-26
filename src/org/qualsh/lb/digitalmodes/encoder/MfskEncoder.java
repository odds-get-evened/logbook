package org.qualsh.lb.digitalmodes.encoder;

import org.qualsh.lb.digital.DigitalMode;
import org.qualsh.lb.digitalmodes.audio.AudioBuffer;
import org.qualsh.lb.digitalmodes.mode.ModeProfile;

/**
 * Encoder for the MFSK16 (Multiple Frequency Shift Keying, 16 tones) digital mode.
 *
 * <p>MFSK16 is a robust HF keyboard mode well suited to noisy paths and polar
 * propagation. On the air it sounds like a series of rapidly changing musical
 * tones scattered across a 316 Hz swath of spectrum. The signal uses
 * {@value #MFSK_TONES} discrete tones spaced {@value #MFSK_TONE_SPACING_HZ} Hz
 * apart, centred at {@value #MFSK_CENTER_FREQUENCY_HZ} Hz in the audio passband.
 *
 * <p>Each symbol carries four bits of information, and symbols are transmitted
 * at {@value #MFSK_BAUD_RATE} baud using an IFK+ (Incremental Frequency Keying)
 * differential encoding scheme that makes the signal immune to constant frequency
 * offsets. MFSK16 also supports an optional Varicode-based forward error
 * correction layer (used in Fldigi's MFSK16 implementation) for additional
 * robustness.
 *
 * <p>Before transmitting you must set your operator callsign via
 * {@link #setOperatorCallsign(String)}. Open <em>Preferences &rarr; Station</em>
 * if it has not been configured yet.
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
     * Creates a new {@code MfskEncoder}, loading the MFSK16 mode profile with its
     * default bandwidth and signal parameters. The operator callsign is initially
     * empty; set it with {@link #setOperatorCallsign(String)} before encoding.
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
     * Returns {@code true} when an operator callsign has been configured and
     * the encoder is ready to produce a transmission.
     *
     * @return {@code true} if the callsign is set; {@code false} otherwise
     */
    @Override
    public boolean isReadyToEncode() {
        return operatorCallsign != null && !operatorCallsign.isEmpty();
    }

    /**
     * Encodes the supplied text into an MFSK16 audio stream.
     *
     * <p>There is no hard character limit for MFSK16, but messages longer than
     * 500 characters may result in very long transmissions. Audio is generated
     * using {@value #MFSK_TONES} tones at {@value #MFSK_BAUD_RATE} baud,
     * spaced {@value #MFSK_TONE_SPACING_HZ} Hz apart around a centre frequency
     * of {@value #MFSK_CENTER_FREQUENCY_HZ} Hz.
     *
     * <p>The returned buffer contains silence as a placeholder; actual MFSK
     * waveform synthesis using IFK+ differential encoding is deferred to a
     * future DSP implementation phase.
     *
     * @param text the message to encode; must not be {@code null} or blank
     * @param mode the digital mode (should be MFSK16)
     * @return an {@link AudioBuffer} containing the encoded audio stream
     * @throws EncoderException if the message is empty or the operator
     *         callsign has not been set
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
