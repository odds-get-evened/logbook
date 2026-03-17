package org.qualsh.lb.view;

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;
import javax.swing.event.MenuKeyEvent;
import javax.swing.event.MenuKeyListener;

import org.qualsh.lb.MainWin;

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
		
		setMenuTools(new JMenu("Tools"));
		add(getMenuTools());

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
		
		setMenuItemExit(new JMenuItem("Exit"));
		this.menuFile.add(getMenuItemExit());
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
