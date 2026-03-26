package org.qualsh.lb.digitalmodes.view;

import org.qualsh.lb.digital.decode.DecodeResult;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import java.awt.*;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * A read-only text area that displays real-time decoded text as it arrives
 * from any active decoder.
 *
 * <p>Text is rendered in a green-on-black terminal style using a monospaced
 * font. Each decoded result is appended as a single formatted line and the
 * view automatically scrolls to the bottom so the most recent output is
 * always visible.
 *
 * <p>To prevent unbounded memory growth during long decode sessions the area
 * retains at most {@value #MAX_LINES} lines; older lines are silently removed
 * from the top when the limit is exceeded.
 *
 * <p>All mutations to the underlying document are dispatched on the Swing
 * Event Dispatch Thread, making it safe to call the public methods from any
 * thread.
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
     * Creates a new {@code DecodeTextArea} with terminal-style appearance.
     * The area is read-only; users cannot type into it.
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
     * Formats a {@link DecodeResult} and appends it as a single line to the
     * display.
     *
     * <p>The line is written in the form:
     * <pre>[HH:mm:ss] [MODE-id] freq Hz | decoded text</pre>
     * where the time is derived from the result's Unix timestamp, the mode
     * identifier is the numeric digital-mode ID stored in the result, the
     * frequency is the audio offset in hertz formatted to one decimal place,
     * and the text is the decoded message.
     *
     * <p>After appending, the oldest lines are trimmed if the total exceeds
     * {@value #MAX_LINES}, and the view scrolls to show the newest entry.
     *
     * <p>This method is safe to call from any thread.
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
     * Appends a raw string directly to the display without any formatting.
     *
     * <p>Use this for status messages such as {@code "--- Listening ---"} or
     * {@code "--- File loaded ---"} that are not associated with a specific
     * decode result.
     *
     * <p>This method is safe to call from any thread.
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
     * Removes all text from the display.
     *
     * <p>This method is safe to call from any thread.
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
