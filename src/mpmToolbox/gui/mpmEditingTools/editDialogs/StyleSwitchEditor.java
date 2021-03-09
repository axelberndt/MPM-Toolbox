package mpmToolbox.gui.mpmEditingTools.editDialogs;

import com.alee.extended.button.WebSplitButton;
import com.alee.laf.button.WebButton;
import com.alee.laf.button.WebToggleButton;
import com.alee.laf.menu.WebMenuItem;
import com.alee.laf.menu.WebPopupMenu;
import com.alee.laf.text.WebTextField;
import meico.mpm.Mpm;
import meico.mpm.elements.maps.ArticulationMap;
import meico.mpm.elements.maps.GenericMap;
import meico.mpm.elements.styles.GenericStyle;
import mpmToolbox.gui.Settings;
import mpmToolbox.gui.mpmEditingTools.editDialogs.supplementary.EditDialogToggleButton;
import mpmToolbox.supplementary.Tools;
import nu.xom.Attribute;
import nu.xom.Element;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * This class creates the style switch editing dialog.
 * @author Axel Berndt
 */
public class StyleSwitchEditor extends EditDialog<StyleSwitchEditor.StyleSwitchData> {
    private WebTextField defaultArticulation;
    private WebToggleButton defaultArticulationButton;
    private WebSplitButton defaultArticulationChooser;

    /**
     * constructor
     * @param map the map that gets or holds the style switch
     */
    public StyleSwitchEditor(GenericMap map) {
        super("Edit Style Switch", map);
    }

    /**
     * the GUI
     */
    @Override
    public void makeContentPanel() {
        this.addDateInput(0);
        this.addNameRef("Style Name:", 1, false);

        if (this.map != null) {         // if a map is specified, it provides some context for valid name.ref values
            this.nameRef.getDocument().addDocumentListener(new DocumentListener() {
                @Override
                public void insertUpdate(DocumentEvent e) {
                    updateNameRefField();
                    if (map instanceof ArticulationMap) {
                        updateArticulationChooserContent();
                        updateArticulationField();
                    }
                }

                @Override
                public void removeUpdate(DocumentEvent e) {
                    updateNameRefField();
                    if (map instanceof ArticulationMap) {
                        updateArticulationChooserContent();
                        updateArticulationField();
                    }
                }

                @Override
                public void changedUpdate(DocumentEvent e) {
                    updateNameRefField();
                    if (map instanceof ArticulationMap) {
                        updateArticulationChooserContent();
                        updateArticulationField();
                    }
                }
            });

            // create content for the nameRefChooser
            WebPopupMenu names = this.makeNameRefChooserContent();
            if (names.getComponentCount() > 0)
                this.nameRefChooser.setPopupMenu(names);
            else
                this.nameRefChooser.setToolTip("No reference names available.");

            // the defaultArticulation field if this is an articulation style switch
            if (this.map instanceof ArticulationMap) {
                this.defaultArticulation = new WebTextField();
                this.defaultArticulation.setMinimumWidth(getFontMetrics(this.defaultArticulation.getFont()).stringWidth("wwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwww"));
                this.defaultArticulation.setHorizontalAlignment(WebTextField.LEFT);
                this.defaultArticulation.setPadding(Settings.paddingInDialogs);
                this.defaultArticulation.setToolTip("Make sure that the name really exists!");
                this.defaultArticulation.getDocument().addDocumentListener(new DocumentListener() {
                    @Override
                    public void insertUpdate(DocumentEvent e) {
                        updateArticulationField();
                    }
                    @Override
                    public void removeUpdate(DocumentEvent e) {
                        updateArticulationField();
                    }
                    @Override
                    public void changedUpdate(DocumentEvent e) {
                        updateArticulationField();
                    }
                });
                this.addToContentPanel(this.defaultArticulation, 1, 2, 2, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH);

                this.defaultArticulationChooser = new WebSplitButton("Choose");
                this.defaultArticulationChooser.setHorizontalAlignment(WebButton.CENTER);
                this.defaultArticulationChooser.setPadding(Settings.paddingInDialogs);
                this.updateArticulationChooserContent();
                this.addToContentPanel(this.defaultArticulationChooser, 3, 2, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH);

                this.defaultArticulationButton = new EditDialogToggleButton("Default Articulation:", new JComponent[]{this.defaultArticulation, this.defaultArticulationChooser}, false);
                this.addToContentPanel(this.defaultArticulationButton, 0, 2, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH);
            }
        } else {
            this.nameRefChooser.setToolTip("No reference names available.");
        }

        this.addIdInput(3);
    }

