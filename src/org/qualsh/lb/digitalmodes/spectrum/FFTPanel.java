package org.qualsh.lb.digitalmodes.spectrum;

import com.github.psambit9791.jdsp.transform.DiscreteFourier;
import org.qualsh.lb.digitalmodes.audio.AudioBuffer;
import org.qualsh.lb.digitalmodes.audio.AudioBuffer.AudioBufferListener;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;

/**
 * Displays a live frequency spectrum graph of the loaded audio.
 *
 * <p>The spectrum panel shows signal strength on the vertical axis and frequency in Hz on the
 * horizontal axis, with a green trace on a dark background. It updates approximately 10 times
 * per second. When no audio is loaded, the panel shows "No Signal".
 *
 * <p>Connect the panel to the application's audio using {@link #setBuffer(AudioBuffer)}.
 * The display will automatically refresh whenever the audio changes.
 *
 * @author Logbook Development Team
 * @version 1.0
 */
public class FFTPanel extends JPanel implements AudioBufferListener {

    private AudioBuffer buffer;
    private double[] magnitudes;
    private Timer refreshTimer;

    private static final int    REFRESH_RATE_MS  = 100;
    private static final Color  BACKGROUND_COLOR = Color.BLACK;
    private static final Color  TRACE_COLOR      = new Color(0, 200, 0);
    private static final Color  GRID_COLOR       = new Color(40, 40, 40);
    private static final Color  LABEL_COLOR      = Color.LIGHT_GRAY;
    private static final int    PADDING          = 30;

    // Cached when updateMagnitudes() runs; used by paintComponent() for Hz labels.
    private float cachedSampleRate = 0f;
    private int   cachedFFTSize    = 0;

    // -------------------------------------------------------------------------
    // Construction
    // -------------------------------------------------------------------------

