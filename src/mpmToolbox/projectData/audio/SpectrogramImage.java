package mpmToolbox.projectData.audio;

import com.tagtraum.jipes.audio.LogFrequencySpectrum;
import com.tagtraum.jipes.math.WindowFunction;
import meico.supplementary.ColorCoding;
import mpmToolbox.projectData.audio.Audio;
import mpmToolbox.supplementary.Tools;
import nu.xom.Attribute;
import nu.xom.Element;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;

/**
 * This class is basically a BufferedImage with some additional information about the spectrogram metrics.
 * @author Axel Berndt
 */
public class SpectrogramImage extends BufferedImage {
    public ArrayList<LogFrequencySpectrum> spectrogram = null; // the spectrogram of this audio data
    private final WindowFunction windowFunction;
    private final int hopSize;
    private final float minFrequency;
    private final float maxFrequency;
    private final int binsPerSemitone;
    public final boolean normalize;
    private File file = null;
//    private final int[] sampleLookup;

    public SpectrogramImage(BufferedImage bi, WindowFunction windowFunction, int hopSize, float minFrequency, float maxFrequency, int binsPerSemitone, boolean normalize) {
        super(bi.getColorModel(), bi.getRaster(), bi.getColorModel().isAlphaPremultiplied(), null);

        this.windowFunction = windowFunction;
        this.hopSize = hopSize;
        this.minFrequency = minFrequency;
        this.maxFrequency = maxFrequency;
        this.binsPerSemitone = binsPerSemitone;
        this.normalize = normalize;

//        this.sampleLookup = new int[this.getWidth()];
//        for (int i = 0; i < this.sampleLookup.length; ++i)
//            this.sampleLookup[i] = i * this.hopSize;
    }

    public SpectrogramImage(ArrayList<LogFrequencySpectrum> spectrogram, WindowFunction windowFunction, int hopSize, float minFrequency, float maxFrequency, int binsPerSemitone, boolean normalize) {
        this(Audio.convertSpectrogramToImage(spectrogram, normalize, 0.1f, new ColorCoding(ColorCoding.INFERNO)), windowFunction, hopSize, minFrequency, maxFrequency, binsPerSemitone, normalize);
        this.spectrogram = spectrogram;
    }

    public boolean sameMetrics(WindowFunction windowFunction, int hopSize, float minFrequency, float maxFrequency, int binsPerSemitone) {
        return this.windowFunction.equals(windowFunction)
                && (this.hopSize == hopSize)
                && (this.minFrequency == minFrequency)
                && (this.maxFrequency == maxFrequency)
                && (this.binsPerSemitone == binsPerSemitone);
    }

    public ArrayList<LogFrequencySpectrum> getSpectrogram() {
        return this.spectrogram;
    }

    public WindowFunction getWindowFunction() {
        return this.windowFunction;
    }

    public File getFile() {
        return this.file;
    }

    public void setFile(File file) {
        this.file = file;
    }

    /**
     * generate the XML data for storing the project in an .mpr file
     * @param projectPath
     * @return
     */
    public Element toXml(Path projectPath) {
        Element spectrogram = new Element("spectrogram");

        spectrogram.addAttribute(new Attribute("windowFunction", this.windowFunction.toString()));
        spectrogram.addAttribute(new Attribute("hopSize", "" + this.hopSize));
        spectrogram.addAttribute(new Attribute("minFrequency", "" + this.minFrequency));
        spectrogram.addAttribute(new Attribute("maxFrequency", "" + this.maxFrequency));
        spectrogram.addAttribute(new Attribute("binsPerSemitone", "" + this.binsPerSemitone));
        spectrogram.addAttribute(new Attribute("normalize", "" + this.normalize));
        spectrogram.addAttribute(new Attribute("file", projectPath.relativize(this.file.toPath()).toString()));

        return spectrogram;
    }

    /**
     * factory to create SpectrogramImage from the corresponding entry in an .mpr file
     * @param projectAudioData
     * @param projectBasePath
     * @return
     */
    protected static SpectrogramImage createSpectrogramImage(Element projectAudioData, String projectBasePath) {
        Element spectrogramData = projectAudioData.getFirstChildElement("spectrogram");
        if (spectrogramData == null)
            return null;

        File imageFile = new File(Tools.uniformPath(projectBasePath + spectrogramData.getAttributeValue("file")));
        if (!imageFile.exists())
            return null;

        BufferedImage image = null;
        try {
            image = ImageIO.read(imageFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (image == null) {
            System.out.println("DEBUG");
            return null;
        }

        int hopSize = Integer.parseInt(spectrogramData.getAttributeValue("hopSize"));
        float minFrequency = Float.parseFloat(spectrogramData.getAttributeValue("minFrequency"));
        float maxFrequency = Float.parseFloat(spectrogramData.getAttributeValue("maxFrequency"));
        int binsPerSemitone = Integer.parseInt(spectrogramData.getAttributeValue("binsPerSemitone"));
        boolean normalize = Boolean.parseBoolean(spectrogramData.getAttributeValue("normalize"));

        String windowFunctionString = spectrogramData.getAttributeValue("windowFunction");
        int windowLength = Integer.parseInt(windowFunctionString.substring(windowFunctionString.lastIndexOf("=") + 1, windowFunctionString.indexOf("}")));

        WindowFunction windowFunction = null;
        if (windowFunctionString.startsWith("Hamming"))
            windowFunction = new WindowFunction.Hamming(windowLength);
        else if (windowFunctionString.startsWith("Hann"))
            windowFunction = new WindowFunction.Hann(windowLength);
        else if (windowFunctionString.startsWith("Triangle"))
            windowFunction = new WindowFunction.Triangle(windowLength);
        else if (windowFunctionString.startsWith("Welch"))
            windowFunction = new WindowFunction.Welch(windowLength);
        else if (windowFunctionString.startsWith("InverseWindowFunction")) {
            String baseFunction = windowFunctionString.substring(windowFunctionString.indexOf("=") + 1);
            if (baseFunction.startsWith("Hamming"))
                windowFunction = new WindowFunction.InverseWindowFunction(new WindowFunction.Hamming(windowLength));
            else if (baseFunction.startsWith("Hann"))
                windowFunction = new WindowFunction.InverseWindowFunction(new WindowFunction.Hann(windowLength));
            else if (baseFunction.startsWith("Triangle"))
                windowFunction = new WindowFunction.InverseWindowFunction(new WindowFunction.Triangle(windowLength));
            else if (baseFunction.startsWith("Welch"))
                windowFunction = new WindowFunction.InverseWindowFunction(new WindowFunction.Welch(windowLength));
            else
                windowFunction = new WindowFunction.InverseWindowFunction(new WindowFunction.Hamming(windowLength));    // default
        } else
            windowFunction = new WindowFunction.Hamming(windowLength);  // default

        return new SpectrogramImage(image, windowFunction, hopSize, minFrequency, maxFrequency, binsPerSemitone, normalize);
    }
}
