package org.qualsh.lb.view.field;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.awt.GraphicsEnvironment;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link CoordinateTextField}.
 *
 * <p>Latitude regex:  {@code -?\\d{1,2}(\\.\\d{1,6})?}
 * <p>Longitude regex: {@code -?\\d{1,3}(\\.\\d{1,6})?}
 */
public class CoordinateTextFieldTest {

    // ── Latitude regex ────────────────────────────────────────────────────────

    @ParameterizedTest
    @ValueSource(strings = {"0", "5", "51", "51.5", "51.123456", "-90", "-45.5",
                            "0.000001", "99.999999"})
    void validLatitudeMatchesRegex(String lat) {
        assertTrue(lat.matches(CoordinateTextField.getRegexlat()),
                "Expected '" + lat + "' to be a valid latitude");
    }

    @ParameterizedTest
    @ValueSource(strings = {"100",          // 3 digits before decimal
                            "abc",          // non-numeric
                            "51.1234567",   // 7 decimal places (exceeds 6)
                            "51.",          // trailing dot, no digits after
                            ""})            // empty string
    void invalidLatitudeDoesNotMatchRegex(String lat) {
        assertFalse(lat.matches(CoordinateTextField.getRegexlat()),
                "Expected '" + lat + "' to be an invalid latitude");
    }

    // ── Longitude regex ───────────────────────────────────────────────────────

    @ParameterizedTest
    @ValueSource(strings = {"0", "5", "51", "180", "180.0", "-180.5", "0.123456",
                            "999.999999", "-0.5"})
    void validLongitudeMatchesRegex(String lng) {
        assertTrue(lng.matches(CoordinateTextField.getRegexLng()),
                "Expected '" + lng + "' to be a valid longitude");
    }

    @ParameterizedTest
    @ValueSource(strings = {"1000",         // 4 digits before decimal
                            "180.1234567",  // 7 decimal places (exceeds 6)
                            "abc",          // non-numeric
                            "180.",         // trailing dot
                            ""})            // empty string
    void invalidLongitudeDoesNotMatchRegex(String lng) {
        assertFalse(lng.matches(CoordinateTextField.getRegexLng()),
                "Expected '" + lng + "' to be an invalid longitude");
    }

    // ── isLongitude flag ──────────────────────────────────────────────────────

    @Test
    void isLongitudeDefaultsFalse() {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(),
                "Skipping Swing instantiation test in headless environment");
        CoordinateTextField field = new CoordinateTextField();
        assertFalse(field.getIsLongitude());
    }

    @Test
    void setIsLongitudeTrueRoundTrips() {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(),
                "Skipping Swing instantiation test in headless environment");
        CoordinateTextField field = new CoordinateTextField();
        field.setIsLongitude(true);
        assertTrue(field.getIsLongitude());
    }

    @Test
    void setIsLongitudeFalseRoundTrips() {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(),
                "Skipping Swing instantiation test in headless environment");
        CoordinateTextField field = new CoordinateTextField();
        field.setIsLongitude(true);
        field.setIsLongitude(false);
        assertFalse(field.getIsLongitude());
    }

    @Test
    void instantiatesWithTextArg() {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(),
                "Skipping Swing instantiation test in headless environment");
        CoordinateTextField field = new CoordinateTextField("51.5");
        assertEquals("51.5", field.getText());
    }

    // ── Static regex mutation (unusual usage – verify getter/setter symmetry) ─

    @Test
    void setAndGetRegexLng() {
        String original = CoordinateTextField.getRegexLng();
        try {
            CoordinateTextField.setRegexLng("custom");
            assertEquals("custom", CoordinateTextField.getRegexLng());
        } finally {
            CoordinateTextField.setRegexLng(original); // restore
        }
    }

    @Test
    void setAndGetRegexLat() {
        String original = CoordinateTextField.getRegexlat();
        try {
            CoordinateTextField.setRegexLat("custom");
            assertEquals("custom", CoordinateTextField.getRegexlat());
        } finally {
            CoordinateTextField.setRegexLat(original); // restore
        }
    }
}
