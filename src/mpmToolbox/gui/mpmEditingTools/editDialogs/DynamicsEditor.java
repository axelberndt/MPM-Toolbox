package mpmToolbox.gui.mpmEditingTools.editDialogs;

import com.alee.extended.button.WebSplitButton;
import com.alee.extended.button.WebSwitch;
import com.alee.laf.button.WebButton;
import com.alee.laf.checkbox.WebCheckBox;
import com.alee.laf.label.WebLabel;
import com.alee.laf.menu.WebMenuItem;
import com.alee.laf.menu.WebPopupMenu;
import com.alee.laf.panel.WebPanel;
import com.alee.laf.slider.WebSlider;
import com.alee.laf.spinner.WebSpinner;
import com.alee.laf.text.WebTextField;
import meico.mpm.Mpm;
import meico.mpm.elements.maps.DynamicsMap;
import meico.mpm.elements.maps.data.DynamicsData;
import meico.mpm.elements.styles.GenericStyle;
import meico.mpm.elements.styles.defs.AbstractDef;
import meico.mpm.elements.styles.defs.DynamicsDef;
import mpmToolbox.gui.Settings;
import mpmToolbox.gui.mpmEditingTools.editDialogs.visualizers.DynamicsVisualizer;
import mpmToolbox.gui.mpmEditingTools.editDialogs.supplementary.EditDialogToggleButton;
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
 * The dynamics editor.
 * @author Axel Berndt
 */
public class DynamicsEditor extends EditDialog<DynamicsData> {
    private WebSwitch volumeMode;
    private WebSpinner numericVolume;
    private WebTextField literalVolume;
    private WebSplitButton literalVolumeChooser;

    private EditDialogToggleButton transitionToButton;
    private WebSwitch transitionToMode;
    private WebSpinner numericTransitionTo;
    private WebTextField literalTransitionTo;
    private WebSplitButton literalTransitionToChooser;

    private WebSpinner curvature;
    private WebSlider curvatureSlider;
    private WebLabel curvatureLabel;

    private WebSpinner protraction;
    private WebSlider protractionSlider;
    private WebLabel protractionLabel;

    private WebCheckBox subNoteDynamics;
    private WebLabel subNoteDynamicsLabel;

    private DynamicsVisualizer visualizer;

    /**
     * constructor
     * @param map the map that gets or holds the dynamics element
     */
    public DynamicsEditor(DynamicsMap map) {
        super("Edit Dynamics", map);
    }

    /**
     * GUI
     */
    @Override
    public void makeContentPanel() {
        this.addDateInput(0);
        this.date.addChangeListener(changeEvent -> {
            this.fullLiteralUpdate(Mpm.DYNAMICS_STYLE, this.literalVolume, this.literalVolumeChooser, this.numericVolume);
            this.fullLiteralUpdate(Mpm.DYNAMICS_STYLE, this.literalTransitionTo, this.literalTransitionToChooser, this.numericTransitionTo);
        });

        ///////////////

        WebLabel visualizerLabel = new WebLabel("Dynamics Shape:");
        visualizerLabel.setHorizontalAlignment(WebLabel.RIGHT);
        visualizerLabel.setPadding(Settings.paddingInDialogs);
        this.addToContentPanel(visualizerLabel, 0, 3, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH);

        this.visualizer = new DynamicsVisualizer(0.0, 0.0, 0.0, 0.0);
        this.addToContentPanel(this.visualizer, 1, 3, 3, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH);

        ///////////////

        this.curvatureLabel = new WebLabel("Curvature:");
        this.curvatureLabel.setHorizontalAlignment(WebLabel.RIGHT);
        this.curvatureLabel.setPadding(Settings.paddingInDialogs);
        this.addToContentPanel(this.curvatureLabel, 0, 4, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH);

        this.curvatureSlider = new WebSlider(WebSlider.HORIZONTAL, 0, 100000, 0);
        Tools.makeSliderSetToClickPosition(this.curvatureSlider);
//        this.curvatureSlider.setPadding(Settings.paddingInDialogs);
        this.curvatureSlider.setToolTip("Increase to create a less linear transition that sounds\nmore pronounced and with stronger emphasis.");
        this.curvatureSlider.setMajorTickSpacing(100000);
        this.curvatureSlider.setMinorTickSpacing(50000);
        this.curvatureSlider.setPaintTicks(true);
        Hashtable<Integer, WebLabel> curvatureSliderLabels = new Hashtable<>();
        curvatureSliderLabels.put(0, new WebLabel("0"));
        curvatureSliderLabels.put(50000, new WebLabel("0.5"));
        curvatureSliderLabels.put(100000, new WebLabel("1"));
        this.curvatureSlider.setLabelTable(curvatureSliderLabels);
        this.curvatureSlider.setPaintLabels(true);
        this.curvatureSlider.addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2)     // set 0 on double click
                    curvatureSlider.setValue(0);
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
        this.curvatureSlider.addChangeListener(changeEvent -> {
            double curvature = ((double) this.curvatureSlider.getValue()) / 100000.0;
            this.curvature.setValue(curvature);
            this.visualizer.setCurvature(curvature);
        });
        this.addToContentPanel(this.curvatureSlider, 1, 4, 2, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH);

