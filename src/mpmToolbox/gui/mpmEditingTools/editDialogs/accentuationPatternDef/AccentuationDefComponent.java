package mpmToolbox.gui.mpmEditingTools.editDialogs.accentuationPatternDef;

import com.alee.laf.button.WebButton;
import com.alee.laf.button.WebToggleButton;
import com.alee.laf.label.WebLabel;
import com.alee.laf.panel.WebPanel;
import com.alee.laf.slider.WebSlider;
import com.alee.laf.spinner.WebSpinner;
import com.alee.laf.text.WebTextField;
import mpmToolbox.gui.Settings;
import mpmToolbox.gui.mpmEditingTools.editDialogs.supplementary.EditDialogToggleButton;
import mpmToolbox.gui.mpmEditingTools.editDialogs.visualizers.AccentuationVisualizer;
import mpmToolbox.supplementary.Tools;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.math.RoundingMode;
import java.util.Hashtable;
import java.util.UUID;

/**
 * This component is used to create accentuation editing components in class AccentuationPatternDefEditDialog.
 * @author Axel Berndt
 */
public class AccentuationDefComponent extends WebPanel {
    private static final int BEAT = 0;
    private static final int VALUE = 1;
    private static final int TRANSITION_FROM = 2;
    private static final int TRANSITION_TO = 3;

    private final int SPINNER_WIDTH;

    private final WebButton delete = new WebButton("<html><b>&times;</b></html>");
    private final WebTextField id = new WebTextField();
    private AccentuationVisualizer visualizer;
    private final double[] accentuation;

    /**
     * constructor
     * @param accentuation a array of the form [beat, value, transition.from, transition.to]
     * @param id
     */
    public AccentuationDefComponent(double[] accentuation, String id) throws Exception {
        super(new GridBagLayout());

        if (accentuation.length < 4)
            throw new Exception("Insufficient accentuation data to create an AccentuationDefComponent. The input array should provide at 4 double values in the form {beat, value, transition.from, transition.to}.");

//        Color transparent = new Color(0, 0, 0, 0);
//        this.setBackground(transparent);          // make the background totally transparent
        this.setBorder(BorderFactory.createCompoundBorder(new LineBorder(Color.BLACK), new EmptyBorder(Settings.paddingInDialogs, Settings.paddingInDialogs, Settings.paddingInDialogs, Settings.paddingInDialogs)));

        this.accentuation = accentuation;
        if (id != null)
            this.id.setText(id);
        this.SPINNER_WIDTH = this.getFontMetrics(this.getFont()).stringWidth("999.999.99");

        // the panel with the beat input and delete button
        WebPanel topPanel = this.makeTopPanel();
        Tools.addComponentToGridBagLayout(this, (GridBagLayout) this.getLayout(), topPanel, 0, 0, 3, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH, GridBagConstraints.LINE_START);

        this.visualizer = new AccentuationVisualizer(this.accentuation[1], this.accentuation[2], this.accentuation[3]);
        Tools.addComponentToGridBagLayout(this, (GridBagLayout) this.getLayout(), this.visualizer, 0, 1, 3, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH, GridBagConstraints.LINE_START);

        // the panel with the accentuation sliders
        WebPanel sliderPanel = this.makeSliderPanel();
        Tools.addComponentToGridBagLayout(this, (GridBagLayout) this.getLayout(), sliderPanel, 0, 2, 3, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH, GridBagConstraints.LINE_START);

        // ID panel
        WebPanel idPanel = this.makeIdPanel();
        Tools.addComponentToGridBagLayout(this, (GridBagLayout) this.getLayout(), idPanel, 0, 3, 3, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH, GridBagConstraints.LINE_START);
    }

    /**
     * a helper method for the change listeners of the sliders and spinners that have to update the underlying values as well as their counterparts
     * @param accentuationIndex the part of the accentuation that must be updated
     * @param value the update value
     * @param counterpart either the slider or the spinner that has to be updated
     */
    private void updateValue(int accentuationIndex, double value, Component counterpart) {
        if (this.accentuation[accentuationIndex] == value)  // this is to prevent that we run into an endless loop
            return;

        this.accentuation[accentuationIndex] = value;

        if (counterpart instanceof WebSlider)
            ((WebSlider) counterpart).setValue((int) (value * 100000));
        else if (counterpart instanceof WebSpinner)
            ((WebSpinner) counterpart).setValue(value);
    }

