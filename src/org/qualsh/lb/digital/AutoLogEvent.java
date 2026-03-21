package org.qualsh.lb.digital;

import java.time.LocalDate;

/**
 * Protocol-agnostic QSO-logged event from a digital-mode backend
 * (WSJT-X, Fldigi, JS8Call, …).
 */
public record AutoLogEvent(
        LocalDate dateOn,
        int       timeOnSecOfDay,
        String    dxCall,
        String    dxGrid,
        double    dialFreqKhz,
        String    mode,
        String    rstSent,
        String    rstRcvd,
        String    txPower,
        String    comments,
        String    name,
        String    myCall,
        String    myGrid,
        String    propMode,
        /** Name of the backend that logged the QSO, e.g. "WSJT-X", "JS8Call", "Fldigi". */
        String    source
) {}
