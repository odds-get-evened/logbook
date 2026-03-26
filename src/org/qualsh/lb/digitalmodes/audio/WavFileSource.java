package org.qualsh.lb.digitalmodes.audio;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.File;
import java.io.IOException;

/**
 * An {@link AudioSource} that reads audio from a WAV file or records live
 * audio input to a WAV file.
 *
 * <p>Two usage patterns are supported:
 * <ol>
 *   <li><strong>File import</strong> — call {@link #loadFile(File)} to read
 *       an existing WAV file into the buffer immediately.</li>
 *   <li><strong>Live recording</strong> — call {@link #startRecording(File)}
 *       to capture microphone input into a WAV file, then
 *       {@link #stopRecording()} to finish and automatically load the
 *       recorded audio into the buffer.</li>
 * </ol>
 *
 * <p>Audio is stored as 16-bit, mono, signed, little-endian PCM at 8000 Hz.
 */
public class WavFileSource implements AudioSource {

    private File wavFile;
    private AudioBuffer buffer;
    private boolean recording;
    private TargetDataLine recordingLine;
    private Thread recordingThread;
    private final AudioFormat recordingFormat;

    /**
     * Creates a new {@code WavFileSource} with an empty buffer.
     *
     * <p>The recording format is fixed at 8000 Hz, 16-bit, mono, signed,
     * little-endian PCM.
     */
    public WavFileSource() {
        this.buffer = new AudioBuffer();
        this.recordingFormat = new AudioFormat(
                AudioFormat.Encoding.PCM_SIGNED,
                8000f,   // sample rate
                16,      // sample size in bits
                1,       // channels (mono)
                2,       // frame size (16-bit mono = 2 bytes)
                8000f,   // frame rate
                false    // little-endian
        );
    }

    /**
     * Loads a WAV file into the buffer, replacing any previously held audio.
     *
     * <p>The file is decoded using {@link AudioSystem} and its raw PCM bytes
     * are stored via {@link AudioBuffer#load(byte[], float)}. The file
     * reference is retained and returned by {@link #getWavFile()}.
     *
     * @param file the WAV file to load; must not be {@code null} and must
     *             exist on disk
     * @throws IOException                   if an I/O error occurs reading
     *                                       the file
     * @throws UnsupportedAudioFileException if the file format is not
     *                                       supported by the system
     */
    public void loadFile(File file) throws IOException, UnsupportedAudioFileException {
        try (AudioInputStream ais = AudioSystem.getAudioInputStream(file)) {
            AudioFormat format = ais.getFormat();
            byte[] bytes = ais.readAllBytes();
            buffer.load(bytes, format.getSampleRate());
        }
        this.wavFile = file;
    }

    /**
     * Starts recording live audio input to the specified output file.
     *
     * <p>A {@link TargetDataLine} is opened with the fixed recording format
     * (8000 Hz, 16-bit mono). Audio is captured on a background thread and
     * written to {@code outputFile} as a WAV file using
     * {@link AudioSystem#write}. Call {@link #stopRecording()} to finish.
     *
     * @param outputFile the file to write the recorded WAV audio into
     * @throws LineUnavailableException if no suitable input line is available
     *                                  on the system
     */
    public void startRecording(File outputFile) throws LineUnavailableException {
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, recordingFormat);
        recordingLine = (TargetDataLine) AudioSystem.getLine(info);
        recordingLine.open(recordingFormat);
        recordingLine.start();
        this.wavFile = outputFile;
        recording = true;

        recordingThread = new Thread(() -> {
            try (AudioInputStream ais = new AudioInputStream(recordingLine)) {
                AudioSystem.write(ais, AudioFileFormat.Type.WAVE, outputFile);
            } catch (IOException e) {
                Thread.currentThread().interrupt();
            }
        }, "WavFileSource-recording");
        recordingThread.setDaemon(true);
        recordingThread.start();
    }

    /**
     * Stops an in-progress recording and loads the recorded audio into the
     * buffer.
     *
     * <p>The {@link TargetDataLine} is stopped and closed, the recording
     * thread is joined, and {@link #loadFile(File)} is called on the output
     * file so the buffer is immediately populated with the captured audio.
     * After this call {@link #isRecording()} returns {@code false}.
     *
     * <p>If no recording is in progress this method returns without doing
     * anything.
     *
     * @throws InterruptedException          if the current thread is
     *                                       interrupted while waiting for
     *                                       the recording thread to finish
     * @throws IOException                   if the recorded file cannot be
     *                                       read back into the buffer
     * @throws UnsupportedAudioFileException if the recorded file format is
     *                                       not supported
     */
    public void stopRecording()
            throws InterruptedException, IOException, UnsupportedAudioFileException {
        if (!recording) {
            return;
        }
        recordingLine.stop();
        recordingLine.close();
        if (recordingThread != null) {
            recordingThread.join();
            recordingThread = null;
        }
        recording = false;

        if (wavFile != null) {
            loadFile(wavFile);
        }
    }

    /**
     * No-op for a file-based source.
     *
     * <p>Use {@link #loadFile(File)} to populate the buffer from a WAV file,
     * or {@link #startRecording(File)} to begin live capture.
     */
    @Override
    public void start() {
        // File source: no action required.
    }

    /**
     * Stops an active recording if one is in progress.
     *
     * <p>Equivalent to calling {@link #stopRecording()} when
     * {@link #isRecording()} is {@code true}. Errors during stop are
     * silently swallowed so that callers are not forced to handle checked
     * exceptions from a generic stop path.
     */
    @Override
    public void stop() {
        if (recording) {
            try {
                stopRecording();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (IOException | UnsupportedAudioFileException e) {
                // best-effort stop
            }
        }
    }

    /**
     * Returns {@code true} while a live recording is in progress.
     *
     * @return {@code true} if currently recording
     */
    @Override
    public boolean isActive() {
        return recording;
    }

    /**
     * Returns the {@link AudioBuffer} used by this source.
     *
     * @return the buffer; never {@code null}
     */
    @Override
    public AudioBuffer getBuffer() {
        return buffer;
    }

    /**
     * Returns {@code true} while a live recording is in progress.
     *
     * @return {@code true} if currently recording
     */
    public boolean isRecording() {
        return recording;
    }

    /**
     * Returns the WAV file most recently loaded or recorded to, or
     * {@code null} if no file operation has been performed yet.
     *
     * @return the current WAV file, or {@code null}
     */
    public File getWavFile() {
        return wavFile;
    }
}
