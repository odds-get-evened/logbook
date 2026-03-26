package org.qualsh.lb.view.digital;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.border.EmptyBorder;

import org.qualsh.lb.data.DigitalContactsModel;
import org.qualsh.lb.digital.DigitalContact;

/**
 * The main panel for the Digital Modes feature. It presents three tabs:
 * <ul>
 *   <li><b>Contacts</b> – a searchable table of all logged digital contacts.</li>
 *   <li><b>Waterfall</b> – a live spectrum waterfall display for monitoring
 *       digital activity on the current frequency.</li>
 *   <li><b>Settings</b> – configuration options for audio devices and
 *       digital mode preferences.</li>
 * </ul>
 *
 * <p>Add this panel as a tab in the main application window to give operators
 * access to all digital mode functions in one place.</p>
 */
public class DigitalModesPanel extends JPanel {

	private static final long serialVersionUID = 3312098765432109876L;

	private JTabbedPane tabbedPane;
	private DigitalContactsTable contactsTable;
	private WaterfallPanel waterfallPanel;
	private JTextField textSearch;
	private JButton btnNewContact;
	private JButton btnDeleteContact;
	private JButton btnResetSearch;
	private DigitalContactsModel contactsModel;

	/**
	 * Creates the Digital Modes panel, loads the contacts table with data
	 * from the logbook, and prepares the waterfall display ready to receive
	 * audio input.
	 */
	public DigitalModesPanel() {
		setBorder(new EmptyBorder(0, 0, 0, 0));
		setLayout(new BorderLayout(0, 0));

		tabbedPane = new JTabbedPane(JTabbedPane.TOP);
		add(tabbedPane, BorderLayout.CENTER);

		tabbedPane.addTab("Contacts", buildContactsTab());
		tabbedPane.addTab("Waterfall", buildWaterfallTab());
		tabbedPane.addTab("Settings", buildSettingsTab());
	}

	/**
	 * Reloads the contacts table from the database and repaints the panel.
	 * Call this after adding or deleting a contact to keep the display
	 * up to date.
	 */
	public void refresh() {
		contactsModel.refresh();
	}

	/**
	 * Returns the {@link DigitalContact} that the user currently has selected
	 * in the contacts table, or {@code null} if no row is selected.
	 */
	public DigitalContact getSelectedContact() {
		int row = contactsTable.getSelectedRow();
		if (row < 0) return null;
		return contactsModel.getContactAt(row);
	}

	/**
	 * Returns the waterfall panel embedded in the Waterfall tab, allowing
	 * callers to start and stop the audio feed or update the dial frequency.
	 */
	public WaterfallPanel getWaterfallPanel() {
		return waterfallPanel;
	}

	/**
	 * Returns the contacts table component displayed in the Contacts tab.
	 */
	public DigitalContactsTable getContactsTable() {
		return contactsTable;
	}

	private JPanel buildContactsTab() {
		JPanel panel = new JPanel(new BorderLayout(0, 0));
		panel.setBorder(new EmptyBorder(0, 0, 0, 0));

		contactsModel = new DigitalContactsModel();
		contactsTable = new DigitalContactsTable(contactsModel);
		contactsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		JScrollPane scrollPane = new JScrollPane(contactsTable);
		scrollPane.setBorder(new EmptyBorder(1, 1, 1, 1));
		panel.add(scrollPane, BorderLayout.CENTER);

		JPanel toolbar = buildContactsToolbar();
		panel.add(toolbar, BorderLayout.NORTH);

		return panel;
	}

	private JPanel buildContactsToolbar() {
		JPanel toolbar = new JPanel();
		toolbar.setBorder(new EmptyBorder(5, 5, 5, 5));

		textSearch = new JTextField();
		textSearch.setColumns(20);
		textSearch.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {
				String query = textSearch.getText().trim();
				if (query.isEmpty()) {
					contactsModel.refresh();
				} else {
					contactsModel.search(query);
				}
			}
		});

		btnResetSearch = new JButton("Reset");
		btnResetSearch.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				textSearch.setText("");
				contactsModel.refresh();
			}
		});

		btnNewContact = new JButton("New Contact");
		btnDeleteContact = new JButton("Delete");

		toolbar.add(textSearch);
		toolbar.add(btnResetSearch);
		toolbar.add(btnNewContact);
		toolbar.add(btnDeleteContact);

		return toolbar;
	}

	private JPanel buildWaterfallTab() {
		JPanel panel = new JPanel(new BorderLayout(0, 0));
		waterfallPanel = new WaterfallPanel();
		panel.add(waterfallPanel, BorderLayout.CENTER);
		return panel;
	}

	private JPanel buildSettingsTab() {
		JPanel panel = new JPanel(new BorderLayout(0, 0));
		panel.setBorder(new EmptyBorder(10, 10, 10, 10));
		return panel;
	}
}
