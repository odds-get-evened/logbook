package org.qualsh.lb.digitalmodes.mode;

import org.qualsh.lb.digital.DigitalMode;

/**
 * Provides default signal parameters for a given {@link DigitalMode}.
 *
 * <p>Call {@link #getProfile(DigitalMode)} to obtain a {@code ModeProfile}
 * containing the recommended settings for that mode. Currently this exposes
 * the default occupied bandwidth, which {@code FrequencySelector} uses to
 * initialise its bandwidth markers whenever the operator switches mode.
 *
 * <p>When a {@code DigitalMode} already carries a non-zero {@code bandwidthHz}
 * value that value is used directly. A small set of well-known modes are
 * given explicit defaults so that newly created or imported modes still produce
 * sensible initial selections even before the operator has configured them.
 */
public class ModeProfile {

    private static final double FALLBACK_BANDWIDTH_HZ = 500.0;

    private final double defaultBandwidthHz;

    private ModeProfile(double defaultBandwidthHz) {
        this.defaultBandwidthHz = defaultBandwidthHz;
    }

    /**
     * Returns a {@code ModeProfile} containing the recommended default
     * parameters for the supplied {@link DigitalMode}.
     *
     * <p>The bandwidth is determined in the following order:
     * <ol>
     *   <li>If the mode's own {@link DigitalMode#getBandwidthHz()} is greater
     *       than zero that value is used.</li>
     *   <li>If the mode's abbreviation matches a built-in preset (FT8, PSK31,
     *       RTTY, JS8, Olivia, WSPR, JT65, etc.) the preset bandwidth is
     *       used.</li>
     *   <li>Otherwise {@value #FALLBACK_BANDWIDTH_HZ} Hz is used as a safe
     *       general-purpose default.</li>
     * </ol>
     *
     * @param mode the digital mode whose profile is requested; must not be
     *             {@code null}
     * @return a new {@code ModeProfile} for the given mode; never {@code null}
     */
    public static ModeProfile getProfile(DigitalMode mode) {
        // Prefer the mode's own stored bandwidth when it has been set.
        if (mode.getBandwidthHz() > 0) {
            return new ModeProfile(mode.getBandwidthHz());
        }

        // Fall back to well-known presets keyed by abbreviation (case-insensitive).
        String abbrev = mode.getAbbreviation() == null ? "" : mode.getAbbreviation().toUpperCase();
        double bw;
        switch (abbrev) {
            case "FT8":    bw =   50.0; break;
            case "FT4":    bw =   90.0; break;
            case "JT65":   bw =  178.0; break;
            case "JT9":    bw =   16.0; break;
            case "WSPR":   bw =    6.0; break;
            case "PSK31":  bw =   31.0; break;
            case "PSK63":  bw =   63.0; break;
            case "RTTY":   bw =  250.0; break;
            case "JS8":    bw =   50.0; break;
            case "OLIVIA": bw =  500.0; break;
            case "MFSK16": bw =  316.0; break;
            case "MFSK32": bw =  632.0; break;
            case "THOR16": bw =  316.0; break;
            case "DominoEX": bw = 316.0; break;
            default:       bw = FALLBACK_BANDWIDTH_HZ; break;
        }
        return new ModeProfile(bw);
    }

    /**
     * Returns the default occupied bandwidth in hertz for this mode profile.
     *
     * <p>Pass this value to
     * {@link org.qualsh.lb.digitalmodes.spectrum.FrequencySelector#setBandwidth(double)}
     * to snap the bandwidth markers to the mode's expected signal footprint.
     *
     * @return bandwidth in hertz; always greater than or equal to 1.0
     */
    public double getDefaultBandwidthHz() {
        return defaultBandwidthHz;
    }
}
