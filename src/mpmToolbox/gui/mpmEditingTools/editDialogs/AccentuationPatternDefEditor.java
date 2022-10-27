package mpmToolbox.gui.mpmEditingTools.editDialogs;

import com.alee.laf.button.WebButton;
import com.alee.laf.label.WebLabel;
import com.alee.laf.panel.WebPanel;
import com.alee.laf.scroll.WebScrollPane;
import com.alee.laf.spinner.WebSpinner;
import com.alee.laf.text.WebTextField;
import com.alee.managers.style.StyleId;
import meico.mei.Helper;
import meico.mpm.elements.styles.MetricalAccentuationStyle;
import meico.mpm.elements.styles.defs.AccentuationPatternDef;
import mpmToolbox.gui.Settings;
import mpmToolbox.gui.mpmEditingTools.editDialogs.accentuationPatternDef.AccentuationDefComponent;
import mpmToolbox.supplementary.Tools;
import nu.xom.Attribute;

import javax.swing.*;
import java.awt.*;
import java.math.RoundingMode;
import java.util.ArrayList;

/**
 * This class represents the dialog for creating and editing an accentuationPatternDef entry in MPM.
 * @author Axel Berndt
 */
public class AccentuationPatternDefEditor extends EditDialog<AccentuationPatternDef> {
    private WebTextField name;
    private final MetricalAccentuationStyle styleDef;
    private WebSpinner length;
    private WebPanel accPanel;    // the panel with the accentuations
    private final ArrayList<AccentuationDefComponent> accentuations = new ArrayList<>();

    /**
     * constructor
     * @param styleDef
     */
    public AccentuationPatternDefEditor(MetricalAccentuationStyle styleDef) {
        super("Edit Accentuation Pattern Definition");
        this.styleDef = styleDef;
    }

    /**
     * the GUI
     */
    @Override
    public void makeContentPanel() {
        WebLabel nameLabel = new WebLabel("Accentuation Pattern Name:");
        nameLabel.setHorizontalAlignment(WebLabel.RIGHT);
        nameLabel.setPadding(Settings.paddingInDialogs);
        this.addToContentPanel(nameLabel, 0, 0, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH);

        this.name = new WebTextField("accentuation pattern name");
        this.name.setHorizontalAlignment(WebTextField.LEFT);
        this.name.setPadding(Settings.paddingInDialogs);
        this.addToContentPanel(this.name, 1, 0, 3, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH);

        ///////////////////

        WebLabel lengthLabel = new WebLabel("Pattern Length:");
        lengthLabel.setHorizontalAlignment(WebLabel.RIGHT);
        lengthLabel.setPadding(Settings.paddingInDialogs);
        addToContentPanel(lengthLabel, 0, 1, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH);

        this.length = new WebSpinner(new SpinnerNumberModel(4.0, 0.0001, Double.POSITIVE_INFINITY, 1.0));
        JSpinner.NumberEditor valueEditor = (JSpinner.NumberEditor) this.length.getEditor();
        valueEditor.getFormat().setMaximumFractionDigits(10);
        valueEditor.getFormat().setRoundingMode(RoundingMode.HALF_UP);
        int width = getFontMetrics(this.length.getFont()).stringWidth("999.999.999");
        this.length.setMinimumWidth(width);
        this.length.setMaximumWidth(width);
        this.addToContentPanel(this.length, 1, 1, 1, 1, 0.03, 1.0, 5, 5, GridBagConstraints.BOTH);

        WebLabel lengthExplanationLabel = new WebLabel("musical beats according to the underlying time signature");
        lengthExplanationLabel.setHorizontalAlignment(WebLabel.LEFT);
        lengthExplanationLabel.setPadding(Settings.paddingInDialogs);
        this.addToContentPanel(lengthExplanationLabel, 2, 1, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.HORIZONTAL);

        ///////////////////

        WebLabel patterLabel = new WebLabel("Specify Accentuation Pattern");
        patterLabel.setHorizontalAlignment(WebLabel.LEFT);
        patterLabel.setPadding(Settings.paddingInDialogs, 0, Settings.paddingInDialogs, 0);
        this.addToContentPanel(patterLabel, 0, 2, 4, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH);

        ///////////////////

        WebButton plus = new WebButton("+");    // the button to add a new accentuation

        this.accPanel = new WebPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));    // new FlowLayout(align, horizontalSpacing, verticalSpacing)
