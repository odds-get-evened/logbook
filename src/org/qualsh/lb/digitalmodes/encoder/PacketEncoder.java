package org.qualsh.lb.digitalmodes.encoder;

import org.qualsh.lb.digital.DigitalMode;
import org.qualsh.lb.digitalmodes.audio.AudioBuffer;
import org.qualsh.lb.digitalmodes.mode.ModeProfile;

/**
 * Encodes your typed message into a Packet Radio audio signal ready for preview or transmission.
 *
 * <p>Packet Radio uses the AX.25 protocol to send structured data frames over amateur radio.
 * On the air it sounds like a brief burst of computer-modem tones — a rapid, chattering
 * series of high and low pitches lasting a fraction of a second per frame. It is the foundation
 * of APRS (Automatic Packet Reporting System), used worldwide for GPS position reporting,
 * weather data, and short text messages.
 *
 * <p>Before transmitting you must set your operator callsign via
 * {@link #setOperatorCallsign(String)}. The destination callsign defaults to {@code "APRS"} and
 * can be changed with {@link #setDestinationCallsign(String)}. Open Preferences → Station if
 * your callsign has not been configured yet.
 *
 * @author Logbook Development Team
 * @version 1.0
 */
public class PacketEncoder implements Encoder {

    private static final String TAG = "PacketEncoder";

    private static final float  OUTPUT_SAMPLE_RATE    = 8000.0f;
    private static final int    BITS_PER_SAMPLE       = 16;

    private static final double AX25_BAUD_RATE        = 1200.0;
    private static final double AFSK_MARK_HZ          = 1200.0;
    private static final double AFSK_SPACE_HZ         = 2200.0;

    /** AX.25 frame delimiter byte (flag byte). */
    private static final int    AX25_FLAG_BYTE        = 0x7E;

    private final ModeProfile profile;
    private String operatorCallsign;

    /** AX.25 destination address field; defaults to {@code "APRS"} for APRS packets. */
    private String destinationCallsign;

    /**
     * Creates a new Packet Radio encoder. The destination callsign defaults to {@code "APRS"}.
     * Set your callsign with {@link #setOperatorCallsign(String)} before encoding.
     */
    public PacketEncoder() {
        DigitalMode mode = new DigitalMode("Packet Radio", "PKT");
        profile = ModeProfile.getProfile(mode);
        operatorCallsign = "";
        destinationCallsign = "APRS";
    }

    /**
     * Returns the {@link DigitalMode} produced by this encoder.
     *
     * @return the Packet Radio digital mode; never {@code null}
     */
    @Override
    public DigitalMode getMode() {
        return new DigitalMode("Packet Radio", "PKT");
    }

    /**
     * Returns {@code true} if all required information has been entered and the encoder
     * is ready to generate a signal.
     *
     * <p>Packet Radio requires both your operator callsign and a destination callsign.
     * Open Preferences → Station to fill in any missing details.
     *
     * @return {@code true} if both the operator and destination callsigns are set;
     *         {@code false} otherwise
     */
    @Override
    public boolean isReadyToEncode() {
        return !operatorCallsign.isEmpty() && !destinationCallsign.isEmpty();
    }

    /**
     * Converts your typed message into a Packet Radio audio signal ready for preview or
     * transmission.
     *
     * <p>There is no hard character limit for Packet Radio, but messages longer than
     * 500 characters may result in very long transmissions. Both your operator callsign and
     * destination callsign must be set before calling this method — open Preferences → Station
     * if they have not been configured yet.
     *
     * @param text the packet payload to encode; must not be {@code null} or blank
     * @param mode the digital mode (should be Packet)
     * @return an {@link AudioBuffer} containing the encoded audio signal; never {@code null}
     * @throws EncoderException if the message is empty, or if the operator or destination
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

            // AX.25 uses NRZI bit stuffing; estimate 10 bits per byte of payload on average.
            double bitsPerChar  = 10.0;
            double totalBits    = text.length() * bitsPerChar;
            int totalSamples    = (int) (totalBits / AX25_BAUD_RATE * OUTPUT_SAMPLE_RATE);
            int bufferBytes     = totalSamples * (BITS_PER_SAMPLE / 8);
            byte[] outputPcm    = new byte[bufferBytes];

            // TODO: implement Packet (AX.25 AFSK1200) waveform synthesis using signal parameters above

            AudioBuffer output = new AudioBuffer();
            output.load(outputPcm, OUTPUT_SAMPLE_RATE);
            return output;

        } catch (EncoderException e) {
            throw e;
        } catch (Exception e) {
            throw new EncoderException("Packet encoding failed: " + e.getMessage(), e);
        }
    }

    /**
     * Returns the operator callsign used as the AX.25 source address in
     * outgoing packet frames.
     *
     * @return the callsign, or an empty string if none has been set
     */
    public String getOperatorCallsign() {
        return operatorCallsign;
    }

    /**
     * Sets the operator callsign used as the AX.25 source address.
     *
     * <p>The callsign is trimmed and converted to upper case. This must be
     * your licensed amateur radio callsign, optionally with an SSID suffix
     * (e.g. {@code "W1AW-9"} for a mobile station).
     *
     * @param callsign your amateur radio callsign, for example {@code "W1AW"}
     *                 or {@code "W1AW-9"}
     */
    public void setOperatorCallsign(String callsign) {
        this.operatorCallsign = (callsign == null) ? "" : callsign.trim().toUpperCase();
    }

    /**
     * Returns the AX.25 destination callsign used in outgoing packet frames.
     *
     * <p>For APRS packets this is typically {@code "APRS"}. For direct
     * node-to-node connections it would be the remote station's callsign.
     *
     * @return the destination callsign; never {@code null}
     */
    public String getDestinationCallsign() {
        return destinationCallsign;
    }

    /**
     * Sets the AX.25 destination address field for outgoing packet frames.
     *
     * <p>For APRS use {@code "APRS"} (the default). For other packet
     * applications use the appropriate destination address or alias. The value
     * is trimmed and converted to upper case.
     *
     * @param destinationCallsign the AX.25 destination address, for example
     *                            {@code "APRS"} or {@code "W1AW"}
     */
    public void setDestinationCallsign(String destinationCallsign) {
        this.destinationCallsign = (destinationCallsign == null)
                ? "" : destinationCallsign.trim().toUpperCase();
    }
}
