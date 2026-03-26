package org.qualsh.lb.view.digital;

import javax.swing.JTable;
import javax.swing.table.TableColumnModel;

import org.qualsh.lb.data.DigitalContactsModel;
import org.qualsh.lb.util.TableColumnAdjuster;

/**
 * Displays your logged digital mode contacts in a sortable, scrollable
 * table. Each row shows the date, time, callsign, band, frequency, mode,
 * signal reports, and any notes saved with that contact.
 *
 * <p>Click any column header to sort the table by that column. Select a
 * row and use the buttons above the table to edit or delete that contact.</p>
 */
public class DigitalContactsTable extends JTable {

	private static final long serialVersionUID = 9087654321098765432L;

	private DigitalContactsModel model;

	/**
	 * Creates a digital contacts table backed by the given data model.
	 * Column widths are automatically adjusted to fit the content on
	 * first display.
	 *
	 * @param model the data model that supplies rows to this table
	 */
	public DigitalContactsTable(DigitalContactsModel model) {
		super(model);
		this.model = model;
		configureColumns();
	}

	/**
	 * Reloads all contacts from the database and repaints the table.
	 * Use this after you have saved or deleted a contact to ensure the
	 * display reflects the latest data.
	 */
	public void refresh() {
		model.refresh();
		repaint();
	}

	/**
	 * Returns the data model that backs this table, allowing callers to
	 * perform searches, inserts, and deletes through the model API.
	 */
	public DigitalContactsModel getDigitalContactsModel() {
		return model;
	}

	private void configureColumns() {
		setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		TableColumnModel tcm = getColumnModel();
		tcm.getColumn(DigitalContactsModel.COLUMN_NUM_DATE_ON).setPreferredWidth(90);
		tcm.getColumn(DigitalContactsModel.COLUMN_NUM_TIME_ON).setPreferredWidth(70);
		tcm.getColumn(DigitalContactsModel.COLUMN_NUM_CALLSIGN).setPreferredWidth(90);
		tcm.getColumn(DigitalContactsModel.COLUMN_NUM_BAND).setPreferredWidth(55);
		tcm.getColumn(DigitalContactsModel.COLUMN_NUM_FREQUENCY_MHZ).setPreferredWidth(110);
		tcm.getColumn(DigitalContactsModel.COLUMN_NUM_MODE).setPreferredWidth(80);
		tcm.getColumn(DigitalContactsModel.COLUMN_NUM_SIGNAL_REPORT_SENT).setPreferredWidth(55);
		tcm.getColumn(DigitalContactsModel.COLUMN_NUM_SIGNAL_REPORT_RECEIVED).setPreferredWidth(70);
		tcm.getColumn(DigitalContactsModel.COLUMN_NUM_NOTES).setPreferredWidth(200);

		TableColumnAdjuster adjuster = new TableColumnAdjuster(this);
		adjuster.adjustColumns();
	}
}
