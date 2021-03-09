package mpmToolbox.gui.mpmEditingTools.editDialogs;

import com.alee.laf.label.WebLabel;
import com.alee.laf.spinner.WebSpinner;
import com.alee.laf.text.WebTextField;
import meico.mpm.elements.styles.ArticulationStyle;
import meico.mpm.elements.styles.defs.ArticulationDef;
import mpmToolbox.gui.Settings;
import mpmToolbox.gui.mpmEditingTools.editDialogs.supplementary.EditDialogToggleButton;
import mpmToolbox.supplementary.Tools;

import javax.swing.*;
import java.awt.*;
import java.math.RoundingMode;

/**
 * This class represents the dialog for creating and editing an accentuationDef entry in MPM.
 * @author Axel Berndt
 */
public class ArticulationDefEditor extends EditDialog<ArticulationDef> {
    private final ArticulationStyle styleDef;

    private WebTextField name;

    private WebSpinner absDuration;
    private EditDialogToggleButton absDurationButton;

    private WebSpinner relDuration;
    private EditDialogToggleButton relDurationButton;

    private WebSpinner absDurationChange;
    private EditDialogToggleButton absDurationChangeButton;

    private WebSpinner absDurationMs;
    private EditDialogToggleButton absDurationMsButton;

    private WebSpinner absDurationChangeMs;
    private EditDialogToggleButton absDurationChangeMsButton;

    private WebSpinner absDelay;
    private EditDialogToggleButton absDelayButton;

    private WebSpinner absDelayMs;
    private EditDialogToggleButton absDelayMsButton;

    private WebSpinner absVelocity;
    private EditDialogToggleButton absVelocityButton;

    private WebSpinner relVelocity;
    private EditDialogToggleButton relVelocityButton;

    private WebSpinner absVelocityChange;
    private EditDialogToggleButton absVelocityChangeButton;

    private WebSpinner detuneCents;
    private EditDialogToggleButton detuneCentsButton;

    private WebSpinner detuneHz;
    private EditDialogToggleButton detuneHzButton;

    /**
     * constructor
     */
    public ArticulationDefEditor(ArticulationStyle styleDef) {
        super("Edit Articulation Definition");
        this.styleDef = styleDef;
    }

