package org.qualsh.lb.view;

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;
import javax.swing.event.MenuKeyEvent;
import javax.swing.event.MenuKeyListener;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.qualsh.lb.MainWin;
import org.qualsh.lb.util.ExportUtil;

public class LogMenuBar extends JMenuBar implements MenuKeyListener {
	
	private JFrame mainFrame;
	private JMenu menuFile;
	private JMenuItem menuItemExit;
	private JMenu menuTools;
	private JMenu menuStation; 
	private JMenuItem menuItemNewStation;
	private JMenu menuLocation;
	private JMenuItem menuItemNewLocation;
	private LogInteraction logInteraction;
	private JMenu menuEdit;
	private JMenuItem menuItemPreferences;
	protected PreferencesDialog prefDialog;
	protected AboutDialog aboutDialog;
	
	public AboutDialog getAboutDialog() {
		return aboutDialog;
	}

	public void setAboutDialog(AboutDialog aboutDialog) {
		this.aboutDialog = aboutDialog;
	}

	protected JMenu menuHelp;
	protected JMenuItem menuItemAbout;

	public JMenu getMenuHelp() {
		return menuHelp;
	}

	public void setMenuHelp(JMenu menuHelp) {
		this.menuHelp = menuHelp;
		this.setMenuItemAbout(new JMenuItem("About"));
		this.getMenuHelp().add(this.getMenuItemAbout());
	}

	public JMenuItem getMenuItemAbout() {
		return menuItemAbout;
	}

