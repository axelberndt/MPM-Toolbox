package mpmToolbox.gui.mpmEditingTools.editDialogs.ornament;

import com.alee.laf.button.WebToggleButton;
import com.alee.laf.grouping.GroupPane;
import meico.mei.Helper;
import meico.midi.EventMaker;
import meico.midi.MidiPlayer;
import mpmToolbox.gui.ProjectPane;
import mpmToolbox.gui.Settings;
import nu.xom.Attribute;
import nu.xom.Element;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.Sequence;
import javax.sound.midi.Track;
import java.awt.*;

/**
 * This class is a wrapper for an MSM note element and adds some
 * further functionality for use in the ornament editor dialog.
 * @author Axel Berndt
 */
public class Note extends WebToggleButton {
    private final ProjectPane projectPane;          // the project pane give us access to the MSM tree where we can play the notes and find the MIDI player for that purpose
    private final Element xml;                      // the MSM note element
    private final double pitch;                     // the MIDI pitch of the note
    private final GroupPane orderButtons = new GroupPane();

    /**
     * constructor
     */
    public Note(Element msmNote, ProjectPane projectPane) {
        super();
        this.projectPane = projectPane;
        this.xml = msmNote;

        this.setSelected(false);

        this.setPadding(Settings.paddingInDialogs);

        Attribute pitchname = this.xml.getAttribute("pitchname");
        String pitchString = "note";
        if (pitchname != null) {
            pitchString = pitchname.getValue();
            Attribute accid = this.xml.getAttribute("accidentals");
            if (accid != null) {
                pitchString = pitchString.concat(Helper.accidDecimal2unicodeString(Double.parseDouble(accid.getValue())));
            }
            Attribute octave = this.xml.getAttribute("octave");
            if (octave != null) {
                String oct = octave.getValue();
                if (oct.endsWith(".0"))
                    oct = oct.replace(".0", "");
                pitchString = pitchString.concat(" " + oct);
            }
        }
        this.setText("<html>" + pitchString + "</html>");

        Attribute pitchAtt = this.xml.getAttribute("midi.pitch");
        if (pitchAtt == null)
            this.pitch = 0.0;
        else
            this.pitch = Double.parseDouble(pitchAtt.getValue());

        this.setToolTip(this.getPartInfo() + "\n" + "Note ID: " + this.getId());    // specify tooltip text

        this.addActionListener(e -> {
            if (this.isSelected())                                                  // if the note is selected, i.e. toggle is activated
                this.play();                                                        // we play it
        });
    }

    /**
     * play this note via MIDI
     */
    public void play() {
        Sequence sequence;
        try {
            sequence = new Sequence(Sequence.PPQ, 720);
        } catch (InvalidMidiDataException e) {
            e.printStackTrace();
            return;
        }

        Track track = sequence.createTrack();
        int pitch = Math.max(Math.min((int) Math.round(this.getPitch()), 127), 0);
        track.add(EventMaker.createNoteOn(0, 0, pitch, 100));
        track.add(EventMaker.createNoteOff(0, 720, pitch, 0));

        MidiPlayer midiPlayer = this.projectPane.getParentMpmToolbox().getMidiPlayerForSingleNotes();
        try {
            midiPlayer.play(sequence);
        } catch (InvalidMidiDataException e) {
            e.printStackTrace();
        }
    }

    /**
     * get the MIDI pitch of the note
     *
     * @return
     */
    public double getPitch() {
        return this.pitch;
    }

    /**
     * generate a String with part information
     *
     * @return
     */
    public String getPartInfo() {
        Element part = (Element) this.xml.getParent().getParent().getParent();
        String output = "Part";

        Attribute number = part.getAttribute("number");
        if (number != null)
            output = output.concat(" " + number.getValue());

        Attribute name = part.getAttribute("name");
        if (name != null)
            output = output.concat(" " + name.getValue());

        return output;
    }

    /**
     * get the ID of the note
     *
     * @return
     */
    public String getId() {
        Attribute idAtt = this.xml.getAttribute("id", "http://www.w3.org/XML/1998/namespace");

        if (idAtt == null)
            return "";

        return idAtt.getValue();
    }

    /**
     * output a description string of the note
     *
     * @return
     */
    public String toString() {
        return this.getText();
    }

    /**
     * enable/disable this note's components
     * @param enable true to enable the note, otherwise false
     */
    @Override
    public void setEnabled(boolean enable) {
        super.setEnabled(enable);

        for (Component c : this.orderButtons.getComponents())
            c.setEnabled(enable);
    }

    /**
     * get the button group widget where we will set the position of the note
     * @return
     */
    protected GroupPane getOrderButtons() {
        return this.orderButtons;
    }

    /**
     * specify how many buttons are available to set the order of the note in the ornament
     * @param number
     * @param parent
     */
    protected void setMaxOrder(int number, NoteOrderComponent parent) {
        this.orderButtons.removeAll();

        if (number < 1)
            return;

        for (int i = 0; i < number; ++i) {
            WebToggleButton button = new WebToggleButton();
            button.setPadding(Settings.paddingInDialogs);
            int finalI = i;
            button.addActionListener(actionEvent -> {
                parent.order.remove(this);      // edit parent.order
                parent.order.add(finalI, this);
                parent.updateNoteOrder();       // update the other notes in parent.order
            });
            this.orderButtons.add(button);
        }

//        if (number > 1) {
//            ((WebToggleButton) this.orderButtons.getComponent(0)).setText("first");
//            ((WebToggleButton) this.orderButtons.getComponent(number - 1)).setText("last");
//        }
    }

    /**
     * set the position of the ote in the ornament's note order
     * @param number must be less than the maximum order
     */
    protected void setOrder(int number) {
        if (this.orderButtons.getComponents().length < number)
            return;
        ((WebToggleButton) this.orderButtons.getComponent(number)).setSelected(true);
    }
}
