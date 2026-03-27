package org.qualsh.lb.digitalmodes.audio;

import com.fazecast.jSerialComm.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Connects to a radio rig over a USB cable and streams live audio into the application.
 *
 * <p>Use {@link #getAvailablePorts()} to find available USB serial ports on your
 * computer, then select the correct port in Preferences and click the Rig button to
 * start streaming audio directly from your transceiver into the spectrum display and
 * decoder.
 *
 * <p>Audio arrives continuously while the connection is active. Call {@link #stop()}
 * to disconnect from the rig and release the USB port.
 *
 * @author Logbook Development Team
 * @version 1.0
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
     * Creates a new rig audio source targeting the given USB serial port.
     *
     * <p>The connection is not opened until you call {@link #start()}.
     *
     * @param portName the name of the serial port to connect to, for example
     *                 {@code "/dev/ttyUSB0"} on Linux or {@code "COM3"} on Windows
     * @param baudRate the baud rate matching your rig's serial configuration, for
     *                 example {@code 9600} or {@code 115200}
     */
    public RigAudioSource(String portName, int baudRate) {
        this.portName = portName;
        this.baudRate = baudRate;
        this.buffer = new AudioBuffer();
        this.active = false;
    }

    /**
     * Begins listening to the radio rig over the configured USB serial connection.
     *
     * <p>Audio will appear in the spectrum display within a few seconds of calling this method.
     * If the port cannot be opened — for example because your rig is not connected or the port
     * is already in use — no audio is captured and an error is logged. This method has no effect
     * if the rig is already connected.
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
     * Disconnects from the radio rig and clears the audio buffer.
     *
     * <p>The USB serial port is released and the spectrum display will go blank.
     * This method has no effect if the rig is not currently connected.
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
     * Returns {@code true} if the rig is currently connected and sending audio.
     *
     * @return {@code true} while audio capture is in progress
     */
    @Override
    public boolean isActive() {
        return active;
    }

    /**
     * Returns the audio buffer that receives the live audio from the rig.
     *
     * @return the buffer; never {@code null}
     */
    @Override
    public AudioBuffer getBuffer() {
        return buffer;
    }

    /**
     * Returns the name of the serial port this source is configured to use.
     *
     * @return the port name, for example {@code "/dev/ttyUSB0"} or {@code "COM3"}
     */
    public String getPortName() {
        return portName;
    }

    /**
     * Sets the serial port to connect to the next time {@link #start()} is called.
     *
     * <p>This has no effect while the rig is already connected. Disconnect first
     * before changing the port.
     *
     * @param portName the new port name, for example {@code "/dev/ttyUSB0"} or {@code "COM3"}
     */
    public void setPortName(String portName) {
        if (!active) {
            this.portName = portName;
        }
    }

    /**
     * Returns the baud rate currently configured for the serial connection.
     *
     * @return the baud rate, for example {@code 9600} or {@code 115200}
     */
    public int getBaudRate() {
        return baudRate;
    }

    /**
     * Sets the baud rate to use the next time {@link #start()} is called.
     *
     * <p>This has no effect while the rig is already connected. Disconnect first
     * before changing the baud rate.
     *
     * @param baudRate the new baud rate, for example {@code 9600} or {@code 115200}
     */
    public void setBaudRate(int baudRate) {
        if (!active) {
            this.baudRate = baudRate;
        }
    }

    /**
     * Returns the sample rate used when interpreting incoming audio from the rig.
     *
     * @return samples per second; default is {@code 8000.0f}
     */
    public float getSampleRate() {
        return sampleRate;
    }

    /**
     * Sets the sample rate used when loading rig audio into the buffer.
     *
     * <p>This may be changed at any time, even while capture is active.
     * The new rate takes effect on the next audio update.
     *
     * @param sampleRate samples per second, for example {@code 8000.0f} or {@code 44100.0f}
     */
    public void setSampleRate(float sampleRate) {
        this.sampleRate = sampleRate;
    }

    /**
     * Returns a list of USB serial ports currently detected on this computer.
     *
     * <p>Use this list in Preferences to select which port your radio rig is connected to.
     * The returned array may be empty if no ports are detected.
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
