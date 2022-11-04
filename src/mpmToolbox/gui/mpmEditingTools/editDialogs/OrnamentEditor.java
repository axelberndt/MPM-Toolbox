package mpmToolbox.gui.mpmEditingTools.editDialogs;

import meico.mpm.Mpm;
import meico.mpm.elements.Performance;
import meico.mpm.elements.maps.OrnamentationMap;
import meico.mpm.elements.maps.data.OrnamentData;
import meico.msm.Msm;
import mpmToolbox.supplementary.Tools;

/**
 * The ornament editor.
 * @author Axel Berndt
 */
public class OrnamentEditor extends EditDialog<OrnamentData> {
    private Msm msm;
    private Performance performance;

    /**
     * constructor
     * @param map the map that gets or holds the ornament element
     */
    public OrnamentEditor(OrnamentationMap map, Msm msm, Performance performance) {
        super("Edit Ornament", map);
        this.msm = msm;
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
            // TODO: this.fillNoteIdChooser();
            // TODO: this.checkNoteId();
        });

        /////////////

        // TODO: note id

        /////////////

        this.addNameRef("Predefined Articulation (optional):", 1, true);

        /////////////

        // TODO ...

        /////////////

        this.addIdInput(2);
    }

    /**
     * execute the editor dialog
     * @param input the object to be edited via the dialog or null to create a new one
     * @return the ornament data or null
     */
    @Override
    public OrnamentData edit(OrnamentData input) {
        if (input != null) {
            this.date.setValue(input.date);
            this.nameRef.setText(input.ornamentDefName);

            // TODO ...

            this.id.setText(input.xmlId);
        }

        this.updateMsmDate();
        this.fullNameRefUpdate(Mpm.ORNAMENTATION_STYLE);
        // TODO: this.fillNoteIdChooser();

        this.nameRef.selectAll();
        // TODO: this.checkNoteId();     // make sure that the initial note ID, if it was set before displaying the dialog, is tinted correctly

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

        // TODO ...

        output.xmlId = id;

        return output;
    }

    /**
     * make sure that the correct value is in the editor's msmDate, so the noteIDs list is filled correctly
     */
    private void updateMsmDate() {
        double date = Tools.round((double) this.date.getValue(), 10);
        if (this.performance.getPPQ() != this.msm.getPPQ())
            this.setMsmDate((date * this.msm.getPPQ()) / this.performance.getPPQ());
        else
            this.setMsmDate(date);
    }

}
