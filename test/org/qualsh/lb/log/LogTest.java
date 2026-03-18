package org.qualsh.lb.log;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the {@link Log} domain model.
 * These tests exercise only the in-memory state of the object; no database
 * connection is required.
 */
public class LogTest {

    private Log log;

    @BeforeEach
    void setUp() {
        log = new Log();
    }

    // ── Default state ─────────────────────────────────────────────────────────

    @Test
    void defaultIdIsZero() {
        assertEquals(0, log.getId());
    }

    @Test
    void defaultDateOnIsZero() {
        assertEquals(0, log.getDateOn());
    }

    @Test
    void defaultFrequencyIsZero() {
        assertEquals(0.0f, log.getFrequency(), 0.0001f);
    }

    @Test
    void defaultModeIsNull() {
        assertNull(log.getMode());
    }

    @Test
    void defaultDescriptionIsNull() {
        assertNull(log.getDescription());
    }

    @Test
    void defaultLocationIsZero() {
        assertEquals(0, log.getLocation());
    }

    @Test
    void defaultMyPlaceIsZero() {
        assertEquals(0, log.getMyPlace());
    }

    @Test
    void defaultCreatedAtIsZero() {
        assertEquals(0, log.getCreatedAt());
    }

    // ── Setters / getters ─────────────────────────────────────────────────────

    @Test
    void setAndGetId() {
        log.setId(42);
        assertEquals(42, log.getId());
    }

    @Test
    void setAndGetDateOn() {
        log.setDateOn(1700000000);
        assertEquals(1700000000, log.getDateOn());
    }

    @Test
    void setAndGetFrequency() {
        log.setFrequency(14025.5f);
        assertEquals(14025.5f, log.getFrequency(), 0.001f);
    }

    @Test
    void setAndGetMode() {
        log.setMode("AM");
        assertEquals("AM", log.getMode());
    }

    @Test
    void setAndGetDescription() {
        log.setDescription("Test station broadcast");
        assertEquals("Test station broadcast", log.getDescription());
    }

    @Test
    void setAndGetLocation() {
        log.setLocation(7);
        assertEquals(7, log.getLocation());
    }

    @Test
    void setAndGetMyPlace() {
        log.setMyPlace(3);
        assertEquals(3, log.getMyPlace());
    }

    @Test
    void setAndGetCreatedAt() {
        log.setCreatedAt(1700000001);
        assertEquals(1700000001, log.getCreatedAt());
    }

    // ── hasLocation ───────────────────────────────────────────────────────────

    @Test
    void hasLocationReturnsFalseWhenLocationIsZero() {
        log.setLocation(0);
        assertFalse(log.hasLocation());
    }

    @Test
    void hasLocationReturnsTrueWhenLocationIsNonZero() {
        log.setLocation(5);
        assertTrue(log.hasLocation());
    }

    @Test
    void hasLocationReturnsTrueForNegativeId() {
        // Negative IDs are unusual but the method only checks != 0
        log.setLocation(-1);
        assertTrue(log.hasLocation());
    }

    // ── toString ──────────────────────────────────────────────────────────────

    @Test
    void toStringContainsId() {
        log.setId(99);
        assertTrue(log.toString().contains("id:99"));
    }

    @Test
    void toStringContainsFrequency() {
        log.setFrequency(9800.0f);
        assertTrue(log.toString().contains("frequency:9800.0"));
    }

    @Test
    void toStringContainsMode() {
        log.setMode("USB");
        assertTrue(log.toString().contains("mode:USB"));
    }

    @Test
    void toStringContainsDescription() {
        log.setDescription("Voice of America");
        assertTrue(log.toString().contains("description:Voice of America"));
    }

    @Test
    void toStringContainsLocationAndMyPlace() {
        log.setLocation(2);
        log.setMyPlace(4);
        String str = log.toString();
        assertTrue(str.contains("location:2"));
        assertTrue(str.contains("my_place:4"));
    }
}
