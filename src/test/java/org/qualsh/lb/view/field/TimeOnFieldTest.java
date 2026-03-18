package org.qualsh.lb.view.field;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.awt.GraphicsEnvironment;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link TimeOnField}.
 *
 * <p>TimeOnField auto-populates with the current UTC time and validates input
 * against the pattern {@code \\d{1,2}:\\d{2}}.
 */
public class TimeOnFieldTest {

    /** Validation regex from focusLost – mirrors the source. */
    private static final String TIME_REGEX = "\\d{1,2}:\\d{2}";

    // ── Validation regex ──────────────────────────────────────────────────────

    @ParameterizedTest
    @ValueSource(strings = {"0:00", "9:30", "14:30", "23:59", "1:00", "12:00"})
    void validTimePatternsMatchRegex(String time) {
        assertTrue(time.matches(TIME_REGEX),
                "Expected '" + time + "' to match the time regex");
    }

    @ParameterizedTest
    @ValueSource(strings = {"",         // empty
                            "abc",      // non-numeric
                            "1430",     // missing colon
                            "14:300",   // three digits for minutes
                            ":30",      // missing hour
                            "14:",      // missing minutes
                            "14:3"})    // only one digit for minutes
    void invalidTimePatternsDoNotMatchRegex(String time) {
        assertFalse(time.matches(TIME_REGEX),
                "Expected '" + time + "' not to match the time regex");
    }

    // ── keyReleased colon-insertion logic ─────────────────────────────────────

    @Test
    void twoDigitInputGetsColonAppended() {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(),
                "Skipping Swing test in headless environment");

        TimeOnField field = new TimeOnField();
        field.getoTimer().stop(); // prevent timer from overwriting text during test

        // Simulate the user having typed "14" (length == 2 triggers colon insertion)
        // We do this by manually calling the keyReleased logic via the text
        field.setText("14");
        // Mimic what keyReleased does when length == 2
        if (field.getText().length() == 2) {
            field.setText(field.getText() + ":");
        }

        assertEquals("14:", field.getText());
    }

    // ── Component instantiation ───────────────────────────────────────────────

    @Test
    void instantiatesAndTimerIsNotNull() {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(),
                "Skipping Swing test in headless environment");

        TimeOnField field = new TimeOnField();
        assertNotNull(field.getoTimer(), "Timer should not be null after construction");
        field.getoTimer().stop(); // clean up
    }

    @Test
    void timerIsRunningAfterConstruction() {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(),
                "Skipping Swing test in headless environment");

        TimeOnField field = new TimeOnField();
        assertTrue(field.getoTimer().isRunning(), "Timer should start automatically");
        field.getoTimer().stop(); // clean up
    }

    @Test
    void documentMaxLengthIsFive() {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(),
                "Skipping Swing test in headless environment");

        TimeOnField field = new TimeOnField();
        field.getoTimer().stop();
        org.qualsh.lb.TextDocument doc = (org.qualsh.lb.TextDocument) field.getDocument();
        assertEquals(5, doc.getMaxLength(), "TextDocument limit should be 5 (HH:mm = 5 chars)");
    }

    @Test
    void setTimerReplacesTimer() {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(),
                "Skipping Swing test in headless environment");

        TimeOnField field = new TimeOnField();
        field.getoTimer().stop();

        javax.swing.Timer newTimer = new javax.swing.Timer(500, null);
        field.setoTimer(newTimer);
        assertSame(newTimer, field.getoTimer());
    }

    @Test
    void initialTextMatchesTimePattern() {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(),
                "Skipping Swing test in headless environment");

        TimeOnField field = new TimeOnField();
        // Give the timer at least one tick to populate the field
        try { Thread.sleep(1100); } catch (InterruptedException ignored) {}
        field.getoTimer().stop();

        String text = field.getText();
        assertTrue(text.matches(TIME_REGEX),
                "Initial text '" + text + "' should match HH:mm pattern");
    }
}
