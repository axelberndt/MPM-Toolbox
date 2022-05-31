package mpmToolbox.gui.mpmEditingTools.editDialogs;

import com.alee.laf.button.WebButton;
import com.alee.laf.combobox.WebComboBox;
import com.alee.laf.label.WebLabel;
import com.alee.laf.list.WebList;
import com.alee.laf.list.editor.ListEditListener;
import com.alee.laf.list.editor.TextListCellEditor;
import com.alee.laf.panel.WebPanel;
import com.alee.laf.scroll.WebScrollPane;
import com.alee.laf.spinner.WebSpinner;
import com.alee.managers.style.StyleId;
import meico.mpm.elements.maps.data.DistributionData;
import mpmToolbox.gui.Settings;
import mpmToolbox.gui.mpmEditingTools.editDialogs.supplementary.EditDialogToggleButton;
import mpmToolbox.gui.mpmEditingTools.editDialogs.visualizers.DistributionVisualizer;
import mpmToolbox.supplementary.Tools;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DragSource;
import java.io.IOException;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Objects;

/**
 * The editor dialog for MPM distribution elements.
 * @author Axel Berndt
 */
public class DistributionEditor extends EditDialog<DistributionData> {
    private static final String UNIFORM = "Uniform Distribution";
    private static final String GAUSSIAN = "Gaussian Distribution";
    private static final String TRIANGULAR = "Triangular Distribution";
    private static final String BROWNIAN = "Brownian Noise";
    private static final String COMPENSATING_TRIANGLE = "Compensating Triangle Distribution";
    private static final String LIST = "Distribution List";

    private WebComboBox type;

    private WebLabel lowerLimitLabel;
    private WebSpinner lowerLimit;

    private WebLabel upperLimitLabel;
    private WebSpinner upperLimit;

    private WebLabel lowerClipLabel;
    private WebSpinner lowerClip;

    private WebLabel upperClipLabel;
    private WebSpinner upperClip;

    private WebLabel standardDeviationLabel;
    private WebSpinner standardDeviation;

    private WebLabel modeLabel;
    private WebSpinner mode;
    private WebLabel modeExplanation;

    private WebLabel maxStepWidthLabel;
    private WebSpinner maxStepWidth;

    private WebLabel degreeOfCorrelationLabel;
    private WebSpinner degreeOfCorrelation;

    private WebLabel timingBasisLabel;
    private WebSpinner timingBasis;
    private WebLabel timingBasisExplanation;

    private WebLabel listLabel;
    private WebList list;
    private WebButton plus, minus;
    private DefaultListModel<Double> listModel;

    private WebSpinner seed;
    private EditDialogToggleButton seedButton;

    private DistributionVisualizer visualizer;

    /**
     * constructor
     */
    public DistributionEditor() {
        super("Edit Distribution");
    }

