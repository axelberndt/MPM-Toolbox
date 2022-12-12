package mpmToolbox.gui.audio.utilities;

import com.alee.api.annotations.NotNull;
import com.google.common.base.Function;
import mpmToolbox.projectData.alignment.Alignment;
import mpmToolbox.projectData.alignment.transcription.Transcriber;

import javax.swing.*;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

/**
 * A usual instance of Thread blocks the GUI. So we have to execute code that takes a considerable amount of time in a SwingWorker,
 * see https://www.codementor.io/@isaib.cicourel/swingworker-in-java-du1084lyl for a tutorial.
 * @author Axel Berndt
 */
public class AlignmentComputationWorker extends SwingWorker<Void, Void> {
    private final AlignmentComputation parent;
    double[] audio;
    int sampleRate;
    String audioId;
    Alignment alignment;
    int minNoteLen;
    double onsetThresh;
    double frameThresh;
    boolean reuseModelOutput;

    Transcriber transcriber;
    /**
     * constructor
     */
    public AlignmentComputationWorker(double[] audio, int sampleRate, String audioId, Alignment alignment, int minNoteLen, double onsetThresh, double frameThresh,
                                      boolean reuseModelOutput, @NotNull AlignmentComputation parent, Transcriber transcriber) {
        super();
        this.audio = audio;
        this.sampleRate = sampleRate;
        this.audioId = audioId;
        this.alignment = alignment;
        this.minNoteLen = minNoteLen;
        this.onsetThresh = onsetThresh;
        this.frameThresh = frameThresh;
        this.reuseModelOutput = reuseModelOutput;
        this.parent = parent;
        this.transcriber = transcriber;
    }

    /**
     * this is where the work is done
     * @return the result is transmitted to method done()
     */
    @Override
    protected Void doInBackground() {

        parent.setMaximum(100);

        Function<Double, Void> onProgress = progress -> {
            parent.setProgress((int)(100 * progress));
            return null;
        };

        Function<String, Void> onLabel = input -> {
            parent.setText(input);
            return null;
        };

        Function<Void, Void> onDone = input -> null;
        transcriber.processWithProgress(audio, sampleRate, audioId, alignment, minNoteLen,
                onsetThresh, frameThresh, true, false,
                onLabel, onProgress, onDone);

        return null;
    }


    /**
     * after the work is done, this "finish sequence" is executed
     */
    @Override
    protected void done() {
        try {
            this.get();
        } catch (InterruptedException | ExecutionException | CancellationException e) {
            e.printStackTrace();
        }
        this.parent.dispose();
    }

    /**
     * this will cancel the process
     */
    protected void cancel() {
        this.cancel(true);          // stop the worker
    }
}
