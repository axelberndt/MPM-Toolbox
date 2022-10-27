package mpmToolbox.gui.mpmEditingTools.editDialogs.ornamentDef;

import com.alee.laf.button.WebButton;
import com.alee.laf.label.WebLabel;
import com.alee.laf.panel.WebPanel;
import com.alee.laf.slider.WebSlider;
import com.alee.laf.spinner.WebSpinner;
import com.alee.laf.text.WebTextField;
import mpmToolbox.gui.Settings;
import mpmToolbox.gui.mpmEditingTools.editDialogs.visualizers.DynamicsGradientVisualizer;
import mpmToolbox.supplementary.Tools;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Hashtable;
import java.util.UUID;

/**
 * This class represents the dynamicsGradient sub-panel in the editor dialog of ornamentsDef.
 * @author Axel Berndt
 */
public class DynamicsGradientPanel extends WebPanel {
    private final int SPINNER_WIDTH = this.getFontMetrics(this.getFont()).stringWidth("999.999.99");
    private final WebSlider transitionFromSlider;
    private final WebSpinner transitionFromSpinner;
    private final WebSlider transitionToSlider;
    private final WebSpinner transitionToSpinner;
    private final WebTextField id = new WebTextField();
    private final WebButton generateId = new WebButton("Generate");
    private final WebLabel idLabel= new WebLabel("ID:");

