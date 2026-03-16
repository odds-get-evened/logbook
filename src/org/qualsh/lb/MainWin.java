package org.qualsh.lb;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GraphicsConfiguration;
import java.awt.HeadlessException;
import java.awt.Image;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.Toolkit;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import org.qualsh.lb.data.Data;
import org.qualsh.lb.data.LogsModel;
import org.qualsh.lb.data.ViewLocationsModel;
import org.qualsh.lb.data.ViewPlacesModel;
import org.qualsh.lb.util.Debugger;
import org.qualsh.lb.view.LogInteraction;
import org.qualsh.lb.view.LogMenuBar;
import org.qualsh.lb.view.LogsPanel;
import org.qualsh.lb.view.MapPanel;
import org.qualsh.lb.view.NewStationDialog;

public class MainWin extends JFrame {
	
	private static final long serialVersionUID = -7871415454437016879L;
	private static final String icoFile = "LogBook.ico";
	private Debugger debugger;
	private LogInteraction logInteraction;
	private LogsPanel logsPanel;
	private LogMenuBar logMenuBar;
	private MapPanel mapPanel;

	public MainWin() throws HeadlessException {
		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		this.setResizable(true);

		setDebugger(new Debugger());

		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		int winW = 1200;
		int winH = (int) (screenSize.getHeight() * 0.8);
		this.setSize(winW, winH);
		this.setMinimumSize(new Dimension(800, 600));
		
		int winX = (int) ((screenSize.getWidth() - this.getSize().getWidth()) / 2);
		int winY = (int) ((screenSize.getHeight() - this.getSize().getHeight()) / 2);
		setLocation(winX, winY);
		
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (UnsupportedLookAndFeelException e) {
			e.printStackTrace();
		}
		
		this.setTitle("Log Book v." + App.VERSION);
		
		this.setupIcons();
		
		this.addMenu(); // LogMenuBar instance
		
		this.addWindowListener(new WindowListener() {

			public void windowActivated(WindowEvent arg0) {}

			public void windowClosed(WindowEvent arg0) {}

			public void windowClosing(WindowEvent e) {
				MainWin.this.exit(MainWin.this);
			}

			public void windowDeactivated(WindowEvent arg0) {}

			public void windowDeiconified(WindowEvent arg0) {}

			public void windowIconified(WindowEvent arg0) {}

			public void windowOpened(WindowEvent arg0) {}
			
		});
		
		this.getContentPane().setLayout(new BorderLayout(0, 0));
		
		final JPanel leftPanel = new JPanel();
		leftPanel.setLayout(new BorderLayout(0, 0));
		this.getContentPane().add(leftPanel, BorderLayout.CENTER);
		
		final JPanel rightPanel = new JPanel();
		rightPanel.setLayout(new BorderLayout(0, 0));
		rightPanel.setPreferredSize(new Dimension(480, this.getSize().height));
		this.getContentPane().add(rightPanel, BorderLayout.EAST);
		
		logsPanel = new LogsPanel();
		mapPanel = new MapPanel();

		// Split left panel: logs table on top, map below
		// map = 2/3 of logs table → logs=60%, map=40%
		JSplitPane leftSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, logsPanel, mapPanel);
		leftSplit.setResizeWeight(0.60);
		leftSplit.setBorder(null);
		leftPanel.add(leftSplit, BorderLayout.CENTER);

		this.setLogInteraction(new LogInteraction());
		this.getLogInteraction().setLogTable(logsPanel.getLogsTable());
		this.getLogInteraction().setMainWin(this);
		this.getLogInteraction().setPreferencesDialog(this.getLogMenuBar().getPrefDialog());
		this.getLogInteraction().getPreferencesDialog().setLogInteraction(this.getLogInteraction());

		logsPanel.getLogsTable().setLogInteraction(this.getLogInteraction());
		logsPanel.getLogsTable().setMapPanel(mapPanel);
		this.getLogInteraction().getLocationsTab().setMapPanel(mapPanel);
		
		rightPanel.add(this.getLogInteraction(), BorderLayout.CENTER);
		
