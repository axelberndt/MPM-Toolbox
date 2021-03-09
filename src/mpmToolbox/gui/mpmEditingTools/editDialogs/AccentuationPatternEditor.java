package mpmToolbox.gui.mpmEditingTools.editDialogs;

import com.alee.laf.checkbox.WebCheckBox;
import com.alee.laf.label.WebLabel;
import com.alee.laf.spinner.WebSpinner;
import meico.mpm.Mpm;
import meico.mpm.elements.maps.MetricalAccentuationMap;
import meico.mpm.elements.maps.data.MetricalAccentuationData;
import mpmToolbox.gui.Settings;
import mpmToolbox.supplementary.Tools;

import javax.swing.*;
import java.awt.*;
import java.math.RoundingMode;

/**
 * This class creates the editor dialog for MPM accentuationPattern elements.
 * @author Axel Berndt
 */
public class AccentuationPatternEditor extends EditDialog<MetricalAccentuationData> {
    private WebSpinner scale;
    private WebCheckBox loop;
    private WebCheckBox stickToMeasures;

    /**
     * constructor
     * @param map the map that gets or holds the accentuationPattern element
     */
    public AccentuationPatternEditor(MetricalAccentuationMap map) {
        super("Edit Accentuation Pattern", map);
    }

    /**
     * GUI
     */
    @Override
    public void makeContentPanel() {
        this.addDateInput(0);
        this.date.addChangeListener(changeEvent -> this.fullNameRefUpdate(Mpm.METRICAL_ACCENTUATION_STYLE));

        this.addNameRef("Pattern Name:", 1, true);

        // scale
        WebLabel scaleLabel = new WebLabel("Scale:");
        scaleLabel.setHorizontalAlignment(WebLabel.RIGHT);
        scaleLabel.setPadding(Settings.paddingInDialogs);
        this.addToContentPanel(scaleLabel, 0, 2, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH);

        this.scale = new WebSpinner(new SpinnerNumberModel(1.0, -99999999999999999.9, 99999999999999999.9, 1.0));
        int width = getFontMetrics(this.scale.getFont()).stringWidth("999.999.999.999.999");
        JSpinner.NumberEditor scaleEditor = (JSpinner.NumberEditor) this.scale.getEditor();
        scaleEditor.getFormat().setMaximumFractionDigits(10);
        scaleEditor.getFormat().setRoundingMode(RoundingMode.HALF_UP);
        this.scale.setMinimumWidth(width);
        this.scale.setMaximumWidth(width);
        this.addToContentPanel(this.scale, 1, 2, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH);

        WebLabel scaleComment = new WebLabel("for MIDI compatibility stay in [-127, 127]");
        scaleComment.setHorizontalAlignment(WebLabel.LEFT);
        scaleComment.setPadding(Settings.paddingInDialogs);
        this.addToContentPanel(scaleComment, 2, 2, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.HORIZONTAL);

        // loop
        WebLabel loopLabel = new WebLabel("Loop:");
        loopLabel.setHorizontalAlignment(WebLabel.RIGHT);
        loopLabel.setPadding(Settings.paddingInDialogs);
        this.addToContentPanel(loopLabel, 0, 3, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH);

        this.loop = new WebCheckBox(false);
        this.loop.setToolTip("The accentuation pattern repeats continuously.");
        this.addToContentPanel(this.loop, 1, 3, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH);

        // stickToMeasures
        WebLabel stickToMeasuresLabel = new WebLabel("Stick to Measures:");
        stickToMeasuresLabel.setHorizontalAlignment(WebLabel.RIGHT);
        stickToMeasuresLabel.setPadding(Settings.paddingInDialogs);
        this.addToContentPanel(stickToMeasuresLabel, 0, 4, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH);

        this.stickToMeasures = new WebCheckBox(false);
        this.stickToMeasures.setToolTip("Align the accentuation pattern with the time signature.");
        this.addToContentPanel(this.stickToMeasures, 1, 4, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH);

        this.addIdInput(5);
    }

    /**
     * execute the editor dialog
     * @param input the object to be edited via the dialog or null to create a new one
     * @return the accentuation pattern data or null
     */
    @Override
    public MetricalAccentuationData edit(MetricalAccentuationData input) {
        if (input != null) {
            this.date.setValue(input.startDate);
            this.nameRef.setText(input.accentuationPatternDefName);
            this.scale.setValue(input.scale);
            this.loop.setSelected(input.loop);
            this.stickToMeasures.setSelected(input.stickToMeasures);
            this.id.setText(input.xmlId);
        }

        this.fullNameRefUpdate(Mpm.METRICAL_ACCENTUATION_STYLE);

        this.nameRef.selectAll();

        this.setVisible(true);  // start the dialog

        // after the dialog closed do the following

        if (!this.isOk() || this.nameRef.getText().isEmpty())   // if dialog was canceled or no pattern is specified
            return input;           // return the input unchanged

        String id = this.id.getText();
        if (id.isEmpty())
            id = null;

        MetricalAccentuationData output = new MetricalAccentuationData();

        output.startDate = Tools.round((double) this.date.getValue(), 10);
        output.accentuationPatternDefName = this.nameRef.getText();
        output.scale = Tools.round((double) this.scale.getValue(), 10);
        output.loop = this.loop.isSelected();
        output.stickToMeasures = this.stickToMeasures.isSelected();
        output.xmlId = id;

        return output;
    }
}
