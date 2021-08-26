package mpmToolbox.projectData;

import com.tagtraum.jipes.audio.LogFrequencySpectrum;
import com.tagtraum.jipes.math.WindowFunction;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

/**
 * This class represents audio data in an MPM Toolbox project. It extends meico's Audio class.
 * @author Axel Berndt
 */
public class Audio extends meico.audio.Audio {
    protected ArrayList<double[]> waveforms = new ArrayList<>();// contains the waveform data for each audio channel as doubles in [-1.0, 1.0]
    private WaveformImage waveformImage = null;                 // the waveform image of this audio data

    private SpectrogramImage spectrogramImage = null;           // the visualization of the above spectrogram

    /**
     * constructor, generates empty instance
     */
    public Audio() {
        super();
    }

    /**
     * constructor with AudioInputStream
     *
     * @param inputStream
     */
    public Audio(AudioInputStream inputStream) {
        super(inputStream);
        this.waveforms = convertByteArray2DoubleArray(this.getAudio(), this.getFormat());
    }

    /**
     * constructor; use this one to load and decode MP3 files
     *
     * @param file
     */
    public Audio(File file) throws IOException, UnsupportedAudioFileException {
        super(file);
        this.waveforms = convertByteArray2DoubleArray(this.getAudio(), this.getFormat());
    }

    /**
     * this constructor reads audio data from the AudioInputStream and associates the file with it;
     * the file may differ from the input stream
     *
     * @param inputStream
     * @param file
     */
    public Audio(AudioInputStream inputStream, File file) {
        super(inputStream, file);
        this.waveforms = convertByteArray2DoubleArray(this.getAudio(), this.getFormat());
    }

    /**
     * with this constructor all data is given explicitly
     *
     * @param audioData
     * @param format
     * @param file
     */
    public Audio(byte[] audioData, AudioFormat format, File file) {
        super(audioData, format, file);
        this.waveforms = convertByteArray2DoubleArray(this.getAudio(), this.getFormat());
    }

    /**
     * a getter for the waveform data
     * @return an ArrayList where each element is the waveform of one channel
     */
    public ArrayList<double[]> getWaveforms() {
        return this.waveforms;
    }

    /**
     * Get the number of samples in the first channel.
     * It is assumed that all other channel have the same number of samples.
     * @return
     */
    public int getNumberOfSamples() {
        return this.waveforms.get(0).length;
    }

    /**
     * produces a BufferedImage of the waveform
     * @param channelNumber
     * @param leftmostSample
     * @param rightmostSample
     * @param width
     * @param height
     * @return true if image has changed
     */
    public boolean computeWaveformImage(int channelNumber, int leftmostSample, int rightmostSample, int width, int height) {
        // if the arguments are equal to those from the last rendering, we do not need to render a new waveform image
        if ((this.waveformImage != null)
                && (this.waveformImage.getWidth() == width)
                && (this.waveformImage.getHeight() == height)
                && this.waveformImage.sameMetrics(channelNumber, leftmostSample, rightmostSample))
            return false;                                                                   // nothing changed

        // update the waveform image
        if (channelNumber >= 0) {                                                           // one specific Waveform/channel should be rendered
            try {
                this.waveformImage = new WaveformImage(convertWaveform2Image(this.waveforms.get(channelNumber), leftmostSample, rightmostSample, width, height), channelNumber, leftmostSample, rightmostSample);
            } catch (ArrayIndexOutOfBoundsException | IllegalArgumentException e) {
                this.waveformImage = null;
                return true;
            }
        } else {                                                                                // all channels should be rendered into one image
            ArrayList<BufferedImage> channels = new ArrayList<>();                              // this will hold a BufferedImage for each single channel
            int heightSubdivision = (int) Math.floor((float) height / this.waveforms.size());   // the pixel height of the sub-images

            // make a horizontal slice of the image for each channel
            for (double[] waveform : this.waveforms) {
                try {
                    BufferedImage img = convertWaveform2Image(waveform, leftmostSample, rightmostSample, width, heightSubdivision);  // draw the waveform to the image
                    channels.add(img);
                } catch (ArrayIndexOutOfBoundsException | IllegalArgumentException e) {
                    this.waveformImage = null;
                    return true;
                }
            }

            // write the waveform images into this.waveformImage
            this.waveformImage = new WaveformImage(width, height, BufferedImage.TYPE_INT_RGB, channelNumber, leftmostSample, rightmostSample);  // we start with an empty image, all black
            for (int channelNum = 0; channelNum < channels.size(); ++channelNum) {
                BufferedImage img = channels.get(channelNum);
                int yOffset = channelNum * heightSubdivision;                                   // the y pixel offset when writing the waveform into this.waveformImage
                for (int y = 0; y < img.getHeight(); ++y) {                                     // for each pixel row
                    for (int x = 0; x < img.getWidth(); ++x) {                                  // go through each pixel
                        this.waveformImage.setRGB(x, y + yOffset, img.getRGB(x, y));            // set the pixel color
                    }
                }
            }
        }

        return true;
    }

