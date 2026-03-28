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
 * <p>Internally the buffer uses a geometrically-growing backing array (doubling
 * strategy, starting at 64 KB) so that streaming playback appends chunks with
 * at most O(log n) array copies instead of one full copy per chunk.
 *
 * <p>Any part of the application that needs to know when audio changes can
 * register as a listener via {@link #addListener(AudioBufferListener)}.
 *
 * @author Logbook Development Team
 * @version 1.0
 */
public class AudioBuffer {

    /** Initial backing-array capacity in bytes (64 KB). */
    private static final int INITIAL_CAPACITY = 65536;

    /**
     * Backing store. May be larger than {@link #size}; only bytes
     * {@code [0, size)} contain valid audio data.
     */
    private byte[] buf;

    /** Number of valid audio bytes currently held in {@link #buf}. */
    private int size;

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
        this.buf        = new byte[INITIAL_CAPACITY];
        this.size       = 0;
        this.sampleRate = 0.0f;
        this.associatedMode = null;
        this.listeners  = new CopyOnWriteArrayList<>();
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
            buf        = Arrays.copyOf(samples, samples.length);
            size       = samples.length;
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
            buf        = new byte[INITIAL_CAPACITY];
            size       = 0;
            sampleRate = 0.0f;
        } finally {
            lock.writeLock().unlock();
        }
        notifyListeners();
    }

    /**
     * Appends a chunk of audio data to the end of the buffer.
     *
     * <p>Uses a geometrically-growing (doubling) backing array so that streaming
     * a long WAV file requires only O(log n) array copies across all chunks, rather
     * than one full copy per chunk. On the first call after a {@link #clear()}, the
     * sample rate is set.
     *
     * @param chunk      the audio bytes to append; must not be {@code null}
     * @param offset     start position in {@code chunk}
     * @param length     number of bytes to append from {@code chunk}
     * @param sampleRate the sample rate of the incoming audio
     */
    public void appendChunk(byte[] chunk, int offset, int length, float sampleRate) {
        lock.writeLock().lock();
        try {
            int required = size + length;
            if (required > buf.length) {
                // Double until large enough — at most O(log n) copies across all appends
                int newCapacity = buf.length == 0 ? INITIAL_CAPACITY : buf.length;
                while (newCapacity < required) {
                    newCapacity <<= 1;
                }
                byte[] grown = new byte[newCapacity];
                System.arraycopy(buf, 0, grown, 0, size);
                buf = grown;
            }
            System.arraycopy(chunk, offset, buf, size, length);
            size += length;
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
            if (size == 0) {
                return 0;
            }
            int totalSamples = size / 2;
            int start = Math.max(0, Math.min(sampleStart, totalSamples));
            int count = Math.min(sampleCount, totalSamples - start);
            if (count <= 0) {
                return 0;
            }
            System.arraycopy(buf, start * 2, dest, 0, count * 2);
            return count;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Returns a copy of the most-recent portion of the audio, up to {@code maxBytes} bytes.
     *
     * <p>Use this in decoders instead of {@link #getSamples()} to avoid copying the
     * entire accumulated buffer on every decode tick. Pass the number of bytes that
     * covers the decoder's maximum analysis window — for example,
     * {@code (int)(15.0f * getSampleRate()) * 2} for a 15-second FT8 window at whatever
     * sample rate is currently loaded.
     *
     * <p>If the buffer holds less audio than requested, all available audio is returned.
     * Returns an empty array when the buffer is empty.
     *
     * @param maxBytes the maximum number of PCM bytes to return; must be &gt; 0
     * @return a copy of the trailing {@code maxBytes} (or fewer) bytes of audio;
     *         never {@code null}
     */
    public byte[] readDecoderWindow(int maxBytes) {
        lock.readLock().lock();
        try {
            if (size == 0) {
                return new byte[0];
            }
            int count  = Math.min(maxBytes, size);
            int start  = size - count;
            byte[] out = new byte[count];
            System.arraycopy(buf, start, out, 0, count);
            return out;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Returns a direct reference to the internal backing array without copying.
     *
     * <p><strong>Warning:</strong> the returned array must be treated as read-only,
     * and only bytes in the range {@code [0, getLength())} contain valid audio data —
     * the array may be larger than the amount of audio currently stored. The array
     * reference itself may be replaced at any time by a concurrent
     * {@link #load(byte[], float)} or {@link #clear()} call; {@link #appendChunk}
     * may grow the array. Use this only on hot paths where copying would be too
     * expensive, and always read {@link #getLength()} to find the valid byte count.
     *
     * @return the internal backing array; never {@code null}
     */
    public byte[] getSamplesRef() {
        lock.readLock().lock();
        try {
            return buf;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Returns the number of valid audio bytes currently stored.
     *
     * @return byte length of stored audio; {@code 0} when empty
     */
    public int getLength() {
        lock.readLock().lock();
        try {
            return size;
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
            return size == 0;
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
            if (size == 0) {
                return new byte[0];
            }
            return Arrays.copyOf(buf, size);
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
            if (size == 0 || sampleRate == 0.0f) {
                return 0.0;
            }
            int numSamples = size / 2; // 16-bit mono: 2 bytes per sample
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
