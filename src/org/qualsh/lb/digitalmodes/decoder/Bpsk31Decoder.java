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
 * Decoder stub for the BPSK31 digital mode.
 *
 * <p>BPSK31 (Binary Phase Shift Keying, 31 baud) is one of the most popular
 * keyboard-to-keyboard chat modes in amateur radio. Operators type messages in
 * real time and the text is transmitted and received live at the other end,
 * making it ideal for casual QSOs and ragchewing on HF.
 *
 * <p>The mode uses a symbol rate of {@value #BPSK31_BAUD_RATE} baud — one
 * symbol per character on average — and occupies an extremely narrow
 * 31&nbsp;Hz bandwidth. This compact footprint makes BPSK31 exceptionally
 * efficient on crowded bands such as 40&nbsp;m and 20&nbsp;m, where dozens of
 * QSOs can fit side by side in the space normally occupied by a single SSB
 * voice transmission.
 *
 * <p>Unlike FT8 or WSPR, BPSK31 is a continuous-stream mode with no fixed
 * time slots. Decoding can begin at any point in the audio and requires only
 * a short segment — approximately 0.3 seconds ({@value #MIN_SAMPLES_REQUIRED}
 * samples at 8000&nbsp;Hz) — to detect a signal.
 *
 * <p>This implementation performs spectral analysis via a Discrete Fourier
 * Transform, preceded by a Butterworth bandpass filter centred at the
 * conventional 1500&nbsp;Hz audio centre frequency, and provides a stub
 * {@link DecodeResult} with frequency and estimated SNR information. Full
 * Varicode character decoding is not yet implemented.
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
     * Creates a new {@code Bpsk31Decoder}, loading the BPSK31 mode profile
     * with its default bandwidth and signal parameters.
     */
    public Bpsk31Decoder() {
        mode = new DigitalMode("BPSK31", "BPSK31");
        profile = ModeProfile.getProfile(mode);
        results = new ArrayList<>();
    }

    /**
     * Returns the {@link DigitalMode} that this decoder handles.
     *
     * @return the BPSK31 digital mode; never {@code null}
     */
    public DigitalMode getMode() {
        return mode;
    }

    /**
     * Attempts to detect a BPSK31 signal in the supplied audio buffer.
     *
     * <p>The buffer must contain at least {@value #MIN_SAMPLES_REQUIRED}
     * PCM samples of 16-bit signed little-endian mono audio. Buffers that are
     * empty or too short are rejected immediately and an empty list is
     * returned.
     *
     * <p>Processing steps:
     * <ol>
     *   <li>Convert raw PCM bytes to a normalised {@code double[]} signal.</li>
     *   <li>Verify the sample count meets the BPSK31 minimum.</li>
     *   <li>Apply a 4th-order Butterworth bandpass filter centred at
     *       1500&nbsp;Hz using the mode's default bandwidth.</li>
     *   <li>Run a Discrete Fourier Transform and retrieve the magnitude
     *       spectrum.</li>
     *   <li>Locate the peak magnitude bin and derive its frequency in Hz.</li>
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
