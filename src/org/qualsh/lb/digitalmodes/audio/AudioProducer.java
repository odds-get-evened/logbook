package org.qualsh.lb.digitalmodes.audio;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Dedicated audio source thread that reads a WAV file in small, fixed-size
 * chunks and pushes them into a {@link StreamingRingBuffer}.
 *
 * <p>Instead of loading the entire file into memory at once, the producer
 * reads 4096 bytes per iteration and blocks naturally via
 * {@link StreamingRingBuffer#write(byte[], int, int)} when the consumer falls
 * behind. This keeps memory usage constant regardless of file size.
 *
 * <p>Supports {@link #pause()}/{@link #resume()} via volatile flags and
 * hot-swapping the audio source via {@link #loadFile(File)} without tearing
 * down the consumer thread.
 *
 * @author Logbook Development Team
 * @version 1.0
 */
public class AudioProducer implements Runnable {

    private static final int CHUNK_SIZE = 4096;

    private final StreamingRingBuffer ringBuffer;
    private final byte[] readChunk = new byte[CHUNK_SIZE];
    private final ReentrantLock streamLock = new ReentrantLock();

    private volatile AudioInputStream currentStream;
    private volatile AudioFormat currentFormat;
    private volatile boolean stopped;
    private volatile boolean paused;
    private volatile boolean fileReady;
    private volatile File lastLoadedFile;

    /**
     * Creates a new audio producer that writes into the given ring buffer.
     *
     * @param ringBuffer the ring buffer to write audio chunks into;
     *                   must not be {@code null}
     */
    public AudioProducer(StreamingRingBuffer ringBuffer) {
        this.ringBuffer = ringBuffer;
    }

    /**
     * Opens a WAV file for streaming. If a previous file was open it is closed
     * first. The ring buffer is reset so stale data from the old file is
     * discarded.
     *
     * <p>This method is safe to call from the EDT while the producer thread is
     * running; the stream lock ensures the swap is atomic with respect to the
     * read loop.
     *
     * @param file the WAV file to open; must not be {@code null}
     * @throws IOException                   if the file cannot be read
     * @throws UnsupportedAudioFileException if the file format is not supported
     */
    public void loadFile(File file) throws IOException, UnsupportedAudioFileException {
        streamLock.lock();
        try {
            closeCurrentStream();
            lastLoadedFile = file;
            ringBuffer.reset();
            AudioInputStream ais = AudioSystem.getAudioInputStream(file);
            currentFormat = ais.getFormat();
            currentStream = ais;
            fileReady = true;
        } finally {
            streamLock.unlock();
        }
    }

    /**
     * Re-opens the last loaded file from the beginning. Used by the consumer
     * thread to restart playback when looping is enabled.
     *
     * @throws IOException                   if the file cannot be read
     * @throws UnsupportedAudioFileException if the file format is not supported
     */
    public void reloadFile() throws IOException, UnsupportedAudioFileException {
        File file = lastLoadedFile;
        if (file != null) {
            loadFile(file);
        }
    }

    /**
     * Returns the audio format of the currently loaded file.
     *
     * @return the audio format, or {@code null} if no file is loaded
     */
    public AudioFormat getAudioFormat() {
        return currentFormat;
    }

    /**
     * Signals the producer thread to stop after the current chunk finishes.
     */
    public void stop() {
        stopped = true;
        ringBuffer.close();
        streamLock.lock();
        try {
            closeCurrentStream();
        } finally {
            streamLock.unlock();
        }
    }

    /**
     * Pauses production. The thread remains alive but stops reading from the
     * audio stream until {@link #resume()} is called.
     */
    public void pause() {
        paused = true;
    }

    /**
     * Resumes production after a {@link #pause()}.
     */
    public void resume() {
        paused = false;
    }

    /**
     * Main producer loop. Reads from the current {@link AudioInputStream} in
     * {@value #CHUNK_SIZE}-byte chunks and writes them into the ring buffer.
     * Blocks naturally when the ring buffer is full.
     */
    @Override
    public void run() {
        while (!stopped) {
            if (paused || !fileReady) {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
                continue;
            }

            streamLock.lock();
            AudioInputStream stream;
            try {
                stream = currentStream;
            } finally {
                streamLock.unlock();
            }

            if (stream == null) {
                fileReady = false;
                continue;
            }

            try {
                int bytesRead = stream.read(readChunk, 0, CHUNK_SIZE);
                if (bytesRead == -1) {
                    // End of file reached — signal consumer, wait for new file
                    fileReady = false;
                    ringBuffer.close();
                    continue;
                }
                ringBuffer.write(readChunk, 0, bytesRead);
            } catch (IOException e) {
                fileReady = false;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    private void closeCurrentStream() {
        if (currentStream != null) {
            try {
                currentStream.close();
            } catch (IOException ignored) {
                // best-effort close
            }
            currentStream = null;
        }
    }
}
