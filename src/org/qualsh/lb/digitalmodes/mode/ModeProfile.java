package org.qualsh.lb.digitalmodes.mode;

import org.qualsh.lb.digital.DigitalMode;

/**
 * Looks up the recommended signal bandwidth for a given digital mode.
 *
 * <p>Each digital mode occupies a known slice of the radio spectrum — for example FT8
 * occupies about 50 Hz while RTTY uses 250 Hz. This class stores those default widths
 * so the spectrum display can automatically set its bandwidth markers whenever you switch
 * modes. Call {@link #getProfile(DigitalMode)} to get the profile for any mode, then use
 * {@link #getDefaultBandwidthHz()} to read the recommended bandwidth.
 *
 * @author Logbook Development Team
 * @version 1.0
 */
public class ModeProfile {

    private static final double FALLBACK_BANDWIDTH_HZ = 500.0;

    private final double defaultBandwidthHz;

    private ModeProfile(double defaultBandwidthHz) {
        this.defaultBandwidthHz = defaultBandwidthHz;
    }

    /**
     * Returns the signal profile for the given digital mode, including its recommended
     * spectrum bandwidth.
     *
     * <p>If the mode already has a bandwidth configured, that value is used. Otherwise a
     * built-in preset is applied for common modes such as FT8, WSPR, PSK31, RTTY, Olivia,
     * and others. Unknown modes fall back to a sensible {@value #FALLBACK_BANDWIDTH_HZ} Hz
     * default.
     *
     * @param mode the digital mode whose profile is requested; must not be {@code null}
     * @return the profile for the given mode; never {@code null}
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
     * Returns the recommended bandwidth in hertz for this mode profile.
     *
     * <p>Use this value to set the bandwidth markers on the spectrum display so they
     * match the expected signal width for the selected mode.
     *
     * @return bandwidth in hertz; always greater than {@code 0.0}
     */
    public double getDefaultBandwidthHz() {
        return defaultBandwidthHz;
    }
}