    /**
     * getter for the waveform image
     * @return
     */
    public WaveformImage getWaveformImage() {
        return this.waveformImage;
    }

    /**
     * This triggers the computation of the spectrogram, and it's rendering to a SpectrogramImage. It can take some time!
     * @param windowFunction
     * @param hopSize
     * @param minFrequency
     * @param maxFrequency
     * @param binsPerSemitone
     * @return true if image has changed
     */
    public boolean computeSpectrogram(WindowFunction windowFunction, int hopSize, float minFrequency, float maxFrequency, int binsPerSemitone) {
        // if the arguments are equal to those from the last time, we do not need to compute a new spectrogram
        if ((this.spectrogramImage != null)
                && this.spectrogramImage.sameMetrics(windowFunction, hopSize, minFrequency, maxFrequency, binsPerSemitone))
            return false;

        ArrayList<LogFrequencySpectrum> spectrogram;
        try {
            spectrogram = this.exportConstantQTransformSpectrogram(windowFunction, hopSize, minFrequency, maxFrequency, binsPerSemitone);
        } catch (IOException | NegativeArraySizeException | IllegalArgumentException e) {
            e.printStackTrace();
            return false;
        }

        this.spectrogramImage = new SpectrogramImage(spectrogram, windowFunction, hopSize, minFrequency, maxFrequency, binsPerSemitone);
        return true;
    }

    /**
     * getter for the spectrogram image
     * @return
     */
    public SpectrogramImage getSpectrogramImage() {
        return this.spectrogramImage;
    }

    /**
     * This class is basically a BufferedImage with some additional information about the display metrics.
     * @author Axel Berndt
     */
    public static class WaveformImage extends BufferedImage {
        private final int channelNumber;        // index of the waveform/channel to be rendered to image; -1 means all channels
        private final int leftmostSample;       // index of the first sample to be rendered to image
        private final int rightmostSample;      // index of the last sample to be rendered to image

        private WaveformImage(int width, int height, int imageType, int channelNumber, int leftmostSample, int rightmostSample) {
            super(width, height, imageType);

            this.channelNumber = channelNumber;
            this.leftmostSample = leftmostSample;
            this.rightmostSample = rightmostSample;
        }

        private WaveformImage(BufferedImage bi, int channelNumber, int leftmostSample, int rightmostSample) {
            super(bi.getColorModel(), bi.getRaster(), bi.getColorModel().isAlphaPremultiplied(), null);

            this.channelNumber = channelNumber;
            this.leftmostSample = leftmostSample;
            this.rightmostSample = rightmostSample;
        }

        private boolean sameMetrics(int channelNumber, int leftmostSample, int rightmostSample) {
            return (this.channelNumber == channelNumber) && (this.leftmostSample == leftmostSample) && (this.rightmostSample == rightmostSample);
        }
    }

    /**
     * This class is basically a BufferedImage with some additional information about the spectrogram metrics.
     * @author Axel Berndt
     */
    public static class SpectrogramImage extends BufferedImage {
        private ArrayList<LogFrequencySpectrum> spectrogram = null; // the spectrogram of this audio data
        private final WindowFunction windowFunction;
        private final int hopSize;
        private final float minFrequency;
        private final float maxFrequency;
        private final int binsPerSemitone;
        private final int[] sampleLookup;

        private SpectrogramImage(BufferedImage bi, WindowFunction windowFunction, int hopSize, float minFrequency, float maxFrequency, int binsPerSemitone) {
            super(bi.getColorModel(), bi.getRaster(), bi.getColorModel().isAlphaPremultiplied(), null);

            this.windowFunction = windowFunction;
            this.hopSize = hopSize;
            this.minFrequency = minFrequency;
            this.maxFrequency = maxFrequency;
            this.binsPerSemitone = binsPerSemitone;

            this.sampleLookup = new int[this.getWidth()];
            for (int i = 0; i < this.sampleLookup.length; ++i)
                this.sampleLookup[i] = i * this.hopSize;
        }

        private SpectrogramImage(ArrayList<LogFrequencySpectrum> spectrogram, WindowFunction windowFunction, int hopSize, float minFrequency, float maxFrequency, int binsPerSemitone) {
            this(convertSpectrogramToImage(spectrogram), windowFunction, hopSize, minFrequency, maxFrequency, binsPerSemitone);
            this.spectrogram = spectrogram;
        }

        private boolean sameMetrics(WindowFunction windowFunction, int hopSize, float minFrequency, float maxFrequency, int binsPerSemitone) {
            return this.windowFunction.equals(windowFunction) && (this.hopSize == hopSize) && (this.minFrequency == minFrequency) && (this.maxFrequency == maxFrequency) && (this.binsPerSemitone == binsPerSemitone);
        }

        public ArrayList<LogFrequencySpectrum> getSpectrogram() {
            return this.spectrogram;
        }
    }
}
