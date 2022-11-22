package mpmToolbox.gui.mpmEditingTools.editDialogs;

import com.alee.extended.button.WebSplitButton;
import com.alee.laf.button.WebButton;
import com.alee.laf.label.WebLabel;
import com.alee.laf.menu.WebMenuItem;
import com.alee.laf.menu.WebPopupMenu;
import com.alee.laf.panel.WebPanel;
import com.alee.laf.spinner.WebSpinner;
import com.alee.laf.text.WebTextField;
import com.alee.laf.window.WebDialog;
import meico.mpm.elements.maps.GenericMap;
import meico.mpm.elements.styles.GenericStyle;
import mpmToolbox.gui.Settings;
import mpmToolbox.supplementary.Tools;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.math.RoundingMode;
import java.util.UUID;

/**
 * This is a super class for all the editing dialogs.
 * @author Axel Berndt
 */
public abstract class EditDialog<E> extends WebDialog<EditDialog<E>> {
    private boolean ok = false;     // this is set true by clicking the OK button or pressing ENTER
    protected GridBagLayout contentPanelLayout = new GridBagLayout();
    protected WebPanel contentPanel = null;
    protected WebSpinner date = null;
    protected WebTextField nameRef = null;
    protected WebSplitButton nameRefChooser = null;
    protected WebTextField id = null;
    protected GenericMap map = null;
    protected GenericStyle style = null;
    protected double msmDate;     // as the usual date attribute is set on the basis of the performance's PPQ value, this attribute is used to keep the original MSM date


    /**
     * constructor
     * @param title
     */
    public EditDialog(String title) {
        super();
        this.makeBaseGui(title);
    }

    /**
     * constructor to be used for map entry editors
     * @param title
     * @param map the map for which we want to create/edit an entry
     */
    public EditDialog(String title, GenericMap map) {
        super();
        this.map = map;
        this.makeBaseGui(title);
    }

    /**
     * generate the basic gui that all editors have in common
     * @param title
     */
    private void makeBaseGui(String title) {
        this.setTitle(title);
        this.setIconImages(Settings.getIcons(null));
        this.setResizable(Settings.debug);
        this.setModal(true);
        this.initKeyboardShortcuts();

        // content panel
        this.contentPanel = new WebPanel(this.contentPanelLayout);
        this.contentPanel.setPadding(Settings.paddingInDialogs);
        this.makeContentPanel();

        // OK / Cancel panel
        GridBagLayout okPanelLayout = new GridBagLayout();
        WebPanel okPanel = new WebPanel(okPanelLayout);
        okPanel.setPadding(Settings.paddingInDialogs);

        WebButton ok = new WebButton("OK", actionEvent -> {this.ok = true; this.dispose();});
        ok.setHorizontalAlignment(WebButton.CENTER);
        ok.setPadding(Settings.paddingInDialogs*2, Settings.paddingInDialogs, Settings.paddingInDialogs*2, Settings.paddingInDialogs);
        Tools.addComponentToGridBagLayout(okPanel, okPanelLayout, ok, 0, 0, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH, GridBagConstraints.LINE_START);

        WebButton cancel = new WebButton("Cancel", actionEvent -> {this.ok = false; this.dispose();});
        cancel.setHorizontalAlignment(WebButton.CENTER);
        cancel.setPadding(Settings.paddingInDialogs*2, Settings.paddingInDialogs, Settings.paddingInDialogs*2, Settings.paddingInDialogs);
        Tools.addComponentToGridBagLayout(okPanel, okPanelLayout, cancel, 1, 0, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH, GridBagConstraints.LINE_START);

        // put all together
        GridBagLayout layout = new GridBagLayout();
        this.setLayout(layout);
        Tools.addComponentToGridBagLayout(this, layout, this.contentPanel, 0, 0, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH, GridBagConstraints.LINE_START);
        Tools.addComponentToGridBagLayout(this, layout, okPanel, 0, 1, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH, GridBagConstraints.LINE_START);

        this.pack();                                    // fit the size to the content

        this.setLocationRelativeTo(null);
    }

    /**
     * This method must be implemented to create the real editing UI. Consider that the content panel's layout manager is of type GridBagLayout.
     */
    public abstract void makeContentPanel(); //{
//        WebLabel placeholder = new WebLabel("This is a placeholder. You have to implement method makeContentPanel()!");
//        placeholder.setHorizontalAlignment(WebLabel.RIGHT);
//        placeholder.setPadding(Settings.paddingInDialogs);
//        this.addToContentPanel(placeholder, 0, 0, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH);
//    }

