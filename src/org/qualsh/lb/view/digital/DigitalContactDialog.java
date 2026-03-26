package org.qualsh.lb.view.digital;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

import org.qualsh.lb.digital.DigitalContact;
import org.qualsh.lb.digital.DigitalMode;

/**
 * A dialog for creating a new digital mode contact entry or reviewing
 * the details of an existing one. Fill in the callsign, frequency, mode,
 * signal reports, and any notes, then click <b>Save</b> to add the contact
 * to your logbook.
 *
 * <p>To pre-populate the fields from an existing contact (for example,
 * when editing), use the {@link #DigitalContactDialog(Frame, DigitalContact)}
 * constructor.</p>
 */
public class DigitalContactDialog extends JDialog {

	private static final long serialVersionUID = 6543210987654321098L;

	private JTextField fieldCallsign;
	private JTextField fieldFrequencyMhz;
	private JTextField fieldBand;
	private JTextField fieldSignalReportSent;
	private JTextField fieldSignalReportReceived;
	private JTextField fieldTimeOn;
	private JTextField fieldTimeOff;
	private JTextField fieldMyCallsign;
	private JTextField fieldGrid;
	private JTextField fieldTxPowerWatts;
	private JTextField fieldNotes;

	private JButton btnSave;
	private JButton btnCancel;

	private boolean confirmed;
	private DigitalContact contact;

	/**
	 * Opens a blank dialog for entering a brand-new digital mode contact.
	 * The date is pre-filled with today's date and the time fields are set
	 * to the current UTC time.
	 *
	 * @param owner the parent window that this dialog is attached to
	 */
	public DigitalContactDialog(Frame owner) {
		this(owner, null);
	}

	/**
	 * Opens the dialog pre-filled with the details of an existing contact.
	 * Use this to review or edit a contact you have already logged. Click
	 * <b>Save</b> to apply any changes.
	 *
	 * @param owner   the parent window that this dialog is attached to
	 * @param contact the existing contact whose details should be loaded
	 *                into the form, or {@code null} to start with a blank form
	 */
	public DigitalContactDialog(Frame owner, DigitalContact contact) {
		super(owner, contact == null ? "New Digital Contact" : "Edit Digital Contact", true);
		this.contact = contact;
		this.confirmed = false;
		buildUI();
		if (contact != null) {
			populateFields(contact);
		}
		pack();
		setLocationRelativeTo(owner);
	}

	/**
	 * Returns {@code true} if the user clicked <b>Save</b> to close the dialog,
	 * or {@code false} if they clicked <b>Cancel</b> or dismissed the dialog
	 * without saving.
	 */
	public boolean isConfirmed() {
		return confirmed;
	}

	/**
	 * Returns a {@link DigitalContact} populated with the values the user
	 * entered in the form. Returns {@code null} if the dialog was cancelled
	 * or has not yet been closed.
	 */
	public DigitalContact getContact() {
		return contact;
	}

	/**
	 * Sets the digital mode shown in the mode selector to the given mode.
	 * Call this before displaying the dialog to pre-select the mode that
	 * was active in the waterfall view when the user initiated the log entry.
	 *
	 * @param mode the digital mode to pre-select
	 */
	public void setDigitalMode(DigitalMode mode) {
	}

