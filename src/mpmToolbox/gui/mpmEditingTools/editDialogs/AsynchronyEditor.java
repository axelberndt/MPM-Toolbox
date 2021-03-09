package mpmToolbox.gui.mpmEditingTools.editDialogs;

import com.alee.laf.label.WebLabel;
import com.alee.laf.spinner.WebSpinner;
import mpmToolbox.gui.Settings;
import mpmToolbox.supplementary.Tools;
import nu.xom.Attribute;
import nu.xom.Element;

import javax.swing.*;
import java.awt.*;
import java.math.RoundingMode;

/**
 * The asynchrony editor.
 * @author Axel Berndt
 */
public class AsynchronyEditor extends EditDialog<AsynchronyEditor.AsynchronyData> {
    private WebSpinner millisecondsOffset;

    /**
     * constructor
     */
    public AsynchronyEditor() {
        super("Edit Asynchrony");
    }

    /**
     * GUI
     */
    @Override
    public void makeContentPanel() {
        this.addDateInput(0);

        // milliseconds.offset
        WebLabel offsetLabel = new WebLabel("Offset:");
        offsetLabel.setHorizontalAlignment(WebLabel.RIGHT);
        offsetLabel.setPadding(Settings.paddingInDialogs);
        this.addToContentPanel(offsetLabel, 0, 1, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH);

        this.millisecondsOffset = new WebSpinner(new SpinnerNumberModel(0.0, -99999999999999999.9, 99999999999999999.9, 1.0));
        int width = getFontMetrics(this.millisecondsOffset.getFont()).stringWidth("999.999.999.999.999");
        JSpinner.NumberEditor millisecondsOffsetEditor = (JSpinner.NumberEditor) this.millisecondsOffset.getEditor();
        millisecondsOffsetEditor.getFormat().setMaximumFractionDigits(10);
        millisecondsOffsetEditor.getFormat().setRoundingMode(RoundingMode.HALF_UP);
        this.millisecondsOffset.setMinimumWidth(width);
        this.millisecondsOffset.setMaximumWidth(width);
        this.addToContentPanel(this.millisecondsOffset, 1, 1, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH);

        WebLabel millisecondsLabel = new WebLabel("milliseconds");
        millisecondsLabel.setHorizontalAlignment(WebLabel.LEFT);
        millisecondsLabel.setPadding(Settings.paddingInDialogs);
        this.addToContentPanel(millisecondsLabel, 2, 1, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.HORIZONTAL);

        this.addIdInput(2);
    }

    /**
     * open asynchrony edit dialog
     * @param input the object to be edited via the dialog or null to create a new one
     * @return
     */
    @Override
    public AsynchronyEditor.AsynchronyData edit(AsynchronyEditor.AsynchronyData input) {
        if (input != null) {
            this.date.setValue(input.date);
            this.millisecondsOffset.setValue(input.millisecondsOffset);
            this.id.setText(input.id);
        }

        this.setVisible(true);      // start the dialog

        // after the dialog closed do the following

        if (!this.isOk())           // if dialog was canceled
            return input;           // return the input unchanged

        String id = this.id.getText();
        if (id.isEmpty())
            id = null;

        AsynchronyData output = new AsynchronyData();
        output.date = Tools.round((double) this.date.getValue(), 10);
        output.millisecondsOffset = Tools.round((double) this.millisecondsOffset.getValue(), 10);
        output.id = id;

        return output;
    }

    /**
     * This class holds all information we need to create an MPM asynchrony element
     */
    public static class AsynchronyData {
        public double date = 0.0;
        public double millisecondsOffset = 0.0;
        public String id = null;

        /**
         * default constructor
         */
        public AsynchronyData() {
        }

        public AsynchronyData(Element asynchrony) {
            this.date = Double.parseDouble(asynchrony.getAttributeValue("date"));
            this.millisecondsOffset = Double.parseDouble(asynchrony.getAttributeValue("milliseconds.offset"));

            Attribute id = asynchrony.getAttribute("id", "http://www.w3.org/XML/1998/namespace");
            if (id != null)
                this.id = id.getValue();
        }
    }
}
