package org.qualsh.lb.digitalmodes.encoder;

import org.qualsh.lb.digital.DigitalMode;
import org.qualsh.lb.digitalmodes.audio.AudioBuffer;
import org.qualsh.lb.digitalmodes.mode.ModeProfile;

/**
 * Encodes your station details into a WSPR beacon signal ready for preview or transmission.
 *
 * <p>WSPR (pronounced "whisper") is a propagation-testing beacon mode. On the air it sounds
 * like a faint, slowly drifting tone lasting about two minutes. Unlike conversational modes,
 * WSPR does not carry free text — every transmission encodes exactly three things: your
 * callsign, your Maidenhead grid locator (e.g. {@code "FN42"}), and your transmit power in dBm.
 *
 * <p>Before transmitting you must set your callsign, grid square, and transmit power via
 * {@link #setOperatorCallsign(String)}, {@link #setGridSquare(String)}, and
 * {@link #setPowerDbm(int)}. Open Preferences → Station if your callsign or grid square
 * have not been configured yet.
 *
 * @author Logbook Development Team
 * @version 1.0
 */
public class WsprEncoder implements Encoder {

    private static final String TAG = "WsprEncoder";

    private static final float  OUTPUT_SAMPLE_RATE         = 8000.0f;
    private static final int    BITS_PER_SAMPLE            = 16;

    private static final int    WSPR_SYMBOL_COUNT          = 162;
    private static final double WSPR_SYMBOL_PERIOD_SECONDS = 0.6827;
    private static final double WSPR_TONE_SPACING_HZ       = 1.4648;
    private static final int    WSPR_TONES                 = 4;

    private final ModeProfile profile;
    private String operatorCallsign;

    /** Maidenhead grid locator required for WSPR message content, e.g. {@code "FN42"}. */
    private String gridSquare;

    /** Transmit power in dBm encoded into every WSPR message. */
    private int powerDbm;

    /**
     * Creates a new WSPR encoder. Set your callsign, grid square, and power level before encoding.
     */
    public WsprEncoder() {
        DigitalMode mode = new DigitalMode("WSPR", "WSPR");
        profile = ModeProfile.getProfile(mode);
        operatorCallsign = "";
        gridSquare = "";
        powerDbm = 10;
    }

    /**
     * Returns the {@link DigitalMode} produced by this encoder.
     *
     * @return the WSPR digital mode; never {@code null}
     */
    @Override
    public DigitalMode getMode() {
        return new DigitalMode("WSPR", "WSPR");
    }

    /**
     * Returns {@code true} if all required information has been entered and the encoder
     * is ready to generate a signal.
     *
     * <p>WSPR requires both your callsign and your Maidenhead grid square. Open
     * Preferences → Station to fill in any missing details.
     *
     * @return {@code true} if both callsign and grid square are set; {@code false} otherwise
     */
    @Override
    public boolean isReadyToEncode() {
        return !operatorCallsign.isEmpty() && !gridSquare.isEmpty();
    }

    /**
     * Converts your station details into a WSPR beacon audio signal ready for preview or
     * transmission.
     *
     * <p>WSPR does not carry free text — the payload is always your callsign, grid square,
     * and power level. The {@code text} parameter is accepted for interface compatibility;
     * pass any non-blank string (typically your callsign). Your callsign and grid square must
     * be set before calling this method — open Preferences → Station if they have not been
     * configured yet.
     *
     * @param text  any non-blank string; the actual WSPR payload is derived from your configured
     *              callsign, grid square, and power level
     * @param mode  the digital mode (should be WSPR)
     * @return an {@link AudioBuffer} containing the encoded audio frame; never {@code null}
     * @throws EncoderException if the text is {@code null} or blank, or if the callsign or
     *         grid square has not been set
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
            if (text.length() > 13) {
                throw new EncoderException(
                        "WSPR encodes callsign, grid, and power level only — "
                        + "free text is not supported in standard WSPR");
            }

            int totalSamples = (int) (WSPR_SYMBOL_COUNT * WSPR_SYMBOL_PERIOD_SECONDS * OUTPUT_SAMPLE_RATE);
            int bufferBytes  = totalSamples * (BITS_PER_SAMPLE / 8);
            byte[] outputPcm = new byte[bufferBytes];

            // TODO: implement WSPR waveform synthesis using signal parameters above

            AudioBuffer output = new AudioBuffer();
            output.load(outputPcm, OUTPUT_SAMPLE_RATE);
            return output;

        } catch (EncoderException e) {
            throw e;
        } catch (Exception e) {
            throw new EncoderException("WSPR encoding failed: " + e.getMessage(), e);
        }
    }

    /**
     * Returns the operator callsign that will be broadcast in each WSPR beacon.
     *
     * @return the callsign, or an empty string if none has been set
     */
    public String getOperatorCallsign() {
        return operatorCallsign;
    }

    /**
     * Sets the operator callsign included in every WSPR transmission.
     *
     * <p>The callsign is trimmed and converted to upper case. This must be
     * your licensed amateur radio callsign.
     *
     * @param callsign your amateur radio callsign, for example {@code "W1AW"}
     */
    public void setOperatorCallsign(String callsign) {
        this.operatorCallsign = (callsign == null) ? "" : callsign.trim().toUpperCase();
    }

    /**
     * Returns the Maidenhead grid locator that will be included in each WSPR
     * beacon, for example {@code "FN42"}.
     *
     * @return the four-character grid square, or an empty string if not set
     */
    public String getGridSquare() {
        return gridSquare;
    }

    /**
     * Sets the Maidenhead grid locator included in every WSPR beacon.
     *
     * <p>Use a standard four-character locator such as {@code "FN42"} or
     * {@code "IO91"}. The value is trimmed and converted to upper case.
     * Open <em>Preferences &rarr; Station</em> to find your grid square.
     *
     * @param gridSquare your four-character Maidenhead grid locator
     */
    public void setGridSquare(String gridSquare) {
        this.gridSquare = (gridSquare == null) ? "" : gridSquare.trim().toUpperCase();
    }

    /**
     * Returns the transmit power level, in dBm, encoded into every WSPR message.
     *
     * @return transmit power in dBm; default is {@code 10}
     */
    public int getPowerDbm() {
        return powerDbm;
    }

    /**
     * Sets the transmit power level encoded into every WSPR beacon.
     *
     * <p>WSPR power values are constrained to specific dBm steps defined by
     * the protocol (e.g. 0, 3, 7, 10, 13, 17, 20 … 60 dBm). Pass the value
     * that matches your actual transmitter output as closely as possible.
     *
     * @param powerDbm transmit power in dBm
     */
    public void setPowerDbm(int powerDbm) {
        this.powerDbm = powerDbm;
    }
}
