package org.qualsh.lb.digitalmodes.spectrum;

import com.github.psambit9791.jdsp.transform.DiscreteFourier;
import org.qualsh.lb.digitalmodes.audio.AudioBuffer;
import org.qualsh.lb.digitalmodes.audio.AudioBuffer.AudioBufferListener;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Displays a scrolling waterfall view of the loaded audio.
 *
 * <p>The waterfall shows frequency on the horizontal axis and time on the vertical axis, with
 * the newest row always at the top and older rows scrolling downward. Pixel colour encodes
 * signal strength, ranging from black (no signal) through dark blue and cyan to yellow and
 * white (strongest signal). The display updates approximately 10 times per second.
 *
 * <p>Connect the panel to the application's audio using {@link #setBuffer(AudioBuffer)}.
 * The waterfall will automatically scroll and update whenever the audio changes.
 *
 * @author Logbook Development Team
 * @version 1.0
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
     * Creates a new waterfall display panel. Call {@link #setBuffer(AudioBuffer)} to connect
     * audio data so the waterfall has something to show.
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
     * Connects this waterfall display to the given audio buffer.
     *
     * <p>The waterfall immediately begins scrolling new rows based on audio from the new buffer.
     * Passing {@code null} disconnects the display and stops the waterfall from updating.
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
    }

    /**
     * Called automatically when the connected audio buffer is loaded or cleared.
     *
     * <p>The waterfall picks up the new audio on the next refresh tick. You do not need to
     * call this method directly.
     *
     * @param buffer the buffer whose content changed; never {@code null}
     */
    @Override
    public void onBufferChanged(AudioBuffer buffer) {
        // The timer-driven scrollAndUpdate() picks up new data automatically;
        // no immediate action is required here.
    }

    /**
     * Scrolls the waterfall image down and adds a new row at the top based on the current
     * audio spectrum.
     *
     * <p>This method is called automatically on each refresh tick. If no audio is loaded,
     * it returns immediately without modifying the waterfall image.
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
     * Draws the waterfall image on screen.
     *
     * <p>This method is called automatically by the display framework.
     *
     * @param g the graphics context provided by the display framework; must not be {@code null}
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
     * Resizes the waterfall image to the specified dimensions, preserving as much existing
     * history as fits. Called automatically when the panel is resized.
     *
     * @param width  the new image width in pixels; must be greater than {@code 0}
     * @param height the new image height in pixels; must be greater than {@code 0}
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
     * Sets the position and size of this panel, resizing the waterfall image to match.
     *
     * @param x      the X coordinate of the panel's top-left corner
     * @param y      the Y coordinate of the panel's top-left corner
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
     * @return a {@link Dimension} of 800 × 200 pixels; never {@code null}
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
