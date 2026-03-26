package org.qualsh.lb.digitalmodes.view;

import org.qualsh.lb.digital.decode.DecodeResult;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * A styled {@link JTable} that displays the history of decoded digital-mode
 * transmissions produced during an active decode session.
 *
 * <p>The table is backed by a {@link DecodeLogModel} and renders each
 * {@link DecodeResult} as a single row with four columns: Time, Mode,
 * Frequency, and Decoded Text. Rows alternate between two dark background
 * colours for readability and the selected row is highlighted in green.
 *
 * <p>New results should be appended via {@link #addDecodeResult(DecodeResult)},
 * which also auto-scrolls the view so the latest entry is always visible.
 * The entire log can be wiped with {@link #clearResults()}.
 *
 * <p>An optional {@link DecodeResultSelectionListener} can be registered with
 * {@link #setDecodeResultSelectionListener(DecodeResultSelectionListener)} to
 * be notified whenever the user clicks a row.
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
     * Callback interface notified when the user selects a row in the decode
     * log table.
     */
    public interface DecodeResultSelectionListener {
        /**
         * Called on the Swing Event Dispatch Thread when the user clicks a row.
         *
         * @param result the {@link DecodeResult} that was selected; never
         *               {@code null}
         */
        void onDecodeResultSelected(DecodeResult result);
    }

    /**
     * Creates a new {@code DecodeLogTable} backed by the given model.
     *
     * @param model the {@link DecodeLogModel} that supplies row data; must not
     *              be {@code null}
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
     * Appends a decode result to the log and scrolls the view so the newest
     * entry is visible.
     *
     * <p>This method delegates storage to the underlying {@link DecodeLogModel}
     * and may be called from any thread.
     *
     * @param result the decode result to add; must not be {@code null}
     */
    public void addDecodeResult(DecodeResult result) {
        decodeLogModel.addResult(result);
        scrollToLatestRow();
    }

    /**
     * Removes all decode results from the log.
     *
     * <p>This method delegates to the underlying {@link DecodeLogModel} and
     * may be called from any thread.
     */
    public void clearResults() {
        decodeLogModel.clearResults();
    }

    /**
     * Scrolls the table viewport so that the last (newest) row is visible.
     *
     * <p>Has no effect if the table is currently empty.
     */
    public void scrollToLatestRow() {
        if (getRowCount() > 0) {
            scrollRectToVisible(getCellRect(getRowCount() - 1, 0, true));
        }
    }

    /**
     * Registers a listener that will be notified when the user clicks a row in
     * the table.
     *
     * <p>Passing {@code null} removes any previously registered listener.
     *
     * @param listener the listener to register, or {@code null} to deregister
     */
    public void setDecodeResultSelectionListener(DecodeResultSelectionListener listener) {
        this.selectionListener = listener;
    }

    /**
     * Returns the {@link DecodeLogModel} that backs this table.
     *
     * @return the model; never {@code null}
     */
    public DecodeLogModel getDecodeLogModel() {
        return decodeLogModel;
    }
}
