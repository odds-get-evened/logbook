package org.qualsh.lb.digitalmodes.spectrum;

import com.github.psambit9791.jdsp.transform.DiscreteFourier;
import org.qualsh.lb.digitalmodes.audio.AudioBuffer;
import org.qualsh.lb.digitalmodes.audio.AudioBuffer.AudioBufferListener;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * A scrolling waterfall display panel for the Digital Modes feature.
 *
 * <p>{@code WaterfallPanel} renders a two-dimensional heat-map image where the
 * X axis represents frequency, the Y axis represents time (newest row at the
 * top, older rows scrolling downward), and the colour of each pixel encodes
 * signal strength using a graduated heat-colour scale ranging from black
 * (no signal) through dark blue, cyan, and yellow to white (peak signal).
 *
 * <p>The panel reads audio data from a connected {@link AudioBuffer},
 * recomputes an FFT magnitude spectrum on every refresh tick (approximately
 * 10 Hz), and scrolls the waterfall image down by one pixel before painting
 * the freshly computed row at the top. The image is automatically resized to
 * match the panel dimensions whenever the panel is laid out.
 *
 * <p>Typical usage:
 * <pre>
 *   WaterfallPanel waterfall = new WaterfallPanel();
 *   waterfall.setBuffer(myAudioBuffer);
 *   parentPanel.add(waterfall);
 * </pre>
 *
 * <p>When no buffer is connected, or the buffer is empty, the waterfall image
 * retains its last painted content and no new rows are added.
 */
public class WaterfallPanel extends JPanel implements AudioBufferListener {

    private AudioBuffer buffer;
    private BufferedImage waterfallImage;
    private Timer refreshTimer;

    private static final int   REFRESH_RATE_MS  = 100;
    private static final int   PREFERRED_HEIGHT  = 200;
    private static final Color BACKGROUND_COLOR  = Color.BLACK;
    private static final Color LABEL_COLOR       = Color.LIGHT_GRAY;

    // -------------------------------------------------------------------------
    // Construction
    // -------------------------------------------------------------------------

    /**
     * Creates a new {@code WaterfallPanel} with a preferred size of
     * 800 × 200 pixels.
     *
     * <p>A blank black waterfall image is allocated immediately and a refresh
     * timer is started so that the display begins scrolling as soon as a
     * buffer is connected via {@link #setBuffer(AudioBuffer)}.
     */
    public WaterfallPanel() {
        setPreferredSize(new Dimension(800, PREFERRED_HEIGHT));
        setBackground(BACKGROUND_COLOR);
        waterfallImage = new BufferedImage(800, PREFERRED_HEIGHT, BufferedImage.TYPE_INT_RGB);
        refreshTimer = new Timer(REFRESH_RATE_MS, e -> {
            scrollAndUpdate();
            repaint();
        });
        refreshTimer.start();
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Connects this panel to the given {@link AudioBuffer}.
     *
     * <p>Any previously connected buffer is unsubscribed first. This panel
     * registers itself as an {@link AudioBufferListener} on the new buffer so
     * it is notified whenever audio data changes. Passing {@code null}
     * disconnects the panel from any buffer, causing the waterfall to stop
     * updating.
     *
     * @param buffer the audio buffer to visualise, or {@code null} to
     *               disconnect
     */
    public void setBuffer(AudioBuffer buffer) {
        if (this.buffer != null) {
            this.buffer.removeListener(this);
        }
        this.buffer = buffer;
        if (buffer != null) {
            buffer.addListener(this);
        }
    }

    /**
     * Called automatically when the connected {@link AudioBuffer} is loaded or
     * cleared.
     *
     * <p>This notification arrives whenever new audio data is available. The
     * refresh timer will incorporate the updated data at its next tick and
     * schedule a repaint on the Event Dispatch Thread.
     *
     * @param buffer the buffer whose content changed; never {@code null}
     */
    @Override
    public void onBufferChanged(AudioBuffer buffer) {
        // The timer-driven scrollAndUpdate() picks up new data automatically;
        // no immediate action is required here.
    }

    /**
     * Scrolls the waterfall image down by one pixel and paints a new top row
     * derived from the current FFT magnitude spectrum.
     *
     * <p>If no buffer is connected, or the buffer is empty, this method
     * returns immediately without modifying the waterfall image.
     *
     * <p>The FFT computation follows the same PCM-to-double conversion used by
     * {@link FFTPanel}: raw 16-bit signed little-endian PCM bytes are
     * normalised to the range −1.0 to 1.0, zero-padded to the next power of
     * two, and transformed via {@link DiscreteFourier}. The resulting magnitude
     * bins are then mapped to pixel colours using
     * {@link #magnitudeToColor(double)} and written into the image's top row.
     */
    public void scrollAndUpdate() {
        if (buffer == null || buffer.isEmpty()) {
            return;
        }

        double[] magnitudes;
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

            DiscreteFourier dft = new DiscreteFourier(signal);
            dft.transform();
            magnitudes = dft.getMagnitude(false);
        } catch (Exception e) {
            return;
        }

        if (magnitudes.length == 0) {
            return;
        }

        int imgW = waterfallImage.getWidth();
        int imgH = waterfallImage.getHeight();

        // Scroll the existing image down by one pixel.
        // Iterate from bottom to top to avoid overwriting source rows before
        // they have been copied (source and destination are the same image).
        int[] rowBuf = new int[imgW];
        for (int y = imgH - 1; y > 0; y--) {
            waterfallImage.getRGB(0, y - 1, imgW, 1, rowBuf, 0, imgW);
            waterfallImage.setRGB(0, y, imgW, 1, rowBuf, 0, imgW);
        }

        // Find peak magnitude for normalisation; guard against all-zero input.
        double maxMag = 1e-10;
        for (double m : magnitudes) {
            if (m > maxMag) maxMag = m;
        }

        // Paint the new top row.
        int n = magnitudes.length;
        for (int x = 0; x < imgW; x++) {
            // Map pixel column to the nearest magnitude bin.
            int binIdx = (int) ((double) x / (imgW - 1) * (n - 1));
            binIdx = Math.max(0, Math.min(binIdx, n - 1));
            double normalised = magnitudes[binIdx] / maxMag;
            Color c = magnitudeToColor(normalised);
            waterfallImage.setRGB(x, 0, c.getRGB());
        }
    }

