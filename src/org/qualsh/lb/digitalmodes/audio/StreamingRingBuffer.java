package org.qualsh.lb.digitalmodes.audio;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Thread-safe circular byte buffer with blocking semantics for the audio
 * producer-consumer pipeline.
 *
 * <p>The producer thread calls {@link #write(byte[], int, int)} to push audio
 * data into the buffer; if the buffer is full the call blocks until the consumer
 * drains enough space. The consumer thread calls {@link #read(byte[], int, int)}
 * to pull data out; if the buffer is empty the call blocks until the producer
 * writes more data.
 *
 * <p>The internal byte array is allocated once at construction and reused for
 * the lifetime of the buffer. Call {@link #reset()} to clear contents without
 * reallocating, or {@link #close()} to permanently unblock waiting threads
 * when the pipeline is shutting down.
 *
 * <p>Synchronisation uses a single {@link ReentrantLock} with two
 * {@link Condition} objects ({@code notFull} and {@code notEmpty}) for
 * efficient blocking without busy-waiting.
 *
 * @author Logbook Development Team
 * @version 1.0
 */
public class StreamingRingBuffer {

    /** Default capacity: 64 KB (~0.7 s of 16-bit 44.1 kHz mono audio). */
    private static final int DEFAULT_CAPACITY = 65536;

    private final byte[] data;
    private final int capacity;
    private int head;   // next write position
    private int tail;   // next read position
    private int count;  // bytes currently stored

    private final ReentrantLock lock = new ReentrantLock();
    private final Condition notFull  = lock.newCondition();
    private final Condition notEmpty = lock.newCondition();

    private volatile boolean closed;

    /**
     * Creates a streaming ring buffer with the default capacity of 64 KB.
     */
    public StreamingRingBuffer() {
        this(DEFAULT_CAPACITY);
    }

    /**
     * Creates a streaming ring buffer with the specified capacity.
     *
     * @param capacity buffer size in bytes; must be positive
     * @throws IllegalArgumentException if {@code capacity} is not positive
     */
    public StreamingRingBuffer(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity must be positive");
        }
        this.capacity = capacity;
        this.data = new byte[capacity];
    }

    /**
     * Writes bytes into the buffer, blocking if necessary until enough space is
     * available or the buffer is {@linkplain #close() closed}.
     *
     * <p>Data is copied from {@code src[offset .. offset+length-1]} into the
     * ring buffer. If the buffer cannot accept all bytes at once the caller
     * blocks on the {@code notFull} condition until space is freed by the
     * consumer.
     *
     * @param src    source array; must not be {@code null}
     * @param offset start position in {@code src}
     * @param length number of bytes to write
     * @throws InterruptedException if the calling thread is interrupted while
     *                              waiting for space
     */
    public void write(byte[] src, int offset, int length) throws InterruptedException {
        int written = 0;
        lock.lockInterruptibly();
        try {
            while (written < length) {
                if (closed) {
                    return;
                }
                while (count == capacity) {
                    if (closed) {
                        return;
                    }
                    notFull.await();
                }
                int spaceAvailable = capacity - count;
                int toWrite = Math.min(length - written, spaceAvailable);
                for (int i = 0; i < toWrite; i++) {
                    data[head] = src[offset + written + i];
                    head = (head + 1) % capacity;
                }
                count += toWrite;
                written += toWrite;
                notEmpty.signalAll();
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Reads bytes from the buffer into {@code dest}, blocking if the buffer is
     * empty until data becomes available or the buffer is closed.
     *
     * <p>Returns the number of bytes actually copied into
     * {@code dest[offset .. offset+return-1]}. A return value of {@code 0}
     * indicates that the buffer has been {@linkplain #close() closed} and no
     * more data will arrive.
     *
     * @param dest   destination array; must not be {@code null}
     * @param offset start position in {@code dest}
     * @param length maximum number of bytes to read
     * @return number of bytes actually read; {@code 0} when closed and empty
     * @throws InterruptedException if the calling thread is interrupted while
     *                              waiting for data
     */
    public int read(byte[] dest, int offset, int length) throws InterruptedException {
        lock.lockInterruptibly();
        try {
            while (count == 0) {
                if (closed) {
                    return 0;
                }
                notEmpty.await();
            }
            int toRead = Math.min(length, count);
            for (int i = 0; i < toRead; i++) {
                dest[offset + i] = data[tail];
                tail = (tail + 1) % capacity;
            }
            count -= toRead;
            notFull.signalAll();
            return toRead;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Permanently closes this buffer, unblocking any threads waiting in
     * {@link #write} or {@link #read}.
     *
     * <p>After closing, {@link #write} returns immediately without storing
     * data, and {@link #read} returns {@code 0} once the remaining buffered
     * data has been drained.
     */
    public void close() {
        lock.lock();
        try {
            closed = true;
            notFull.signalAll();
            notEmpty.signalAll();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Resets the buffer to an empty, open state so it can be reused for a new
     * audio file without reallocating the underlying array.
     */
    public void reset() {
        lock.lock();
        try {
            head = 0;
            tail = 0;
            count = 0;
            closed = false;
            notFull.signalAll();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Returns the number of bytes currently available to read.
     *
     * @return buffered byte count
     */
    public int available() {
        lock.lock();
        try {
            return count;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Returns {@code true} if this buffer has been closed.
     *
     * @return {@code true} after {@link #close()} has been called and before
     *         {@link #reset()}
     */
    public boolean isClosed() {
        return closed;
    }
}
