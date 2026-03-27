package org.qualsh.lb.digitalmodes.decoder;

import org.qualsh.lb.digital.DigitalMode;
import org.qualsh.lb.digital.decode.DecodeResult;
import org.qualsh.lb.digitalmodes.audio.AudioBuffer;

import java.util.List;

/**
 * The shared contract that all digital-mode decoders in the application follow.
 *
 * <p>Each decoder handles one specific digital mode — FT8, WSPR, BPSK31, and so on —
 * and knows how to search the loaded audio for signals of that type. When signals are
 * found, their details (time, mode, frequency, and decoded text) are returned as a list
 * of results that appear in the decode output area and the decode log.
 *
 * @author Logbook Development Team
 * @version 1.0
 */
public interface Decoder {

    /**
     * Analyzes the loaded audio and attempts to find and decode any signals of this mode present.
     *
     * <p>Results appear in the decode output area and are added to the decode log.
     * An empty list is returned when no signals are found or the audio is too short.
     *
     * @param buffer the audio to analyze; must not be {@code null}
     * @return a list of decoded signals, possibly empty; never {@code null}
     */
    List<DecodeResult> decode(AudioBuffer buffer);

    /**
     * Returns the digital mode that this decoder handles.
     *
     * @return the associated digital mode; never {@code null}
     */
    DigitalMode getMode();
}
