package org.qualsh.lb.view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Window;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

/**
 * Simple modal calendar date-picker dialog.
 * Shows a monthly grid; clicking a day sets the selected date and closes.
 */
public class DatePickerDialog extends JDialog {

    private static final long serialVersionUID = 1L;

    private static final String[] MONTHS = {
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    };
    private static final String[] DAY_HEADERS = { "Su", "Mo", "Tu", "We", "Th", "Fr", "Sa" };

    private final Calendar calendar;
    private Date selectedDate = null;

    private final JLabel monthYearLabel;
    private final JPanel daysPanel;

    /**
     * @param owner       parent window (for modal centering)
     * @param initialDate date to pre-select; if null, defaults to today (UTC)
     */
    public DatePickerDialog(Window owner, Date initialDate) {
        super(owner, "Select Date", ModalityType.APPLICATION_MODAL);

        calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        if (initialDate != null) {
            calendar.setTime(initialDate);
        }

        JPanel root = new JPanel(new BorderLayout(4, 4));
        root.setBorder(new EmptyBorder(8, 8, 8, 8));
        setContentPane(root);

        // Navigation row
        JPanel nav = new JPanel(new BorderLayout(4, 0));
        JButton prevBtn = new JButton("\u2039"); // ‹
        JButton nextBtn = new JButton("\u203a"); // ›
        prevBtn.setFont(prevBtn.getFont().deriveFont(Font.BOLD, 14f));
        nextBtn.setFont(nextBtn.getFont().deriveFont(Font.BOLD, 14f));
        prevBtn.setMargin(new Insets(2, 6, 2, 6));
        nextBtn.setMargin(new Insets(2, 6, 2, 6));
        monthYearLabel = new JLabel("", SwingConstants.CENTER);
        monthYearLabel.setFont(monthYearLabel.getFont().deriveFont(Font.BOLD, 12f));
        nav.add(prevBtn, BorderLayout.WEST);
        nav.add(monthYearLabel, BorderLayout.CENTER);
        nav.add(nextBtn, BorderLayout.EAST);
        root.add(nav, BorderLayout.NORTH);

        // Days grid: 7 header + up to 6 week rows
        daysPanel = new JPanel(new GridLayout(0, 7, 2, 2));
        root.add(daysPanel, BorderLayout.CENTER);

        prevBtn.addActionListener(e -> {
            calendar.add(Calendar.MONTH, -1);
            refreshDays();
        });
        nextBtn.addActionListener(e -> {
            calendar.add(Calendar.MONTH, 1);
            refreshDays();
        });

        refreshDays();
        pack();
        setResizable(false);
        setLocationRelativeTo(owner);
    }

    private void refreshDays() {
        daysPanel.removeAll();

        // Day-of-week header row
        for (String h : DAY_HEADERS) {
            JLabel lbl = new JLabel(h, SwingConstants.CENTER);
            lbl.setFont(lbl.getFont().deriveFont(Font.BOLD, 11f));
            lbl.setForeground(Color.DARK_GRAY);
            daysPanel.add(lbl);
        }

        monthYearLabel.setText(MONTHS[calendar.get(Calendar.MONTH)] + "  " + calendar.get(Calendar.YEAR));

        // Clone to find the first day of the displayed month
        Calendar c = (Calendar) calendar.clone();
        c.set(Calendar.DAY_OF_MONTH, 1);
        int firstDow = c.get(Calendar.DAY_OF_WEEK) - 1; // 0 = Sunday
        int daysInMonth = c.getActualMaximum(Calendar.DAY_OF_MONTH);
        int selectedDay = calendar.get(Calendar.DAY_OF_MONTH);

        // Empty padding before the first day
        for (int i = 0; i < firstDow; i++) {
            daysPanel.add(new JLabel(""));
        }

        // Day buttons
        for (int day = 1; day <= daysInMonth; day++) {
            final int d = day;
            JButton btn = new JButton(String.valueOf(day));
            btn.setMargin(new Insets(2, 2, 2, 2));
            btn.setFont(btn.getFont().deriveFont(11f));
            if (day == selectedDay) {
                btn.setBackground(new Color(60, 120, 210));
                btn.setForeground(Color.WHITE);
                btn.setOpaque(true);
            }
            btn.addActionListener(e -> {
                Calendar sel = (Calendar) calendar.clone();
                sel.set(Calendar.DAY_OF_MONTH, d);
                selectedDate = sel.getTime();
                dispose();
            });
            daysPanel.add(btn);
        }

        // Pad the last partial week row
        int filled = firstDow + daysInMonth;
        int remainder = filled % 7;
        if (remainder != 0) {
            for (int i = remainder; i < 7; i++) {
                daysPanel.add(new JLabel(""));
            }
        }

        daysPanel.revalidate();
        daysPanel.repaint();
        pack();
    }

    /** Returns the date the user clicked, or {@code null} if the dialog was closed without selection. */
    public Date getSelectedDate() {
        return selectedDate;
    }
}
