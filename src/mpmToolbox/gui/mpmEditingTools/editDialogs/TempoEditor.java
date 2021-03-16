package mpmToolbox.gui.mpmEditingTools.editDialogs;

import com.alee.extended.button.WebSplitButton;
import com.alee.extended.button.WebSwitch;
import com.alee.laf.button.WebButton;
import com.alee.laf.label.WebLabel;
import com.alee.laf.menu.WebMenuItem;
import com.alee.laf.menu.WebPopupMenu;
import com.alee.laf.panel.WebPanel;
import com.alee.laf.slider.WebSlider;
import com.alee.laf.spinner.WebSpinner;
import com.alee.laf.text.WebTextField;
import meico.mpm.Mpm;
import meico.mpm.elements.maps.TempoMap;
import meico.mpm.elements.maps.data.TempoData;
import meico.mpm.elements.styles.GenericStyle;
import meico.mpm.elements.styles.defs.AbstractDef;
import meico.mpm.elements.styles.defs.TempoDef;
import mpmToolbox.gui.Settings;
import mpmToolbox.gui.mpmEditingTools.editDialogs.supplementary.EditDialogToggleButton;
import mpmToolbox.gui.mpmEditingTools.editDialogs.visualizers.TempoVisualizer;
import mpmToolbox.supplementary.Tools;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.math.RoundingMode;
import java.util.Hashtable;

/**
 * The Tempo editor.
 * @author Axel Berndt
 */
public class TempoEditor extends EditDialog<TempoData> {
    private WebSwitch bpmMode;
    private WebSpinner numericBpm;
    private WebTextField literalBpm;
    private WebSplitButton literalBpmChooser;

    private EditDialogToggleButton transitionToButton;
    private WebSwitch transitionToMode;
    private WebSpinner numericTransitionTo;
    private WebTextField literalTransitionTo;
    private WebSplitButton literalTransitionToChooser;

    private WebLabel meanTempoAtLabel;
    private WebSlider meanTempoAtSlider;
    private WebSpinner meanTempoAt;

    private WebSpinner beatLength;

    private TempoVisualizer visualizer;

    /**
     * constructor
     * @param map the map that gets or holds the tempo element
     */
    public TempoEditor(TempoMap map) {
        super("Edit Tempo", map);
    }

