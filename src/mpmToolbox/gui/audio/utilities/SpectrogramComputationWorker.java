package mpmToolbox.gui.audio.utilities;

import com.tagtraum.jipes.SignalPump;
import com.tagtraum.jipes.audio.AudioBuffer;
import com.tagtraum.jipes.math.WindowFunction;

import javax.swing.*;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

/**
 * A usual instance of Thread blocks the GUI. So we have to execute code that takes a considerable amount of time in a SwingWorker,
 * see https://www.codementor.io/@isaib.cicourel/swingworker-in-java-du1084lyl for a tutorial.
 * @author Axel Berndt
 */
public class SpectrogramComputationWorker extends SwingWorker<SpectrogramImage, Void> {
    private final WindowFunction windowFunction;
    private final int hopSize;
    private final float minFreq;
    private final float maxFreq;
    private final int bins;
    private final boolean normalize;
    private final SignalPump<AudioBuffer> pump = new SignalPump<>();
    private final SpectrogramComputation parent;


    /**
     * constructor
     */
    public SpectrogramComputationWorker(WindowFunction windowFunction, int hopSize, float minFreq, float maxFreq, int bins, boolean normalize, SpectrogramComputation parent) {
        super();
        this.windowFunction = windowFunction;
        this.hopSize = hopSize;
        this.minFreq = minFreq;
        this.maxFreq = maxFreq;
        this.bins = bins;
        this.normalize = normalize;
        this.parent = parent;
    }

    /**
     * this is where the work is done
     * @return the result is transmitted to method done()
     */
    @Override
    protected SpectrogramImage doInBackground() {
        this.parent.parent.parent.parent.getAudio().computeSpectrogram(this.windowFunction, this.hopSize, this.minFreq, this.maxFreq, this.bins, this.normalize, this.pump, this.parent);
        return this.parent.parent.parent.parent.getSpectrogramImage();
    }

//    /**
//     * act on intermediate results
//     * @param list the intermediate results
//     */
//    @Override
//    protected void process(List<Void> list) {
//    }

    /**
     * after the work is done, this "finish sequence" is executed
     */
    @Override
    protected void done() {
        try {
            if (this.get() != null) {                       // if we have a spectrogram image
                SwingUtilities.invokeLater(() -> {          // process this in the EDT thread
                    this.parent.parent.parent.remove(this.parent.parent); // remove the spectrogram specification panel, so we can see the image
                    this.parent.parent.parent.updateZoom();
                    this.parent.parent.parent.updateScroll();
                    this.parent.parent.parent.repaint();
                });
            }
        } catch (InterruptedException | ExecutionException | CancellationException e) {
            e.printStackTrace();
        }
        this.parent.dispose();
    }

    /**
     * this will cancel the process
     */
    protected void cancel() {
        this.pump.cancel();         // stop the pump
        this.cancel(true);          // stop the worker
    }
}
