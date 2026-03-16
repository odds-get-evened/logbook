package org.qualsh.lb.view;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

import org.jxmapviewer.JXMapViewer;
import org.jxmapviewer.OSMTileFactoryInfo;
import org.jxmapviewer.input.PanMouseInputListener;
import org.jxmapviewer.input.ZoomMouseWheelListenerCursor;
import org.jxmapviewer.viewer.DefaultTileFactory;
import org.jxmapviewer.viewer.DefaultWaypoint;
import org.jxmapviewer.viewer.GeoPosition;
import org.jxmapviewer.viewer.WaypointPainter;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.qualsh.lb.location.Location;
import org.qualsh.lb.log.Log;
import org.qualsh.lb.place.Place;

/**
 * Map panel showing log TX/RX locations on an OpenStreetMap base layer.
 *
 * Red markers  = TX (transmitter) locations.
 * Blue markers = RX (receiver / listening post) locations.
 * Orange marker = currently selected log's TX location.
 *
 * Layer filters:
 *   All Stations  – plots every location/place in the database.
 *   Log Entries   – plots only TX/RX locations referenced by log entries.
 */
public class MapPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    /** Which set of waypoints to display. */
    enum LayerFilter { ALL_LOCATIONS, LOG_ENTRIES }

    private final JXMapViewer mapViewer;

    /** Waypoints derived from actual log entries (TX + RX per log). */
    private final Set<LogMapWaypoint> logEntryWaypoints = new HashSet<>();
    /** Waypoints from all locations/places in the database. */
    private final Set<LogMapWaypoint> allLocWaypoints   = new HashSet<>();

    private LayerFilter activeLayer = LayerFilter.ALL_LOCATIONS;
    private LogMapWaypoint selectedWaypoint = null;

    private boolean pickingMode = false;
    private Consumer<GeoPosition> locationPickCallback = null;

    private final JTextField searchField;
    private final JLabel statusLabel;
    private final JToggleButton btnPick;

    public MapPanel() {
        setLayout(new BorderLayout());

        // ---- tile factory (OpenStreetMap) ----
        DefaultTileFactory tileFactory = new DefaultTileFactory(new OSMTileFactoryInfo() {
            @Override
            public String getTileUrl(int x, int y, int zoom) {
                return super.getTileUrl(x, y, zoom).replace("http://", "https://");
            }
        });
        tileFactory.setThreadPoolSize(4);

        mapViewer = new JXMapViewer();
        mapViewer.setTileFactory(tileFactory);
        mapViewer.setZoom(17);                                       // world-level zoom
        mapViewer.setAddressLocation(new GeoPosition(20.0, 0.0));   // centred near equator

        // ---- standard pan / zoom interactions ----
        PanMouseInputListener pml = new PanMouseInputListener(mapViewer);
        mapViewer.addMouseListener(pml);
        mapViewer.addMouseMotionListener(pml);
        mapViewer.addMouseWheelListener(new ZoomMouseWheelListenerCursor(mapViewer));

        // ---- click handler: waypoint popup / coordinate display / pick callback ----
        mapViewer.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (!SwingUtilities.isLeftMouseButton(e)) return;
                GeoPosition pos = mapViewer.convertPointToGeoPosition(e.getPoint());
                if (!pickingMode) {
                    LogMapWaypoint hit = findWaypointAt(e.getPoint());
                    if (hit != null) {
                        statusLabel.setText(formatCoord(pos.getLatitude(), pos.getLongitude()));
                        showWaypointPopup(hit, e.getPoint());
                        return;
                    }
                }
                String coordText = formatCoord(pos.getLatitude(), pos.getLongitude());
                statusLabel.setText(coordText);
                if (pickingMode && locationPickCallback != null) {
                    locationPickCallback.accept(pos);
                }
            }
        });

        // ---- search / toolbar panel ----
        JPanel toolbar = new JPanel(new GridBagLayout());
        toolbar.setBorder(new EmptyBorder(4, 4, 2, 4));

        searchField = new JTextField();
        JButton btnSearch = new JButton("Go");
        btnPick = new JToggleButton("Pick Location");
        statusLabel = new JLabel(" ");
        statusLabel.setForeground(Color.DARK_GRAY);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridy = 0;
        gbc.insets = new Insets(0, 0, 2, 4);

        gbc.gridx = 0;
        gbc.fill = GridBagConstraints.NONE;
        toolbar.add(new JLabel("Search:"), gbc);

        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        toolbar.add(searchField, gbc);

        gbc.gridx = 2;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        toolbar.add(btnSearch, gbc);

        gbc.gridx = 3;
        toolbar.add(btnPick, gbc);

        gbc.gridx = 4;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 0.8;
        toolbar.add(statusLabel, gbc);

        // ---- layer filter row ----
        JRadioButton rbAllLocations = new JRadioButton("All Stations", true);
        JRadioButton rbLogEntries   = new JRadioButton("Log Entries Only");
        ButtonGroup layerGroup = new ButtonGroup();
        layerGroup.add(rbAllLocations);
        layerGroup.add(rbLogEntries);

        rbAllLocations.addActionListener(e -> {
            activeLayer = LayerFilter.ALL_LOCATIONS;
            updatePainters();
        });
        rbLogEntries.addActionListener(e -> {
            activeLayer = LayerFilter.LOG_ENTRIES;
            updatePainters();
        });

        GridBagConstraints gbcL = new GridBagConstraints();
        gbcL.gridy = 1;
        gbcL.insets = new Insets(0, 0, 0, 4);

        gbcL.gridx = 0;
        gbcL.fill = GridBagConstraints.NONE;
        toolbar.add(new JLabel("Layer:"), gbcL);

        gbcL.gridx = 1;
        toolbar.add(rbAllLocations, gbcL);

        gbcL.gridx = 2;
        gbcL.gridwidth = 3;
        toolbar.add(rbLogEntries, gbcL);

        btnSearch.addActionListener(e -> geocodeSearch());
        searchField.addActionListener(e -> geocodeSearch());
        btnPick.addActionListener(e -> {
            pickingMode = btnPick.isSelected();
            mapViewer.setCursor(pickingMode
                    ? Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR)
                    : Cursor.getDefaultCursor());
            statusLabel.setText(pickingMode ? "Click on the map to pick a location" : " ");
        });

        add(toolbar, BorderLayout.NORTH);
        add(mapViewer, BorderLayout.CENTER);
    }

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    /**
     * Plot all database locations and places as markers (the "All Stations" layer).
     * Runs DB lookups on a background thread.
     */
    public void plotAllLocations(List<Location> locations, List<Place> places) {
        new Thread(() -> {
            Set<LogMapWaypoint> fresh = new HashSet<>();
            if (locations != null) {
                for (Location loc : locations) {
                    tryAddWaypoint(fresh, loc.getStrLatitude(), loc.getStrLongitude(),
                            LogMapWaypoint.Type.TX, null, loc.getLocationName());
                }
            }
            if (places != null) {
                for (Place place : places) {
                    tryAddWaypoint(fresh, place.getLatitude(), place.getLongitude(),
                            LogMapWaypoint.Type.RX, null, place.getPlaceName());
                }
            }
            SwingUtilities.invokeLater(() -> {
                allLocWaypoints.clear();
                allLocWaypoints.addAll(fresh);
                updatePainters();
            });
        }, "MapPanel-plotAll").start();
    }

    /**
     * Plot all log entries on the map (the "Log Entries" layer).
     * TX locations are red; RX locations are blue.
     * Runs the DB lookups on a background thread to keep the EDT responsive.
     */
    public void plotLogs(List<Log> logs) {
        if (logs == null) return;
        new Thread(() -> {
            Set<LogMapWaypoint> fresh = new HashSet<>();
            for (Log log : logs) {
                Place txPlace = log.getFullTxPlace();
                if (txPlace != null) {
                    String txLabel = txPlace.getPlaceName() != null ? txPlace.getPlaceName() : "";
                    tryAddWaypoint(fresh, txPlace.getLatitude(), txPlace.getLongitude(),
                            LogMapWaypoint.Type.TX, log, txLabel);
                }
                Place place = log.getFullMyPlace();
                if (place != null) {
                    String rxLabel = place.getPlaceName() != null ? place.getPlaceName() : "";
                    tryAddWaypoint(fresh, place.getLatitude(), place.getLongitude(),
                            LogMapWaypoint.Type.RX, log, rxLabel);
                }
            }
            SwingUtilities.invokeLater(() -> {
                logEntryWaypoints.clear();
                logEntryWaypoints.addAll(fresh);
                selectedWaypoint = null;
                updatePainters();
            });
        }, "MapPanel-plot").start();
    }

    /**
     * Highlight the TX location of the given log and pan the map to it.
     * The selection highlight is shown regardless of the active layer.
     */
    public void highlightLog(Log log) {
        selectedWaypoint = null;
        for (LogMapWaypoint wp : logEntryWaypoints) {
            if (wp.getLog() != null
                    && wp.getLog().getId() == log.getId()
                    && wp.getType() == LogMapWaypoint.Type.TX) {
                selectedWaypoint = wp;
                break;
            }
        }
        if (selectedWaypoint != null) {
            mapViewer.setAddressLocation(selectedWaypoint.getPosition());
            mapViewer.setZoom(5);
        }
        updatePainters();
    }

    /** Remove the current selection highlight. */
    public void clearSelection() {
        selectedWaypoint = null;
        updatePainters();
    }

    /**
     * Zoom the map to fit both TX and RX locations of the given log entry.
     * Falls back to panning if only one position is available.
     */
    public void zoomToLogBounds(Log log) {
        Set<GeoPosition> positions = new HashSet<>();
        for (LogMapWaypoint wp : logEntryWaypoints) {
            if (wp.getLog() != null && wp.getLog().getId() == log.getId()) {
                positions.add(wp.getPosition());
            }
        }
        if (positions.isEmpty()) return;
        SwingUtilities.invokeLater(() -> {
            if (positions.size() == 1) {
                mapViewer.setAddressLocation(positions.iterator().next());
                mapViewer.setZoom(5);
            } else {
                mapViewer.zoomToBestFit(positions, 0.8);
            }
        });
    }

    /**
     * Pan the map to the given coordinates without changing existing markers.
     * Useful for previewing a station or place location.
     */
    public void panToLocation(double lat, double lon) {
        mapViewer.setAddressLocation(new GeoPosition(lat, lon));
        mapViewer.setZoom(5);
    }

    /**
     * Enable or disable location-picking mode.
     * While active the cursor changes to a crosshair and each click fires the callback.
     */
    public void setPickingMode(boolean active, Consumer<GeoPosition> callback) {
        pickingMode = active;
        locationPickCallback = callback;
        btnPick.setSelected(active);
        mapViewer.setCursor(active
                ? Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR)
                : Cursor.getDefaultCursor());
        statusLabel.setText(active ? "Click on the map to pick a location" : " ");
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    private void tryAddWaypoint(Set<LogMapWaypoint> target,
                                String latStr, String lonStr,
                                LogMapWaypoint.Type type, Log log, String label) {
        if (latStr == null || latStr.isBlank() || lonStr == null || lonStr.isBlank()) return;
        try {
            double lat = Double.parseDouble(latStr.trim());
            double lon = Double.parseDouble(lonStr.trim());
            target.add(new LogMapWaypoint(new GeoPosition(lat, lon), type, log, label));
        } catch (NumberFormatException ignored) {
        }
    }

    private void updatePainters() {
        // Build the display set from the active layer; always include the selected marker.
        Set<LogMapWaypoint> active = (activeLayer == LayerFilter.ALL_LOCATIONS)
                ? allLocWaypoints : logEntryWaypoints;
        Set<LogMapWaypoint> displaySet = new HashSet<>(active);
        if (selectedWaypoint != null) displaySet.add(selectedWaypoint);

        WaypointPainter<LogMapWaypoint> painter = new WaypointPainter<>();
        painter.setWaypoints(displaySet);
        painter.setRenderer((g, map, wp) -> {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                // WaypointPainter.doPaint() already translates the Graphics2D by
                // (-viewportBounds.x, -viewportBounds.y), so we use raw tile-pixel
                // coordinates directly — no need to subtract the viewport origin.
                Point2D p = map.getTileFactory().geoToPixel(wp.getPosition(), map.getZoom());
                int size = (wp == selectedWaypoint) ? 16 : 12;
                int x = (int) p.getX() - size / 2;
                int y = (int) p.getY() - size / 2;

                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);

                Color fill;
                if (wp == selectedWaypoint) {
                    fill = new Color(255, 140, 0);       // orange – selected
                } else if (wp.getType() == LogMapWaypoint.Type.TX) {
                    fill = new Color(210, 40, 40);       // red – transmitter
                } else {
                    fill = new Color(40, 90, 210);       // blue – receiver
                }

                g2.setColor(fill);
                g2.fillOval(x, y, size, size);
                g2.setColor(Color.WHITE);
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawOval(x, y, size, size);
            } finally {
                g2.dispose();
            }
        });
        mapViewer.setOverlayPainter(painter);
        mapViewer.repaint();
    }

    /** Return the waypoint under the given screen point, or null if none. */
    private LogMapWaypoint findWaypointAt(Point screenPoint) {
        Set<LogMapWaypoint> active = (activeLayer == LayerFilter.ALL_LOCATIONS)
                ? allLocWaypoints : logEntryWaypoints;
        Set<LogMapWaypoint> toSearch = new HashSet<>(active);
        if (selectedWaypoint != null) toSearch.add(selectedWaypoint);

        int hitRadius = 10;
        for (LogMapWaypoint wp : toSearch) {
            Point2D tilePos = mapViewer.getTileFactory().geoToPixel(
                    wp.getPosition(), mapViewer.getZoom());
            java.awt.geom.Rectangle2D vb = mapViewer.getViewportBounds();
            int px = (int) (tilePos.getX() - vb.getX());
            int py = (int) (tilePos.getY() - vb.getY());
            int dx = screenPoint.x - px;
            int dy = screenPoint.y - py;
            if (dx * dx + dy * dy <= hitRadius * hitRadius) {
                return wp;
            }
        }
        return null;
    }

    /** Show a station-details popup card near the clicked waypoint. */
    private void showWaypointPopup(LogMapWaypoint wp, Point screenPoint) {
        JPanel card = new JPanel(new GridLayout(0, 1, 0, 3));
        card.setBorder(new EmptyBorder(6, 8, 6, 8));

        String typeText = wp.getType() == LogMapWaypoint.Type.TX ? "TX Transmitter" : "RX Location";
        JLabel titleLabel = new JLabel(typeText);
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 12f));
        card.add(titleLabel);

        if (!wp.getLabel().isEmpty()) {
            card.add(new JLabel(wp.getLabel()));
        }

        if (wp.getLog() != null) {
            Log log = wp.getLog();
            card.add(new JLabel(log.getFrequency() + " kHz  \u2022  " + log.getMode()));
            String desc = log.getDescription();
            if (desc != null && !desc.isEmpty()) {
                if (desc.length() > 60) desc = desc.substring(0, 57) + "\u2026";
                card.add(new JLabel("<html>" + desc + "</html>"));
            }
        }

        GeoPosition pos = wp.getPosition();
        JLabel coordLabel = new JLabel(formatCoord(pos.getLatitude(), pos.getLongitude()));
        coordLabel.setForeground(Color.DARK_GRAY);
        card.add(coordLabel);

        JPopupMenu popup = new JPopupMenu();
        popup.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
        popup.add(card);
        popup.show(mapViewer, screenPoint.x + 6, screenPoint.y + 6);
    }

    @SuppressWarnings("unchecked")
    private void geocodeSearch() {
        String query = searchField.getText().trim();
        if (query.isEmpty()) return;
        statusLabel.setText("Searching…");
        new Thread(() -> {
            try {
                String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8);
                String url = "https://nominatim.openstreetmap.org/search?q="
                        + encoded + "&format=json&limit=1";
                HttpClient client = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("User-Agent", "LogBook/1.0 radio-log-application")
                        .build();
                HttpResponse<String> response =
                        client.send(request, HttpResponse.BodyHandlers.ofString());
                JSONArray results = (JSONArray) new JSONParser().parse(response.body());
                if (results == null || results.isEmpty()) {
                    SwingUtilities.invokeLater(() -> statusLabel.setText("No results found"));
                    return;
                }
                JSONObject first = (JSONObject) results.get(0);
                double lat = Double.parseDouble((String) first.get("lat"));
                double lon = Double.parseDouble((String) first.get("lon"));
                String name = (String) first.get("display_name");
                String label = (name != null) ? name.split(",")[0] : "Found";
                SwingUtilities.invokeLater(() -> {
                    mapViewer.setAddressLocation(new GeoPosition(lat, lon));
                    mapViewer.setZoom(5);
                    statusLabel.setText(label);
                });
            } catch (Exception e) {
                SwingUtilities.invokeLater(
                        () -> statusLabel.setText("Search failed: " + e.getMessage()));
            }
        }, "MapPanel-geocode").start();
    }

    private static String formatCoord(double lat, double lon) {
        String ns = lat >= 0 ? "N" : "S";
        String ew = lon >= 0 ? "E" : "W";
        return String.format("%.4f°%s  %.4f°%s", Math.abs(lat), ns, Math.abs(lon), ew);
    }

    // -----------------------------------------------------------------------
    // Waypoint model
    // -----------------------------------------------------------------------

    static class LogMapWaypoint extends DefaultWaypoint {

        enum Type { TX, RX }

        private final Type type;
        private final Log log;
        private final String label;

        LogMapWaypoint(GeoPosition pos, Type type, Log log, String label) {
            super(pos);
            this.type = type;
            this.log = log;
            this.label = label != null ? label : "";
        }

        Type getType()   { return type; }
        Log getLog()     { return log; }
        String getLabel(){ return label; }
    }
}
