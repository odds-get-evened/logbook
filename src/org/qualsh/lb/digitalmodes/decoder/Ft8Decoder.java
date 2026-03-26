package org.qualsh.lb.digitalmodes.decoder;

import com.github.psambit9791.jdsp.transform.DiscreteFourier;
import org.qualsh.lb.digital.DigitalMode;
import org.qualsh.lb.digital.decode.DecodeResult;
import org.qualsh.lb.digitalmodes.audio.AudioBuffer;
import org.qualsh.lb.digitalmodes.mode.ModeProfile;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Decoder stub for the FT8 digital mode.
 *
 * <p>FT8 (Franke-Taylor design, 8-FSK modulation) is a weak-signal mode
 * designed for long-distance amateur radio contacts under poor propagation
 * conditions. It can decode signals that are 20&nbsp;dB or more below the
 * audible noise floor, making it extremely effective on paths where voice or
 * CW communication would be impossible.
 *
 * <p>FT8 operates in fixed 15-second time slots synchronised to the UTC
 * clock. Each transmission carries a structured 77-bit message — typically
 * a callsign exchange and signal report — encoded with strong forward error
 * correction. Audio frames shorter than 15 seconds cannot contain a complete
 * FT8 transmission and will be rejected by this decoder.
 *
 * <p>This implementation performs spectral analysis via a Discrete Fourier
 * Transform to locate the dominant signal peak within the audio passband and
 * provides a stub {@link DecodeResult} with frequency and estimated SNR
 * information. Full message decoding (LDPC, synchronisation, message
 * unpacking) is not yet implemented.
 */
public class Ft8Decoder {

    private static final String TAG = "Ft8Decoder";

    /** Minimum audio frame length required for a complete FT8 transmission. */
    private static final double MIN_FRAME_SECONDS = 15.0;

    private DiscreteFourier dft;
    private final DigitalMode mode;
    private final ModeProfile profile;
    private List<DecodeResult> results;

    /**
     * Creates a new {@code Ft8Decoder}, loading the FT8 mode profile with its
     * default bandwidth and signal parameters.
     */
    public Ft8Decoder() {
        mode = new DigitalMode("FT8", "FT8");
        profile = ModeProfile.getProfile(mode);
        results = new ArrayList<>();
    }

    /**
     * Returns the {@link DigitalMode} that this decoder handles.
     *
     * @return the FT8 digital mode; never {@code null}
     */
    public DigitalMode getMode() {
        return mode;
    }

    /**
     * Attempts to decode an FT8 frame from the supplied audio buffer.
     *
     * <p>The buffer must contain at least {@value #MIN_FRAME_SECONDS} seconds
     * of 16-bit signed little-endian mono PCM audio. Buffers that are empty or
     * too short are rejected immediately and an empty list is returned.
     *
     * <p>Processing steps:
     * <ol>
     *   <li>Convert raw PCM bytes to a normalised {@code double[]} signal.</li>
     *   <li>Verify the frame duration meets the FT8 minimum.</li>
     *   <li>Run a Discrete Fourier Transform and retrieve the magnitude spectrum.</li>
     *   <li>Locate the peak magnitude bin and derive its frequency in Hz.</li>
     *   <li>Estimate the signal-to-noise ratio from the peak vs. average magnitude.</li>
     *   <li>Package the findings into a {@link DecodeResult} and return it.</li>
     * </ol>
     *
     * @param buffer the audio buffer to analyse; must not be {@code null}
     * @return a list containing one {@link DecodeResult} when a frame is
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

            if (buffer.getDurationSeconds() < MIN_FRAME_SECONDS) {
                System.err.println(TAG + ": buffer duration "
                        + buffer.getDurationSeconds()
                        + "s is below the FT8 minimum of " + MIN_FRAME_SECONDS + "s; skipping");
                return results;
            }

            dft = new DiscreteFourier(signal);
            dft.transform();
            double[] magnitudes = dft.getMagnitude(false);

            int peakBin = findPeakBin(magnitudes);
            double peakMagnitude = magnitudes[peakBin];
            double peakHz = ((double) peakBin * buffer.getSampleRate()) / signal.length;

            double averageMagnitude = calculateAverageMagnitude(magnitudes);
            double snrDb = 10.0 * Math.log10(peakMagnitude / averageMagnitude);

            String decodedText = "[FT8 FRAME DETECTED] Peak: " + String.format("%.1f", peakHz) + " Hz";
            int timestamp = (int) Instant.now().getEpochSecond();

            DecodeResult result = new DecodeResult(
                    "",
                    decodedText,
                    (float) snrDb,
                    (float) peakHz,
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
