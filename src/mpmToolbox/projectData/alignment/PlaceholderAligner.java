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
        WebLabel placeholder = new WebLabel(
                "<html>This is a placeholder. You can add your own audio-to-score alignment algorithm here.<br><br>" +
                "To do so, have a look into the source code of MPM Toolbox. Find class \"PlaceHolderAligner.java\"<br>" +
                "in package \"mpmToolbox.projectData.alignment\". It is a minimal prototype model for an alignment<br>" +
                "computation class and explains all necessary implementation details. Basically, you can copy<br>" +
                "that class, rename it and add your code to it. Three methods from the parent abstract class<br>" +
                "must be implemented, the constructor method, \"makeContentPanel()\" and \"compute()\". The latter<br>" +
                "is the method that performs the alignment computation, whereas makeContentPanel() is the place<br>" +
                "where this user interface is defined. Put all parameters of your algorithm in here, so users can<br>" +
                "tweak them. Do not forget to label them and, maybe, add some explanatory text like this.<br><br>" +
                "If your implementation comprises more than one class put them all into a subpackage. Any external<br>" +
                "dependencies go into the \"externals\" folder. In MPM Toolbox we decided to store them locally<br>" +
                "with the source code for several reasons (offline development, version stability, being able to<br>" +
                "compile the code even if the dependency disappears from its web address for whatever reason).<br><br>" +
                "Finally, find class \"AudioDocumentData.java\" in package \"mpmToolbox.gui.audio\". Add an<br>" +
                "instance of your class to the array in the class variable  \"alignmentComputationChooser\",<br>" +
                "just next to \"new PlaceholderAligner()\". Done.</html>");
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
