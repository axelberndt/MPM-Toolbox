package mpmToolbox.projectData.alignment.basicPitchLcsAligner;

import com.alee.laf.label.WebLabel;
import com.alee.laf.spinner.WebSpinner;
import mpmToolbox.gui.Settings;
import mpmToolbox.projectData.alignment.AbstractAlignmentComputation;
import mpmToolbox.projectData.alignment.Alignment;
import mpmToolbox.projectData.audio.Audio;

import javax.swing.*;
import java.awt.*;

/**
 * This is an illustration on how to use AbstractAlignmentComputation.
 * @author Vladimir Viro
 */
public class BasicPitchLCSAligner extends AbstractAlignmentComputation {

    private WebSpinner smoothingWidthSpinner;
    private WebSpinner pitchShiftSpinner;
    private WebSpinner toleranceSpinner;

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

        String desc = "<html><b>Basic Pitch + Longest Common Subsequence Alignment</b><br>" +
                "Author: Vladimir Viro<br><br>" +
                "This alignment algorithm first transcribes the audio using Basic Pitch, a music transcription method by Spotify.<br>" +
                "The resulting MIDI sequence is then aligned with the symbolic music data via the Longest Common Subsequence<br>" +
                "algorithm. The algorithm does not touch notes that are already fixed! This allows you to support it and improve<br>" +
                "its results by fixing some notes yourself before you start the algorithm.<br><br>" +
                "Your workflow is as follows:<ol>" +
                "<li>Cancel this dialog. Reset the alignment.</li>" +
                "<li>Drag the first fixed note of the piano roll to the beginning of the music in the audio. It is often easier<br>" +
                "    to see in the CQT spectrogram, so you might want to compute it first.</li>" +
                "<li>Drag the last fixed note of the piano roll to its place at the end of the music. Do not worry if the tone<br>" +
                "    duration does not match. The onsets must align!</li>" +
                "<li>You can also fix some more notes in the course of the music if you like. This is esp. helpful when the<br>" +
                "    algorithm has trouble finding a good alignment due to, e.g., bad audio quality, low signal-to-noise<br>" +
                "    ratio or blurry tone onsets.</li>" +
                "<li>Return to this dialog and click Run.</li>" +
                "<li>The algorithm will fix some further notes. You can drag or unfix them to apply manual corrections.</li>" +
                "</ol>Below parameters can be adjusted to fine-tune the alignment algorithm:<dl>" +
                "<dt>Tempo smoothing</dt>" +
                "   <dd>If the audio is difficult to transcribe precisely and you get erratic, musically implausible tempo changes,<br>" +
                "       you may try to increase this value. This results in a lower mean error at the cost of higher median error.</dd>" +
                "<dt>Pitch shift</dt>" +
                "   <dd>It can happen that the music in the audio recording was played transposed. The algorithm would not be<br>" +
                "       able to find an alignment when the pitches do not match. Use this parameter to compensate for such a<br>" +
                "       pitch offset. If the audio is, let us say, 2 semitones higher than the piano roll/symbolic music, set<br>" +
                "       this parameter to -2.</dd>" +
                "<dt>Simplification tolerance</dt>" +
                "   <dd>Notes will only be fixed if their estimated position differs from their unfixed position by more than the<br>" +
                "       value of this parameter. The higher this parameter, the fewer notes will be fixed  when their timing<br>" +
                "       varies only subtly, and the coarser is the alignment in these cases. Fewer fixed notes make manual<br>" +
                "       adjustments less tedious. On the other hand, the lower this value, the more fine-grained will the<br>" +
                "       alignment be.</dd></dl></html>";

        WebLabel algoDescription = new WebLabel(desc);
        algoDescription.setPadding(Settings.paddingInDialogs);

        this.addToContentPanel(algoDescription, 0 , 0, 3, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH);

        WebLabel smoothingLabel = new WebLabel("Tempo smoothing:", WebLabel.RIGHT);
        smoothingLabel.setPadding(Settings.paddingInDialogs);
        smoothingLabel.setToolTip("A value of 0 effectively disables the smoothing. Set it to higher values to prevent erratic tempo changes.");
        this.addToContentPanel(smoothingLabel, 0 , 1, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH);
        WebLabel smoothingUnitsLabel = new WebLabel("seconds", WebLabel.LEFT);
        smoothingUnitsLabel.setPadding(Settings.paddingInDialogs);
        this.addToContentPanel(smoothingUnitsLabel, 2, 1, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH);

        WebLabel windowFunctionLabel = new WebLabel("Pitch shift:", WebLabel.RIGHT);
        windowFunctionLabel.setPadding(Settings.paddingInDialogs);
        windowFunctionLabel.setToolTip("<html><center>If the music has been performed transposed, use this value to adjust its pitch to the score.<br>E.g., if the audio is 7 semitones lower than the score, set this value to 7.</center></html>");
        this.addToContentPanel(windowFunctionLabel, 0 , 2, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH);
        WebLabel windowUnitsLabel = new WebLabel("semitones", WebLabel.LEFT);
        windowUnitsLabel.setPadding(Settings.paddingInDialogs);
        this.addToContentPanel(windowUnitsLabel, 2, 2, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH);

        WebLabel alignmentApproximationToleranceLabel = new WebLabel("Simplification tolerance:", WebLabel.RIGHT);
        alignmentApproximationToleranceLabel.setPadding(Settings.paddingInDialogs);
        alignmentApproximationToleranceLabel.setToolTip("The lower this value, the more fine-grained the alignment and the more notes will be fixed.");
        this.addToContentPanel(alignmentApproximationToleranceLabel, 0 , 3, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH);
        WebLabel toleranceUnitsLabel = new WebLabel("milliseconds", WebLabel.LEFT);
        toleranceUnitsLabel.setPadding(Settings.paddingInDialogs);
        this.addToContentPanel(toleranceUnitsLabel, 2, 3, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH);

        smoothingWidthSpinner = new WebSpinner(new SpinnerNumberModel(0, 0, 5, 0.25));
        pitchShiftSpinner = new WebSpinner(new SpinnerNumberModel(0, -36, 36, 1));
        toleranceSpinner = new WebSpinner(new SpinnerNumberModel(100., 0, 500, 20.));

        this.addToContentPanel(this.smoothingWidthSpinner, 1 , 1, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH);
        this.addToContentPanel(this.pitchShiftSpinner, 1 , 2, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH);
        this.addToContentPanel(this.toleranceSpinner, 1 , 3, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH);
    }

    /**
     *
     * @param audio the audio object whose alignment you wish to compute
     * @return the input audio object with altered alignment
     */
    @Override
    public Audio compute(Audio audio) {

        boolean reuseModelOutput = audio != null && audio == this.previousAudio;

        int pitchShift = (int) pitchShiftSpinner.getValue();
        double smoothingWidth = (double) smoothingWidthSpinner.getValue();
        double tolerance = (double) toleranceSpinner.getValue();
        tolerance = tolerance / 1000;

        Alignment a = audio.getAlignment();
        double[] _audio = audio.getWaveforms().get(0);
        float sr = audio.getFrameRate();
        String audioId = audio.getFile().getAbsolutePath();

        AlignmentComputation alignCom = new AlignmentComputation(_audio, (int)sr, audioId, a,
                50, 0.3, 0.3,
                pitchShift, smoothingWidth, tolerance,
                reuseModelOutput, transcriber);
        alignCom.execute();

        this.previousAudio = audio;

        return audio;
    }
}