    /**
     * the GUI
     */
    @Override
    public void makeContentPanel() {
        WebLabel nameLabel = new WebLabel("Articulation Name:");
        nameLabel.setHorizontalAlignment(WebLabel.RIGHT);
        nameLabel.setPadding(Settings.paddingInDialogs);
        this.addToContentPanel(nameLabel, 0, 0, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH);

        this.name = new WebTextField("articulation name");
        this.name.setHorizontalAlignment(WebTextField.LEFT);
        this.name.setPadding(Settings.paddingInDialogs);
        this.addToContentPanel(this.name, 1, 0, 3, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH);

        ///////////////

        this.absDuration = new WebSpinner(new SpinnerNumberModel(1.0, 0.0, Double.POSITIVE_INFINITY, 1.0));
        JSpinner.NumberEditor absDurationEditor = (JSpinner.NumberEditor) this.absDuration.getEditor(); // https://stackoverflow.com/questions/34627998/jspinner-number-editor
        absDurationEditor.getFormat().setMaximumFractionDigits(10);
        absDurationEditor.getFormat().setRoundingMode(RoundingMode.HALF_UP);
        int width = getFontMetrics(this.absDuration.getFont()).stringWidth("999.999.999.999");
        this.absDuration.setMinimumWidth(width);
        this.absDuration.setMaximumWidth(width);
        this.addToContentPanel(this.absDuration, 1, 1, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH);

        WebLabel absDurationExplanationLabel = new WebLabel("ticks");
        absDurationExplanationLabel.setHorizontalAlignment(WebLabel.LEFT);
        absDurationExplanationLabel.setPadding(Settings.paddingInDialogs);
        this.addToContentPanel(absDurationExplanationLabel, 2, 1, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.HORIZONTAL);

        this.absDurationButton = new EditDialogToggleButton("Absolute Duration:", new JComponent[]{this.absDuration, absDurationExplanationLabel}, false);
        this.addToContentPanel(this.absDurationButton, 0, 1, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH);

        ///////////////

        this.relDuration = new WebSpinner(new SpinnerNumberModel(1.0, 0.0, Double.POSITIVE_INFINITY, 0.01));
        JSpinner.NumberEditor relDurationEditor = (JSpinner.NumberEditor) this.relDuration.getEditor();
        relDurationEditor.getFormat().setMaximumFractionDigits(10);
        relDurationEditor.getFormat().setRoundingMode(RoundingMode.HALF_UP);
        this.relDuration.setMinimumWidth(width);
        this.relDuration.setMaximumWidth(width);
        this.addToContentPanel(this.relDuration, 1, 2, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH);

        WebLabel relDurationExplanationLabel = new WebLabel("1.0 = 100%");
        relDurationExplanationLabel.setHorizontalAlignment(WebLabel.LEFT);
        relDurationExplanationLabel.setPadding(Settings.paddingInDialogs);
        this.addToContentPanel(relDurationExplanationLabel, 2, 2, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.HORIZONTAL);

        this.relDurationButton = new EditDialogToggleButton("Relative Duration:", new JComponent[]{this.relDuration, relDurationExplanationLabel}, false);
        this.addToContentPanel(this.relDurationButton, 0, 2, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH);

        ///////////////

        this.absDurationChange = new WebSpinner(new SpinnerNumberModel(0.0, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, 1.0));
        JSpinner.NumberEditor absDurationChangeEditor = (JSpinner.NumberEditor) this.absDurationChange.getEditor();
        absDurationChangeEditor.getFormat().setMaximumFractionDigits(10);
        absDurationChangeEditor.getFormat().setRoundingMode(RoundingMode.HALF_UP);
        this.absDurationChange.setMinimumWidth(width);
        this.absDurationChange.setMaximumWidth(width);
        this.addToContentPanel(this.absDurationChange, 1, 3, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH);

        WebLabel absDurationChangeExplanationLabel = new WebLabel("ticks");
        absDurationChangeExplanationLabel.setHorizontalAlignment(WebLabel.LEFT);
        absDurationChangeExplanationLabel.setPadding(Settings.paddingInDialogs);
        this.addToContentPanel(absDurationChangeExplanationLabel, 2, 3, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.HORIZONTAL);

        this.absDurationChangeButton = new EditDialogToggleButton("Absolute Duration Change:", new JComponent[]{this.absDurationChange, absDurationChangeExplanationLabel}, false);
        this.addToContentPanel(this.absDurationChangeButton, 0, 3, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH);

        ///////////////

        this.absDurationMs = new WebSpinner(new SpinnerNumberModel(1.0, 0.0, Double.POSITIVE_INFINITY, 1.0));
        JSpinner.NumberEditor absDurationMsEditor = (JSpinner.NumberEditor) this.absDurationMs.getEditor();
        absDurationMsEditor.getFormat().setMaximumFractionDigits(10);
        absDurationMsEditor.getFormat().setRoundingMode(RoundingMode.HALF_UP);
        this.absDurationMs.setMinimumWidth(width);
        this.absDurationMs.setMaximumWidth(width);
        this.addToContentPanel(this.absDurationMs, 1, 4, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH);

        WebLabel absDurationMsExplanationLabel = new WebLabel("milliseconds");
        absDurationMsExplanationLabel.setHorizontalAlignment(WebLabel.LEFT);
        absDurationMsExplanationLabel.setPadding(Settings.paddingInDialogs);
        this.addToContentPanel(absDurationMsExplanationLabel, 2, 4, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.HORIZONTAL);

        this.absDurationMsButton = new EditDialogToggleButton("Absolute Duration:", new JComponent[]{this.absDurationMs, absDurationMsExplanationLabel}, false);
        this.addToContentPanel(this.absDurationMsButton, 0, 4, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH);

        ///////////////

        this.absDurationChangeMs = new WebSpinner(new SpinnerNumberModel(0.0, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, 1.0));
        JSpinner.NumberEditor absDurationChangeMsEditor = (JSpinner.NumberEditor) this.absDurationChangeMs.getEditor();
        absDurationChangeMsEditor.getFormat().setMaximumFractionDigits(10);
        absDurationChangeMsEditor.getFormat().setRoundingMode(RoundingMode.HALF_UP);
        this.absDurationChangeMs.setMinimumWidth(width);
        this.absDurationChangeMs.setMaximumWidth(width);
        this.addToContentPanel(this.absDurationChangeMs, 1, 5, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH);

        WebLabel absDurationChangeMsExplanationLabel = new WebLabel("milliseconds");
        absDurationChangeMsExplanationLabel.setHorizontalAlignment(WebLabel.LEFT);
        absDurationChangeMsExplanationLabel.setPadding(Settings.paddingInDialogs);
        this.addToContentPanel(absDurationChangeMsExplanationLabel, 2, 5, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.HORIZONTAL);

        this.absDurationChangeMsButton = new EditDialogToggleButton("Absolute Duration Change:", new JComponent[]{this.absDurationChangeMs, absDurationChangeMsExplanationLabel}, false);
        this.addToContentPanel(this.absDurationChangeMsButton, 0, 5, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH);

        ///////////////

        this.absDelay = new WebSpinner(new SpinnerNumberModel(0.0, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, 1.0));
        JSpinner.NumberEditor absDelayEditor = (JSpinner.NumberEditor) this.absDelay.getEditor();
        absDelayEditor.getFormat().setMaximumFractionDigits(10);
        absDelayEditor.getFormat().setRoundingMode(RoundingMode.HALF_UP);
        this.absDelay.setMinimumWidth(width);
        this.absDelay.setMaximumWidth(width);
        this.addToContentPanel(this.absDelay, 1, 6, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH);

        WebLabel absDelayExplanationLabel = new WebLabel("ticks");
        absDelayExplanationLabel.setHorizontalAlignment(WebLabel.LEFT);
        absDelayExplanationLabel.setPadding(Settings.paddingInDialogs);
        this.addToContentPanel(absDelayExplanationLabel, 2, 6, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.HORIZONTAL);

        this.absDelayButton = new EditDialogToggleButton("Absolute Delay:", new JComponent[]{this.absDelay, absDelayExplanationLabel}, false);
        this.addToContentPanel(this.absDelayButton, 0, 6, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH);

        ///////////////

        this.absDelayMs = new WebSpinner(new SpinnerNumberModel(0.0, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, 1.0));
        JSpinner.NumberEditor absDelayMsEditor = (JSpinner.NumberEditor) this.absDelayMs.getEditor();
        absDelayMsEditor.getFormat().setMaximumFractionDigits(10);
        absDelayMsEditor.getFormat().setRoundingMode(RoundingMode.HALF_UP);
        this.absDelayMs.setMinimumWidth(width);
        this.absDelayMs.setMaximumWidth(width);
        this.addToContentPanel(this.absDelayMs, 1, 7, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH);

        WebLabel absDelayMsExplanationLabel = new WebLabel("milliseconds");
        absDelayMsExplanationLabel.setHorizontalAlignment(WebLabel.LEFT);
        absDelayMsExplanationLabel.setPadding(Settings.paddingInDialogs);
        this.addToContentPanel(absDelayMsExplanationLabel, 2, 7, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.HORIZONTAL);

        this.absDelayMsButton = new EditDialogToggleButton("Absolute Delay:", new JComponent[]{this.absDelayMs, absDelayMsExplanationLabel}, false);
        this.addToContentPanel(this.absDelayMsButton, 0, 7, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH);

        ///////////////

        this.absVelocity = new WebSpinner(new SpinnerNumberModel(100.0, 0.0, Double.POSITIVE_INFINITY, 1.0));
        JSpinner.NumberEditor absVelocityEditor = (JSpinner.NumberEditor) this.absVelocity.getEditor();
        absVelocityEditor.getFormat().setMaximumFractionDigits(10);
        absVelocityEditor.getFormat().setRoundingMode(RoundingMode.HALF_UP);
        this.absVelocity.setMinimumWidth(width);
        this.absVelocity.setMaximumWidth(width);
        this.addToContentPanel(this.absVelocity, 1, 8, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH);

        WebLabel absVelocityExplanationLabel = new WebLabel("for MIDI compatibility stay in [0, 127]");
        absVelocityExplanationLabel.setHorizontalAlignment(WebLabel.LEFT);
        absVelocityExplanationLabel.setPadding(Settings.paddingInDialogs);
        this.addToContentPanel(absVelocityExplanationLabel, 2, 8, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.HORIZONTAL);

        this.absVelocityButton = new EditDialogToggleButton("Absolute Velocity:", new JComponent[]{this.absVelocity, absVelocityExplanationLabel}, false);
        this.addToContentPanel(this.absVelocityButton, 0, 8, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH);

        ///////////////

        this.relVelocity = new WebSpinner(new SpinnerNumberModel(1.0, 0.0, Double.POSITIVE_INFINITY, 0.01));
        JSpinner.NumberEditor relVelocityEditor = (JSpinner.NumberEditor) this.relVelocity.getEditor(); // https://stackoverflow.com/questions/34627998/jspinner-number-editor
        relVelocityEditor.getFormat().setMaximumFractionDigits(10);
        relVelocityEditor.getFormat().setRoundingMode(RoundingMode.HALF_UP);
        this.relVelocity.setMinimumWidth(width);
        this.relVelocity.setMaximumWidth(width);
        this.addToContentPanel(this.relVelocity, 1, 9, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH);

        WebLabel relVelocityExplanationLabel = new WebLabel("1.0 = 100%");
        relVelocityExplanationLabel.setHorizontalAlignment(WebLabel.LEFT);
        relVelocityExplanationLabel.setPadding(Settings.paddingInDialogs);
        this.addToContentPanel(relVelocityExplanationLabel, 2, 9, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.HORIZONTAL);

        this.relVelocityButton = new EditDialogToggleButton("Relative Velocity:", new JComponent[]{this.relVelocity, relVelocityExplanationLabel}, false);
        this.addToContentPanel(this.relVelocityButton, 0, 9, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH);

        ///////////////

        this.absVelocityChange = new WebSpinner(new SpinnerNumberModel(0.0, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, 1.0));
        JSpinner.NumberEditor absVelocityChangeEditor = (JSpinner.NumberEditor) this.absVelocityChange.getEditor();
        absVelocityChangeEditor.getFormat().setMaximumFractionDigits(10);
        absVelocityChangeEditor.getFormat().setRoundingMode(RoundingMode.HALF_UP);
        this.absVelocityChange.setMinimumWidth(width);
        this.absVelocityChange.setMaximumWidth(width);
        this.addToContentPanel(this.absVelocityChange, 1, 10, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH);

        WebLabel absVelocityChangeExplanationLabel = new WebLabel("for MIDI compatibility stay in [0, 127]");
        absVelocityChangeExplanationLabel.setHorizontalAlignment(WebLabel.LEFT);
        absVelocityChangeExplanationLabel.setPadding(Settings.paddingInDialogs);
        this.addToContentPanel(absVelocityChangeExplanationLabel, 2, 10, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.HORIZONTAL);

        this.absVelocityChangeButton = new EditDialogToggleButton("Absolute Velocity Change:", new JComponent[]{this.absVelocityChange, absVelocityChangeExplanationLabel}, false);
        this.addToContentPanel(this.absVelocityChangeButton, 0, 10, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH);

        ///////////////

        this.detuneCents = new WebSpinner(new SpinnerNumberModel(0.0, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, 1.0));
        JSpinner.NumberEditor detuneCentsEditor = (JSpinner.NumberEditor) this.detuneCents.getEditor();
        detuneCentsEditor.getFormat().setMaximumFractionDigits(10);
        detuneCentsEditor.getFormat().setRoundingMode(RoundingMode.HALF_UP);
        this.detuneCents.setMinimumWidth(width);
        this.detuneCents.setMaximumWidth(width);
        this.addToContentPanel(this.detuneCents, 1, 11, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH);

        WebLabel detuneCentsExplanationLabel = new WebLabel("cents");
        detuneCentsExplanationLabel.setHorizontalAlignment(WebLabel.LEFT);
        detuneCentsExplanationLabel.setPadding(Settings.paddingInDialogs);
        this.addToContentPanel(detuneCentsExplanationLabel, 2, 11, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.HORIZONTAL);

        this.detuneCentsButton = new EditDialogToggleButton("Detune:", new JComponent[]{this.detuneCents, detuneCentsExplanationLabel}, false);
        this.addToContentPanel(this.detuneCentsButton, 0, 11, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH);

        ///////////////

        this.detuneHz = new WebSpinner(new SpinnerNumberModel(0.0, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, 1.0));
        JSpinner.NumberEditor detuneHzEditor = (JSpinner.NumberEditor) this.detuneHz.getEditor();
        detuneHzEditor.getFormat().setMaximumFractionDigits(10);
        detuneHzEditor.getFormat().setRoundingMode(RoundingMode.HALF_UP);
        this.detuneHz.setMinimumWidth(width);
        this.detuneHz.setMaximumWidth(width);
        this.addToContentPanel(this.detuneHz, 1, 12, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH);

        WebLabel detuneHzExplanationLabel = new WebLabel("Hertz");
        detuneHzExplanationLabel.setHorizontalAlignment(WebLabel.LEFT);
        detuneHzExplanationLabel.setPadding(Settings.paddingInDialogs);
        this.addToContentPanel(detuneHzExplanationLabel, 2, 12, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.HORIZONTAL);

        this.detuneHzButton = new EditDialogToggleButton("Detune:", new JComponent[]{this.detuneHz, detuneHzExplanationLabel}, false);
        this.addToContentPanel(this.detuneHzButton, 0, 12, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH);

        ///////////////

        this.addIdInput(13);
    }

