package mpmToolbox.gui.mpmEditingTools.editDialogs;

import com.alee.laf.label.WebLabel;
import com.alee.laf.spinner.WebSpinner;
import com.alee.laf.text.WebTextField;
import meico.mei.Helper;
import meico.mpm.elements.Performance;
import mpmToolbox.gui.Settings;

import javax.swing.*;
import java.awt.*;

/**
 * This class represents the dialog for creating and editing a Performance object.
 * @author Axel Berndt
 */
public class PerformanceEditor extends EditDialog<Performance> {
    private WebTextField name;
    private WebSpinner ppq;

    /**
     * constructor
     */
    public PerformanceEditor() {
        super("Edit Performance");
    }

    /**
     * the GUI
     */
    @Override
    public void makeContentPanel() {
        WebLabel nameLabel = new WebLabel("Performance Name:");
        nameLabel.setHorizontalAlignment(WebLabel.RIGHT);
        nameLabel.setPadding(Settings.paddingInDialogs);
        this.addToContentPanel(nameLabel, 0, 0, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH);

        this.name = new WebTextField("name your performance!");
        this.name.setHorizontalAlignment(WebTextField.LEFT);
        this.name.setPadding(Settings.paddingInDialogs);
        this.addToContentPanel(this.name, 1, 0, 3, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH);

        this.addIdInput(1);

        WebLabel timingBasisLabel = new WebLabel("Timing Basis:");
        timingBasisLabel.setHorizontalAlignment(WebLabel.RIGHT);
        timingBasisLabel.setPadding(Settings.paddingInDialogs);
        this.addToContentPanel(timingBasisLabel, 0, 2, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH);

        this.ppq = new WebSpinner(new SpinnerNumberModel(720, 1, Integer.MAX_VALUE, 1));
        int width = getFontMetrics(this.ppq.getFont()).stringWidth("999.999.999");
        this.ppq.setMinimumWidth(width);
        this.ppq.setMaximumWidth(width);
        this.addToContentPanel(this.ppq, 1, 2, 1, 1, 0.03, 1.0, 5, 5, GridBagConstraints.BOTH);

        WebLabel ppqLabel = new WebLabel("pulses per quarter note");
        ppqLabel.setHorizontalAlignment(WebLabel.LEFT);
        ppqLabel.setPadding(Settings.paddingInDialogs);
        this.addToContentPanel(ppqLabel, 2, 2, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.HORIZONTAL);
    }
    /**
     * Open the performance editing dialog.
     * @param performance the performance to be edited or null if a new one should be created
     * @return the performance or null
     */
    @Override
    public Performance edit(Performance performance) {
        if (performance != null) {  // if a performance is to be edited, read its current values
            this.name.setText(performance.getName());
            this.ppq.setValue(performance.getPulsesPerQuarter());
            this.id.setText(Helper.getAttributeValue("id", performance.getXml()));
        }

        this.name.selectAll();

        this.setVisible(true);      // start the dialog

        // after the dialog closed do the following

        if (!this.isOk())           // if input was canceled
            return performance;     // return the performance or null with no alterations

        String id = this.id.getText();
        if (id.isEmpty())
            id = null;

//        if (this.name.isEmpty())                                    // make sure that the performance does actually have a name
//            this.name.setText("random_name_" + UUID.randomUUID().toString());

        if (performance == null) {                                  // if a new performance is to be created
            performance = Performance.createPerformance(this.name.getText());    // create it
            if (performance == null)                                // if performance creation failed
                return null;                                        // return null
        } else {                                                    // if an existing performance was edited
            performance.setName(this.name.getText());               // set its name
        }

        performance.setPulsesPerQuarter((int) this.ppq.getValue());
        performance.setId(id);

        return performance;
    }
}
