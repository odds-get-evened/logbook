package org.qualsh.lb.util;

import org.junit.jupiter.api.Test;

import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link Utilities} static helper methods.
 */
public class UtilitiesTest {

    // ── isValidDate ───────────────────────────────────────────────────────────

    @Test
    void isValidDateReturnsFalseForNull() {
        assertFalse(Utilities.isValidDate(null, "HH:mm"));
    }

    @Test
    void isValidDateReturnsTrueForValidTime() {
        assertTrue(Utilities.isValidDate("14:30", "HH:mm"));
    }

    @Test
    void isValidDateReturnsTrueForMidnight() {
        assertTrue(Utilities.isValidDate("00:00", "HH:mm"));
    }

    @Test
    void isValidDateReturnsTrueForEndOfDay() {
        assertTrue(Utilities.isValidDate("23:59", "HH:mm"));
    }

    @Test
    void isValidDateReturnsFalseForInvalidHour() {
        assertFalse(Utilities.isValidDate("25:00", "HH:mm"));
    }

    @Test
    void isValidDateReturnsFalseForInvalidMinute() {
        assertFalse(Utilities.isValidDate("14:60", "HH:mm"));
    }

    @Test
    void isValidDateReturnsFalseForGarbageString() {
        assertFalse(Utilities.isValidDate("not-a-time", "HH:mm"));
    }

    @Test
    void isValidDateReturnsTrueForValidFullDate() {
        assertTrue(Utilities.isValidDate("2024-01-15", "yyyy-MM-dd"));
    }

    @Test
    void isValidDateReturnsFalseForInvalidDay() {
        assertFalse(Utilities.isValidDate("2024-02-30", "yyyy-MM-dd"));
    }

    @Test
    void isValidDateReturnsTrueForSlashDate() {
        assertTrue(Utilities.isValidDate("03/18/2026", "MM/dd/yyyy"));
    }

    // ── unixTimestampToString ─────────────────────────────────────────────────

    @Test
    void unixTimestampToStringProducesCorrectDate() {
        // Unix timestamp 0 = 1970-01-01 00:00:00 UTC
        // Force UTC so the test is timezone-independent
        TimeZone original = TimeZone.getDefault();
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
            String result = Utilities.unixTimestampToString(0, "yyyy-MM-dd");
            assertEquals("1970-01-01", result);
        } finally {
            TimeZone.setDefault(original);
        }
    }

    @Test
    void unixTimestampToStringProducesCorrectTime() {
        TimeZone original = TimeZone.getDefault();
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
            // 3661 seconds = 01:01:01 UTC on 1970-01-01
            String result = Utilities.unixTimestampToString(3661, "HH:mm");
            assertEquals("01:01", result);
        } finally {
            TimeZone.setDefault(original);
        }
    }

    @Test
    void unixTimestampToStringProducesCorrectMMddyyyy() {
        TimeZone original = TimeZone.getDefault();
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
            // 86400 seconds = 1970-01-02
            String result = Utilities.unixTimestampToString(86400, "MM/dd/yyyy");
            assertEquals("01/02/1970", result);
        } finally {
            TimeZone.setDefault(original);
        }
    }

    // ── stringToUnixTimestamp ─────────────────────────────────────────────────

    @Test
    void stringToUnixTimestampRoundTripsWithUnixTimestampToString() {
        TimeZone original = TimeZone.getDefault();
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
            String dateStr = "01/01/1970 00:00";
            int ts = Utilities.stringToUnixTimeStamp(dateStr, "MM/dd/yyyy HH:mm");
            assertEquals(0, ts);
        } finally {
            TimeZone.setDefault(original);
        }
    }

    @Test
    void stringToUnixTimestampForKnownDate() {
        TimeZone original = TimeZone.getDefault();
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
            // 1970-01-02 00:00 UTC = 86400 seconds
            int ts = Utilities.stringToUnixTimeStamp("01/02/1970 00:00", "MM/dd/yyyy HH:mm");
            assertEquals(86400, ts);
        } finally {
            TimeZone.setDefault(original);
        }
    }

    @Test
    void stringToUnixTimestampReturnsZeroForBadInput() {
        int ts = Utilities.stringToUnixTimeStamp("not-a-date", "MM/dd/yyyy HH:mm");
        assertEquals(0, ts);
    }
}
