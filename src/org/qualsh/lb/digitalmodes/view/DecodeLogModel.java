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
 * Stores the list of decoded signals shown in the decode log table.
 *
 * <p>Each row represents one decoded signal and shows its time, mode, frequency, and decoded
 * text. To keep memory usage sensible during long sessions, only the most recent
 * {@value #MAX_ROWS} entries are kept; older entries are quietly removed as new ones arrive.
 * Use {@link #addResult(DecodeResult)} to add entries and {@link #clearResults()} to reset
 * the log.
 *
 * @author Logbook Development Team
 * @version 1.0
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
     * Creates a new, empty decode log model with no entries.
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
     * Returns the value for a specific cell in the log table.
     *
     * <p>Column 0 is the decode time, column 1 is the mode, column 2 is the frequency in Hz,
     * and column 3 is the decoded text.
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
     * Adds a decoded signal to the log.
     *
     * <p>If the log is at its maximum size, the oldest entry is removed first to make room.
     * The decode log table updates automatically to show the new entry.
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
     * Removes all entries from the decode log. The table updates automatically to show an
     * empty log.
     */
    public void clearResults() {
        SwingUtilities.invokeLater(() -> {
            data.clear();
            fireTableDataChanged();
        });
    }

    /**
     * Returns the decoded signal at the given row index.
     *
     * @param rowIndex the zero-based row index
     * @return the result at that row; never {@code null}
     * @throws IndexOutOfBoundsException if {@code rowIndex} is out of range
     */
    public DecodeResult getResultAt(int rowIndex) {
        return data.get(rowIndex);
    }

    /**
     * Returns all decoded signals currently in the log as an unmodifiable list.
     *
     * @return an unmodifiable list of all results; never {@code null}
     */
    public List<DecodeResult> getAllResults() {
        return Collections.unmodifiableList(data);
    }
}
