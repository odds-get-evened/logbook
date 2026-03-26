package org.qualsh.lb.digitalmodes.audio;

/**
 * Represents a source of audio data that populates an {@link AudioBuffer}.
 *
 * <p>Implementations cover different origin types — WAV file import,
 * live microphone capture, rig audio input — each producing 16-bit
 * mono PCM data consumed by decoders and the spectrum display.
 */
public interface AudioSource {

    /**
     * Starts the audio source, beginning the flow of audio data into the
     * associated {@link AudioBuffer}.
     *
     * <p>For file-based sources this may be a no-op; for live-input sources
     * it opens the hardware line and begins capture.
     */
    void start();

    /**
     * Stops the audio source, ending the flow of audio data.
     *
     * <p>Resources such as hardware lines should be released. After this
     * call {@link #isActive()} must return {@code false}.
     */
    void stop();

    /**
     * Returns {@code true} if the source is currently active and producing
     * audio data.
     *
     * @return {@code true} while the source is running
     */
    boolean isActive();

    /**
     * Returns the {@link AudioBuffer} that this source writes into.
     *
     * @return the buffer; never {@code null}
     */
    AudioBuffer getBuffer();
}
