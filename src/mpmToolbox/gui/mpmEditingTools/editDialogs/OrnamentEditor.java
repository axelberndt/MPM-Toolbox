package mpmToolbox.gui.mpmEditingTools.editDialogs;

import com.alee.laf.button.WebToggleButton;
import com.alee.laf.grouping.GroupPane;
import com.alee.laf.label.WebLabel;
import com.alee.laf.spinner.WebSpinner;
import meico.mei.Helper;
import meico.mpm.Mpm;
import meico.mpm.elements.Performance;
import meico.mpm.elements.maps.OrnamentationMap;
import meico.mpm.elements.maps.data.OrnamentData;
import meico.msm.Msm;
import mpmToolbox.gui.ProjectPane;
import mpmToolbox.gui.Settings;
import mpmToolbox.gui.mpmEditingTools.editDialogs.ornament.Note;
import mpmToolbox.gui.mpmEditingTools.editDialogs.ornament.NoteOrderComponent;
import mpmToolbox.supplementary.Tools;
import nu.xom.Element;

import javax.swing.*;
import java.awt.*;
import java.math.RoundingMode;
import java.util.ArrayList;

/**
 * The ornament editor.
 * @author Axel Berndt
 */
public class OrnamentEditor extends EditDialog<OrnamentData> {
    private ProjectPane projectPane;
    private Msm msm;
    private Performance performance;
    private WebToggleButton ascendingPitchToggle;
    private WebToggleButton descendingPitchToggle;
    private WebToggleButton noteOrderToggle;
    private NoteOrderComponent noteOrderComponent;
    private WebSpinner scale;

    /**
     * constructor
     * @param map the map that gets or holds the ornament element
     */
    public OrnamentEditor(OrnamentationMap map, ProjectPane projectPane, Performance performance) {
        super("Edit Ornament", map);
        this.projectPane = projectPane;
        this.msm = projectPane.getMsm();
        this.performance = performance;
    }

    /**
     * the GUI
     */
    @Override
    public void makeContentPanel() {
        this.addDateInput(0);
        this.date.addChangeListener(changeEvent -> {
            this.fullNameRefUpdate(Mpm.ORNAMENTATION_STYLE);
            this.updateMsmDate();
            this.updateNoteList();
        });

        /////////////

        this.addNameRef("Select Ornament:", 1, true);

        // note.order
        WebLabel noteOrderLabel = new WebLabel("Note Order:");
        noteOrderLabel.setHorizontalAlignment(WebLabel.RIGHT);
        noteOrderLabel.setPadding(Settings.paddingInDialogs);
        noteOrderLabel.setToolTip("The order in which the notes are processed by the ornament.");
        this.addToContentPanel(noteOrderLabel, 0, 2, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH);

        this.noteOrderComponent = new NoteOrderComponent();
        this.addToContentPanel(this.noteOrderComponent, 1, 3, 3, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH);

        this.ascendingPitchToggle = new WebToggleButton("Ascending Pitch");
        this.ascendingPitchToggle.setPadding(Settings.paddingInDialogs);
        this.ascendingPitchToggle.setToolTip("All the notes displayed are processed ordered by ascending pitch.");
        this.descendingPitchToggle = new WebToggleButton("Descending Pitch");
        this.descendingPitchToggle.setPadding(Settings.paddingInDialogs);
        this.descendingPitchToggle.setToolTip("All the notes displayed are processed ordered by descending pitch.");
        this.noteOrderToggle = new WebToggleButton("Specify Note Order");
        this.noteOrderToggle.setPadding(Settings.paddingInDialogs);
        this.noteOrderToggle.setToolTip("Activate the note(s) that take part in the ornament and adjust their order.");
        this.noteOrderToggle.addChangeListener(e -> this.noteOrderComponent.setEnabled(this.noteOrderToggle.isSelected()));
        GroupPane noteOrderMode = new GroupPane(this.ascendingPitchToggle, this.descendingPitchToggle, this.noteOrderToggle);
        this.addToContentPanel(noteOrderMode, 1, 2, 2, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH);

        // scale
        WebLabel scaleLabel = new WebLabel("Scale Dynamics Gradient:");
        scaleLabel.setHorizontalAlignment(WebLabel.RIGHT);
        scaleLabel.setPadding(Settings.paddingInDialogs);
        scaleLabel.setToolTip("The dynamics gradient is defined in [-1, 1] as part of the ornament's definition.\nHere it is scaled to actual loudness or MIDI velocity values.");
        this.addToContentPanel(scaleLabel, 0, 4, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH);

        this.scale = new WebSpinner(new SpinnerNumberModel(0.0, -99999999999999999.9, 99999999999999999.9, 1.0));
        int width = getFontMetrics(this.scale.getFont()).stringWidth("999.999.999.999.999");
        JSpinner.NumberEditor scaleEditor = (JSpinner.NumberEditor) this.scale.getEditor();
        scaleEditor.getFormat().setMaximumFractionDigits(10);
        scaleEditor.getFormat().setRoundingMode(RoundingMode.HALF_UP);
        this.scale.setMinimumWidth(width);
        this.scale.setMaximumWidth(width);
        this.addToContentPanel(this.scale, 1, 4, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH);

        WebLabel scaleComment = new WebLabel("for MIDI compatibility stay in [-127, 127]");
        scaleComment.setHorizontalAlignment(WebLabel.LEFT);
        scaleComment.setPadding(Settings.paddingInDialogs);
        this.addToContentPanel(scaleComment, 2, 4, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH);

        /////////////

        this.addIdInput(5);
    }