    /**
     * GUI
     */
    @Override
    public void makeContentPanel() {
        this.addDateInput(0);

        ////////////////

        WebLabel typeLabel = new WebLabel("Distribution Type:");
        typeLabel.setHorizontalAlignment(WebLabel.RIGHT);
        typeLabel.setPadding(Settings.paddingInDialogs);
        this.addToContentPanel(typeLabel, 0, 1, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH);

        this.type = new WebComboBox(new String[]{DistributionEditor.UNIFORM, DistributionEditor.GAUSSIAN, DistributionEditor.TRIANGULAR, DistributionEditor.BROWNIAN, DistributionEditor.COMPENSATING_TRIANGLE, DistributionEditor.LIST});
        this.type.addActionListener(actionEvent -> {
            if (this.type.getSelectedItem() != null)
                this.switchDistributionTypeTo((String) this.type.getSelectedItem());
        });
        this.type.setToolTip("Choose the distribution type.");
        this.addToContentPanel(this.type, 1, 1, 2, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH);

        ////////////////

        WebLabel visualizerLabel = new WebLabel("Distribution Preview:");
        visualizerLabel.setHorizontalAlignment(WebLabel.RIGHT);
        visualizerLabel.setPadding(Settings.paddingInDialogs);
        this.addToContentPanel(visualizerLabel, 0, 2, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH);

        this.visualizer = new DistributionVisualizer();
        this.addToContentPanel(this.visualizer, 1, 2, 3, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH);

        ////////////////

        this.lowerLimitLabel = new WebLabel("Lower Limit:");
        this.lowerLimitLabel.setHorizontalAlignment(WebLabel.RIGHT);
        this.lowerLimitLabel.setPadding(Settings.paddingInDialogs);
        this.addToContentPanel(this.lowerLimitLabel, 0, 3, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH);

        this.lowerLimit = new WebSpinner(new SpinnerNumberModel(0.0, -99999999999999999.9, 99999999999999999.9, 1.0));
        int width = getFontMetrics(this.lowerLimit.getFont()).stringWidth("999.999.999.999.999");
        JSpinner.NumberEditor lowerLimitEditor = (JSpinner.NumberEditor) this.lowerLimit.getEditor();
        lowerLimitEditor.getFormat().setMaximumFractionDigits(10);
        lowerLimitEditor.getFormat().setRoundingMode(RoundingMode.HALF_UP);
        this.lowerLimit.setMinimumWidth(width);
        this.lowerLimit.setMaximumWidth(width);
        this.lowerLimit.addChangeListener(changeEvent -> {  // make sure the limits don't invert
            if (((double) this.lowerLimit.getValue()) > ((double) this.upperLimit.getValue()))
                this.lowerLimit.setValue(this.upperLimit.getValue());
            this.updateVisualizer();
        });
        this.addToContentPanel(this.lowerLimit, 1, 3, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH);

        ////////////////

        this.upperLimitLabel = new WebLabel("Upper Limit");
        this.upperLimitLabel.setHorizontalAlignment(WebLabel.LEFT);
        this.upperLimitLabel.setPadding(Settings.paddingInDialogs);
        this.addToContentPanel(this.upperLimitLabel, 3, 3, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH);

        this.upperLimit = new WebSpinner(new SpinnerNumberModel(0.0, -99999999999999999.9, 99999999999999999.9, 1.0));
        width = getFontMetrics(this.upperLimit.getFont()).stringWidth("999.999.999.999.999");
        JSpinner.NumberEditor upperLimitEditor = (JSpinner.NumberEditor) this.upperLimit.getEditor();
        upperLimitEditor.getFormat().setMaximumFractionDigits(10);
        upperLimitEditor.getFormat().setRoundingMode(RoundingMode.HALF_UP);
        this.upperLimit.setMinimumWidth(width);
        this.upperLimit.setMaximumWidth(width);
        this.upperLimit.addChangeListener(changeEvent -> {  // make sure the limits don't invert
            if (((double) this.lowerLimit.getValue()) > ((double) this.upperLimit.getValue()))
                this.upperLimit.setValue(this.lowerLimit.getValue());
            this.updateVisualizer();
        });
        this.addToContentPanel(this.upperLimit, 2, 3, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH);

        ////////////////

        this.lowerClipLabel = new WebLabel("Lower Clipping Border:");
        this.lowerClipLabel.setHorizontalAlignment(WebLabel.RIGHT);
        this.lowerClipLabel.setPadding(Settings.paddingInDialogs);
        this.addToContentPanel(this.lowerClipLabel, 0, 4, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH);

        this.lowerClip = new WebSpinner(new SpinnerNumberModel(0.0, -99999999999999999.9, 99999999999999999.9, 1.0));
        width = getFontMetrics(this.lowerClip.getFont()).stringWidth("999.999.999.999.999");
        JSpinner.NumberEditor lowerClipEditor = (JSpinner.NumberEditor) this.lowerClip.getEditor();
        lowerClipEditor.getFormat().setMaximumFractionDigits(10);
        lowerClipEditor.getFormat().setRoundingMode(RoundingMode.HALF_UP);
        this.lowerClip.setMinimumWidth(width);
        this.lowerClip.setMaximumWidth(width);
        this.lowerClip.addChangeListener(changeEvent -> {  // make sure the clippings don't invert
            if (((double) this.lowerClip.getValue()) > ((double) this.upperClip.getValue()))
                this.lowerClip.setValue(this.upperClip.getValue());
            this.updateVisualizer();
        });
        this.addToContentPanel(this.lowerClip, 1, 4, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH);

        ////////////////

        this.upperClipLabel = new WebLabel("Upper Clipping Border");
        this.upperClipLabel.setHorizontalAlignment(WebLabel.LEFT);
        this.upperClipLabel.setPadding(Settings.paddingInDialogs);
        this.addToContentPanel(this.upperClipLabel, 3, 4, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH);

        this.upperClip = new WebSpinner(new SpinnerNumberModel(0.0, -99999999999999999.9, 99999999999999999.9, 1.0));
        width = getFontMetrics(this.upperClip.getFont()).stringWidth("999.999.999.999.999");
        JSpinner.NumberEditor upperClipEditor = (JSpinner.NumberEditor) this.upperClip.getEditor();
        upperClipEditor.getFormat().setMaximumFractionDigits(10);
        upperClipEditor.getFormat().setRoundingMode(RoundingMode.HALF_UP);
        this.upperClip.setMinimumWidth(width);
        this.upperClip.setMaximumWidth(width);
        this.upperClip.addChangeListener(changeEvent -> {  // make sure the clippings don't invert
            if (((double) this.lowerClip.getValue()) > ((double) this.upperClip.getValue()))
                this.upperClip.setValue(this.lowerClip.getValue());
            this.updateVisualizer();
        });
        this.addToContentPanel(this.upperClip, 2, 4, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH);

        ////////////////

        this.standardDeviationLabel = new WebLabel("Standard Deviation:");
        this.standardDeviationLabel.setHorizontalAlignment(WebLabel.RIGHT);
        this.standardDeviationLabel.setPadding(Settings.paddingInDialogs);
        this.addToContentPanel(this.standardDeviationLabel, 0, 5, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH);

        this.standardDeviation = new WebSpinner(new SpinnerNumberModel(0.0, -99999999999999999.9, 99999999999999999.9, 0.1));
        width = getFontMetrics(this.standardDeviation.getFont()).stringWidth("999.999.999.999.999");
        JSpinner.NumberEditor standardDeviationEditor = (JSpinner.NumberEditor) this.standardDeviation.getEditor();
        standardDeviationEditor.getFormat().setMaximumFractionDigits(10);
        standardDeviationEditor.getFormat().setRoundingMode(RoundingMode.HALF_UP);
        this.standardDeviation.setMinimumWidth(width);
        this.standardDeviation.setMaximumWidth(width);
        this.standardDeviation.addChangeListener(changeEvent -> this.updateVisualizer());
        this.addToContentPanel(this.standardDeviation, 1, 5, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH);

        ////////////////

        this.modeLabel = new WebLabel("Mode:");
        this.modeLabel.setHorizontalAlignment(WebLabel.RIGHT);
        this.modeLabel.setPadding(Settings.paddingInDialogs);
        this.addToContentPanel(this.modeLabel, 0, 6, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH);

        this.mode = new WebSpinner(new SpinnerNumberModel(0.0, -99999999999999999.9, 99999999999999999.9, 0.1));
        width = getFontMetrics(this.mode.getFont()).stringWidth("999.999.999.999.999");
        JSpinner.NumberEditor modeEditor = (JSpinner.NumberEditor) this.mode.getEditor();
        modeEditor.getFormat().setMaximumFractionDigits(10);
        modeEditor.getFormat().setRoundingMode(RoundingMode.HALF_UP);
        this.mode.setMinimumWidth(width);
        this.mode.setMaximumWidth(width);
        this.mode.addChangeListener(changeEvent -> {    // make sure that lowerLimit <= mode <= upperLimit
            if (((double) this.mode.getValue()) < ((double) this.lowerLimit.getValue()))
                this.mode.setValue(this.lowerLimit.getValue());
            else if (((double) this.mode.getValue()) > ((double) this.upperLimit.getValue()))
                this.mode.setValue(this.upperLimit.getValue());
            this.updateVisualizer();
        });
        this.addToContentPanel(this.mode, 1, 6, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH);

        this.modeExplanation = new WebLabel("triangle's peak position in [lower limit; upper limit]");
        this.modeExplanation.setHorizontalAlignment(WebLabel.LEFT);
        this.modeExplanation.setPadding(Settings.paddingInDialogs);
        this.addToContentPanel(this.modeExplanation, 2, 6, 2, 1, 1.0, 1.0, 0, 0, GridBagConstraints.HORIZONTAL);

        ////////////////

        this.maxStepWidthLabel = new WebLabel("Max. Step Width:");
        this.maxStepWidthLabel.setHorizontalAlignment(WebLabel.RIGHT);
        this.maxStepWidthLabel.setPadding(Settings.paddingInDialogs);
        this.addToContentPanel(this.maxStepWidthLabel, 0, 7, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH);

        this.maxStepWidth = new WebSpinner(new SpinnerNumberModel(0.0, 0.0, 99999999999999999.9, 0.1));
        width = getFontMetrics(this.maxStepWidth.getFont()).stringWidth("999.999.999.999.999");
        JSpinner.NumberEditor maxStepWidthEditor = (JSpinner.NumberEditor) this.maxStepWidth.getEditor();
        maxStepWidthEditor.getFormat().setMaximumFractionDigits(10);
        maxStepWidthEditor.getFormat().setRoundingMode(RoundingMode.HALF_UP);
        this.maxStepWidth.setMinimumWidth(width);
        this.maxStepWidth.setMaximumWidth(width);
        this.maxStepWidth.addChangeListener(changeEvent -> this.updateVisualizer());
        this.addToContentPanel(this.maxStepWidth, 1, 7, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH);

        ////////////////

        this.degreeOfCorrelationLabel = new WebLabel("Degree of Correlation:");
        this.degreeOfCorrelationLabel.setHorizontalAlignment(WebLabel.RIGHT);
        this.degreeOfCorrelationLabel.setPadding(Settings.paddingInDialogs);
        this.addToContentPanel(this.degreeOfCorrelationLabel, 0, 8, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH);

        this.degreeOfCorrelation = new WebSpinner(new SpinnerNumberModel(0.0, 0.0, 99999999999999999.9, 0.1));
        width = getFontMetrics(this.degreeOfCorrelation.getFont()).stringWidth("999.999.999.999.999");
        JSpinner.NumberEditor degreeOfCorrelationEditor = (JSpinner.NumberEditor) this.degreeOfCorrelation.getEditor();
        degreeOfCorrelationEditor.getFormat().setMaximumFractionDigits(10);
        degreeOfCorrelationEditor.getFormat().setRoundingMode(RoundingMode.HALF_UP);
        this.degreeOfCorrelation.setMinimumWidth(width);
        this.degreeOfCorrelation.setMaximumWidth(width);
        this.degreeOfCorrelation.addChangeListener(changeEvent -> this.updateVisualizer());
        this.addToContentPanel(this.degreeOfCorrelation, 1, 8, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH);

        ////////////////

        this.timingBasisLabel = new WebLabel("Timing Basis:");
        this.timingBasisLabel.setHorizontalAlignment(WebLabel.RIGHT);
        this.timingBasisLabel.setPadding(Settings.paddingInDialogs);
        this.addToContentPanel(this.timingBasisLabel, 0, 9, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH);

        this.timingBasis = new WebSpinner(new SpinnerNumberModel(300.0, 0.0000000001, 99999999999999999.9, 1.0));
        width = getFontMetrics(this.timingBasis.getFont()).stringWidth("999.999.999.999.999");
        JSpinner.NumberEditor timingBasisEditor = (JSpinner.NumberEditor) this.timingBasis.getEditor();
        timingBasisEditor.getFormat().setMaximumFractionDigits(10);
        timingBasisEditor.getFormat().setRoundingMode(RoundingMode.HALF_UP);
        this.timingBasis.setMinimumWidth(width);
        this.timingBasis.setMaximumWidth(width);
        this.addToContentPanel(this.timingBasis, 1, 9, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH);

        this.timingBasisExplanation = new WebLabel("milliseconds");
        this.timingBasisExplanation.setHorizontalAlignment(WebLabel.LEFT);
        this.timingBasisExplanation.setPadding(Settings.paddingInDialogs);
        this.addToContentPanel(this.timingBasisExplanation, 2, 9, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.HORIZONTAL);

        ////////////////

        this.seed = new WebSpinner(new SpinnerNumberModel(0L, -99999999999999999L, 99999999999999999L, 1L));
        width = getFontMetrics(this.seed.getFont()).stringWidth("999.999.999.999.999");
        this.seed.setMinimumWidth(width);
        this.seed.setMaximumWidth(width);
        this.seed.addChangeListener(changeEvent -> this.updateVisualizer());
        this.addToContentPanel(this.seed, 1, 10, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH);

        WebLabel seedExplanation = new WebLabel("seed value for random number generation");
        seedExplanation.setHorizontalAlignment(WebLabel.LEFT);
        seedExplanation.setPadding(Settings.paddingInDialogs);
        this.addToContentPanel(seedExplanation, 2, 10, 2, 1, 1.0, 1.0, 0, 0, GridBagConstraints.HORIZONTAL);

        this.seedButton = new EditDialogToggleButton("Seed:", new JComponent[]{this.seed, seedExplanation}, false);
        this.seedButton.addActionListener(actionEvent -> this.updateVisualizer());
        this.addToContentPanel(this.seedButton, 0, 10, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH);

        ////////////////

        this.listLabel = new WebLabel("Static Distribution Values:");
        this.listLabel.setHorizontalAlignment(WebLabel.RIGHT);
//        this.listLabel.setVerticalAlignment(WebLabel.TOP);
        this.listLabel.setPadding(Settings.paddingInDialogs);
        this.addToContentPanel(this.listLabel, 0, 11, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH);

        this.listModel = new DefaultListModel<>();
        this.list = new WebList(listModel);
        this.list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        this.list.setLayoutOrientation(WebList.VERTICAL_WRAP);
        this.list.setEditable(true);
        this.list.setCellEditor(new DoubleListCellEditor());
        this.list.setDragEnabled(true);
        this.list.setTransferHandler(new ListItemTransferHandler());
        this.list.setDropMode(DropMode.INSERT);
        this.list.setVisibleRowCount(10);
        this.list.addListEditListener(new ListEditListener() {
            @Override
            public void editStarted(int index) {}
            @Override
            public void editFinished(int index, Object oldValue, Object newValue) {
                updateVisualizer();
            }
            @Override
            public void editCancelled(int index) {}
        });
        this.list.setToolTipText("read column-wise, top down, left to right");

        WebScrollPane listScrollPane = new WebScrollPane(this.list);
        listScrollPane.setStyleId(StyleId.scrollpaneUndecoratedButtonless);
        width = getFontMetrics(this.list.getFont()).stringWidth("999.999.999.999.999");
        int height = (int) (this.list.getFont().getSize() * 21.5);
        listScrollPane.setMinimumWidth(width);
        listScrollPane.setMaximumWidth(width);
        listScrollPane.setMinimumHeight(height);
        listScrollPane.setMaximumHeight(height);
        this.addToContentPanel(listScrollPane, 1, 11, 2, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH);

        this.plus = new WebButton("+", actionEvent -> {
            int i = Math.max(this.list.getSelectedIndex(), 0);
            this.listModel.insertElementAt(0.0, i);
            this.updateVisualizer();
            this.list.setSelectedIndex(i);
            this.list.scrollToCell(i);
        });
        this.plus.addToolTip("Inset a new value");
        this.plus.setHorizontalAlignment(WebButton.CENTER);
        this.plus.setPadding(Settings.paddingInDialogs);
        this.minus = new WebButton("-", actionEvent -> {
            int i = this.list.getSelectedIndex();
            if (i >= 0) {
                this.listModel.remove(i);
                this.updateVisualizer();
                if (!this.listModel.isEmpty()) {
                    this.list.setSelectedIndex(Math.min(i, this.listModel.size() - 1));
                    this.list.scrollToCell(this.list.getSelectedIndex());
                }
            }
        });
        this.minus.addToolTip("Remove selected value");
        this.minus.setHorizontalAlignment(WebButton.CENTER);
        this.minus.setPadding(Settings.paddingInDialogs);
        WebPanel plusMinuPanel = new WebPanel(new GridBagLayout());
        Tools.addComponentToGridBagLayout(plusMinuPanel, (GridBagLayout) plusMinuPanel.getLayout(), this.plus, 0, 0, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH, GridBagConstraints.BASELINE_LEADING);
        Tools.addComponentToGridBagLayout(plusMinuPanel, (GridBagLayout) plusMinuPanel.getLayout(), this.minus, 0, 1, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH, GridBagConstraints.BASELINE_LEADING);
        this.addToContentPanel(plusMinuPanel, 3, 11, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BASELINE_LEADING);

        ////////////////

        this.addIdInput(12);
    }

