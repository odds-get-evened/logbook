package org.qualsh.lb.digital;

/**
 * Digital modes supported by the built-in decoder.
 *
 * <p>Currently only BPSK31 (PSK31) is supported.  BPSK31 is decoded
 * in-process by {@link Bpsk31Decoder} – no external application is required.
 */
public enum DigitalMode {

    BPSK31(
        "BPSK31 (PSK31)",
        "USB",
        14_070_000L,
        1_000,
        0
    );

    private final String label;
    /** CAT mode string sent to the radio via rigctld / serial. */
    private final String catMode;
    /** Default dial frequency in Hz (20 m band). */
    private final long   defaultFreqHz;
    /** Nominal audio carrier frequency in Hz. */
    private final int    audioCarrierHz;
    /** T/R cycle period in seconds (0 = continuous). */
    private final int    trPeriodSecs;

    DigitalMode(String label, String catMode, long defaultFreqHz,
                int audioCarrierHz, int trPeriodSecs) {
        this.label          = label;
        this.catMode        = catMode;
        this.defaultFreqHz  = defaultFreqHz;
        this.audioCarrierHz = audioCarrierHz;
        this.trPeriodSecs   = trPeriodSecs;
    }

    public String getLabel()          { return label; }
    public String getCatMode()        { return catMode; }
    public long   getDefaultFreqHz()  { return defaultFreqHz; }
    public double getDefaultFreqKhz() { return defaultFreqHz / 1000.0; }
    public int    getAudioCarrierHz() { return audioCarrierHz; }
    public int    getTrPeriodSecs()   { return trPeriodSecs; }

    @Override
    public String toString() { return label; }
}