    /**
     * constructor
     */
    public DynamicsGradientPanel() {
        super();
        GridBagLayout layout = new GridBagLayout();
        this.setLayout(layout);

        this.setPadding(Settings.paddingInDialogs);
        this.setBorder(BorderFactory.createCompoundBorder(new LineBorder(Color.BLACK), new EmptyBorder(Settings.paddingInDialogs, Settings.paddingInDialogs, Settings.paddingInDialogs, Settings.paddingInDialogs)));

        // visualizer
        DynamicsGradientVisualizer dynamicsGradientVisualizer = new DynamicsGradientVisualizer();
        Tools.addComponentToGridBagLayout(this, layout, dynamicsGradientVisualizer, 3, 0, 1, 1, 20.0, 1.0, 0, 0, GridBagConstraints.BOTH, GridBagConstraints.LINE_START);

        // transitionFrom slider
        this.transitionFromSlider = new WebSlider(WebSlider.VERTICAL, -100000, 100000, 0);
        this.transitionFromSlider.setPadding(0, 0, 0, 0);
        this.transitionFromSlider.setMinimumHeight(Settings.getDefaultFontSize() * 3);
        this.transitionFromSlider.setPreferredHeight(this.transitionFromSlider.getMinimumHeight());
        this.transitionFromSlider.setMaximumHeight(this.transitionFromSlider.getMinimumHeight());
        this.transitionFromSlider.setMajorTickSpacing(100000);
        this.transitionFromSlider.setMinorTickSpacing(100000);
        this.transitionFromSlider.setPaintTicks(true);
        Hashtable<Integer, WebLabel> transitionFromSliderLabels = new Hashtable<>();
        transitionFromSliderLabels.put(-100000, new WebLabel("-1"));
        transitionFromSliderLabels.put(0, new WebLabel("0"));
        transitionFromSliderLabels.put(100000, new WebLabel("1"));
        this.transitionFromSlider.setLabelTable(transitionFromSliderLabels);
        this.transitionFromSlider.setPaintLabels(true);
        Tools.makeSliderSetToClickPosition(this.transitionFromSlider);
        this.transitionFromSlider.addMouseListener(new MouseListener() {
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
        Tools.addComponentToGridBagLayout(this, layout, this.transitionFromSlider, 1, 0, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH, GridBagConstraints.LINE_START);

        this.transitionFromSpinner = new WebSpinner(new SpinnerNumberModel(0, -1.0, 1.0, 0.1));
        this.transitionFromSpinner.setMinimumWidth(this.SPINNER_WIDTH);
        this.transitionFromSpinner.setMaximumWidth(this.SPINNER_WIDTH);
        Tools.addComponentToGridBagLayout(this, layout, this.transitionFromSpinner, 2, 0, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH, GridBagConstraints.LINE_START);

        this.transitionFromSlider.addChangeListener(changeEvent -> {
            double value = Tools.round(((double) this.transitionFromSlider.getValue()) / 100000.0, 10);
            this.transitionFromSpinner.setValue(value);
            dynamicsGradientVisualizer.setTransitionFrom(value);
        });
        this.transitionFromSpinner.addChangeListener(changeEvent -> {
            double value = Tools.round(((double) this.transitionFromSpinner.getValue()), 10);
            dynamicsGradientVisualizer.setTransitionFrom(value);
            this.transitionFromSlider.setValue((int) Math.round(value * 100000));
        });

        // transitionTo slider
        this.transitionToSlider = new WebSlider(WebSlider.VERTICAL, -100000, 100000, 0);
        this.transitionToSlider.setPadding(0, 0, 0, 0);
        this.transitionToSlider.setMinimumHeight(Settings.getDefaultFontSize() * 3);
        this.transitionToSlider.setPreferredHeight(this.transitionToSlider.getMinimumHeight());
        this.transitionToSlider.setMaximumHeight(this.transitionToSlider.getMinimumHeight());
        this.transitionToSlider.setMajorTickSpacing(100000);
        this.transitionToSlider.setMinorTickSpacing(100000);
        this.transitionToSlider.setPaintTicks(true);
        Hashtable<Integer, WebLabel> transitionToSliderLabels = new Hashtable<>();
        transitionToSliderLabels.put(-100000, new WebLabel("-1"));
        transitionToSliderLabels.put(0, new WebLabel("0"));
        transitionToSliderLabels.put(100000, new WebLabel("1"));
        this.transitionToSlider.setLabelTable(transitionToSliderLabels);
        this.transitionToSlider.setPaintLabels(true);
        Tools.makeSliderSetToClickPosition(this.transitionToSlider);
        this.transitionToSlider.addMouseListener(new MouseListener() {
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
        this.transitionToSlider.addChangeListener(changeEvent -> {
            dynamicsGradientVisualizer.setTransitionTo(Tools.round(((double) this.transitionToSlider.getValue()) / 100000.0, 10));
        });
        Tools.addComponentToGridBagLayout(this, layout, this.transitionToSlider, 5, 0, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH, GridBagConstraints.LINE_START);

        this.transitionToSpinner = new WebSpinner(new SpinnerNumberModel(0, -1.0, 1.0, 0.1));
        this.transitionToSpinner.setMinimumWidth(this.SPINNER_WIDTH);
        this.transitionToSpinner.setMaximumWidth(this.SPINNER_WIDTH);
        Tools.addComponentToGridBagLayout(this, layout, this.transitionToSpinner, 4, 0, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH, GridBagConstraints.LINE_START);

        this.transitionToSlider.addChangeListener(changeEvent -> {
            double value = Tools.round(((double) this.transitionToSlider.getValue()) / 100000.0, 10);
            this.transitionToSpinner.setValue(value);
            dynamicsGradientVisualizer.setTransitionTo(value);
        });
        this.transitionToSpinner.addChangeListener(changeEvent -> {
            double value = Tools.round(((double) this.transitionToSpinner.getValue()), 10);
            dynamicsGradientVisualizer.setTransitionTo(value);
            this.transitionToSlider.setValue((int) Math.round(value * 100000));
        });

        // id input
        Tools.addComponentToGridBagLayout(this, layout, this.makeIdPanel(), 0, 1, 6, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH, GridBagConstraints.LINE_START);
    }

    /**
     * generate an ID panel for the sub-elements of the ornamentDef
     * @return
     */
    private WebPanel makeIdPanel() {
        GridBagLayout layout = new GridBagLayout();
        WebPanel idPanel = new WebPanel(layout);
        idPanel.setPadding(Settings.paddingInDialogs, 0, 0, 0);

        this.idLabel.setHorizontalAlignment(WebTextField.LEFT);
//        idLabel.setPadding(Settings.paddingInDialogs, 0, Settings.paddingInDialogs, 0);
//        idLabel.setBackground(transparent);
        Tools.addComponentToGridBagLayout(idPanel, layout, this.idLabel, 0, 0, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH, GridBagConstraints.LINE_START);

        this.id.setMaximumWidth(this.getFontMetrics(this.id.getFont()).stringWidth("wwwwwwwwwww"));
        this.id.setMinimumWidth(this.getFontMetrics(this.id.getFont()).stringWidth("wwwwwwwwwww"));
        Tools.addComponentToGridBagLayout(idPanel, layout, this.id, 1, 0, 1, 1, 8.0, 1.0, 0, 0, GridBagConstraints.BOTH, GridBagConstraints.LINE_START);

        this.generateId.setHorizontalAlignment(WebButton.CENTER);
        this.generateId.addActionListener(actionEvent -> this.id.setText(UUID.randomUUID().toString()));
        Tools.addComponentToGridBagLayout(idPanel, layout, this.generateId, 2, 0, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH, GridBagConstraints.LINE_START);

        return idPanel;
    }

    /**
     * get transitionFrom value
     * @return
     */
    public double getTransitionFrom() {
        return Tools.round(((double) this.transitionFromSpinner.getValue()), 10);
    }

    /**
     * set the transitionFrom value
     * @param value
     */
    public void setTransitionFrom(double value) {
        this.transitionFromSpinner.setValue(value);
    }

    /**
     * get transitionTo value
     * @return
     */
    public double getTransitionTo() {
        return Tools.round(((double) this.transitionToSpinner.getValue()), 10);
    }

    /**
     * set the transitionTo value
     * @param value
     */
    public void setTransitionTo(double value) {
        this.transitionToSpinner.setValue(value);
    }

    /**
     * get the id
     * @return
     */
    public String getId() {
        return this.id.getText();
    }

    /**
     * set the id
     * @param id
     */
    public void setId(String id) {
        this.id.setText(id);
    }

    /**
     * enable/disable the components
     * @param enabled true if this component should be enabled, false otherwise
     */
    @Override
    public void setEnabled(boolean enabled) {
        this.transitionFromSlider.setEnabled(enabled);
        this.transitionFromSpinner.setEnabled(enabled);
        this.transitionToSlider.setEnabled(enabled);
        this.transitionToSpinner.setEnabled(enabled);
        this.id.setEnabled(enabled);
        this.generateId.setEnabled(enabled);
        this.idLabel.setEnabled(enabled);
    }
}
