package org.qualsh.lb.digitalmodes.audio;

import org.qualsh.lb.digital.DigitalMode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The application's audio memory — it holds whatever audio is currently loaded,
 * whether captured live from a radio rig, recorded from a microphone, or imported
 * from a WAV file on your computer.
 *
 * <p>Only one block of audio can be held at a time; loading new audio completely
 * replaces whatever was there before. The spectrum display and all decoders read
 * directly from this buffer, so any change here is immediately reflected on screen.
 *
 * <p>Any part of the application that needs to know when audio changes can
 * register as a listener via {@link #addListener(AudioBufferListener)}.
 *
 * @author Logbook Development Team
 * @version 1.0
 */
public class AudioBuffer {

    private byte[] samples;
    private float sampleRate;
    private DigitalMode associatedMode;
    private final List<AudioBufferListener> listeners;

    /**
     * Notified whenever the audio held in the application's memory is replaced or cleared.
     *
     * <p>The spectrum display and decoders register as listeners so they can update
     * themselves automatically each time new audio is loaded or cleared.
     */
    public interface AudioBufferListener {

        /**
         * Called after the audio in memory has been replaced by a load or clear operation.
         *
         * @param buffer the audio buffer whose content just changed; never {@code null}
         */
        void onBufferChanged(AudioBuffer buffer);
    }

    /**
     * Creates a new, empty audio buffer with no audio loaded and no listeners registered.
     */
    public AudioBuffer() {
        this.samples = new byte[0];
        this.sampleRate = 0.0f;
        this.associatedMode = null;
        this.listeners = new ArrayList<>();
    }

    /**
     * Loads new audio into memory, replacing any previously loaded audio.
     *
     * <p>The spectrum display and decoder will immediately begin working with the
     * new audio. Any audio that was loaded before this call is permanently discarded.
     *
     * @param samples    the raw audio data to load; must not be {@code null}
     * @param sampleRate the number of audio samples per second in the supplied data,
     *                   for example {@code 8000.0f} or {@code 44100.0f}
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
     * Removes all audio from memory.
     *
     * <p>The spectrum display will go blank until new audio is loaded.
     * After this call, {@link #isEmpty()} returns {@code true}.
     */
    public void clear() {
        this.samples = new byte[0];
        this.sampleRate = 0.0f;
        notifyListeners();
    }

    /**
     * Returns {@code true} if no audio has been loaded yet, or if the audio has been cleared.
     *
     * @return {@code true} when no audio is available
     */
    public boolean isEmpty() {
        return samples == null || samples.length == 0;
    }

    /**
     * Returns the raw audio data currently held in memory.
     *
     * <p>Returns an empty array if no audio has been loaded. The returned data
     * is a copy, so modifying it has no effect on the stored audio.
     *
     * @return a copy of the stored audio bytes; never {@code null}
     */
    public byte[] getSamples() {
        if (samples == null) {
            return new byte[0];
        }
        return Arrays.copyOf(samples, samples.length);
    }

    /**
     * Returns the sample rate of the audio currently loaded, measured in samples per second.
     *
     * <p>Common values are {@code 8000.0f} and {@code 44100.0f}. Returns {@code 0.0f}
     * if no audio is loaded.
     *
     * @return samples per second; {@code 0.0f} when no audio is loaded
     */
    public float getSampleRate() {
        return sampleRate;
    }

    /**
     * Returns the total length of the loaded audio in seconds.
     *
     * <p>Returns {@code 0.0} when no audio is loaded or the sample rate is unknown.
     *
     * @return duration in seconds, or {@code 0.0} if no audio is loaded
     */
    public double getDurationSeconds() {
        if (isEmpty() || sampleRate == 0.0f) {
            return 0.0;
        }
        int numSamples = samples.length / 2; // 16-bit mono: 2 bytes per sample
        return numSamples / (double) sampleRate;
    }

    /**
     * Records which digital mode the audio was captured or imported for.
     *
     * <p>This is set automatically when you change the active mode in the Mode selector.
     * Pass {@code null} to clear the association.
     *
     * @param mode the digital mode to associate with the current audio, or {@code null}
     */
    public void setAssociatedMode(DigitalMode mode) {
        this.associatedMode = mode;
    }

    /**
     * Returns the digital mode associated with the currently loaded audio,
     * or {@code null} if none has been set.
     *
     * @return the associated digital mode, or {@code null}
     */
    public DigitalMode getAssociatedMode() {
        return associatedMode;
    }

    /**
     * Registers a listener to be notified whenever the audio in memory is replaced or cleared.
     *
     * <p>The spectrum display and decoders use this internally to keep themselves up to date.
     * Adding the same listener more than once will cause it to receive duplicate notifications.
     *
     * @param listener the listener to add; must not be {@code null}
     */
    public void addListener(AudioBufferListener listener) {
        listeners.add(listener);
    }

    /**
     * Removes a previously registered listener so it no longer receives audio change notifications.
     *
     * <p>Has no effect if the listener is not currently registered.
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