    /**
     * execute the editor dialog
     * @param input the object to be edited via the dialog or null to create a new one
     * @return the distribution data or null
     */
    @Override
    public DistributionData edit(DistributionData input) {
        if (input != null) {
            this.date.setValue(input.startDate);
            this.id.setText(input.xmlId);

            if (input.lowerLimit != null)
                this.lowerLimit.setValue(input.lowerLimit);

            if (input.upperLimit != null)
                this.upperLimit.setValue(input.upperLimit);

            if (input.lowerClip != null)
                this.lowerClip.setValue(input.lowerClip);

            if (input.upperClip != null)
                this.upperClip.setValue(input.upperClip);

            if (input.standardDeviation != null)
                this.standardDeviation.setValue(input.standardDeviation);

            if (input.maxStepWidth != null)
                this.maxStepWidth.setValue(input.maxStepWidth);

            if (input.degreeOfCorrelation != null)
                this.degreeOfCorrelation.setValue(input.degreeOfCorrelation);

            if (input.mode != null)
                this.mode.setValue(input.mode);

            if (input.millisecondsTimingBasis != null)
                this.timingBasis.setValue(input.millisecondsTimingBasis);

            if (input.seed != null) {
                this.seed.setValue(input.seed);
                this.seedButton.setSelected(true);
            } else
                this.seedButton.setSelected(false);

            for (Double d : input.distributionList)
                this.listModel.addElement(d);

            switch (input.type) {
                case DistributionData.UNIFORM:
                    this.type.setSelectedItem(DistributionEditor.UNIFORM);
                    break;
                case DistributionData.GAUSSIAN:
                    this.type.setSelectedItem(DistributionEditor.GAUSSIAN);
                    break;
                case DistributionData.TRIANGULAR:
                    this.type.setSelectedItem(DistributionEditor.TRIANGULAR);
                    break;
                case DistributionData.BROWNIAN:
                    this.type.setSelectedItem(DistributionEditor.BROWNIAN);
                    break;
                case DistributionData.COMPENSATING_TRIANGLE:
                    this.type.setSelectedItem(DistributionEditor.COMPENSATING_TRIANGLE);
                    break;
                case DistributionData.LIST:
                    this.type.setSelectedItem(DistributionEditor.LIST);
                    break;
                default:
                    this.type.setSelectedItem(DistributionEditor.UNIFORM);
            }
        } else {
            this.type.setSelectedItem(DistributionEditor.UNIFORM);
            this.seedButton.setSelected(false);
        }

        this.setVisible(true);      // start the dialog

        // after the dialog closed do the following

        if (!this.isOk() || (this.type.getSelectedItem() == null))  // if dialog was canceled
            return input;                                           // return the input unchanged

        String id = this.id.getText();
        if (id.isEmpty())
            id = null;

        DistributionData output = new DistributionData();
        output.startDate = Tools.round((double) this.date.getValue(), 10);

        switch ((String) this.type.getSelectedItem()) {
            case DistributionEditor.UNIFORM:
                output.type = DistributionData.UNIFORM;
                output.lowerLimit = Tools.round((double) this.lowerLimit.getValue(), 10);
                output.upperLimit = Tools.round((double) this.upperLimit.getValue(), 10);
                break;
            case DistributionEditor.GAUSSIAN:
                output.type = DistributionData.GAUSSIAN;
                output.lowerLimit = Tools.round((double) this.lowerLimit.getValue(), 10);
                output.upperLimit = Tools.round((double) this.upperLimit.getValue(), 10);
                output.standardDeviation = Tools.round((double) this.standardDeviation.getValue(), 10);
                break;
            case DistributionEditor.TRIANGULAR:
                output.type = DistributionData.TRIANGULAR;
                output.lowerLimit = Tools.round((double) this.lowerLimit.getValue(), 10);
                output.upperLimit = Tools.round((double) this.upperLimit.getValue(), 10);
                output.lowerClip = Tools.round((double) this.lowerClip.getValue(), 10);
                output.upperClip = Tools.round((double) this.upperClip.getValue(), 10);
                output.mode = Tools.round((double) this.mode.getValue(), 10);
                break;
            case DistributionEditor.BROWNIAN:
                output.type = DistributionData.BROWNIAN;
                output.lowerLimit = Tools.round((double) this.lowerLimit.getValue(), 10);
                output.upperLimit = Tools.round((double) this.upperLimit.getValue(), 10);
                output.millisecondsTimingBasis = Tools.round((double) this.timingBasis.getValue(), 10);
                output.maxStepWidth = Tools.round((double) this.maxStepWidth.getValue(), 10);
                break;
            case DistributionEditor.COMPENSATING_TRIANGLE:
                output.type = DistributionData.COMPENSATING_TRIANGLE;
                output.lowerLimit = Tools.round((double) this.lowerLimit.getValue(), 10);
                output.upperLimit = Tools.round((double) this.upperLimit.getValue(), 10);
                output.lowerClip = Tools.round((double) this.lowerClip.getValue(), 10);
                output.upperClip = Tools.round((double) this.upperClip.getValue(), 10);
                output.millisecondsTimingBasis = Tools.round((double) this.timingBasis.getValue(), 10);
                output.degreeOfCorrelation = Tools.round((double) this.degreeOfCorrelation.getValue(), 10);
                break;
            case DistributionEditor.LIST:
                output.type = DistributionData.LIST;
                output.millisecondsTimingBasis = Tools.round((double) this.timingBasis.getValue(), 10);
                for (int i = 0; i < this.listModel.size(); ++i)
                    output.distributionList.add(this.listModel.get(i));
                break;
            default:
                output.type = DistributionData.UNIFORM;
        }

        if (!output.type.equals(DistributionData.LIST) && this.seedButton.isSelected())
            output.seed = (long) ((double) this.seed.getValue());

        output.xmlId = id;

        return output;
    }

