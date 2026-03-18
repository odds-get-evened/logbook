package org.qualsh.lb.dx;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link DXClusterClient}.
 *
 * <p>These tests focus on the spot-parsing logic and listener registration,
 * which do not require an actual network connection.
 */
public class DXClusterClientTest {

    private DXClusterClient client;

    @BeforeEach
    void setUp() {
        client = new DXClusterClient();
    }

    // ── Initial state ─────────────────────────────────────────────────────────

    @Test
    void isNotConnectedByDefault() {
        assertFalse(client.isConnected());
    }

    // ── parseLine – valid spots ───────────────────────────────────────────────

    @Test
    void parseLineStandardSpot() {
        String line = "DX de KC9AAA:   14025.0  W1AW         599 cw                          0501Z";
        DXSpot spot = client.parseLine(line);

        assertNotNull(spot);
        assertEquals("KC9AAA", spot.getSpotter());
        assertEquals(14025.0, spot.getFrequency(), 0.001);
        assertEquals("W1AW", spot.getCallsign());
        assertEquals("0501Z", spot.getTime());
    }

    @Test
    void parseLineExtractsComment() {
        String line = "DX de VE3ABC:   7074.0   DL1XYZ       FT8 -12dB                       1200Z";
        DXSpot spot = client.parseLine(line);

        assertNotNull(spot);
        assertEquals("FT8 -12dB", spot.getComment());
    }

    @Test
    void parseLineWithDecimalFrequency() {
        String line = "DX de F5XYZ:    14074.5  G3ABC        FT8                             0930Z";
        DXSpot spot = client.parseLine(line);

        assertNotNull(spot);
        assertEquals(14074.5, spot.getFrequency(), 0.001);
    }

    @Test
    void parseLineSpotterCallsignIsPreserved() {
        String line = "DX de JA1ABC:   21074.0  VK2XYZ       FT8 signal                      1800Z";
        DXSpot spot = client.parseLine(line);

        assertNotNull(spot);
        assertEquals("JA1ABC", spot.getSpotter());
        assertEquals("VK2XYZ", spot.getCallsign());
    }

    @Test
    void parseLineTimeHasSuffix() {
        String line = "DX de N0CALL:   3500.0   W6ABC        CW 599                          2359Z";
        DXSpot spot = client.parseLine(line);

        assertNotNull(spot);
        assertTrue(spot.getTime().endsWith("Z"), "Time should end with Z");
    }

    @Test
    void parseLineMinimalComment() {
        String line = "DX de W1XYZ:    10100.0  EA3ABC       cq                              0001Z";
        DXSpot spot = client.parseLine(line);

        assertNotNull(spot);
        assertNotNull(spot.getComment());
    }

    // ── parseLine – invalid or non-spot lines ─────────────────────────────────

    @Test
    void parseLineReturnsNullForNull() {
        assertNull(client.parseLine(null));
    }

    @Test
    void parseLineReturnsNullForEmptyString() {
        assertNull(client.parseLine(""));
    }

    @Test
    void parseLineReturnsNullForWelcomeBanner() {
        assertNull(client.parseLine("Welcome to the DX cluster!"));
    }

    @Test
    void parseLineReturnsNullForLoginPrompt() {
        assertNull(client.parseLine("Please enter your callsign:"));
    }

    @Test
    void parseLineReturnsNullForMalformedSpot() {
        // Missing the time field at the end
        assertNull(client.parseLine("DX de W1XYZ:   14000.0  W2ABC       no time here"));
    }

    @Test
    void parseLineReturnsNullForPartialLine() {
        assertNull(client.parseLine("DX de W1XYZ:"));
    }

    @Test
    void parseLineReturnsNullForNonDXLine() {
        assertNull(client.parseLine("To all: test message 1234Z"));
    }

    // ── Listener registration ─────────────────────────────────────────────────

    @Test
    void addAndRemoveSpotListenerDoesNotThrow() {
        Consumer<DXSpot> listener = spot -> {};
        assertDoesNotThrow(() -> client.addSpotListener(listener));
        assertDoesNotThrow(() -> client.removeSpotListener(listener));
    }

    @Test
    void addAndRemoveStatusListenerDoesNotThrow() {
        Consumer<Boolean> listener = status -> {};
        assertDoesNotThrow(() -> client.addStatusListener(listener));
        assertDoesNotThrow(() -> client.removeStatusListener(listener));
    }

    @Test
    void addAndRemoveRawListenerDoesNotThrow() {
        Consumer<String> listener = line -> {};
        assertDoesNotThrow(() -> client.addRawListener(listener));
        assertDoesNotThrow(() -> client.removeRawListener(listener));
    }

    @Test
    void multipleSpotListenersCanBeRegistered() {
        List<String> received = new ArrayList<>();
        client.addSpotListener(s -> received.add("listener1"));
        client.addSpotListener(s -> received.add("listener2"));
        // Listeners are registered; spot delivery is tested via real connection
        // so we just verify no exception is thrown and state is consistent
        assertFalse(client.isConnected());
    }

    // ── disconnect when not connected ─────────────────────────────────────────

    @Test
    void disconnectWhenNotConnectedDoesNotThrow() {
        assertDoesNotThrow(() -> client.disconnect());
        assertFalse(client.isConnected());
    }

    @Test
    void disconnectTwiceDoesNotThrow() {
        assertDoesNotThrow(() -> {
            client.disconnect();
            client.disconnect();
        });
    }
}