    /**
     * Open the def editing dialog.
     * @param def the def to be edited or null if a new one should be created
     * @return the def or null
     */
    @Override
    public ArticulationDef edit(ArticulationDef def) {
        if (def != null) {
            this.name.setText(def.getName());
            this.id.setText(def.getId());

            this.absDelay.setValue(def.getAbsoluteDelay());
            this.absDelayButton.setSelected(def.getAbsoluteDelay() != 0.0);

            this.absDelayMs.setValue(def.getAbsoluteDelayMs());
            this.absDelayMsButton.setSelected(def.getAbsoluteDelayMs() != 0.0);

            this.relDuration.setValue(def.getRelativeDuration());
            this.relDurationButton.setSelected(def.getRelativeDuration() != 1.0);

            if (def.getAbsoluteDuration() != null) {
                this.absDuration.setValue(def.getAbsoluteDuration());
                this.absDurationButton.setSelected(true);
            } else {
                this.absDurationButton.setSelected(false);
            }

            if (def.getAbsoluteDurationMs() != null) {
                this.absDurationMs.setValue(def.getAbsoluteDurationMs());
                this.absDurationMsButton.setSelected(true);
            } else {
                this.absDurationMsButton.setSelected(false);
            }

            this.absDurationChange.setValue(def.getAbsoluteDurationChange());
            this.absDurationChangeButton.setSelected(def.getAbsoluteDurationChange() != 0.0);

            this.absDurationChangeMs.setValue(def.getAbsoluteDurationChangeMs());
            this.absDurationChangeMsButton.setSelected(def.getAbsoluteDurationChangeMs() != 0.0);

            this.relVelocity.setValue(def.getRelativeVelocity());
            this.relVelocityButton.setSelected(def.getRelativeVelocity() != 1.0);

            if (def.getAbsoluteVelocity() != null) {
                this.absVelocity.setValue(def.getAbsoluteVelocity());
                this.absVelocityButton.setSelected(true);
            } else {
                this.absVelocityButton.setSelected(false);
            }

            this.absVelocityChange.setValue(def.getAbsoluteVelocityChange());
            this.absVelocityChangeButton.setSelected(def.getAbsoluteVelocityChange() != 0.0);

            this.detuneCents.setValue(def.getDetuneCents());
            this.detuneCentsButton.setSelected(def.getDetuneCents() != 0.0);

            this.detuneHz.setValue(def.getDetuneHz());
            this.detuneHzButton.setSelected(def.getDetuneHz() != 0.0);
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
            def = ArticulationDef.createArticulationDef(this.name.getText());
        else {
            this.styleDef.removeDef(def.getName());
            def = ArticulationDef.createArticulationDef(this.name.getText());
            this.styleDef.addDef(def);
        }

        if (this.absDuration.isEnabled())
            def.setAbsoluteDuration(Tools.round((double) this.absDuration.getValue(), 10));

        if (this.relDuration.isEnabled())
            def.setRelativeDuration(Tools.round((double) this.relDuration.getValue(), 10));

        if (this.absDurationChange.isEnabled())
            def.setAbsoluteDurationChange(Tools.round((double) this.absDurationChange.getValue(), 10));

        if (this.absDurationMs.isEnabled())
            def.setAbsoluteDurationMs(Tools.round((double) this.absDurationMs.getValue(), 10));

        if (this.absDurationChangeMs.isEnabled())
            def.setAbsoluteDurationChangeMs(Tools.round((double) this.absDurationChangeMs.getValue(), 10));

        if (this.absDelay.isEnabled())
            def.setAbsoluteDelay(Tools.round((double) this.absDelay.getValue(), 10));

        if (this.absDelayMs.isEnabled())
            def.setAbsoluteDelayMs(Tools.round((double) this.absDelayMs.getValue(), 10));

        if (this.absVelocity.isEnabled())
            def.setAbsoluteVelocity(Tools.round((double) this.absVelocity.getValue(), 10));

        if (this.relVelocity.isEnabled())
            def.setRelativeVelocity(Tools.round((double) this.relVelocity.getValue(), 10));

        if (this.absVelocityChange.isEnabled())
            def.setAbsoluteVelocityChange(Tools.round((double) this.absVelocityChange.getValue(), 10));

        if (this.detuneCents.isEnabled())
            def.setDetuneCents(Tools.round((double) this.detuneCents.getValue(), 10));

        if (this.detuneHz.isEnabled())
            def.setDetuneHz(Tools.round((double) this.detuneHz.getValue(), 10));

        def.setId(id);

        return def;
    }
}
