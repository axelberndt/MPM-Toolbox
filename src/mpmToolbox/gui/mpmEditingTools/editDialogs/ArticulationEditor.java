package mpmToolbox.gui.mpmEditingTools.editDialogs;

import com.alee.extended.button.WebSplitButton;
import com.alee.laf.button.WebButton;
import com.alee.laf.label.WebLabel;
import com.alee.laf.menu.WebMenuItem;
import com.alee.laf.menu.WebPopupMenu;
import com.alee.laf.spinner.WebSpinner;
import com.alee.laf.text.WebTextField;
import meico.mei.Helper;
import meico.mpm.Mpm;
import meico.mpm.elements.maps.ArticulationMap;
import meico.mpm.elements.maps.data.ArticulationData;
import meico.msm.Msm;
import mpmToolbox.gui.Settings;
import mpmToolbox.gui.mpmEditingTools.editDialogs.supplementary.EditDialogToggleButton;
import mpmToolbox.supplementary.Tools;
import nu.xom.Element;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.math.RoundingMode;
import java.util.ArrayList;

/**
 * This class creates the editor dialog for MPM articulation elements.
 * @author Axel Berndt
 */
public class ArticulationEditor extends EditDialog<ArticulationData> {
    private Msm msm;

    private WebTextField noteId;
    private WebSplitButton noteIdChooser;
    private ArrayList<String> noteIds = new ArrayList<>();

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
     * @param map the map that gets or holds the articulation element
     * @param msm the MSM to which the performance applies, so we can access noteIds
     */
    public ArticulationEditor(ArticulationMap map, Msm msm) {
        super("Edit Articulation", map);
        this.msm = msm;
    }

    /**
     * An interface to set the note ID value externally.
     * @param id
     */
    public void setNoteId(String id) {
        this.noteId.setText(id);
    }

