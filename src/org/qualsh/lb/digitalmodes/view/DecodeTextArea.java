package org.qualsh.lb.digitalmodes.view;

import org.qualsh.lb.digital.decode.DecodeResult;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import java.awt.*;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * The scrolling decode output area that shows decoded signals as they arrive.
 *
 * <p>Decoded messages appear in green-on-black terminal style, one per line, with the newest
 * at the bottom. The display automatically scrolls to keep the latest decode in view. To keep
 * memory usage sensible during long sessions, only the most recent {@value #MAX_LINES} lines
 * are kept; older lines are quietly removed from the top.
 *
 * @author Logbook Development Team
 * @version 1.0
 */
public class DecodeTextArea extends JTextArea {

    /** Maximum number of lines retained before the oldest lines are trimmed. */
    private static final int MAX_LINES = 500;

    /** Monospaced font used for all displayed text. */
    private static final Font DISPLAY_FONT = new Font(Font.MONOSPACED, Font.PLAIN, 12);

    /** Near-black background colour for the terminal aesthetic. */
    private static final Color BACKGROUND_COLOR = new Color(15, 15, 15);

    /** Green foreground colour for the terminal aesthetic. */
    private static final Color TEXT_COLOR = new Color(0, 220, 0);

    /** Caret colour matching the foreground so it remains visible. */
    private static final Color CARET_COLOR = new Color(0, 220, 0);

    private static final DateTimeFormatter TIME_FMT =
            DateTimeFormatter.ofPattern("HH:mm:ss");

    /**
     * Creates a new decode output area with terminal-style appearance. The area is read-only.
     */
    public DecodeTextArea() {
        super();
        setEditable(false);
        setLineWrap(true);
        setWrapStyleWord(true);
        setFont(DISPLAY_FONT);
        setBackground(BACKGROUND_COLOR);
        setForeground(TEXT_COLOR);
        setCaretColor(CARET_COLOR);
        setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
    }

    /**
     * Adds a decoded signal to the output area.
     *
     * <p>The result is formatted as a single line showing the time, mode, frequency, and decoded
     * text, then appended at the bottom of the display. The view scrolls automatically to show
     * the new entry.
     *
     * @param result the decoded result to display; must not be {@code null}
     */
    public void appendDecodeResult(DecodeResult result) {
        String time = Instant.ofEpochSecond(result.getTimestamp())
                .atZone(ZoneId.systemDefault())
                .toLocalTime()
                .format(TIME_FMT);
        String mode = "MODE-" + result.getDigitalModeId();
        String line = String.format("[%s] [%s] %.1f Hz | %s%n",
                time, mode, result.getFrequencyOffsetHz(), result.getMessage());

        SwingUtilities.invokeLater(() -> {
            append(line);
            trimToMaxLines();
            scrollToBottom();
        });
    }

    /**
     * Appends a plain status message to the display, such as {@code "--- File loaded ---"}.
     *
     * <p>Use this for informational text that is not a decoded signal result.
     *
     * @param text the string to append; must not be {@code null}
     */
    public void appendText(String text) {
        SwingUtilities.invokeLater(() -> {
            append(text);
            scrollToBottom();
        });
    }

    /**
     * Clears all text from the decode output area.
     */
    public void clearDisplay() {
        SwingUtilities.invokeLater(() -> setText(""));
    }

    /**
     * Removes lines from the top of the display until the total line count is
     * at or below {@value #MAX_LINES}.
     *
     * <p>Must be called on the Swing EDT.
     */
    private void trimToMaxLines() {
        int lineCount = getLineCount();
        if (lineCount <= MAX_LINES) {
            return;
        }
        int linesToRemove = lineCount - MAX_LINES;
        try {
            int endOffset = getLineEndOffset(linesToRemove - 1);
            getDocument().remove(0, endOffset);
        } catch (BadLocationException e) {
            // Document state changed between check and removal; safe to ignore.
        }
    }

    /**
     * Moves the caret to the end of the document, causing the scroll pane (if
     * any) to reveal the most recently appended text.
     *
     * <p>Must be called on the Swing EDT.
     */
    private void scrollToBottom() {
        setCaretPosition(getDocument().getLength());
    }
}