    /**
     * GUI
     */
    @Override
    public void makeContentPanel() {
        this.addDateInput(0);
        this.date.addChangeListener(changeEvent -> {
            this.fullLiteralUpdate(Mpm.TEMPO_STYLE, this.literalBpm, this.literalBpmChooser, this.numericBpm);
            this.fullLiteralUpdate(Mpm.TEMPO_STYLE, this.literalTransitionTo, this.literalTransitionToChooser, this.numericTransitionTo);
        });

        ///////////////

        WebLabel beatLengthLabel = new WebLabel("Beat Length:");
        beatLengthLabel.setHorizontalAlignment(WebLabel.RIGHT);
        beatLengthLabel.setPadding(Settings.paddingInDialogs);
        this.addToContentPanel(beatLengthLabel, 0, 1, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH);

        this.beatLength = new WebSpinner(new SpinnerNumberModel(0.25, 1.0/128.0, 2.0, 0.01));
        JSpinner.NumberEditor beatLengthEditor = (JSpinner.NumberEditor) this.beatLength.getEditor(); // https://stackoverflow.com/questions/34627998/jspinner-number-editor
        beatLengthEditor.getFormat().setMaximumFractionDigits(10);
        beatLengthEditor.getFormat().setRoundingMode(RoundingMode.HALF_UP);
        int beatLengthWidth = getFontMetrics(this.beatLength.getFont()).stringWidth("999.999.999.999");
        this.beatLength.setMinimumWidth(beatLengthWidth);
        this.beatLength.setMaximumWidth(beatLengthWidth);
        this.addToContentPanel(this.beatLength, 1, 1, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH);

        WebLabel beatLengthExplanation =  new WebLabel("the musical length of one beat, e.g. 1/2 = 0.5, 1/4 = 0.25, 1/8 = 0.125");
        beatLengthExplanation.setHorizontalAlignment(WebLabel.LEFT);
        beatLengthExplanation.setPadding(Settings.paddingInDialogs);
        this.addToContentPanel(beatLengthExplanation, 2, 1, 2, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH);

        ///////////////

        WebLabel visualizerLabel = new WebLabel("Tempo Shape:");
        visualizerLabel.setHorizontalAlignment(WebLabel.RIGHT);
        visualizerLabel.setPadding(Settings.paddingInDialogs);
        this.addToContentPanel(visualizerLabel, 0, 4, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH);

        this.visualizer = new TempoVisualizer(0.0, 0.0, 0.5);
        this.addToContentPanel(this.visualizer, 1, 4, 3, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH);

        ///////////////

        this.meanTempoAtLabel = new WebLabel("Mean Tempo at:");
        this.meanTempoAtLabel.setHorizontalAlignment(WebLabel.RIGHT);
        this.meanTempoAtLabel.setPadding(Settings.paddingInDialogs);
        this.addToContentPanel(this.meanTempoAtLabel, 0, 5, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH);

        this.meanTempoAtSlider = new WebSlider(WebSlider.HORIZONTAL, 0, 100000, 50000);
        Tools.makeSliderSetToClickPosition(this.meanTempoAtSlider);
//        this.meanTempoAtSlider.setPadding(Settings.paddingInDialogs);
        this.meanTempoAtSlider.setToolTip("This indicates the relative position between the start (0) and end (1)\nof the tempo transition where the mean tempo is reached.");
        this.meanTempoAtSlider.setMajorTickSpacing(100000);
        this.meanTempoAtSlider.setMinorTickSpacing(50000);
        this.meanTempoAtSlider.setPaintTicks(true);
        Hashtable<Integer, WebLabel> meanTempoAtSliderLabels = new Hashtable<>();
        meanTempoAtSliderLabels.put(0, new WebLabel("0"));
        meanTempoAtSliderLabels.put(50000, new WebLabel("0.5"));
        meanTempoAtSliderLabels.put(100000, new WebLabel("1"));
        this.meanTempoAtSlider.setLabelTable(meanTempoAtSliderLabels);
        this.meanTempoAtSlider.setPaintLabels(true);
        this.meanTempoAtSlider.addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2)     // set 0 on double click
                    meanTempoAtSlider.setValue(50000);
            }
            @Override
            public void mousePressed(MouseEvent e) {
            }
            @Override
            public void mouseReleased(MouseEvent e) {
            }
            @Override
            public void mouseEntered(MouseEvent e) {
            }
            @Override
            public void mouseExited(MouseEvent e) {
            }
        });
        this.meanTempoAtSlider.addChangeListener(changeEvent -> {
            double meanTempoAt = ((double) this.meanTempoAtSlider.getValue()) / 100000.0;
            this.meanTempoAt.setValue(meanTempoAt);
            this.visualizer.setMeanTempoAt(meanTempoAt);
        });
        this.addToContentPanel(this.meanTempoAtSlider, 1, 5, 2, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH);

        this.meanTempoAt = new WebSpinner(new SpinnerNumberModel(0.5, 0.0, 1.0, 0.01));
        JSpinner.NumberEditor meanTempoAtEditor = (JSpinner.NumberEditor) this.meanTempoAt.getEditor(); // https://stackoverflow.com/questions/34627998/jspinner-number-editor
        meanTempoAtEditor.getFormat().setMaximumFractionDigits(10);
        meanTempoAtEditor.getFormat().setRoundingMode(RoundingMode.HALF_UP);
        int meanTempoAtWidth = getFontMetrics(this.meanTempoAt.getFont()).stringWidth("999.999.999.999");
        this.meanTempoAt.setMinimumWidth(meanTempoAtWidth);
        this.meanTempoAt.setMaximumWidth(meanTempoAtWidth);
        this.meanTempoAt.addChangeListener(changeEvent -> {
            double meanTempoAt = Tools.round(((double) this.meanTempoAt.getValue()), 10);
            this.meanTempoAtSlider.setValue((int) (meanTempoAt * 100000));
            this.visualizer.setMeanTempoAt(meanTempoAt);
        });
        this.addToContentPanel(this.meanTempoAt, 3, 5, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH);

        ///////////////

        this.numericLiteralInputs("Beats per Minute:", 2);
        this.numericLiteralInputs("Transition to:", 3);

        this.numericBpm.addChangeListener(changeEvent -> {
            if (this.transitionToButton.isSelected())
                this.visualizer.setBpmTransitionTo((double) this.numericBpm.getValue(), (double) this.numericTransitionTo.getValue());
            else
                this.visualizer.setBpmTransitionTo((double) this.numericBpm.getValue(), (double) this.numericBpm.getValue());
        });

        this.numericTransitionTo.addChangeListener(changeEvent -> {
            if (this.transitionToButton.isSelected())
                this.visualizer.setTransitionTo((double) this.numericTransitionTo.getValue());
            else
                this.visualizer.setTransitionTo((double) this.numericBpm.getValue());
        });

        ///////////////

        this.addIdInput(6);
    }

    /**
     * execute the editor dialog
     * @param input the object to be edited via the dialog or null to create a new one
     * @return
     */
    @Override
    public TempoData edit(TempoData input) {
        if (input != null) {
            this.date.setValue(input.startDate);
            this.beatLength.setValue(input.beatLength);

            if (input.bpmString != null) {
                this.literalBpm.setText(input.bpmString);
                this.bpmMode.setSelected(false);
            } else {
                this.bpmMode.setSelected(true);
                if (input.bpm != null) {
                    this.numericBpm.setValue(input.bpm);
                }
            }
            if (input.transitionToString != null) {
                this.literalTransitionTo.setText(input.transitionToString);
                this.transitionToButton.setSelected(true);
                this.transitionToMode.setSelected(false);

                if (input.meanTempoAt != null)
                    this.meanTempoAt.setValue(input.meanTempoAt);

            } else {
                this.transitionToMode.setSelected(true);
                if (input.transitionTo != null) {
                    this.numericTransitionTo.setValue(input.transitionTo);
                    this.transitionToButton.setSelected(true);
                } else {
                    this.transitionToButton.setSelected(false);
                }
            }

            this.id.setText(input.xmlId);
        }
        else {
            this.bpmMode.setSelected(true);
            this.transitionToMode.setSelected(true);
        }

        // the mode switches need an additional initialization for the gripper text
        ((WebLabel) this.bpmMode.getGripper().getFirstComponent()).setText("Switch to " + (this.bpmMode.isSelected() ? "Literal" : "Numeric") + " Input");
        ((WebLabel) this.transitionToMode.getGripper().getFirstComponent()).setText("Switch to " + (this.transitionToMode.isSelected() ? "Literal" : "Numeric") + " Input");

        // initialize the choosers' contents
        this.fullLiteralUpdate(Mpm.TEMPO_STYLE, this.literalBpm, this.literalBpmChooser, this.numericBpm);
        this.fullLiteralUpdate(Mpm.TEMPO_STYLE, this.literalTransitionTo, this.literalTransitionToChooser, this.numericTransitionTo);

        // initialize the visualizer
        if (this.transitionToButton.isSelected())
            this.visualizer.setBpmTransitionTo((double) this.numericBpm.getValue(), (double) this.numericTransitionTo.getValue());
        else
            this.visualizer.setBpmTransitionTo((double) this.numericBpm.getValue(), (double) this.numericBpm.getValue());

        this.setVisible(true);      // start the dialog

        // after the dialog closed do the following

        if (!this.isOk())           // if input was canceled
            return input;

        String id = this.id.getText();
        if (id.isEmpty())
            id = null;

        TempoData output = new TempoData();
        output.startDate = Tools.round((double) this.date.getValue(), 10);
        output.beatLength = Tools.round((double) this.beatLength.getValue(), 10);

        if (this.bpmMode.isSelected())                  // numeric tempo
            output.bpm = Tools.round((double) this.numericBpm.getValue(), 10);
        else {                                          // literal tempo
            output.bpmString = this.literalBpm.getText();

            // try to get the numeric value of the literal
//            AbstractDef def = this.style.getDef(output.bpmString);
//            if (def != null)
//                output.bpm = ((TempoDef) def).getValue();
        }

        if (this.transitionToButton.isSelected()) {
            if (this.transitionToMode.isSelected())     // numeric transition to
                output.transitionTo = Tools.round((double) this.numericTransitionTo.getValue(), 10);
            else {                                      // literal transition to
                output.transitionToString = this.literalTransitionTo.getText();

                // try to get the numeric value of the literal
//                AbstractDef def = this.style.getDef(output.transitionToString);
//                if (def != null)
//                    output.transitionTo = ((TempoDef) def).getValue();
            }

//            if ((output.bpm == null) || !output.bpm.equals(output.transitionTo)) {      // the following makes only sense when the transition transitions to a different value than the start value; to activate, uncomment this line and the above 2 commented blocks
                double meanTempoAt = Tools.round((double) this.meanTempoAt.getValue(), 10);
                if (meanTempoAt != 0.5)
                    output.meanTempoAt = meanTempoAt;
//            }
        }

        output.xmlId = id;

        return output;
    }

    /**
     * create an input row where it is possible to switch between numerical and literal input
     * @param label
     * @param gridBagRow
     */
    protected void numericLiteralInputs(String label, int gridBagRow) {
        WebSwitch mode;
        WebSpinner numeric;
        WebTextField literal;
        WebSplitButton literalChooser;

        switch (label) {
            case "Beats per Minute:":
                this.bpmMode = new WebSwitch(false);
                this.numericBpm = new WebSpinner(new SpinnerNumberModel(100.0, 1.0, Double.MAX_VALUE, 1.0));
                this.literalBpm = new WebTextField();
                this.literalBpmChooser = new WebSplitButton("Choose");
                mode = this.bpmMode;
                numeric = this.numericBpm;
                literal = this.literalBpm;
                literalChooser = this.literalBpmChooser;

                WebLabel inputLabel = new WebLabel(label);
                inputLabel.setHorizontalAlignment(WebLabel.RIGHT);
                inputLabel.setPadding(Settings.paddingInDialogs);
                this.addToContentPanel(inputLabel, 0, gridBagRow, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH);
                break;
            case "Transition to:":
                this.transitionToMode = new WebSwitch(false);
                this.numericTransitionTo = new WebSpinner(new SpinnerNumberModel(100.0, 1.0, Double.MAX_VALUE, 1.0));
                this.literalTransitionTo = new WebTextField();
                this.literalTransitionToChooser = new WebSplitButton("Choose");
                mode = this.transitionToMode;
                numeric = this.numericTransitionTo;
                literal = this.literalTransitionTo;
                literalChooser = this.literalTransitionToChooser;

                this.transitionToButton = new EditDialogToggleButton(label, new JComponent[]{this.transitionToMode, this.numericTransitionTo, this.literalTransitionTo, this.meanTempoAtLabel, this.meanTempoAtSlider, this.meanTempoAt}, false);
                this.transitionToButton.addActionListener(actionEvent -> {
                    this.literalTransitionToChooser.setEnabled(this.transitionToButton.isSelected() && (this.style != null));
                    if (this.transitionToButton.isSelected())
                        this.visualizer.setTransitionTo((double) this.numericTransitionTo.getValue());
                    else
                        this.visualizer.setTransitionTo((double) this.numericBpm.getValue());
                });
                this.addToContentPanel(this.transitionToButton, 0, gridBagRow, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH);
                break;
            default:
                return;
        }

        int width = getFontMetrics(numeric.getFont()).stringWidth("999.999.999.999.999");
        JSpinner.NumberEditor numericEditor = (JSpinner.NumberEditor) numeric.getEditor();
        numericEditor.getFormat().setMaximumFractionDigits(10);
        numericEditor.getFormat().setRoundingMode(RoundingMode.HALF_UP);
        numeric.setMinimumWidth(width);
        numeric.setMaximumWidth(width);

        literalChooser.setHorizontalAlignment(WebButton.CENTER);
        literalChooser.setPadding(Settings.paddingInDialogs);

        literal.setMinimumWidth(getFontMetrics(literal.getFont()).stringWidth("999.999.999.999.999"));
        literal.setHorizontalAlignment(WebTextField.LEFT);
        literal.setPadding(Settings.paddingInDialogs);
        literal.setToolTip("Make sure that the name really exists!");
        this.makeLiteralCheckAgainstStyles(literal, numeric);

        GridBagLayout literalPanelLayout = new GridBagLayout();
        WebPanel literalPanel = new WebPanel(literalPanelLayout);
        Tools.addComponentToGridBagLayout(literalPanel, literalPanelLayout, literal, 0, 0, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH, GridBagConstraints.LINE_START);
        Tools.addComponentToGridBagLayout(literalPanel, literalPanelLayout, literalChooser, 1, 0, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH, GridBagConstraints.LINE_START);

        mode.setSwitchComponents(numeric, literalPanel);
        WebLabel gripperLabel = new WebLabel("Switch to Numeric Input", WebLabel.CENTER);
        mode.getGripper().add(gripperLabel);
        mode.getGripper().onMouseClick(e -> {
            mode.setSelected(!mode.isSelected());
            gripperLabel.setText("Switch to " + (mode.isSelected() ? "Literal" : "Numeric") + " Input");
        });
        mode.getGripper().setToolTip("Switch between numeric and literal input.");

        this.addToContentPanel(mode, 1, gridBagRow, 3, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH);
    }

    /**
     * This adds functionality to the literal textfield. If its content changes it will automatically
     * check against the localStyle and globalStyle if the name exists and color the text accordingly
     * via method updateLiteralField().
     * @param literal
     * @param numeric
     */
    private void makeLiteralCheckAgainstStyles(WebTextField literal, WebSpinner numeric) {
        literal.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent documentEvent) {
                updateLiteralField(literal, numeric);
            }
            @Override
            public void removeUpdate(DocumentEvent documentEvent) {
                updateLiteralField(literal, numeric);
            }
            @Override
            public void changedUpdate(DocumentEvent documentEvent) {
                updateLiteralField(literal, numeric);
            }
        });
    }

    /**
     * format the literal text according to whether the name exists in the underlying style or not
     * @param literal
     * @param numeric
     */
    private void updateLiteralField(WebTextField literal, WebSpinner numeric) {
        // check if the name exists in the linked style
        AbstractDef def = null;
        if (this.style != null)
            def = this.style.getDef(literal.getText());

        if (def != null) {
            numeric.setValue(((TempoDef) def).getValue());
            literal.setForeground(Settings.foregroundColor);
        } else
            literal.setForeground(Settings.errorColor);

        literal.revalidate();
        literal.repaint();
    }

    /**
     * This method takes the given style and puts all its defs' names into the chooser.
     * @param style
     * @param chooser
     */
    protected void fillChooser(GenericStyle style, WebTextField literal, WebSplitButton chooser, WebSpinner numeric) {
        if (style == null) {
            chooser.setPopupMenu(null);
            chooser.setEnabled(false);
            chooser.setToolTip("No (valid) style defined for this date.\nMake sure there is a style switch in the map before or at this date.");
            return;
        }

        WebPopupMenu nameRefs = new WebPopupMenu();
        for (Object key : style.getAllDefs().keySet()) {
            String keyString = (String) key;
            double value = ((TempoDef) style.getDef(keyString)).getValue();
            WebMenuItem item = new WebMenuItem(keyString + " = " + value + "bpm");
            item.addActionListener(menuItemActionEvent -> {
                literal.setText(keyString);
                numeric.setValue(value);
            });
            nameRefs.add(item);
        }

        chooser.setEnabled((chooser != this.literalTransitionToChooser) || this.transitionToButton.isSelected());   // basically true, but if it is the literalTransitionToChooser we have to make sure that the transitionToButton is activated
        chooser.setPopupMenu(nameRefs);
        chooser.setToolTip("from style \"" + style.getName() + "\"");
    }

    /**
     * Invoke this method in a map element dialog to
     * 1. get the style that applies to the element's date,
     * 2. fill the chooser with the style's def names
     * 3. update the text color in the nameRef field according to whether the name is in the style or not.
     * @param styleType use the constants in class Mpm
     * @param literal
     * @param chooser
     */
    private void fullLiteralUpdate(String styleType, WebTextField literal, WebSplitButton chooser, WebSpinner numeric) {
        if ((this.map == null) || (this.date == null))
            return;
        double date = Tools.round((double) this.date.getValue(), 10);
        this.style = this.map.getStyleAt(date, styleType);          // update the underlying style
        this.fillChooser(this.style, literal, chooser, numeric);    // update the chooser's content
        this.updateLiteralField(literal, numeric);                  // check whether the name is present in the style
    }
}
