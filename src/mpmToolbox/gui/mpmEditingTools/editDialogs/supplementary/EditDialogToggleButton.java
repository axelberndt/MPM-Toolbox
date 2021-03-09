package mpmToolbox.gui.mpmEditingTools.editDialogs.supplementary;

import com.alee.laf.button.WebToggleButton;
import mpmToolbox.gui.Settings;

import javax.swing.*;

/**
 * Some attributes in the edit dialogs can be de/activated. This toggle button can be used for that.
 * @author Axel Berndt
 */
public class EditDialogToggleButton extends WebToggleButton {
    private final JComponent[] componentsToEnable;

    /**
     * constructor
     * @param label
     * @param componentsToEnable
     */
    public EditDialogToggleButton(String label, JComponent[] componentsToEnable, boolean initialState) {
        super(label);
        this.componentsToEnable = componentsToEnable;
        this.setHorizontalAlignment(WebToggleButton.RIGHT);
        this.setPadding(Settings.paddingInDialogs);
        this.setToolTip(initialState ? "deactivate" : "activate");
        this.setSelected(initialState);
        this.trigger();
        this.addActionListener(actionEvent -> this.trigger());
    }

    /**
     * the action to perform when this is triggered
     */
    private void trigger() {
        if (this.isSelected()) {
            for (JComponent component : this.componentsToEnable)
                component.setEnabled(true);
            this.setToolTip("deactivate");
        } else {
            for (JComponent component : this.componentsToEnable)
                component.setEnabled(false);
            this.setToolTip("activate");
        }
    }

    /**
     * This corresponds to WebToggleButton's setSelected() method but also triggers the button's functionality.
     * @param b
     */
    @Override
    public void setSelected(boolean b) {
        super.setSelected(b);
        this.trigger();
    }
}