    /**
     * Paints the waterfall image scaled to fill the panel, then draws a small
     * "Waterfall" label in the top-left corner.
     *
     * @param g the {@code Graphics} context provided by Swing; must not be
     *          {@code null}
     */
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;

        int w = getWidth();
        int h = getHeight();

        // Draw the waterfall image scaled to fill the panel.
        g2.drawImage(waterfallImage, 0, 0, w, h, null);

        // "Waterfall" label in the top-left corner.
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setColor(LABEL_COLOR);
        g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 10f));
        g2.drawString("Waterfall", 4, g2.getFontMetrics().getAscent() + 2);
    }

    /**
     * Resizes the internal waterfall image to the specified dimensions,
     * copying as much of the existing image content as fits into the new
     * image.
     *
     * <p>This method is called automatically from the {@link #setBounds}
     * override whenever the panel is resized so that the waterfall history is
     * preserved across layout changes.
     *
     * @param width  the new image width in pixels; must be greater than zero
     * @param height the new image height in pixels; must be greater than zero
     */
    public void resizeImage(int width, int height) {
        if (width <= 0 || height <= 0) {
            return;
        }
        BufferedImage newImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = newImage.createGraphics();
        g2.drawImage(waterfallImage, 0, 0, null);
        g2.dispose();
        waterfallImage = newImage;
    }

    /**
     * Sets the bounds of this panel and resizes the waterfall image to match
     * the new panel dimensions.
     *
     * @param x      the new X coordinate of the panel's top-left corner
     * @param y      the new Y coordinate of the panel's top-left corner
     * @param width  the new width of the panel in pixels
     * @param height the new height of the panel in pixels
     */
    @Override
    public void setBounds(int x, int y, int width, int height) {
        super.setBounds(x, y, width, height);
        if (width > 0 && height > 0) {
            resizeImage(width, height);
        }
    }

    /**
     * Returns the preferred display size of this panel.
     *
     * @return a {@link Dimension} of 800 × 200 pixels
     */
    @Override
    public Dimension getPreferredSize() {
        return new Dimension(800, PREFERRED_HEIGHT);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Maps a normalised magnitude value in the range 0.0–1.0 to a heat colour
     * using a five-stop gradient:
     * <ul>
     *   <li>0.0 → black</li>
     *   <li>0.2 → dark blue</li>
     *   <li>0.5 → cyan</li>
     *   <li>0.8 → yellow</li>
     *   <li>1.0 → white</li>
     * </ul>
     * Values outside 0.0–1.0 are clamped before interpolation.
     *
     * @param normalizedMagnitude the signal strength normalised to 0.0–1.0
     * @return the interpolated {@link Color} for that magnitude
     */
    private Color magnitudeToColor(double normalizedMagnitude) {
        double t = Math.max(0.0, Math.min(1.0, normalizedMagnitude));

        // Gradient stops: {position, R, G, B}
        double[][] stops = {
            {0.0,  0,   0,   0},    // black
            {0.2,  0,   0,   139},  // dark blue
            {0.5,  0,   255, 255},  // cyan
            {0.8,  255, 255, 0},    // yellow
            {1.0,  255, 255, 255},  // white
        };

        // Find the two surrounding stops and interpolate linearly.
        for (int i = 0; i < stops.length - 1; i++) {
            double lo = stops[i][0];
            double hi = stops[i + 1][0];
            if (t >= lo && t <= hi) {
                double f = (t - lo) / (hi - lo);
                int r = (int) Math.round(stops[i][1] + f * (stops[i + 1][1] - stops[i][1]));
                int g = (int) Math.round(stops[i][2] + f * (stops[i + 1][2] - stops[i][2]));
                int b = (int) Math.round(stops[i][3] + f * (stops[i + 1][3] - stops[i][3]));
                return new Color(r, g, b);
            }
        }

        // Fallback (should not be reached for clamped input).
        return Color.WHITE;
    }
}
