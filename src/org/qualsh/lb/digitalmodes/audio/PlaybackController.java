package org.qualsh.lb.digitalmodes.audio;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

/**
 * Controls playback of whatever audio is currently loaded in the application.
 *
 * <p>Audio plays through your computer's default speakers or headphones. Use
 * {@link #play()}, {@link #pause()}, and {@link #stop()} to control playback,
 * and {@link #setLooping(boolean)} to repeat audio automatically.
 *
 * <p>In the streaming pipeline, playback is driven by the
 * {@link AudioPipelineController} rather than an internal thread.
 * The controller's playback position is exposed for the DSP consumer
 * thread to track the current FFT window.
 *
 * @author Logbook Development Team
 * @version 1.0
 */
public class PlaybackController {

    private final AudioBuffer buffer;
    private volatile boolean playing;
    private volatile boolean looping;
    private volatile int playbackPositionSamples;

    /**
     * Creates a new playback controller linked to the given audio buffer.
     *
     * @param buffer the audio buffer to play back; must not be {@code null}
     */
    public PlaybackController(AudioBuffer buffer) {
        this.buffer = buffer;
    }

    /**
     * Starts playing. In the streaming pipeline, this is a state flag —
     * actual audio output is handled by {@link AudioPipelineController}.
     */
    public synchronized void play() {
        if (buffer.isEmpty()) {
            return;
        }
        playing = true;
    }

    /**
     * Pauses playback at the current position.
     */
    public synchronized void pause() {
        playing = false;
    }

    /**
     * Stops playback and resets position to the beginning.
     */
    public synchronized void stop() {
        playing = false;
        playbackPositionSamples = 0;
    }

    /**
     * Turns looping on or off.
     *
     * @param loop {@code true} to loop continuously; {@code false} to stop at the end
     */
    public void setLooping(boolean loop) {
        this.looping = loop;
    }

    /**
     * Returns {@code true} if looping is currently turned on.
     *
     * @return {@code true} if audio will repeat automatically when it finishes
     */
    public boolean isLooping() {
        return looping;
    }

    /**
     * Returns {@code true} if audio is currently playing.
     *
     * @return {@code true} while playback is active
     */
    public boolean isPlaying() {
        return playing;
    }

    /**
     * Returns the current playback position measured in audio samples from the start.
     *
     * @return the number of samples played so far; {@code 0} when at the beginning
     */
    public int getPlaybackPositionSamples() {
        return playbackPositionSamples;
    }

    /**
     * Updates the playback position. Called by the pipeline controller as
     * audio chunks are consumed.
     *
     * @param samples the new position in samples from the start
     */
    public void setPlaybackPositionSamples(int samples) {
        this.playbackPositionSamples = samples;
    }

    /**
     * Returns the current playback position in seconds from the start of the audio.
     *
     * @return the playback position in seconds
     */
    public double getPlaybackPositionSeconds() {
        float sampleRate = buffer.getSampleRate();
        if (sampleRate == 0f) {
            return 0.0;
        }
        return playbackPositionSamples / (double) sampleRate;
    }
}
