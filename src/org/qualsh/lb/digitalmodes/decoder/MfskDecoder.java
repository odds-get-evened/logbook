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
 * Decoder stub for the MFSK16 digital mode.
 *
 * <p>MFSK16 (Multiple Frequency Shift Keying, 16 tones) transmits data by
 * switching between 16 discrete audio tones simultaneously, making it
 * dramatically more robust than single-tone modes like BPSK31 or RTTY. Each
 * tone occupies a slot {@value #MFSK_TONE_SPACING_HZ}&nbsp;Hz wide, and the
 * full comb of {@value #MFSK_TONES} tones spans roughly 250&nbsp;Hz of audio
 * bandwidth.
 *
 * <p>The multi-tone architecture means that even if several tones are lost to
 * fading or interference, the remaining tones carry enough redundancy to
 * reconstruct the original text. This makes MFSK16 a popular choice for
 * long-distance HF paths prone to selective fading — including maritime mobile
 * and DXpedition communications — where BPSK31 would fail entirely.
 *
 * <p>Like BPSK31 and RTTY, MFSK16 is a continuous-stream, keyboard-to-keyboard
 * mode with no fixed time slots. At least {@value #MIN_SAMPLES_REQUIRED}
 * samples are required to detect a signal reliably.
 *
 * <p>This implementation performs a Discrete Fourier Transform and scans the
 * resulting magnitude spectrum for the characteristic comb of
 * {@value #MFSK_TONES} evenly-spaced peaks. Full character decoding is not yet
 * implemented.
 */
public class MfskDecoder {

    private static final String TAG = "MfskDecoder";

    /** Number of tones used by MFSK16. */
    private static final int MFSK_TONES = 16;

    /** Spacing between adjacent MFSK16 tones in hertz. */
    private static final double MFSK_TONE_SPACING_HZ = 15.625;

    /**
     * Minimum number of PCM samples required before attempting a decode.
     * Approximately 0.5 seconds at an 8000&nbsp;Hz sample rate.
     */
    private static final int MIN_SAMPLES_REQUIRED = 4000;

    private final DigitalMode mode;
    private final ModeProfile profile;
    private List<DecodeResult> results;

    /**
     * Creates a new {@code MfskDecoder}, loading the MFSK16 mode profile
     * with its default bandwidth and signal parameters.
     */
    public MfskDecoder() {
        mode = new DigitalMode("MFSK16", "MFSK16");
        profile = ModeProfile.getProfile(mode);
        results = new ArrayList<>();
    }

    /**
     * Returns the {@link DigitalMode} that this decoder handles.
     *
     * @return the MFSK16 digital mode; never {@code null}
     */
    public DigitalMode getMode() {
        return mode;
    }

    /**
     * Attempts to detect an MFSK16 signal in the supplied audio buffer.
     *
     * <p>The buffer must contain at least {@value #MIN_SAMPLES_REQUIRED}
     * PCM samples of 16-bit signed little-endian mono audio. Buffers that are
     * empty or too short are rejected immediately and an empty list is
     * returned.
     *
     * <p>Processing steps:
     * <ol>
     *   <li>Convert raw PCM bytes to a normalised {@code double[]} signal.</li>
     *   <li>Verify the sample count meets the MFSK16 minimum.</li>
     *   <li>Run a Discrete Fourier Transform and retrieve the magnitude
     *       spectrum.</li>
     *   <li>Scan the spectrum for {@value #MFSK_TONES} evenly-spaced tone
     *       positions centred at 1500&nbsp;Hz, record the magnitude at each
     *       expected bin, identify the strongest tone, and count how many tones
     *       exceed 10&nbsp;% of the peak magnitude.</li>
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
                        + " samples is below the MFSK16 minimum of "
                        + MIN_SAMPLES_REQUIRED + " samples; skipping");
                return results;
            }

            DiscreteFourier dft = new DiscreteFourier(signal);
            dft.transform();
            double[] magnitudes = dft.getMagnitude(false);

            double binWidthHz = (double) buffer.getSampleRate() / signal.length;
            double startHz = 1500.0 - (MFSK_TONES / 2.0 - 0.5) * MFSK_TONE_SPACING_HZ;

            double peakToneMagnitude = 0.0;
            double strongestToneHz = startHz;
            double[] toneMagnitudes = new double[MFSK_TONES];

            for (int t = 0; t < MFSK_TONES; t++) {
                double toneHz = startHz + t * MFSK_TONE_SPACING_HZ;
                int bin = (int) Math.round(toneHz / binWidthHz);
                if (bin >= 0 && bin < magnitudes.length) {
                    toneMagnitudes[t] = magnitudes[bin];
                    if (toneMagnitudes[t] > peakToneMagnitude) {
                        peakToneMagnitude = toneMagnitudes[t];
                        strongestToneHz = toneHz;
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

            String decodedText = "[MFSK16 SIGNAL DETECTED] Strongest tone: "
                    + String.format("%.1f", strongestToneHz)
                    + " Hz | Active tones detected: " + activeToneCount;
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
