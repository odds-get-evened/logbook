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
 * loaded into the decoder so you can decode it immediately.
 *
 * @author Logbook Development Team
 * @version 1.0
 */
public class WavFileSource implements AudioSource {

    private File wavFile;
    private AudioBuffer buffer;
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
     * Opens an audio file from your computer and loads it into the application for decoding.
     *
     * <p>Any previously loaded audio is replaced. The spectrum display and decoder will
     * immediately begin working with the newly loaded file.
     *
     * @param file the WAV file to open; must not be {@code null} and must exist on disk
     * @throws IOException                   if the file cannot be read
     * @throws UnsupportedAudioFileException if the file format is not supported
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
     * Stops the current recording, saves the file, and automatically loads the recorded audio for decoding.
     *
     * <p>After this call, the recorded audio is available in the spectrum display and decoder
     * exactly as if you had opened the file manually. If no recording is in progress, this
     * method does nothing.
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
