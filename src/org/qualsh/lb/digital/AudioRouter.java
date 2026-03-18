package org.qualsh.lb.digital;

import javax.sound.sampled.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * AudioRouter – low-latency soundcard audio capture and playback for digital modes.
 *
 * <h2>Architecture</h2>
 * <pre>
 *   Radio RX audio  ──►  Capture device (TargetDataLine)  ──►  byte[] callbacks
 *                                                                  │
 *                                                            decoder thread
 *                                                                  │
 *   Radio TX audio  ◄──  Playback device (SourceDataLine) ◄──  tone generator
 * </pre>
 *
 * <h2>Latency tuning</h2>
 * <p>The key knob is the buffer size fed to {@link DataLine.Info}.  A smaller buffer
 * means lower latency but higher risk of underrun/overrun (audible glitches).
 *
 * <p>Recommended values at 48 000 Hz / 16-bit / mono:
 * <pre>
 *   Frame size  = 2 bytes (16-bit mono)
 *   Buffer size = FRAMES_PER_BUFFER * frame_size
 *
 *   FRAMES_PER_BUFFER = 512  →  ~10.7 ms   (demanding; use for FT8/FT4 critical timing)
 *   FRAMES_PER_BUFFER = 1024 →  ~21 ms     (comfortable; recommended default)
 *   FRAMES_PER_BUFFER = 2048 →  ~42 ms     (safe for most systems)
 * </pre>
 *
 * <p>FT8 is 15-second T/R windows, FT4 is 7.5 seconds, JS8Call is 15 seconds.
 * End-to-end latency well under 100 ms is sufficient; aim for ≤ 21 ms.
 *
 * <h2>Audio device discovery</h2>
 * <p>Use {@link #availableCaptureDevices()} and {@link #availablePlaybackDevices()} to
 * populate combo-boxes in the settings dialog.  Devices are identified by
 * {@link Mixer.Info} name strings (e.g. "USB Audio CODEC", "Analog Stereo").
 *
 * <h2>Thread model</h2>
 * <ul>
 *   <li>Capture runs on a dedicated daemon thread {@code AudioRouter-capture}.</li>
 *   <li>Raw PCM bytes are delivered to registered {@link Consumer}&lt;byte[]&gt; listeners
 *       on that same thread – keep listeners non-blocking.</li>
 *   <li>Playback ({@link #play(byte[])}) blocks the caller until the buffer drains;
 *       call it from the audio-generation thread, never from the EDT.</li>
 * </ul>
 */
public class AudioRouter {

    // ── Audio format ─────────────────────────────────────────────────────────

    /** 48 kHz / 16-bit signed / mono – widest hardware support, good SNR. */
    public static final AudioFormat FORMAT = new AudioFormat(
            AudioFormat.Encoding.PCM_SIGNED,
            48_000f,   // sample rate (Hz)
            16,        // bits per sample
            1,         // channels (mono)
            2,         // frame size (bytes) = channels * (bits/8)
            48_000f,   // frame rate
            false      // little-endian (native for most hardware)
    );

    /** Number of PCM frames per capture/playback buffer. 1024 ≈ 21 ms at 48 kHz. */
    private static final int FRAMES_PER_BUFFER = 1024;
    private static final int BUFFER_BYTES = FRAMES_PER_BUFFER * FORMAT.getFrameSize();

    // ── Singleton ─────────────────────────────────────────────────────────────

    private static AudioRouter instance;

    public static synchronized AudioRouter getInstance() {
        if (instance == null) instance = new AudioRouter();
        return instance;
    }

    private AudioRouter() {}

    // ── State ─────────────────────────────────────────────────────────────────

    private TargetDataLine captureDevice;
    private SourceDataLine playbackDevice;
    private Thread captureThread;
    private final AtomicBoolean capturing = new AtomicBoolean(false);

    private final List<Consumer<byte[]>> captureListeners = new ArrayList<>();

    // ── Device discovery ──────────────────────────────────────────────────────

    /**
     * Return the names of all mixer lines that support audio capture
     * with the required format.
     */
    public static String[] availableCaptureDevices() {
        List<String> names = new ArrayList<>();
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, FORMAT);
        for (Mixer.Info mi : AudioSystem.getMixerInfo()) {
            try {
                Mixer m = AudioSystem.getMixer(mi);
                if (m.isLineSupported(info)) names.add(mi.getName());
            } catch (Exception ignored) {}
        }
        return names.toArray(new String[0]);
    }

    /**
     * Return the names of all mixer lines that support audio playback
     * with the required format.
     */
    public static String[] availablePlaybackDevices() {
        List<String> names = new ArrayList<>();
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, FORMAT);
        for (Mixer.Info mi : AudioSystem.getMixerInfo()) {
            try {
                Mixer m = AudioSystem.getMixer(mi);
                if (m.isLineSupported(info)) names.add(mi.getName());
            } catch (Exception ignored) {}
        }
        return names.toArray(new String[0]);
    }

    // ── Capture (Radio → Application) ─────────────────────────────────────────

    /**
     * Open the named capture device and start reading audio in a background thread.
     *
     * @param deviceName mixer name from {@link #availableCaptureDevices()}, or
     *                   {@code null} to use the system default
     * @return {@code true} on success
     */
    public synchronized boolean startCapture(String deviceName) {
        stopCapture();
        try {
            captureDevice = openTargetLine(deviceName);
            captureDevice.start();
            capturing.set(true);

            captureThread = new Thread(() -> {
                byte[] buf = new byte[BUFFER_BYTES];
                while (capturing.get()) {
                    int read = captureDevice.read(buf, 0, buf.length);
                    if (read > 0) {
                        byte[] chunk = new byte[read];
                        System.arraycopy(buf, 0, chunk, 0, read);
                        notifyCaptureListeners(chunk);
                    }
                }
            }, "AudioRouter-capture");
            captureThread.setDaemon(true);
            captureThread.start();
            return true;
        } catch (LineUnavailableException e) {
            System.err.println("AudioRouter: capture open failed – " + e.getMessage());
            return false;
        }
    }

    /** Stop audio capture and release the capture device. */
    public synchronized void stopCapture() {
        capturing.set(false);
        if (captureThread != null) {
            captureThread.interrupt();
            captureThread = null;
        }
        if (captureDevice != null) {
            captureDevice.stop();
            captureDevice.close();
            captureDevice = null;
        }
    }

    public boolean isCapturing() { return capturing.get(); }

    // ── Playback (Application → Radio) ────────────────────────────────────────

    /**
     * Open the named playback device, ready to accept audio via {@link #play(byte[])}.
     *
     * @param deviceName mixer name from {@link #availablePlaybackDevices()}, or
     *                   {@code null} to use the system default
     * @return {@code true} on success
     */
    public synchronized boolean openPlayback(String deviceName) {
        closePlayback();
        try {
            playbackDevice = openSourceLine(deviceName);
            playbackDevice.start();
            return true;
        } catch (LineUnavailableException e) {
            System.err.println("AudioRouter: playback open failed – " + e.getMessage());
            return false;
        }
    }

    /**
     * Write PCM audio bytes to the playback device.
     *
     * <p>Blocks until all bytes are queued in the hardware FIFO.  Call from the
     * audio-generation thread; <strong>never</strong> from the Swing EDT.
     *
     * @param pcm raw PCM bytes in {@link #FORMAT}
     */
    public void play(byte[] pcm) {
        if (playbackDevice == null || !playbackDevice.isOpen()) {
            System.err.println("AudioRouter: playback device not open");
            return;
        }
        playbackDevice.write(pcm, 0, pcm.length);
    }

    /** Drain any buffered audio and release the playback device. */
    public synchronized void closePlayback() {
        if (playbackDevice != null) {
            playbackDevice.drain();
            playbackDevice.stop();
            playbackDevice.close();
            playbackDevice = null;
        }
    }

    public boolean isPlaybackOpen() {
        return playbackDevice != null && playbackDevice.isOpen();
    }

    // ── Listener management ───────────────────────────────────────────────────

    /**
     * Register a listener to receive raw PCM bytes from the capture device.
     *
     * <p>The listener is called from the capture thread; do not block it.
     */
    public void addCaptureListener(Consumer<byte[]> listener) {
        synchronized (captureListeners) {
            captureListeners.add(listener);
        }
    }

    public void removeCaptureListener(Consumer<byte[]> listener) {
        synchronized (captureListeners) {
            captureListeners.remove(listener);
        }
    }

    // ── Open helpers ──────────────────────────────────────────────────────────

    /**
     * Open a {@link TargetDataLine} on the named mixer, or on the system default
     * if {@code deviceName} is null/empty.
     *
     * <p>The buffer size hint ({@code BUFFER_BYTES * 2}) tells the JVM/driver how
     * large the internal ring buffer should be.  Smaller = lower latency,
     * higher underrun risk.
     */
    private TargetDataLine openTargetLine(String deviceName) throws LineUnavailableException {
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, FORMAT, BUFFER_BYTES * 2);
        if (deviceName == null || deviceName.isEmpty()) {
            return (TargetDataLine) AudioSystem.getLine(info);
        }
        for (Mixer.Info mi : AudioSystem.getMixerInfo()) {
            if (!mi.getName().equals(deviceName)) continue;
            Mixer m = AudioSystem.getMixer(mi);
            if (m.isLineSupported(info)) {
                return (TargetDataLine) m.getLine(info);
            }
        }
        throw new LineUnavailableException("Capture device not found: " + deviceName);
    }

    private SourceDataLine openSourceLine(String deviceName) throws LineUnavailableException {
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, FORMAT, BUFFER_BYTES * 2);
        if (deviceName == null || deviceName.isEmpty()) {
            SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
            line.open(FORMAT, BUFFER_BYTES * 2);
            return line;
        }
        for (Mixer.Info mi : AudioSystem.getMixerInfo()) {
            if (!mi.getName().equals(deviceName)) continue;
            Mixer m = AudioSystem.getMixer(mi);
            if (m.isLineSupported(info)) {
                SourceDataLine line = (SourceDataLine) m.getLine(info);
                line.open(FORMAT, BUFFER_BYTES * 2);
                return line;
            }
        }
        throw new LineUnavailableException("Playback device not found: " + deviceName);
    }

    // ── Notification ──────────────────────────────────────────────────────────

    private void notifyCaptureListeners(byte[] chunk) {
        List<Consumer<byte[]>> snapshot;
        synchronized (captureListeners) {
            snapshot = new ArrayList<>(captureListeners);
        }
        for (Consumer<byte[]> l : snapshot) {
            try { l.accept(chunk); } catch (Exception ignored) {}
        }
    }
}