    /**
     * execute the editor dialog
     * @param input the object to be edited via the dialog or null to create a new one
     * @return the ornament data or null
     */
    @Override
    public OrnamentData edit(OrnamentData input) {
        this.ascendingPitchToggle.setSelected(true);    // the default state of note.order may be changed when parsing input
        boolean initNoteOrder = false;                  // this is set true when the note.order attribute of the input object contains a sequence of IDs, so we can later initialize the NoteOrderComponent

        if (input != null) {
            this.date.setValue(input.date);
            this.nameRef.setText(input.ornamentDefName);
            this.scale.setValue(input.scale);
            this.id.setText(input.xmlId);

            // parse note.order
            if ((input.noteOrder != null) && (input.noteOrder.size() > 0)) {
                switch (input.noteOrder.get(0)) {
                    case "ascending pitch":
                        this.ascendingPitchToggle.setSelected(true);    // already set by default
                        break;
                    case "descending pitch":
                        this.descendingPitchToggle.setSelected(true);
                        break;
                    default:
                        this.noteOrderToggle.setSelected(true);
                        initNoteOrder = true;                           // signal to a later point in this method to read and set the order of notes in the NoteOrderComponent after it is initializes
                }
            }
        }

        this.updateMsmDate();
        this.updateNoteList();
        if (initNoteOrder)                                              // if note.order is a sequence of IDs
            this.noteOrderComponent.setNoteOrder(input.noteOrder);      // communicate the sequence to the NoteOrderComponent
        this.noteOrderComponent.setEnabled(this.noteOrderToggle.isSelected());  // if ascending or descending pitch is selected, deactivate all notes in the noteOrderComponent

        if (this.nameRef.isEmpty())                                     // the user MUST choose an ornament; so, if the nameRef field is empty, write something in it that will be marked red
            this.nameRef.setText("Choose an ornament!");
        this.fullNameRefUpdate(Mpm.ORNAMENTATION_STYLE);
//        this.nameRef.selectAll();

        this.setVisible(true);  // start the dialog

        // after the dialog closed do the following

        if (!this.isOk())       // if dialog was canceled
            return input;       // return the input unchanged

        String id = this.id.getText();
        if (id.isEmpty())
            id = null;

        OrnamentData output = new OrnamentData();

        output.date = Tools.round((double) this.date.getValue(), 10);

        if (!this.nameRef.getText().isEmpty())
            output.ornamentDefName = this.nameRef.getText();

        output.scale = Tools.round((double) this.scale.getValue(), 10);
        output.xmlId = id;

        // note.order
        if (this.descendingPitchToggle.isSelected()) {
            output.noteOrder = new ArrayList<>();
            output.noteOrder.add("descending pitch");
        } else if (this.noteOrderToggle.isSelected()) {
            output.noteOrder = this.noteOrderComponent.getNoteOrder();
        } //else if (this.ascendingPitchToggle.isSelected()) {
//            output.noteOrder = null;                          // unnecessary
//        }

        return output;
    }

