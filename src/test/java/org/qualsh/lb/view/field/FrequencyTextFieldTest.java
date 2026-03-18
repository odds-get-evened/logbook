package org.qualsh.lb.view.field;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.awt.GraphicsEnvironment;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link FrequencyTextField}.
 *
 * <p>Validation regex: {@code \\d{3,6}(\\.\\d{1,2})?}
 * i.e. 3–6 digits, optionally followed by a dot and 1–2 more digits.
 */
public class FrequencyTextFieldTest {

    /** The regex applied on focusLost – mirrors the source code. */
    private static final String FREQ_REGEX = "\\d{3,6}(\\.\\d{1,2})?";

    // ── Validation regex – valid frequencies ──────────────────────────────────

    @ParameterizedTest
    @ValueSource(strings = {"900", "6000", "14025", "100000", "14025.5", "14025.55",
                            "153", "999999"})
    void validFrequencyMatchesRegex(String freq) {
        assertTrue(freq.matches(FREQ_REGEX),
                "Expected '" + freq + "' to be a valid frequency");
    }

    @ParameterizedTest
    @ValueSource(strings = {"12",          // fewer than 3 digits
                            "1234567",     // more than 6 digits
                            "14025.555",   // more than 2 decimal places
                            "14025.",      // trailing dot, no digits after
                            "abc",         // non-numeric
                            "",            // empty
                            "14 025",      // space inside
                            "1402.5.5"})   // double dot
    void invalidFrequencyDoesNotMatchRegex(String freq) {
        assertFalse(freq.matches(FREQ_REGEX),
                "Expected '" + freq + "' to be an invalid frequency");
    }

    // ── Component instantiation (requires display) ────────────────────────────

    @Test
    void instantiatesWithNoArgs() {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(),
                "Skipping Swing instantiation test in headless environment");
        FrequencyTextField field = new FrequencyTextField();
        assertNotNull(field);
    }

    @Test
    void instantiatesWithText() {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(),
                "Skipping Swing instantiation test in headless environment");
        FrequencyTextField field = new FrequencyTextField("9800");
        assertEquals("9800", field.getText());
    }

    @Test
    void instantiatesWithColumns() {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(),
                "Skipping Swing instantiation test in headless environment");
        FrequencyTextField field = new FrequencyTextField(8);
        assertNotNull(field);
    }

    @Test
    void documentMaxLengthIsEight() {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(),
                "Skipping Swing instantiation test in headless environment");
        FrequencyTextField field = new FrequencyTextField();
        // The default constructor sets TextDocument(8)
        org.qualsh.lb.TextDocument doc = (org.qualsh.lb.TextDocument) field.getDocument();
        assertEquals(8, doc.getMaxLength());
    }

    @Test
    void initialTextIsEmpty() {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(),
                "Skipping Swing instantiation test in headless environment");
        FrequencyTextField field = new FrequencyTextField();
        assertEquals("", field.getText());
    }
}
