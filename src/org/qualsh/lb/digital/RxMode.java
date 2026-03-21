package org.qualsh.lb.digital;

/**
 * Receive demodulation mode for the audio filter and waterfall selection display.
 *
 * <p>The mode controls both where the passband highlight is drawn on the waterfall
 * and where the audio bandpass filter is centred relative to the cursor:
 * <ul>
 *   <li><b>USB</b> – passband from cursor to cursor+BW (upper sideband)</li>
 *   <li><b>LSB</b> – passband from cursor−BW to cursor (lower sideband)</li>
 *   <li><b>AM / DSB</b> – passband symmetric around cursor (cursor±BW/2)</li>
 *   <li><b>CW</b>  – same symmetric shape as AM, typically narrow</li>
 * </ul>
 */
public enum RxMode {

    USB("USB"),
    LSB("LSB"),
    AM("AM"),
    DSB("DSB"),
    CW("CW");

    private final String label;

    RxMode(String label) {
        this.label = label;
    }

    public String getLabel() { return label; }

    @Override
    public String toString() { return label; }

    /** Passband extends above (to the right of) the centre cursor. */
    public boolean isUpperSide() { return this == USB; }

    /** Passband extends below (to the left of) the centre cursor. */
    public boolean isLowerSide() { return this == LSB; }

    /** Passband is symmetric around the centre cursor. */
    public boolean isSymmetric() { return this == AM || this == DSB || this == CW; }
}
