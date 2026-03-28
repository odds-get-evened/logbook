package org.qualsh.lb.digitalmodes.audio;

import org.qualsh.lb.digital.DigitalMode;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReentrantReadWriteLock;

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
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

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
        this.listeners = new CopyOnWriteArrayList<>();
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
        lock.writeLock().lock();
        try {
            this.samples = Arrays.copyOf(samples, samples.length);
            this.sampleRate = sampleRate;
        } finally {
            lock.writeLock().unlock();
        }
        notifyListeners();
    }

    /**
     * Removes all audio from memory.
     *
     * <p>The spectrum display will go blank until new audio is loaded.
     * After this call, {@link #isEmpty()} returns {@code true}.
     */
    public void clear() {
        lock.writeLock().lock();
        try {
            this.samples = new byte[0];
            this.sampleRate = 0.0f;
        } finally {
            lock.writeLock().unlock();
        }
        notifyListeners();
    }

    /**
     * Appends a chunk of audio data to the end of the buffer.
     *
     * <p>Used by the streaming pipeline's {@link AudioConsumer} to build up
     * audio incrementally instead of loading the entire file at once.
     * On the first call after a {@link #clear()}, the sample rate is set.
     *
     * @param chunk      the audio bytes to append; must not be {@code null}
     * @param offset     start position in {@code chunk}
     * @param length     number of bytes to append from {@code chunk}
     * @param sampleRate the sample rate of the incoming audio
     */
    public void appendChunk(byte[] chunk, int offset, int length, float sampleRate) {
        lock.writeLock().lock();
        try {
            int oldLen = this.samples.length;
            byte[] grown = new byte[oldLen + length];
            System.arraycopy(this.samples, 0, grown, 0, oldLen);
            System.arraycopy(chunk, offset, grown, oldLen, length);
            this.samples = grown;
            this.sampleRate = sampleRate;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Copies a window of audio samples into a pre-allocated destination array.
     *
     * <p>Only the requested range is copied — not the entire buffer. This is
     * far more efficient than {@link #getSamples()} when only a small FFT
     * window is needed.
     *
     * @param dest        destination byte array; must have room for
     *                    {@code sampleCount * 2} bytes starting at index 0
     * @param sampleStart the first sample index to copy (not byte index)
     * @param sampleCount the number of samples to copy
     * @return the number of samples actually copied (may be less than requested
     *         if the buffer is shorter)
     */
    public int readWindow(byte[] dest, int sampleStart, int sampleCount) {
        lock.readLock().lock();
        try {
            if (samples == null || samples.length == 0) {
                return 0;
            }
            int totalSamples = samples.length / 2;
            int start = Math.max(0, Math.min(sampleStart, totalSamples));
            int count = Math.min(sampleCount, totalSamples - start);
            if (count <= 0) {
                return 0;
            }
            System.arraycopy(samples, start * 2, dest, 0, count * 2);
            return count;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Returns a direct reference to the internal sample array without copying.
     *
     * <p><strong>Warning:</strong> the returned array must be treated as
     * read-only. It may be replaced at any time by a concurrent
     * {@link #load(byte[], float)} or {@link #appendChunk} call. Use this
     * only on hot paths where copying would be too expensive.
     *
     * @return the internal sample array; never {@code null}
     */
    public byte[] getSamplesRef() {
        lock.readLock().lock();
        try {
            return samples != null ? samples : new byte[0];
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Returns the length of the internal sample array in bytes without copying.
     *
     * @return byte length of stored audio; {@code 0} when empty
     */
    public int getLength() {
        lock.readLock().lock();
        try {
            return samples != null ? samples.length : 0;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Returns {@code true} if no audio has been loaded yet, or if the audio has been cleared.
     *
     * @return {@code true} when no audio is available
     */
    public boolean isEmpty() {
        lock.readLock().lock();
        try {
            return samples == null || samples.length == 0;
        } finally {
            lock.readLock().unlock();
        }
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
        lock.readLock().lock();
        try {
            if (samples == null) {
                return new byte[0];
            }
            return Arrays.copyOf(samples, samples.length);
        } finally {
            lock.readLock().unlock();
        }
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
        lock.readLock().lock();
        try {
            return sampleRate;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Returns the total length of the loaded audio in seconds.
     *
     * <p>Returns {@code 0.0} when no audio is loaded or the sample rate is unknown.
     *
     * @return duration in seconds, or {@code 0.0} if no audio is loaded
     */
    public double getDurationSeconds() {
        lock.readLock().lock();
        try {
            if (samples == null || samples.length == 0 || sampleRate == 0.0f) {
                return 0.0;
            }
            int numSamples = samples.length / 2; // 16-bit mono: 2 bytes per sample
            return numSamples / (double) sampleRate;
        } finally {
            lock.readLock().unlock();
        }
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
