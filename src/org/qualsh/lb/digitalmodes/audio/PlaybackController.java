package org.qualsh.lb.digitalmodes.audio;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

/**
 * Controls playback of audio stored in an {@link AudioBuffer}.
 *
 * <p>Audio is rendered through the default system output using a
 * {@link SourceDataLine}. Playback runs on a dedicated background thread,
 * leaving the calling thread free for UI updates.
 *
 * <p>Typical usage:
 * <pre>{@code
 * PlaybackController pc = new PlaybackController(myBuffer);
 * pc.setLooping(true);
 * pc.play();
 * // ... later ...
 * pc.stop();
 * }</pre>
 *
 * <p>Only {@link javax.sound.sampled} is used — no external libraries are
 * required. The buffer is assumed to contain 16-bit, mono, signed,
 * little-endian PCM audio.
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
     * Creates a new {@code PlaybackController} that will play audio from the
     * given buffer.
     *
     * @param buffer the audio buffer to play back; must not be {@code null}
     */
    public PlaybackController(AudioBuffer buffer) {
        this.buffer = buffer;
    }

    /**
     * Starts playback from the current position.
     *
     * <p>If the buffer is empty this method returns immediately without
     * doing anything. Otherwise a {@link SourceDataLine} is opened using
     * the buffer's sample rate (16-bit, mono, signed, little-endian) and a
     * background thread begins writing audio data to the line in 4096-byte
     * chunks.
     *
     * <p>When the end of the buffer is reached:
     * <ul>
     *   <li>If {@link #setLooping(boolean) looping} is {@code true} the
     *       position resets to zero and playback continues seamlessly.</li>
     *   <li>Otherwise playback stops automatically, the line is closed, and
     *       {@link #isPlaying()} returns {@code false}.</li>
     * </ul>
     *
     * <p>Calling {@code play()} while already playing has no effect.
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
     * <p>The playback thread is stopped and the output line is closed. The
     * current sample position is preserved so that a subsequent call to
     * {@link #play()} resumes from where playback left off.
     *
     * <p>If playback is not currently active this method has no effect.
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
     * Stops playback and resets the position to the beginning of the buffer.
     *
     * <p>The playback thread is stopped, the output line is closed, and
     * {@link #getPlaybackPositionSamples()} returns {@code 0} after this
     * call. If playback is not currently active the position is still reset.
     */
    public synchronized void stop() {
        playing = false;
        stopPlaybackThread();
        closeOutputLine();
        playbackPositionSamples = 0;
    }

    /**
     * Enables or disables looping.
     *
     * <p>When looping is enabled, playback automatically restarts from the
     * beginning each time the end of the buffer is reached. This setting
     * takes effect immediately, even during active playback.
     *
     * @param loop {@code true} to loop continuously; {@code false} to stop
     *             at the end of the buffer
     */
    public void setLooping(boolean loop) {
        this.looping = loop;
    }

    /**
     * Returns whether looping is currently enabled.
     *
     * @return {@code true} if looping is enabled
     */
    public boolean isLooping() {
        return looping;
    }

    /**
     * Returns whether audio is currently being played back.
     *
     * @return {@code true} while the playback thread is active
     */
    public boolean isPlaying() {
        return playing;
    }

    /**
     * Returns the current playback position as a sample index.
     *
     * <p>For 16-bit mono audio, multiply by 2 to get the corresponding
     * byte offset in the buffer.
     *
     * @return the number of samples from the start of the buffer at which
     *         playback is currently positioned
     */
    public int getPlaybackPositionSamples() {
        return playbackPositionSamples;
    }

    /**
     * Returns the current playback position in seconds.
     *
     * <p>Calculated as
     * {@code playbackPositionSamples / buffer.getSampleRate()}.
     * Returns {@code 0.0} if the buffer's sample rate is zero.
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
