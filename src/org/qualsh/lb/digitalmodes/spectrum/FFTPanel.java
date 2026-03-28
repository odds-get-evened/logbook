package org.qualsh.lb.digitalmodes.spectrum;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;

/**
 * Displays a live frequency spectrum graph of the loaded audio.
 *
 * <p>The spectrum panel shows signal strength on the vertical axis and frequency in Hz on the
 * horizontal axis, with a green trace on a dark background.  When no data has been received
 * the panel shows "No Signal".
 *
 * <p>This panel is a passive display: it does not perform any FFT computation itself.
 * All spectrum data is pushed in from the outside via {@link #setMagnitudes(double[], float, int)},
 * which must be called on the Event Dispatch Thread (typically via
 * {@link SwingUtilities#invokeLater(Runnable)} from the {@code DspConsumerThread}).
 *
 * @author Logbook Development Team
 * @version 1.0
 */
public class FFTPanel extends JPanel {

    private double[] magnitudes;

    private static final Color  BACKGROUND_COLOR = Color.BLACK;
    private static final Color  TRACE_COLOR      = new Color(0, 200, 0);
    private static final Color  GRID_COLOR       = new Color(40, 40, 40);
    private static final Color  LABEL_COLOR      = Color.LIGHT_GRAY;
    private static final int    PADDING          = 30;

    // Supplied by setMagnitudes(); used by paintComponent() for Hz axis labels.
    private float cachedSampleRate = 0f;
    private int   cachedFFTSize    = 0;

    // -------------------------------------------------------------------------
    // Construction
    // -------------------------------------------------------------------------

    /**
     * Creates a new spectrum display panel.
     *
     * <p>The panel shows "No Signal" until the first call to
     * {@link #setMagnitudes(double[], float, int)}.
     */
    public FFTPanel() {
        setPreferredSize(new Dimension(800, 150));
        setBackground(BACKGROUND_COLOR);
        magnitudes = new double[0];
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Updates the displayed spectrum with new pre-computed FFT magnitudes.
     *
     * <p>Must be called on the Event Dispatch Thread.  Pass an empty or {@code null}
     * array to return the panel to its "No Signal" state.
     *
     * @param magnitudes the FFT magnitude array (half-spectrum); {@code null} is treated
     *                   as an empty array
     * @param sampleRate the sample rate of the source audio in Hz, used for frequency labels
     * @param fftSize    the FFT window size used to produce {@code magnitudes}, used for
     *                   frequency labels
     */
    public void setMagnitudes(double[] magnitudes, float sampleRate, int fftSize) {
        this.magnitudes      = (magnitudes != null) ? magnitudes : new double[0];
        this.cachedSampleRate = sampleRate;
        this.cachedFFTSize    = fftSize;
        repaint();
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
