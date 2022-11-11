package mpmToolbox.gui.mpmEditingTools.editDialogs;

import com.alee.laf.label.WebLabel;
import com.alee.laf.text.WebTextField;
import meico.mpm.elements.styles.OrnamentationStyle;
import meico.mpm.elements.styles.defs.OrnamentDef;
import mpmToolbox.gui.Settings;
import mpmToolbox.gui.mpmEditingTools.editDialogs.ornamentDef.DynamicsGradientComponent;
import mpmToolbox.gui.mpmEditingTools.editDialogs.ornamentDef.TemporalSpreadComponent;
import mpmToolbox.gui.mpmEditingTools.editDialogs.supplementary.EditDialogToggleButton;

import javax.swing.*;
import java.awt.*;

/**
 * The ornamentDef editor.
 * @author Axel Berndt
 */
public class OrnamentDefEditor extends EditDialog<OrnamentDef> {
    private WebTextField name;
    private final OrnamentationStyle styleDef;
    private EditDialogToggleButton temporalSpreadButton;
    private TemporalSpreadComponent temporalSpreadPanel;
    private EditDialogToggleButton dynamicsGradientButton;
    private DynamicsGradientComponent dynamicsGradientPanel;

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
        WebLabel nameLabel = new WebLabel("Ornament Name:");
        nameLabel.setHorizontalAlignment(WebLabel.RIGHT);
        nameLabel.setPadding(Settings.paddingInDialogs);
        this.addToContentPanel(nameLabel, 0, 0, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH);

        this.name = new WebTextField("ornament name");
        this.name.setHorizontalAlignment(WebTextField.LEFT);
        this.name.setPadding(Settings.paddingInDialogs);
        this.addToContentPanel(this.name, 1, 0, 3, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH);

        ///////////////////

        WebLabel transformersLabel = new WebLabel("Specify Ornament by Transformers");
        transformersLabel.setHorizontalAlignment(WebLabel.LEFT);
        transformersLabel.setPadding(Settings.paddingInDialogs, 0, Settings.paddingInDialogs, 0);
        this.addToContentPanel(transformersLabel, 0, 2, 4, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH);

        // temporalSpread
        this.temporalSpreadPanel = new TemporalSpreadComponent();
        this.addToContentPanel(this.temporalSpreadPanel, 1, 3, 4, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH);
        this.temporalSpreadButton = new EditDialogToggleButton("Temporal Spread:", new JComponent[]{this.temporalSpreadPanel}, false);
        this.addToContentPanel(this.temporalSpreadButton, 0, 3, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH);

        // dynamicsGradient
        this.dynamicsGradientPanel = new DynamicsGradientComponent();
        this.addToContentPanel(this.dynamicsGradientPanel, 1, 4, 4, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH);
        this.dynamicsGradientButton = new EditDialogToggleButton("Dynamics Gradient:", new JComponent[]{this.dynamicsGradientPanel}, false);
        this.addToContentPanel(this.dynamicsGradientButton, 0, 4, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH);

        ///////////////////

        this.addIdInput(5);
    }

    /**
     * Open the def editing dialog.
     * @param def the def to be edited or null if a new one should be created
     * @return the def or null
     */
    @Override
    public OrnamentDef edit(OrnamentDef def) {
        if (def != null) {
            this.name.setText(def.getName());
            this.id.setText(def.getId());

            if (def.getTemporalSpread() != null) {
                this.temporalSpreadButton.setSelected(true);
                this.temporalSpreadPanel.setId(def.getTemporalSpread().getId());
                this.temporalSpreadPanel.setNoteOffShift(def.getTemporalSpread().noteOffShift);
                this.temporalSpreadPanel.setTimeDomain(def.getTemporalSpread().frameDomain);
                this.temporalSpreadPanel.setFrameStart(def.getTemporalSpread().frameStart);
                this.temporalSpreadPanel.setFrameLength(def.getTemporalSpread().getFrameLength());
                this.temporalSpreadPanel.setIntensity(def.getTemporalSpread().intensity);
            }

            if (def.getDynamicsGradient() != null) {
                this.dynamicsGradientButton.setSelected(true);
                this.dynamicsGradientPanel.setId(def.getDynamicsGradient().getId());
                this.dynamicsGradientPanel.setTransitionFrom(def.getDynamicsGradient().transitionFrom);
                this.dynamicsGradientPanel.setTransitionTo(def.getDynamicsGradient().transitionTo);
            }
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
            def = OrnamentDef.createOrnamentDef(this.name.getText());
        else {
            this.styleDef.removeDef(def.getName());
            def = OrnamentDef.createOrnamentDef(this.name.getText());
            this.styleDef.addDef(def);
        }

        // read and add the ornament transformers to the def
        if (this.temporalSpreadButton.isSelected()) {           // create the temporalSpread element
            OrnamentDef.TemporalSpread temporalSpread = new OrnamentDef.TemporalSpread();
            temporalSpread.frameStart = this.temporalSpreadPanel.getFrameStart();
            temporalSpread.setFrameLength(this.temporalSpreadPanel.getFrameLength());
            temporalSpread.frameDomain = this.temporalSpreadPanel.getTimeDomain();
            temporalSpread.intensity = this.temporalSpreadPanel.getIntensity();
            temporalSpread.noteOffShift = this.temporalSpreadPanel.getNoteOffShift();
            String tsId = this.temporalSpreadPanel.getId();
            if ((tsId != null) && !tsId.isEmpty())
                temporalSpread.setId(tsId);
            def.setTemporalSpread(temporalSpread);
        } else {
            def.setTemporalSpread(null);
        }

        if (this.dynamicsGradientButton.isSelected()) {         // create the dynamicsGradient element
            OrnamentDef.DynamicsGradient dynamicsGradient = new OrnamentDef.DynamicsGradient();
            dynamicsGradient.transitionFrom = this.dynamicsGradientPanel.getTransitionFrom();
            dynamicsGradient.transitionTo = this.dynamicsGradientPanel.getTransitionTo();
            String dgId = this.dynamicsGradientPanel.getId();
            if ((dgId != null) && !dgId.isEmpty())
                dynamicsGradient.setId(dgId);
            def.setDynamicsGradient(dynamicsGradient);
        } else {
            def.setDynamicsGradient(null);
        }

        def.setId(id);

        return def;
    }
}
