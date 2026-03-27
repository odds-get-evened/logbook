package org.qualsh.lb.digitalmodes.audio;

/**
 * Represents one of the ways audio can enter the Digital Modes application.
 *
 * <p>Implementations include loading a WAV file from your computer, recording
 * audio from your computer's microphone input, and streaming live audio from
 * a radio rig connected via a USB serial cable. Each implementation fills the
 * shared audio buffer that drives the spectrum display and decoders.
 *
 * @author Logbook Development Team
 * @version 1.0
 */
public interface AudioSource {

    /**
     * Starts the audio source so it begins supplying audio to the application.
     *
     * <p>For a rig connection this opens the serial port and begins streaming.
     * For a file-based source this may have no effect — use the source's specific
     * load method instead.
     */
    void start();

    /**
     * Stops the audio source and releases any hardware connections it holds.
     *
     * <p>After this call, {@link #isActive()} returns {@code false}.
     */
    void stop();

    /**
     * Returns {@code true} if this source is currently active and supplying audio.
     *
     * @return {@code true} while audio is being supplied
     */
    boolean isActive();

    /**
     * Returns the audio buffer that this source fills with incoming audio data.
     *
     * @return the buffer; never {@code null}
     */
    AudioBuffer getBuffer();
}