    /**
     * When selecting a certain distribution type some input elements need to be de/activated.
     * That is what this method does.
     * @param type (String) this.type.getSelectedItem()
     */
    private void switchDistributionTypeTo(String type) {
        switch (type) {
            case DistributionEditor.UNIFORM: {
                this.lowerLimitLabel.setEnabled(true);
                this.lowerLimit.setEnabled(true);
                this.upperLimitLabel.setEnabled(true);
                this.upperLimit.setEnabled(true);
                this.lowerClipLabel.setEnabled(false);
                this.lowerClip.setEnabled(false);
                this.upperClipLabel.setEnabled(false);
                this.upperClip.setEnabled(false);
                this.standardDeviationLabel.setEnabled(false);
                this.standardDeviation.setEnabled(false);
                this.modeLabel.setEnabled(false);
                this.mode.setEnabled(false);
                this.modeExplanation.setEnabled(false);
                this.maxStepWidthLabel.setEnabled(false);
                this.maxStepWidth.setEnabled(false);
                this.degreeOfCorrelationLabel.setEnabled(false);
                this.degreeOfCorrelation.setEnabled(false);
                this.timingBasisLabel.setEnabled(false);
                this.timingBasis.setEnabled(false);
                this.timingBasisExplanation.setEnabled(false);
                this.listLabel.setEnabled(false);
                this.list.setEnabled(false);
                this.plus.setEnabled(false);
                this.minus.setEnabled(false);

                double lowerLimit = Tools.round((double) this.lowerLimit.getValue(), 10);
                double upperLimit = Tools.round((double) this.upperLimit.getValue(), 10);
                Long seed = (this.seedButton.isSelected()) ? (long) this.seed.getValue() : null;
                this.visualizer.setUniform(lowerLimit, upperLimit, seed);
                break;
            }
            case DistributionEditor.GAUSSIAN: {
                this.lowerLimitLabel.setEnabled(true);
                this.lowerLimit.setEnabled(true);
                this.upperLimitLabel.setEnabled(true);
                this.upperLimit.setEnabled(true);
                this.lowerClipLabel.setEnabled(false);
                this.lowerClip.setEnabled(false);
                this.upperClipLabel.setEnabled(false);
                this.upperClip.setEnabled(false);
                this.standardDeviationLabel.setEnabled(true);
                this.standardDeviation.setEnabled(true);
                this.modeLabel.setEnabled(false);
                this.mode.setEnabled(false);
                this.modeExplanation.setEnabled(false);
                this.maxStepWidthLabel.setEnabled(false);
                this.maxStepWidth.setEnabled(false);
                this.degreeOfCorrelationLabel.setEnabled(false);
                this.degreeOfCorrelation.setEnabled(false);
                this.timingBasisLabel.setEnabled(false);
                this.timingBasis.setEnabled(false);
                this.timingBasisExplanation.setEnabled(false);
                this.listLabel.setEnabled(false);
                this.list.setEnabled(false);
                this.plus.setEnabled(false);
                this.minus.setEnabled(false);

                double lowerLimit = Tools.round((double) this.lowerLimit.getValue(), 10);
                double upperLimit = Tools.round((double) this.upperLimit.getValue(), 10);
                double standardDeviation = Tools.round((double) this.standardDeviation.getValue(), 10);
                Long seed = (this.seedButton.isSelected()) ? (long) ((double) this.seed.getValue()) : null;
                this.visualizer.setGaussian(lowerLimit, upperLimit, standardDeviation, seed);
                break;
            }
            case DistributionEditor.TRIANGULAR: {
                this.lowerLimitLabel.setEnabled(true);
                this.lowerLimit.setEnabled(true);
                this.upperLimitLabel.setEnabled(true);
                this.upperLimit.setEnabled(true);
                this.lowerClipLabel.setEnabled(true);
                this.lowerClip.setEnabled(true);
                this.upperClipLabel.setEnabled(true);
                this.upperClip.setEnabled(true);
                this.standardDeviationLabel.setEnabled(false);
                this.standardDeviation.setEnabled(false);
                this.modeLabel.setEnabled(true);
                this.mode.setEnabled(true);
                this.modeExplanation.setEnabled(true);
                this.maxStepWidthLabel.setEnabled(false);
                this.maxStepWidth.setEnabled(false);
                this.degreeOfCorrelationLabel.setEnabled(false);
                this.degreeOfCorrelation.setEnabled(false);
                this.timingBasisLabel.setEnabled(false);
                this.timingBasis.setEnabled(false);
                this.timingBasisExplanation.setEnabled(false);
                this.listLabel.setEnabled(false);
                this.list.setEnabled(false);
                this.plus.setEnabled(false);
                this.minus.setEnabled(false);

                double lowerLimit = Tools.round((double) this.lowerLimit.getValue(), 10);
                double upperLimit = Tools.round((double) this.upperLimit.getValue(), 10);
                double lowerClip = Tools.round((double) this.lowerClip.getValue(), 10);
                double upperClip = Tools.round((double) this.upperClip.getValue(), 10);
                double mode = Tools.round((double) this.mode.getValue(), 10);
                Long seed = (this.seedButton.isSelected()) ? (long) ((double) this.seed.getValue()) : null;
                this.visualizer.setTriangular(lowerLimit, upperLimit, lowerClip, upperClip, mode, seed);
                break;
            }
            case DistributionEditor.BROWNIAN: {
                this.lowerLimitLabel.setEnabled(true);
                this.lowerLimit.setEnabled(true);
                this.upperLimitLabel.setEnabled(true);
                this.upperLimit.setEnabled(true);
                this.lowerClipLabel.setEnabled(false);
                this.lowerClip.setEnabled(false);
                this.upperClipLabel.setEnabled(false);
                this.upperClip.setEnabled(false);
                this.standardDeviationLabel.setEnabled(false);
                this.standardDeviation.setEnabled(false);
                this.modeLabel.setEnabled(false);
                this.mode.setEnabled(false);
                this.modeExplanation.setEnabled(false);
                this.maxStepWidthLabel.setEnabled(true);
                this.maxStepWidth.setEnabled(true);
                this.degreeOfCorrelationLabel.setEnabled(false);
                this.degreeOfCorrelation.setEnabled(false);
                this.timingBasisLabel.setEnabled(true);
                this.timingBasis.setEnabled(true);
                this.timingBasisExplanation.setEnabled(true);
                this.listLabel.setEnabled(false);
                this.list.setEnabled(false);
                this.plus.setEnabled(false);
                this.minus.setEnabled(false);

                double lowerLimit = Tools.round((double) this.lowerLimit.getValue(), 10);
                double upperLimit = Tools.round((double) this.upperLimit.getValue(), 10);
                double maxStepWidth = Tools.round((double) this.maxStepWidth.getValue(), 10);
                Long seed = (this.seedButton.isSelected()) ? (long) ((double) this.seed.getValue()) : null;
                this.visualizer.setBrownian(lowerLimit, upperLimit, maxStepWidth, seed);
                break;
            }
            case DistributionEditor.COMPENSATING_TRIANGLE: {
                this.lowerLimitLabel.setEnabled(true);
                this.lowerLimit.setEnabled(true);
                this.upperLimitLabel.setEnabled(true);
                this.upperLimit.setEnabled(true);
                this.lowerClipLabel.setEnabled(true);
                this.lowerClip.setEnabled(true);
                this.upperClipLabel.setEnabled(true);
                this.upperClip.setEnabled(true);
                this.standardDeviationLabel.setEnabled(false);
                this.standardDeviation.setEnabled(false);
                this.modeLabel.setEnabled(false);
                this.mode.setEnabled(false);
                this.modeExplanation.setEnabled(false);
                this.maxStepWidthLabel.setEnabled(false);
                this.maxStepWidth.setEnabled(false);
                this.degreeOfCorrelationLabel.setEnabled(true);
                this.degreeOfCorrelation.setEnabled(true);
                this.timingBasisLabel.setEnabled(true);
                this.timingBasis.setEnabled(true);
                this.timingBasisExplanation.setEnabled(true);
                this.listLabel.setEnabled(false);
                this.list.setEnabled(false);
                this.plus.setEnabled(false);
                this.minus.setEnabled(false);

                double lowerLimit = Tools.round((double) this.lowerLimit.getValue(), 10);
                double upperLimit = Tools.round((double) this.upperLimit.getValue(), 10);
                double lowerClip = Tools.round((double) this.lowerClip.getValue(), 10);
                double upperClip = Tools.round((double) this.upperClip.getValue(), 10);
                double degreeOfCorrelation = Tools.round((double) this.degreeOfCorrelation.getValue(), 10);
                Long seed = (this.seedButton.isSelected()) ? (long) ((double) this.seed.getValue()) : null;
                this.visualizer.setCompensatingTriangle(lowerLimit, upperLimit, lowerClip, upperClip, degreeOfCorrelation, seed);
                break;
            }
            case DistributionEditor.LIST: {
                this.lowerLimitLabel.setEnabled(false);
                this.lowerLimit.setEnabled(false);
                this.upperLimitLabel.setEnabled(false);
                this.upperLimit.setEnabled(false);
                this.lowerClipLabel.setEnabled(false);
                this.lowerClip.setEnabled(false);
                this.upperClipLabel.setEnabled(false);
                this.upperClip.setEnabled(false);
                this.standardDeviationLabel.setEnabled(false);
                this.standardDeviation.setEnabled(false);
                this.modeLabel.setEnabled(false);
                this.mode.setEnabled(false);
                this.modeExplanation.setEnabled(false);
                this.maxStepWidthLabel.setEnabled(false);
                this.maxStepWidth.setEnabled(false);
                this.degreeOfCorrelationLabel.setEnabled(false);
                this.degreeOfCorrelation.setEnabled(false);
                this.timingBasisLabel.setEnabled(true);
                this.timingBasis.setEnabled(true);
                this.timingBasisExplanation.setEnabled(true);
                this.listLabel.setEnabled(true);
                this.list.setEnabled(true);
                this.plus.setEnabled(true);
                this.minus.setEnabled(true);

                ArrayList<Double> list = new ArrayList<>();
                for (int i = 0; i < this.listModel.size(); ++i)
                    list.add(this.listModel.get(i));
                this.visualizer.setDistributionList(list);
                break;
            }
        }
    }

