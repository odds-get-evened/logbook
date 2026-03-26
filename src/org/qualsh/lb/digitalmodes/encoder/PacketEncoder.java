package org.qualsh.lb.digitalmodes.encoder;

import org.qualsh.lb.digital.DigitalMode;
import org.qualsh.lb.digitalmodes.audio.AudioBuffer;
import org.qualsh.lb.digitalmodes.mode.ModeProfile;

/**
 * Encoder for the Packet Radio (AX.25 / AFSK1200) digital mode.
 *
 * <p>Packet Radio uses the AX.25 link-layer protocol to carry structured data
 * frames over amateur radio. On the air it sounds like a brief burst of
 * computer-modem tones — a rapid, chattering series of high and low pitches
 * lasting a fraction of a second per frame. The audio is produced by an
 * AFSK (Audio Frequency Shift Keying) modulator that switches between a
 * mark tone at {@value #AFSK_MARK_HZ} Hz and a space tone at
 * {@value #AFSK_SPACE_HZ} Hz at {@value #AX25_BAUD_RATE} baud.
 *
 * <p>Packet is most commonly used for APRS (Automatic Packet Reporting System)
 * on 144.390 MHz (North America), which carries real-time GPS position
 * reports, weather data, and short messages. The destination callsign
 * ({@code "APRS"} by default) identifies the packet type on the APRS network.
 * Custom AX.25 applications may use other destination addresses.
 *
 * <p>Each AX.25 frame is delimited by {@value #AX25_FLAG_BYTE} (0x7E) flag
 * bytes. There is no hard limit on message length, but very long packets
 * increase the chance of corruption on busy channels.
 *
 * <p>Before transmitting you must set your operator callsign via
 * {@link #setOperatorCallsign(String)}. The destination callsign defaults to
 * {@code "APRS"} and can be changed with {@link #setDestinationCallsign(String)}.
 * Open <em>Preferences &rarr; Station</em> if your callsign has not been
 * configured yet.
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
     * Creates a new {@code PacketEncoder}, loading the Packet mode profile with its
     * default bandwidth and signal parameters. The operator callsign is initially
     * empty; the destination callsign defaults to {@code "APRS"}. Set your
     * callsign with {@link #setOperatorCallsign(String)} before encoding.
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
     * Returns {@code true} when both an operator callsign and a destination
     * callsign have been configured so the encoder can build a valid AX.25
     * frame.
     *
     * @return {@code true} if both callsigns are set; {@code false} otherwise
     */
    @Override
    public boolean isReadyToEncode() {
        return !operatorCallsign.isEmpty() && !destinationCallsign.isEmpty();
    }

    /**
     * Encodes the supplied text into an AX.25 packet audio stream using AFSK1200.
     *
     * <p>There is no hard character limit for Packet, but messages longer than
     * 500 characters may result in very long transmissions and exceed the
     * practical AX.25 information field size. The audio alternates between a
     * mark tone at {@value #AFSK_MARK_HZ} Hz and a space tone at
     * {@value #AFSK_SPACE_HZ} Hz at {@value #AX25_BAUD_RATE} baud. Each frame
     * is wrapped in {@code 0x7E} flag bytes per the AX.25 specification.
     *
     * <p>The returned buffer contains silence as a placeholder; actual AX.25
     * frame building and AFSK waveform synthesis are deferred to a future DSP
     * implementation phase.
     *
     * @param text the packet payload to encode; must not be {@code null} or blank
     * @param mode the digital mode (should be Packet)
     * @return an {@link AudioBuffer} containing the encoded audio stream
     * @throws EncoderException if the message is empty, or if the operator or
     *         destination callsign has not been set
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
