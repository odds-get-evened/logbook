package org.qualsh.lb.digitalmodes.audio;

import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * A fixed-capacity circular buffer of normalized audio samples (float, range −1.0 to 1.0).
 *
 * <p>Designed for single-producer / multi-reader use in the audio pipeline.
 * {@link #write(float[], int)} is called by the audio capture thread; it overwrites the
 * oldest samples when the buffer is full (ring semantics). Readers such as the DSP consumer
 * thread call {@link #readLatest(float[], int)} to obtain the most-recent N samples without
 * disturbing the write position.
 *
 * <p>Thread safety is provided by a {@link ReentrantReadWriteLock}: concurrent reads are
 * allowed; writes exclude all other accessors.
 *
 * @author Logbook Development Team
 * @version 1.0
 */
public class AudioRingBuffer {

    private final float[] data;
    private final int capacity;
    private long totalWritten;   // total samples ever written; never wraps on long
    private float sampleRate;

    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    /**
     * Creates a ring buffer with the given capacity.
     *
     * @param capacitySamples number of samples the buffer can hold; must be positive
     * @param sampleRate      sample rate of the audio that will be written, in Hz
     * @throws IllegalArgumentException if {@code capacitySamples} is not positive
     */
    public AudioRingBuffer(int capacitySamples, float sampleRate) {
        if (capacitySamples <= 0) {
            throw new IllegalArgumentException("capacitySamples must be positive");
        }
        this.capacity = capacitySamples;
        this.data = new float[capacitySamples];
        this.totalWritten = 0;
        this.sampleRate = sampleRate;
    }

    /**
     * Writes up to {@code len} normalized samples from {@code chunk} into the buffer.
     *
     * <p>When the buffer is full, the oldest samples are silently overwritten.
     * Only the single producer (audio capture) thread should call this method.
     *
     * @param chunk source array of normalized samples; must not be {@code null}
     * @param len   number of samples to write from the start of {@code chunk};
     *              clamped to {@code chunk.length} if larger
     */
    public void write(float[] chunk, int len) {
        if (len <= 0) {
            return;
        }
        int actual = Math.min(len, chunk.length);
        lock.writeLock().lock();
        try {
            for (int i = 0; i < actual; i++) {
                data[(int) (totalWritten % capacity)] = chunk[i];
                totalWritten++;
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Copies the most-recent {@code count} samples into {@code dest[0..count-1]}.
     *
     * <p>If fewer than {@code count} samples have been written so far, as many as are
     * available are copied starting at {@code dest[0]}.  The rest of {@code dest} is
     * not modified.
     *
     * @param dest  destination array; must have length ≥ {@code count}
     * @param count number of recent samples requested
     * @return the number of samples actually copied (≤ {@code count})
     */
    public int readLatest(float[] dest, int count) {
        lock.readLock().lock();
        try {
            int available = (int) Math.min(totalWritten, capacity);
            int toCopy = Math.min(count, available);
            if (toCopy <= 0) {
                return 0;
            }
            long startIdx = totalWritten - toCopy;
            for (int i = 0; i < toCopy; i++) {
                dest[i] = data[(int) ((startIdx + i) % capacity)];
            }
            return toCopy;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Converts the most-recent {@code count} samples to a 16-bit signed little-endian PCM
     * byte array, suitable for loading into an {@link AudioBuffer}.
     *
     * <p>If fewer than {@code count} samples are available, a shorter array is returned.
     *
     * @param count maximum number of samples to include in the snapshot
     * @return PCM byte array (2 bytes per sample, little-endian); never {@code null}
     */
    public byte[] snapshotAsPcm(int count) {
        lock.readLock().lock();
        try {
            int available = (int) Math.min(totalWritten, capacity);
            int toCopy = Math.min(count, available);
            if (toCopy <= 0) {
                return new byte[0];
            }
            byte[] pcm = new byte[toCopy * 2];
            long startIdx = totalWritten - toCopy;
            for (int i = 0; i < toCopy; i++) {
                float f = data[(int) ((startIdx + i) % capacity)];
                int s = (int) Math.max(-32768, Math.min(32767, Math.round(f * 32767f)));
                pcm[i * 2]     = (byte)  (s & 0xFF);
                pcm[i * 2 + 1] = (byte) ((s >> 8) & 0xFF);
            }
            return pcm;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Returns the total number of samples written since this buffer was created.
     *
     * <p>This value is monotonically increasing and can be used as a position counter.
     *
     * @return total samples written; never decreases
     */
    public long totalSamplesWritten() {
        lock.readLock().lock();
        try {
            return totalWritten;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Returns the number of valid samples currently available to read (at most
     * equal to the buffer {@link #capacity()}).
     *
     * @return samples available; 0 if nothing has been written yet
     */
    public int available() {
        lock.readLock().lock();
        try {
            return (int) Math.min(totalWritten, capacity);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Returns the fixed capacity of this buffer in samples.
     *
     * @return capacity in samples; always positive
     */
    public int capacity() {
        return capacity;
    }

    /**
     * Returns the sample rate associated with the audio in this buffer.
     *
     * @return samples per second
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
     * Updates the sample rate. May be called from the producer thread if the rate changes.
     *
     * @param sampleRate new sample rate in samples per second
     */
    public void setSampleRate(float sampleRate) {
        lock.writeLock().lock();
        try {
            this.sampleRate = sampleRate;
        } finally {
            lock.writeLock().unlock();
        }
    }
}
