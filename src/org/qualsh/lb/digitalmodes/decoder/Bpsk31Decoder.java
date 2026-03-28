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
 * Decodes BPSK31 signals from loaded audio.
 *
 * <p>BPSK31 is a narrow keyboard-to-keyboard chat mode, recognizable by its distinctive
 * warbling sound. Popular on HF bands for real-time text conversations between operators,
 * it fits dozens of simultaneous contacts into the same space a single voice signal would occupy.
 *
 * <p>Unlike FT8 or WSPR, BPSK31 has no fixed time slots — decoding can begin at any point
 * in the audio. Only a fraction of a second of audio is needed before the decoder can
 * attempt to find a signal.
 *
 * @author Logbook Development Team
 * @version 1.0
 */
public class Bpsk31Decoder {

    private static final String TAG = "Bpsk31Decoder";

    /** Standard BPSK31 symbol rate in baud. */
    private static final double BPSK31_BAUD_RATE = 31.25;

    /**
     * Minimum number of PCM samples required before attempting a decode.
     * Approximately 0.3 seconds at an 8000&nbsp;Hz sample rate — enough to
     * contain at least one complete symbol.
     */
    private static final int MIN_SAMPLES_REQUIRED = 2756;

    private final DigitalMode mode;
    private final ModeProfile profile;
    private List<DecodeResult> results;

    /**
     * Creates a new BPSK31 decoder with default signal parameters.
     */
    public Bpsk31Decoder() {
        mode = new DigitalMode("BPSK31", "BPSK31");
        profile = ModeProfile.getProfile(mode);
        results = new ArrayList<>();
    }

    /**
     * Returns the digital mode handled by this decoder.
     *
     * @return the BPSK31 digital mode; never {@code null}
     */
    public DigitalMode getMode() {
        return mode;
    }

    /**
     * Analyzes the loaded audio and attempts to find and decode any BPSK31 signals present.
     *
     * <p>Results appear in the decode output area and are added to the decode log.
     * A very short amount of audio is sufficient; an empty list is returned when
     * no BPSK31 signals are detected or the audio is too short.
     *
     * @param buffer the audio to analyze; must not be {@code null}
     * @return a list of decoded BPSK31 signals, possibly empty; never {@code null}
     */
    public List<DecodeResult> decode(AudioBuffer buffer) {
        results.clear();
        try {
            if (buffer.isEmpty()) {
                return results;
            }

            // Read only the last 1 second of audio — avoids copying the entire accumulated buffer
            int maxBytes = (int) buffer.getSampleRate() * 2;
            double[] signal = pcmToDouble(buffer.readDecoderWindow(maxBytes));

            if (signal.length < MIN_SAMPLES_REQUIRED) {
                System.err.println(TAG + ": signal length " + signal.length
                        + " samples is below the BPSK31 minimum of "
                        + MIN_SAMPLES_REQUIRED + " samples; skipping");
                return results;
            }

            double halfBw = profile.getDefaultBandwidthHz() / 2.0;
            double lowCut = 1500.0 - halfBw;
            double highCut = 1500.0 + halfBw;
            Butterworth butter = new Butterworth((double) buffer.getSampleRate());
            double[] filtered = butter.bandPassFilter(signal, 4, lowCut, highCut);

            DiscreteFourier dft = new DiscreteFourier(filtered);
            dft.transform();
            double[] magnitudes = dft.getMagnitude(false);

            int peakBin = findPeakBin(magnitudes);
            double peakMagnitude = magnitudes[peakBin];
            double peakHz = ((double) peakBin * buffer.getSampleRate()) / filtered.length;

            double averageMagnitude = calculateAverageMagnitude(magnitudes);
            double snrDb = 10.0 * Math.log10(peakMagnitude / averageMagnitude);

            String decodedText = "[BPSK31 SIGNAL DETECTED] Peak: "
                    + String.format("%.1f", peakHz) + " Hz | BW: 31 Hz";
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
