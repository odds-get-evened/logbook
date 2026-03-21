package org.qualsh.lb.digital;

/**
 * Protocol-agnostic status update from a digital-mode backend
 * (WSJT-X, Fldigi, JS8Call, …).
 */
public record StatusEvent(
        double  dialFreqHz,
        String  mode,
        boolean transmitting
) {
    public double dialFreqKhz() { return dialFreqHz / 1000.0; }
}
