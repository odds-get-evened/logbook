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
 * Handles loading WAV audio files from your computer and recording radio audio to WAV files.
 *
 * <p>You can load an existing WAV file for decoding via {@link #loadFile(File)}, or record
 * live audio from your computer's audio input via {@link #startRecording(File)} and
 * {@link #stopRecording()}. When recording stops, the recorded audio is automatically
 * available for the pipeline to stream.
 *
 * <p>File loading no longer reads the entire WAV into memory. Instead, it reads
 * only the audio format header and stores the file reference. The actual audio
 * streaming is handled by the {@link AudioProducer} via
 * {@link AudioPipelineController#loadWavFile(File)}.
 *
 * @author Logbook Development Team
 * @version 1.0
 */
public class WavFileSource implements AudioSource {

    private File wavFile;
    private AudioBuffer buffer;
    private AudioFormat audioFormat;
    private boolean recording;
    private TargetDataLine recordingLine;
    private Thread recordingThread;
    private final AudioFormat recordingFormat;

    /**
     * Creates a new WAV file source with an empty audio buffer.
     *
     * <p>The recording format is fixed at 8000 Hz, 16-bit, mono — suitable for
     * all supported digital modes.
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
     * Reads the audio format header from a WAV file and stores the file
     * reference for later streaming by the pipeline.
     *
     * <p>Unlike the previous implementation, this method does <strong>not</strong>
     * read the file contents into memory. The actual streaming is handled by
     * {@link AudioProducer}.
     *
     * @param file the WAV file to open; must not be {@code null} and must exist on disk
     * @throws IOException                   if the file cannot be read
     * @throws UnsupportedAudioFileException if the file format is not supported
     */
    public void loadFile(File file) throws IOException, UnsupportedAudioFileException {
        try (AudioInputStream ais = AudioSystem.getAudioInputStream(file)) {
            this.audioFormat = ais.getFormat();
        }
        this.wavFile = file;
    }

    /**
     * Returns the audio format of the most recently loaded or recorded file.
     *
     * @return the audio format, or {@code null} if no file has been loaded
     */
    public AudioFormat getAudioFormat() {
        return audioFormat;
    }

    /**
     * Begins recording audio from your computer's audio input and saves it to the selected file.
     *
     * <p>Recording continues until you call {@link #stopRecording()}. The audio is saved
     * as a WAV file at the path you specify.
     *
     * @param outputFile the file to save the recording into
     * @throws LineUnavailableException if no audio input is available on this computer
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
     * Stops the current recording, saves the file, and makes the recorded file
     * available for streaming.
     *
     * <p>After this call, the recorded file can be loaded into the streaming
     * pipeline via {@link AudioPipelineController#loadWavFile(File)}.
     *
     * @throws InterruptedException          if the operation is interrupted while finishing the recording
     * @throws IOException                   if the recorded file cannot be read back after saving
     * @throws UnsupportedAudioFileException if the saved file format cannot be loaded
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
     * No action is taken for a file-based source.
     *
     * <p>Use {@link #loadFile(File)} to open a WAV file, or
     * {@link #startRecording(File)} to begin recording.
     */
    @Override
    public void start() {
        // File source: no action required.
    }

    /**
     * Stops an active recording if one is in progress.
     *
     * <p>Equivalent to pressing Stop Recording. Any errors during the stop
     * are handled silently.
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
     * @return {@code true} if currently recording audio
     */
    @Override
    public boolean isActive() {
        return recording;
    }

    /**
     * Returns the audio buffer used by this source.
     *
     * @return the buffer; never {@code null}
     */
    @Override
    public AudioBuffer getBuffer() {
        return buffer;
    }

    /**
     * Returns {@code true} if a live recording is currently in progress.
     *
     * @return {@code true} if currently recording
     */
    public boolean isRecording() {
        return recording;
    }

    /**
     * Returns the WAV file most recently opened or recorded to.
     *
     * <p>Returns {@code null} if no file operation has been performed yet.
     *
     * @return the current WAV file, or {@code null}
     */
    public File getWavFile() {
        return wavFile;
    }
}
