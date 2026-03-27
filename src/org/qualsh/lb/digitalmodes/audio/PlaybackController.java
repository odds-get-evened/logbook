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
 * <p>Playback runs quietly in the background so the rest of the application
 * remains fully responsive while audio is playing.
 *
 * @author Logbook Development Team
 * @version 1.0
 */
public class PlaybackController {

    private static final int CHUNK_BYTES = 4096;

    private final AudioBuffer buffer;
    private volatile boolean playing;
    private volatile boolean looping;
    private volatile int playbackPositionSamples;
    private SourceDataLine outputLine;
    private Thread playbackThread;

    /**
     * Creates a new playback controller linked to the given audio buffer.
     *
     * @param buffer the audio buffer to play back; must not be {@code null}
     */
    public PlaybackController(AudioBuffer buffer) {
        this.buffer = buffer;
    }

    /**
     * Starts playing the loaded audio through your computer's speakers or headphones.
     *
     * <p>If no audio is loaded, or if playback is already running, this method has no effect.
     * When the audio reaches the end, playback stops automatically unless looping is turned on,
     * in which case it restarts from the beginning.
     */
    public synchronized void play() {
        if (buffer.isEmpty() || playing) {
            return;
        }

        AudioFormat format = new AudioFormat(
                AudioFormat.Encoding.PCM_SIGNED,
                buffer.getSampleRate(),
                16,    // sample size in bits
                1,     // channels (mono)
                2,     // frame size
                buffer.getSampleRate(),
                false  // little-endian
        );

        try {
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
            outputLine = (SourceDataLine) AudioSystem.getLine(info);
            outputLine.open(format);
            outputLine.start();
        } catch (LineUnavailableException e) {
            return;
        }

        playing = true;

        playbackThread = new Thread(() -> {
            try {
                while (playing) {
                    byte[] samples = buffer.getSamples();
                    int bytePos = playbackPositionSamples * 2; // 2 bytes per sample

                    while (bytePos < samples.length && playing) {
                        int remaining = samples.length - bytePos;
                        int toWrite = Math.min(CHUNK_BYTES, remaining);
                        outputLine.write(samples, bytePos, toWrite);
                        bytePos += toWrite;
                        playbackPositionSamples = bytePos / 2;
                    }

                    if (playing) {
                        if (looping) {
                            playbackPositionSamples = 0;
                        } else {
                            break;
                        }
                    }
                }
            } finally {
                if (outputLine != null) {
                    outputLine.drain();
                    outputLine.close();
                }
                playing = false;
            }
        }, "PlaybackController-playback");
        playbackThread.setDaemon(true);
        playbackThread.start();
    }

    /**
     * Pauses playback at the current position.
     *
     * <p>Press Play to resume from the same spot. If playback is not currently
     * active, this method has no effect.
     */
    public synchronized void pause() {
        if (!playing) {
            return;
        }
        playing = false;
        stopPlaybackThread();
        closeOutputLine();
    }

    /**
     * Stops playback and returns to the beginning of the audio.
     *
     * <p>The position is reset even if playback was not active at the time of the call.
     */
    public synchronized void stop() {
        playing = false;
        stopPlaybackThread();
        closeOutputLine();
        playbackPositionSamples = 0;
    }

    /**
     * Turns looping on or off.
     *
     * <p>When looping is turned on, the audio will automatically restart from the
     * beginning each time it finishes playing. This setting takes effect immediately,
     * even during active playback.
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
     * Returns the current playback position in seconds from the start of the audio.
     *
     * <p>Returns {@code 0.0} if no audio is loaded or the sample rate is unknown.
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

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    private void stopPlaybackThread() {
        if (playbackThread != null) {
            playbackThread.interrupt();
            try {
                playbackThread.join(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            playbackThread = null;
        }
    }

    private void closeOutputLine() {
        if (outputLine != null) {
            outputLine.drain();
            outputLine.close();
            outputLine = null;
        }
    }
}
