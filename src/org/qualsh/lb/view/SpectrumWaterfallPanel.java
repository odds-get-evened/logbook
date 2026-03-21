package org.qualsh.lb.view;

import org.qualsh.lb.digital.RxMode;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * SpectrumWaterfallPanel – real-time FFT spectrum analyser + scrolling waterfall.
 *
 * <h2>Layout</h2>
 * <pre>
 *   ┌─ spectrum (SPECTRUM_HEIGHT px) ──────────────────────────────┐
 *   │  amplitude ▲                                                  │
 *   │            │╭╮ ╭──╮                                          │
 *   │            └─┴─┴──┴──────── freq →                           │
 *   ├─ waterfall ───────────────────────────────────────────────────┤
 *   │  newest ─────── [selection band] ───────────────────────────  │
 *   │  older  ──────────────────────────────────────────────────    │
 *   │  …                                                            │
 *   └───────────────────────────── 0 Hz ──────────── 4000 Hz ──────┘
 * </pre>
 *
 * <h2>Usage</h2>
 * <ol>
 *   <li>Create the panel and add it to your layout.</li>
 *   <li>Register a capture listener: {@code AudioRouter.getInstance().addCaptureListener(panel::feedPcm)}</li>
 *   <li>Optionally set a selection listener to be notified when the user
 *       clicks/drags to select a frequency band.</li>
 * </ol>
 *
 * <h2>Frequency selection</h2>
 * <ul>
 *   <li><b>Click</b> – places the centre cursor and resets bandwidth to the mode default.</li>
 *   <li><b>Click + drag</b> – defines a custom bandwidth selection.</li>
 *   <li>The selection is highlighted on both the spectrum and waterfall sections.</li>
 * </ul>
 *
 * <h2>Thread safety</h2>
 * <p>{@link #feedPcm(byte[])} may be called from any thread (typically the audio
 * capture thread).  All image modifications are protected by {@code imgLock}.
 * {@link #repaint()} is always posted to the EDT via
 * {@link SwingUtilities#invokeLater}.
 */
public class SpectrumWaterfallPanel extends JPanel {

    // ── Constants ─────────────────────────────────────────────────────────────

    private static final int SAMPLE_RATE     = 48_000;
    private static final int FFT_SIZE        = 2048;
    private static final int HOP_SIZE        = FFT_SIZE / 2;   // 50% overlap → ~21 ms/line
    private static final float MAX_FREQ_HZ   = 4_000f;         // display 0–4 kHz
    private static final int  SPECTRUM_HEIGHT = 80;             // px for the top spectrum strip

    // ── Pre-computed tables ────────────────────────────────────────────────────

    /** Hann window coefficients, length FFT_SIZE. */
    private static final float[] HANN = buildHannWindow(FFT_SIZE);

    /**
     * Color palette (256 entries, packed RGB).
     * Maps normalised signal power (0 = quiet, 255 = loud) to a heat-map color:
     * black → dark-blue → cyan → green → yellow → red.
     */
    private static final int[] PALETTE_RGB = buildPaletteRGB();

    // ── Audio accumulation ────────────────────────────────────────────────────

    /** Circular sample ring for overlapping FFT windows. */
    private final float[] ringBuf = new float[FFT_SIZE];
    private int ringPos          = 0;
    private int newSamplesCount  = 0;

    // ── Spectrum / waterfall state ────────────────────────────────────────────

    /** Last computed magnitude spectrum in dB (length = FFT_SIZE/2). */
    private volatile float[] spectrumDb;

    /** dB range for color-mapping.  Signals outside this range are clipped. */
    private float displayMinDb = -90f;
    private float displayMaxDb = -20f;

    /** Waterfall scroll image (width=panel width, height=panel height−SPECTRUM_HEIGHT). */
    private BufferedImage waterfallImg;
    private final Object  imgLock = new Object();

    // ── Receive mode ──────────────────────────────────────────────────────────

    /** Current demodulation mode; controls filter placement and passband display. */
    private RxMode rxMode = RxMode.USB;

    // ── Frequency selection ────────────────────────────────────────────────────

    private int centerFreqHz = 1500;
    private int bandwidthHz  = 200;

    /** Pixel half-width of the padded grab zone around the centre cursor line. */
    private static final int CENTER_GRAB_PX = 12;

    private enum DragMode { NONE, CENTER, LOW_EDGE, HIGH_EDGE }
    private DragMode dragMode    = DragMode.NONE;
    private int      dragStartX  = -1;

    // ── Audio bandpass filter ──────────────────────────────────────────────────

    /**
     * Immutable container for 2nd-order IIR biquad bandpass coefficients.
     * A new instance is published via a volatile field whenever the selection
     * changes, ensuring the audio-capture thread always sees a consistent set.
     */
    private static final class FilterCoeffs {
        final double b0, b2, a1, a2; // b1 = 0 for a bandpass biquad
        FilterCoeffs(double b0, double b2, double a1, double a2) {
            this.b0 = b0; this.b2 = b2; this.a1 = a1; this.a2 = a2;
        }
    }

    /** Non-null when bandpass filtering is active; null means pass-through. */
    private volatile FilterCoeffs filterCoeffs = null;

    // Filter delay lines – only written from the audio-capture thread; the
    // occasional benign data race when coefficients are swapped is acceptable
    // (it causes at most a brief transient on the spectrum display).
    private double flt_x1, flt_x2, flt_y1, flt_y2;

    /** Called with (centerHz, bandwidthHz) whenever the selection changes. */
    private BiConsumer<Integer, Integer> selectionListener;

    // ── Filtered audio output ─────────────────────────────────────────────────

    /**
     * When non-null, receives filtered PCM audio chunks (16-bit LE mono, 48 kHz)
     * reflecting the current bandpass selection.  Called from the capture thread.
     */
    private volatile Consumer<byte[]> filteredOutputListener;

    /** Accumulate filtered float samples before converting and emitting. */
    private final float[] filteredOutBuf = new float[HOP_SIZE];
    private int filteredOutPos = 0;

    // ── Passband signal level ─────────────────────────────────────────────────

    /** Average dB level within the current passband, updated each FFT frame. */
    private volatile float passbandAvgDb = -100f;

    /** Threshold above which a signal is considered present in the passband. */
    private static final float SIGNAL_THRESHOLD_DB = -65f;

    // ── Constructor ───────────────────────────────────────────────────────────

    public SpectrumWaterfallPanel() {
        setPreferredSize(new Dimension(700, 250));
        setMinimumSize(new Dimension(300, 130));
        setBackground(Color.BLACK);
        setToolTipText("Drag centre line to move frequency · drag sides to adjust bandwidth");

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.getButton() != MouseEvent.BUTTON1) return;
                dragStartX = e.getX();
                int cx = freqToX(centerFreqHz, getWidth());

                if (Math.abs(e.getX() - cx) <= CENTER_GRAB_PX) {
                    // Centre grab zone: drag moves centre frequency, bandwidth unchanged
                    dragMode = DragMode.CENTER;
                } else if (e.getX() < cx) {
                    // Left of centre: drag expands/contracts bandwidth symmetrically
                    dragMode = DragMode.LOW_EDGE;
                } else {
                    // Right of centre: drag expands/contracts bandwidth symmetrically
                    dragMode = DragMode.HIGH_EDGE;
                }
                repaint();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (dragMode == DragMode.NONE || dragStartX < 0) return;

                if (Math.abs(e.getX() - dragStartX) <= 4) {
                    // Treat as a simple click: move centre to click position, keep bandwidth
                    centerFreqHz = xToFreq(dragStartX);
                } else if (dragMode != DragMode.CENTER) {
                    // Edge drag: enforce a minimum bandwidth on release
                    bandwidthHz = Math.max(bandwidthHz, 50);
                }

                dragMode   = DragMode.NONE;
                dragStartX = -1;
                recomputeFilter();
                updateCursor(e.getX());
                fireSelection();
                repaint();
            }
        });

        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (dragMode == DragMode.NONE) return;
                int freq = xToFreq(e.getX());

                switch (dragMode) {
                    case CENTER:
                        centerFreqHz = freq;
                        // bandwidth unchanged
                        break;

                    case LOW_EDGE:
                    case HIGH_EDGE: {
                        if (rxMode.isUpperSide()) {
                            // USB: passband extends right of centre; right drag sets width
                            bandwidthHz = Math.max(10, freq - centerFreqHz);
                        } else if (rxMode.isLowerSide()) {
                            // LSB: passband extends left of centre; left drag sets width
                            bandwidthHz = Math.max(10, centerFreqHz - freq);
                        } else {
                            // AM / DSB / CW: symmetric, both edges move together
                            int halfBw = Math.abs(freq - centerFreqHz);
                            bandwidthHz = Math.max(10, halfBw * 2);
                        }
                        break;
                    }

                    default: break;
                }
                repaint();
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                updateCursor(e.getX());
            }
        });

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                rebuildWaterfallImage();
                repaint();
            }
        });
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Feed raw PCM audio data to the spectrum/waterfall.
     *
     * <p>Expected format: 16-bit signed little-endian, {@value SAMPLE_RATE} Hz, mono.
     * This matches {@link org.qualsh.lb.digital.AudioRouter#FORMAT}.
     *
     * <p>Safe to call from any thread (typically the audio-capture thread).
     */
    public void feedPcm(byte[] pcm) {
        int nSamples = pcm.length / 2;
        FilterCoeffs f = filterCoeffs; // read volatile once per buffer
        for (int i = 0; i < nSamples; i++) {
            // Little-endian 16-bit → float in [−1, +1)
            int lo = pcm[i * 2]     & 0xFF;
            int hi = pcm[i * 2 + 1] & 0xFF;
            short s = (short) ((hi << 8) | lo);
            float raw = s / 32768f;

            if (f != null) {
                // Direct-Form II biquad bandpass (b1 = 0, so that term is omitted)
                double x0 = raw;
                double y0 = f.b0 * x0 + f.b2 * flt_x2
                          - f.a1 * flt_y1 - f.a2 * flt_y2;
                flt_x2 = flt_x1; flt_x1 = x0;
                flt_y2 = flt_y1; flt_y1 = y0;
                ringBuf[ringPos] = (float) y0;
            } else {
                ringBuf[ringPos] = raw;
            }

            // Accumulate filtered sample for audio output
            filteredOutBuf[filteredOutPos++] = ringBuf[ringPos];
            if (filteredOutPos >= filteredOutBuf.length) {
                emitFilteredAudio();
                filteredOutPos = 0;
            }

            ringPos = (ringPos + 1) % FFT_SIZE;
            if (++newSamplesCount >= HOP_SIZE) {
                newSamplesCount = 0;
                processFFT();
            }
        }
    }

    /**
     * Set the receive demodulation mode.
     * Updates the filter placement and passband display immediately.
     */
    public void setRxMode(RxMode mode) {
        this.rxMode = mode;
        recomputeFilter();
        fireSelection();
        repaint();
    }

    /** @return Currently active receive mode. */
    public RxMode getRxMode() { return rxMode; }

    /** Set the current frequency selection programmatically. */
    public void setSelection(int centerHz, int bwHz) {
        this.centerFreqHz = Math.max(0, Math.min((int) MAX_FREQ_HZ, centerHz));
        this.bandwidthHz  = Math.max(0, bwHz);
        recomputeFilter();
        repaint();
    }

    /** @return Currently selected centre frequency in Hz. */
    public int getCenterFreqHz() { return centerFreqHz; }

    /** @return Currently selected bandwidth in Hz. */
    public int getBandwidthHz() { return bandwidthHz; }

    /**
     * Register a listener to be notified whenever the user changes the
     * frequency selection.  Called with {@code (centreHz, bandwidthHz)}.
     */
    public void setSelectionListener(BiConsumer<Integer, Integer> listener) {
        this.selectionListener = listener;
    }

    /**
     * Register a listener that receives bandpass-filtered PCM audio.
     * The chunks match {@link org.qualsh.lb.digital.AudioRouter#FORMAT}
     * (16-bit LE mono 48 kHz).  Called from the audio-capture thread.
     * Pass {@code null} to disable.
     */
    public void setFilteredOutputListener(Consumer<byte[]> listener) {
        this.filteredOutputListener = listener;
    }

    /** @return Average dB level within the current passband (updated each FFT frame). */
    public float getPassbandAvgDb() { return passbandAvgDb; }

    /**
     * Set the dB range used for color-mapping the waterfall.
     *
     * @param minDb signals at or below this level map to the coldest color
     * @param maxDb signals at or above this level map to the hottest color
     */
    public void setDbRange(float minDb, float maxDb) {
        this.displayMinDb = minDb;
        this.displayMaxDb = maxDb;
    }

    // ── FFT processing ────────────────────────────────────────────────────────

    private void processFFT() {
        float[] re = new float[FFT_SIZE];
        float[] im = new float[FFT_SIZE];

        // Extract FFT_SIZE samples from the ring buffer (oldest first) and apply Hann window
        for (int i = 0; i < FFT_SIZE; i++) {
            int idx = (ringPos + i) % FFT_SIZE;
            re[i] = ringBuf[idx] * HANN[i];
            im[i] = 0f;
        }

        fft(re, im, FFT_SIZE);

        // Convert to dB magnitudes (positive-frequency bins only)
        int numBins = FFT_SIZE / 2;
        float[] db = new float[numBins];
        for (int i = 0; i < numBins; i++) {
            float mag = (float) Math.sqrt((double) re[i] * re[i] + (double) im[i] * im[i]) / FFT_SIZE;
            db[i] = (float) (20.0 * Math.log10(Math.max(mag, 1e-10)));
        }

        spectrumDb = db;

        // Compute average dB within the current passband for signal detection
        int pLo, pHi;
        if (rxMode.isUpperSide()) {
            pLo = centerFreqHz; pHi = centerFreqHz + bandwidthHz;
        } else if (rxMode.isLowerSide()) {
            pLo = centerFreqHz - bandwidthHz; pHi = centerFreqHz;
        } else {
            pLo = centerFreqHz - bandwidthHz / 2; pHi = centerFreqHz + bandwidthHz / 2;
        }
        pLo = Math.max(0, pLo); pHi = Math.min((int) MAX_FREQ_HZ, pHi);
        int binLo = (int) ((float) pLo / SAMPLE_RATE * FFT_SIZE);
        int binHi = (int) ((float) pHi / SAMPLE_RATE * FFT_SIZE);
        binLo = Math.max(0, Math.min(numBins - 1, binLo));
        binHi = Math.max(binLo, Math.min(numBins - 1, binHi));
        float dbSum = 0f; int dbCount = 0;
        for (int i = binLo; i <= binHi; i++) { dbSum += db[i]; dbCount++; }
        passbandAvgDb = (dbCount > 0) ? dbSum / dbCount : -100f;

        addWaterfallLine(db);
        SwingUtilities.invokeLater(this::repaint);
    }

    // ── Waterfall image ───────────────────────────────────────────────────────

    private void addWaterfallLine(float[] db) {
        synchronized (imgLock) {
            if (waterfallImg == null) return;
            int w = waterfallImg.getWidth();
            int h = waterfallImg.getHeight();

            // Shift entire image down by 1 pixel (newest line goes at the top)
            Graphics2D g = waterfallImg.createGraphics();
            g.copyArea(0, 0, w, h - 1, 0, 1);

            // Draw the new spectral line at row 0
            int numBins = FFT_SIZE / 2;
            for (int x = 0; x < w; x++) {
                float freq = (float) x / w * MAX_FREQ_HZ;
                int bin = Math.round(freq / SAMPLE_RATE * FFT_SIZE);
                bin = Math.min(bin, numBins - 1);

                float d = db[bin];
                int colorIdx = (int) ((d - displayMinDb) / (displayMaxDb - displayMinDb) * 255f);
                colorIdx = Math.max(0, Math.min(255, colorIdx));
                int rgb = PALETTE_RGB[colorIdx];
                waterfallImg.setRGB(x, 0, rgb);
            }
            g.dispose();
        }
    }

    private void rebuildWaterfallImage() {
        int w = getWidth();
        int h = getHeight() - SPECTRUM_HEIGHT - 18; // 18 px for freq scale
        if (w <= 0 || h <= 0) return;
        synchronized (imgLock) {
            waterfallImg = new BufferedImage(w, Math.max(h, 1), BufferedImage.TYPE_INT_RGB);
            Graphics2D g = waterfallImg.createGraphics();
            g.setColor(Color.BLACK);
            g.fillRect(0, 0, w, h);
            g.dispose();
        }
    }

    // ── Painting ──────────────────────────────────────────────────────────────

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            int w = getWidth();
            int h = getHeight();
            int scaleH = 18; // frequency scale strip at bottom

            // Black background
            g2.setColor(Color.BLACK);
            g2.fillRect(0, 0, w, h);

            // Spectrum strip
            drawSpectrum(g2, w, SPECTRUM_HEIGHT);

            // Waterfall image
            int waterfallY = SPECTRUM_HEIGHT;
            int waterfallH = h - SPECTRUM_HEIGHT - scaleH;
            synchronized (imgLock) {
                if (waterfallImg != null && waterfallH > 0) {
                    g2.drawImage(waterfallImg, 0, waterfallY, w, waterfallH, null);
                }
            }

            // Frequency scale
            drawFrequencyScale(g2, w, h, scaleH);

            // Selection overlay (on top of everything)
            drawSelection(g2, w, h, scaleH);

        } finally {
            g2.dispose();
        }
    }

    private void drawSpectrum(Graphics2D g, int w, int h) {
        float[] db = spectrumDb;
        if (db == null) {
            // Draw placeholder text
            g.setColor(new Color(60, 60, 60));
            g.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
            g.drawString("Waiting for audio…", 10, h / 2);
            return;
        }

        // Grid lines
        g.setColor(new Color(30, 30, 30));
        for (int level = -80; level <= 0; level += 20) {
            int y = dbToSpectrumY(level, h);
            g.drawLine(0, y, w, y);
        }

        // dB labels on grid
        g.setColor(new Color(80, 80, 80));
        g.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 9));
        for (int level = -80; level <= 0; level += 20) {
            int y = dbToSpectrumY(level, h);
            g.drawString(level + "dB", 2, y - 1);
        }

        // Filled spectrum
        int numBins = db.length;
        int[] xPts = new int[w + 2];
        int[] yPts = new int[w + 2];
        xPts[0] = 0; yPts[0] = h;
        for (int px = 0; px < w; px++) {
            float freq = (float) px / w * MAX_FREQ_HZ;
            int bin = Math.round(freq / SAMPLE_RATE * FFT_SIZE);
            bin = Math.min(bin, numBins - 1);
            xPts[px + 1] = px;
            yPts[px + 1] = dbToSpectrumY(db[bin], h);
        }
        xPts[w + 1] = w - 1; yPts[w + 1] = h;

        // Fill under spectrum
        g.setColor(new Color(0, 80, 30, 120));
        g.fillPolygon(xPts, yPts, w + 2);

        // Spectrum line
        g.setColor(new Color(0, 220, 80));
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        for (int px = 1; px < w; px++) {
            g.drawLine(xPts[px], yPts[px], xPts[px + 1], yPts[px + 1]);
        }
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_DEFAULT);
    }

    private int dbToSpectrumY(float db, int h) {
        float norm = (db - displayMinDb) / (displayMaxDb - displayMinDb);
        norm = Math.max(0f, Math.min(1f, norm));
        return (int) (h - norm * h);
    }

    private void drawFrequencyScale(Graphics2D g, int w, int h, int scaleH) {
        int y0 = h - scaleH;
        g.setColor(new Color(50, 50, 50));
        g.fillRect(0, y0, w, scaleH);

        g.setColor(new Color(140, 140, 140));
        g.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 10));

        int[] ticks = { 500, 1000, 1500, 2000, 2500, 3000, 3500, 4000 };
        for (int freq : ticks) {
            if (freq > MAX_FREQ_HZ) continue;
            int x = freqToX(freq, w);
            g.drawLine(x, y0, x, y0 + 4);
            String label = freq >= 1000 ? (freq / 1000) + "k" : freq + "";
            FontMetrics fm = g.getFontMetrics();
            g.drawString(label, x - fm.stringWidth(label) / 2, h - 2);
        }

        // 0 Hz label
        g.drawString("0", 2, h - 2);
    }

    private void drawSelection(Graphics2D g, int w, int h, int scaleH) {
        int cx     = freqToX(centerFreqHz, w);
        int bwPx   = (int) ((float) bandwidthHz / MAX_FREQ_HZ * w);
        int waterfallTop = SPECTRUM_HEIGHT;
        int waterfallBot = h - scaleH;

        // Determine passband pixel bounds based on RX mode
        int bandLeft, bandRight;
        if (rxMode.isUpperSide()) {
            bandLeft  = cx;
            bandRight = cx + bwPx;
        } else if (rxMode.isLowerSide()) {
            bandLeft  = cx - bwPx;
            bandRight = cx;
        } else {
            // AM / DSB / CW – symmetric
            int half  = bwPx / 2;
            bandLeft  = cx - half;
            bandRight = cx + half;
        }
        int bandW = bandRight - bandLeft;

        // Semi-transparent band overlay on spectrum
        g.setColor(new Color(255, 220, 0, 35));
        g.fillRect(bandLeft, 0, bandW, SPECTRUM_HEIGHT);

        // Semi-transparent band on waterfall
        g.setColor(new Color(255, 220, 0, 45));
        g.fillRect(bandLeft, waterfallTop, bandW, waterfallBot - waterfallTop);

        // Band edge lines on waterfall
        if (bandW > 0) {
            g.setColor(new Color(200, 180, 0, 160));
            g.drawLine(bandLeft,  waterfallTop, bandLeft,  waterfallBot);
            g.drawLine(bandRight, waterfallTop, bandRight, waterfallBot);
        }

        // Centre cursor line (full height)
        g.setColor(new Color(255, 230, 0, 220));
        g.drawLine(cx, 0, cx, waterfallBot);

        // Frequency label near the cursor
        String freqLabel = centerFreqHz + " Hz";
        g.setFont(new Font(Font.MONOSPACED, Font.BOLD, 10));
        FontMetrics fm = g.getFontMetrics();
        int labelX = Math.min(cx + 3, w - fm.stringWidth(freqLabel) - 2);
        g.setColor(new Color(0, 0, 0, 180));
        g.fillRect(labelX - 1, 4, fm.stringWidth(freqLabel) + 2, 13);
        g.setColor(new Color(255, 230, 0));
        g.drawString(freqLabel, labelX, 14);

        // Mode + bandwidth label – top-right corner of spectrum strip
        String bwLabel = rxMode.getLabel() + "  BW: " + bandwidthHz + " Hz";
        FontMetrics fmBw = g.getFontMetrics(); // same font (MONOSPACED BOLD 10)
        int bwLabelW = fmBw.stringWidth(bwLabel);
        int bwX = w - bwLabelW - 4;
        g.setColor(new Color(0, 0, 0, 180));
        g.fillRect(bwX - 2, 4, bwLabelW + 4, 13);
        g.setColor(new Color(180, 230, 255));
        g.drawString(bwLabel, bwX, 14);

        // Signal detection indicator – shown below the BW label when signal is present
        boolean signalPresent = passbandAvgDb > SIGNAL_THRESHOLD_DB;
        String sigLabel = signalPresent ? "\u25CF SIGNAL" : "\u25CB quiet";
        Color  sigColor = signalPresent ? new Color(0, 255, 120) : new Color(80, 80, 80);
        FontMetrics fmSig = g.getFontMetrics();
        int sigLabelW = fmSig.stringWidth(sigLabel);
        int sigX = w - sigLabelW - 4;
        g.setColor(new Color(0, 0, 0, 160));
        g.fillRect(sigX - 2, 19, sigLabelW + 4, 13);
        g.setColor(sigColor);
        g.drawString(sigLabel, sigX, 29);

        // When signal is present, brighten the band overlay on the spectrum
        if (signalPresent && bandW > 0) {
            g.setColor(new Color(0, 255, 120, 30));
            g.fillRect(bandLeft, 0, bandW, SPECTRUM_HEIGHT);
        }
    }

    // ── Cursor management ─────────────────────────────────────────────────────

    private void updateCursor(int mouseX) {
        if (bandwidthHz > 0) {
            int cx = freqToX(centerFreqHz, getWidth());
            if (Math.abs(mouseX - cx) <= CENTER_GRAB_PX) {
                setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
            } else {
                setCursor(Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR));
            }
        } else {
            setCursor(Cursor.getDefaultCursor());
        }
    }

    // ── Coordinate conversions ────────────────────────────────────────────────

    private int freqToX(int freqHz, int w) {
        return (int) ((float) freqHz / MAX_FREQ_HZ * w);
    }

    private int xToFreq(int x) {
        int w = getWidth();
        if (w == 0) return 0;
        return (int) Math.max(0, Math.min(MAX_FREQ_HZ, (float) x / w * MAX_FREQ_HZ));
    }

    // ── Listener ──────────────────────────────────────────────────────────────

    private void fireSelection() {
        BiConsumer<Integer, Integer> l = selectionListener;
        if (l != null) l.accept(centerFreqHz, bandwidthHz);
    }

    // ── Bandpass filter ───────────────────────────────────────────────────────

    /**
     * Recompute 2nd-order Butterworth bandpass biquad coefficients from the
     * current {@link #centerFreqHz} / {@link #bandwidthHz} selection.
     *
     * <p>Uses the Audio-EQ-Cookbook bandpass formula (constant 0 dB peak gain):
     * <pre>
     *   ω₀ = 2π·fc/Fs,  α = sin(ω₀)/(2Q),  Q = fc/bw
     *   b0 =  α,  b1 = 0,  b2 = −α
     *   a0 = 1+α, a1 = −2cos(ω₀), a2 = 1−α
     * </pre>
     * Called from the EDT; the new coefficients are published via a volatile
     * field so the audio-capture thread picks them up without a lock.
     */
    private void recomputeFilter() {
        if (bandwidthHz < 50) {
            filterCoeffs = null;
            return;
        }

        // Shift the filter centre so the passband covers the correct side
        int filterCenterHz;
        if (rxMode.isUpperSide()) {
            filterCenterHz = centerFreqHz + bandwidthHz / 2;
        } else if (rxMode.isLowerSide()) {
            filterCenterHz = centerFreqHz - bandwidthHz / 2;
        } else {
            filterCenterHz = centerFreqHz; // symmetric modes
        }

        if (filterCenterHz <= 0) {
            filterCoeffs = null;
            return;
        }

        double Q     = Math.max(0.5, (double) filterCenterHz / bandwidthHz);
        double omega = 2.0 * Math.PI * filterCenterHz / SAMPLE_RATE;
        double sinW  = Math.sin(omega);
        double cosW  = Math.cos(omega);
        double alpha = sinW / (2.0 * Q);
        double a0inv = 1.0 / (1.0 + alpha);
        filterCoeffs = new FilterCoeffs(
                alpha * a0inv,          // b0
               -alpha * a0inv,          // b2  (b1 = 0)
               -2.0 * cosW * a0inv,     // a1
                (1.0 - alpha) * a0inv   // a2
        );
        // Reset delay lines to avoid a transient on the spectrum display
        flt_x1 = flt_x2 = flt_y1 = flt_y2 = 0.0;
    }

    // ── Filtered audio emission ───────────────────────────────────────────────

    /**
     * Convert {@link #filteredOutBuf} to 16-bit PCM and fire
     * {@link #filteredOutputListener}.  Called from the capture thread.
     */
    private void emitFilteredAudio() {
        Consumer<byte[]> l = filteredOutputListener;
        if (l == null) return;
        byte[] out = new byte[filteredOutBuf.length * 2];
        for (int i = 0; i < filteredOutBuf.length; i++) {
            short s = (short) Math.max(-32768, Math.min(32767,
                    (int) (filteredOutBuf[i] * 32767f)));
            out[i * 2]     = (byte) (s & 0xFF);
            out[i * 2 + 1] = (byte) ((s >> 8) & 0xFF);
        }
        l.accept(out);
    }

    // ── Waterfall rebuild ─────────────────────────────────────────────────────

    // (public so DigitalModesWindow can call it after first setVisible)

    // ── Static helpers ────────────────────────────────────────────────────────

    /**
     * In-place Cooley–Tukey DIT FFT.
     *
     * @param re real parts (length must be a power of 2)
     * @param im imaginary parts (same length as re)
     * @param n  transform size (power of 2)
     */
    private static void fft(float[] re, float[] im, int n) {
        // Bit-reversal permutation
        for (int i = 1, j = 0; i < n; i++) {
            int bit = n >> 1;
            for (; (j & bit) != 0; bit >>= 1) j ^= bit;
            j ^= bit;
            if (i < j) {
                float t = re[i]; re[i] = re[j]; re[j] = t;
                t = im[i]; im[i] = im[j]; im[j] = t;
            }
        }

        // Butterfly stages
        for (int len = 2; len <= n; len <<= 1) {
            double ang  = -2.0 * Math.PI / len;
            float wRe = (float) Math.cos(ang);
            float wIm = (float) Math.sin(ang);
            for (int i = 0; i < n; i += len) {
                float curRe = 1f, curIm = 0f;
                for (int k = 0, half = len / 2; k < half; k++) {
                    float uRe = re[i + k];
                    float uIm = im[i + k];
                    float vRe = re[i + k + half] * curRe - im[i + k + half] * curIm;
                    float vIm = re[i + k + half] * curIm + im[i + k + half] * curRe;
                    re[i + k]        = uRe + vRe;
                    im[i + k]        = uIm + vIm;
                    re[i + k + half] = uRe - vRe;
                    im[i + k + half] = uIm - vIm;
                    float newCurRe = curRe * wRe - curIm * wIm;
                    curIm = curRe * wIm + curIm * wRe;
                    curRe = newCurRe;
                }
            }
        }
    }

    private static float[] buildHannWindow(int n) {
        float[] w = new float[n];
        for (int i = 0; i < n; i++) {
            w[i] = 0.5f * (1f - (float) Math.cos(2.0 * Math.PI * i / (n - 1)));
        }
        return w;
    }

    /**
     * Build a 256-entry packed-RGB heat-map palette:
     * black → dark-blue → cyan → green → yellow → red.
     */
    private static int[] buildPaletteRGB() {
        int[] p = new int[256];
        for (int i = 0; i < 256; i++) {
            float t = i / 255f;
            int r, g, b;
            if (t < 0.25f) {
                float u = t / 0.25f;
                r = 0; g = 0; b = (int) (u * 180);
            } else if (t < 0.5f) {
                float u = (t - 0.25f) / 0.25f;
                r = 0; g = (int) (u * 255); b = (int) ((1f - u) * 180);
            } else if (t < 0.75f) {
                float u = (t - 0.5f) / 0.25f;
                r = (int) (u * 255); g = 255; b = 0;
            } else {
                float u = (t - 0.75f) / 0.25f;
                r = 255; g = (int) ((1f - u) * 255); b = 0;
            }
            p[i] = (Math.min(r, 255) << 16) | (Math.min(g, 255) << 8) | Math.min(b, 255);
        }
        return p;
    }
}
