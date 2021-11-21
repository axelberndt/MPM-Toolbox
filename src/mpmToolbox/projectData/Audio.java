package mpmToolbox.projectData;

import com.tagtraum.jipes.audio.LogFrequencySpectrum;
import com.tagtraum.jipes.math.WindowFunction;
import meico.msm.Msm;
import meico.supplementary.ColorCoding;
import mpmToolbox.projectData.alignment.Alignment;
import nu.xom.Attribute;
import nu.xom.Element;

import javax.sound.sampled.UnsupportedAudioFileException;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;

/**
 * This class represents audio data in an MPM Toolbox project. It extends meico's Audio class.
 * @author Axel Berndt
 */
public class Audio extends meico.audio.Audio {
    protected final ArrayList<double[]> waveforms;              // contains the waveform data for each audio channel as doubles in [-1.0, 1.0]
    private WaveformImage waveformImage = null;                 // the waveform image of this audio data
    private SpectrogramImage spectrogramImage = null;           // the visualization of the above spectrogram
    private final Alignment alignment;

    /**
     * constructor; use this one to load and decode MP3 files
     *
     * @param file
     * @param msm the Msm instance to be aligned with this Audio object
     */
    public Audio(File file, Msm msm) throws IOException, UnsupportedAudioFileException {
        super(file);

        this.waveforms = convertByteArray2DoubleArray(this.getAudio(), this.getFormat());
        this.alignment = new Alignment(msm, null);
        this.alignment.scaleTiming(((double) this.getNumberOfSamples() / this.getFrameRate()) * 1000.0);    // scale the initial alignment to the milliseconds length of the audio; so all notes are visible and in a good starting position
    }

    /**
     * constructor; use this one when loading a new MPM Toolbox project
     * @param projectAudioData
     * @param projectBasePath
     * @param msm
     * @throws IOException
     * @throws UnsupportedAudioFileException
     */
    public Audio(Element projectAudioData, String projectBasePath, Msm msm) throws IOException, UnsupportedAudioFileException {
        super(new File(projectBasePath + projectAudioData.getAttributeValue("file").replaceAll("\\\\/", File.separator)));

        this.waveforms = convertByteArray2DoubleArray(this.getAudio(), this.getFormat());

        Element alignmentData = projectAudioData.getFirstChildElement("alignment");
        this.alignment = new Alignment(msm, alignmentData);
        if (alignmentData == null)      // if we had no alignment data from the project file, an initial alignment was generated with a default tempo that will potentially not fit the audio length
            this.alignment.scaleTiming(((double) this.getNumberOfSamples() / this.getFrameRate()) * 1000.0);    // scale the initial alignment to the milliseconds length of the audio; so all notes are visible and in a good starting position
    }

    /**
     * generate a project data XML element
     * @param projectPath generate it this way: Paths.get(projectFile.getParent())
     * @return
     */
    public Element toXml(Path projectPath) {
        Element out = new Element("audio");
        out.addAttribute(new Attribute("file", projectPath.relativize(this.getFile().toPath()).toString()));
        out.appendChild(this.alignment.toXml());
        return out;
    }

    /**
     * access this audio's alignment data
     * @return
     */
    public Alignment getAlignment() {
        return this.alignment;
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
     * @param normalize
     * @return true if image has changed
     */
    public boolean computeSpectrogram(WindowFunction windowFunction, int hopSize, float minFrequency, float maxFrequency, int binsPerSemitone, boolean normalize) {
        // if the arguments are equal to those from the last time, we do not need to compute a new spectrogram
        if ((this.spectrogramImage != null)
                && this.spectrogramImage.sameMetrics(windowFunction, hopSize, minFrequency, maxFrequency, binsPerSemitone)) {

            // if the normalization flag changed we can reuse the spectrogram and need to rerender the image
            if (this.spectrogramImage.normalize != normalize)
                this.spectrogramImage = new SpectrogramImage(this.spectrogramImage.spectrogram, windowFunction, hopSize, minFrequency, maxFrequency, binsPerSemitone, normalize);

            return false;
        }

        ArrayList<LogFrequencySpectrum> spectrogram;
        try {
            spectrogram = this.exportConstantQTransformSpectrogram(windowFunction, hopSize, minFrequency, maxFrequency, binsPerSemitone);
        } catch (IOException | NegativeArraySizeException | IllegalArgumentException e) {
            e.printStackTrace();
            return false;
        }

        this.spectrogramImage = new SpectrogramImage(spectrogram, windowFunction, hopSize, minFrequency, maxFrequency, binsPerSemitone, normalize);
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
        private final boolean normalize;
        private final int[] sampleLookup;

        private SpectrogramImage(BufferedImage bi, WindowFunction windowFunction, int hopSize, float minFrequency, float maxFrequency, int binsPerSemitone, boolean normalize) {
            super(bi.getColorModel(), bi.getRaster(), bi.getColorModel().isAlphaPremultiplied(), null);

            this.windowFunction = windowFunction;
            this.hopSize = hopSize;
            this.minFrequency = minFrequency;
            this.maxFrequency = maxFrequency;
            this.binsPerSemitone = binsPerSemitone;
            this.normalize = normalize;

            this.sampleLookup = new int[this.getWidth()];
            for (int i = 0; i < this.sampleLookup.length; ++i)
                this.sampleLookup[i] = i * this.hopSize;
        }

        private SpectrogramImage(ArrayList<LogFrequencySpectrum> spectrogram, WindowFunction windowFunction, int hopSize, float minFrequency, float maxFrequency, int binsPerSemitone, boolean normalize) {
            this(convertSpectrogramToImage(spectrogram, normalize, 0.1f, new ColorCoding(ColorCoding.INFERNO)), windowFunction, hopSize, minFrequency, maxFrequency, binsPerSemitone, normalize);
            this.spectrogram = spectrogram;
        }

        private boolean sameMetrics(WindowFunction windowFunction, int hopSize, float minFrequency, float maxFrequency, int binsPerSemitone) {
            return this.windowFunction.equals(windowFunction)
                    && (this.hopSize == hopSize)
                    && (this.minFrequency == minFrequency)
                    && (this.maxFrequency == maxFrequency)
                    && (this.binsPerSemitone == binsPerSemitone);
        }

        public ArrayList<LogFrequencySpectrum> getSpectrogram() {
            return this.spectrogram;
        }
    }
}
