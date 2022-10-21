package mpmToolbox.projectData.audio;

import com.alee.extended.window.WebProgressDialog;
import com.tagtraum.jipes.AbstractSignalProcessor;
import com.tagtraum.jipes.SignalPipeline;
import com.tagtraum.jipes.SignalPump;
import com.tagtraum.jipes.audio.*;
import com.tagtraum.jipes.math.WindowFunction;
import com.tagtraum.jipes.universal.Mapping;
import meico.msm.Msm;
import meico.supplementary.KeyValue;
import mpmToolbox.projectData.alignment.Alignment;
import mpmToolbox.projectData.alignment.Note;
import mpmToolbox.supplementary.Tools;
import nu.xom.Attribute;
import nu.xom.Element;

import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Map;

/**
 * This class represents audio data in an MPM Toolbox project. It extends meico's Audio class and adds some more
 * elaborate waveform and spectrogram image provider functionality. It also adds alignment data.
 * @author Axel Berndt
 */
public class Audio extends meico.audio.Audio {
    protected final ArrayList<double[]> waveforms;                      // contains the waveform data for each audio channel as doubles in [-1.0, 1.0]
    protected final ArrayList<PeakList> peakList = new ArrayList<>();   // for each channel, the list of minima and maxima in the waveform is used to accelerate waveform image rendering
    private WaveformImage waveformImage = null;                         // the waveform image of this audio data
    private SpectrogramImage spectrogramImage = null;                   // the visualization of the above spectrogram
    private Alignment alignment;                                        // audio to MSM alignment

