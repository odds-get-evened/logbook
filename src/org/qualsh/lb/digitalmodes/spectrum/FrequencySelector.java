package org.qualsh.lb.digitalmodes.spectrum;

import org.qualsh.lb.digital.DigitalMode;
import org.qualsh.lb.digitalmodes.mode.ModeProfile;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;

/**
 * A transparent overlay panel that renders interactive frequency and bandwidth
 * markers on top of an {@link FFTPanel}.
 *
 * <p>{@code FrequencySelector} draws three vertical lines:
 * <ul>
 *   <li>A solid <strong>yellow centre-frequency line</strong> that the
 *       operator can reposition by clicking anywhere on the panel.</li>
 *   <li>Two dashed <strong>orange bandwidth boundary lines</strong> (left and
 *       right) that indicate the occupied signal bandwidth. Either boundary
 *       can be dragged horizontally to resize the bandwidth window
 *       symmetrically about the centre frequency.</li>
 * </ul>
 * A very subtle semi-transparent fill is drawn between the two boundary lines
 * to highlight the selected passband.
 *
 * <p>Typical usage — place this panel as an overlay on top of an
 * {@code FFTPanel} using a {@link javax.swing.JLayeredPane}:
 * <pre>
 *   FFTPanel fftPanel = new FFTPanel();
 *   FrequencySelector selector = new FrequencySelector();
 *
 *   JLayeredPane layered = new JLayeredPane();
 *   layered.add(fftPanel,  JLayeredPane.DEFAULT_LAYER);
 *   layered.add(selector,  JLayeredPane.PALETTE_LAYER);
 * </pre>
 *
 * <p>Register a {@link FrequencySelectorListener} to be notified whenever the
 * operator changes the centre frequency or bandwidth.
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
     * Receives notifications when the operator adjusts the centre frequency or
     * bandwidth on a {@link FrequencySelector}.
     */
    public interface FrequencySelectorListener {

        /**
         * Called when the centre-frequency marker has been moved.
         *
         * @param centerHz the new centre frequency in hertz
         */
        void onCenterFrequencyChanged(double centerHz);

        /**
         * Called when either bandwidth boundary has been dragged, changing the
         * width of the passband selection.
         *
         * @param bandwidthHz the new occupied bandwidth in hertz
         */
        void onBandwidthChanged(double bandwidthHz);
    }

    // -------------------------------------------------------------------------
    // Construction
    // -------------------------------------------------------------------------

    /**
     * Creates a new {@code FrequencySelector} with default settings:
     * centre frequency 1500 Hz, bandwidth 500 Hz, sample rate 8000 Hz, FFT
     * size 1024.
     *
     * <p>The panel is transparent ({@code opaque = false}) so the
     * {@link FFTPanel} beneath it shows through. Mouse listeners are attached
     * automatically; no further configuration is required before adding the
     * panel to a component hierarchy.
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
     * Paints the frequency and bandwidth markers.
     *
     * <p>The following elements are drawn:
     * <ul>
     *   <li>A very subtle white filled rectangle between the left and right
     *       bandwidth boundary pixels.</li>
     *   <li>A dashed orange vertical line at the left bandwidth boundary.</li>
     *   <li>A dashed orange vertical line at the right bandwidth boundary.</li>
     *   <li>A solid 2 px yellow vertical line at the centre frequency.</li>
     *   <li>A small frequency label (e.g. {@code "1500 Hz"}) drawn just above
     *       the centre line.</li>
     * </ul>
     *
     * @param g the {@code Graphics} context provided by Swing
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
     * <p>The value is clamped to the range {@code [0, sampleRate / 2]}. The
     * panel is repainted and all registered listeners are notified.
     *
     * @param hz the desired centre frequency in hertz
     */
    public void setCenterFrequency(double hz) {
        centerFrequencyHz = Math.max(0.0, Math.min(hz, sampleRate / 2.0));
        repaint();
        notifyFrequencyChanged();
    }

    /**
     * Sets the occupied bandwidth shown between the two boundary markers.
     *
     * <p>The value is clamped to a minimum of 50 Hz. The panel is repainted
     * and all registered listeners are notified.
     *
     * @param hz the desired bandwidth in hertz; values below 50 are raised to 50
     */
    public void setBandwidth(double hz) {
        bandwidthHz = Math.max(50.0, hz);
        repaint();
        notifyBandwidthChanged();
    }

    /**
     * Configures the bandwidth markers to match the typical signal footprint of
     * the supplied {@link DigitalMode}.
     *
     * <p>The bandwidth is obtained from {@link ModeProfile#getProfile(DigitalMode)}
     * and applied via {@link #setBandwidth(double)}, which clamps the value,
     * triggers a repaint, and notifies listeners.
     *
     * @param mode the digital mode whose default bandwidth should be applied;
     *             must not be {@code null}
     */
    public void applyModeProfile(DigitalMode mode) {
        ModeProfile profile = ModeProfile.getProfile(mode);
        setBandwidth(profile.getDefaultBandwidthHz());
    }

    /**
     * Returns the current centre frequency shown by the yellow marker line.
     *
     * @return centre frequency in hertz
     */
    public double getCenterFrequency() {
        return centerFrequencyHz;
    }

    /**
     * Returns the current bandwidth shown between the two orange boundary lines.
     *
     * @return occupied bandwidth in hertz
     */
    public double getBandwidth() {
        return bandwidthHz;
    }

    // -------------------------------------------------------------------------
    // Public API — display configuration
    // -------------------------------------------------------------------------

    /**
     * Sets the audio sample rate used to map frequencies to pixel positions.
     *
     * <p>The displayable frequency range is {@code [0, sampleRate / 2]}
     * (Nyquist). Changing this value forces a repaint.
     *
     * @param sampleRate the sample rate in hertz; must be greater than zero
     */
    public void setSampleRate(double sampleRate) {
        this.sampleRate = sampleRate;
        repaint();
    }

    /**
     * Sets the FFT window size used to map frequencies to pixel positions.
     *
     * <p>This should match the FFT size used by the {@link FFTPanel} so that
     * the marker lines align with the displayed spectrum bins. Changing this
     * value forces a repaint.
     *
     * @param fftSize the number of FFT bins; must be greater than zero
     */
    public void setFftSize(int fftSize) {
        this.fftSize = fftSize;
        repaint();
    }

    // -------------------------------------------------------------------------
    // Public API — listener management
    // -------------------------------------------------------------------------

    /**
     * Registers a listener to be notified when the centre frequency or
     * bandwidth changes.
     *
     * <p>Adding the same listener instance more than once results in duplicate
     * notifications. Adding {@code null} is silently ignored.
     *
     * @param listener the listener to add; ignored if {@code null}
     */
    public void addListener(FrequencySelectorListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    /**
     * Removes a previously registered listener.
     *
     * <p>If the listener was not registered this method does nothing.
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
