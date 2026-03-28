package org.qualsh.lb.digitalmodes.audio;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Lifecycle manager for the streaming audio pipeline used by the Digital Modes
 * window.
 *
 * <p>Owns the {@link StreamingRingBuffer}, {@link AudioProducer}, and
 * {@link AudioConsumer} and manages them through a fixed thread pool of two
 * threads. All public methods are designed to be called from the Swing Event
 * Dispatch Thread.
 *
 * <p>Typical usage:
 * <pre>{@code
 * AudioPipelineController pipeline = new AudioPipelineController(sharedBuffer);
 * pipeline.start();
 * pipeline.loadWavFile(selectedFile);
 * pipeline.startPlayback();
 * // ... later ...
 * pipeline.shutdown();
 * }</pre>
 *
 * @author Logbook Development Team
 * @version 1.0
 */
public class AudioPipelineController {

    private final StreamingRingBuffer ringBuffer;
    private final AudioProducer producer;
    private final AudioConsumer consumer;
    private final AudioBuffer decoderBuffer;

    private ExecutorService executor;
    private volatile AudioFormat currentFormat;
    private volatile File currentFile;

    /**
     * Creates a new pipeline controller.
     *
     * <p>The ring buffer is allocated once here and reused for every file load.
     * The decoder buffer receives audio chunks incrementally as the consumer
     * processes them.
     *
     * @param decoderBuffer the shared audio buffer that accumulates data for
     *                      decoders; must not be {@code null}
     */
    private PlaybackController playbackController;

    /**
     * Creates a new pipeline controller.
     *
     * <p>The ring buffer is allocated once here and reused for every file load.
     * The decoder buffer receives audio chunks incrementally as the consumer
     * processes them.
     *
     * @param decoderBuffer the shared audio buffer that accumulates data for
     *                      decoders; must not be {@code null}
     */
    public AudioPipelineController(AudioBuffer decoderBuffer) {
        this.decoderBuffer = decoderBuffer;
        this.ringBuffer    = new StreamingRingBuffer();
        this.producer      = new AudioProducer(ringBuffer);
        this.consumer      = new AudioConsumer(ringBuffer, decoderBuffer, producer);
    }

    /**
     * Sets the playback controller whose position will be kept in sync with
     * the streaming consumer's playback position. This allows the DSP consumer
     * thread to track the current FFT window position.
     *
     * @param controller the playback controller to synchronise with
     */
    public void setPlaybackController(PlaybackController controller) {
        this.playbackController = controller;
        consumer.setPlaybackController(controller);
    }

    /**
     * Starts the producer and consumer threads. Call this once when the
     * Digital Modes window opens.
     */
    public void start() {
        if (executor != null && !executor.isShutdown()) {
            return;
        }
        executor = Executors.newFixedThreadPool(2, r -> {
            Thread t = new Thread(r);
            t.setDaemon(true);
            return t;
        });
        executor.submit(producer);
        executor.submit(consumer);
    }

    /**
     * Loads a new WAV file into the pipeline.
     *
     * <p>The current audio is discarded, the decoder buffer is cleared, and the
     * producer begins streaming the new file in small chunks. If playback was
     * active it is stopped first.
     *
     * @param file the WAV file to load; must not be {@code null}
     * @throws IOException                   if the file cannot be read
     * @throws UnsupportedAudioFileException if the file format is not supported
     */
    public void loadWavFile(File file)
            throws IOException, UnsupportedAudioFileException {
        stopPlayback();
        decoderBuffer.clear();
        consumer.resetEof();
        producer.loadFile(file);
        currentFormat = producer.getAudioFormat();
        currentFile   = file;
        if (currentFormat != null) {
            consumer.setSampleRate(currentFormat.getSampleRate());
        }
    }

    /**
     * Starts playing the loaded audio through the computer's speakers.
     *
     * <p>If no file has been loaded or playback is already active this method
     * has no effect. The file is reloaded from the beginning so the user hears
     * the full audio.
     */
    public void startPlayback() {
        if (currentFormat == null || consumer.isPlaying()) {
            return;
        }
        try {
            // Reload file from start for playback
            if (currentFile != null) {
                decoderBuffer.clear();
                consumer.resetEof();
                producer.loadFile(currentFile);
                consumer.setSampleRate(currentFormat.getSampleRate());
            }
            consumer.startPlayback(currentFormat);
        } catch (LineUnavailableException e) {
            // No audio output available — silently ignore
        } catch (IOException | UnsupportedAudioFileException e) {
            // File re-open failed — silently ignore
        }
    }

    /**
     * Stops playback and resets the position to the beginning.
     */
    public void stopPlayback() {
        consumer.stopPlayback();
    }

    /**
     * Pauses playback at the current position.
     */
    public void pausePlayback() {
        consumer.pausePlayback();
        producer.pause();
    }

    /**
     * Resumes playback after a pause.
     */
    public void resumePlayback() {
        producer.resume();
        consumer.resumePlayback();
    }

    /**
     * Turns looping on or off.
     *
     * @param loop {@code true} to loop continuously
     */
    public void setLooping(boolean loop) {
        consumer.setLooping(loop);
    }

    /**
     * Returns {@code true} if looping is enabled.
     *
     * @return {@code true} when audio will repeat automatically
     */
    public boolean isLooping() {
        return consumer.isLooping();
    }

    /**
     * Returns {@code true} if audio is currently playing.
     *
     * @return {@code true} while playback is active
     */
    public boolean isPlaying() {
        return consumer.isPlaying();
    }

    /**
     * Returns the current playback position in samples.
     *
     * @return samples played from the beginning
     */
    public int getPlaybackPositionSamples() {
        return consumer.getPlaybackPositionSamples();
    }

    /**
     * Returns the current playback position in seconds.
     *
     * @param sampleRate the sample rate to use for conversion
     * @return seconds from the start of the audio
     */
    public double getPlaybackPositionSeconds(float sampleRate) {
        if (sampleRate == 0f) {
            return 0.0;
        }
        return consumer.getPlaybackPositionSamples() / (double) sampleRate;
    }

    /**
     * Registers a listener to receive audio chunks from the consumer thread.
     *
     * @param listener the listener to add; must not be {@code null}
     */
    public void addAudioDataListener(AudioDataListener listener) {
        consumer.addListener(listener);
    }

    /**
     * Removes a previously registered listener.
     *
     * @param listener the listener to remove
     */
    public void removeAudioDataListener(AudioDataListener listener) {
        consumer.removeListener(listener);
    }

    /**
     * Shuts down the pipeline, stopping both threads and releasing all audio
     * resources.
     *
     * <p>Call this when the Digital Modes window is closed. After shutdown the
     * controller cannot be restarted — create a new instance if needed.
     */
    public void shutdown() {
        producer.stop();
        consumer.stop();
        ringBuffer.close();
        if (executor != null) {
            executor.shutdownNow();
            try {
                executor.awaitTermination(2, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            executor = null;
        }
    }

    /**
     * Returns the audio format of the currently loaded file.
     *
     * @return the format, or {@code null} if no file is loaded
     */
    public AudioFormat getCurrentFormat() {
        return currentFormat;
    }
}