    /**
     * constructor; use this one to load and decode MP3 files
     *
     * @param file
     * @param msm the Msm instance to be aligned with this Audio object
     */
    public Audio(File file, Msm msm) throws IOException, UnsupportedAudioFileException {
        super(file);

        this.waveforms = convertByteArray2DoubleArray(this.getAudio(), this.getFormat());
        for (double[] chan : this.waveforms)
            this.peakList.add(new PeakList(chan));

        this.initAlignment(msm);
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
        super(new File(Tools.uniformPath(projectBasePath + projectAudioData.getAttributeValue("file"))));

        this.waveforms = convertByteArray2DoubleArray(this.getAudio(), this.getFormat());
        for (double[] chan : this.waveforms)
            this.peakList.add(new PeakList(chan));

        Element alignmentData = projectAudioData.getFirstChildElement("alignment");
        this.alignment = new Alignment(msm, alignmentData);
        if (alignmentData == null) {     // if we had no alignment data from the project file, an initial alignment was generated with a default tempo that will potentially not fit the audio length
            this.initAlignment(msm);
        }
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
     * import an alignment and replace the current alignment
     * @param alignment
     */
    public void setAlignment(Alignment alignment) {
        this.alignment = alignment;
    }

    /**
     * initialize or reset the alignment data for this Audio object
     * @param msm the Msm instance to be aligned with this Audio object
     */
    public void initAlignment(Msm msm) {
        if (this.alignment == null)
            this.alignment = new Alignment(msm, null);

        Note firstNote = this.alignment.getFirstNote();
        if (firstNote != null) {
            firstNote.setFixed(true);
        }

        this.alignment.scaleOverallTiming(((double) this.getNumberOfSamples() / this.getFrameRate()) * 1000.0);    // scale the initial alignment to the milliseconds length of the audio; so all notes are visible and in a good starting position
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
        // the below lines are obsolete because of the later exception handling
//        if ((height <= 0) || (width <= 0)) {
//            this.waveformImage = null;
//            return true;
//        }

        // if the arguments are equal to those from the last rendering, we do not need to render a new waveform image
        if ((this.waveformImage != null)
                && (this.waveformImage.getWidth() == width)
                && (this.waveformImage.getHeight() == height)
                && this.waveformImage.sameMetrics(channelNumber, leftmostSample, rightmostSample))
            return false;                                                                   // nothing changed

        // update the waveform image
        if (channelNumber >= 0) {                                                           // one specific Waveform/channel should be rendered
            try {
//                this.waveformImage = new WaveformImage(convertWaveform2Image(this.waveforms.get(channelNumber), leftmostSample, rightmostSample, width, height), channelNumber, leftmostSample, rightmostSample);
                this.waveformImage = new WaveformImage(this.convertWaveform2Image(channelNumber, leftmostSample, rightmostSample, width, height), channelNumber, leftmostSample, rightmostSample);
            } catch (ArrayIndexOutOfBoundsException | IllegalArgumentException e) {
                e.printStackTrace();
                this.waveformImage = null;
                return true;
            }
        } else {                                                                                // all channels should be rendered into one image
            ArrayList<BufferedImage> channels = new ArrayList<>();                              // this will hold a BufferedImage for each single channel
            int heightSubdivision = (int) Math.floor((float) height / this.waveforms.size());   // the pixel height of the sub-images

            // make a horizontal slice of the image for each channel
            for (int chan = 0; chan < this.waveforms.size(); ++chan) {
                try {
//                    BufferedImage img = convertWaveform2Image(this.waveforms.get(chan), leftmostSample, rightmostSample, width, heightSubdivision);  // draw the waveform to the image
                    BufferedImage img = this.convertWaveform2Image(chan, leftmostSample, rightmostSample, width, heightSubdivision);  // draw the waveform to the image
                    channels.add(img);
                } catch (ArrayIndexOutOfBoundsException | IllegalArgumentException e) {
//                    e.printStackTrace();
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
     * Make a waveform image from the input audio data. This is an optimized version of the same method in meico.audio.Audio.
     * @param channelNumber the audio channel to be rendered
     * @param leftmostSample where in the audio data should we start
     * @param rightmostSample where in the audio will we end
     * @param width the width of the image in pixels
     * @param height the height of the image in pixels
     * @return the waveform image or null
     * @throws ArrayIndexOutOfBoundsException
     */
    public BufferedImage convertWaveform2Image(int channelNumber, int leftmostSample, int rightmostSample, int width, int height) throws ArrayIndexOutOfBoundsException {
        double[] sampleArray = this.waveforms.get(channelNumber);           // the waveform of this channel
        double numSamples = rightmostSample + 1 - leftmostSample;           // how many samples are to be displayed in the panel frame
        double samplesPerPixelColumn = numSamples / width;                  // how many samples are rendered into one pixel
        double[][] maxValues = new double[width][2];                        // this array collects the max and min values at each pixel column

        PeakList peaks = this.peakList.get(channelNumber);                              // the peak list of this channel
        KeyValue<Integer, Integer> firstPeak = peaks.ceilingEntry(leftmostSample + 1);  // quick search for the first entry in the peaks list after leftmostSample and before rightmostSample
        int peakIndex = (firstPeak == null) ? peaks.size() : firstPeak.getKey();        // the index of the first peak where we will later start checking for min and max values

        BufferedImage waveform = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);  // we start with an empty image, all black
        double yTranslationFactor = -0.5 * waveform.getHeight();                                // this value is needed several times to scale and translate the sample values (in [-1.0, 1.0]) to vertical pixel coordinates (in [waveform.getHeight(), 0])

        double[] sampleSlice = new double[] {leftmostSample, leftmostSample + samplesPerPixelColumn};   // the sample indices for the slice to be rendered into the next pixel column
        for (int x = 0; x < width; ++x) {                                   // for each pixel column
            // get integer sample indices of the slice to be rendered
            int s2 = Math.min((int) Math.floor(sampleSlice[1]), rightmostSample);
            int s1 = Math.min((int) Math.ceil(sampleSlice[0]), s2);

            if (s1 == s2) {                                                 // if we have to look into one precise sample
                double value = sampleArray[s1];                             // we read its value
                if (value > 0.0)
                    maxValues[x][0] = value;
                else
                    maxValues[x][1] = value;
            } else {                                                        // if we have to consider more than one sample, collect all peaks int [s1, s2] and check against the values at s1 and s2
                // check first sample in the slice
                double v = sampleArray[s1];
                if (maxValues[x][0] < v)
                    maxValues[x][0] = v;
                else if (maxValues[x][1] > v)
                    maxValues[x][1] = v;

                // check the peaks
                for (; (peakIndex < peaks.size()) && (peaks.get(peakIndex) < s2); ++peakIndex) {
                    if (peaks.get(peakIndex) <= s1)
                        continue;
                    v = sampleArray[peaks.get(peakIndex)];
                    if (maxValues[x][0] < v)
                        maxValues[x][0] = v;
                    else if (maxValues[x][1] > v)
                        maxValues[x][1] = v;
                }

                // check last sample in the slice
                v = sampleArray[s2];
                if (maxValues[x][0] < v)
                    maxValues[x][0] = v;
                else if (maxValues[x][1] > v)
                    maxValues[x][1] = v;
            }

            // draw the amplitude line of the waveform in the BufferedImage instance
            int yPositive = (int) Math.round((maxValues[x][0] * yTranslationFactor) - yTranslationFactor);
            int yNegative = (int) Math.round((maxValues[x][1] * yTranslationFactor) - yTranslationFactor);
            waveform.setRGB(x, yPositive, Color.DARK_GRAY.getRGB());        // draw a dark gray center line
            for (int y = yPositive; y < yNegative; ++y)
                waveform.setRGB(x, y, Color.WHITE.getRGB());

            // update limits for the next sample slice
            sampleSlice[0] = sampleSlice[1];
            sampleSlice[1] += samplesPerPixelColumn;
        }

        return waveform;
    }

    /**
     * getter for the waveform image
     * @return
     */
    public WaveformImage getWaveformImage() {
        return this.waveformImage;
    }

    /**
     * This computes a Contant Q Transform spectrogram and returns it as array of CQT slices.
     *
     * @param windowFunction
     * @param hopSize
     * @param minFrequency
     * @param maxFrequency
     * @param binsPerSemitone
     * @param pump In other dsp frameworks the pump might be called dispatcher, it delivers the audio frames. The application can provide its own pump. Via the pump the application can cancel the processing which is useful in multithreaded environments.
     * @param progressBar the progress bar to be updated and display the progress
     * @return
     */
    public ArrayList<LogFrequencySpectrum> exportConstantQTransformSpectrogram(WindowFunction windowFunction, int hopSize, float minFrequency, float maxFrequency, int binsPerSemitone, SignalPump<AudioBuffer> pump, WebProgressDialog progressBar) throws IOException {
        long startTime = System.currentTimeMillis();                    // we measure the time that the conversion consumes
        System.out.println("\nComputing CQT spectrogram (window: " + windowFunction + ", hop size: " + hopSize + ", min freq: " + minFrequency + ", max freq: " + maxFrequency + ", bins per semitone: " + binsPerSemitone + ").");

        int numSamples = this.getAudio().length / (2 * this.getChannels());
        progressBar.setMaximum(numSamples);
        SwingUtilities.invokeLater(() -> progressBar.setText("Initializing Signal Processing Pipeline ..."));

        SignalPipeline<AudioBuffer, LogFrequencySpectrum> cqtPipeline = new SignalPipeline<>(
                new Mono(),                                             // if there are more than one channel, reduce them to mono
                new SlidingWindow(windowFunction.getLength(), hopSize),
                new Mapping<AudioBuffer>(AudioBufferFunctions.createMapFunction(windowFunction)),
                new ConstantQTransform(minFrequency, maxFrequency, 12 * binsPerSemitone),
                new AbstractSignalProcessor<LogFrequencySpectrum, ArrayList<LogFrequencySpectrum>>("specID") {  // aggregate the CQTs to a spectrum with id "specID" (needed to access it in the results)
                    private final ArrayList<LogFrequencySpectrum> spectrogram = new ArrayList<>();

                    @Override
                    protected ArrayList<LogFrequencySpectrum> processNext(LogFrequencySpectrum input) throws IOException {
                        this.spectrogram.add(input);
                        SwingUtilities.invokeLater(() -> {
                            int state = this.spectrogram.size() * hopSize;
                            progressBar.setProgress(state);
                            progressBar.setText((numSamples - state) + " samples left");
                        });
                        return this.spectrogram;
                    }
                }
        );

        AudioSignalSource source = new AudioSignalSource(meico.audio.Audio.convertByteArray2AudioInputStream(this.getAudio(), this.getFormat()));
        pump.setSignalSource(source);                                   // in other dsp frameworks the pump might be called dispatcher, it delivers the audio frames
        pump.add(cqtPipeline);
        Map<Object, Object> results = pump.pump();

        System.out.println("Computing CQT spectrogram finished. Time consumed: " + (System.currentTimeMillis() - startTime) + " milliseconds");

        return (ArrayList<LogFrequencySpectrum>) results.get("specID");
    }


    /**
     * This triggers the computation of the spectrogram, and it's rendering to a SpectrogramImage. It can take some time!
     * @param windowFunction
     * @param hopSize
     * @param minFrequency
     * @param maxFrequency
     * @param binsPerSemitone
     * @param normalize
     * @param pump the audio frame dispatcher
     * @param progressBar
     * @return true if image has changed
     */
    public boolean computeSpectrogram(WindowFunction windowFunction, int hopSize, float minFrequency, float maxFrequency, int binsPerSemitone, boolean normalize, SignalPump<AudioBuffer> pump, WebProgressDialog progressBar) {
        // if the arguments are equal to those from the last time, we do not need to compute a new spectrogram
        if ((this.spectrogramImage != null)
                && this.spectrogramImage.sameMetrics(windowFunction, hopSize, minFrequency, maxFrequency, binsPerSemitone)) {

            // if the normalization flag changed we can reuse the spectrogram and need to rerender the image
            if (this.spectrogramImage.normalize != normalize)
                this.spectrogramImage = new SpectrogramImage(this.spectrogramImage.spectrogram, windowFunction, hopSize, minFrequency, maxFrequency, binsPerSemitone, normalize);

            return false;
        }

        this.spectrogramImage = null;
        ArrayList<LogFrequencySpectrum> spectrogram;
        try {
            spectrogram = this.exportConstantQTransformSpectrogram(windowFunction, hopSize, minFrequency, maxFrequency, binsPerSemitone, pump, progressBar);
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
}
