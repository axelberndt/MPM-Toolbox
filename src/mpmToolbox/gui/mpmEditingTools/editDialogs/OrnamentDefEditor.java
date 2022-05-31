package mpmToolbox.gui.mpmEditingTools.editDialogs;

import meico.mpm.elements.styles.OrnamentationStyle;
import meico.mpm.elements.styles.defs.OrnamentDef;

/**
 * The ornamentDef editor.
 * @author Axel Berndt
 */
public class OrnamentDefEditor extends EditDialog<OrnamentDef> {
    private final OrnamentationStyle styleDef;

    /**
     * constructor
     * @param styleDef
     */
    public OrnamentDefEditor(OrnamentationStyle styleDef) {
        super("Edit Ornament Definition");
        this.styleDef = styleDef;
    }

    /**
     * the GUI
     */
    @Override
    public void makeContentPanel() {

    }

    /**
     * Open the def editing dialog.
     * @param def the def to be edited or null if a new one should be created
     * @return the def or null
     */
    @Override
    public OrnamentDef edit(OrnamentDef def) {
        return def;
    }
}
