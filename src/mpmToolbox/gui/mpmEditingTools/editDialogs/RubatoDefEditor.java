package mpmToolbox.gui.mpmEditingTools.editDialogs;

import com.alee.laf.label.WebLabel;
import com.alee.laf.panel.WebPanel;
import com.alee.laf.spinner.WebSpinner;
import com.alee.laf.text.WebTextField;
import meico.mpm.elements.styles.RubatoStyle;
import meico.mpm.elements.styles.defs.RubatoDef;
import mpmToolbox.gui.Settings;
import mpmToolbox.gui.mpmEditingTools.editDialogs.visualizers.RubatoVisualizer;
import mpmToolbox.supplementary.Tools;
import mpmToolbox.supplementary.rangeSlider.RangeSlider;

import javax.swing.*;
import java.awt.*;
import java.math.RoundingMode;
import java.util.Hashtable;

/**
 * This class represents the dialog for creating and editing a rubatoDef entry in MPM.
 * @author Axel Berndt
 */
public class RubatoDefEditor extends EditDialog<RubatoDef> {
    private WebTextField name;
    private final RubatoStyle styleDef;
    private RubatoVisualizer visualizer;
    private WebSpinner frameLength;
    private WebSpinner intensity;
    private WebSpinner lateStart;
    private WebSpinner earlyEnd;
    private RangeSlider rangeSlider;

    /**
     * constructor
     * @param styleDef
     */
    public RubatoDefEditor(RubatoStyle styleDef) {
        super("Edit Rubato Definition");
        this.styleDef = styleDef;
    }

