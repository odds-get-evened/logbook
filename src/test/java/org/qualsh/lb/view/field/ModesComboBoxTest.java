package org.qualsh.lb.view.field;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assumptions;

import java.awt.GraphicsEnvironment;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link ModesComboBox}.
 *
 * <p>ModesComboBox provides a fixed list of broadcast modes: AM, FM, USB, LSB,
 * CW, FSK, MFSK, PSK, Image.
 */
public class ModesComboBoxTest {

    private static final List<String> EXPECTED_MODES = List.of(
            "AM", "FM", "USB", "LSB", "CW", "FSK", "MFSK", "PSK", "Image");

    // ── Component instantiation ───────────────────────────────────────────────

    @Test
    void instantiatesWithoutException() {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(),
                "Skipping Swing test in headless environment");
        ModesComboBox combo = new ModesComboBox();
        assertNotNull(combo);
    }

    @Test
    void hasNineItems() {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(),
                "Skipping Swing test in headless environment");
        ModesComboBox combo = new ModesComboBox();
        assertEquals(9, combo.getItemCount());
    }

    @Test
    void firstItemIsAM() {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(),
                "Skipping Swing test in headless environment");
        ModesComboBox combo = new ModesComboBox();
        assertEquals("AM", combo.getItemAt(0));
    }

    @Test
    void defaultSelectionIsAM() {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(),
                "Skipping Swing test in headless environment");
        ModesComboBox combo = new ModesComboBox();
        assertEquals("AM", combo.getSelectedItem());
    }

    @Test
    void allExpectedModesArePresent() {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(),
                "Skipping Swing test in headless environment");
        ModesComboBox combo = new ModesComboBox();
        for (String mode : EXPECTED_MODES) {
            boolean found = false;
            for (int i = 0; i < combo.getItemCount(); i++) {
                if (mode.equals(combo.getItemAt(i))) {
                    found = true;
                    break;
                }
            }
            assertTrue(found, "Expected mode '" + mode + "' not found in combo box");
        }
    }

    @Test
    void modesArrayMatchesExpected() {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(),
                "Skipping Swing test in headless environment");
        ModesComboBox combo = new ModesComboBox();
        String[] modes = combo.getModes();
        assertArrayEquals(EXPECTED_MODES.toArray(new String[0]), modes);
    }

    @Test
    void selectionCanBeChanged() {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(),
                "Skipping Swing test in headless environment");
        ModesComboBox combo = new ModesComboBox();
        combo.setSelectedItem("CW");
        assertEquals("CW", combo.getSelectedItem());
    }

    @Test
    void setModesUpdatesArray() {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(),
                "Skipping Swing test in headless environment");
        ModesComboBox combo = new ModesComboBox();
        String[] newModes = {"AM", "FM"};
        combo.setModes(newModes);
        assertArrayEquals(newModes, combo.getModes());
    }

    // ── Mode ordering ─────────────────────────────────────────────────────────

    @Test
    void modesAreInExpectedOrder() {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(),
                "Skipping Swing test in headless environment");
        ModesComboBox combo = new ModesComboBox();
        for (int i = 0; i < EXPECTED_MODES.size(); i++) {
            assertEquals(EXPECTED_MODES.get(i), combo.getItemAt(i),
                    "Mode at index " + i + " should be " + EXPECTED_MODES.get(i));
        }
    }

    @Test
    void containsFM() {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(),
                "Skipping Swing test in headless environment");
        ModesComboBox combo = new ModesComboBox();
        boolean found = false;
        for (int i = 0; i < combo.getItemCount(); i++) {
            if ("FM".equals(combo.getItemAt(i))) { found = true; break; }
        }
        assertTrue(found, "FM should be in the modes list");
    }

    @Test
    void containsImageMode() {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(),
                "Skipping Swing test in headless environment");
        ModesComboBox combo = new ModesComboBox();
        boolean found = false;
        for (int i = 0; i < combo.getItemCount(); i++) {
            if ("Image".equals(combo.getItemAt(i))) { found = true; break; }
        }
        assertTrue(found, "Image should be in the modes list");
    }
}