    /**
     * a helper method to add components to the content panel
     * @param component
     * @param x
     * @param y
     * @param width
     * @param height
     * @param weightx
     * @param weighty
     * @param ipadx
     * @param ipady
     * @param fill
     */
    public void addToContentPanel(Component component, int x, int y, int width, int height, double weightx, double weighty, int ipadx, int ipady, int fill) {
        Tools.addComponentToGridBagLayout(this.contentPanel, this.contentPanelLayout, component, x, y, width, height, weightx, weighty, ipadx, ipady, fill, GridBagConstraints.LINE_START);
    }

    /**
     * A shorthand to invoke edit() with null as input to signal that it has to create a new object.
     * @return
     */
    public E create() {
        return this.edit(null);
    }

    /**
     * Open the editor dialog and edit the input object or create a new one (if input is null).
     * @param input the object to be edited via the dialog or null to create a new one
     * @return
     */
    public abstract E edit(E input);

    /**
     * this initializes the keyboard shortcuts (ESC, ENTER)
     */
    private void initKeyboardShortcuts() {
        InputMap inputMap = this.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "Cancel");
        this.getRootPane().getActionMap().put("Cancel", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                ok = false;
                dispose();
            }
        });

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "OK");
        this.getRootPane().getActionMap().put("OK", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                ok = true;
                dispose();
            }
        });
    }

    /**
     * read the ok value
     * @return
     */
    public boolean isOk() {
        return this.ok;
    }

    /**
     * This is a prefabricated method to add ID input to the GUI.
     * @param gridBagRow
     */
    protected void addIdInput(int gridBagRow) {
        WebLabel idLabel = new WebLabel("Identifier (optional):");
        idLabel.setHorizontalAlignment(WebLabel.RIGHT);
        idLabel.setPadding(Settings.paddingInDialogs);
        this.addToContentPanel(idLabel, 0, gridBagRow, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH);

        this.id = new WebTextField();
        this.id.setMinimumWidth(getFontMetrics(this.id.getFont()).stringWidth("wwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwww"));
        this.id.setMaximumWidth(this.id.getMinimumWidth());
        this.id.setHorizontalAlignment(WebTextField.LEFT);
        this.id.setPadding(Settings.paddingInDialogs);
        this.addToContentPanel(this.id, 1, gridBagRow, 2, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH);

        WebButton generateId = new WebButton("Generate ID", actionEvent -> this.id.setText(UUID.randomUUID().toString()));
        generateId.setHorizontalAlignment(WebButton.CENTER);
        generateId.setPadding(Settings.paddingInDialogs);
        this.addToContentPanel(generateId, 3, gridBagRow, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH);
    }

    /**
     * This is a prefabricated method to add date input to the GUI.
     * @param gridBagRow
     */
    protected void addDateInput(int gridBagRow) {
        WebLabel dateLabel = new WebLabel("Date:");
        dateLabel.setHorizontalAlignment(WebLabel.RIGHT);
        dateLabel.setPadding(Settings.paddingInDialogs);
        this.addToContentPanel(dateLabel, 0, gridBagRow, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH);

        this.date = new WebSpinner(new SpinnerNumberModel(0, 0, Double.MAX_VALUE, 1));
        int width = getFontMetrics(this.date.getFont()).stringWidth("999.999.999.999.999");
        JSpinner.NumberEditor dateEditor = (JSpinner.NumberEditor) this.date.getEditor();
        dateEditor.getFormat().setMaximumFractionDigits(10);
        dateEditor.getFormat().setRoundingMode(RoundingMode.HALF_UP);
        this.date.setMinimumWidth(width);
        this.date.setMaximumWidth(width);
        this.addToContentPanel(this.date, 1, gridBagRow, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH);

        WebLabel dateExplanation = new WebLabel("ticks");
        dateExplanation.setHorizontalAlignment(WebLabel.LEFT);
        dateExplanation.setPadding(Settings.paddingInDialogs);
        this.addToContentPanel(dateExplanation, 2, gridBagRow, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.HORIZONTAL);
    }

    /**
     * This provides an interface to set the date externally.
     * @param date
     */
    public void setDate(double date) {
        if (this.date != null)
            this.date.setValue(date);
    }

    /**
     * While this.date holds the date according to the performance's PPQ, this.msmDate holds the date according to the MSM's PPQ.
     * @param date
     */
    public void setMsmDate(double date) {
        this.msmDate = date;
    }



    /**
     * This is a prefabricated method to add name.ref input to the GUI.
     * @param title
     * @param gridBagRow
     * @param checkAgainstStyles set true to activate automatic check if the name in this field exists in localStyle or globalStyle and set the text color accordingly.
     */
    protected void addNameRef(String title, int gridBagRow, boolean checkAgainstStyles) {
        WebLabel nameRefLabel = new WebLabel(title);
        nameRefLabel.setHorizontalAlignment(WebLabel.RIGHT);
        nameRefLabel.setPadding(Settings.paddingInDialogs);
        this.addToContentPanel(nameRefLabel, 0, gridBagRow, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH);

        this.nameRef = new WebTextField();
        this.nameRef.setMinimumWidth(getFontMetrics(this.nameRef.getFont()).stringWidth("999.999.999.999.999"));
        this.nameRef.setHorizontalAlignment(WebTextField.LEFT);
        this.nameRef.setPadding(Settings.paddingInDialogs);
        this.nameRef.setToolTip("Make sure that the name really exists!");
        this.addToContentPanel(this.nameRef, 1, gridBagRow, 2, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH);

        this.nameRefChooser = new WebSplitButton("Choose");
        this.nameRefChooser.setHorizontalAlignment(WebButton.CENTER);
        this.nameRefChooser.setPadding(Settings.paddingInDialogs);
        this.addToContentPanel(this.nameRefChooser, 3, gridBagRow, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH);

        if (checkAgainstStyles)
            makeNameRefCheckAgainstStyles();
    }

    /**
     * This adds functionality to the nameRef textfield. If its content changes it will automatically
     * check against the localStyle and globalStyle if the name exists and color the text accordingly
     * via method updateNameRefField().
     */
    private void makeNameRefCheckAgainstStyles() {
        this.nameRef.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent documentEvent) {
                updateNameRefField();
            }
            @Override
            public void removeUpdate(DocumentEvent documentEvent) {
                updateNameRefField();
            }
            @Override
            public void changedUpdate(DocumentEvent documentEvent) {
                updateNameRefField();
            }
        });
    }

    /**
     * format the nameRef text according to whether the name exists in the underlying style or not
     */
    protected void updateNameRefField() {
        // check if the name exists in the linked style
        if ((this.style != null) && (this.style.getDef(this.nameRef.getText()) != null))
            this.nameRef.setForeground(Settings.foregroundColor);
        else
            this.nameRef.setForeground(Settings.errorColor);

        this.nameRef.revalidate();
        this.nameRef.repaint();
    }

    /**
     * This method takes the given style and puts all its defs' names into the nameRefChooser.
     * @param style
     */
    protected void fillNameRefChooser(GenericStyle style) {
        if (style == null) {
            this.nameRefChooser.setPopupMenu(null);
            this.nameRefChooser.setEnabled(false);
            this.nameRefChooser.setToolTip("No (valid) style defined for this date.\nMake sure there is a style switch in the map before or at this date.");
            return;
        }

        WebPopupMenu nameRefs = new WebPopupMenu();
        for (Object key : style.getAllDefs().keySet()) {
            WebMenuItem item = new WebMenuItem((String) key);
            item.addActionListener(menuItemActionEvent -> this.nameRef.setText(item.getText()));
            nameRefs.add(item);
        }

        this.nameRefChooser.setEnabled(true);
        this.nameRefChooser.setPopupMenu(nameRefs);
        this.nameRefChooser.setToolTip("from style \"" + style.getName() + "\"");
    }

    /**
     * Invoke this method in a map element dialog to
     * 1. get the style that applies to the element's date,
     * 2. fill the nameRefChooser with the style's def names
     * 3. update the text color in the nameRef field according to whether the name is in the style or not.
     * @param styleType use the constants in class Mpm
     */
    protected void fullNameRefUpdate(String styleType) {
        if ((this.map == null) || (this.date == null))
            return;
        double date = Tools.round((double) this.date.getValue(), 10);
        this.style = this.map.getStyleAt(date, styleType);  // update the underlying style
        this.fillNameRefChooser(this.style);                // update the nameRefChooser's content
        this.updateNameRefField();                          // check whether the name is present in the style
    }
}
