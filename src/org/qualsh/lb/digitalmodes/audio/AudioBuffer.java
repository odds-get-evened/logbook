package org.qualsh.lb.digitalmodes.audio;

import org.qualsh.lb.digital.DigitalMode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Central shared audio data store for the Digital Modes feature.
 *
 * <p>All audio sources — rig audio capture, WAV file import, and live
 * recording — write into this buffer via {@link #load(byte[], float)}.
 * All decoders and the spectrum display read from it via
 * {@link #getSamples()} and {@link #getSampleRate()}.
 *
 * <p>Loading new audio <em>completely replaces</em> any existing content;
 * audio is never appended. Interested components register as
 * {@link AudioBufferListener listeners} to be notified whenever the buffer
 * content changes.
 *
 * <p>Audio data is expected to be 16-bit, mono, little-endian PCM.
 */
public class AudioBuffer {

    private byte[] samples;
    private float sampleRate;
    private DigitalMode associatedMode;
    private final List<AudioBufferListener> listeners;

    /**
     * Listener interface for components that need to react whenever the
     * audio buffer is replaced or cleared.
     */
    public interface AudioBufferListener {

        /**
         * Called after the buffer content has been replaced by a
         * {@link AudioBuffer#load(byte[], float) load} or
         * {@link AudioBuffer#clear() clear} operation.
         *
         * @param buffer the {@link AudioBuffer} whose content changed;
         *               never {@code null}
         */
        void onBufferChanged(AudioBuffer buffer);
    }

    /**
     * Creates a new, empty {@code AudioBuffer} with no associated mode and
     * no registered listeners.
     */
    public AudioBuffer() {
        this.samples = new byte[0];
        this.sampleRate = 0.0f;
        this.associatedMode = null;
        this.listeners = new ArrayList<>();
    }

    /**
     * Replaces the current buffer content with the supplied PCM audio data.
     *
     * <p>The provided array is copied defensively, so subsequent changes to
     * the caller's array do not affect the buffer. Any previously held audio
     * data is discarded. All registered listeners are notified after the
     * replacement is complete.
     *
     * @param samples    the raw 16-bit mono PCM bytes to store; must not be
     *                   {@code null}
     * @param sampleRate the number of samples per second, for example
     *                   {@code 8000.0f} or {@code 44100.0f}
     * @throws IllegalArgumentException if {@code samples} is {@code null}
     */
    public void load(byte[] samples, float sampleRate) {
        if (samples == null) {
            throw new IllegalArgumentException("samples must not be null");
        }
        this.samples = Arrays.copyOf(samples, samples.length);
        this.sampleRate = sampleRate;
        notifyListeners();
    }

    /**
     * Clears the buffer, discarding all audio data and resetting the sample
     * rate to zero.
     *
     * <p>After this call, {@link #isEmpty()} returns {@code true}. All
     * registered listeners are notified.
     */
    public void clear() {
        this.samples = new byte[0];
        this.sampleRate = 0.0f;
        notifyListeners();
    }

    /**
     * Returns {@code true} if the buffer contains no audio data.
     *
     * @return {@code true} when the internal samples array is {@code null}
     *         or has a length of zero
     */
    public boolean isEmpty() {
        return samples == null || samples.length == 0;
    }

    /**
     * Returns a defensive copy of the raw PCM audio bytes currently held in
     * the buffer.
     *
     * <p>Modifying the returned array has no effect on the buffer's internal
     * state. Returns an empty array when the buffer is empty.
     *
     * @return a copy of the stored PCM bytes; never {@code null}
     */
    public byte[] getSamples() {
        if (samples == null) {
            return new byte[0];
        }
        return Arrays.copyOf(samples, samples.length);
    }

    /**
     * Returns the sample rate of the audio currently held in the buffer.
     *
     * @return samples per second, for example {@code 44100.0f}; {@code 0.0f}
     *         when the buffer is empty
     */
    public float getSampleRate() {
        return sampleRate;
    }

    /**
     * Calculates and returns the duration of the buffered audio in seconds.
     *
     * <p>The calculation assumes 16-bit mono PCM encoding (2 bytes per
     * sample). Returns {@code 0.0} when the buffer is empty or the sample
     * rate is zero.
     *
     * @return duration in seconds, or {@code 0.0} if the buffer is empty
     */
    public double getDurationSeconds() {
        if (isEmpty() || sampleRate == 0.0f) {
            return 0.0;
        }
        int numSamples = samples.length / 2; // 16-bit mono: 2 bytes per sample
        return numSamples / (double) sampleRate;
    }

    /**
     * Associates a {@link DigitalMode} with this buffer.
     *
     * <p>This records which digital mode the current audio was captured or
     * imported for. May be set to {@code null} to clear the association.
     *
     * @param mode the digital mode to associate, or {@code null}
     */
    public void setAssociatedMode(DigitalMode mode) {
        this.associatedMode = mode;
    }

    /**
     * Returns the {@link DigitalMode} currently associated with this buffer,
     * or {@code null} if none has been set.
     *
     * @return the associated digital mode, or {@code null}
     */
    public DigitalMode getAssociatedMode() {
        return associatedMode;
    }

    /**
     * Registers a listener to be notified whenever the buffer content is
     * replaced or cleared.
     *
     * <p>Adding the same listener instance more than once will result in it
     * receiving multiple notifications per event.
     *
     * @param listener the listener to add; must not be {@code null}
     */
    public void addListener(AudioBufferListener listener) {
        listeners.add(listener);
    }

    /**
     * Removes a previously registered listener.
     *
     * <p>If the listener was registered more than once, only the first
     * occurrence is removed. Has no effect if the listener is not currently
     * registered.
     *
     * @param listener the listener to remove; must not be {@code null}
     */
    public void removeListener(AudioBufferListener listener) {
        listeners.remove(listener);
    }

    /**
     * Notifies all registered listeners that the buffer content has changed.
     */
    private void notifyListeners() {
        for (AudioBufferListener listener : listeners) {
            listener.onBufferChanged(this);
        }
    }
}
