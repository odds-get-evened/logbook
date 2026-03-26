package org.qualsh.lb.digitalmodes.audio;

import com.fazecast.jSerialComm.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * An {@link AudioSource} that captures audio from a radio rig connected via
 * a USB serial port.
 *
 * <p>Use {@link #getAvailablePorts()} to enumerate ports on the host system,
 * then construct an instance with the desired port name and baud rate. Call
 * {@link #start()} to open the port and begin streaming audio data into the
 * associated {@link AudioBuffer}; call {@link #stop()} to end capture and
 * release the port.
 *
 * <p>Audio data arriving over the serial link is treated as raw 16-bit mono
 * PCM bytes at the configured {@link #getSampleRate() sample rate} (default
 * {@code 8000.0f} Hz). The buffer is updated continuously as data arrives,
 * so any registered {@link AudioBuffer.AudioBufferListener} will receive
 * frequent notifications while capture is active.
 */
public class RigAudioSource implements AudioSource {

    private static final int READ_BUFFER_SIZE = 4096;

    private String portName;
    private int baudRate;
    private float sampleRate = 8000.0f;

    private final AudioBuffer buffer;
    private SerialPort serialPort;
    private Thread captureThread;
    private boolean active;

    /**
     * Creates a new {@code RigAudioSource} targeting the given serial port.
     *
     * <p>The source is not active after construction; call {@link #start()}
     * to begin capture.
     *
     * @param portName the system name of the serial port, for example
     *                 {@code "/dev/ttyUSB0"} on Linux or {@code "COM3"} on
     *                 Windows
     * @param baudRate the baud rate to use when opening the port, for example
     *                 {@code 9600} or {@code 115200}
     */
    public RigAudioSource(String portName, int baudRate) {
        this.portName = portName;
        this.baudRate = baudRate;
        this.buffer = new AudioBuffer();
        this.active = false;
    }

    /**
     * Opens the serial port and begins streaming audio data into the
     * {@link AudioBuffer}.
     *
     * <p>A background thread is started that continuously reads bytes from
     * the serial port in chunks of up to {@value #READ_BUFFER_SIZE} bytes.
     * Each successful read appends to an accumulating byte stream, and the
     * buffer is updated with the full accumulated data on every chunk
     * received.
     *
     * <p>If the port cannot be opened (for example because it does not exist
     * or is already in use) a warning is printed to {@code System.err} and
     * this method returns without starting capture.
     *
     * <p>This method has no effect if the source is already active.
     */
    @Override
    public void start() {
        if (active) {
            return;
        }

        serialPort = SerialPort.getCommPort(portName);
        serialPort.setBaudRate(baudRate);

        if (!serialPort.openPort()) {
            System.err.println("RigAudioSource: failed to open serial port: " + portName);
            return;
        }

        active = true;

        captureThread = new Thread(() -> {
            ByteArrayOutputStream accumulator = new ByteArrayOutputStream();
            byte[] chunk = new byte[READ_BUFFER_SIZE];
            InputStream in = serialPort.getInputStream();

            try {
                while (!Thread.currentThread().isInterrupted()) {
                    int bytesRead = in.read(chunk);
                    if (bytesRead > 0) {
                        accumulator.write(chunk, 0, bytesRead);
                        buffer.load(accumulator.toByteArray(), sampleRate);
                    }
                }
            } catch (IOException e) {
                if (!Thread.currentThread().isInterrupted()) {
                    System.err.println("RigAudioSource: read error on " + portName + ": " + e.getMessage());
                }
            }
        }, "rig-audio-capture");

        captureThread.setDaemon(true);
        captureThread.start();
    }

    /**
     * Stops audio capture, interrupts the background read thread, closes the
     * serial port, and clears the {@link AudioBuffer}.
     *
     * <p>After this call, {@link #isActive()} returns {@code false}. This
     * method has no effect if the source is not currently active.
     */
    @Override
    public void stop() {
        if (!active) {
            return;
        }

        active = false;

        if (captureThread != null) {
            captureThread.interrupt();
            captureThread = null;
        }

        if (serialPort != null && serialPort.isOpen()) {
            serialPort.closePort();
        }
        serialPort = null;

        buffer.clear();
    }

    /**
     * Returns {@code true} if the source is currently capturing audio from
     * the serial port.
     *
     * @return {@code true} while capture is in progress
     */
    @Override
    public boolean isActive() {
        return active;
    }

    /**
     * Returns the {@link AudioBuffer} that receives audio data from this
     * source.
     *
     * @return the buffer; never {@code null}
     */
    @Override
    public AudioBuffer getBuffer() {
        return buffer;
    }

    /**
     * Returns the serial port name used by this source.
     *
     * @return the port name, for example {@code "/dev/ttyUSB0"} or
     *         {@code "COM3"}
     */
    public String getPortName() {
        return portName;
    }

    /**
     * Sets the serial port name to use when {@link #start()} is next called.
     *
     * <p>This method has no effect if the source is currently active. Stop
     * the source before changing the port name.
     *
     * @param portName the new port name, for example {@code "/dev/ttyUSB0"}
     *                 or {@code "COM3"}
     */
    public void setPortName(String portName) {
        if (!active) {
            this.portName = portName;
        }
    }

    /**
     * Returns the baud rate configured for the serial port.
     *
     * @return the baud rate, for example {@code 9600} or {@code 115200}
     */
    public int getBaudRate() {
        return baudRate;
    }

    /**
     * Sets the baud rate to use when {@link #start()} is next called.
     *
     * <p>This method has no effect if the source is currently active. Stop
     * the source before changing the baud rate.
     *
     * @param baudRate the new baud rate, for example {@code 9600} or
     *                 {@code 115200}
     */
    public void setBaudRate(int baudRate) {
        if (!active) {
            this.baudRate = baudRate;
        }
    }

    /**
     * Returns the sample rate at which incoming bytes are interpreted as PCM
     * audio data.
     *
     * @return samples per second; default is {@code 8000.0f}
     */
    public float getSampleRate() {
        return sampleRate;
    }

    /**
     * Sets the sample rate used when loading data into the {@link AudioBuffer}.
     *
     * <p>This may be changed at any time, including while capture is active.
     * The new rate takes effect on the next buffer update.
     *
     * @param sampleRate samples per second, for example {@code 8000.0f} or
     *                   {@code 44100.0f}
     */
    public void setSampleRate(float sampleRate) {
        this.sampleRate = sampleRate;
    }

    /**
     * Returns the names of all serial ports currently available on this
     * system.
     *
     * <p>This method is intended for use by configuration dialogs (such as
     * the Preferences panel) to populate a port-selection drop-down. The
     * returned array may be empty if no ports are detected.
     *
     * @return an array of port name strings; never {@code null}
     */
    public static String[] getAvailablePorts() {
        SerialPort[] ports = SerialPort.getCommPorts();
        String[] names = new String[ports.length];
        for (int i = 0; i < ports.length; i++) {
            names[i] = ports[i].getSystemPortName();
        }
        return names;
    }
}
