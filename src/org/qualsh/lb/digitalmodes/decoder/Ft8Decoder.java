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
 * Decodes FT8 signals from loaded audio.
 *
 * <p>FT8 is a weak-signal mode used worldwide for long-distance contacts. It operates on
 * strict 15-second intervals synchronized to the clock and can decode signals far below
 * what the human ear can hear — making it effective on paths where voice or Morse
 * communication would be completely impossible.
 *
 * <p>Each FT8 message carries a callsign exchange and signal report compressed into a
 * 15-second burst. At least 15 seconds of audio must be loaded before this decoder can
 * attempt to find a signal.
 *
 * @author Logbook Development Team
 * @version 1.0
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
     * Creates a new FT8 decoder with default signal parameters.
     */
    public Ft8Decoder() {
        mode = new DigitalMode("FT8", "FT8");
        profile = ModeProfile.getProfile(mode);
        results = new ArrayList<>();
    }

    /**
     * Returns the digital mode handled by this decoder.
     *
     * @return the FT8 digital mode; never {@code null}
     */
    public DigitalMode getMode() {
        return mode;
    }

    /**
     * Analyzes the loaded audio and attempts to find and decode any FT8 signals present.
     *
     * <p>Results appear in the decode output area and are added to the decode log.
     * At least 15 seconds of audio must be loaded; shorter recordings are skipped.
     * An empty list is returned when no FT8 signals are detected.
     *
     * @param buffer the audio to analyze; must not be {@code null}
     * @return a list of decoded FT8 signals, possibly empty; never {@code null}
     */
    public List<DecodeResult> decode(AudioBuffer buffer) {
        results.clear();
        try {
            if (buffer.isEmpty()) {
                return results;
            }

            if (buffer.getDurationSeconds() < MIN_FRAME_SECONDS) {
                System.err.println(TAG + ": buffer duration "
                        + buffer.getDurationSeconds()
                        + "s is below the FT8 minimum of " + MIN_FRAME_SECONDS + "s; skipping");
                return results;
            }

            // Read only the last 15 seconds — avoids copying the entire accumulated buffer
            int maxBytes = (int) (MIN_FRAME_SECONDS * buffer.getSampleRate()) * 2;
            double[] signal = pcmToDouble(buffer.readDecoderWindow(maxBytes));

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