    /**
     * create the slider panel
     * @return
     */
    private WebPanel makeSliderPanel() {
        WebPanel sliderPanel = new WebPanel(new GridBagLayout());
//        sliderPanel.setBackground(transparent);

        // sliders
        WebSlider valueSlider = this.makeSlider(VALUE);
        Tools.makeSliderSetToClickPosition(valueSlider);
        valueSlider.addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2)     // set 0 on double click
                    valueSlider.setValue(0);
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

        WebSlider transitionFromSlider = this.makeSlider(TRANSITION_FROM);
        Tools.makeSliderSetToClickPosition(transitionFromSlider);
        transitionFromSlider.addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2)     // set 0 on double click
                    transitionFromSlider.setValue(0);
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

        WebSlider transitionToSlider = this.makeSlider(TRANSITION_TO);
        Tools.makeSliderSetToClickPosition(transitionToSlider);
        transitionToSlider.addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2)     // set 0 on double click
                    transitionToSlider.setValue(0);
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

        Tools.addComponentToGridBagLayout(sliderPanel, (GridBagLayout) sliderPanel.getLayout(), valueSlider, 0, 0, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH, GridBagConstraints.LINE_START);    // value
        Tools.addComponentToGridBagLayout(sliderPanel, (GridBagLayout) sliderPanel.getLayout(), transitionFromSlider, 1, 0, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH, GridBagConstraints.LINE_START);    // transition.from
        Tools.addComponentToGridBagLayout(sliderPanel, (GridBagLayout) sliderPanel.getLayout(), transitionToSlider, 2, 0, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH, GridBagConstraints.LINE_START);    // transition.to

        // numeric spinners below the sliders
        WebSpinner valueSpinner = new WebSpinner(new SpinnerNumberModel(this.getValue(), -1.0, 1.0, 0.1));
        valueSpinner.setMinimumWidth(this.SPINNER_WIDTH);
        valueSpinner.setMaximumWidth(this.SPINNER_WIDTH);
        WebSpinner transitionFromSpinner = new WebSpinner(new SpinnerNumberModel(this.getTransitionFrom(), -1.0, 1.0, 0.1));
        transitionFromSpinner.setMinimumWidth(this.SPINNER_WIDTH);
        transitionFromSpinner.setMaximumWidth(this.SPINNER_WIDTH);
        WebSpinner transitionToSpinner = new WebSpinner(new SpinnerNumberModel(this.getTransitionTo(), -1.0, 1.0, 0.1));
        transitionToSpinner.setMinimumWidth(this.SPINNER_WIDTH);
        transitionToSpinner.setMaximumWidth(this.SPINNER_WIDTH);
        Tools.addComponentToGridBagLayout(sliderPanel, (GridBagLayout) sliderPanel.getLayout(), valueSpinner, 0, 1, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH, GridBagConstraints.LINE_START);    // value
        Tools.addComponentToGridBagLayout(sliderPanel, (GridBagLayout) sliderPanel.getLayout(), transitionFromSpinner, 1, 1, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH, GridBagConstraints.LINE_START);    // transition.from
        Tools.addComponentToGridBagLayout(sliderPanel, (GridBagLayout) sliderPanel.getLayout(), transitionToSpinner, 2, 1, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH, GridBagConstraints.LINE_START);    // transition.to

        // on/off switches for transitionFrom and transitionTo
        EditDialogToggleButton transitionFromButton = new EditDialogToggleButton("Off", new JComponent[]{transitionFromSlider, transitionFromSpinner}, false);
        transitionFromButton.addChangeListener(changeEvent -> {
            if (!transitionFromButton.isSelected())
                transitionFromSlider.setValue(valueSlider.getValue());
            transitionFromButton.setText(transitionFromButton.isSelected() ? "On" : "Off");
        });
        transitionFromButton.setHorizontalAlignment(WebToggleButton.CENTER);
        transitionFromButton.setPadding(1);
        transitionFromButton.setSelected(this.accentuation[TRANSITION_FROM] != this.accentuation[VALUE]);
        Tools.addComponentToGridBagLayout(sliderPanel, (GridBagLayout) sliderPanel.getLayout(), transitionFromButton, 1, 2, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH, GridBagConstraints.CENTER);    // transition.from

        EditDialogToggleButton transitionToButton = new EditDialogToggleButton("Off", new JComponent[]{transitionToSlider, transitionToSpinner}, false);
        transitionToButton.addChangeListener(changeEvent -> {
            if (!transitionToButton.isSelected())
                transitionToSlider.setValue(transitionFromSlider.getValue());
            transitionToButton.setText(transitionToButton.isSelected() ? "On" : "Off");
        });
        transitionToButton.setHorizontalAlignment(WebToggleButton.CENTER);
        transitionToButton.setPadding(1);
        transitionToButton.setSelected(this.accentuation[TRANSITION_TO] != this.accentuation[TRANSITION_FROM]);
        Tools.addComponentToGridBagLayout(sliderPanel, (GridBagLayout) sliderPanel.getLayout(), transitionToButton, 2, 2, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH, GridBagConstraints.CENTER);    // transition.to

        // interactions of sliders and spinners
        valueSlider.addChangeListener(changeEvent -> {
            this.updateValue(VALUE, Tools.round(((double) valueSlider.getValue()) / 100000.0, 10), valueSpinner);
            if (!transitionFromButton.isSelected())
                transitionFromSlider.setValue(valueSlider.getValue());
            this.visualizer.setValue(this.getValue());
        });
        valueSpinner.addChangeListener(changeEvent -> {
            this.updateValue(VALUE, Tools.round((double) valueSpinner.getValue(), 10), valueSlider);
            if (!transitionFromButton.isSelected())
                transitionFromSpinner.setValue(valueSpinner.getValue());
            this.visualizer.setValue(this.getValue());
        });

        transitionFromSlider.addChangeListener(changeEvent -> {
            this.updateValue(TRANSITION_FROM, Tools.round(((double) transitionFromSlider.getValue()) / 100000.0, 10), transitionFromSpinner);
            if (!transitionToButton.isSelected())
                transitionToSlider.setValue(transitionFromSlider.getValue());
            this.visualizer.setTransitionFrom(this.getTransitionFrom());
        });
        transitionFromSpinner.addChangeListener(changeEvent -> {
            this.updateValue(TRANSITION_FROM, Tools.round((double) transitionFromSpinner.getValue(), 10), transitionFromSlider);
            if (!transitionToButton.isSelected())
                transitionToSpinner.setValue(transitionFromSpinner.getValue());
            this.visualizer.setTransitionFrom(this.getTransitionFrom());
        });

        transitionToSlider.addChangeListener(changeEvent -> {
            this.updateValue(TRANSITION_TO, Tools.round(((double) transitionToSlider.getValue()) / 100000.0, 10), transitionToSpinner);
            this.visualizer.setTransitionTo(this.getTransitionTo());
        });
        transitionToSpinner.addChangeListener(changeEvent -> {
            this.updateValue(TRANSITION_TO, Tools.round((double) transitionToSpinner.getValue(), 10), transitionToSlider);
            this.visualizer.setTransitionTo(this.getTransitionTo());
        });

        return sliderPanel;
    }

    /**
     * This creates a slider for the value, transition.from and transition.to settings.
     * @param accentuationIndex 1 = value, 2 = transition.from, 3 = transition.to
     * @return
     */
    private WebSlider makeSlider(int accentuationIndex) {
        WebSlider slider = new WebSlider(WebSlider.VERTICAL, -100000, 100000, (int)(this.accentuation[accentuationIndex] * 100000));
        slider.setPadding(Settings.paddingInDialogs, 0, Settings.paddingInDialogs, 0);
//        slider.setBackground(new Color(0, 0, 0, 0));

        slider.setMajorTickSpacing(100000);
        slider.setMinorTickSpacing(50000);
        slider.setPaintTicks(true);

        Hashtable<Integer, WebLabel> labels = new Hashtable<>();
        labels.put(-100000, new WebLabel("-1"));
        labels.put(0, new WebLabel("0"));
        labels.put(100000, new WebLabel("1"));
        slider.setLabelTable(labels);
        slider.setPaintLabels(true);

        switch (accentuationIndex) {
//            case BEAT:
//                tooltip = "beat";
//                break;
            case VALUE:
                slider.addToolTip("Accentuation on the specified beat position.");
                break;
            case TRANSITION_FROM:
                slider.addToolTip("<html><center>Transition From<br>Accentuation value between this and the next accentuation.</center></html>");
                break;
            case TRANSITION_TO:
                slider.addToolTip("<html><center>Transition To<br>Accentuation value between this and the next accentuation (linear transition from &#8594; to)</center></html>");
                break;
        }

        return slider;
    }

    /**
     * the ID panel on the bottom
     * @return
     */
    private WebPanel makeIdPanel() {
        WebPanel idPanel = new WebPanel(new GridBagLayout());
        idPanel.setPadding(Settings.paddingInDialogs, 0, 0, 0);

        WebLabel idLabel = new WebLabel("ID:");
        idLabel.setHorizontalAlignment(WebTextField.LEFT);
//        idLabel.setPadding(Settings.paddingInDialogs, 0, Settings.paddingInDialogs, 0);
//        idLabel.setBackground(transparent);
        Tools.addComponentToGridBagLayout(idPanel, (GridBagLayout) idPanel.getLayout(), idLabel, 0, 0, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH, GridBagConstraints.LINE_START);

        this.id.setMaximumWidth(this.getFontMetrics(this.id.getFont()).stringWidth("wwwwwwwwwww"));
        this.id.setMinimumWidth(this.getFontMetrics(this.id.getFont()).stringWidth("wwwwwwwwwww"));
        Tools.addComponentToGridBagLayout(idPanel, (GridBagLayout) idPanel.getLayout(), this.id, 1, 0, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH, GridBagConstraints.LINE_START);

        WebButton generateId = new WebButton("Generate");
        generateId.setHorizontalAlignment(WebButton.CENTER);
        generateId.addActionListener(actionEvent -> this.id.setText(UUID.randomUUID().toString()));
        Tools.addComponentToGridBagLayout(idPanel, (GridBagLayout) idPanel.getLayout(), generateId, 2, 0, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH, GridBagConstraints.LINE_START);

        return idPanel;
    }

    /**
     * the panel with the beat input and delete button
     * @return
     */
    private WebPanel makeTopPanel() {
        WebPanel topPanel = new WebPanel(new GridBagLayout());
//        topPanel.setBackground(transparent);

        // beat label
        WebLabel beatLabel = new WebLabel("Musical Beat:");
        beatLabel.setHorizontalAlignment(WebTextField.LEFT);
//        beatLabel.setPadding(Settings.paddingInDialogs, 0, Settings.paddingInDialogs, 0);
//        beatLabel.setBackground(transparent);
        Tools.addComponentToGridBagLayout(topPanel, (GridBagLayout) topPanel.getLayout(), beatLabel, 0, 0, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH, GridBagConstraints.LINE_START);

        // the beat spinner
        WebSpinner beatSpinner = new WebSpinner(new SpinnerNumberModel(this.getBeat(), 1.0, Double.POSITIVE_INFINITY, 0.5));
        beatSpinner.setMinimumWidth(this.SPINNER_WIDTH);
        beatSpinner.setMaximumWidth(this.SPINNER_WIDTH);
        JSpinner.NumberEditor beatEditor = (JSpinner.NumberEditor) beatSpinner.getEditor();
        beatEditor.getFormat().setMaximumFractionDigits(10);
        beatEditor.getFormat().setRoundingMode(RoundingMode.HALF_UP);
        beatSpinner.setToolTipText("the musical beat to be accentuated");
        beatSpinner.addChangeListener(changeEvent -> this.accentuation[BEAT] = Tools.round((double) beatSpinner.getValue(), 10));
        Tools.addComponentToGridBagLayout(topPanel, (GridBagLayout) topPanel.getLayout(), beatSpinner, 1, 0, 1, 1, 100.0, 1.0, 0, 0, GridBagConstraints.BOTH, GridBagConstraints.LINE_START);

        // delete button
        this.delete.addToolTip("delete accentuation");
//        this.delete.setPadding(Settings.paddingInDialogs);
        Tools.addComponentToGridBagLayout(topPanel, (GridBagLayout) topPanel.getLayout(), this.delete, 2, 0, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH, GridBagConstraints.LINE_START);

        return topPanel;
    }

    public void setCloseAction(ActionListener actionListener) {
        this.delete.addActionListener(actionListener);
    }

    /**
     * get all accentuation values formatted in an array for further use in the MPM API
     * @return
     */
    public double[] getAccentuation() {
        return this.accentuation;
    }

    /**
     * get the beat at which the accentuation is placed
     * @return
     */
    public double getBeat() {
        return this.accentuation[BEAT];
    }

    /**
     * get the accentuation value
     * @return
     */
    public double getValue() {
        return this.accentuation[VALUE];
    }

    /**
     * get the value for the transition.from attribute
     * @return
     */
    public double getTransitionFrom() {
        return this.accentuation[TRANSITION_FROM];
    }

    /**
     * get the value for the transition.to attribute
     * @return
     */
    public double getTransitionTo() {
        return this.accentuation[TRANSITION_TO];
    }

    /**
     * get the id of the accentuation
     * @return the id string or null
     */
    public String getId() {
        return this.id.getText();
    }
}