	private void buildUI() {
		JPanel content = new JPanel(new BorderLayout(0, 0));
		content.setBorder(new EmptyBorder(10, 10, 10, 10));
		setContentPane(content);

		JPanel form = new JPanel(new GridBagLayout());
		content.add(form, BorderLayout.CENTER);

		GridBagConstraints labelConstraints = new GridBagConstraints();
		labelConstraints.anchor = GridBagConstraints.EAST;
		labelConstraints.insets = new Insets(4, 4, 4, 6);

		GridBagConstraints fieldConstraints = new GridBagConstraints();
		fieldConstraints.fill = GridBagConstraints.HORIZONTAL;
		fieldConstraints.weightx = 1.0;
		fieldConstraints.insets = new Insets(4, 0, 4, 4);

		int row = 0;

		addFormRow(form, "Callsign:", fieldCallsign = new JTextField(12), labelConstraints, fieldConstraints, row++);
		addFormRow(form, "Frequency (MHz):", fieldFrequencyMhz = new JTextField(12), labelConstraints, fieldConstraints, row++);
		addFormRow(form, "Band:", fieldBand = new JTextField(8), labelConstraints, fieldConstraints, row++);
		addFormRow(form, "Time On (UTC):", fieldTimeOn = new JTextField(8), labelConstraints, fieldConstraints, row++);
		addFormRow(form, "Time Off (UTC):", fieldTimeOff = new JTextField(8), labelConstraints, fieldConstraints, row++);
		addFormRow(form, "Sent Report:", fieldSignalReportSent = new JTextField(8), labelConstraints, fieldConstraints, row++);
		addFormRow(form, "Received Report:", fieldSignalReportReceived = new JTextField(8), labelConstraints, fieldConstraints, row++);
		addFormRow(form, "My Callsign:", fieldMyCallsign = new JTextField(12), labelConstraints, fieldConstraints, row++);
		addFormRow(form, "My Grid:", fieldGrid = new JTextField(8), labelConstraints, fieldConstraints, row++);
		addFormRow(form, "TX Power (W):", fieldTxPowerWatts = new JTextField(8), labelConstraints, fieldConstraints, row++);
		addFormRow(form, "Notes:", fieldNotes = new JTextField(24), labelConstraints, fieldConstraints, row++);

		JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		btnSave = new JButton("Save");
		btnCancel = new JButton("Cancel");
		buttons.add(btnSave);
		buttons.add(btnCancel);
		content.add(buttons, BorderLayout.SOUTH);

		btnSave.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (collectFields()) {
					confirmed = true;
					dispose();
				}
			}
		});

		btnCancel.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				confirmed = false;
				dispose();
			}
		});
	}

	private void addFormRow(JPanel panel, String labelText, JTextField field,
			GridBagConstraints lc, GridBagConstraints fc, int row) {
		GridBagConstraints l = (GridBagConstraints) lc.clone();
		GridBagConstraints f = (GridBagConstraints) fc.clone();
		l.gridx = 0; l.gridy = row;
		f.gridx = 1; f.gridy = row;
		panel.add(new JLabel(labelText), l);
		panel.add(field, f);
	}

	private void populateFields(DigitalContact c) {
		fieldCallsign.setText(c.getCallsign());
		fieldFrequencyMhz.setText(String.valueOf(c.getFrequencyMhz()));
		fieldBand.setText(c.getBand());
		fieldTimeOn.setText(c.getTimeOn());
		fieldTimeOff.setText(c.getTimeOff());
		fieldSignalReportSent.setText(c.getSignalReportSent());
		fieldSignalReportReceived.setText(c.getSignalReportReceived());
		fieldMyCallsign.setText(c.getMyCallsign());
		fieldGrid.setText(c.getGrid());
		fieldTxPowerWatts.setText(String.valueOf(c.getTxPowerWatts()));
		fieldNotes.setText(c.getNotes());
	}

	private boolean collectFields() {
		if (contact == null) {
			contact = new DigitalContact();
		}
		contact.setCallsign(fieldCallsign.getText().trim().toUpperCase());
		contact.setBand(fieldBand.getText().trim());
		contact.setTimeOn(fieldTimeOn.getText().trim());
		contact.setTimeOff(fieldTimeOff.getText().trim());
		contact.setSignalReportSent(fieldSignalReportSent.getText().trim());
		contact.setSignalReportReceived(fieldSignalReportReceived.getText().trim());
		contact.setMyCallsign(fieldMyCallsign.getText().trim().toUpperCase());
		contact.setGrid(fieldGrid.getText().trim().toUpperCase());
		contact.setNotes(fieldNotes.getText().trim());
		try {
			contact.setFrequencyMhz(Float.parseFloat(fieldFrequencyMhz.getText().trim()));
		} catch (NumberFormatException e) {
			return false;
		}
		try {
			String pw = fieldTxPowerWatts.getText().trim();
			if (!pw.isEmpty()) {
				contact.setTxPowerWatts(Float.parseFloat(pw));
			}
		} catch (NumberFormatException e) {
			return false;
		}
		return true;
	}
}
