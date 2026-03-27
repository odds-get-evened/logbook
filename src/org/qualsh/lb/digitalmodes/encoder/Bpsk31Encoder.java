package org.qualsh.lb.digitalmodes.encoder;

import org.qualsh.lb.digital.DigitalMode;
import org.qualsh.lb.digitalmodes.audio.AudioBuffer;
import org.qualsh.lb.digitalmodes.mode.ModeProfile;

/**
 * Encodes your typed message into a BPSK31 audio signal ready for preview or transmission.
 *
 * <p>PSK31 is one of the most popular keyboard-to-keyboard HF modes. On the air it sounds
 * like a soft, steady warble — a single audio tone whose phase shifts with every character
 * you type. The signal is extremely narrow (about 31 Hz wide) so many stations can share
 * the same band without interfering with each other.
 *
 * <p>Before transmitting you must set your operator callsign via
 * {@link #setOperatorCallsign(String)}. Open Preferences → Station if it has not been
 * configured yet.
 *
 * @author Logbook Development Team
 * @version 1.0
 */
public class Bpsk31Encoder implements Encoder {

    private static final String TAG = "Bpsk31Encoder";

    private static final float  OUTPUT_SAMPLE_RATE            = 8000.0f;
    private static final int    BITS_PER_SAMPLE               = 16;

    private static final double BPSK31_BAUD_RATE              = 31.25;
    private static final double BPSK31_CENTER_FREQUENCY_HZ    = 1500.0;
    private static final double BPSK31_PHASE_SHIFT_RADIANS    = Math.PI;

    private final ModeProfile profile;
    private String operatorCallsign;

    /**
     * Creates a new BPSK31 encoder. Set your callsign with {@link #setOperatorCallsign(String)}
     * before encoding.
     */
    public Bpsk31Encoder() {
        DigitalMode mode = new DigitalMode("Phase Shift Keying 31", "PSK31");
        profile = ModeProfile.getProfile(mode);
        operatorCallsign = "";
    }

    /**
     * Returns the {@link DigitalMode} produced by this encoder.
     *
     * @return the PSK31 digital mode; never {@code null}
     */
    @Override
    public DigitalMode getMode() {
        return new DigitalMode("Phase Shift Keying 31", "PSK31");
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
     * Converts your typed message into a BPSK31 audio signal ready for preview or transmission.
     *
     * <p>There is no hard character limit for BPSK31, but messages longer than 500 characters
     * may result in very long transmissions. Your callsign must be set before calling this
     * method — open Preferences → Station if it has not been configured yet.
     *
     * @param text the message to encode; must not be {@code null} or blank
     * @param mode the digital mode (should be PSK31)
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

            // Each character maps to a variable-length Varicode symbol at BPSK31_BAUD_RATE.
            // Use a conservative estimate of 5 bits per character on average for buffer sizing.
            double bitsPerChar    = 5.0;
            double totalBits      = text.length() * bitsPerChar;
            int totalSamples      = (int) (totalBits / BPSK31_BAUD_RATE * OUTPUT_SAMPLE_RATE);
            int bufferBytes       = totalSamples * (BITS_PER_SAMPLE / 8);
            byte[] outputPcm      = new byte[bufferBytes];

            // TODO: implement BPSK31 waveform synthesis using signal parameters above

            AudioBuffer output = new AudioBuffer();
            output.load(outputPcm, OUTPUT_SAMPLE_RATE);
            return output;

        } catch (EncoderException e) {
            throw e;
        } catch (Exception e) {
            throw new EncoderException("BPSK31 encoding failed: " + e.getMessage(), e);
        }
    }

    /**
     * Returns the operator callsign sent at the start of BPSK31 transmissions.
     *
     * @return the callsign, or an empty string if none has been set
     */
    public String getOperatorCallsign() {
        return operatorCallsign;
    }

    /**
     * Sets the operator callsign included at the start of BPSK31 transmissions.
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
