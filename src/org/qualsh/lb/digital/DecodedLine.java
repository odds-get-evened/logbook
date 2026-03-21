package org.qualsh.lb.digital;

import java.time.LocalTime;

/**
 * A single decoded line from a digital-mode backend
 * (WSJT-X decode, Fldigi RX text, JS8Call frame, …).
 *
 * <p>Fields that are not meaningful for a given backend may be null or
 * zero (e.g. Fldigi does not report per-line SNR or DT).
 */
public record DecodedLine(
        LocalTime time,
        String    call,
        String    grid,
        Integer   snr,
        Double    dt,
        Double    freqHz,
        String    message
) {}