    /**
     * This method builds the popupmenu for the nameRefChooser.
     * @return
     */
    private WebPopupMenu makeNameRefChooserContent() {
        WebPopupMenu styles = new WebPopupMenu();

        String styleType = this.map.getType().replace("Map", "Styles");

        HashMap<String, GenericStyle> globalStyleDefs = (this.map.getGlobalHeader() == null) ? null : this.map.getGlobalHeader().getAllStyleDefs(styleType);
        HashMap<String, GenericStyle> localStyleDefs = (this.map.getLocalHeader() == null) ? null : this.map.getLocalHeader().getAllStyleDefs(styleType);

        if (globalStyleDefs != null) {
            for (Map.Entry<String, GenericStyle> entry : globalStyleDefs.entrySet()) {
                if ((localStyleDefs == null) || !localStyleDefs.containsKey(entry.getKey())) {     // if there is a local styleDef with the same name, it dominates/masks the global ant is ignored here
                    WebMenuItem menuItem = new WebMenuItem(entry.getKey() + " (global)");
                    menuItem.addActionListener(menuItemActionEvent -> this.nameRef.setText(entry.getKey()));
                    styles.add(menuItem);
                }
            }
        }

        if (localStyleDefs != null) {
            for (Map.Entry<String, GenericStyle> entry : localStyleDefs.entrySet()) {
                WebMenuItem menuItem = new WebMenuItem(entry.getKey() + " (local)");
                menuItem.addActionListener(menuItemActionEvent -> this.nameRef.setText(entry.getKey()));
                styles.add(menuItem);
            }
        }

        return styles;
    }

    /**
     * This methods builds the popupmenu for the defaultArticulationChooser.
     * @return
     */
    private WebPopupMenu makeArticulationChooserContent() {
        WebPopupMenu articulations = new WebPopupMenu();
        ArrayList<String> keys = new ArrayList<>();

        if (this.map.getLocalHeader() != null) {
            GenericStyle localStyle = this.map.getLocalHeader().getStyleDef(Mpm.ARTICULATION_STYLE, this.nameRef.getText());
            if (localStyle != null) {
                localStyle.getAllDefs().forEach((key, value) -> keys.add((String) key));
            }
        }

        if (this.map.getGlobalHeader() != null) {
            GenericStyle globalStyle = this.map.getGlobalHeader().getStyleDef(Mpm.ARTICULATION_STYLE, this.nameRef.getText());
            if (globalStyle != null) {
                globalStyle.getAllDefs().forEach((key, value) -> {
                    if (!keys.contains(key)) {          // from the global style we collect those articulations that were not already in the local style
                        keys.add((String) key);
                    }
                });
            }
        }

        for (String key : keys) {
            WebMenuItem articulation = new WebMenuItem(key);
            articulation.addActionListener(actionEvent -> this.defaultArticulation.setText(key));
            articulations.add(articulation);
        }

        return articulations;
    }

    /**
     * This method is to be used when the style (nameRef) changed.
     * It makes sure that the defaultArticulationChooser offers only articulations from the underlying style.
     */
    private void updateArticulationChooserContent() {
        WebPopupMenu articulations = this.makeArticulationChooserContent();
        if (articulations.getComponentCount() > 0) {
            this.defaultArticulationChooser.setPopupMenu(articulations);
            this.defaultArticulationChooser.removeToolTips();
        }
        else {
            this.defaultArticulationChooser.setPopupMenu(null);
            this.defaultArticulationChooser.setToolTip("No articulation definitions found in the specified style.");
        }
    }

