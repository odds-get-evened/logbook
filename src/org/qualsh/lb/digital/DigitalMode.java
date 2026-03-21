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
 *   <li>The T/R period in seconds (used for PTT timing; 0 = continuous/async)</li>
 *   <li>The backend class that decodes this mode</li>
 * </ul>
 */
public enum DigitalMode {

    // ── WSJT-X / JS8Call modes ────────────────────────────────────────────────

    FT8(
        "FT8",
        "USB",
        14_074_000L,
        1_500,
        15,
        BackendType.WSJTX
    ),
    FT4(
        "FT4",
        "USB",
        14_080_000L,
        1_500,
        7,
        BackendType.WSJTX
    ),
    JS8CALL(
        "JS8Call",
        "USB",
        14_078_000L,
        1_500,
        15,
        BackendType.WSJTX
    ),

    // ── Fldigi modes (PSK, RTTY, etc.) ───────────────────────────────────────

    PSK31(
        "PSK31",
        "USB",
        14_070_000L,
        1_500,
        0,
        BackendType.FLDIGI
    ),
    PSK63(
        "PSK63",
        "USB",
        14_070_000L,
        1_500,
        0,
        BackendType.FLDIGI
    ),
    PSK125(
        "PSK125",
        "USB",
        14_070_000L,
        1_500,
        0,
        BackendType.FLDIGI
    ),
    RTTY(
        "RTTY",
        "USB",
        14_080_000L,
        1_500,
        0,
        BackendType.FLDIGI
    ),
    OLIVIA(
        "Olivia",
        "USB",
        14_072_000L,
        1_500,
        0,
        BackendType.FLDIGI
    ),
    MFSK16(
        "MFSK-16",
        "USB",
        14_080_000L,
        1_500,
        0,
        BackendType.FLDIGI
    );

    // ── Backend type ──────────────────────────────────────────────────────────

    /** Which external decoder application handles this mode. */
    public enum BackendType { WSJTX, FLDIGI }

    private final String      label;
    /** CAT mode string (Yaesu/Kenwood ASCII or rigctld mode name). */
    private final String      catMode;
    /** Default dial frequency in Hz. */
    private final long        defaultFreqHz;
    /** Nominal audio tone centre in Hz (informational / waterfall reference). */
    private final int         audioToneCentreHz;
    /** T/R cycle period in seconds (0 = continuous / no fixed period). */
    private final int         trPeriodSecs;
    /** Backend application required to decode this mode. */
    private final BackendType backendType;

    DigitalMode(String label, String catMode, long defaultFreqHz,
                int audioToneCentreHz, int trPeriodSecs, BackendType backendType) {
        this.label             = label;
        this.catMode           = catMode;
        this.defaultFreqHz     = defaultFreqHz;
        this.audioToneCentreHz = audioToneCentreHz;
        this.trPeriodSecs      = trPeriodSecs;
        this.backendType       = backendType;
    }

    public String      getLabel()             { return label; }
    public String      getCatMode()           { return catMode; }
    public long        getDefaultFreqHz()     { return defaultFreqHz; }
    public double      getDefaultFreqKhz()    { return defaultFreqHz / 1000.0; }
    public int         getAudioToneCentreHz() { return audioToneCentreHz; }
    public int         getTrPeriodSecs()      { return trPeriodSecs; }
    public BackendType getBackendType()       { return backendType; }

    @Override
    public String toString() { return label; }
}
