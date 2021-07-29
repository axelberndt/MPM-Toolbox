package mpmToolbox.gui.mpmEditingTools.editDialogs;

import com.alee.laf.button.WebButton;
import com.alee.laf.checkbox.WebCheckBox;
import com.alee.laf.label.WebLabel;
import com.alee.laf.panel.WebPanel;
import com.alee.laf.spinner.WebSpinner;
import meico.mpm.Mpm;
import meico.mpm.elements.maps.RubatoMap;
import meico.mpm.elements.maps.data.RubatoData;
import meico.mpm.elements.styles.defs.RubatoDef;
import mpmToolbox.gui.Settings;
import mpmToolbox.gui.mpmEditingTools.editDialogs.visualizers.RubatoVisualizer;
import mpmToolbox.gui.mpmEditingTools.editDialogs.supplementary.EditDialogToggleButton;
import mpmToolbox.supplementary.Tools;
import mpmToolbox.supplementary.rangeSlider.RangeSlider;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.math.RoundingMode;
import java.util.Hashtable;

/**
 * The rubato editor.
 * @author Axel Berndt
 */
public class RubatoEditor extends EditDialog<RubatoData> {
    private RubatoVisualizer visualizer;

    private WebSpinner frameLength;
    private EditDialogToggleButton frameLengthButton;

    private WebSpinner intensity;
    private EditDialogToggleButton intensityButton;

    private WebSpinner lateStart;
    private EditDialogToggleButton lateStartButton;

    private WebSpinner earlyEnd;
    private EditDialogToggleButton earlyEndButton;

    private RangeSlider rangeSlider;

    private WebCheckBox loop;

    /**
     * constructor
     * @param map the map that gets or holds the rubato element
     */
    public RubatoEditor(RubatoMap map) {
        super("Edit Rubato", map);
    }

