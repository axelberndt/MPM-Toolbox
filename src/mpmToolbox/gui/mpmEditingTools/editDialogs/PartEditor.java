package mpmToolbox.gui.mpmEditingTools.editDialogs;

import com.alee.laf.label.WebLabel;
import com.alee.laf.spinner.WebSpinner;
import com.alee.laf.text.WebTextField;
import meico.mpm.elements.Part;
import mpmToolbox.gui.Settings;

import javax.swing.*;
import java.awt.*;

/**
 * the editor dialog for MPM part elements
 * @author Axel Berndt
 */
public class PartEditor extends EditDialog<Part> {
    private WebTextField name;
    private WebSpinner number;
    private WebSpinner midiChannel;
    private WebSpinner midiPort;

    /**
     * constructor
     */
    public PartEditor() {
        super("Edit Part");
    }

    /**
     * the GUI
     */
    @Override
    public void makeContentPanel() {
        WebLabel nameLabel = new WebLabel("Part Name:");
        nameLabel.setHorizontalAlignment(WebLabel.RIGHT);
        nameLabel.setPadding(Settings.paddingInDialogs);
        this.addToContentPanel(nameLabel, 0, 0, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH);

        this.name = new WebTextField();
        this.name.setHorizontalAlignment(WebTextField.LEFT);
        this.name.setPadding(Settings.paddingInDialogs);
        this.addToContentPanel(this.name, 1, 0, 3, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH);

        WebLabel numberLabel = new WebLabel("Number:");
        numberLabel.setHorizontalAlignment(WebLabel.RIGHT);
        numberLabel.setPadding(Settings.paddingInDialogs);
        this.addToContentPanel(numberLabel, 0, 2, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH);

        this.number = new WebSpinner(new SpinnerNumberModel(1, 0, Integer.MAX_VALUE, 1));
        int width = getFontMetrics(this.number.getFont()).stringWidth("999.999.999");
        this.number.setMinimumWidth(width);
        this.number.setMaximumWidth(width);
        this.addToContentPanel(this.number, 1, 2, 1, 1, 0.01, 1.0, 5, 5, GridBagConstraints.BOTH);

        WebLabel channelLabel = new WebLabel("MIDI Channel:");
        channelLabel.setHorizontalAlignment(WebLabel.RIGHT);
        channelLabel.setPadding(Settings.paddingInDialogs);
        this.addToContentPanel(channelLabel, 0, 3, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH);

        this.midiChannel = new WebSpinner(new SpinnerNumberModel(0, 0, 15, 1));
        this.midiChannel.setMinimumWidth(getFontMetrics(this.midiChannel.getFont()).stringWidth("99"));
        this.addToContentPanel(this.midiChannel, 1, 3, 1, 1, 0.01, 1.0, 5, 5, GridBagConstraints.BOTH);

        WebLabel portLabel = new WebLabel("MIDI Port:");
        portLabel.setHorizontalAlignment(WebLabel.RIGHT);
        portLabel.setPadding(Settings.paddingInDialogs);
        this.addToContentPanel(portLabel, 0, 4, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH);

        this.midiPort = new WebSpinner(new SpinnerNumberModel(0, 0, 255, 1));
        this.midiPort.setMinimumWidth(getFontMetrics(this.midiPort.getFont()).stringWidth("999"));
        this.addToContentPanel(this.midiPort, 1, 4, 1, 1, 0.01, 1.0, 5, 5, GridBagConstraints.BOTH);

        this.addToContentPanel(Box.createHorizontalGlue(), 2, 4, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.HORIZONTAL);

       this.addIdInput(5);
    }

    /**
     * create or edit a part with the dialog
     * @param part the object to be edited via the dialog or null to create a new one
     * @return
     */
    @Override
    public Part edit(Part part) {
        if (part != null) {
            this.name.setText(part.getName());
            this.number.setValue(part.getNumber());
            this.midiChannel.setValue(part.getMidiChannel());
            this.midiPort.setValue(part.getMidiPort());
            this.id.setText(part.getId());
        }

        this.name.selectAll();

        this.setVisible(true);

        // after the dialog closed do the following

        if (!this.isOk())
            return part;

        String id = this.id.getText();
        if (id.isEmpty())
            id = null;

        if (part == null)
            return Part.createPart(this.name.getText(), (int) this.number.getValue(), (int) this.midiChannel.getValue(), (int) this.midiPort.getValue(), id);

        part.setName(this.name.getText());
        part.setNumber((int) this.number.getValue());
        part.setMidiChannel((int) this.midiChannel.getValue());
        part.setMidiPort((int) this.midiPort.getValue());
        part.setId(id);

        return part;
    }
}
