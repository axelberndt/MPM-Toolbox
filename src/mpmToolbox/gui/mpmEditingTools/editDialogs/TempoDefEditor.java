package mpmToolbox.gui.mpmEditingTools.editDialogs;

import com.alee.laf.label.WebLabel;
import com.alee.laf.spinner.WebSpinner;
import com.alee.laf.text.WebTextField;
import meico.mpm.elements.styles.TempoStyle;
import meico.mpm.elements.styles.defs.TempoDef;
import mpmToolbox.gui.Settings;
import mpmToolbox.supplementary.Tools;

import javax.swing.*;
import java.awt.*;
import java.math.RoundingMode;

/**
 * This class represents the dialog for creating and editing a tempoDef entry in MPM.
 * @author Axel Berndt
 */
public class TempoDefEditor extends EditDialog<TempoDef> {
    private WebTextField name;
    private final TempoStyle styleDef;
    private WebSpinner bpm;

    /**
     * constructor
     */
    public TempoDefEditor(TempoStyle styleDef) {
        super("Edit Tempo Definition");
        this.styleDef = styleDef;
    }

    /**
     * the GUI
     */
    @Override
    public void makeContentPanel() {
        WebLabel nameLabel = new WebLabel("Tempo Name:");
        nameLabel.setHorizontalAlignment(WebLabel.RIGHT);
        nameLabel.setPadding(Settings.paddingInDialogs);
        this.addToContentPanel(nameLabel, 0, 0, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH);

        this.name = new WebTextField("tempo name");
        this.name.setHorizontalAlignment(WebTextField.LEFT);
        this.name.setPadding(Settings.paddingInDialogs);
        this.addToContentPanel(this.name, 1, 0, 3, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH);

        WebLabel tempoLabel = new WebLabel("Tempo:");
        tempoLabel.setHorizontalAlignment(WebLabel.RIGHT);
        tempoLabel.setPadding(Settings.paddingInDialogs);
        addToContentPanel(tempoLabel, 0, 1, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH);

        this.bpm = new WebSpinner(new SpinnerNumberModel(100.0, 0.0000000001, Double.POSITIVE_INFINITY, 1.0));
        int width = getFontMetrics(this.bpm.getFont()).stringWidth("999.999.999");
        JSpinner.NumberEditor bpmEditor = (JSpinner.NumberEditor) this.bpm.getEditor();
        bpmEditor.getFormat().setMaximumFractionDigits(10);
        bpmEditor.getFormat().setRoundingMode(RoundingMode.HALF_UP);
        this.bpm.setMinimumWidth(width);
        this.bpm.setMaximumWidth(width);
        this.addToContentPanel(this.bpm, 1, 1, 1, 1, 0.03, 1.0, 5, 5, GridBagConstraints.BOTH);

        WebLabel bpmLabel = new WebLabel("beats per minute");
        bpmLabel.setHorizontalAlignment(WebLabel.LEFT);
        bpmLabel.setPadding(Settings.paddingInDialogs);
        this.addToContentPanel(bpmLabel, 2, 1, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.HORIZONTAL);

        this.addIdInput(2);
    }

    /**
     * Open the def editing dialog.
     * @param def the def to be edited or null if a new one should be created
     * @return the def or null
     */
    @Override
    public TempoDef edit(TempoDef def) {
        if (def != null) {
            this.name.setText(def.getName());
            this.id.setText(def.getId());
            this.bpm.setValue(def.getValue());
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
            def = TempoDef.createTempoDef(this.name.getText(), Tools.round((double) this.bpm.getValue(), 10));
        else {
            this.styleDef.removeDef(def.getName());
            def = TempoDef.createTempoDef(this.name.getText(), Tools.round((double) this.bpm.getValue(), 10));
            this.styleDef.addDef(def);
        }

        def.setId(id);

        return def;
    }
}
