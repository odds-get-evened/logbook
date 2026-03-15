package org.qualsh.lb.view;

import java.awt.Color;
import java.awt.Component;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableCellRenderer;

import org.qualsh.lb.data.LogsModel;
import org.qualsh.lb.log.Log;
import org.qualsh.lb.util.TableColumnAdjuster;
import org.qualsh.lb.util.Utilities;

public class LogsTable extends JTable {
	
	protected int sortCol = 0;
	
	protected boolean isSortAsc = true;
	
	private static final long serialVersionUID = -3632751004830947138L;
	
	private LogInteraction logInteraction;

	private MapPanel mapPanel;

	private JPopupMenu popup;

	private Log selectedLog;
		
	public LogsTable() {}
	
	public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
		Component returnComp = super.prepareRenderer(renderer, row, column);
		Color alternateColor = new Color(230, 241, 242);
		Color white = Color.WHITE;
		if(!returnComp.getBackground().equals(getSelectionBackground())) {
			Color bg = (row%2 == 0 ? alternateColor : white);
			returnComp.setBackground(bg);
			bg = null;
		}
		
		return returnComp;
	}

	public LogsTable(LogsModel dm) {
		super(dm);
				
		this.createPopup();
		
		setAutoCreateRowSorter(true);
		
		getSelectionModel().addListSelectionListener(new ListSelectionListener() {

			public void valueChanged(ListSelectionEvent e) {
				
				ListSelectionModel lsm = (ListSelectionModel) e.getSource();
				
				if(lsm.isSelectionEmpty()) {
					LogsTable.this.getLogInteraction().getBtnDeleteLog().setEnabled(false);
					LogsTable.this.getLogInteraction().getBtnEditLog().setEnabled(false);
					LogsTable.this.emptyFields();
					if (LogsTable.this.getMapPanel() != null) {
						LogsTable.this.getMapPanel().clearSelection();
					}
				} else {
					int selectedRow = lsm.getMinSelectionIndex();
					LogsTable.this.getLogInteraction().getBtnDeleteLog().setEnabled(true);
					LogsTable.this.getLogInteraction().getBtnEditLog().setEnabled(true);

					LogsModel lm = (LogsModel) LogsTable.this.getModel();
					Log log = lm.getData().get(LogsTable.this.convertRowIndexToModel(selectedRow));

					LogsTable.this.fillFields(log);
					LogsTable.this.getLogInteraction().resetEntry();
					LogsTable.this.getLogInteraction().getTabbedPane().setSelectedIndex(0);

					if (LogsTable.this.getMapPanel() != null) {
						LogsTable.this.getMapPanel().highlightLog(log);
					}
				}
			}
			
		});
		
		/**
		 * add support for right click on table row, to show popup
		 */
		this.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseReleased(MouseEvent e) {
				Point point = e.getPoint();
				JTable table = (JTable) e.getSource();
				
				if(e.getButton() == MouseEvent.BUTTON3) {
					int r = table.rowAtPoint(point);
					System.out.println(r);
					
					LogsModel lm = (LogsModel) getModel();
					Log log = lm.getData().get(convertRowIndexToModel(r));
					LogsTable.this.setSelectedLog(log);
					
					if(r >= 0 && r < table.getRowCount()) {
						table.setRowSelectionInterval(r, r);
						LogsTable.this.getPopup().show(e.getComponent(), e.getX(), e.getY());
					} else {
						LogsTable.this.setSelectedLog(null);
						table.clearSelection();
					}
				}
			}
		});
		
		TableColumnAdjuster tca = new TableColumnAdjuster(this);
		tca.adjustColumns();
		
		this.setRowHeight(20);
	}

	private void createPopup() {
		this.setPopup(new JPopupMenu());
		
		JMenuItem editMenuItem = new JMenuItem("Edit");
		editMenuItem.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				Log log = LogsTable.this.getSelectedLog();
				LogsTable.this.getLogInteraction().fillForm(log);
				LogsTable.this.getLogInteraction().getTabbedPane().setSelectedIndex(1);
			}
			
		});
		
		JMenuItem deleteMenuItem = new JMenuItem("Delete");
		deleteMenuItem.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				int confirmed = JOptionPane.showConfirmDialog(SwingUtilities.getWindowAncestor(LogsTable.this), "Are you sure you want to delete this log entry?", "Confirm delete!", JOptionPane.YES_NO_OPTION);
				
				if(confirmed == JOptionPane.YES_OPTION) {
					LogsModel lm = (LogsModel) LogsTable.this.getModel();
					lm.delete(LogsTable.this.getSelectedLog());
				}
			}
			
		});
		
		this.getPopup().add(editMenuItem);
		this.getPopup().add(deleteMenuItem);
	}

	private void emptyFields() {
		LogInteraction li = getLogInteraction();
		li.getTextInfoFreq().setText("");
		li.getTextInfoDateOn().setText("");
		li.getTextInfoDesc().setText("");
		
		li.getViewLocationPanel().setCurrentLocation(null);
		li.getViewLocationPanel().resetFields();
		
		li.getViewRXPanel().setCurrentPlace(null);
		li.getViewRXPanel().resetFields();
	}
	
	private void fillFields(Log log) {
		LogInteraction li = getLogInteraction();
		
		li.getViewLocationPanel().setCurrentLocation(null);
		li.getViewLocationPanel().resetFields();
		
		li.getTextInfoFreq().setText(String.valueOf(log.getFrequency() + " " + log.getMode()));
		li.getTextInfoDateOn().setText(Utilities.unixTimestampToString(log.getDateOn(), "MM/dd/yyyy HH:mm"));
		li.getTextInfoDesc().setText(log.getDescription());
		
		li.getViewLocationPanel().setCurrentLocation(log.getFullLocation());
		li.getViewLocationPanel().fillFields();
		
		li.getViewRXPanel().setCurrentPlace(log.getFullMyPlace());
		li.getViewRXPanel().fillFields();
	}

	public LogInteraction getLogInteraction() {
		return logInteraction;
	}

	public void setLogInteraction(LogInteraction logInteraction) {
		this.logInteraction = logInteraction;
	}

	public MapPanel getMapPanel() {
		return mapPanel;
	}

	public void setMapPanel(MapPanel mapPanel) {
		this.mapPanel = mapPanel;
	}

	public JPopupMenu getPopup() {
		return popup;
	}

	public void setPopup(JPopupMenu popup) {
		this.popup = popup;
	}

	public Log getSelectedLog() {
		return selectedLog;
	}

	public void setSelectedLog(Log selectedLog) {
		this.selectedLog = selectedLog;
	}

}
