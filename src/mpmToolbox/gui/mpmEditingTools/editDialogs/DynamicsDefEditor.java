package mpmToolbox.gui.mpmEditingTools.editDialogs;

import com.alee.laf.label.WebLabel;
import com.alee.laf.spinner.WebSpinner;
import com.alee.laf.text.WebTextField;
import meico.mpm.elements.styles.DynamicsStyle;
import meico.mpm.elements.styles.defs.DynamicsDef;
import mpmToolbox.gui.Settings;
import mpmToolbox.supplementary.Tools;

import javax.swing.*;
import java.awt.*;
import java.math.RoundingMode;

/**
 * This class represents the dialog for creating and editing a dynamicsDef entry in MPM.
 * @author Axel Berndt
 */
public class DynamicsDefEditor extends EditDialog<DynamicsDef> {
    private WebTextField name;
    private final DynamicsStyle styleDef;
    private WebSpinner value;

    /**
     * constructor
     */
    public DynamicsDefEditor(DynamicsStyle styleDef) {
        super("Edit Dynamics Definition");
        this.styleDef = styleDef;
    }

    /**
     * the GUI
     */
    @Override
    public void makeContentPanel() {
        WebLabel nameLabel = new WebLabel("Dynamics Name:");
        nameLabel.setHorizontalAlignment(WebLabel.RIGHT);
        nameLabel.setPadding(Settings.paddingInDialogs);
        this.addToContentPanel(nameLabel, 0, 0, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH);

        this.name = new WebTextField("dynamics name");
        this.name.setHorizontalAlignment(WebTextField.LEFT);
        this.name.setPadding(Settings.paddingInDialogs);
        this.addToContentPanel(this.name, 1, 0, 3, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH);

        WebLabel valueLabel = new WebLabel("Numeric Value:");
        valueLabel.setHorizontalAlignment(WebLabel.RIGHT);
        valueLabel.setPadding(Settings.paddingInDialogs);
        addToContentPanel(valueLabel, 0, 1, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH);

        this.value = new WebSpinner(new SpinnerNumberModel(100.0, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, 1.0));
        JSpinner.NumberEditor valueEditor = (JSpinner.NumberEditor) this.value.getEditor();
        valueEditor.getFormat().setMaximumFractionDigits(10);
        valueEditor.getFormat().setRoundingMode(RoundingMode.HALF_UP);
        int width = getFontMetrics(this.value.getFont()).stringWidth("999.999.999");
        this.value.setMinimumWidth(width);
        this.value.setMaximumWidth(width);
        this.addToContentPanel(this.value, 1, 1, 1, 1, 0.03, 1.0, 5, 5, GridBagConstraints.BOTH);

        WebLabel midiCompatibilityLabel = new WebLabel("for MIDI compatibility use values from 0 to 127");
        midiCompatibilityLabel.setHorizontalAlignment(WebLabel.LEFT);
        midiCompatibilityLabel.setPadding(Settings.paddingInDialogs);
        this.addToContentPanel(midiCompatibilityLabel, 2, 1, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.HORIZONTAL);

        this.addIdInput(2);
    }

    /**
     * Open the def editing dialog.
     * @param def the def to be edited or null if a new one should be created
     * @return the def or null
     */
    @Override
    public DynamicsDef edit(DynamicsDef def) {
        if (def != null) {
            this.name.setText(def.getName());
            this.id.setText(def.getId());
            this.value.setValue(def.getValue());
        }

        this.name.selectAll();

        this.setVisible(true);      // start the dialog

        // after the dialog closed do the following

        if (!this.isOk())           // if input was canceled
            return def;             // return the input unchanged

        String id = this.id.getText();
        if (id.isEmpty())
            id = null;

        if (def == null)
            def = DynamicsDef.createDynamicsDef(this.name.getText(), Tools.round((double) this.value.getValue(), 10));
        else {
            this.styleDef.removeDef(def.getName());
            def = DynamicsDef.createDynamicsDef(this.name.getText(), Tools.round((double) this.value.getValue(), 10));
            this.styleDef.addDef(def);
        }

        def.setId(id);

        return def;
    }
}
