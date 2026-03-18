package org.qualsh.lb.dx;

/**
 * Represents a known public DX cluster server.
 */
public class DXClusterServer {

    private int    id;
    private String callNode;
    private String host;
    private int    port;
    private String location;
    private double lat;
    private double lon;

    public DXClusterServer() {}

    public DXClusterServer(String callNode, String host, int port, String location, double lat, double lon) {
        this.callNode = callNode;
        this.host     = host;
        this.port     = port;
        this.location = location;
        this.lat      = lat;
        this.lon      = lon;
    }

    /** Display name shown in combo boxes: "CALL (host:port, Location)" */
    @Override
    public String toString() {
        return callNode + "  (" + host + ":" + port + " — " + location + ")";
    }

    // ── Getters / Setters ─────────────────────────────────────────────────

    public int getId()                    { return id; }
    public void setId(int id)             { this.id = id; }

    public String getCallNode()               { return callNode; }
    public void setCallNode(String callNode)  { this.callNode = callNode; }

    public String getHost()               { return host; }
    public void setHost(String host)      { this.host = host; }

    public int getPort()                  { return port; }
    public void setPort(int port)         { this.port = port; }

    public String getLocation()                   { return location; }
    public void setLocation(String location)      { this.location = location; }

    public double getLat()                { return lat; }
    public void setLat(double lat)        { this.lat = lat; }

    public double getLon()                { return lon; }
    public void setLon(double lon)        { this.lon = lon; }
}