    /**
     * Read the current parameter settings and repaint the visualizer.
     */
    private void updateVisualizer() {
        if (this.type.getSelectedItem() == null)
            return;

        switch ((String) this.type.getSelectedItem()) {
            case DistributionEditor.UNIFORM: {
                double lowerLimit = Tools.round((double) this.lowerLimit.getValue(), 10);
                double upperLimit = Tools.round((double) this.upperLimit.getValue(), 10);
                Long seed = (this.seedButton.isSelected()) ? (long) ((double) this.seed.getValue()) : null;
                this.visualizer.setUniform(lowerLimit, upperLimit, seed);
                break;
            }
            case DistributionEditor.GAUSSIAN: {
                double lowerLimit = Tools.round((double) this.lowerLimit.getValue(), 10);
                double upperLimit = Tools.round((double) this.upperLimit.getValue(), 10);
                double standardDeviation = Tools.round((double) this.standardDeviation.getValue(), 10);
                Long seed = (this.seedButton.isSelected()) ? (long) ((double) this.seed.getValue()) : null;
                this.visualizer.setGaussian(lowerLimit, upperLimit, standardDeviation, seed);
                break;
            }
            case DistributionEditor.TRIANGULAR: {
                double lowerLimit = Tools.round((double) this.lowerLimit.getValue(), 10);
                double upperLimit = Tools.round((double) this.upperLimit.getValue(), 10);
                double lowerClip = Tools.round((double) this.lowerClip.getValue(), 10);
                double upperClip = Tools.round((double) this.upperClip.getValue(), 10);
                double mode = Tools.round((double) this.mode.getValue(), 10);
                Long seed = (this.seedButton.isSelected()) ? (long) ((double) this.seed.getValue()) : null;
                this.visualizer.setTriangular(lowerLimit, upperLimit, lowerClip, upperClip, mode, seed);
                break;
            }
            case DistributionEditor.BROWNIAN: {
                double lowerLimit = Tools.round((double) this.lowerLimit.getValue(), 10);
                double upperLimit = Tools.round((double) this.upperLimit.getValue(), 10);
                double maxStepWidth = Tools.round((double) this.maxStepWidth.getValue(), 10);
                Long seed = (this.seedButton.isSelected()) ? (long) ((double) this.seed.getValue()) : null;
                this.visualizer.setBrownian(lowerLimit, upperLimit, maxStepWidth, seed);
                break;
            }
            case DistributionEditor.COMPENSATING_TRIANGLE: {
                double lowerLimit = Tools.round((double) this.lowerLimit.getValue(), 10);
                double upperLimit = Tools.round((double) this.upperLimit.getValue(), 10);
                double lowerClip = Tools.round((double) this.lowerClip.getValue(), 10);
                double upperClip = Tools.round((double) this.upperClip.getValue(), 10);
                double degreeOfCorrelation = Tools.round((double) this.degreeOfCorrelation.getValue(), 10);
                Long seed = (this.seedButton.isSelected()) ? (long) ((double) this.seed.getValue()) : null;
                this.visualizer.setCompensatingTriangle(lowerLimit, upperLimit, lowerClip, upperClip, degreeOfCorrelation, seed);
                break;
            }
            case DistributionEditor.LIST: {
                ArrayList<Double> list = new ArrayList<>();
                for (int i = 0; i < this.listModel.size(); ++i)
                    list.add(this.listModel.get(i));
                this.visualizer.setDistributionList(list);
                break;
            }
        }
    }

