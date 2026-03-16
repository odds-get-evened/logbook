package org.qualsh.lb;

import javax.swing.UIManager;
import org.qualsh.lb.data.Data;
import org.qualsh.lb.util.Preferences;

public class App {
	public static final String VERSION = "0.0.7";

	public App() {}

	public static void main(String[] args) {
		/**
		 * this gets rid of exception for not using native acceleration
		 */
		System.setProperty("com.sun.media.jai.disableMediaLib", "true");

		// establish DB file stuff first
		new Data();

		// Apply saved UI theme before creating any windows
		String savedTheme = Preferences.getOne(Preferences.PREF_NAME_THEME);
		try {
			if (savedTheme != null && !savedTheme.equals("system")) {
				UIManager.setLookAndFeel(savedTheme);
			} else {
				UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		new MainWin();
	}

}
