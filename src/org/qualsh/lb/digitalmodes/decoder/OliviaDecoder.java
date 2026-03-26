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
 * Decoder stub for the Olivia digital mode.
 *
 * <p>Olivia is an extremely robust amateur radio digital mode designed
 * specifically to be decoded even when the signal is completely inaudible to
 * the human ear. It achieves this through a combination of multi-tone
 * frequency-shift keying, a powerful forward-error-correction (FEC) scheme,
 * and synchronisation tones that allow the decoder to lock on to a signal
 * buried deep in the noise floor.
 *
 * <p>The default variant implemented here — Olivia 8/250 — uses
 * {@value #OLIVIA_TONES} tones spread across a {@value #OLIVIA_BANDWIDTH_HZ}&nbsp;Hz
 * passband, with the tones spaced {@value #OLIVIA_BANDWIDTH_HZ}&nbsp;Hz&nbsp;/&nbsp;{@value #OLIVIA_TONES}
 * = 31.25&nbsp;Hz apart and centred at the conventional 1500&nbsp;Hz audio
 * frequency. Other common variants include Olivia 4/125, 8/500, 16/500, and
 * 32/1000, each offering different trade-offs between throughput and noise
 * immunity.
 *
 * <p>Olivia is popular on HF bands during poor propagation conditions — for
 * example during solar minimum or during periods of high geomagnetic activity —
 * where faster modes like FT8 or PSK31 fail to decode reliably. At least
 * {@value #MIN_SAMPLES_REQUIRED} samples (one full second at 8000&nbsp;Hz) are
 * required to detect a signal.
 *
 * <p>This implementation performs a Discrete Fourier Transform and checks the
 * magnitude spectrum at each of the {@value #OLIVIA_TONES} expected Olivia
 * tone positions. Full FEC decoding and character output are not yet
 * implemented.
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
     * Creates a new {@code OliviaDecoder}, loading the Olivia mode profile
     * with its default bandwidth and signal parameters.
     */
    public OliviaDecoder() {
        mode = new DigitalMode("Olivia", "Olivia");
        profile = ModeProfile.getProfile(mode);
        results = new ArrayList<>();
    }

    /**
     * Returns the {@link DigitalMode} that this decoder handles.
     *
     * @return the Olivia digital mode; never {@code null}
     */
    public DigitalMode getMode() {
        return mode;
    }

    /**
     * Attempts to detect an Olivia 8/250 signal in the supplied audio buffer.
     *
     * <p>The buffer must contain at least {@value #MIN_SAMPLES_REQUIRED}
     * PCM samples of 16-bit signed little-endian mono audio. Buffers that are
     * empty or too short are rejected immediately and an empty list is
     * returned.
     *
     * <p>Processing steps:
     * <ol>
     *   <li>Convert raw PCM bytes to a normalised {@code double[]} signal.</li>
     *   <li>Verify the sample count meets the Olivia minimum.</li>
     *   <li>Run a Discrete Fourier Transform and retrieve the magnitude
     *       spectrum.</li>
     *   <li>Compute the expected positions of the {@value #OLIVIA_TONES} Olivia
     *       tones spanning {@value #OLIVIA_BANDWIDTH_HZ}&nbsp;Hz and centred at
     *       1500&nbsp;Hz. Record the magnitude at each expected bin and count
     *       the number of active tones.</li>
     *   <li>Estimate the signal-to-noise ratio from the peak vs. average
     *       magnitude.</li>
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