    /**
     * This class is needed to make the list cells editable. As these should only
     * accept double values, this class' methods implement the conversions from/to String.
     * @author Axel Berndt
     */
    private static class DoubleListCellEditor extends TextListCellEditor<Double> {
        @Override
        protected String valueToText(JList list, int index, Double value) {
            return value.toString();
        }

        @Override
        protected Double textToValue(JList list, int index, Double oldValue, String text) {
            try {
                return Double.parseDouble(text);
            } catch (NumberFormatException e) { // if the input text cannot be parsed to Double
                return oldValue;                // keep the old value
            }
        }
    }

    /**
     * This is needed to enable drag and drop interaction in the list.
     * Source: https://docs.oracle.com/javase/tutorial/uiswing/dnd/dropmodedemo.html
     */
    private static class ListItemTransferHandler extends TransferHandler {
        protected final DataFlavor localObjectFlavor;
        protected int[] indices;
        protected int addIndex = -1;    // Location where items were added
        protected int addCount;         // Number of items added.

        public ListItemTransferHandler() {
            super();
            localObjectFlavor = new DataFlavor(Object[].class, "Array of items");
        }

        @Override
        protected Transferable createTransferable(JComponent c) {
            JList<?> source = (JList<?>) c;
            c.getRootPane().getGlassPane().setVisible(true);

            indices = source.getSelectedIndices();
            Object[] transferedObjects = source.getSelectedValuesList().toArray(new Object[0]);
            // return new DataHandler(transferedObjects, localObjectFlavor.getMimeType());
            return new Transferable() {
                @Override public DataFlavor[] getTransferDataFlavors() {
                    return new DataFlavor[] {localObjectFlavor};
                }
                @Override public boolean isDataFlavorSupported(DataFlavor flavor) {
                    return Objects.equals(localObjectFlavor, flavor);
                }
                @Override public Object getTransferData(DataFlavor flavor)
                        throws UnsupportedFlavorException, IOException {
                    if (isDataFlavorSupported(flavor)) {
                        return transferedObjects;
                    } else {
                        throw new UnsupportedFlavorException(flavor);
                    }
                }
            };
        }

