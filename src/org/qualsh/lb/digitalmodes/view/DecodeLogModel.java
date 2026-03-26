package org.qualsh.lb.digitalmodes.view;

import org.qualsh.lb.digital.decode.DecodeResult;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Table model backing the decode log table in the Digital Modes panel.
 *
 * <p>Each row represents one {@link DecodeResult} produced by an active
 * decoder. The model exposes four read-only columns — Time, Mode, Frequency,
 * and Decoded Text — and caps its row count at {@value #MAX_ROWS} to prevent
 * unbounded memory growth during long decode sessions. When the cap is
 * reached the oldest result is silently discarded as each new one arrives.
 *
 * <p>All mutations to the underlying list are dispatched on the Swing Event
 * Dispatch Thread, making it safe to call the public mutator methods from any
 * thread.
 */
public class DecodeLogModel extends AbstractTableModel {

    private static final String[] COLUMN_NAMES =
            {"Time", "Mode", "Frequency (Hz)", "Decoded Text"};

    /** Maximum number of rows retained before the oldest entry is discarded. */
    private static final int MAX_ROWS = 1000;

    private static final DateTimeFormatter TIME_FMT =
            DateTimeFormatter.ofPattern("HH:mm:ss");

    private final List<DecodeResult> data;

    /**
     * Creates a new, empty {@code DecodeLogModel}.
     */
    public DecodeLogModel() {
        data = new ArrayList<>();
    }

    /**
     * Returns the number of decode results currently held by this model.
     *
     * @return the current row count; never negative
     */
    @Override
    public int getRowCount() {
        return data.size();
    }

    /**
     * Returns the number of columns in the decode log table.
     *
     * @return always {@code 4}
     */
    @Override
    public int getColumnCount() {
        return COLUMN_NAMES.length;
    }

    /**
     * Returns the header label for the given column.
     *
     * @param column the zero-based column index
     * @return the column header string; never {@code null}
     */
    @Override
    public String getColumnName(int column) {
        return COLUMN_NAMES[column];
    }

    /**
     * Returns the cell value for the requested row and column.
     *
     * <p>Column mapping:
     * <ul>
     *   <li>0 — decode time formatted as {@code HH:mm:ss}</li>
     *   <li>1 — numeric digital-mode identifier</li>
     *   <li>2 — audio frequency offset in hertz, one decimal place</li>
     *   <li>3 — decoded message text</li>
     * </ul>
     *
     * @param rowIndex    the zero-based row index
     * @param columnIndex the zero-based column index
     * @return the formatted cell value; never {@code null}
     */
    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        DecodeResult result = data.get(rowIndex);
        switch (columnIndex) {
            case 0:
                return Instant.ofEpochSecond(result.getTimestamp())
                        .atZone(ZoneId.systemDefault())
                        .toLocalTime()
                        .format(TIME_FMT);
            case 1:
                return "MODE-" + result.getDigitalModeId();
            case 2:
                return String.format("%.1f", result.getFrequencyOffsetHz());
            case 3:
                return result.getMessage();
            default:
                return "";
        }
    }

    /**
     * Returns the class used to render all columns in this model.
     *
     * @param columnIndex the zero-based column index (unused)
     * @return always {@code String.class}
     */
    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return String.class;
    }

    /**
     * Appends a new decode result to the end of the log.
     *
     * <p>If the number of rows would exceed {@value #MAX_ROWS} after the
     * insertion, the oldest result is removed first so the count stays at the
     * limit. Registered table listeners are notified of the insertion.
     *
     * <p>This method is safe to call from any thread.
     *
     * @param result the decode result to add; must not be {@code null}
     */
    public void addResult(DecodeResult result) {
        SwingUtilities.invokeLater(() -> {
            if (data.size() >= MAX_ROWS) {
                data.remove(0);
                fireTableRowsDeleted(0, 0);
            }
            data.add(result);
            int lastIndex = data.size() - 1;
            fireTableRowsInserted(lastIndex, lastIndex);
        });
    }

    /**
     * Removes all decode results from the model and notifies registered table
     * listeners.
     *
     * <p>This method is safe to call from any thread.
     */
    public void clearResults() {
        SwingUtilities.invokeLater(() -> {
            data.clear();
            fireTableDataChanged();
        });
    }

    /**
     * Returns the {@link DecodeResult} at the given row index.
     *
     * <p>Use this when the user selects a row in the decode log table to
     * inspect the full details of that result.
     *
     * @param rowIndex the zero-based row index
     * @return the result at that row; never {@code null}
     * @throws IndexOutOfBoundsException if {@code rowIndex} is out of range
     */
    public DecodeResult getResultAt(int rowIndex) {
        return data.get(rowIndex);
    }

    /**
     * Returns an unmodifiable view of all decode results currently held by
     * this model.
     *
     * <p>The returned list reflects the model's state at the time of the call.
     * Subsequent calls to {@link #addResult} or {@link #clearResults} are not
     * reflected in a previously returned list.
     *
     * @return an unmodifiable list of all results; never {@code null}
     */
    public List<DecodeResult> getAllResults() {
        return Collections.unmodifiableList(data);
    }
}
