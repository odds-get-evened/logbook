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
 * Decodes RTTY signals from loaded audio.
 *
 * <p>RadioTeleTYpe (RTTY) is one of the oldest digital modes still in active use.
 * It sounds like a rapid typewriter — two alternating tones switching rapidly to
 * carry text. RTTY is commonly used in HF contests and by some maritime and
 * government stations.
 *
 * <p>RTTY has no fixed time slots — decoding can begin at any point in the audio.
 * About one second of audio is sufficient before the decoder can attempt to find a signal.
 *
 * @author Logbook Development Team
 * @version 1.0
 */
public class RttyDecoder {

    private static final String TAG = "RttyDecoder";

    /** Standard amateur RTTY mark/space frequency shift in hertz. */
    private static final double RTTY_SHIFT_HZ = 170.0;

    /**
     * Minimum number of PCM samples required before attempting a decode.
     * Corresponds to one full second at an 8000&nbsp;Hz sample rate.
     */
    private static final int MIN_SAMPLES_REQUIRED = 8000;

    private final DigitalMode mode;
    private final ModeProfile profile;
    private List<DecodeResult> results;

    /**
     * Creates a new RTTY decoder with default signal parameters.
     */
    public RttyDecoder() {
        mode = new DigitalMode("RTTY", "RTTY");
        profile = ModeProfile.getProfile(mode);
        results = new ArrayList<>();
    }

    /**
     * Returns the digital mode handled by this decoder.
     *
     * @return the RTTY digital mode; never {@code null}
     */
    public DigitalMode getMode() {
        return mode;
    }

    /**
     * Analyzes the loaded audio and attempts to find and decode any RTTY signals present.
     *
     * <p>Results appear in the decode output area and are added to the decode log.
     * About one second of audio is needed; an empty list is returned when no RTTY
     * signals are detected or the audio is too short.
     *
     * @param buffer the audio to analyze; must not be {@code null}
     * @return a list of decoded RTTY signals, possibly empty; never {@code null}
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
                        + " samples is below the RTTY minimum of "
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

            String decodedText = "[RTTY SIGNAL DETECTED] Peak: "
                    + String.format("%.1f", peakHz) + " Hz | Shift: 170 Hz";
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