        @Override
        public boolean canImport(TransferSupport info) {
            return info.isDrop() && info.isDataFlavorSupported(localObjectFlavor);
        }

        @Override
        public int getSourceActions(JComponent c) {
            Component glassPane = c.getRootPane().getGlassPane();
            glassPane.setCursor(DragSource.DefaultMoveDrop);
            return MOVE; // COPY_OR_MOVE;
        }

        @SuppressWarnings("unchecked")
        @Override
        public boolean importData(TransferSupport info) {
            TransferHandler.DropLocation tdl = info.getDropLocation();
            if (!canImport(info) || !(tdl instanceof JList.DropLocation)) {
                return false;
            }

            JList.DropLocation dl = (JList.DropLocation) tdl;
            JList target = (JList) info.getComponent();
            DefaultListModel listModel = (DefaultListModel) target.getModel();
            int max = listModel.getSize();
            int index = dl.getIndex();
            index = index < 0 ? max : index; // If it is out of range, it is appended to the end
            index = Math.min(index, max);

            addIndex = index;

            try {
                Object[] values = (Object[]) info.getTransferable().getTransferData(localObjectFlavor);
                for (int i = 0; i < values.length; i++) {
                    int idx = index++;
                    listModel.add(idx, values[i]);
                    target.addSelectionInterval(idx, idx);
                }
                addCount = values.length;
                return true;
            } catch (UnsupportedFlavorException | IOException ex) {
                ex.printStackTrace();
            }

            return false;
        }

        @Override
        protected void exportDone(JComponent c, Transferable data, int action) {
            c.getRootPane().getGlassPane().setVisible(false);
            cleanup(c, action == MOVE);
        }

        private void cleanup(JComponent c, boolean remove) {
            if (remove && Objects.nonNull(indices)) {
                if (addCount > 0) {
                    // https://github.com/aterai/java-swing-tips/blob/master/DragSelectDropReordering/src/java/example/MainPanel.java
                    for (int i = 0; i < indices.length; i++) {
                        if (indices[i] >= addIndex) {
                            indices[i] += addCount;
                        }
                    }
                }
                JList source = (JList) c;
                DefaultListModel model = (DefaultListModel) source.getModel();
                for (int i = indices.length - 1; i >= 0; i--) {
                    model.remove(indices[i]);
                }
            }

            indices = null;
            addCount = 0;
            addIndex = -1;
        }
    }
}
