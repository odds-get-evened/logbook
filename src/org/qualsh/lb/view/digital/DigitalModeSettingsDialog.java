package org.qualsh.lb.view.digital;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;

/**
 * A settings dialog for configuring the Digital Modes feature. Use this
 * dialog to choose which audio input device the waterfall and decoder
 * should listen on, set the audio sample rate, and toggle optional
 * behaviours such as automatic contact logging.
 *
 * <p>Open this dialog from the Digital Modes panel or the main
 * <b>Settings</b> menu. Changes take effect the next time you start
 * a decoding session.</p>
 */
public class DigitalModeSettingsDialog extends JDialog {

	private static final long serialVersionUID = 5432109876543210987L;

	private JComboBox<String> comboAudioInputDevice;
	private JComboBox<Integer> comboSampleRate;
	private JCheckBox checkAutoLog;
	private JCheckBox checkShowDecodeOverlay;
	private JButton btnSave;
	private JButton btnCancel;

	private boolean confirmed;

	/**
	 * Opens the Digital Modes settings dialog, pre-populated with the
	 * settings currently saved in the application preferences.
	 *
	 * @param owner the parent window that this dialog is centred on
	 */
	public DigitalModeSettingsDialog(Frame owner) {
		super(owner, "Digital Modes Settings", true);
		this.confirmed = false;
		buildUI();
		populateAudioDevices();
		pack();
		setLocationRelativeTo(owner);
	}

	/**
	 * Returns {@code true} if the user clicked <b>Save</b> to close this
	 * dialog, indicating that the new settings should be applied and saved.
	 */
	public boolean isConfirmed() {
		return confirmed;
	}

	/**
	 * Returns the name of the audio input device the user selected in the
	 * Audio Device drop-down. Pass this name to the decoder when starting
	 * a new decoding session.
	 */
	public String getSelectedAudioDevice() {
		Object selected = comboAudioInputDevice.getSelectedItem();
		return selected != null ? selected.toString() : null;
	}

	/**
	 * Returns the audio sample rate in hertz that the user selected.
	 * Configure your audio capture at this rate to match the decoder's
	 * expectations and avoid pitch errors in decoded signals.
	 */
	public int getSelectedSampleRate() {
		Object selected = comboSampleRate.getSelectedItem();
		return selected instanceof Integer ? (Integer) selected : 44100;
	}

	/**
	 * Returns {@code true} if the user has enabled automatic contact
	 * logging. When this option is on, every successfully decoded CQ or
	 * contact message is offered to you as a draft log entry.
	 */
	public boolean isAutoLogEnabled() {
		return checkAutoLog.isSelected();
	}

	/**
	 * Returns {@code true} if decoded signal labels should be drawn as
	 * overlays on top of the waterfall display.
	 */
	public boolean isDecodeOverlayEnabled() {
		return checkShowDecodeOverlay.isSelected();
	}

	private void buildUI() {
		JPanel content = new JPanel(new BorderLayout(0, 8));
		content.setBorder(new EmptyBorder(10, 10, 10, 10));
		setContentPane(content);

		JPanel audioPanel = new JPanel(new GridBagLayout());
		audioPanel.setBorder(new TitledBorder("Audio"));

		GridBagConstraints lc = new GridBagConstraints();
		lc.anchor = GridBagConstraints.EAST;
		lc.insets = new Insets(4, 4, 4, 6);

		GridBagConstraints fc = new GridBagConstraints();
		fc.fill = GridBagConstraints.HORIZONTAL;
		fc.weightx = 1.0;
		fc.insets = new Insets(4, 0, 4, 4);

		lc.gridx = 0; lc.gridy = 0;
		fc.gridx = 1; fc.gridy = 0;
		audioPanel.add(new JLabel("Input Device:"), lc);
		comboAudioInputDevice = new JComboBox<String>();
		audioPanel.add(comboAudioInputDevice, fc);

		lc.gridy = 1; fc.gridy = 1;
		audioPanel.add(new JLabel("Sample Rate (Hz):"), lc);
		comboSampleRate = new JComboBox<Integer>(new DefaultComboBoxModel<Integer>(
				new Integer[]{8000, 11025, 22050, 44100, 48000}));
		comboSampleRate.setSelectedItem(44100);
		audioPanel.add(comboSampleRate, fc);

		content.add(audioPanel, BorderLayout.NORTH);

		JPanel optionsPanel = new JPanel(new GridBagLayout());
		optionsPanel.setBorder(new TitledBorder("Options"));

		GridBagConstraints cc = new GridBagConstraints();
		cc.anchor = GridBagConstraints.WEST;
		cc.insets = new Insets(4, 6, 4, 4);
		cc.gridx = 0;

		cc.gridy = 0;
		checkAutoLog = new JCheckBox("Offer decoded contacts as draft log entries");
		optionsPanel.add(checkAutoLog, cc);

		cc.gridy = 1;
		checkShowDecodeOverlay = new JCheckBox("Show decoded signal labels on waterfall");
		checkShowDecodeOverlay.setSelected(true);
		optionsPanel.add(checkShowDecodeOverlay, cc);

		content.add(optionsPanel, BorderLayout.CENTER);

		JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		btnSave = new JButton("Save");
		btnCancel = new JButton("Cancel");
		buttons.add(btnSave);
		buttons.add(btnCancel);
		content.add(buttons, BorderLayout.SOUTH);

		btnSave.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				confirmed = true;
				dispose();
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

	private void populateAudioDevices() {
	}
}