    /**
     * GUI
     */
    @Override
    public void makeContentPanel() {
        this.addDateInput(0);
        this.date.addChangeListener(changeEvent -> this.fullNameRefUpdate(Mpm.RUBATO_STYLE));

        this.addNameRef("Predefined Rubato (optional):", 1, true);
        this.nameRef.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                displayNameRef();
            }
            @Override
            public void removeUpdate(DocumentEvent e) {
                displayNameRef();
            }
            @Override
            public void changedUpdate(DocumentEvent e) {
                displayNameRef();
            }
        });

        /////////////

        WebLabel visualizerLabel = new WebLabel("Rubato Timing Preview:");
        visualizerLabel.setHorizontalAlignment(WebLabel.RIGHT);
        visualizerLabel.setPadding(Settings.paddingInDialogs);
        this.addToContentPanel(visualizerLabel, 0, 2, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH);

        this.visualizer = new RubatoVisualizer(1.0, 0.0, 1.0);
        this.addToContentPanel(this.visualizer, 1, 2, 3, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH);

        /////////////

        WebLabel explainLabel = new WebLabel("Below attributes can be used instead of or in addition to the above referred predefined rubato.");
        explainLabel.setHorizontalAlignment(WebLabel.LEFT);
        explainLabel.setPadding(Settings.paddingInDialogs);
        this.addToContentPanel(explainLabel, 0, 3, 4, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH);

        /////////////

        this.frameLength = new WebSpinner(new SpinnerNumberModel(1.0, 0.01, Double.POSITIVE_INFINITY, 1.0));
        int width = getFontMetrics(this.frameLength.getFont()).stringWidth("999.999.999.999");
        this.frameLength.setMinimumWidth(width);
        this.frameLength.setMaximumWidth(width);
        this.addToContentPanel(this.frameLength, 1, 4, 1, 1, 0.03, 1.0, 5, 5, GridBagConstraints.BOTH);

        WebLabel frameLengthExplanationLabel = new WebLabel("ticks");
        frameLengthExplanationLabel.setHorizontalAlignment(WebLabel.LEFT);
        frameLengthExplanationLabel.setPadding(Settings.paddingInDialogs);
        this.addToContentPanel(frameLengthExplanationLabel, 2, 4, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.HORIZONTAL);

        this.frameLengthButton = new EditDialogToggleButton("Frame Length:", new JComponent[]{this.frameLength, frameLengthExplanationLabel}, false);
        this.addToContentPanel(this.frameLengthButton, 0, 4, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH);

        /////////////

        this.intensity = new WebSpinner(new SpinnerNumberModel(1.0, 0.0000000001, Double.POSITIVE_INFINITY, 0.01));
        JSpinner.NumberEditor intensityEditor = (JSpinner.NumberEditor) this.intensity.getEditor(); // https://stackoverflow.com/questions/34627998/jspinner-number-editor
        intensityEditor.getFormat().setMaximumFractionDigits(10);
        intensityEditor.getFormat().setRoundingMode(RoundingMode.HALF_UP);
        this.intensity.setMinimumWidth(width);
        this.intensity.setMaximumWidth(width);
        this.intensity.addChangeListener(changeEvent -> this.visualizer.setIntensity(Tools.round((double) this.intensity.getValue(), 10)));
        this.addToContentPanel(this.intensity, 1, 5, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH);

        WebLabel intensityExplanationLabel = new WebLabel("intensity of timing distortion (1.0 is even timing)");
        intensityExplanationLabel.setHorizontalAlignment(WebLabel.LEFT);
        intensityExplanationLabel.setPadding(Settings.paddingInDialogs);
        this.addToContentPanel(intensityExplanationLabel, 2, 5, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.HORIZONTAL);

        this.intensityButton = new EditDialogToggleButton("Rubato Intensity:", new JComponent[]{this.intensity, intensityExplanationLabel}, false);
        this.intensityButton.addActionListener(actionEvent -> this.displayNameRef());
        this.addToContentPanel(this.intensityButton, 0, 5, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH);

        /////////////

        this.lateStart = new WebSpinner(new SpinnerNumberModel(0.0, 0.0, 0.9999999999, 0.01));
        this.lateStart.addChangeListener(changeEvent -> {        // make sure that late start won't collide with early end
            if (((double) this.lateStart.getValue()) >= ((double) this.earlyEnd.getValue()))
                this.lateStart.setValue(((double) this.earlyEnd.getValue()) - 0.0000000001);
            this.rangeSlider.setValue((int) (Tools.round((double) this.lateStart.getValue(), 10) * 1000));
            if (this.lateStart.isEnabled())
                this.visualizer.setLateStart(Tools.round((double) this.lateStart.getValue(), 10));
        });
        JSpinner.NumberEditor lateStartEditor = (JSpinner.NumberEditor) this.lateStart.getEditor();
        lateStartEditor.getFormat().setMaximumFractionDigits(10);
        lateStartEditor.getFormat().setRoundingMode(RoundingMode.HALF_UP);
        this.lateStart.setMinimumWidth(width);
        this.lateStart.setMaximumWidth(width);

        this.earlyEnd = new WebSpinner(new SpinnerNumberModel(1.0, 0.0000000001, 1.0, 0.01));
        this.earlyEnd.addChangeListener(changeEvent -> {        // make sure that early end won't collide with late start
            if (((double) this.earlyEnd.getValue()) <= ((double) this.lateStart.getValue()))
                this.earlyEnd.setValue(((double) this.lateStart.getValue()) + 0.0000000001);
            this.rangeSlider.setUpperValue((int) (Tools.round((double) this.earlyEnd.getValue(), 10) * 1000));
            if (this.earlyEnd.isEnabled())
                this.visualizer.setEarlyEnd(Tools.round((double) this.earlyEnd.getValue(), 10));
        });
        JSpinner.NumberEditor earlyEndEditor = (JSpinner.NumberEditor) this.earlyEnd.getEditor();
        earlyEndEditor.getFormat().setMaximumFractionDigits(10);
        earlyEndEditor.getFormat().setRoundingMode(RoundingMode.HALF_UP);
        this.earlyEnd.setMinimumWidth(width);
        this.earlyEnd.setMaximumWidth(width);

        this.lateStartButton = new EditDialogToggleButton("Late Start", new JComponent[]{this.lateStart}, false);
        this.lateStartButton.setHorizontalAlignment(WebButton.CENTER);
        this.lateStartButton.addActionListener(actionEvent -> this.displayNameRef());
        this.earlyEndButton = new EditDialogToggleButton("Early End", new JComponent[]{this.earlyEnd}, false);
        this.earlyEndButton.setHorizontalAlignment(WebButton.CENTER);
        this.earlyEndButton.addActionListener(actionEvent -> this.displayNameRef());
        WebPanel lateStartEarlyEndButtonPanel = new WebPanel(new GridBagLayout());
        Tools.addComponentToGridBagLayout(lateStartEarlyEndButtonPanel, (GridBagLayout) lateStartEarlyEndButtonPanel.getLayout(), this.lateStartButton, 0, 0, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH, GridBagConstraints.LINE_START);
        Tools.addComponentToGridBagLayout(lateStartEarlyEndButtonPanel, (GridBagLayout) lateStartEarlyEndButtonPanel.getLayout(), this.earlyEndButton, 1, 0, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH, GridBagConstraints.LINE_START);
        this.addToContentPanel(lateStartEarlyEndButtonPanel, 0, 6, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH);

        // implement range slider: https://ernienotes.wordpress.com/2010/12/27/creating-a-java-swing-range-slider/
        this.rangeSlider = new RangeSlider(0, 1000);
        this.rangeSlider.setValue(0);
        this.rangeSlider.setUpperValue(1000);
        this.rangeSlider.setOrientation(JSlider.HORIZONTAL);
