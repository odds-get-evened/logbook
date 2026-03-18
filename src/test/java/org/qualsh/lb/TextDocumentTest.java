package org.qualsh.lb;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import javax.swing.text.BadLocationException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for TextDocument – a PlainDocument subclass that enforces a maximum
 * character length on text fields.
 */
public class TextDocumentTest {

    private TextDocument doc;

    @BeforeEach
    void setUp() {
        doc = new TextDocument(10);
    }

    // ── Constructor ────────────────────────────────────────────────────────────

    @Test
    void defaultConstructorCreatesDocumentWithNoLimitEnforcement() {
        TextDocument noLimit = new TextDocument();
        // maxLength is 0 by default; insertString compares (0 + len) <= 0 so nothing
        // can be inserted, but at least construction succeeds without exception.
        assertNotNull(noLimit);
    }

    @Test
    void constructorStoresMaxLength() {
        assertEquals(10, doc.getMaxLength());
    }

    @Test
    void negativeMaxLengthThrowsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> new TextDocument(-1));
    }

    @Test
    void zeroMaxLengthIsAllowed() {
        TextDocument zero = new TextDocument(0);
        assertEquals(0, zero.getMaxLength());
    }

    // ── insertString ──────────────────────────────────────────────────────────

    @Test
    void insertStringUpToMaxLengthSucceeds() throws BadLocationException {
        doc.insertString(0, "Hello", null);
        assertEquals(5, doc.getLength());
    }

    @Test
    void insertStringExactlyAtMaxLengthSucceeds() throws BadLocationException {
        doc.insertString(0, "0123456789", null); // exactly 10 chars
        assertEquals(10, doc.getLength());
    }

    @Test
    void insertStringBeyondMaxLengthIsRejected() throws BadLocationException {
        doc.insertString(0, "01234567890", null); // 11 chars, should be rejected
        assertEquals(0, doc.getLength());
    }

    @Test
    void insertStringThatWouldExceedMaxWhenCombinedIsRejected() throws BadLocationException {
        doc.insertString(0, "12345", null);  // 5 chars OK
        doc.insertString(5, "123456", null); // 6 more would exceed 10 – rejected
        assertEquals(5, doc.getLength());
    }

    @Test
    void insertNullStringDoesNothing() throws BadLocationException {
        doc.insertString(0, null, null);
        assertEquals(0, doc.getLength());
    }

    @Test
    void insertEmptyStringDoesNothing() throws BadLocationException {
        doc.insertString(0, "", null);
        assertEquals(0, doc.getLength());
    }

    // ── setMaxLength ──────────────────────────────────────────────────────────

    @Test
    void setMaxLengthUpdatesValue() {
        doc.setMaxLength(20);
        assertEquals(20, doc.getMaxLength());
    }

    @Test
    void multipleInsertsAccumulateUpToLimit() throws BadLocationException {
        doc.insertString(0, "abcde", null);
        doc.insertString(5, "fghij", null);
        assertEquals(10, doc.getLength());

        // Now at the limit – further insert is silently dropped
        doc.insertString(10, "X", null);
        assertEquals(10, doc.getLength());
    }
}