        this.curvature = new WebSpinner(new SpinnerNumberModel(0.0, 0.0, 1.0, 0.1));
        JSpinner.NumberEditor curvatureEditor = (JSpinner.NumberEditor) this.curvature.getEditor(); // https://stackoverflow.com/questions/34627998/jspinner-number-editor
        curvatureEditor.getFormat().setMaximumFractionDigits(10);
        curvatureEditor.getFormat().setRoundingMode(RoundingMode.HALF_UP);
        int curvatureWidth = getFontMetrics(this.curvature.getFont()).stringWidth("999.999.999.999");
        this.curvature.setMinimumWidth(curvatureWidth);
        this.curvature.setMaximumWidth(curvatureWidth);
        this.curvature.addChangeListener(changeEvent -> {
            double curvature = Tools.round((double) this.curvature.getValue(), 10);
            curvatureSlider.setValue((int) (curvature * 100000));
            this.visualizer.setCurvature(curvature);
        });
        this.addToContentPanel(this.curvature, 3, 4, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH);

        ///////////////

        this.protractionLabel = new WebLabel("Protraction:");
        this.protractionLabel.setHorizontalAlignment(WebLabel.RIGHT);
        this.protractionLabel.setPadding(Settings.paddingInDialogs);
        this.addToContentPanel(this.protractionLabel, 0, 5, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH);