    /**
     * the GUI
     */
    @Override
    public void makeContentPanel() {
        WebLabel nameLabel = new WebLabel("Rubato Name:");
        nameLabel.setHorizontalAlignment(WebLabel.RIGHT);
        nameLabel.setPadding(Settings.paddingInDialogs);
        this.addToContentPanel(nameLabel, 0, 0, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH);

        this.name = new WebTextField("rubato name");
        this.name.setHorizontalAlignment(WebTextField.LEFT);
        this.name.setPadding(Settings.paddingInDialogs);
        this.addToContentPanel(this.name, 1, 0, 3, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH);

        /////////////

        WebLabel visualizerLabel = new WebLabel("Rubato Timing Preview:");
        visualizerLabel.setHorizontalAlignment(WebLabel.RIGHT);
        visualizerLabel.setPadding(Settings.paddingInDialogs);
        this.addToContentPanel(visualizerLabel, 0, 1, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH);

        this.visualizer = new RubatoVisualizer(1.0, 0.0, 1.0);
        this.addToContentPanel(this.visualizer, 1, 1, 3, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH);

        /////////////

        WebLabel frameLengthLabel = new WebLabel("Frame Length:");
        frameLengthLabel.setHorizontalAlignment(WebLabel.RIGHT);
        frameLengthLabel.setPadding(Settings.paddingInDialogs);
        this.addToContentPanel(frameLengthLabel, 0, 2, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH);

        this.frameLength = new WebSpinner(new SpinnerNumberModel(1.0, 0.01, Double.POSITIVE_INFINITY, 1.0));
        int width = getFontMetrics(this.frameLength.getFont()).stringWidth("999.999.999.999");
        this.frameLength.setMinimumWidth(width);
        this.frameLength.setMaximumWidth(width);
        this.addToContentPanel(this.frameLength, 1, 2, 1, 1, 0.03, 1.0, 5, 5, GridBagConstraints.BOTH);

        WebLabel frameLengthExplanationLabel = new WebLabel("ticks");
        frameLengthExplanationLabel.setHorizontalAlignment(WebLabel.LEFT);
        frameLengthExplanationLabel.setPadding(Settings.paddingInDialogs);
        this.addToContentPanel(frameLengthExplanationLabel, 2, 2, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.HORIZONTAL);

        //////////////

        WebLabel intensityLabel = new WebLabel("Rubato Intensity:");
        intensityLabel.setHorizontalAlignment(WebLabel.RIGHT);
        intensityLabel.setPadding(Settings.paddingInDialogs);
        this.addToContentPanel(intensityLabel, 0, 3, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH);

        this.intensity = new WebSpinner(new SpinnerNumberModel(1.0, 0.0000000001, Double.POSITIVE_INFINITY, 0.01));
        JSpinner.NumberEditor intensityEditor = (JSpinner.NumberEditor) this.intensity.getEditor(); // https://stackoverflow.com/questions/34627998/jspinner-number-editor
        intensityEditor.getFormat().setMaximumFractionDigits(10);
        intensityEditor.getFormat().setRoundingMode(RoundingMode.HALF_UP);
        this.intensity.setMinimumWidth(width);
        this.intensity.setMaximumWidth(width);
        this.intensity.addChangeListener(changeEvent -> this.visualizer.setIntensity(Tools.round((double) this.intensity.getValue(), 10)));
        this.addToContentPanel(this.intensity, 1, 3, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH);

        WebLabel intensityExplanationLabel = new WebLabel("intensity of timing distortion (1.0 is even timing)");
        intensityExplanationLabel.setHorizontalAlignment(WebLabel.LEFT);
        intensityExplanationLabel.setPadding(Settings.paddingInDialogs);
        this.addToContentPanel(intensityExplanationLabel, 2, 3, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.HORIZONTAL);

        //////////////

        this.lateStart = new WebSpinner(new SpinnerNumberModel(0.0, 0.0, 0.9999999999, 0.01));
        this.lateStart.addChangeListener(changeEvent -> {        // make sure that late start won't collide with early end
            if (((double) this.lateStart.getValue()) >= ((double) this.earlyEnd.getValue()))
                this.lateStart.setValue(((double) this.earlyEnd.getValue()) - 0.0000000001);
            this.rangeSlider.setValue((int) (Tools.round((double) this.lateStart.getValue(), 10) * 1000));
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
            this.visualizer.setEarlyEnd(Tools.round((double) this.earlyEnd.getValue(), 10));
        });
        JSpinner.NumberEditor earlyEndEditor = (JSpinner.NumberEditor) this.earlyEnd.getEditor();
        earlyEndEditor.getFormat().setMaximumFractionDigits(10);
        earlyEndEditor.getFormat().setRoundingMode(RoundingMode.HALF_UP);
        this.earlyEnd.setMinimumWidth(width);
        this.earlyEnd.setMaximumWidth(width);

        WebLabel lateStartEarlyEndLabel = new WebLabel("Late Start, Early End:");
        lateStartEarlyEndLabel.setHorizontalAlignment(WebLabel.RIGHT);
        lateStartEarlyEndLabel.setPadding(Settings.paddingInDialogs);
        this.addToContentPanel(lateStartEarlyEndLabel, 0, 4, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH);

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
        this.addToContentPanel(lateStartEarlyEndPanel, 1, 4, 3, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH);

        /////////////

        this.addIdInput(5);
    }

    /**
     * Open the def editing dialog.
     * @param def the def to be edited or null if a new one should be created
     * @return the def or null
     */
    @Override
    public RubatoDef edit(RubatoDef def) {
        if (def != null) {
            this.name.setText(def.getName());
            this.frameLength.setValue(def.getFrameLength());
            this.intensity.setValue(def.getIntensity());
            this.lateStart.setValue(def.getLateStart());
            this.earlyEnd.setValue(def.getEarlyEnd());
            this.id.setText(def.getId());
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
            def = RubatoDef.createRubatoDef(this.name.getText(), Tools.round((double) this.frameLength.getValue(), 10));
        else {
            this.styleDef.removeDef(def.getName());
            def = RubatoDef.createRubatoDef(this.name.getText(), Tools.round((double) this.frameLength.getValue(), 10));
            this.styleDef.addDef(def);
        }

        def.setIntensity(Tools.round((double) this.intensity.getValue(), 10));
        def.setLateStart(Tools.round((double) this.lateStart.getValue(), 10));
        def.setEarlyEnd(Tools.round((double) this.earlyEnd.getValue(), 10));
        def.setId(id);

        return def;
    }
}
