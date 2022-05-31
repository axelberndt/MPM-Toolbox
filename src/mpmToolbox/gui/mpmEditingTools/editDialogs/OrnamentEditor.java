package mpmToolbox.gui.mpmEditingTools.editDialogs;

import meico.mpm.elements.maps.OrnamentationMap;
import meico.mpm.elements.maps.data.OrnamentData;

/**
 * The ornament editor.
 * @author Axel Berndt
 */
public class OrnamentEditor extends EditDialog<OrnamentData> {
    /**
     * constructor
     * @param map the map that gets or holds the ornament element
     */
    public OrnamentEditor(OrnamentationMap map) {
        super("Edit Ornament", map);
    }

    /**
     * the GUI
     */
    @Override
    public void makeContentPanel() {

    }

    /**
     * execute the editor dialog
     * @param input the object to be edited via the dialog or null to create a new one
     * @return the ornament data or null
     */
    @Override
    public OrnamentData edit(OrnamentData input) {
        return input;
    }
}
