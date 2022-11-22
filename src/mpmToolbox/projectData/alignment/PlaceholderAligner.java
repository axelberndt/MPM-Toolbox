package mpmToolbox.projectData.alignment;

import com.alee.laf.label.WebLabel;
import mpmToolbox.gui.Settings;
import mpmToolbox.projectData.audio.Audio;

import java.awt.*;

/**
 * This is an illustration on how to use AbstractAlignmentComputation.
 * @author Axel Berndt
 */
public class PlaceholderAligner extends AbstractAlignmentComputation {
    // put all your class variables in here
    private Audio previousAudio = null;         // if you wish to keep track of whether a new audio is processed or one for which you have already generated other data

    /**
     * Constructor, the constructor method of the implementation should start with super(...) to
     * make sure that the implementation has a meaningful name.
     */
    public PlaceholderAligner() {
        super("Placeholder Aligner");       // here you name your alignment method, e.g. "FastDTW" or "UltraFastMPSearch" or "Smith-Waterman" or "Needleman-Wunsch" or "Longest Common Subsequence" ...
        // anything else you wish to initialize?
    }

    /**
     * This method defines the real contents of the dialog. Consider that the content panel's layout manager is of type GridBagLayout.
     * Use this.addToContentPanel() to add your GUI components to the GridBagLayout.
     */
    @Override
    public void makeContentPanel() {
        WebLabel placeholder = new WebLabel("This is a placeholder. You have to implement method makeContentPanel()!");
        placeholder.setHorizontalAlignment(WebLabel.RIGHT);
        placeholder.setPadding(Settings.paddingInDialogs);
        this.addToContentPanel(placeholder, 0, 0, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH);
    }

    /**
     *
     * @param audio the audio object whose alignment you wish to compute
     * @return the input audio object with altered alignment
     */
    @Override
    public Audio compute(Audio audio) {
        if (audio != this.previousAudio) {
            // do your preprocessing
        }
        this.previousAudio = audio;     // store for later reference

        // compute the alignment and update audio.getAlignment() accordingly

        return audio;
    }
}