    /**
     * This will change the text color to error color if the articulation name cannot be found in the style.
     */
    private void updateArticulationField() {
        boolean isValid = false;

        if (this.map.getLocalHeader() != null) {
            GenericStyle localStyle = this.map.getLocalHeader().getStyleDef(Mpm.ARTICULATION_STYLE, this.nameRef.getText());
            if ((localStyle != null) && (localStyle.getDef(this.defaultArticulation.getText()) != null)) {
                isValid = true;
            }
        }
        if (this.map.getGlobalHeader() != null){
            GenericStyle globalStyle = this.map.getGlobalHeader().getStyleDef(Mpm.ARTICULATION_STYLE, this.nameRef.getText());
            if ((globalStyle != null) && (globalStyle.getDef(this.defaultArticulation.getText()) != null))  {
                isValid = true;
            }
        }

        if (isValid)
            this.defaultArticulation.setForeground(Settings.foregroundColor);
        else
            this.defaultArticulation.setForeground(Settings.errorColor);

        this.defaultArticulation.revalidate();
        this.defaultArticulation.repaint();
    }

    /**
     * This will change the text color to error color if the style in nameRef cannot be found in the style collection.
     */
    @Override
    protected void updateNameRefField() {
        String styleType = this.map.getType().replace("Map", "Styles");
        boolean isValid = false;
        if ((this.map.getLocalHeader() != null) && (this.map.getLocalHeader().getStyleDef(styleType, this.nameRef.getText()) != null))
            isValid = true;
        else if ((this.map.getGlobalHeader() != null) && (this.map.getGlobalHeader().getStyleDef(styleType, this.nameRef.getText()) != null))
            isValid = true;

        if (isValid)
            this.nameRef.setForeground(Settings.foregroundColor);
        else
            this.nameRef.setForeground(Settings.errorColor);

        this.nameRef.revalidate();
        this.nameRef.repaint();
    }

    /**
     * open style switch edit dialog
     * @param input the object to be edited via the dialog or null to create a new one
     * @return style switch data or null
     */
    @Override
    public StyleSwitchData edit(StyleSwitchData input) {
        if (input != null) {
            this.date.setValue(input.date);
            this.nameRef.setText(input.styleName);
            this.id.setText(input.id);

            if (this.defaultArticulation != null) {
                if (input.defaultArticulation != null) {
                    this.defaultArticulation.setText(input.defaultArticulation);
                    this.defaultArticulationButton.setSelected(true);
                } else {
                    this.defaultArticulationButton.setSelected(false);
                }
            }
        }

        this.nameRef.selectAll();

        this.setVisible(true);      // start the dialog

        // after the dialog closed do the following

        if (!this.isOk())           // if dialog was canceled
            return input;           // return the input unchanged

        String id = this.id.getText();
        if (id.isEmpty())
            id = null;

        StyleSwitchData output = new StyleSwitchData();

        output.date = Tools.round((double) this.date.getValue(), 10);
        output.styleName = this.nameRef.getText();
        output.id = id;

        if ((this.defaultArticulation != null) && this.defaultArticulation.isEnabled())
            output.defaultArticulation = this.defaultArticulation.getText();

        return output;
    }

    /**
     * This class holds all information we need to create an MPM style switch.
     */
    public static class StyleSwitchData {
        public double date = 0.0;
        public String styleName = null;
        public String defaultArticulation = null;
        public String id = null;

        /**
         * default constructor
         */
        public StyleSwitchData() {
        }

        /**
         * constructor with XML element parsing
         * @param xml MPM style element
         */
        public StyleSwitchData(Element xml) {
            this.date = Double.parseDouble(xml.getAttributeValue("date"));
            this.styleName = xml.getAttributeValue("name.ref");

            Attribute defaultArticulation = xml.getAttribute("defaultArticulation");
            if (defaultArticulation != null)
                this.defaultArticulation = defaultArticulation.getValue();

            Attribute id = xml.getAttribute("id", "http://www.w3.org/XML/1998/namespace");
            if (id != null)
                this.id = id.getValue();
        }
    }
}
