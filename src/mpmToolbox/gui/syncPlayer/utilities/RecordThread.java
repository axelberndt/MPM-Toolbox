package mpmToolbox.gui.syncPlayer.utilities;

import com.alee.api.annotations.NotNull;
import com.alee.laf.progressbar.WebProgressBar;
import mpmToolbox.projectData.audio.Audio;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;
import javax.swing.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * This thread performs the recording of audio input.
 * @author Axel Berndt
 */
public class RecordThread extends Thread {
    private final TargetDataLine line;
    private volatile boolean stopMe = false;
    private final ByteArrayOutputStream recording = new ByteArrayOutputStream();
    private final WebProgressBar vuMeter;

    /**
     * constructor
     * @param line
     */
    public RecordThread(@NotNull TargetDataLine line, @NotNull WebProgressBar vuMeter) {
        this.line = line;
        this.vuMeter = vuMeter;
    }

    /**
     * start the thread
     */
    @Override
    public synchronized void start() {
        try {
            this.line.open(this.line.getFormat());
        } catch (LineUnavailableException e) {
            e.printStackTrace();
            return;
        }
        this.line.start();

        super.start();
    }

    /**
     * execute the recording
     */
    @Override
    public void run() {
//        byte[] buffer = new byte[this.line.getBufferSize()];
        byte[] buffer = new byte[8192];     // smaller than the line's buffer size, thereby we get a faster update rate for the VU meter

        while (!this.stopMe) {  // perform recording process
            while (this.line.available() >= buffer.length) {
                // do the recording
                int bytesRead = this.line.read(buffer, 0, buffer.length);
                this.recording.write(buffer, 0, bytesRead);

                // do the monitoring, i.e. communicate maximum amplitude to the GUI
                SwingUtilities.invokeLater(() -> {
                    double amplitude = this.calculateMaxAmplitude(buffer, bytesRead);
                    this.vuMeter.setValue((int) (Math.pow(amplitude, 0.5) * 100));
                    if (amplitude >= 1.0)
                        this.vuMeter.setString("CLIPPING");
                });
            }
        }

        // if recording should be ended
        this.line.stop();
        this.line.close();
        try {
            this.recording.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * compute the maximum amplitude of all line input signals channels
     * @param buffer
     * @param bytesRead
     * @return
     */
    private double calculateMaxAmplitude(byte[] buffer, int bytesRead) {
        int sampleSizeInBytes = this.line.getFormat().getSampleSizeInBits() / 8;
        int channels = this.line.getFormat().getChannels();
        double maxAmplitude = 0;

        for (int channel = 0; channel < channels; channel++) {
            int channelOffset = channel * sampleSizeInBytes;

            for (int i = channelOffset; i < bytesRead; i += (channels * sampleSizeInBytes)) {
                int sample = 0;
                if (sampleSizeInBytes == 2) {
                    sample = (buffer[i + 1] << 8) | (buffer[i] & 0xFF);
                } else if (sampleSizeInBytes == 1) {
                    sample = buffer[i] & 0xFF;
                }

                double amplitude = Math.abs(sample / (Math.pow(2, this.line.getFormat().getSampleSizeInBits() - 1)));
                maxAmplitude = Math.max(maxAmplitude, amplitude);
            }
        }

        return maxAmplitude;
    }

    /**
     * stop the recording
     */
    public synchronized void terminate() {
        this.stopMe = true;                 // signal the thread to terminate
        try {
            this.join();                    // wait until the thread terminates, so subsequent operations like getRecording() are save
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * access the recording
     * @return
     */
    public synchronized AudioInputStream getRecording() {
        return Audio.convertByteArray2AudioInputStream(this.recording.toByteArray(), this.line.getFormat());
    }
}