	public void setMenuItemAbout(JMenuItem menuItemAbout) {
		this.menuItemAbout = menuItemAbout;
		this.menuItemAbout.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				LogMenuBar.this.getAboutDialog().setVisible(true);
			}
			
		});
	}

	private static final long serialVersionUID = 334445486128104212L;
	
	public LogMenuBar(JFrame frame) {
		super();

		setMainFrame(frame);

		setMenuFile(new JMenu("File"));
		add(getMenuFile());

		setMenuEdit(new JMenu("Edit"));
		add(getMenuEdit());

		JMenu menuTools = new JMenu("Tools");
		add(menuTools);
		JMenuItem menuItemCatSettings = new JMenuItem("CAT Settings\u2026");
		menuItemCatSettings.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				CATSettingsDialog dlg = new CATSettingsDialog((JFrame) LogMenuBar.this.getMainFrame());
				dlg.setVisible(true);
			}
		});
		menuTools.add(menuItemCatSettings);

		this.setMenuHelp(new JMenu("Help"));
		add(this.getMenuHelp());

		this.setPrefDialog(new PreferencesDialog(this.getMainFrame()));
		this.setAboutDialog(new AboutDialog(this.getMainFrame()));
	}

	private void setMenuEdit(JMenu jMenu) {
		this.menuEdit = jMenu;
		this.setMenuItemPreferences(new JMenuItem("Preferences"));
		this.getMenuEdit().add(this.getMenuItemPreferences());
	}

	private JMenuItem getMenuItemPreferences() {
		return this.menuItemPreferences;
	}

	private void setMenuItemPreferences(JMenuItem jMenuItem) {
		this.menuItemPreferences = jMenuItem;
		this.getMenuItemPreferences().addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				LogMenuBar.this.getPrefDialog().setVisible(true);
			}
			
		});
	}

	public JFrame getMainFrame() {
		return mainFrame;
	}

	public void setMainFrame(JFrame mainFrame) {
		this.mainFrame = mainFrame;
	}

	public JMenu getMenuFile() {
		return menuFile;
	}

	public void setMenuFile(JMenu menuFile) {
		this.menuFile = menuFile;

		// Export submenu
		JMenu menuExport = new JMenu("Export…");
		this.menuFile.add(menuExport);

		JMenuItem menuItemExportCsv = new JMenuItem("CSV");
		menuItemExportCsv.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				File f = chooseExportFile("csv", "CSV Files (*.csv)", "logbook_export.csv");
				if (f == null) return;
				try {
					ExportUtil.exportCsv(f);
					JOptionPane.showMessageDialog(getMainFrame(), "Exported successfully to:\n" + f.getAbsolutePath(), "Export Complete", JOptionPane.INFORMATION_MESSAGE);
				} catch (Exception ex) {
					JOptionPane.showMessageDialog(getMainFrame(), "Export failed: " + ex.getMessage(), "Export Error", JOptionPane.ERROR_MESSAGE);
				}
			}
		});
		menuExport.add(menuItemExportCsv);

		JMenuItem menuItemExportJson = new JMenuItem("JSON");
		menuItemExportJson.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				File f = chooseExportFile("json", "JSON Files (*.json)", "logbook_export.json");
				if (f == null) return;
				try {
					ExportUtil.exportJson(f);
					JOptionPane.showMessageDialog(getMainFrame(), "Exported successfully to:\n" + f.getAbsolutePath(), "Export Complete", JOptionPane.INFORMATION_MESSAGE);
				} catch (Exception ex) {
					JOptionPane.showMessageDialog(getMainFrame(), "Export failed: " + ex.getMessage(), "Export Error", JOptionPane.ERROR_MESSAGE);
				}
			}
		});
		menuExport.add(menuItemExportJson);

		JMenuItem menuItemExportAdif = new JMenuItem("ADIF");
		menuItemExportAdif.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				File f = chooseExportFile("adi", "ADIF Files (*.adi)", "logbook_export.adi");
				if (f == null) return;
				try {
					ExportUtil.exportAdif(f);
					JOptionPane.showMessageDialog(getMainFrame(), "Exported successfully to:\n" + f.getAbsolutePath(), "Export Complete", JOptionPane.INFORMATION_MESSAGE);
				} catch (Exception ex) {
					JOptionPane.showMessageDialog(getMainFrame(), "Export failed: " + ex.getMessage(), "Export Error", JOptionPane.ERROR_MESSAGE);
				}
			}
		});
		menuExport.add(menuItemExportAdif);

		this.menuFile.addSeparator();
		setMenuItemExit(new JMenuItem("Exit"));
		this.menuFile.add(getMenuItemExit());
	}

	private File chooseExportFile(String ext, String desc, String defaultName) {
		JFileChooser fc = new JFileChooser();
		fc.setDialogTitle("Export Logs");
		fc.setFileFilter(new FileNameExtensionFilter(desc, ext));
		fc.setSelectedFile(new File(System.getProperty("user.home"), defaultName));
		int result = fc.showSaveDialog(getMainFrame());
		if (result != JFileChooser.APPROVE_OPTION) return null;
		File f = fc.getSelectedFile();
		// Append extension if missing
		if (!f.getName().contains(".")) {
			f = new File(f.getAbsolutePath() + "." + ext);
		}
		return f;
	}

	public JMenuItem getMenuItemExit() {
		return menuItemExit;
	}

	public void setMenuItemExit(JMenuItem menuItemExit) {
		this.menuItemExit = menuItemExit;
		this.menuItemExit.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_X, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
		this.menuItemExit.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				MainWin mainWin = (MainWin) LogMenuBar.this.getMainFrame();
				mainWin.exit(mainWin);
			}
			
		});
	}

	public JMenu getMenuTools() {
		return menuTools;
	}
	
	public void setMenuTools(JMenu menuTools) {
		this.menuTools = menuTools;
	}

	public JMenu getMenuStation() {
		return menuStation;
	}

	public void setMenuStation(JMenu menuStation) {
		this.menuStation = menuStation;
		
		this.setMenuItemNewStation(new JMenuItem("New..."));
		this.getMenuItemNewStation().setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
		this.getMenuItemNewStation().addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				NewStationDialog stationDialog = new NewStationDialog(LogMenuBar.this.getMainFrame());
				stationDialog.setLogInteraction(getLogInteraction());
				stationDialog.setVisible(true);
			}
			
		});
		
		
		
		this.menuStation.add(this.getMenuItemNewStation());
	}

	public JMenuItem getMenuItemNewStation() {
		return menuItemNewStation;
	}

	public void setMenuItemNewStation(JMenuItem menuItemNewStation) {
		this.menuItemNewStation = menuItemNewStation;
	}

	public JMenu getMenuLocation() {
		return menuLocation;
	}

	public void setMenuLocation(JMenu menuLocation) {
		this.menuLocation = menuLocation;
		
		this.setMenuItemNewLocation(new JMenuItem("New..."));
		//this.getMenuItemNewLocation().setMnemonic(KeyEvent.VK_L);
		this.getMenuItemNewLocation().setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_L, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
		this.getMenuItemNewLocation().addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				NewLocationDialog nld = new NewLocationDialog(LogMenuBar.this.getMainFrame());
				nld.setVisible(true);
			}
			
		});
		this.menuLocation.add(getMenuItemNewLocation());
	}

	public JMenuItem getMenuItemNewLocation() {
		return menuItemNewLocation;
	}

	public void setMenuItemNewLocation(JMenuItem menuItemNewLocation) {
		this.menuItemNewLocation = menuItemNewLocation;
	}

	public void menuKeyTyped(MenuKeyEvent e) {}

	public void menuKeyPressed(MenuKeyEvent e) {}

	public void menuKeyReleased(MenuKeyEvent e) {}

	public void setLogInteraction(LogInteraction logInteraction) {
		this.logInteraction = logInteraction;
	}

	public LogInteraction getLogInteraction() {
		return logInteraction;
	}

	public JMenu getMenuEdit() {
		return menuEdit;
	}

	public PreferencesDialog getPrefDialog() {
		return prefDialog;
	}

	public void setPrefDialog(PreferencesDialog prefDialog) {
		this.prefDialog = prefDialog;
	}

}
