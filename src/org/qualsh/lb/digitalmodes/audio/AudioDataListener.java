package org.qualsh.lb.digitalmodes.audio;

/**
 * Callback interface for components that consume audio data from the streaming
 * pipeline.
 *
 * <p>Implementors receive fixed-size audio chunks as they flow through the
 * {@link AudioConsumer}. The spectrum display and waterfall panel register as
 * listeners so they can update in near-real-time without polling the audio
 * buffer.
 *
 * <p>Callbacks are invoked on the consumer thread. Implementations that touch
 * Swing components must marshal updates to the Event Dispatch Thread via
 * {@link javax.swing.SwingUtilities#invokeLater(Runnable)}.
 *
 * @author Logbook Development Team
 * @version 1.0
 */
public interface AudioDataListener {

    /**
     * Called when a new chunk of audio data has been consumed from the ring
     * buffer.
     *
     * <p>The {@code data} array is a shared, pre-allocated buffer that will be
     * overwritten on the next call. Implementations must not retain a reference
     * to it; copy the bytes if they are needed beyond this call.
     *
     * @param data       the audio chunk bytes (16-bit signed little-endian PCM);
     *                   only bytes {@code 0 .. length-1} are valid
     * @param length     the number of valid bytes in {@code data}
     * @param sampleRate the sample rate of the audio in samples per second
     */
    void onAudioChunk(byte[] data, int length, float sampleRate);
}
