package mpmToolbox.projectData.alignment;

import com.alee.laf.label.WebLabel;
import meico.supplementary.KeyValue;
import mpmToolbox.gui.Settings;
import mpmToolbox.projectData.audio.Audio;

import java.awt.*;
import java.util.ArrayList;

/**
 * This is an illustration of how to use AbstractAlignmentComputation.
 * Once a new class is defined and ready for use, add an instance of it to
 * AudioDocumentData's alignmentComputationChooser list. This registers it in the GUI.
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
     * Compute the alignment. This is method where all the work is done.
     * @param audio the audio object whose alignment you wish to compute
     * @return the input audio object with altered alignment
     */
    @Override
    public Audio compute(Audio audio) {
        // Maybe we have some class variables that hold preprocessing data for the audio. We do not need to do the preprocessing again if it is the same audio as last time. The following if block checks, if that is the case.
        if (audio != this.previousAudio) {  // we have to process new audio data, preprocessing is necessary
            this.previousAudio = audio;     // store for later reference
            // do your preprocessing
        }

        // Compute the alignment and update audio.getAlignment() as follows.
        // Your alignment algorithm should have computed a milliseconds timestamp for some notes in the alignment.
        // You do not need to have timestamps for all notes. Instead, once you set the timestamp for some notes, the others in-between will be linearly interpolated by method repositionAll().
        // The timestamped notes should be provided as a list of (Note, Double) pairs such as the following.
        ArrayList<KeyValue<Note, Double>> repositionTheseNotes = new ArrayList<>();
        // The following loop is just a placeholder for your code that generates more meaningful (note, Double) pairs.
        for (Note note : audio.getAlignment().getNoteSequenceInTicks()) {                       // for each note that gets a timestamp ... here I take every note; you will probably have a subset of this
            KeyValue<Note, Double> entry = new KeyValue<>(note, note.getMillisecondsDate());    // instead of note.getMillisecondsDate() you should state your milliseconds timestamp for the note here
            repositionTheseNotes.add(entry);                                                    // add it to the list; no need to list them in any special order
        }

        audio.getAlignment().repositionAll(repositionTheseNotes);  // This updates the audio`s alignment. The time position of notes that are not in the list will be linearly interpolated while keeping their rhythmic property.

        return audio;
    }
}