    /**
     * GUI
     */
    @Override
    public void makeContentPanel() {
        this.addDateInput(0);
        this.date.addChangeListener(changeEvent -> {
            this.fullNameRefUpdate(Mpm.ARTICULATION_STYLE);
            this.fillNoteIdChooser();
            this.checkNoteId();
        });

        ///////////////

        WebLabel noteIdLabel = new WebLabel("Note ID (optional):");
        noteIdLabel.setHorizontalAlignment(WebLabel.RIGHT);
        noteIdLabel.setPadding(Settings.paddingInDialogs);
        this.addToContentPanel(noteIdLabel, 0, 1, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH);

        this.noteId = new WebTextField();
        this.noteId.setMinimumWidth(getFontMetrics(this.noteId.getFont()).stringWidth("wwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwww"));
        this.noteId.setHorizontalAlignment(WebTextField.LEFT);
        this.noteId.setPadding(Settings.paddingInDialogs);
        this.noteId.setToolTip("Assign this articulation to a specific note at the date.\nLeave blank to assign it to every note at the date.");
        this.noteId.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {checkNoteId();}
            @Override
            public void removeUpdate(DocumentEvent e) {checkNoteId();}
            @Override
            public void changedUpdate(DocumentEvent e) {checkNoteId();}
        });
        this.addToContentPanel(this.noteId, 1, 1, 2, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH);

        this.noteIdChooser = new WebSplitButton("Choose");
        this.noteIdChooser.setHorizontalAlignment(WebButton.CENTER);
        this.noteIdChooser.setPadding(Settings.paddingInDialogs);
        this.addToContentPanel(this.noteIdChooser, 3, 1, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH);

        ///////////////

        this.addNameRef("Predefined Articulation (optional):", 2, true);

        ///////////////

        WebLabel explainLabel = new WebLabel("Below attributes can be used instead of or in addition to the above referred predefined articulation.");
        explainLabel.setHorizontalAlignment(WebLabel.LEFT);
        explainLabel.setPadding(Settings.paddingInDialogs);
        this.addToContentPanel(explainLabel, 0, 3, 4, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH);

        ///////////////

        this.absDuration = new WebSpinner(new SpinnerNumberModel(1.0, 0.0, Double.POSITIVE_INFINITY, 1.0));
        JSpinner.NumberEditor absDurationEditor = (JSpinner.NumberEditor) this.absDuration.getEditor(); // https://stackoverflow.com/questions/34627998/jspinner-number-editor
        absDurationEditor.getFormat().setMaximumFractionDigits(10);
        absDurationEditor.getFormat().setRoundingMode(RoundingMode.HALF_UP);
        int width = getFontMetrics(this.absDuration.getFont()).stringWidth("999.999.999.999");
        this.absDuration.setMinimumWidth(width);
        this.absDuration.setMaximumWidth(width);
        this.addToContentPanel(this.absDuration, 1, 4, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH);

        WebLabel absDurationExplanationLabel = new WebLabel("ticks");
        absDurationExplanationLabel.setHorizontalAlignment(WebLabel.LEFT);
        absDurationExplanationLabel.setPadding(Settings.paddingInDialogs);
        this.addToContentPanel(absDurationExplanationLabel, 2, 4, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.HORIZONTAL);

        this.absDurationButton = new EditDialogToggleButton("Absolute Duration:", new JComponent[]{this.absDuration, absDurationExplanationLabel}, false);
        this.addToContentPanel(this.absDurationButton, 0, 4, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH);

        ///////////////

        this.relDuration = new WebSpinner(new SpinnerNumberModel(1.0, 0.0, Double.POSITIVE_INFINITY, 0.01));
        JSpinner.NumberEditor relDurationEditor = (JSpinner.NumberEditor) this.relDuration.getEditor();
        relDurationEditor.getFormat().setMaximumFractionDigits(10);
        relDurationEditor.getFormat().setRoundingMode(RoundingMode.HALF_UP);
        this.relDuration.setMinimumWidth(width);
        this.relDuration.setMaximumWidth(width);
        this.addToContentPanel(this.relDuration, 1, 5, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH);

        WebLabel relDurationExplanationLabel = new WebLabel("1.0 = 100%");
        relDurationExplanationLabel.setHorizontalAlignment(WebLabel.LEFT);
        relDurationExplanationLabel.setPadding(Settings.paddingInDialogs);
        this.addToContentPanel(relDurationExplanationLabel, 2, 5, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.HORIZONTAL);

        this.relDurationButton = new EditDialogToggleButton("Relative Duration:", new JComponent[]{this.relDuration, relDurationExplanationLabel}, false);
        this.addToContentPanel(this.relDurationButton, 0, 5, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH);

        ///////////////

        this.absDurationChange = new WebSpinner(new SpinnerNumberModel(0.0, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, 1.0));
        JSpinner.NumberEditor absDurationChangeEditor = (JSpinner.NumberEditor) this.absDurationChange.getEditor();
        absDurationChangeEditor.getFormat().setMaximumFractionDigits(10);
        absDurationChangeEditor.getFormat().setRoundingMode(RoundingMode.HALF_UP);
        this.absDurationChange.setMinimumWidth(width);
        this.absDurationChange.setMaximumWidth(width);
        this.addToContentPanel(this.absDurationChange, 1, 6, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH);

        WebLabel absDurationChangeExplanationLabel = new WebLabel("ticks");
        absDurationChangeExplanationLabel.setHorizontalAlignment(WebLabel.LEFT);
        absDurationChangeExplanationLabel.setPadding(Settings.paddingInDialogs);
        this.addToContentPanel(absDurationChangeExplanationLabel, 2, 6, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.HORIZONTAL);

        this.absDurationChangeButton = new EditDialogToggleButton("Absolute Duration Change:", new JComponent[]{this.absDurationChange, absDurationChangeExplanationLabel}, false);
        this.addToContentPanel(this.absDurationChangeButton, 0, 6, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH);

        ///////////////

        this.absDurationMs = new WebSpinner(new SpinnerNumberModel(1.0, 0.0, Double.POSITIVE_INFINITY, 1.0));
        JSpinner.NumberEditor absDurationMsEditor = (JSpinner.NumberEditor) this.absDurationMs.getEditor();
        absDurationMsEditor.getFormat().setMaximumFractionDigits(10);
        absDurationMsEditor.getFormat().setRoundingMode(RoundingMode.HALF_UP);
        this.absDurationMs.setMinimumWidth(width);
        this.absDurationMs.setMaximumWidth(width);
        this.addToContentPanel(this.absDurationMs, 1, 7, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH);

        WebLabel absDurationMsExplanationLabel = new WebLabel("milliseconds");
        absDurationMsExplanationLabel.setHorizontalAlignment(WebLabel.LEFT);
        absDurationMsExplanationLabel.setPadding(Settings.paddingInDialogs);
        this.addToContentPanel(absDurationMsExplanationLabel, 2, 7, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.HORIZONTAL);

        this.absDurationMsButton = new EditDialogToggleButton("Absolute Duration:", new JComponent[]{this.absDurationMs, absDurationMsExplanationLabel}, false);
        this.addToContentPanel(this.absDurationMsButton, 0, 7, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH);

        ///////////////

        this.absDurationChangeMs = new WebSpinner(new SpinnerNumberModel(0.0, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, 1.0));
        JSpinner.NumberEditor absDurationChangeMsEditor = (JSpinner.NumberEditor) this.absDurationChangeMs.getEditor();
        absDurationChangeMsEditor.getFormat().setMaximumFractionDigits(10);
        absDurationChangeMsEditor.getFormat().setRoundingMode(RoundingMode.HALF_UP);
        this.absDurationChangeMs.setMinimumWidth(width);
        this.absDurationChangeMs.setMaximumWidth(width);
        this.addToContentPanel(this.absDurationChangeMs, 1, 8, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH);

        WebLabel absDurationChangeMsExplanationLabel = new WebLabel("milliseconds");
        absDurationChangeMsExplanationLabel.setHorizontalAlignment(WebLabel.LEFT);
        absDurationChangeMsExplanationLabel.setPadding(Settings.paddingInDialogs);
        this.addToContentPanel(absDurationChangeMsExplanationLabel, 2, 8, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.HORIZONTAL);

        this.absDurationChangeMsButton = new EditDialogToggleButton("Absolute Duration Change:", new JComponent[]{this.absDurationChangeMs, absDurationChangeMsExplanationLabel}, false);
        this.addToContentPanel(this.absDurationChangeMsButton, 0, 8, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH);

        ///////////////

        this.absDelay = new WebSpinner(new SpinnerNumberModel(0.0, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, 1.0));
        JSpinner.NumberEditor absDelayEditor = (JSpinner.NumberEditor) this.absDelay.getEditor();
        absDelayEditor.getFormat().setMaximumFractionDigits(10);
        absDelayEditor.getFormat().setRoundingMode(RoundingMode.HALF_UP);
        this.absDelay.setMinimumWidth(width);
        this.absDelay.setMaximumWidth(width);
        this.addToContentPanel(this.absDelay, 1, 9, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH);

        WebLabel absDelayExplanationLabel = new WebLabel("ticks");
        absDelayExplanationLabel.setHorizontalAlignment(WebLabel.LEFT);
        absDelayExplanationLabel.setPadding(Settings.paddingInDialogs);
        this.addToContentPanel(absDelayExplanationLabel, 2, 9, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.HORIZONTAL);

        this.absDelayButton = new EditDialogToggleButton("Absolute Delay:", new JComponent[]{this.absDelay, absDelayExplanationLabel}, false);
        this.addToContentPanel(this.absDelayButton, 0, 9, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH);

        ///////////////

        this.absDelayMs = new WebSpinner(new SpinnerNumberModel(0.0, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, 1.0));
        JSpinner.NumberEditor absDelayMsEditor = (JSpinner.NumberEditor) this.absDelayMs.getEditor();
        absDelayMsEditor.getFormat().setMaximumFractionDigits(10);
        absDelayMsEditor.getFormat().setRoundingMode(RoundingMode.HALF_UP);
        this.absDelayMs.setMinimumWidth(width);
        this.absDelayMs.setMaximumWidth(width);
        this.addToContentPanel(this.absDelayMs, 1, 10, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH);

        WebLabel absDelayMsExplanationLabel = new WebLabel("milliseconds");
        absDelayMsExplanationLabel.setHorizontalAlignment(WebLabel.LEFT);
        absDelayMsExplanationLabel.setPadding(Settings.paddingInDialogs);
        this.addToContentPanel(absDelayMsExplanationLabel, 2, 10, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.HORIZONTAL);

        this.absDelayMsButton = new EditDialogToggleButton("Absolute Delay:", new JComponent[]{this.absDelayMs, absDelayMsExplanationLabel}, false);
        this.addToContentPanel(this.absDelayMsButton, 0, 10, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH);

        ///////////////

        this.absVelocity = new WebSpinner(new SpinnerNumberModel(100.0, 0.0, Double.POSITIVE_INFINITY, 1.0));
        JSpinner.NumberEditor absVelocityEditor = (JSpinner.NumberEditor) this.absVelocity.getEditor();
        absVelocityEditor.getFormat().setMaximumFractionDigits(10);
        absVelocityEditor.getFormat().setRoundingMode(RoundingMode.HALF_UP);
        this.absVelocity.setMinimumWidth(width);
        this.absVelocity.setMaximumWidth(width);
        this.addToContentPanel(this.absVelocity, 1, 11, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH);

        WebLabel absVelocityExplanationLabel = new WebLabel("for MIDI compatibility stay in [0, 127]");
        absVelocityExplanationLabel.setHorizontalAlignment(WebLabel.LEFT);
        absVelocityExplanationLabel.setPadding(Settings.paddingInDialogs);
        this.addToContentPanel(absVelocityExplanationLabel, 2, 11, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.HORIZONTAL);

        this.absVelocityButton = new EditDialogToggleButton("Absolute Velocity:", new JComponent[]{this.absVelocity, absVelocityExplanationLabel}, false);
        this.addToContentPanel(this.absVelocityButton, 0, 11, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH);

        ///////////////

        this.relVelocity = new WebSpinner(new SpinnerNumberModel(1.0, 0.0, Double.POSITIVE_INFINITY, 0.01));
        JSpinner.NumberEditor relVelocityEditor = (JSpinner.NumberEditor) this.relVelocity.getEditor(); // https://stackoverflow.com/questions/34627998/jspinner-number-editor
        relVelocityEditor.getFormat().setMaximumFractionDigits(10);
        relVelocityEditor.getFormat().setRoundingMode(RoundingMode.HALF_UP);
        this.relVelocity.setMinimumWidth(width);
        this.relVelocity.setMaximumWidth(width);
        this.addToContentPanel(this.relVelocity, 1, 12, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH);

        WebLabel relVelocityExplanationLabel = new WebLabel("1.0 = 100%");
        relVelocityExplanationLabel.setHorizontalAlignment(WebLabel.LEFT);
        relVelocityExplanationLabel.setPadding(Settings.paddingInDialogs);
        this.addToContentPanel(relVelocityExplanationLabel, 2, 12, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.HORIZONTAL);

        this.relVelocityButton = new EditDialogToggleButton("Relative Velocity:", new JComponent[]{this.relVelocity, relVelocityExplanationLabel}, false);
        this.addToContentPanel(this.relVelocityButton, 0, 12, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH);

        ///////////////

        this.absVelocityChange = new WebSpinner(new SpinnerNumberModel(0.0, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, 1.0));
        JSpinner.NumberEditor absVelocityChangeEditor = (JSpinner.NumberEditor) this.absVelocityChange.getEditor();
        absVelocityChangeEditor.getFormat().setMaximumFractionDigits(10);
        absVelocityChangeEditor.getFormat().setRoundingMode(RoundingMode.HALF_UP);
        this.absVelocityChange.setMinimumWidth(width);
        this.absVelocityChange.setMaximumWidth(width);
        this.addToContentPanel(this.absVelocityChange, 1, 13, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH);

        WebLabel absVelocityChangeExplanationLabel = new WebLabel("for MIDI compatibility stay in [0, 127]");
        absVelocityChangeExplanationLabel.setHorizontalAlignment(WebLabel.LEFT);
        absVelocityChangeExplanationLabel.setPadding(Settings.paddingInDialogs);
        this.addToContentPanel(absVelocityChangeExplanationLabel, 2, 13, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.HORIZONTAL);

        this.absVelocityChangeButton = new EditDialogToggleButton("Absolute Velocity Change:", new JComponent[]{this.absVelocityChange, absVelocityChangeExplanationLabel}, false);
        this.addToContentPanel(this.absVelocityChangeButton, 0, 13, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH);

        ///////////////

        this.detuneCents = new WebSpinner(new SpinnerNumberModel(0.0, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, 1.0));
        JSpinner.NumberEditor detuneCentsEditor = (JSpinner.NumberEditor) this.detuneCents.getEditor();
        detuneCentsEditor.getFormat().setMaximumFractionDigits(10);
        detuneCentsEditor.getFormat().setRoundingMode(RoundingMode.HALF_UP);
        this.detuneCents.setMinimumWidth(width);
        this.detuneCents.setMaximumWidth(width);
        this.addToContentPanel(this.detuneCents, 1, 14, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH);

        WebLabel detuneCentsExplanationLabel = new WebLabel("cents");
        detuneCentsExplanationLabel.setHorizontalAlignment(WebLabel.LEFT);
        detuneCentsExplanationLabel.setPadding(Settings.paddingInDialogs);
        this.addToContentPanel(detuneCentsExplanationLabel, 2, 14, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.HORIZONTAL);

        this.detuneCentsButton = new EditDialogToggleButton("Detune:", new JComponent[]{this.detuneCents, detuneCentsExplanationLabel}, false);
        this.addToContentPanel(this.detuneCentsButton, 0, 14, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH);

        ///////////////

        this.detuneHz = new WebSpinner(new SpinnerNumberModel(0.0, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, 1.0));
        JSpinner.NumberEditor detuneHzEditor = (JSpinner.NumberEditor) this.detuneHz.getEditor();
        detuneHzEditor.getFormat().setMaximumFractionDigits(10);
        detuneHzEditor.getFormat().setRoundingMode(RoundingMode.HALF_UP);
        this.detuneHz.setMinimumWidth(width);
        this.detuneHz.setMaximumWidth(width);
        this.addToContentPanel(this.detuneHz, 1, 15, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH);

        WebLabel detuneHzExplanationLabel = new WebLabel("Hertz");
        detuneHzExplanationLabel.setHorizontalAlignment(WebLabel.LEFT);
        detuneHzExplanationLabel.setPadding(Settings.paddingInDialogs);
        this.addToContentPanel(detuneHzExplanationLabel, 2, 15, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.HORIZONTAL);

        this.detuneHzButton = new EditDialogToggleButton("Detune:", new JComponent[]{this.detuneHz, detuneHzExplanationLabel}, false);
        this.addToContentPanel(this.detuneHzButton, 0, 15, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH);

        ///////////////

        this.addIdInput(16);
    }

    /**
     * execute the editor dialog
     * @param input the object to be edited via the dialog or null to create a new one
     * @return the articulation data or null
     */
    @Override
    public ArticulationData edit(ArticulationData input) {
        if (input != null) {
            this.date.setValue(input.date);
            this.nameRef.setText(input.articulationDefName);

            if (input.noteid != null)
                this.noteId.setText((input.noteid.startsWith("#")) ? input.noteid.substring(1) : input.noteid);

            this.absDelay.setValue(input.absoluteDelay);
            this.absDelayButton.setSelected(input.absoluteDelay != 0.0);

            this.absDelayMs.setValue(input.absoluteDelayMs);
            this.absDelayMsButton.setSelected(input.absoluteDelayMs != 0.0);

            this.relDuration.setValue(input.relativeDuration);
            this.relDurationButton.setSelected(input.relativeDuration != 1.0);

            if (input.absoluteDuration != null) {
                this.absDuration.setValue(input.absoluteDuration);
                this.absDurationButton.setSelected(true);
            } else {
                this.absDurationButton.setSelected(false);
            }

            if (input.absoluteDurationMs != null) {
                this.absDurationMs.setValue(input.absoluteDurationMs);
                this.absDurationMsButton.setSelected(true);
            } else {
                this.absDurationMsButton.setSelected(false);
            }

            this.absDurationChange.setValue(input.absoluteDurationChange);
            this.absDurationChangeButton.setSelected(input.absoluteDurationChange != 0.0);

            this.absDurationChangeMs.setValue(input.absoluteDurationChangeMs);
            this.absDurationChangeMsButton.setSelected(input.absoluteDurationChangeMs != 0.0);

            this.relVelocity.setValue(input.relativeVelocity);
            this.relVelocityButton.setSelected(input.relativeVelocity != 1.0);

            if (input.absoluteVelocity != null) {
                this.absVelocity.setValue(input.absoluteVelocity);
                this.absVelocityButton.setSelected(true);
            } else {
                this.absVelocityButton.setSelected(false);
            }

            this.absVelocityChange.setValue(input.absoluteVelocityChange);
            this.absVelocityChangeButton.setSelected(input.absoluteVelocityChange != 0.0);

            this.detuneCents.setValue(input.detuneCents);
            this.detuneCentsButton.setSelected(input.detuneCents != 0.0);

            this.detuneHz.setValue(input.detuneHz);
            this.detuneHzButton.setSelected(input.detuneHz != 0.0);

            this.id.setText(input.xmlId);
        }

        this.fullNameRefUpdate(Mpm.ARTICULATION_STYLE);
        this.fillNoteIdChooser();

        this.nameRef.selectAll();

        this.setVisible(true);  // start the dialog

        // after the dialog closed do the following

        if (!this.isOk())       // if dialog was canceled
            return input;       // return the input unchanged

        String id = this.id.getText();
        if (id.isEmpty())
            id = null;

        ArticulationData output = new ArticulationData();

        output.date = Tools.round((double) this.date.getValue(), 10);

        if (!this.nameRef.getText().isEmpty())
            output.articulationDefName = this.nameRef.getText();

        if (!this.noteId.getText().isEmpty())
            output.noteid = "#" + this.noteId.getText();

        if (this.absDuration.isEnabled())
            output.absoluteDuration = Tools.round((double) this.absDuration.getValue(), 10);

        if (this.relDuration.isEnabled())
            output.relativeDuration = Tools.round((double) this.relDuration.getValue(), 10);

        if (this.absDurationChange.isEnabled())
            output.absoluteDurationChange = Tools.round((double) this.absDurationChange.getValue(), 10);

        if (this.absDurationMs.isEnabled())
            output.absoluteDurationMs = Tools.round((double) this.absDurationMs.getValue(), 10);

        if (this.absDurationChangeMs.isEnabled())
            output.absoluteDurationChangeMs = Tools.round((double) this.absDurationChangeMs.getValue(), 10);

        if (this.absDelay.isEnabled())
            output.absoluteDelay = Tools.round((double) this.absDelay.getValue(), 10);

        if (this.absDelayMs.isEnabled())
            output.absoluteDelayMs = Tools.round((double) this.absDelayMs.getValue(), 10);

        if (this.absVelocity.isEnabled())
            output.absoluteVelocity = Tools.round((double) this.absVelocity.getValue(), 10);

        if (this.relVelocity.isEnabled())
            output.relativeVelocity = Tools.round((double) this.relVelocity.getValue(), 10);

        if (this.absVelocityChange.isEnabled())
            output.absoluteVelocityChange = Tools.round((double) this.absVelocityChange.getValue(), 10);

        if (this.detuneCents.isEnabled())
            output.detuneCents = Tools.round((double) this.detuneCents.getValue(), 10);

        if (this.detuneHz.isEnabled())
            output.detuneHz = Tools.round((double) this.detuneHz.getValue(), 10);

        output.xmlId = id;

        return output;
    }

    /**
     * Match the value of noteId with the available noteIds at the current date and color the text accordingly.
     */
    private void checkNoteId() {
        this.noteId.setForeground((this.noteIds.contains(this.noteId.getText())) ? Settings.foregroundColor : Settings.errorColor);
        this.noteId.revalidate();
        this.noteId.repaint();
    }

    /**
     * This method finds the MSM notes at the current date and sets the noteIdChooser accordingly.
     */
    private void fillNoteIdChooser() {
        if ((this.map == null) || (this.date == null))
            return;

        ArrayList<Element> scores = this.getMsmScores();
        if (scores.isEmpty())
            return;

        double date = Tools.round((double) this.date.getValue(), 10);
        this.noteIds.clear();

        for (Element score : scores) {
            for (Element note : score.getChildElements("note")) {
                double noteDate = Double.parseDouble(note.getAttributeValue("date"));
                if (noteDate < date)
                    continue;
                if (noteDate > date)
                    break;
                if ((noteDate == date) && (note.getAttribute("id", "http://www.w3.org/XML/1998/namespace") != null))    // if the note has no ID we cannot refer it
                    this.noteIds.add(note.getAttributeValue("id", "http://www.w3.org/XML/1998/namespace"));
            }
        }

        if (this.noteIds.isEmpty()) {
            this.noteIdChooser.setPopupMenu(null);
            this.noteIdChooser.setEnabled(false);
            this.noteIdChooser.setToolTip("No notes (with IDs) at the specified date.");
            return;
        }

        WebPopupMenu noteIdsPopup = new WebPopupMenu();
        for (String id : this.noteIds) {
            WebMenuItem item = new WebMenuItem(id);
            item.addActionListener(menuItemActionEvent -> this.noteId.setText(item.getText()));
            noteIdsPopup.add(item);
        }
        this.noteIdChooser.setPopupMenu(noteIdsPopup);
        this.noteIdChooser.setEnabled(true);
        this.noteIdChooser.removeToolTips();
    }

    /**
     * This method finds the parts that this' map applies to and collects their score elements. Be sure that this.map != null.
     * @return a list of MSM score elements
     */
    private ArrayList<Element> getMsmScores() {
        if (this.msm == null)
            return new ArrayList<>();

        ArrayList<Element> result = new ArrayList<>();

        if (this.map.getLocalHeader() != null) {    // if we are in a local map we need to find the corresponding MSM part
            Element mpmPart = (Element) this.map.getLocalHeader().getXml().getParent();

            String number = Helper.getAttributeValue("number", mpmPart);
            for (Element part : this.msm.getParts()) {
                if (Helper.getAttributeValue("number", part).equals(number)
                        && (part.getFirstChildElement("dated") != null)
                        && (part.getFirstChildElement("dated").getFirstChildElement("score") != null)){
                    result.add(part.getFirstChildElement("dated").getFirstChildElement("score"));
                    return result;
                }
            }
            String name = Helper.getAttributeValue("name", mpmPart);
            for (Element part : this.msm.getParts()) {
                if (Helper.getAttributeValue("name", part).equals(name)
                        && (part.getFirstChildElement("dated") != null)
                        && (part.getFirstChildElement("dated").getFirstChildElement("score") != null)){
                    result.add(part.getFirstChildElement("dated").getFirstChildElement("score"));
                    return result;
                }
            }
            String midiChannel = Helper.getAttributeValue("midi.channel", mpmPart);
            String midiPort = Helper.getAttributeValue("midi.port", mpmPart);
            for (Element part : this.msm.getParts()) {
                if (Helper.getAttributeValue("midi.channel", part).equals(midiChannel)
                        && Helper.getAttributeValue("midi.port", part).equals(midiPort)
                        && (part.getFirstChildElement("dated") != null)
                        && (part.getFirstChildElement("dated").getFirstChildElement("score") != null)){
                    result.add(part.getFirstChildElement("dated").getFirstChildElement("score"));
                    return result;
                }
            }
            return result;
        }

        // we are in a global map, so we return all parts
        for (Element part : this.msm.getParts()) {
            if ((part.getFirstChildElement("dated") != null) && (part.getFirstChildElement("dated").getFirstChildElement("score") != null)) // in this context we are interested only in parts with scores
                result.add(part.getFirstChildElement("dated").getFirstChildElement("score"));
        }
        return result;
    }

}
