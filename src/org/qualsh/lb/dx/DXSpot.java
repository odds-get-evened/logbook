package org.qualsh.lb.dx;

/**
 * Represents a single DX cluster spot.
 */
public class DXSpot {

    private final String time;
    private final double frequency; // kHz
    private final String callsign;
    private final String spotter;
    private final String comment;

    public DXSpot(String time, double frequency, String callsign, String spotter, String comment) {
        this.time = time;
        this.frequency = frequency;
        this.callsign = callsign;
        this.spotter = spotter;
        this.comment = comment;
    }

    public String getTime()      { return time; }
    public double getFrequency() { return frequency; }
    public String getCallsign()  { return callsign; }
    public String getSpotter()   { return spotter; }
    public String getComment()   { return comment; }
}
