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
 * Decodes Olivia signals from loaded audio.
 *
 * <p>Olivia is an extremely robust mode designed to be decoded even when completely inaudible
 * to the human ear. It is popular on HF during poor band conditions — for example during solar
 * minimum or geomagnetic disturbances — where faster modes like FT8 or PSK31 fail to decode.
 *
 * <p>This decoder handles the Olivia 8/250 variant (8 tones, 250 Hz bandwidth).
 * About one second of audio is needed before the decoder can attempt to find a signal.
 *
 * @author Logbook Development Team
 * @version 1.0
 */
public class OliviaDecoder {

    private static final String TAG = "OliviaDecoder";

    /** Number of tones in the Olivia 8/250 variant. */
    private static final int OLIVIA_TONES = 8;

    /** Total bandwidth of the Olivia 8/250 signal in hertz. */
    private static final int OLIVIA_BANDWIDTH_HZ = 250;

    /**
     * Minimum number of PCM samples required before attempting a decode.
     * Corresponds to one full second at an 8000&nbsp;Hz sample rate.
     */
    private static final int MIN_SAMPLES_REQUIRED = 8000;

    private final DigitalMode mode;
    private final ModeProfile profile;
    private List<DecodeResult> results;

    /**
     * Creates a new Olivia decoder with default signal parameters.
     */
    public OliviaDecoder() {
        mode = new DigitalMode("Olivia", "Olivia");
        profile = ModeProfile.getProfile(mode);
        results = new ArrayList<>();
    }

    /**
     * Returns the digital mode handled by this decoder.
     *
     * @return the Olivia digital mode; never {@code null}
     */
    public DigitalMode getMode() {
        return mode;
    }

    /**
     * Analyzes the loaded audio and attempts to find and decode any Olivia signals present.
     *
     * <p>Results appear in the decode output area and are added to the decode log.
     * About one second of audio is needed; an empty list is returned when no Olivia
     * signals are detected or the audio is too short.
     *
     * @param buffer the audio to analyze; must not be {@code null}
     * @return a list of decoded Olivia signals, possibly empty; never {@code null}
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
                        + " samples is below the Olivia minimum of "
                        + MIN_SAMPLES_REQUIRED + " samples; skipping");
                return results;
            }

            DiscreteFourier dft = new DiscreteFourier(signal);
            dft.transform();
            double[] magnitudes = dft.getMagnitude(false);

            double binWidthHz = (double) buffer.getSampleRate() / signal.length;
            double toneSpacingHz = (double) OLIVIA_BANDWIDTH_HZ / OLIVIA_TONES;
            double startHz = 1500.0 - (OLIVIA_BANDWIDTH_HZ / 2.0) + (toneSpacingHz / 2.0);

            double peakToneMagnitude = 0.0;
            double[] toneMagnitudes = new double[OLIVIA_TONES];

            for (int t = 0; t < OLIVIA_TONES; t++) {
                double toneHz = startHz + t * toneSpacingHz;
                int bin = (int) Math.round(toneHz / binWidthHz);
                if (bin >= 0 && bin < magnitudes.length) {
                    toneMagnitudes[t] = magnitudes[bin];
                    if (toneMagnitudes[t] > peakToneMagnitude) {
                        peakToneMagnitude = toneMagnitudes[t];
                    }
                }
            }

            double threshold = peakToneMagnitude * 0.1;
            int activeToneCount = 0;
            for (double mag : toneMagnitudes) {
                if (mag > threshold) {
                    activeToneCount++;
                }
            }

            double averageMagnitude = calculateAverageMagnitude(magnitudes);
            double snrDb = 10.0 * Math.log10(peakToneMagnitude / averageMagnitude);

            String decodedText = "[OLIVIA 8/250 SIGNAL DETECTED] Center: 1500 Hz | Tones found: "
                    + activeToneCount + "/8";
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
