package org.qualsh.lb.digitalmodes.spectrum;

import org.qualsh.lb.digital.DigitalMode;
import org.qualsh.lb.digitalmodes.mode.ModeProfile;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;

/**
 * An interactive overlay that lets you select a centre frequency and bandwidth on the
 * spectrum display.
 *
 * <p>The selector draws a yellow vertical line for your chosen centre frequency and two orange
 * dashed lines showing the signal bandwidth on either side. Click anywhere on the spectrum to
 * move the centre frequency there, or drag either orange bandwidth line to widen or narrow the
 * selected passband. Register a {@link FrequencySelectorListener} to be notified when you make
 * changes.
 *
 * @author Logbook Development Team
 * @version 1.0
 */
public class FrequencySelector extends JPanel {

    // -------------------------------------------------------------------------
    // Constants
    // -------------------------------------------------------------------------

    /** Pixels within which a mouse click counts as targeting a bandwidth bar. */
    private static final int MARKER_CLICK_TOLERANCE_PX = 6;

    private static final Color CENTER_COLOR    = Color.YELLOW;
    private static final Color BANDWIDTH_COLOR = new Color(255, 165, 0, 180);
    private static final Color FILL_COLOR      = new Color(255, 255, 255, 20);

    // -------------------------------------------------------------------------
    // State
    // -------------------------------------------------------------------------

    private double centerFrequencyHz = 1500.0;
    private double bandwidthHz       = 500.0;
    private double sampleRate        = 8000.0;
    private int    fftSize           = 1024;

    private boolean draggingLeft  = false;
    private boolean draggingRight = false;

    private final List<FrequencySelectorListener> listeners = new ArrayList<>();

    // -------------------------------------------------------------------------
    // Listener interface
    // -------------------------------------------------------------------------

    /**
     * Notified when you adjust the centre frequency or bandwidth on a {@link FrequencySelector}.
     *
     * @author Logbook Development Team
     * @version 1.0
     */
    public interface FrequencySelectorListener {

        /**
         * Called when you click to move the centre-frequency marker to a new position.
         *
         * @param centerHz the new centre frequency in hertz
         */
        void onCenterFrequencyChanged(double centerHz);

        /**
         * Called when you drag a bandwidth boundary line to change the selected passband width.
         *
         * @param bandwidthHz the new bandwidth in hertz
         */
        void onBandwidthChanged(double bandwidthHz);
    }

    // -------------------------------------------------------------------------
    // Construction
    // -------------------------------------------------------------------------

