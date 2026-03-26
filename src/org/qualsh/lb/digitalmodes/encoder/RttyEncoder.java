package org.qualsh.lb.digitalmodes.encoder;

import org.qualsh.lb.digital.DigitalMode;
import org.qualsh.lb.digitalmodes.audio.AudioBuffer;
import org.qualsh.lb.digitalmodes.mode.ModeProfile;

/**
 * Encoder for the RTTY (Radio Teletype) digital mode.
 *
 * <p>RTTY is one of the oldest digital modes still used on the amateur HF bands.
 * On the air it sounds like a distinctive two-tone clatter — a mark tone at
 * {@value #RTTY_MARK_HZ} Hz and a space tone at {@value #RTTY_SPACE_HZ} Hz,
 * separated by a {@value #RTTY_SHIFT_HZ} Hz shift. The rapid switching between
 * these tones gives RTTY its characteristic rattling sound.
 *
 * <p>RTTY uses Baudot/ITA2 encoding, which supports upper-case letters, digits,
 * and a limited set of punctuation characters. Each character is transmitted as
 * a 5-bit code at {@value #RTTY_BAUD_RATE} baud, giving a typing speed of
 * roughly 60 words per minute. RTTY is widely used in HF contests and by coast
 * guard and maritime stations.
 *
 * <p>Before transmitting you must set your operator callsign via
 * {@link #setOperatorCallsign(String)}. Open <em>Preferences &rarr; Station</em>
 * if it has not been configured yet.
 */
public class RttyEncoder implements Encoder {

    private static final String TAG = "RttyEncoder";

    private static final float  OUTPUT_SAMPLE_RATE  = 8000.0f;
    private static final int    BITS_PER_SAMPLE     = 16;

    private static final double RTTY_BAUD_RATE      = 45.45;
    private static final double RTTY_MARK_HZ        = 1615.0;
    private static final double RTTY_SPACE_HZ       = 1785.0;
    private static final double RTTY_SHIFT_HZ       = 170.0;

    private final ModeProfile profile;
    private String operatorCallsign;

    /**
     * Creates a new {@code RttyEncoder}, loading the RTTY mode profile with its
     * default bandwidth and signal parameters. The operator callsign is initially
     * empty; set it with {@link #setOperatorCallsign(String)} before encoding.
     */
    public RttyEncoder() {
        DigitalMode mode = new DigitalMode("Radio Teletype", "RTTY");
        profile = ModeProfile.getProfile(mode);
        operatorCallsign = "";
    }

    /**
     * Returns the {@link DigitalMode} produced by this encoder.
     *
     * @return the RTTY digital mode; never {@code null}
     */
    @Override
    public DigitalMode getMode() {
        return new DigitalMode("Radio Teletype", "RTTY");
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
     * Encodes the supplied text into an RTTY audio stream.
     *
     * <p>There is no hard character limit for RTTY, but messages longer than
     * 500 characters may result in very long transmissions. The audio alternates
     * between a mark tone at {@value #RTTY_MARK_HZ} Hz and a space tone at
     * {@value #RTTY_SPACE_HZ} Hz ({@value #RTTY_SHIFT_HZ} Hz shift) at
     * {@value #RTTY_BAUD_RATE} baud using Baudot/ITA2 encoding.
     *
     * <p>The returned buffer contains silence as a placeholder; actual FSK
     * waveform synthesis using Baudot encoding is deferred to a future DSP
     * implementation phase.
     *
     * @param text the message to encode; must not be {@code null} or blank
     * @param mode the digital mode (should be RTTY)
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

            // Baudot uses 7.5 bits per character (5 data bits + start + 1.5 stop bits).
            double bitsPerChar  = 7.5;
            double totalBits    = text.length() * bitsPerChar;
            int totalSamples    = (int) (totalBits / RTTY_BAUD_RATE * OUTPUT_SAMPLE_RATE);
            int bufferBytes     = totalSamples * (BITS_PER_SAMPLE / 8);
            byte[] outputPcm    = new byte[bufferBytes];

            // TODO: implement RTTY waveform synthesis using signal parameters above

            AudioBuffer output = new AudioBuffer();
            output.load(outputPcm, OUTPUT_SAMPLE_RATE);
            return output;

        } catch (EncoderException e) {
            throw e;
        } catch (Exception e) {
            throw new EncoderException("RTTY encoding failed: " + e.getMessage(), e);
        }
    }

    /**
     * Returns the operator callsign sent at the start of RTTY transmissions.
     *
     * @return the callsign, or an empty string if none has been set
     */
    public String getOperatorCallsign() {
        return operatorCallsign;
    }

    /**
     * Sets the operator callsign included at the start of RTTY transmissions.
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
