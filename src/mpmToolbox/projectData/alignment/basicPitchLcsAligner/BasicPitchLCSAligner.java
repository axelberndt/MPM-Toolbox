package mpmToolbox.projectData.alignment.basicPitchLcsAligner;

import com.alee.laf.spinner.WebSpinner;
import mpmToolbox.projectData.alignment.AbstractAlignmentComputation;
import mpmToolbox.projectData.alignment.Alignment;
import mpmToolbox.projectData.audio.Audio;

import javax.swing.*;
import java.awt.*;

/**
 * This is an illustration on how to use AbstractAlignmentComputation.
 * @author Axel Berndt
 */
public class BasicPitchLCSAligner extends AbstractAlignmentComputation {

    private WebSpinner frameThreshSpinner;
    private WebSpinner onsetThreshSpinner;
    private WebSpinner minNoteLenSpinner;

    private final Transcriber transcriber;
    private Audio previousAudio = null;

    /**
     * Constructor, the constructor method of the implementation should start with super(...) to
     * make sure that the implementation has a meaningful name.
     */
    public BasicPitchLCSAligner() {
        super("BasicPitch+LCS Aligner");

        transcriber = new Transcriber(":memory:");
    }

    /**
     * This method defines the real contents of the dialog. Consider that the content panel's layout manager is of type GridBagLayout.
     * Use this.addToContentPanel() to add your GUI components to the GridBagLayout.
     */
    @Override
    public void makeContentPanel() {
        frameThreshSpinner = new WebSpinner(new SpinnerNumberModel(0.3, 0.1, 0.7, 0.01));
        onsetThreshSpinner = new WebSpinner(new SpinnerNumberModel(0.3, 0.1, 0.7, 0.01));
        minNoteLenSpinner = new WebSpinner(new SpinnerNumberModel(50, 30, 300, 5));

        this.addToContentPanel(this.onsetThreshSpinner, 0 , 1, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH);
        this.addToContentPanel(this.frameThreshSpinner, 0 , 2, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH);
        this.addToContentPanel(this.minNoteLenSpinner, 0 , 3, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH);
    }

    /**
     *
     * @param audio the audio object whose alignment you wish to compute
     * @return the input audio object with altered alignment
     */
    @Override
    public Audio compute(Audio audio) {

        boolean reuseModelOutput = audio != null && audio == this.previousAudio;

        int minNoteLen = (int)minNoteLenSpinner.getValue();
        double onsetThresh = (double) onsetThreshSpinner.getValue();
        double frameThresh = (double) frameThreshSpinner.getValue();
        Alignment a = audio.getAlignment();
        double[] _audio = audio.getWaveforms().get(0);
        float sr = audio.getFrameRate();
        String audioId = audio.getFile().getAbsolutePath();

        AlignmentComputation alignCom = new AlignmentComputation(_audio, (int)sr, audioId, a, minNoteLen,
                onsetThresh, frameThresh, reuseModelOutput, transcriber);
        alignCom.execute();

        this.previousAudio = audio;

        return audio;
    }
}