//        this.accPanel.setBackground(new Color(105, 109, 112));
        WebScrollPane accScrollPane = new WebScrollPane(this.accPanel);
        accScrollPane.setStyleId(StyleId.scrollpaneUndecoratedButtonless);
//        accScrollPane.setHorizontalScrollBarPolicy(WebScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        accScrollPane.setMinimumHeight(370 + plus.getFontSize()*4);
        accScrollPane.setPadding(0, 0, Settings.paddingInDialogs, 0);

        plus.setPadding(180, Settings.paddingInDialogs*2, 180, Settings.paddingInDialogs*2);
        plus.setToolTip("Add an Accentuation");
        plus.addActionListener(actionEvent -> {
            double beat = (this.accentuations.isEmpty()) ? 1.0 : Math.floor(this.accentuations.get(this.accentuations.size() - 1).getBeat()) + 1.0; // initial beat position for the next accentuation should is the next higher integer beat after the beat of the previous accentuation
            this.makeAccentuationComponent(new double[]{beat, 0.0, 0.0, 0.0}, null);
            SwingUtilities.invokeLater(() -> {
                this.accPanel.revalidate();
                this.accPanel.repaint();
            });
        });
        this.accPanel.add(plus);

        this.addToContentPanel(accScrollPane, 0, 3, 4, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH);

        ///////////////////

        this.addIdInput(4);
    }

    /**
     * Open the def editing dialog.
     * @param def the def to be edited or null if a new one should be created
     * @return the def or null
     */
    @Override
    public AccentuationPatternDef edit(AccentuationPatternDef def) {
        if (def != null) {
            this.name.setText(def.getName());
            this.id.setText(def.getId());
            this.length.setValue(def.getLength());

            // read the accentuations into AccentuationDefComponents and add them to the GUI
            for (int i = 0; i < def.size(); ++i) {
                double[] accentuation = def.getAccentuationAttributes(i);
                Attribute idAtt = Helper.getAttribute("id", def.getAccentuationXml(i));
                String idStr = (idAtt == null) ? null : idAtt.getValue();
                this.makeAccentuationComponent(accentuation, idStr);
            }
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
            def = AccentuationPatternDef.createAccentuationPatternDef(this.name.getText(), Tools.round((double) this.length.getValue(), 10));
        else {
            this.styleDef.removeDef(def.getName());
            def = AccentuationPatternDef.createAccentuationPatternDef(this.name.getText(), Tools.round((double) this.length.getValue(), 10));
            this.styleDef.addDef(def);
        }

        // read and add the accentuations to the def
        for (AccentuationDefComponent ac : this.accentuations)
            def.addAccentuation(ac.getBeat(), ac.getValue(), ac.getTransitionFrom(), ac.getTransitionTo(), ac.getId().isEmpty() ? null : ac.getId());

        def.setId(id);

        return def;
    }

    /**
     * create and add an accentuation component to the accPanel
     * @param accentuation
     * @param idStr
     * @return
     */
    private AccentuationDefComponent makeAccentuationComponent(double[] accentuation, String idStr) {
        AccentuationDefComponent ac;
        try {
            ac = new AccentuationDefComponent(accentuation, idStr);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

        if (!this.accentuations.add(ac))
            return null;

        ac.setCloseAction(closeEvent -> {
            this.accentuations.remove(ac);
            ac.getParent().remove(ac);
            SwingUtilities.invokeLater(() -> {
                this.accPanel.revalidate();
                this.accPanel.repaint();
            });
        });
        this.accPanel.add(ac, this.accPanel.getComponentCount() - 1);

        return ac;
    }
}
