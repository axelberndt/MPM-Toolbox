package mpmToolbox.gui.mpmEditingTools.editDialogs.ornament;

import com.alee.laf.panel.WebPanel;
import mpmToolbox.supplementary.Tools;

import java.awt.*;
import java.util.ArrayList;

/**
 * The component to specify an ornament's note order.
 * @author Axel Berndt
 */
public class NoteOrderComponent extends WebPanel {
    private final ArrayList<Note> notes = new ArrayList<>();    // the notes sorted by ascending pitch
    protected final ArrayList<Note> order = new ArrayList<>();    // the notes in the order set by the user

    /**
     * constructor
     */
    public NoteOrderComponent() {
        super(new GridBagLayout());
//        this.setBorder(BorderFactory.createCompoundBorder(new LineBorder(this.noteOrderPanel.getBackground(), Settings.paddingInDialogs/2), new EmptyBorder(Settings.paddingInDialogs, Settings.paddingInDialogs, Settings.paddingInDialogs, Settings.paddingInDialogs)));
//        this.setBackground(Tools.darker(this.getBackground(), 0.1));
    }

    /**
     * enable/disable all sub-components of this
     * @param enable true if this component should be enabled, false otherwise
     */
    @Override
    public void setEnabled(boolean enable) {
        super.setEnabled(enable);

        // de-/activate all sub-components
        for (Note note : this.notes) {
            note.setEnabled(enable);
        }
    }

    /**
     * remove all notes from this component
     */
    public void clear() {
        this.notes.clear();
        this.order.clear();
        this.removeAll();                                   // clear the panel from its current contents
    }

    /**
     * add a note to this component
     * @param note
     */
    public void addNote(Note note) {
        // sort the note into the notes list so the list is ordered by ascending pitch
        int n = 0;
        for (; n < this.notes.size(); ++n) {
            if (note.getPitch() < this.notes.get(n).getPitch())
                break;
        }
        this.notes.add(n, note);

        note.setEnabled(this.isEnabled());
        note.setSelected(false);                            // initially the note is not selected

        // what happens when the note is de-/selected
        note.addActionListener(actionEvent -> {
            if (note.isSelected()) {
                this.order.add(note);
                this.updateMaxNoteOrder();
            } else {
                this.order.remove(note);
                this.updateMaxNoteOrder();
            }
        });

        // add the note to the GUI
        this.removeAll();                                   // clear the panel from its current contents
        for (int i = 0; i < this.notes.size(); ++i) {       // add the notes in reverse (pitch top down) order to the panel
            int row = this.notes.size() - 1 - i;
            Tools.addComponentToGridBagLayout(this, (GridBagLayout) this.getLayout(), this.notes.get(i), 0, row, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH, GridBagConstraints.LINE_START);
            Tools.addComponentToGridBagLayout(this, (GridBagLayout) this.getLayout(), this.notes.get(i).getOrderButtons(), 1, row, 1, 1, 10.0, 1.0, 0, 0, GridBagConstraints.BOTH, GridBagConstraints.LINE_START);
        }
    }

    /**
     * find the note that has the given ID
     * @param id
     * @return the note or null
     */
    private Note getNoteByID(String id) {
        for (Note note : this.notes)
            if (note.getId().equals(id))
                return note;
        return null;
    }

    /**
     * read a note.order string from an MPM ornament element and set the order of the notes accordingly
     * @param noteOrder the ID sequence from MPM ornament element
     */
    public void setNoteOrder(ArrayList<String> noteOrder) {
        this.order.clear();

        for (String id : noteOrder) {
            Note note = this.getNoteByID(id);
            if (note != null) {
                note.setSelected(true);
                this.order.add(note);
            }
        }
        this.updateMaxNoteOrder();
    }

    /**
     * update the order widgets of each note
     */
    private void updateMaxNoteOrder() {
        for (Note note : this.notes) {
            if (this.order.contains(note))                  // if the note is selected
                note.setMaxOrder(this.order.size(), this);  // it should have an order widget
            else                                            // if the note is not selected
                note.setMaxOrder(0, this);                  // it gets no order widget
        }
        this.updateNoteOrder();
    }

    /**
     * set the order of the notes' order widgets according to their position in this.order
     */
    protected void updateNoteOrder() {
        for (int i = 0; i < this.order.size(); ++i)
            this.order.get(i).setOrder(i);
    }

    /**
     * generate the ID sequence for attribute note.order of the MPM ornament element
     * @return
     */
    public ArrayList<String> getNoteOrder() {
        ArrayList<String> output = new ArrayList<>();
        for (Note note : this.order)
            output.add(note.getId());
        return output;
    }
}
