package org.qualsh.lb.digitalmodes.decoder;

import org.qualsh.lb.digital.DigitalMode;
import org.qualsh.lb.digital.decode.DecodeResult;
import org.qualsh.lb.digitalmodes.audio.AudioBuffer;

import java.util.List;

/**
 * Common contract for all digital-mode decoder implementations.
 *
 * <p>Each concrete decoder handles one {@link DigitalMode} and is capable of
 * analysing an {@link AudioBuffer} to extract zero or more {@link DecodeResult}
 * objects. Decoders are stateless between calls to {@link #decode(AudioBuffer)};
 * the buffer must contain a complete (or sufficiently long) audio frame each
 * time {@code decode} is invoked.
 *
 * <p>Implementations should return an empty list — never {@code null} — when
 * the buffer is too short, does not contain a recognisable signal, or an error
 * occurs during analysis.
 */
public interface Decoder {

    /**
     * Analyses the supplied audio buffer and returns any decode results found.
     *
     * <p>The buffer is expected to contain 16-bit, mono, signed,
     * little-endian PCM audio. Short or empty buffers should be handled
     * gracefully and will typically yield an empty result list.
     *
     * <p>This method may be called from a background thread; implementations
     * must not touch Swing components directly.
     *
     * @param buffer the audio data to analyse; must not be {@code null}
     * @return a list of decode results, possibly empty; never {@code null}
     */
    List<DecodeResult> decode(AudioBuffer buffer);

    /**
     * Returns the {@link DigitalMode} that this decoder handles.
     *
     * @return the associated digital mode; never {@code null}
     */
    DigitalMode getMode();
}
