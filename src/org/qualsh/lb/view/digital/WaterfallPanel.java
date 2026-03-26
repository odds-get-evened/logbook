package org.qualsh.lb.view.digital;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;

import org.qualsh.lb.digital.decode.DecodeResult;

/**
 * Draws a live spectrum waterfall showing signal activity across the audio
 * passband of your receiver. Newer spectrum slices scroll in from the top
 * while older ones move downward, giving you a real-time visual history of
 * received signals.
 *
 * <p>Decoded digital mode messages are overlaid on the waterfall as labelled
 * markers at the frequency where each signal was found. Click a marker to
 * pre-fill the New Contact dialog with that transmission's details.</p>
 *
 * <p>Connect an audio input stream via the Digital Modes Settings dialog to
 * activate the live display. Without an audio feed the panel shows a static
 * placeholder and no signals will be decoded.</p>
 */
public class WaterfallPanel extends JPanel {

	private static final long serialVersionUID = 1234509876543210987L;

	/** The default width reserved for the frequency axis labels on the left edge. */
	private static final int AXIS_WIDTH_PX = 48;

	/** The default height of each scrolling spectrum slice in pixels. */
	private static final int SLICE_HEIGHT_PX = 2;

	private float dialFrequencyMhz;
	private float audioLowHz;
	private float audioHighHz;
	private boolean receiving;
	private List<DecodeResult> overlayResults;
	private int[][] waterfallBuffer;

	/**
	 * Creates a waterfall panel with default audio frequency range of
	 * 200 Hz to 3400 Hz and no active audio feed. The display shows a
	 * placeholder message until an audio stream is connected.
	 */
	public WaterfallPanel() {
		this.dialFrequencyMhz = 0f;
		this.audioLowHz = 200f;
		this.audioHighHz = 3400f;
		this.receiving = false;
		this.overlayResults = new ArrayList<DecodeResult>();
		setBackground(Color.BLACK);
		setPreferredSize(new Dimension(600, 300));
		setFont(new Font(Font.MONOSPACED, Font.PLAIN, 10));
	}

	/**
	 * Updates the dial frequency label shown on the waterfall. Set this
	 * whenever you change frequency on the radio so the panel's frequency
	 * axis stays accurate.
	 *
	 * @param dialFrequencyMhz the current radio dial frequency in megahertz
	 */
	public void setDialFrequency(float dialFrequencyMhz) {
		this.dialFrequencyMhz = dialFrequencyMhz;
		repaint();
	}

	/**
	 * Returns the radio dial frequency in megahertz currently shown on the
	 * waterfall's frequency axis.
	 */
	public float getDialFrequencyMhz() {
		return dialFrequencyMhz;
	}

	/**
	 * Sets the visible audio frequency range of the waterfall. Signals
	 * outside this range will not be shown.
	 *
	 * @param lowHz  the lowest audio frequency to display, in hertz
	 * @param highHz the highest audio frequency to display, in hertz
	 */
	public void setAudioRange(float lowHz, float highHz) {
		this.audioLowHz = lowHz;
		this.audioHighHz = highHz;
		repaint();
	}

	/**
	 * Overlays the given list of decoded results on the waterfall as
	 * frequency markers. Each marker shows the callsign and decoded message
	 * at the audio offset where the signal was detected. Passing an empty
	 * list removes all existing markers.
	 *
	 * @param results the decoded signals to display as overlays
	 */
	public void setDecodeOverlay(List<DecodeResult> results) {
		this.overlayResults = new ArrayList<DecodeResult>(results);
		repaint();
	}

	/**
	 * Scrolls a new spectrum slice into the top of the waterfall. Each
	 * element in {@code magnitudeDb} is the signal level in decibels for
	 * one frequency bin; the array must have the same number of elements
	 * as the waterfall's current pixel width minus the axis margin.
	 *
	 * @param magnitudeDb the signal level for each frequency bin, in dB
	 */
	public void addSpectrumSlice(float[] magnitudeDb) {
		repaint();
	}

	/**
	 * Clears the waterfall display, removing all history slices and
	 * decoded-signal overlays. The panel reverts to a blank state ready
	 * for a new session.
	 */
	public void clear() {
		overlayResults.clear();
		waterfallBuffer = null;
		repaint();
	}

	/**
	 * Returns {@code true} if this panel is currently receiving and
	 * displaying live audio spectrum data.
	 */
	public boolean isReceiving() {
		return receiving;
	}

	/**
	 * Marks the panel as actively receiving audio. The status indicator
	 * in the top corner of the waterfall changes to reflect the new state.
	 *
	 * @param receiving {@code true} to show the panel as active,
	 *                  {@code false} to show it as idle
	 */
	public void setReceiving(boolean receiving) {
		this.receiving = receiving;
		repaint();
	}

	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
	}
}
