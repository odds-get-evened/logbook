package org.qualsh.lb.view.field;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.awt.GraphicsEnvironment;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link GenericTimeField}.
 *
 * <p>GenericTimeField validates text against {@code \\d{2}:\\d{2}} (strict two
 * digits for both hours and minutes) and also uses
 * {@link org.qualsh.lb.util.Utilities#isValidDate} for semantic correctness.
 * An empty field is replaced with {@code "00:00"} on focus-lost.
 */
public class GenericTimeFieldTest {

    /** Strict two-digit-colon-two-digit pattern from focusLost. */
    private static final String STRICT_TIME_REGEX = "\\d{2}:\\d{2}";

    // ── Validation regex (strict) ─────────────────────────────────────────────

    @ParameterizedTest
    @ValueSource(strings = {"00:00", "14:30", "23:59", "09:05", "12:00"})
    void validStrictTimePatternsMatch(String time) {
        assertTrue(time.matches(STRICT_TIME_REGEX),
                "Expected '" + time + "' to match strict HH:mm pattern");
    }

    @ParameterizedTest
    @ValueSource(strings = {"",        // empty
                            "9:30",    // single-digit hour
                            "14:3",    // single-digit minute
                            "abc",     // non-numeric
                            "14:300",  // three-digit minutes
                            "1430",    // no colon
                            ":30",     // missing hour
                            "14:"})    // missing minutes
    void invalidStrictTimePatternsDoNotMatch(String time) {
        assertFalse(time.matches(STRICT_TIME_REGEX),
                "Expected '" + time + "' not to match strict HH:mm pattern");
    }

    // ── Semantic time validation (via Utilities.isValidDate) ──────────────────

    @Test
    void invalidHour25IsRejectedSemanticCheck() {
        // "25:00" passes the structural regex but Utilities.isValidDate rejects it
        String time = "25:00";
        boolean passesRegex = time.matches(STRICT_TIME_REGEX);
        boolean passesSemantics = org.qualsh.lb.util.Utilities.isValidDate(time, "HH:mm");
        assertTrue(passesRegex, "Structural regex should pass");
        assertFalse(passesSemantics, "Semantic validation should reject 25:00");
    }

    @Test
    void invalidMinute60IsRejectedSemanticCheck() {
        String time = "14:60";
        boolean passesRegex = time.matches(STRICT_TIME_REGEX);
        boolean passesSemantics = org.qualsh.lb.util.Utilities.isValidDate(time, "HH:mm");
        assertTrue(passesRegex);
        assertFalse(passesSemantics);
    }

    @Test
    void validTimePassesBothChecks() {
        String time = "14:30";
        assertTrue(time.matches(STRICT_TIME_REGEX));
        assertTrue(org.qualsh.lb.util.Utilities.isValidDate(time, "HH:mm"));
    }

    // ── Component instantiation ───────────────────────────────────────────────

    @Test
    void instantiatesWithNoArgs() {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(),
                "Skipping Swing instantiation test in headless environment");
        GenericTimeField field = new GenericTimeField();
        assertNotNull(field);
    }

    @Test
    void instantiatesWithTextArg() {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(),
                "Skipping Swing instantiation test in headless environment");
        GenericTimeField field = new GenericTimeField("14:30");
        assertEquals("14:30", field.getText());
    }

    @Test
    void documentMaxLengthIsFive() {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(),
                "Skipping Swing instantiation test in headless environment");
        GenericTimeField field = new GenericTimeField();
        org.qualsh.lb.TextDocument doc = (org.qualsh.lb.TextDocument) field.getDocument();
        assertEquals(5, doc.getMaxLength());
    }
}