    /**
     * make sure that the correct value is in the editor's msmDate, so the noteOrderComponent list is filled correctly
     */
    private void updateMsmDate() {
        double date = Tools.round((double) this.date.getValue(), 10);
        if (this.performance.getPPQ() != this.msm.getPPQ())
            this.setMsmDate((date * this.msm.getPPQ()) / this.performance.getPPQ());
        else
            this.setMsmDate(date);
    }

    /**
     * This method finds the parts that this map applies to and collects their score elements. Be sure that this.map != null.
     * @return a list of MSM score elements
     */
    private ArrayList<Element> getMsmScores() {
        if (this.msm == null)
            return new ArrayList<>();

        ArrayList<Element> result = new ArrayList<>();

        if (this.map.getLocalHeader() != null) {    // if we are in a local map we need to find the corresponding MSM part
            Element mpmPart = (Element) this.map.getLocalHeader().getXml().getParent();

            String number = Helper.getAttributeValue("number", mpmPart);
            for (Element part : this.msm.getParts()) {
                if (Helper.getAttributeValue("number", part).equals(number)
                        && (part.getFirstChildElement("dated") != null)
                        && (part.getFirstChildElement("dated").getFirstChildElement("score") != null)){
                    result.add(part.getFirstChildElement("dated").getFirstChildElement("score"));
                    return result;
                }
            }
            String name = Helper.getAttributeValue("name", mpmPart);
            for (Element part : this.msm.getParts()) {
                if (Helper.getAttributeValue("name", part).equals(name)
                        && (part.getFirstChildElement("dated") != null)
                        && (part.getFirstChildElement("dated").getFirstChildElement("score") != null)){
                    result.add(part.getFirstChildElement("dated").getFirstChildElement("score"));
                    return result;
                }
            }
            String midiChannel = Helper.getAttributeValue("midi.channel", mpmPart);
            String midiPort = Helper.getAttributeValue("midi.port", mpmPart);
            for (Element part : this.msm.getParts()) {
                if (Helper.getAttributeValue("midi.channel", part).equals(midiChannel)
                        && Helper.getAttributeValue("midi.port", part).equals(midiPort)
                        && (part.getFirstChildElement("dated") != null)
                        && (part.getFirstChildElement("dated").getFirstChildElement("score") != null)){
                    result.add(part.getFirstChildElement("dated").getFirstChildElement("score"));
                    return result;
                }
            }
            return result;
        }

        // we are in a global map, so we return all parts
        for (Element part : this.msm.getParts()) {
            if ((part.getFirstChildElement("dated") != null) && (part.getFirstChildElement("dated").getFirstChildElement("score") != null)) // in this context we are interested only in parts with scores
                result.add(part.getFirstChildElement("dated").getFirstChildElement("score"));
        }
        return result;
    }

    /**
     * This method finds the MSM notes at the current date and updates the list of notes.
     */
    private void updateNoteList() {
        if ((this.map == null) || (this.date == null))
            return;

        ArrayList<Element> scores = this.getMsmScores();
        if (scores.isEmpty())
            return;

        this.noteOrderComponent.clear();

        for (Element score : scores) {      // collect all notes at the specified date and add them to the noteOrderComponent
            for (Element noteElement : score.getChildElements("note")) {
                double noteDate = Double.parseDouble(noteElement.getAttributeValue("date"));
                if (noteDate < this.msmDate)
                    continue;
                if (noteDate > this.msmDate)
                    break;
                if ((noteDate == this.msmDate) && (noteElement.getAttribute("id", "http://www.w3.org/XML/1998/namespace") != null)) {   // if the note has no ID we cannot refer it
                    Note note = new Note(noteElement, this.projectPane);
                    this.noteOrderComponent.addNote(note);
                }
            }
        }
        this.pack();                        // resize the dialog so all notes are visible
    }
}
