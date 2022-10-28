package mpmToolbox.gui.mpmEditingTools.editDialogs;

import com.alee.api.annotations.NotNull;
import com.alee.laf.label.WebLabel;
import com.alee.laf.text.WebTextField;
import meico.mpm.Mpm;
import meico.mpm.elements.Header;
import meico.mpm.elements.styles.*;
import mpmToolbox.gui.Settings;

import java.awt.*;

/**
 * The editor dialog for MPM styleDef elements.
 * @author Axel Berndt
 */
public class StyleDefEditor<E extends GenericStyle> extends EditDialog<E> {
    private WebTextField name;
    private final Header header;
    private final String type;

    /**
     * constructor
     */
    public StyleDefEditor(@NotNull String type, Header header) {
        super("Edit Style Definition");
        this.type = type;
        this.header = header;
    }

    /**
     * the GUI
     */
    @Override
    public void makeContentPanel() {
        WebLabel nameLabel = new WebLabel("Style Name:");
        nameLabel.setHorizontalAlignment(WebLabel.RIGHT);
        nameLabel.setPadding(Settings.paddingInDialogs);
        this.addToContentPanel(nameLabel, 0, 0, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH);

        this.name = new WebTextField("style name");
        this.name.setHorizontalAlignment(WebTextField.LEFT);
        this.name.setPadding(Settings.paddingInDialogs);
        this.addToContentPanel(this.name, 1, 0, 3, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH);

        this.addIdInput(1);
    }

    /**
     * Edit or create an MPM styleDef.
     * If an existing one is to be edited and is already part of an MPM, do not forget to set the header first via setContext(),
     * so the dialog will update it accordingly!
     * @param styleDef if not null, don't forget to set the header first via setContext()!
     * @return
     */
    @Override
    public E edit(E styleDef) {
        if (styleDef != null) {
            this.name.setText(styleDef.getName());
            this.id.setText(styleDef.getId());
        }

        this.name.selectAll();

        this.setVisible(true);      // start the dialog

        // after the dialog closed do the following

        if (!this.isOk())           // if input was canceled
            return styleDef;        // return the input unchanged

        String id = this.id.getText();
        if (id.isEmpty())
            id = null;

        if ((styleDef == null) || (this.header == null)) {    // if we created a new styleDef or the one we wanted to edit does not seem to be part of an MPM header (at least it not to our knowledge)
            switch (this.type) {
                case Mpm.ARTICULATION_STYLE:
                    return (E) ArticulationStyle.createArticulationStyle(this.name.getText(), id);
                case Mpm.DYNAMICS_STYLE:
                    return (E) DynamicsStyle.createDynamicsStyle(this.name.getText(), id);
                case Mpm.METRICAL_ACCENTUATION_STYLE:
                    return (E) MetricalAccentuationStyle.createMetricalAccentuationStyle(this.name.getText(), id);
                case Mpm.RUBATO_STYLE:
                    return (E) RubatoStyle.createRubatoStyle(this.name.getText(), id);
                case Mpm.TEMPO_STYLE:
                    return (E) TempoStyle.createTempoStyle(this.name.getText(), id);
                case Mpm.ORNAMENTATION_STYLE:
                    return (E) OrnamentationStyle.createOrnamentationStyle(this.name.getText(), id);
                default:
                    return (E) GenericStyle.createGenericStyle(this.name.getText(), id);
            }
        }

        if (!styleDef.getName().equals(this.name.getText()))
            styleDef = (E) this.header.renameStyleDef(this.type, styleDef.getName(), this.name.getText());

        styleDef.setId(id);

        return styleDef;
    }
}