		this.getLogMenuBar().setLogInteraction(this.getLogInteraction());

		// Plot all existing logs on the map and keep it refreshed on changes
		LogsModel logsModel = (LogsModel) logsPanel.getLogsTable().getModel();
		mapPanel.plotLogs(logsModel.getData());
		logsModel.addTableModelListener(e -> mapPanel.plotLogs(logsModel.getData()));

		// Load all locations/places from DB for the "All Stations" layer
		ViewLocationsModel allLocModel = new ViewLocationsModel();
		allLocModel.setAllLocations();
		ViewPlacesModel allPlacesModel = new ViewPlacesModel();
		allPlacesModel.setAllPlaces();
		mapPanel.plotAllLocations(allLocModel.getData(), allPlacesModel.getData());

		KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(new KeyEventDispatcher() {

			public boolean dispatchKeyEvent(KeyEvent e) {
				
				// quit application
				if(e.getKeyCode() == KeyEvent.VK_Q && e.getModifiers() == InputEvent.CTRL_MASK) {
					MainWin.this.exit(MainWin.this);
				}
				
				// open up new station dialog
				if(e.getKeyCode() == KeyEvent.VK_S && e.getModifiers() == InputEvent.CTRL_MASK) {
					NewStationDialog stationDialog = new NewStationDialog(MainWin.this);
					stationDialog.setLogInteraction(MainWin.this.getLogInteraction());
					stationDialog.setVisible(true);
				}
				
				return false;
			}
			
		});
		
		setVisible(true);
		SwingUtilities.invokeLater(() -> leftSplit.setDividerLocation(0.60));
	}

	private void setupIcons() {
		List<Image> images = new ArrayList<Image>();

		String[] sizes = {"/imgs/lb_16x16.png", "/imgs/lb_32x32.png", "/imgs/lb_48x48.png", "/imgs/lb_128x128.png"};
		for (String path : sizes) {
			java.net.URL url = App.class.getResource(path);
			if (url != null) {
				images.add(Toolkit.getDefaultToolkit().getImage(url));
			}
		}

		if (!images.isEmpty()) {
			setIconImages(images);
		}
	}

	private void addMenu() {
		this.setLogMenuBar(new LogMenuBar(this));
		
		setJMenuBar(this.getLogMenuBar());
	}

	public MainWin(GraphicsConfiguration gc) {
		super(gc);
	}

	public MainWin(String title) throws HeadlessException {
		super(title);
	}

	public MainWin(String title, GraphicsConfiguration gc) {
		super(title, gc);
	}

	public static String getIcofile() {
		return icoFile;
	}

	public Debugger getDebugger() {
		return debugger;
	}

	public void setDebugger(Debugger debugger) {
		this.debugger = debugger;
	}

	public LogsPanel getLogsPanel() {
		return logsPanel;
	}

	public void setLogsPanel(LogsPanel logsPanel) {
		this.logsPanel = logsPanel;
	}

	public LogInteraction getLogInteraction() {
		return logInteraction;
	}

	public void setLogInteraction(LogInteraction logInteraction) {
		this.logInteraction = logInteraction;
	}

	public LogMenuBar getLogMenuBar() {
		return logMenuBar;
	}

	public void setLogMenuBar(LogMenuBar logMenuBar) {
		this.logMenuBar = logMenuBar;
	}
	
	public MapPanel getMapPanel() {
		return mapPanel;
	}

	public void setMapPanel(MapPanel mapPanel) {
		this.mapPanel = mapPanel;
	}

	public void exit(JFrame jframe) {
		int confirmed = JOptionPane.showConfirmDialog(jframe, "Are you sure you want to quit?", "Confirm quit", JOptionPane.YES_NO_OPTION);
		
		if(confirmed == JOptionPane.YES_OPTION) {
			Frame[] frames = Frame.getFrames();
			
			for(Frame frame : frames) {
				frame.dispose();
			}
			
			try {
				Data.getConnection().close();
			} catch (SQLException e1) {
				e1.printStackTrace();
			}
			
			System.exit(0);
		}
	}

}
