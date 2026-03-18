package org.qualsh.lb.digital;

/**
 * Supported digital modes with their typical defaults.
 *
 * <p>Each mode carries:
 * <ul>
 *   <li>A display label</li>
 *   <li>The radio mode string sent via CAT (USB-D / DIGIU varies by radio)</li>
 *   <li>The typical dial frequency for the 20 m band (Hz)</li>
 *   <li>The audio sub-band centre used by the mode (Hz) – informational</li>
 *   <li>The T/R period in seconds (used for PTT timing)</li>
 * </ul>
 */
public enum DigitalMode {

    FT8(
        "FT8",
        "USB",
        14_074_000L,
        1_500,
        15
    ),
    FT4(
        "FT4",
        "USB",
        14_080_000L,
        1_500,
        7
    ),
    JS8CALL(
        "JS8Call",
        "USB",
        14_078_000L,
        1_500,
        15
    );

    private final String label;
    /** CAT mode string (Yaesu/Kenwood ASCII or rigctld mode name). */
    private final String catMode;
    /** Default dial frequency in Hz. */
    private final long defaultFreqHz;
    /** Nominal audio tone centre in Hz (informational / waterfall reference). */
    private final int audioToneCentreHz;
    /** T/R cycle period in seconds. */
    private final int trPeriodSecs;

    DigitalMode(String label, String catMode, long defaultFreqHz,
                int audioToneCentreHz, int trPeriodSecs) {
        this.label             = label;
        this.catMode           = catMode;
        this.defaultFreqHz     = defaultFreqHz;
        this.audioToneCentreHz = audioToneCentreHz;
        this.trPeriodSecs      = trPeriodSecs;
    }

    public String getLabel()             { return label; }
    public String getCatMode()           { return catMode; }
    public long   getDefaultFreqHz()     { return defaultFreqHz; }
    public double getDefaultFreqKhz()    { return defaultFreqHz / 1000.0; }
    public int    getAudioToneCentreHz() { return audioToneCentreHz; }
    public int    getTrPeriodSecs()      { return trPeriodSecs; }

    @Override
    public String toString() { return label; }
}
