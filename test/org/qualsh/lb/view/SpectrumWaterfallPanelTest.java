package org.qualsh.lb.view;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.awt.GraphicsEnvironment;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link SpectrumWaterfallPanel} signal-detection logic.
 *
 * <p>Tests drive the panel with synthetic PCM (generated entirely in memory) so no
 * live audio device or running decoder is required.  All tests are skipped
 * automatically in headless CI environments where Swing components cannot be
 * constructed.
 *
 * <h2>PCM format</h2>
 * All helpers produce 16-bit signed little-endian mono at 48 000 Hz, matching
 * {@code AudioRouter#FORMAT}.
 *
 * <h2>Convergence</h2>
 * The signal-presence indicator uses an EMA with α = 0.15 (~21 ms/hop).  After
 * 80 hops (~1.7 s) the smoothed level has converged to &gt;99 % of its final
 * value, so each test feeds exactly that many hops.
 */
class SpectrumWaterfallPanelTest {

    // ── PCM constants (must match SpectrumWaterfallPanel internals) ───────────

    private static final int SAMPLE_RATE = 48_000;
    private static final int HOP_SIZE    = 1_024;   // samples per FFT hop
    private static final int HOPS        = 80;       // enough for EMA convergence

    // ── PCM helpers ───────────────────────────────────────────────────────────

    /**
     * Generates {@value #HOPS} × {@value #HOP_SIZE} samples of a pure sine wave
     * at {@code freqHz} with peak amplitude {@code amplitude} (0.0 – 1.0).
     * Output is 16-bit signed little-endian PCM.
     */
    static byte[] tone(int freqHz, float amplitude) {
        int nSamples = HOPS * HOP_SIZE;
        byte[] pcm = new byte[nSamples * 2];
        for (int i = 0; i < nSamples; i++) {
            double phase = 2.0 * Math.PI * freqHz * i / SAMPLE_RATE;
            short s = (short) Math.round(amplitude * Short.MAX_VALUE * Math.sin(phase));
            pcm[i * 2]     = (byte) (s & 0xFF);
            pcm[i * 2 + 1] = (byte) ((s >> 8) & 0xFF);
        }
        return pcm;
    }

    /**
     * Generates {@value #HOPS} × {@value #HOP_SIZE} samples of digital silence
     * (all zero bytes).
     */
    static byte[] silence() {
        return new byte[HOPS * HOP_SIZE * 2];
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private static void assumeNotHeadless() {
        Assumptions.assumeFalse(
                GraphicsEnvironment.isHeadless(),
                "Skipping: Swing components require a display");
    }

    // ── Tests ─────────────────────────────────────────────────────────────────

    /**
     * Feeding silence should leave the passband average well below the detection
     * threshold and {@code isSignalPresent()} must return {@code false}.
     */
    @Test
    void silenceYieldsNoSignal() {
        assumeNotHeadless();

        SpectrumWaterfallPanel panel = new SpectrumWaterfallPanel();
        panel.setSelection(1_500, 400); // 1 300 – 1 700 Hz passband

        panel.feedPcm(silence());

        assertFalse(panel.isSignalPresent(),
                "No signal should be present after feeding silence");
        assertTrue(panel.getPassbandAvgDb() < -65f,
                "Passband dB should be below threshold for silence, got "
                        + panel.getPassbandAvgDb());
    }

    /**
     * A strong tone placed inside the passband must be detected as a signal after
     * the EMA has converged.
     */
    @Test
    void strongToneInPassbandIsDetected() {
        assumeNotHeadless();

        SpectrumWaterfallPanel panel = new SpectrumWaterfallPanel();
        panel.setSelection(1_500, 400); // passband: 1 300 – 1 700 Hz

        panel.feedPcm(tone(1_500, 0.5f)); // 1 500 Hz at −6 dBFS

        assertTrue(panel.isSignalPresent(),
                "A strong in-band tone should trigger signal detection");
        assertTrue(panel.getPassbandAvgDb() > -65f,
                "Passband dB should be above threshold for a strong tone, got "
                        + panel.getPassbandAvgDb());
    }

    /**
     * A tone placed well outside the passband must not trigger the signal detector.
     * The IIR bandpass filter attenuates it enough that the passband average stays
     * below the threshold.
     */
    @Test
    void toneOutsidePassbandIsNotDetected() {
        assumeNotHeadless();

        SpectrumWaterfallPanel panel = new SpectrumWaterfallPanel();
        panel.setSelection(1_500, 400); // passband: 1 300 – 1 700 Hz

        panel.feedPcm(tone(3_000, 0.5f)); // 3 000 Hz – well outside passband

        assertFalse(panel.isSignalPresent(),
                "A tone outside the passband should not trigger signal detection");
    }

    /**
     * Verifies that the passband average falls within a physically plausible dB
     * range for a known-amplitude tone.  A 0.5-amplitude sine (−6 dBFS full-scale)
     * spread across a 400 Hz passband should produce a passband average somewhere
     * between −40 dB and −5 dB after FFT normalisation.
     */
    @Test
    void passbandDbIsInExpectedRangeForKnownTone() {
        assumeNotHeadless();

        SpectrumWaterfallPanel panel = new SpectrumWaterfallPanel();
        panel.setSelection(1_500, 400);

        panel.feedPcm(tone(1_500, 0.5f));

        float db = panel.getPassbandAvgDb();
        assertTrue(db > -40f && db < -5f,
                "Passband average for a 0.5-amplitude tone should be in [-40, -5] dB, got " + db);
    }
}
