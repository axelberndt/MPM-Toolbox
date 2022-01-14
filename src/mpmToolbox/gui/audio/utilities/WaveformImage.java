package mpmToolbox.gui.audio.utilities;

import java.awt.image.BufferedImage;

/**
 * This class is basically a BufferedImage with some additional information about the display metrics.
 * @author Axel Berndt
 */
public class WaveformImage extends BufferedImage {
    private final int channelNumber;        // index of the waveform/channel to be rendered to image; -1 means all channels
    private final int leftmostSample;       // index of the first sample to be rendered to image
    private final int rightmostSample;      // index of the last sample to be rendered to image

    public WaveformImage(int width, int height, int imageType, int channelNumber, int leftmostSample, int rightmostSample) {
        super(width, height, imageType);

        this.channelNumber = channelNumber;
        this.leftmostSample = leftmostSample;
        this.rightmostSample = rightmostSample;
    }

    public WaveformImage(BufferedImage bi, int channelNumber, int leftmostSample, int rightmostSample) {
        super(bi.getColorModel(), bi.getRaster(), bi.getColorModel().isAlphaPremultiplied(), null);

        this.channelNumber = channelNumber;
        this.leftmostSample = leftmostSample;
        this.rightmostSample = rightmostSample;
    }

    public boolean sameMetrics(int channelNumber, int leftmostSample, int rightmostSample) {
        return (this.channelNumber == channelNumber) && (this.leftmostSample == leftmostSample) && (this.rightmostSample == rightmostSample);
    }
}
