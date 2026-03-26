package org.qualsh.lb.digitalmodes.decoder;

import com.github.psambit9791.jdsp.filter.Butterworth;
import com.github.psambit9791.jdsp.transform.DiscreteFourier;
import org.qualsh.lb.digital.DigitalMode;
import org.qualsh.lb.digital.decode.DecodeResult;
import org.qualsh.lb.digitalmodes.audio.AudioBuffer;
import org.qualsh.lb.digitalmodes.mode.ModeProfile;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Decoder stub for the Packet Radio digital mode.
 *
 * <p>Packet radio uses the AX.25 link-layer protocol — the same protocol
 * that underpins APRS (Automatic Packet Reporting System) — to carry
 * structured data frames over the air. Every frame includes a header
 * containing the source and destination callsigns, optional digipeater
 * addresses, and a payload that can hold free-form text, position reports,
 * weather data, or other structured information.
 *
 * <p>On VHF/UHF, the most common implementation uses Audio Frequency Shift
 * Keying (AFSK) at {@value #AX25_BAUD_RATE} baud. Two audio tones represent
 * the binary states: {@value #AFSK_MARK_HZ}&nbsp;Hz for mark (binary 1) and
 * {@value #AFSK_SPACE_HZ}&nbsp;Hz for space (binary 0). This tone pair fits
 * within the 3&nbsp;kHz passband of a standard FM voice radio, making packet
 * compatible with off-the-shelf handheld transceivers.
 *
 * <p>1200-baud packet is widely used for APRS position reporting, messaging,
 * and telemetry, particularly from mobile stations and weather instruments.
 * At least {@value #MIN_SAMPLES_REQUIRED} samples are required to detect a
 * signal reliably.
 *
 * <p>This implementation applies separate Butterworth bandpass filters around
 * the mark and space tones, then compares their peak magnitudes to determine
 * which tone is currently dominant. Full AX.25 frame synchronisation, HDLC
 * decoding, and NRZI bit recovery are not yet implemented.
 */
public class PacketDecoder {

    private static final String TAG = "PacketDecoder";

    /** Standard 1200-baud packet radio symbol rate. */
    private static final double AX25_BAUD_RATE = 1200.0;

    /** AFSK mark tone frequency in hertz (binary 1). */
    private static final double AFSK_MARK_HZ = 1200.0;

    /** AFSK space tone frequency in hertz (binary 0). */
    private static final double AFSK_SPACE_HZ = 2200.0;

    /** Bandwidth of each per-tone bandpass filter in hertz. */
    private static final double TONE_FILTER_BW_HZ = 200.0;

    /**
     * Minimum number of PCM samples required before attempting a decode.
     * Approximately 0.3 seconds at an 8000&nbsp;Hz sample rate — enough to
     * contain at least one complete AX.25 flag byte.
     */
    private static final int MIN_SAMPLES_REQUIRED = 2400;

    private final DigitalMode mode;
    private final ModeProfile profile;
    private List<DecodeResult> results;

    /**
     * Creates a new {@code PacketDecoder}, loading the Packet mode profile
     * with its default bandwidth and signal parameters.
     */
    public PacketDecoder() {
        mode = new DigitalMode("Packet", "Packet");
        profile = ModeProfile.getProfile(mode);
        results = new ArrayList<>();
    }

    /**
     * Returns the {@link DigitalMode} that this decoder handles.
     *
     * @return the Packet digital mode; never {@code null}
     */
    public DigitalMode getMode() {
        return mode;
    }

    /**
     * Attempts to detect an AX.25 packet signal in the supplied audio buffer.
     *
     * <p>The buffer must contain at least {@value #MIN_SAMPLES_REQUIRED}
     * PCM samples of 16-bit signed little-endian mono audio. Buffers that are
     * empty or too short are rejected immediately and an empty list is
     * returned.
     *
     * <p>Processing steps:
     * <ol>
     *   <li>Convert raw PCM bytes to a normalised {@code double[]} signal.</li>
     *   <li>Verify the sample count meets the packet minimum.</li>
     *   <li>Apply a 4th-order Butterworth bandpass filter centred at the
     *       mark frequency ({@value #AFSK_MARK_HZ}&nbsp;Hz) with
     *       {@value #TONE_FILTER_BW_HZ}&nbsp;Hz bandwidth, then perform a
     *       Discrete Fourier Transform and record the peak magnitude.</li>
     *   <li>Repeat step 3 for the space frequency
     *       ({@value #AFSK_SPACE_HZ}&nbsp;Hz).</li>
     *   <li>Compare the two peak magnitudes to determine whether mark or
     *       space is currently dominant.</li>
     *   <li>Estimate the signal-to-noise ratio from the stronger tone's peak
     *       vs. the overall average magnitude of the full unfiltered
     *       spectrum.</li>
     *   <li>Package the findings into a {@link DecodeResult} and return
     *       it.</li>
     * </ol>
     *
     * @param buffer the audio buffer to analyse; must not be {@code null}
     * @return a list containing one {@link DecodeResult} when a signal is
     *         detected, or an empty list when the buffer is too short, empty,
     *         or an error occurs during processing
     */
    public List<DecodeResult> decode(AudioBuffer buffer) {
        results.clear();
        try {
            if (buffer.isEmpty()) {
                return results;
            }

            double[] signal = pcmToDouble(buffer.getSamples());

            if (signal.length < MIN_SAMPLES_REQUIRED) {
                System.err.println(TAG + ": signal length " + signal.length
                        + " samples is below the packet minimum of "
                        + MIN_SAMPLES_REQUIRED + " samples; skipping");
                return results;
            }

            double sampleRate = (double) buffer.getSampleRate();
            Butterworth butter = new Butterworth(sampleRate);

            double markLow  = AFSK_MARK_HZ  - TONE_FILTER_BW_HZ / 2.0;
            double markHigh = AFSK_MARK_HZ  + TONE_FILTER_BW_HZ / 2.0;
            double[] markFiltered = butter.bandPassFilter(signal, 4, markLow, markHigh);

            DiscreteFourier markDft = new DiscreteFourier(markFiltered);
            markDft.transform();
            double[] markMagnitudes = markDft.getMagnitude(false);
            double markPeak = markMagnitudes[findPeakBin(markMagnitudes)];

            Butterworth butter2 = new Butterworth(sampleRate);
            double spaceLow  = AFSK_SPACE_HZ - TONE_FILTER_BW_HZ / 2.0;
            double spaceHigh = AFSK_SPACE_HZ + TONE_FILTER_BW_HZ / 2.0;
            double[] spaceFiltered = butter2.bandPassFilter(signal, 4, spaceLow, spaceHigh);

            DiscreteFourier spaceDft = new DiscreteFourier(spaceFiltered);
            spaceDft.transform();
            double[] spaceMagnitudes = spaceDft.getMagnitude(false);
            double spacePeak = spaceMagnitudes[findPeakBin(spaceMagnitudes)];

            boolean markStronger = markPeak >= spacePeak;

            DiscreteFourier fullDft = new DiscreteFourier(signal);
            fullDft.transform();
            double[] fullMagnitudes = fullDft.getMagnitude(false);
            double peakMagnitude = Math.max(markPeak, spacePeak);
            double averageMagnitude = calculateAverageMagnitude(fullMagnitudes);
            double snrDb = 10.0 * Math.log10(peakMagnitude / averageMagnitude);

            String decodedText = "[AX.25 PACKET DETECTED] Mark: "
                    + String.format("%.0f", AFSK_MARK_HZ)
                    + " Hz | Space: "
                    + String.format("%.0f", AFSK_SPACE_HZ)
                    + " Hz | Dominant: "
                    + (markStronger ? "MARK" : "SPACE");
            int timestamp = (int) Instant.now().getEpochSecond();

            DecodeResult result = new DecodeResult(
                    "",
                    decodedText,
                    (float) snrDb,
                    1500.0f,
                    timestamp,
                    mode.getId());
            results.add(result);

        } catch (Exception e) {
            System.err.println(TAG + ": decode failed: " + e.getMessage());
            return results;
        }
        return results;
    }

    /**
     * Converts a raw PCM byte array (16-bit signed little-endian mono) into a
     * normalised {@code double[]} with values in the range {@code -1.0} to
     * {@code 1.0}.
     *
     * @param pcm the raw PCM bytes; must have an even length
     * @return normalised signal samples; length is {@code pcm.length / 2}
     */
    private double[] pcmToDouble(byte[] pcm) {
        int numSamples = pcm.length / 2;
        double[] signal = new double[numSamples];
        for (int i = 0; i < numSamples; i++) {
            int lo = pcm[i * 2] & 0xFF;
            int hi = pcm[i * 2 + 1];
            short sample = (short) ((hi << 8) | lo);
            signal[i] = sample / 32768.0;
        }
        return signal;
    }

    /**
     * Returns the index of the highest value in the given magnitude array.
     *
     * @param magnitudes the DFT magnitude spectrum; must not be empty
     * @return index of the peak bin
     */
    private int findPeakBin(double[] magnitudes) {
        int peak = 0;
        for (int i = 1; i < magnitudes.length; i++) {
            if (magnitudes[i] > magnitudes[peak]) {
                peak = i;
            }
        }
        return peak;
    }

    /**
     * Calculates the mean value of the given magnitude array.
     *
     * @param magnitudes the DFT magnitude spectrum
     * @return the arithmetic mean, or {@code 0.0} if the array is empty
     */
    private double calculateAverageMagnitude(double[] magnitudes) {
        if (magnitudes.length == 0) {
            return 0.0;
        }
        double sum = 0.0;
        for (double m : magnitudes) {
            sum += m;
        }
        return sum / magnitudes.length;
    }
}