//        this.rangeSlider.setBackground(this.getBackground());
        this.rangeSlider.setOpaque(false);
        this.rangeSlider.setColors(this.rangeSlider.getForeground(), Color.GRAY, Color.GRAY);
        this.rangeSlider.setMajorTickSpacing(500);
        this.rangeSlider.setMinorTickSpacing(250);
        this.rangeSlider.setPaintTicks(true);
        Hashtable<Integer, WebLabel> rangeSliderLabels = new Hashtable<>();
        rangeSliderLabels.put(0, new WebLabel("0"));
        rangeSliderLabels.put(500, new WebLabel("0.5"));
        rangeSliderLabels.put(1000, new WebLabel("1"));
        this.rangeSlider.setLabelTable(rangeSliderLabels);
        this.rangeSlider.setPaintLabels(true);
//        this.rangeSlider.setPreferredSize(new Dimension(240, this.rangeSlider.getPreferredSize().height));
//        this.rangeSlider.setMinimum(0);
//        this.rangeSlider.setMaximum(1000);
        this.rangeSlider.addChangeListener(e -> {
            RangeSlider slider = (RangeSlider) e.getSource();
            this.lateStart.setValue(((double) slider.getValue()) / 1000.0);
            this.earlyEnd.setValue(((double) slider.getUpperValue()) / 1000.0);
        });

        WebPanel lateStartEarlyEndPanel = new WebPanel(new GridBagLayout());
        Tools.addComponentToGridBagLayout(lateStartEarlyEndPanel, (GridBagLayout) lateStartEarlyEndPanel.getLayout(), this.lateStart, 0, 0, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH, GridBagConstraints.LINE_START);
        Tools.addComponentToGridBagLayout(lateStartEarlyEndPanel, (GridBagLayout) lateStartEarlyEndPanel.getLayout(), this.rangeSlider, 1, 0, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH, GridBagConstraints.LINE_START);
        Tools.addComponentToGridBagLayout(lateStartEarlyEndPanel, (GridBagLayout) lateStartEarlyEndPanel.getLayout(), this.earlyEnd, 2, 0, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH, GridBagConstraints.LINE_START);
        this.addToContentPanel(lateStartEarlyEndPanel, 1, 6, 3, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH);

        /////////////

        WebLabel loopLabel = new WebLabel("Loop:");
        loopLabel.setHorizontalAlignment(WebLabel.RIGHT);
        loopLabel.setPadding(Settings.paddingInDialogs);
        this.addToContentPanel(loopLabel, 0, 7, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH);

        this.loop = new WebCheckBox(false);
        this.loop.setToolTip("The rubato repeats continuously.");
        this.addToContentPanel(this.loop, 1, 7, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH);

        /////////////

        this.addIdInput(8);
    }

    /**
     * execute the editor dialog
     * @param input the object to be edited via the dialog or null to create a new one
     * @return the rubato data or null
     */
    @Override
    public RubatoData edit(RubatoData input) {
        if (input != null) {
            this.date.setValue(input.startDate);
            this.nameRef.setText(input.rubatoDefString);
            this.loop.setSelected(input.loop);

            if (input.intensity != null) {
                this.intensity.setValue(input.intensity);
                this.intensityButton.setSelected(true);
            }

            if (input.frameLength != null) {
                this.frameLength.setValue(input.frameLength);
                this.frameLengthButton.setSelected(true);
            }

            if (input.lateStart != null) {
                this.lateStart.setValue(input.lateStart);
                this.lateStartButton.setSelected(true);
            }

            if (input.earlyEnd != null) {
                this.earlyEnd.setValue(input.earlyEnd);
                this.earlyEndButton.setSelected(true);
            }

            this.id.setText(input.xmlId);
        }

        this.fullNameRefUpdate(Mpm.RUBATO_STYLE);
        this.displayNameRef();

        this.nameRef.selectAll();

        this.setVisible(true);      // start the dialog

        // after the dialog closed do the following

        if (!this.isOk())           // if input was canceled
            return input;

        String id = this.id.getText();
        if (id.isEmpty())
            id = null;

        RubatoData output = new RubatoData();
        output.startDate = Tools.round((double) this.date.getValue(), 10);

        if (!this.nameRef.getText().isEmpty())
            output.rubatoDefString = this.nameRef.getText();

        output.frameLength = (this.frameLength.isEnabled()) ? Tools.round((double) this.frameLength.getValue(), 10) : null;
        output.intensity = (this.intensity.isEnabled()) ? Tools.round((double) this.intensity.getValue(), 10) : null;

        output.lateStart = (this.lateStart.isEnabled()) ? Tools.round((double) this.lateStart.getValue(), 10) : null;
        output.earlyEnd = (this.earlyEnd.isEnabled()) ? Tools.round((double) this.earlyEnd.getValue(), 10) : null;

        output.loop = this.loop.isSelected();
        output.xmlId = id;

        return output;
    }

    /**
     * this updates the visualization according to the underlying rubatoDef
     */
    private void displayNameRef() {
        if (this.style == null)
            return;

        double intensity = 1.0;
        double lateStart = 0.0;
        double earlyEnd = 1.0;

        RubatoDef def = (RubatoDef) this.style.getDef(this.nameRef.getText());
        if (def != null) {
            intensity = def.getIntensity();
            lateStart = def.getLateStart();
            earlyEnd = def.getEarlyEnd();
        }

        if (this.intensityButton.isSelected())
            intensity = Tools.round((double) this.intensity.getValue(), 10);

        if (this.lateStartButton.isSelected())
            lateStart = Tools.round((double) this.lateStart.getValue(), 10);

        if (this.earlyEndButton.isSelected())
            earlyEnd = Tools.round((double) this.earlyEnd.getValue(), 10);

        this.visualizer.setAll(intensity, lateStart, earlyEnd);
    }
}