        this.protractionSlider = new WebSlider(WebSlider.HORIZONTAL, -100000, 100000, 0);
        Tools.makeSliderSetToClickPosition(this.protractionSlider);
//        this.protractionSlider.setPadding(Settings.paddingInDialogs);
        this.protractionSlider.setToolTip("Shift the bulk of the transition to the front (negative value) or back (positive value).");
        this.protractionSlider.setMajorTickSpacing(100000);
        this.protractionSlider.setMinorTickSpacing(50000);
        this.protractionSlider.setPaintTicks(true);
        Hashtable<Integer, WebLabel> protractionSliderLabels = new Hashtable<>();
        protractionSliderLabels.put(-100000, new WebLabel("-1"));
        protractionSliderLabels.put(0, new WebLabel("0"));
        protractionSliderLabels.put(100000, new WebLabel("1"));
        this.protractionSlider.setLabelTable(protractionSliderLabels);
        this.protractionSlider.setPaintLabels(true);
        this.protractionSlider.addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2)     // set 0 on double click
                    protractionSlider.setValue(0);
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
        this.protractionSlider.addChangeListener(changeEvent -> {
            double protraction = ((double) this.protractionSlider.getValue()) / 100000;
            this.protraction.setValue(protraction);
            this.visualizer.setProtraction(protraction);
        });
        this.addToContentPanel(protractionSlider, 1, 5, 2, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH);

        this.protraction = new WebSpinner(new SpinnerNumberModel(0.0, -1.0, 1.0, 0.1));
        JSpinner.NumberEditor protractionEditor = (JSpinner.NumberEditor) this.protraction.getEditor(); // https://stackoverflow.com/questions/34627998/jspinner-number-editor
        protractionEditor.getFormat().setMaximumFractionDigits(10);
        protractionEditor.getFormat().setRoundingMode(RoundingMode.HALF_UP);
        int protractionWidth = getFontMetrics(this.protraction.getFont()).stringWidth("999.999.999.999");
        this.protraction.setMinimumWidth(protractionWidth);
        this.protraction.setMaximumWidth(protractionWidth);
        this.protraction.addChangeListener(changeEvent -> {
            double protraction = Tools.round(((double) this.protraction.getValue()), 10);
            this.protractionSlider.setValue((int) (protraction * 100000));
            this.visualizer.setProtraction(protraction);
        });
        this.addToContentPanel(this.protraction, 3, 5, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH);

        ///////////////

        this.subNoteDynamicsLabel = new WebLabel("Sub-Note Dynamics:");
        this.subNoteDynamicsLabel.setHorizontalAlignment(WebLabel.RIGHT);
        this.subNoteDynamicsLabel.setPadding(Settings.paddingInDialogs);
        this.addToContentPanel(this.subNoteDynamicsLabel, 0, 6, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH);

        this.subNoteDynamics = new WebCheckBox(false);
        this.subNoteDynamics.setToolTip("Continuous dynamics transitions may be performed note-by-note\n(deactivate this check box; suitable e.g. for piano) or within the\nnote (activate this check box; suitable e.g. for swells on held notes.)");
        this.addToContentPanel(this.subNoteDynamics, 1, 6, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH);

        ///////////////

        this.numericLiteralInputs("Volume:", 1);
        this.numericLiteralInputs("Transition to:", 2);

        this.numericVolume.addChangeListener(changeEvent -> {
            if (this.transitionToButton.isSelected())
                this.visualizer.setVolumeTransitionTo((double) this.numericVolume.getValue(), (double) this.numericTransitionTo.getValue());
            else
                this.visualizer.setVolumeTransitionTo((double) this.numericVolume.getValue(), (double) this.numericVolume.getValue());
        });

        this.numericTransitionTo.addChangeListener(changeEvent -> {
            if (this.transitionToButton.isSelected())
                this.visualizer.setTransitionTo((double) this.numericTransitionTo.getValue());
            else
                this.visualizer.setTransitionTo((double) this.numericVolume.getValue());
        });

        ///////////////

        this.addIdInput(7);
    }

    /**
     * execute the editor dialog
     * @param input the object to be edited via the dialog or null to create a new one
     * @return the dynamics data or null
     */
    @Override
    public DynamicsData edit(DynamicsData input) {
        if (input != null) {
            this.date.setValue(input.startDate);

            if (input.volumeString != null) {
                this.literalVolume.setText(input.volumeString);
                this.volumeMode.setSelected(false);
            } else {
                this.volumeMode.setSelected(true);
                if (input.volume != null) {
                    this.numericVolume.setValue(input.volume);
                }
            }
            if (input.transitionToString != null) {
                this.literalTransitionTo.setText(input.transitionToString);
                this.transitionToButton.setSelected(true);
                this.transitionToMode.setSelected(false);
            } else {
                this.transitionToMode.setSelected(true);
                if (input.transitionTo != null) {
                    this.numericTransitionTo.setValue(input.transitionTo);
                    this.transitionToButton.setSelected(true);
                } else {
                    this.transitionToButton.setSelected(false);
                }
            }

            if (input.curvature != null)
                this.curvature.setValue(input.curvature);

            if (input.protraction != null)
                this.protraction.setValue(input.protraction);

            this.subNoteDynamics.setSelected(input.subNoteDynamics);

            this.id.setText(input.xmlId);
        }
        else {
            this.volumeMode.setSelected(true);
            this.transitionToMode.setSelected(true);
        }

        // the mode switches need an additional initialization for the gripper text
        ((WebLabel) this.volumeMode.getGripper().getFirstComponent()).setText("Switch to " + (this.volumeMode.isSelected() ? "Literal" : "Numeric") + " Input");
        ((WebLabel) this.transitionToMode.getGripper().getFirstComponent()).setText("Switch to " + (this.transitionToMode.isSelected() ? "Literal" : "Numeric") + " Input");

        // initialize the choosers' contents
        this.fullLiteralUpdate(Mpm.DYNAMICS_STYLE, this.literalVolume, this.literalVolumeChooser, this.numericVolume);
        this.fullLiteralUpdate(Mpm.DYNAMICS_STYLE, this.literalTransitionTo, this.literalTransitionToChooser, this.numericTransitionTo);

        // initialize the visualizer
        if (this.transitionToButton.isSelected())
            this.visualizer.setVolumeTransitionTo((double) this.numericVolume.getValue(), (double) this.numericTransitionTo.getValue());
        else
            this.visualizer.setVolumeTransitionTo((double) this.numericVolume.getValue(), (double) this.numericVolume.getValue());

        this.setVisible(true);      // start the dialog

        // after the dialog closed do the following

        if (!this.isOk())           // if input was canceled
            return input;

        String id = this.id.getText();
        if (id.isEmpty())
            id = null;

        DynamicsData output = new DynamicsData();
        output.startDate = Tools.round((double) this.date.getValue(), 10);

        if (this.volumeMode.isSelected())               // numeric volume
            output.volume = Tools.round((double) this.numericVolume.getValue(), 10);
        else {                                          // literal volume
            output.volumeString = this.literalVolume.getText();

            // try to get the numeric value of the literal
//            AbstractDef def = this.style.getDef(output.volumeString);
//            if (def != null)
//                output.volume = ((DynamicsDef) def).getValue();
        }

        if (this.transitionToButton.isSelected()) {
            if (this.transitionToMode.isSelected())     // numeric transition to
                output.transitionTo = Tools.round((double) this.numericTransitionTo.getValue(), 10);
            else {                                      // literal transition to
                output.transitionToString = this.literalTransitionTo.getText();

                // try to get the numeric value of the literal
//                AbstractDef def = this.style.getDef(output.transitionToString);
//                if (def != null)
//                    output.transitionTo = ((DynamicsDef) def).getValue();
            }

//            if ((output.volume == null) || !output.volume.equals(output.transitionTo)) {    // the following makes only sense when the transition transitions to a different value than the start value; to activate, uncomment this line and the above 2 commented blocks
                double curvature = Tools.round((double) this.curvature.getValue(), 10);
                if (curvature > 0.0)
                    output.curvature = curvature;

                double protraction = Tools.round((double) this.protraction.getValue(), 10);
                if (protraction != 0.0)
                    output.protraction = protraction;

                output.subNoteDynamics = this.subNoteDynamics.isSelected();
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
            case "Volume:":
                this.volumeMode = new WebSwitch(false);
                this.numericVolume = new WebSpinner(new SpinnerNumberModel(0, 0, Double.MAX_VALUE, 1));
                this.literalVolume = new WebTextField();
                this.literalVolumeChooser = new WebSplitButton("Choose");
                mode = this.volumeMode;
                numeric = this.numericVolume;
                literal = this.literalVolume;
                literalChooser = this.literalVolumeChooser;

                WebLabel inputLabel = new WebLabel(label);
                inputLabel.setHorizontalAlignment(WebLabel.RIGHT);
                inputLabel.setPadding(Settings.paddingInDialogs);
                this.addToContentPanel(inputLabel, 0, gridBagRow, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH);
                break;
            case "Transition to:":
                this.transitionToMode = new WebSwitch(false);
                this.numericTransitionTo = new WebSpinner(new SpinnerNumberModel(0, 0, Double.MAX_VALUE, 1));
                this.literalTransitionTo = new WebTextField();
                this.literalTransitionToChooser = new WebSplitButton("Choose");
                mode = this.transitionToMode;
                numeric = this.numericTransitionTo;
                literal = this.literalTransitionTo;
                literalChooser = this.literalTransitionToChooser;

                this.transitionToButton = new EditDialogToggleButton(label, new JComponent[]{this.transitionToMode, this.numericTransitionTo, this.literalTransitionTo, this.curvature, this.curvatureSlider, this.curvatureLabel, this.protraction, this.protractionSlider, this.protractionLabel, this.subNoteDynamics, this.subNoteDynamicsLabel}, false);
                this.transitionToButton.addActionListener(actionEvent -> {
                    this.literalTransitionToChooser.setEnabled(this.transitionToButton.isSelected() && (this.style != null));
                    if (this.transitionToButton.isSelected())
                        this.visualizer.setTransitionTo((double) this.numericTransitionTo.getValue());
                    else
                        this.visualizer.setTransitionTo((double) this.numericVolume.getValue());
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
            numeric.setValue(((DynamicsDef) def).getValue());
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
            double value = ((DynamicsDef) style.getDef(keyString)).getValue();
            WebMenuItem item = new WebMenuItem(keyString + " = " + value);
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
        this.updateLiteralField(literal, numeric);                           // check whether the name is present in the style
    }
}
