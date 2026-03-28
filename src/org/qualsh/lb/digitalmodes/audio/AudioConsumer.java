package org.qualsh.lb.digitalmodes.audio;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Dedicated consumer thread that pulls audio chunks from a
 * {@link StreamingRingBuffer} and distributes them to three destinations:
 *
 * <ol>
 *   <li><strong>Speaker playback</strong> — writes to a {@link SourceDataLine}
 *       when playback is active.</li>
 *   <li><strong>Decoder accumulation</strong> — appends each chunk to the
 *       shared {@link AudioBuffer} so decoders always have the full audio
 *       available.</li>
 *   <li><strong>DSP / UI listeners</strong> — notifies registered
 *       {@link AudioDataListener} instances at most 20 times per second
 *       (every 50 ms), using a {@link System#nanoTime()} check rather than
 *       {@code Thread.sleep()}.</li>
 * </ol>
 *
 * <p>All byte arrays used in the read loop are pre-allocated at construction
 * time and reused on every iteration — zero allocations inside the hot path.
 *
 * @author Logbook Development Team
 * @version 1.0
 */
public class AudioConsumer implements Runnable {

    private static final int CHUNK_SIZE = 4096;
    private static final long UI_UPDATE_INTERVAL_NANOS = 50_000_000L; // 50 ms → 20 FPS

    private final StreamingRingBuffer ringBuffer;
    private final AudioBuffer decoderBuffer;
    private final byte[] consumeChunk = new byte[CHUNK_SIZE];
    private final List<AudioDataListener> listeners = new CopyOnWriteArrayList<>();

    private volatile boolean stopped;
    private volatile boolean playbackActive;
    private volatile float sampleRate;
    private volatile int playbackPositionSamples;
    private volatile boolean looping;

    private SourceDataLine outputLine;
    private long lastUiUpdateNanos;

    // Fields used for looping support — producer signals when EOF is reached
    private volatile boolean eofReached;
    private final AudioProducer producer;
    private volatile PlaybackController playbackController;

    /**
     * Creates a new audio consumer.
     *
     * @param ringBuffer    the ring buffer to read audio chunks from;
     *                      must not be {@code null}
     * @param decoderBuffer the shared audio buffer that accumulates data for
     *                      decoders; must not be {@code null}
     * @param producer      the audio producer, used to re-trigger file load
     *                      when looping; must not be {@code null}
     */
    public AudioConsumer(StreamingRingBuffer ringBuffer,
                         AudioBuffer decoderBuffer,
                         AudioProducer producer) {
        this.ringBuffer    = ringBuffer;
        this.decoderBuffer = decoderBuffer;
        this.producer      = producer;
    }

    /**
     * Opens a {@link SourceDataLine} for speaker playback at the given format.
     *
     * @param format the audio format to play; must not be {@code null}
     * @throws LineUnavailableException if the system cannot provide an audio
     *                                  output line
     */
    public void startPlayback(AudioFormat format) throws LineUnavailableException {
        stopPlayback();
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
        outputLine = (SourceDataLine) AudioSystem.getLine(info);
        outputLine.open(format);
        outputLine.start();
        playbackActive = true;
        playbackPositionSamples = 0;
    }

    /**
     * Closes the speaker output line and stops playback.
     */
    public void stopPlayback() {
        playbackActive = false;
        if (outputLine != null) {
            outputLine.drain();
            outputLine.close();
            outputLine = null;
        }
        playbackPositionSamples = 0;
    }

    /**
     * Pauses playback without closing the output line. Call
     * {@link #resumePlayback()} to continue.
     */
    public void pausePlayback() {
        playbackActive = false;
        if (outputLine != null) {
            outputLine.stop();
        }
    }

    /**
     * Resumes playback after a {@link #pausePlayback()}.
     */
    public void resumePlayback() {
        if (outputLine != null) {
            outputLine.start();
            playbackActive = true;
        }
    }

    /**
     * Turns looping on or off. When enabled the audio restarts from the
     * beginning each time the file ends.
     *
     * @param loop {@code true} to loop continuously
     */
    public void setLooping(boolean loop) {
        this.looping = loop;
    }

    /**
     * Returns {@code true} if looping is enabled.
     *
     * @return {@code true} when audio will repeat automatically
     */
    public boolean isLooping() {
        return looping;
    }

    /**
     * Returns {@code true} if playback is currently active.
     *
     * @return {@code true} while audio is playing through speakers
     */
    public boolean isPlaying() {
        return playbackActive;
    }

    /**
     * Returns the current playback position in samples from the start.
     *
     * @return samples played; {@code 0} when at the beginning
     */
    public int getPlaybackPositionSamples() {
        return playbackPositionSamples;
    }

    /**
     * Sets the sample rate for the current audio stream.
     *
     * @param rate samples per second
     */
    public void setSampleRate(float rate) {
        this.sampleRate = rate;
    }

    /**
     * Registers a listener to receive audio chunks.
     *
     * @param listener the listener to add; must not be {@code null}
     */
    public void addListener(AudioDataListener listener) {
        listeners.add(listener);
    }

    /**
     * Removes a previously registered listener.
     *
     * @param listener the listener to remove
     */
    public void removeListener(AudioDataListener listener) {
        listeners.remove(listener);
    }

    /**
     * Sets a playback controller whose position will be kept in sync with
     * this consumer's playback position.
     *
     * @param controller the playback controller to update
     */
    public void setPlaybackController(PlaybackController controller) {
        this.playbackController = controller;
    }

    /**
     * Signals the consumer thread to stop.
     */
    public void stop() {
        stopped = true;
    }

    /**
     * Notifies the consumer that the producer has reached end-of-file.
     */
    public void signalEof() {
        eofReached = true;
    }

    /**
     * Resets the EOF flag, typically called when a new file is loaded.
     */
    public void resetEof() {
        eofReached = false;
    }

    /**
     * Main consumer loop. Pulls chunks from the ring buffer and distributes
     * them to playback, decoder buffer, and registered listeners.
     */
    @Override
    public void run() {
        lastUiUpdateNanos = System.nanoTime();

        while (!stopped) {
            try {
                int bytesRead = ringBuffer.read(consumeChunk, 0, CHUNK_SIZE);

                if (bytesRead == 0) {
                    // Ring buffer is closed (EOF from producer)
                    if (looping && playbackActive) {
                        // Re-load the file and continue
                        eofReached = false;
                        ringBuffer.reset();
                        decoderBuffer.clear();
                        playbackPositionSamples = 0;
                        // Producer will detect fileReady and resume reading
                        continue;
                    }
                    // Not looping — wait for a new file to be loaded
                    if (!stopped) {
                        Thread.sleep(50);
                    }
                    continue;
                }

                // 1. Feed to speaker playback if active
                if (playbackActive && outputLine != null) {
                    outputLine.write(consumeChunk, 0, bytesRead);
                    playbackPositionSamples += bytesRead / 2; // 16-bit mono: 2 bytes per sample
                    // Keep DspConsumerThread's position reference in sync
                    PlaybackController pc = playbackController;
                    if (pc != null) {
                        pc.setPlaybackPositionSamples(playbackPositionSamples);
                    }
                }

                // 2. Accumulate into decoder buffer
                decoderBuffer.appendChunk(consumeChunk, 0, bytesRead, sampleRate);

                // 3. Notify listeners (throttled to 20 FPS)
                long now = System.nanoTime();
                if (now - lastUiUpdateNanos >= UI_UPDATE_INTERVAL_NANOS) {
                    lastUiUpdateNanos = now;
                    float rate = sampleRate;
                    for (AudioDataListener listener : listeners) {
                        listener.onAudioChunk(consumeChunk, bytesRead, rate);
                    }
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }

        // Clean up on exit
        stopPlayback();
    }
}