    /**
     * Creates a new spectrum display panel. Call {@link #setBuffer(AudioBuffer)} to connect
     * audio data so the display has something to show.
     */
    public FFTPanel() {
        setPreferredSize(new Dimension(800, 150));
        setBackground(BACKGROUND_COLOR);
        magnitudes = new double[0];
        refreshTimer = new Timer(REFRESH_RATE_MS, e -> {
            updateMagnitudes();
            repaint();
        });
        refreshTimer.start();
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Connects this spectrum display to the given audio buffer.
     *
     * <p>The display immediately begins showing the spectrum of audio from the new buffer.
     * Passing {@code null} disconnects the display and shows "No Signal".
     *
     * @param buffer the audio buffer to display, or {@code null} to disconnect
     */
    public void setBuffer(AudioBuffer buffer) {
        if (this.buffer != null) {
            this.buffer.removeListener(this);
        }
        this.buffer = buffer;
        if (buffer != null) {
            buffer.addListener(this);
        }
        updateMagnitudes();
    }

    /**
     * Called automatically when the connected audio buffer is loaded or cleared.
     *
     * <p>The spectrum display immediately recalculates and shows the new audio on the
     * next refresh. You do not need to call this method directly.
     *
     * @param buffer the buffer whose content changed; never {@code null}
     */
    @Override
    public void onBufferChanged(AudioBuffer buffer) {
        updateMagnitudes();
    }

    /**
     * Recalculates the frequency spectrum from the current audio buffer.
     *
     * <p>If the buffer is empty or absent the display will show "No Signal" on the next repaint.
     * This method is called automatically on each refresh tick; you can also call it manually
     * to force an immediate update.
     */
    public void updateMagnitudes() {
        if (buffer == null || buffer.isEmpty()) {
            magnitudes = new double[0];
            return;
        }
        try {
            byte[] raw = buffer.getSamples();
            int numSamples = raw.length / 2; // 16-bit mono: 2 bytes per sample

            // Convert 16-bit signed little-endian PCM to normalised doubles.
            double[] pcm = new double[numSamples];
            for (int i = 0; i < numSamples; i++) {
                int lo     = raw[i * 2]     & 0xFF; // unsigned low byte
                int hi     = raw[i * 2 + 1];        // signed high byte
                int sample = (hi << 8) | lo;
                pcm[i] = sample / 32768.0;
            }

            // Pad to next power of two (required for efficient FFT).
            int fftSize = 1;
            while (fftSize < numSamples) fftSize <<= 1;

            double[] signal = new double[fftSize];
            System.arraycopy(pcm, 0, signal, 0, numSamples);
            // Remaining entries are implicitly zero (zero-padding).

            DiscreteFourier dft = new DiscreteFourier(signal);
            dft.transform();
            magnitudes = dft.getMagnitude(false);

            cachedSampleRate = buffer.getSampleRate();
            cachedFFTSize    = fftSize;
        } catch (Exception e) {
            magnitudes = new double[0];
        }
    }

    /**
     * Draws the frequency spectrum graph on screen.
     *
     * <p>This method is called automatically by the display framework. When audio is loaded
     * it draws the green spectrum trace with frequency labels. When no audio is present it
     * displays "No Signal" instead.
     *
     * @param g the graphics context provided by the display framework; must not be {@code null}
     */
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,      RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();

        // ------------------------------------------------------------------ //
        // No-signal state                                                     //
        // ------------------------------------------------------------------ //
        if (magnitudes.length == 0) {
            g2.setColor(LABEL_COLOR);
            g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 12f));
            FontMetrics fm = g2.getFontMetrics();
            String text = "No Signal";
            g2.drawString(text,
                    (w - fm.stringWidth(text)) / 2,
                    (h + fm.getAscent()) / 2);
            return;
        }

        // ------------------------------------------------------------------ //
        // Draw area within padding                                            //
        // ------------------------------------------------------------------ //
        int drawX = PADDING;
        int drawY = PADDING;
        int drawW = w - 2 * PADDING;
        int drawH = h - 2 * PADDING;

        if (drawW <= 0 || drawH <= 0) return;

        // ------------------------------------------------------------------ //
        // Grid                                                                //
        // ------------------------------------------------------------------ //
        g2.setColor(GRID_COLOR);

        // Vertical lines every ~50 pixels.
        for (int x = drawX; x <= drawX + drawW; x += 50) {
            g2.drawLine(x, drawY, x, drawY + drawH);
        }

        // Horizontal lines at 25 %, 50 %, 75 %.
        for (int pct : new int[]{25, 50, 75}) {
            int y = drawY + drawH - (drawH * pct / 100);
            g2.drawLine(drawX, y, drawX + drawW, y);
        }

        // ------------------------------------------------------------------ //
        // Magnitude trace                                                     //
        // ------------------------------------------------------------------ //
        int n = magnitudes.length;

        // Find peak for normalisation; guard against all-zero input.
        double maxMag = 1e-10;
        for (double m : magnitudes) {
            if (m > maxMag) maxMag = m;
        }

        // Build (x, y) pairs for the trace; close the polygon at the baseline.
        int[] polyX = new int[n + 2];
        int[] polyY = new int[n + 2];
        for (int i = 0; i < n; i++) {
            polyX[i] = drawX + (int) ((double) i / (n - 1) * drawW);
            polyY[i] = drawY + drawH - (int) (magnitudes[i] / maxMag * drawH);
        }
        polyX[n]     = drawX + drawW;
        polyY[n]     = drawY + drawH;
        polyX[n + 1] = drawX;
        polyY[n + 1] = drawY + drawH;

        // Filled area under the curve (semi-transparent green, alpha 80).
        g2.setColor(new Color(
                TRACE_COLOR.getRed(),
                TRACE_COLOR.getGreen(),
                TRACE_COLOR.getBlue(),
                80));
        g2.fillPolygon(polyX, polyY, n + 2);

        // Solid line trace on top.
        g2.setColor(TRACE_COLOR);
        for (int i = 0; i < n - 1; i++) {
            g2.drawLine(polyX[i], polyY[i], polyX[i + 1], polyY[i + 1]);
        }

        // ------------------------------------------------------------------ //
        // Axis labels                                                         //
        // ------------------------------------------------------------------ //
        g2.setColor(LABEL_COLOR);
        Font smallFont = g2.getFont().deriveFont(Font.PLAIN, 10f);
        g2.setFont(smallFont);
        FontMetrics fm = g2.getFontMetrics();

        // "Frequency (Hz)" centred along the bottom edge.
        String xLabel = "Frequency (Hz)";
        g2.drawString(xLabel,
                drawX + (drawW - fm.stringWidth(xLabel)) / 2,
                h - 5);

        // "Level" rotated 90° counter-clockwise on the left side.
        String yLabel = "Level";
        AffineTransform origTransform = g2.getTransform();
        g2.translate(10, (double) h / 2 + (double) fm.stringWidth(yLabel) / 2);
        g2.rotate(-Math.PI / 2);
        g2.drawString(yLabel, 0, 0);
        g2.setTransform(origTransform);

        // ------------------------------------------------------------------ //
        // Frequency tick labels                                               //
        // ------------------------------------------------------------------ //
        if (cachedSampleRate > 0 && cachedFFTSize > 0 && n > 1) {
            float hzPerBin = cachedSampleRate / cachedFFTSize;
            int numTicks = Math.max(1, drawW / 80);

            for (int t = 0; t <= numTicks; t++) {
                int binIdx = (int) ((double) t / numTicks * (n - 1));
                float hz   = binIdx * hzPerBin;
                int tickX  = drawX + (int) ((double) binIdx / (n - 1) * drawW);
                int tickY  = drawY + drawH;

                // Short tick mark.
                g2.setColor(GRID_COLOR);
                g2.drawLine(tickX, tickY, tickX, tickY + 4);

                // Hz label — use "k" suffix for values ≥ 1000 Hz.
                g2.setColor(LABEL_COLOR);
                String hzStr = hz >= 1000f
                        ? String.format("%.1fk", hz / 1000f)
                        : String.format("%d", (int) hz);
                g2.drawString(hzStr,
                        tickX - fm.stringWidth(hzStr) / 2,
                        tickY + 4 + fm.getAscent());
            }
        }
    }

    /**
     * Returns the preferred display size of this panel.
     *
     * @return a {@link Dimension} of 800 × 150 pixels; never {@code null}
     */
    @Override
    public Dimension getPreferredSize() {
        return new Dimension(800, 150);
    }
}
