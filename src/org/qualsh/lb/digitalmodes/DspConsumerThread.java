package org.qualsh.lb.digitalmodes;

import com.github.psambit9791.jdsp.transform.FastFourier;
import org.qualsh.lb.digitalmodes.audio.AudioBuffer;
import org.qualsh.lb.digitalmodes.audio.AudioRingBuffer;
import org.qualsh.lb.digitalmodes.audio.PlaybackController;
import org.qualsh.lb.digitalmodes.spectrum.FFTPanel;
import org.qualsh.lb.digitalmodes.spectrum.WaterfallPanel;

import javax.swing.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Background thread that continuously pulls audio data from the active source,
 * computes the FFT spectrum and waterfall row entirely off the Event Dispatch Thread,
 * and pushes the pre-computed results to the display panels via
 * {@link SwingUtilities#invokeLater(Runnable)}.
 *
 * <p>Two source modes are supported:
 * <ul>
 *   <li><strong>Ring-buffer mode</strong> (live rig): when a non-{@code null}
 *       {@link AudioRingBuffer} is set via {@link #setRingBuffer(AudioRingBuffer)},
 *       the most-recent window of samples is read from the ring buffer on every tick.
 *       The waterfall scrolls continuously.</li>
 *   <li><strong>Snapshot mode</strong> (WAV file / recording): when no ring buffer
 *       is set, the thread reads from the shared {@link AudioBuffer} snapshot.
 *       The waterfall only scrolls while the {@link PlaybackController} reports
 *       that audio is playing.</li>
 * </ul>
 *
 * <p>Call {@link #start()} to begin ticking and {@link #stop()} to shut down cleanly.
 * The underlying executor thread is a daemon so it will not prevent JVM exit.
 *
 * @author Logbook Development Team
 * @version 1.0
 */
public class DspConsumerThread {

    private static final int REFRESH_RATE_MS  = 80;
    private static final int FFT_WINDOW_SAMPLES = 4096;

    private final AudioBuffer        snapshotBuffer;
    private final PlaybackController playbackController;
    private final FFTPanel           fftPanel;
    private final WaterfallPanel     waterfallPanel;

    /** Non-null when the live rig is active; {@code null} in WAV/recording mode. */
    private volatile AudioRingBuffer ringBuffer;

    private ScheduledExecutorService scheduler;

    /**
     * Creates a new DSP consumer thread.
     *
     * @param snapshotBuffer     the shared audio snapshot buffer used in WAV/recording mode;
     *                           must not be {@code null}
     * @param playbackController the playback controller used to track WAV playback position;
     *                           must not be {@code null}
     * @param fftPanel           the spectrum display panel that receives computed magnitudes;
     *                           must not be {@code null}
     * @param waterfallPanel     the waterfall display panel that receives new rows;
     *                           must not be {@code null}
     */
    public DspConsumerThread(AudioBuffer snapshotBuffer,
                             PlaybackController playbackController,
                             FFTPanel fftPanel,
                             WaterfallPanel waterfallPanel) {
        this.snapshotBuffer     = snapshotBuffer;
        this.playbackController = playbackController;
        this.fftPanel           = fftPanel;
        this.waterfallPanel     = waterfallPanel;
    }

    /**
     * Switches to ring-buffer mode by supplying the live-rig ring buffer.
     *
     * <p>Pass {@code null} to return to snapshot (WAV) mode. This method is
     * thread-safe and takes effect on the next scheduled tick.
     *
     * @param ringBuffer the ring buffer to read from, or {@code null} to clear
     */
    public void setRingBuffer(AudioRingBuffer ringBuffer) {
        this.ringBuffer = ringBuffer;
    }

    /**
     * Starts the scheduled DSP tick at {@value #REFRESH_RATE_MS} ms intervals.
     *
     * <p>Has no effect if already running.
     */
    public void start() {
        if (scheduler != null && !scheduler.isShutdown()) {
            return;
        }
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "dsp-consumer");
            t.setDaemon(true);
            return t;
        });
        scheduler.scheduleAtFixedRate(this::dspTick, 0, REFRESH_RATE_MS, TimeUnit.MILLISECONDS);
    }

    /**
     * Stops the DSP tick and releases the executor thread.
     *
     * <p>Has no effect if already stopped.
     */
    public void stop() {
        if (scheduler != null) {
            scheduler.shutdownNow();
            scheduler = null;
        }
    }

    // -------------------------------------------------------------------------
    // Internal tick logic
    // -------------------------------------------------------------------------

    private void dspTick() {
        try {
            AudioRingBuffer rb = ringBuffer;
            if (rb != null) {
                tickFromRingBuffer(rb);
            } else {
                tickFromSnapshotBuffer();
            }
        } catch (Exception e) {
            // DSP errors are non-fatal; the next tick will retry silently.
        }
    }

    /**
     * Ring-buffer tick: reads the most-recent window from the live rig buffer,
     * computes FFT, and pushes results to the panels. Waterfall scrolls always.
     */
    private void tickFromRingBuffer(AudioRingBuffer rb) {
        float[] window = new float[FFT_WINDOW_SAMPLES];
        int count = rb.readLatest(window, FFT_WINDOW_SAMPLES);
        if (count < 2) {
            return;
        }

        float sampleRate = rb.getSampleRate();
        double[] magnitudes = computeFft(window, count);
        if (magnitudes == null) {
            return;
        }

        final double[] mag    = magnitudes;
        final float    rate   = sampleRate;
        final int      fftSz  = nextPowerOfTwo(count);

        SwingUtilities.invokeLater(() -> {
            fftPanel.setMagnitudes(mag, rate, fftSz);
            waterfallPanel.appendRow(mag); // continuous scroll in rig mode
        });
    }

    /**
     * Snapshot-buffer tick: reads a window from the shared AudioBuffer at the current
     * playback position, computes FFT, and pushes results. Waterfall only scrolls
     * while the playback controller reports active playback.
     */
    private void tickFromSnapshotBuffer() {
        if (snapshotBuffer == null || snapshotBuffer.isEmpty()) {
            return;
        }

        int currentPosition = (playbackController != null)
                ? playbackController.getPlaybackPositionSamples()
                : 0;

        byte[] raw        = snapshotBuffer.getSamples();
        float  sampleRate = snapshotBuffer.getSampleRate();

        int numSamples  = raw.length / 2; // 16-bit mono: 2 bytes per sample
        int windowStart = currentPosition;
        if (windowStart + FFT_WINDOW_SAMPLES > numSamples) {
            windowStart = Math.max(0, numSamples - FFT_WINDOW_SAMPLES);
        }
        int windowSamples = Math.min(FFT_WINDOW_SAMPLES, numSamples - windowStart);
        if (windowSamples <= 0) {
            return;
        }

        // Convert 16-bit signed little-endian PCM to normalised floats.
        float[] window = new float[windowSamples];
        for (int i = 0; i < windowSamples; i++) {
            int idx    = windowStart + i;
            int lo     = raw[idx * 2]     & 0xFF;
            int hi     = raw[idx * 2 + 1];
            window[i]  = ((hi << 8) | lo) / 32768.0f;
        }

        double[] magnitudes = computeFft(window, windowSamples);
        if (magnitudes == null) {
            return;
        }

        boolean isPlaying = playbackController != null && playbackController.isPlaying();

        final double[] mag   = magnitudes;
        final float    rate  = sampleRate;
        final int      fftSz = nextPowerOfTwo(windowSamples);
        final boolean  play  = isPlaying;

        SwingUtilities.invokeLater(() -> {
            fftPanel.setMagnitudes(mag, rate, fftSz);
            if (play) {
                waterfallPanel.appendRow(mag);
            }
        });
    }

    // -------------------------------------------------------------------------
    // DSP helpers
    // -------------------------------------------------------------------------

    /**
     * Computes the FFT magnitude spectrum for the given floating-point window.
     *
     * @param window samples in the window (normalized)
     * @param count  number of valid samples in {@code window}
     * @return magnitude array (half-spectrum), or {@code null} on error
     */
    private double[] computeFft(float[] window, int count) {
        try {
            int fftSize = nextPowerOfTwo(count);
            double[] signal = new double[fftSize];
            for (int i = 0; i < count; i++) {
                signal[i] = window[i];
            }
            FastFourier fft = new FastFourier(signal);
            fft.transform();
            return fft.getMagnitude(false);
        } catch (Exception e) {
            return null;
        }
    }

    private static int nextPowerOfTwo(int n) {
        int p = 1;
        while (p < n) {
            p <<= 1;
        }
        return p;
    }
}
