package org.qualsh.lb.dx;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the {@link DXSpot} value object.
 */
public class DXSpotTest {

    @Test
    void constructorStoresAllFields() {
        DXSpot spot = new DXSpot("0501Z", 14025.0, "W1AW", "KC9AAA", "599 CW");

        assertEquals("0501Z",   spot.getTime());
        assertEquals(14025.0,  spot.getFrequency(), 0.001);
        assertEquals("W1AW",   spot.getCallsign());
        assertEquals("KC9AAA", spot.getSpotter());
        assertEquals("599 CW", spot.getComment());
    }

    @Test
    void frequencyIsStoredAsDouble() {
        DXSpot spot = new DXSpot("1200Z", 7074.5, "DL1ABC", "F5XYZ", "FT8");
        assertEquals(7074.5, spot.getFrequency(), 0.0001);
    }

    @Test
    void emptyCommentIsStoredAsEmptyString() {
        DXSpot spot = new DXSpot("0000Z", 3500.0, "N0CALL", "G3XYZ", "");
        assertEquals("", spot.getComment());
    }

    @Test
    void nullFieldsAreStoredAsNull() {
        DXSpot spot = new DXSpot(null, 0.0, null, null, null);
        assertNull(spot.getTime());
        assertNull(spot.getCallsign());
        assertNull(spot.getSpotter());
        assertNull(spot.getComment());
    }

    @Test
    void zeroFrequencyIsStoredCorrectly() {
        DXSpot spot = new DXSpot("0000Z", 0.0, "TEST", "TEST", "test");
        assertEquals(0.0, spot.getFrequency(), 0.0001);
    }

    @Test
    void highFrequencyIsStoredCorrectly() {
        DXSpot spot = new DXSpot("1800Z", 144000.0, "VK2XYZ", "ZL3ABC", "SSB");
        assertEquals(144000.0, spot.getFrequency(), 0.001);
    }

    @Test
    void timeStringWithZSuffixIsStoredVerbatim() {
        DXSpot spot = new DXSpot("2359Z", 21074.0, "JA1XYZ", "VE3ABC", "FT8");
        assertEquals("2359Z", spot.getTime());
    }
}
