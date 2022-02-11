package mpmToolbox.projectData.audio;

import com.tagtraum.jipes.audio.LogFrequencySpectrum;
import com.tagtraum.jipes.math.WindowFunction;
import meico.supplementary.ColorCoding;
import mpmToolbox.projectData.audio.Audio;

import java.awt.image.BufferedImage;
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
}