    /**
     * Creates a new frequency selector with a default centre frequency of {@code 1500} Hz
     * and bandwidth of {@code 500} Hz. The panel is transparent so the spectrum display
     * beneath it shows through.
     */
    public FrequencySelector() {
        setOpaque(false);
        setPreferredSize(new Dimension(800, 150));

        MouseAdapter mouseHandler = new MouseAdapter() {

            @Override
            public void mouseClicked(MouseEvent e) {
                int x = e.getX();
                int leftPx  = hzToPixel(centerFrequencyHz - bandwidthHz / 2.0);
                int rightPx = hzToPixel(centerFrequencyHz + bandwidthHz / 2.0);

                boolean nearLeft  = Math.abs(x - leftPx)  <= MARKER_CLICK_TOLERANCE_PX;
                boolean nearRight = Math.abs(x - rightPx) <= MARKER_CLICK_TOLERANCE_PX;

                if (!nearLeft && !nearRight) {
                    double hz = pixelToHz(x);
                    setCenterFrequency(hz);
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
                int x = e.getX();
                int leftPx  = hzToPixel(centerFrequencyHz - bandwidthHz / 2.0);
                int rightPx = hzToPixel(centerFrequencyHz + bandwidthHz / 2.0);

                if (Math.abs(x - leftPx) <= MARKER_CLICK_TOLERANCE_PX) {
                    draggingLeft = true;
                } else if (Math.abs(x - rightPx) <= MARKER_CLICK_TOLERANCE_PX) {
                    draggingRight = true;
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                draggingLeft  = false;
                draggingRight = false;
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (draggingLeft) {
                    double leftHz = pixelToHz(e.getX());
                    double newBw  = centerFrequencyHz - leftHz;
                    if (newBw < 50.0) newBw = 50.0;
                    bandwidthHz = newBw;
                    notifyBandwidthChanged();
                    repaint();
                } else if (draggingRight) {
                    double rightHz = pixelToHz(e.getX());
                    double newBw   = rightHz - centerFrequencyHz;
                    if (newBw < 50.0) newBw = 50.0;
                    bandwidthHz = newBw;
                    notifyBandwidthChanged();
                    repaint();
                }
            }
        };

        addMouseListener(mouseHandler);
        addMouseMotionListener(mouseHandler);
    }

    // -------------------------------------------------------------------------
    // Painting
    // -------------------------------------------------------------------------

    /**
     * Draws the centre-frequency and bandwidth markers on screen.
     *
     * <p>This method is called automatically by the display framework.
     *
     * @param g the graphics context provided by the display framework
     */
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,      RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int h       = getHeight();
        int centerPx = hzToPixel(centerFrequencyHz);
        int leftPx   = hzToPixel(centerFrequencyHz - bandwidthHz / 2.0);
        int rightPx  = hzToPixel(centerFrequencyHz + bandwidthHz / 2.0);

        // Subtle fill between boundary lines.
        g2.setColor(FILL_COLOR);
        g2.fillRect(leftPx, 0, rightPx - leftPx, h);

        // Dashed stroke for bandwidth boundary lines.
        Stroke dashedStroke = new BasicStroke(
                1f,
                BasicStroke.CAP_BUTT,
                BasicStroke.JOIN_MITER,
                10f,
                new float[]{4f, 4f},
                0f);
        g2.setStroke(dashedStroke);
        g2.setColor(BANDWIDTH_COLOR);
        g2.drawLine(leftPx,  0, leftPx,  h);
        g2.drawLine(rightPx, 0, rightPx, h);

        // Solid 2 px centre-frequency line.
        g2.setStroke(new BasicStroke(2f));
        g2.setColor(CENTER_COLOR);
        g2.drawLine(centerPx, 0, centerPx, h);

        // Frequency label above the centre line.
        g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 10f));
        FontMetrics fm  = g2.getFontMetrics();
        String label    = String.format("%.0f Hz", centerFrequencyHz);
        int labelX      = centerPx - fm.stringWidth(label) / 2;
        int labelY      = fm.getAscent() + 2;
        g2.drawString(label, labelX, labelY);
    }

    // -------------------------------------------------------------------------
    // Public API — frequency and bandwidth
    // -------------------------------------------------------------------------

    /**
     * Moves the centre-frequency marker to the specified frequency.
     *
     * <p>The value is clamped to the valid frequency range. Registered listeners are notified
     * of the change.
     *
     * @param hz the desired centre frequency in hertz
     */
    public void setCenterFrequency(double hz) {
        centerFrequencyHz = Math.max(0.0, Math.min(hz, sampleRate / 2.0));
        repaint();
        notifyFrequencyChanged();
    }

    /**
     * Sets the bandwidth shown between the two orange boundary markers.
     *
     * <p>Values below {@code 50} Hz are raised to {@code 50} Hz. Registered listeners are
     * notified of the change.
     *
     * @param hz the desired bandwidth in hertz
     */
    public void setBandwidth(double hz) {
        bandwidthHz = Math.max(50.0, hz);
        repaint();
        notifyBandwidthChanged();
    }

    /**
     * Adjusts the bandwidth markers to match the typical signal width of the given digital mode.
     *
     * <p>For example, selecting FT8 sets the bandwidth to about 50 Hz; selecting RTTY sets it
     * to about 250 Hz. Use this when you switch modes to keep the display accurate.
     *
     * @param mode the digital mode to apply; must not be {@code null}
     */
    public void applyModeProfile(DigitalMode mode) {
        ModeProfile profile = ModeProfile.getProfile(mode);
        setBandwidth(profile.getDefaultBandwidthHz());
    }

    /**
     * Returns the current centre frequency shown by the yellow marker line.
     *
     * @return centre frequency in hertz; never negative
     */
    public double getCenterFrequency() {
        return centerFrequencyHz;
    }

    /**
     * Returns the current bandwidth shown between the two orange boundary lines.
     *
     * @return bandwidth in hertz; always at least {@code 50.0}
     */
    public double getBandwidth() {
        return bandwidthHz;
    }

    // -------------------------------------------------------------------------
    // Public API — display configuration
    // -------------------------------------------------------------------------

    /**
     * Sets the audio sample rate used to convert frequencies to screen positions.
     *
     * <p>This should match the sample rate of the connected audio buffer. Changing this value
     * updates the display immediately.
     *
     * @param sampleRate the sample rate in hertz; must be greater than {@code 0}
     */
    public void setSampleRate(double sampleRate) {
        this.sampleRate = sampleRate;
        repaint();
    }

    /**
     * Sets the FFT window size so marker positions align with the spectrum display.
     *
     * <p>This should match the FFT size used by the connected {@link FFTPanel}. Changing this
     * value updates the display immediately.
     *
     * @param fftSize the number of FFT bins; must be greater than {@code 0}
     */
    public void setFftSize(int fftSize) {
        this.fftSize = fftSize;
        repaint();
    }

    // -------------------------------------------------------------------------
    // Public API — listener management
    // -------------------------------------------------------------------------

    /**
     * Registers a listener to be notified when you change the centre frequency or bandwidth.
     *
     * @param listener the listener to add; ignored if {@code null}
     */
    public void addListener(FrequencySelectorListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    /**
     * Removes a previously registered listener. Does nothing if the listener was not registered.
     *
     * @param listener the listener to remove; ignored if {@code null}
     */
    public void removeListener(FrequencySelectorListener listener) {
        listeners.remove(listener);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Converts a frequency in hertz to an X pixel position within the panel.
     *
     * <p>The full panel width maps the range {@code [0, sampleRate / 2]}.
     *
     * @param hz the frequency in hertz
     * @return the corresponding X pixel coordinate
     */
    private int hzToPixel(double hz) {
        int w = getWidth();
        if (w == 0) w = getPreferredSize().width;
        return (int) (hz / (sampleRate / 2.0) * w);
    }

    /**
     * Converts an X pixel position within the panel to a frequency in hertz.
     *
     * <p>This is the inverse of {@link #hzToPixel(double)}.
     *
     * @param px the X pixel coordinate
     * @return the corresponding frequency in hertz
     */
    private double pixelToHz(int px) {
        int w = getWidth();
        if (w == 0) w = getPreferredSize().width;
        return (double) px / w * (sampleRate / 2.0);
    }

    /** Notifies all registered listeners that the centre frequency has changed. */
    private void notifyFrequencyChanged() {
        for (FrequencySelectorListener l : listeners) {
            l.onCenterFrequencyChanged(centerFrequencyHz);
        }
    }

    /** Notifies all registered listeners that the bandwidth has changed. */
    private void notifyBandwidthChanged() {
        for (FrequencySelectorListener l : listeners) {
            l.onBandwidthChanged(bandwidthHz);
        }
    }
}
