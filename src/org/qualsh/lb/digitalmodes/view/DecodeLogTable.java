package org.qualsh.lb.digitalmodes.view;

import org.qualsh.lb.digital.decode.DecodeResult;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * The decode log table that shows the history of decoded signals in the current session.
 *
 * <p>Each row represents one decoded signal and shows its time, mode, frequency, and decoded
 * text. Rows alternate between two dark shades for easy reading, and clicking a row selects
 * it. New results are added with {@link #addDecodeResult(DecodeResult)} and the view
 * automatically scrolls to show the latest entry. Register a
 * {@link DecodeResultSelectionListener} to be notified when the user clicks a row.
 *
 * @author Logbook Development Team
 * @version 1.0
 */
public class DecodeLogTable extends JTable {

    private static final long serialVersionUID = 1L;

    private DecodeLogModel decodeLogModel;

    private static final Font TABLE_FONT  = new Font(Font.MONOSPACED,  Font.PLAIN, 11);
    private static final Font HEADER_FONT = new Font(Font.SANS_SERIF,  Font.BOLD,  11);

    private static final Color ROW_COLOR       = new Color(30,  30,  30);
    private static final Color ALT_ROW_COLOR   = new Color(40,  40,  40);
    private static final Color SELECTION_COLOR = new Color(0,  100,  60);
    private static final Color TEXT_COLOR      = new Color(200, 200, 200);
    private static final Color HEADER_COLOR    = new Color(20,  20,  20);

    private static final int ROW_HEIGHT = 22;

    private DecodeResultSelectionListener selectionListener;

    /**
     * Notified when the user clicks a row in the decode log table.
     *
     * @author Logbook Development Team
     * @version 1.0
     */
    public interface DecodeResultSelectionListener {
        /**
         * Called when the user clicks a row in the decode log table.
         *
         * @param result the decoded signal that was selected; never {@code null}
         */
        void onDecodeResultSelected(DecodeResult result);
    }

    /**
     * Creates a new decode log table backed by the given model.
     *
     * @param model the model that supplies the row data; must not be {@code null}
     */
    public DecodeLogTable(DecodeLogModel model) {
        super(model);
        this.decodeLogModel = model;

        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        setFont(TABLE_FONT);
        setRowHeight(ROW_HEIGHT);
        setShowGrid(false);
        setIntercellSpacing(new Dimension(0, 0));
        setFillsViewportHeight(true);

        setBackground(ROW_COLOR);
        setForeground(TEXT_COLOR);
        setSelectionBackground(SELECTION_COLOR);
        setSelectionForeground(Color.WHITE);

        JTableHeader header = getTableHeader();
        header.setFont(HEADER_FONT);
        header.setBackground(HEADER_COLOR);
        header.setForeground(Color.LIGHT_GRAY);
        header.setReorderingAllowed(false);

        // Column widths — Time, Mode, Frequency each fixed; Decoded Text gets remainder
        TableColumn timeCol  = getColumnModel().getColumn(0);
        TableColumn modeCol  = getColumnModel().getColumn(1);
        TableColumn freqCol  = getColumnModel().getColumn(2);
        TableColumn textCol  = getColumnModel().getColumn(3);

        timeCol.setPreferredWidth(70);
        modeCol.setPreferredWidth(80);
        freqCol.setPreferredWidth(100);
        textCol.setPreferredWidth(400);

        // Mouse listener for single-click row selection
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int rowIndex = getSelectedRow();
                if (rowIndex < 0) {
                    return;
                }
                DecodeResult result = decodeLogModel.getResultAt(rowIndex);
                if (selectionListener != null) {
                    selectionListener.onDecodeResultSelected(result);
                }
            }
        });

        // Alternating row renderer
        DefaultTableCellRenderer alternatingRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table,
                    Object value, boolean isSelected, boolean hasFocus,
                    int row, int column) {
                Component c = super.getTableCellRendererComponent(
                        table, value, isSelected, hasFocus, row, column);
                if (!isSelected) {
                    c.setBackground(row % 2 == 0 ? ROW_COLOR : ALT_ROW_COLOR);
                    c.setForeground(TEXT_COLOR);
                }
                return c;
            }
        };

        for (int i = 0; i < getColumnCount(); i++) {
            getColumnModel().getColumn(i).setCellRenderer(alternatingRenderer);
        }
    }

    /**
     * Adds a decoded signal to the log and scrolls the table to show the newest entry.
     *
     * @param result the decode result to add; must not be {@code null}
     */
    public void addDecodeResult(DecodeResult result) {
        decodeLogModel.addResult(result);
        scrollToLatestRow();
    }

    /**
     * Removes all entries from the decode log, leaving an empty table.
     */
    public void clearResults() {
        decodeLogModel.clearResults();
    }

    /**
     * Scrolls the table to bring the most recently added row into view. Does nothing if the
     * log is empty.
     */
    public void scrollToLatestRow() {
        if (getRowCount() > 0) {
            scrollRectToVisible(getCellRect(getRowCount() - 1, 0, true));
        }
    }

    /**
     * Registers a listener to be notified when the user clicks a row. Pass {@code null} to
     * remove any existing listener.
     *
     * @param listener the listener to register, or {@code null} to deregister
     */
    public void setDecodeResultSelectionListener(DecodeResultSelectionListener listener) {
        this.selectionListener = listener;
    }

    /**
     * Returns the model that backs this table.
     *
     * @return the decode log model; never {@code null}
     */
    public DecodeLogModel getDecodeLogModel() {
        return decodeLogModel;
    }
}
