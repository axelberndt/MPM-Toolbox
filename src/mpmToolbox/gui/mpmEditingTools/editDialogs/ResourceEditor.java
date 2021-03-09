package mpmToolbox.gui.mpmEditingTools.editDialogs;

import com.alee.laf.button.WebButton;
import com.alee.laf.label.WebLabel;
import com.alee.laf.text.WebTextField;
import meico.mpm.elements.metadata.RelatedResource;
import mpmToolbox.gui.Settings;

import java.awt.*;

/**
 * This class represents the dialog for creating and editing a related resource entry in MPM.
 * @author Axel Berndt
 */
public class ResourceEditor extends EditDialog<RelatedResource> {
    private WebTextField uri;
    private WebTextField type;

    /**
     * constructor
     */
    public ResourceEditor() {
        super("Edit Related Resource");
    }

    /**
     * the GUI
     */
    @Override
    public void makeContentPanel() {
        WebLabel uriLabel = new WebLabel("Uniform Resource Identifier (URI):");
        uriLabel.setHorizontalAlignment(WebLabel.RIGHT);
        uriLabel.setPadding(Settings.paddingInDialogs);
        this.addToContentPanel(uriLabel, 0, 0, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH);

        this.uri = new WebTextField();
        this.uri.setMinimumWidth(getFontMetrics(this.uri.getFont()).stringWidth("wwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwww"));
        this.uri.setHorizontalAlignment(WebTextField.LEFT);
        this.uri.setPadding(Settings.paddingInDialogs);
        this.addToContentPanel(this.uri, 1, 0, 2, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH);

        WebLabel typeLabel = new WebLabel("Type:");
        typeLabel.setHorizontalAlignment(WebLabel.RIGHT);
        typeLabel.setPadding(Settings.paddingInDialogs);
        this.addToContentPanel(typeLabel, 0, 1, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH);

        this.type = new WebTextField();
        this.type.setMinimumWidth(getFontMetrics(this.type.getFont()).stringWidth("wwwwwwww"));
        this.type.setHorizontalAlignment(WebTextField.LEFT);
        this.type.setPadding(Settings.paddingInDialogs);
        this.addToContentPanel(this.type, 1, 1, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH);

        WebButton generateType = new WebButton("Get Type from URI");
        generateType.addActionListener(actionEvent -> {
            String uri = this.uri.getText();
            int index = uri.lastIndexOf(".");
            if (index > -1)
                this.type.setText(uri.substring(index + 1));
        });
        generateType.setHorizontalAlignment(WebButton.CENTER);
        generateType.setPadding(Settings.paddingInDialogs);
        this.addToContentPanel(generateType, 2, 1, 1, 1, 0.1, 1.0, 0, 0, GridBagConstraints.BOTH);
    }

    /**
     * edit the specified resource or create a new one (if resource is null)
     * @param resource the resource to be edited or null to create a new one
     * @return the resource or null
     */
    @Override
    public RelatedResource edit(RelatedResource resource) {
        if (resource != null) {
            this.uri.setText(resource.getUri());
            this.uri.selectAll();
            this.type.setText(resource.getType());
        }

        this.setVisible(true);      // start the dialog

        // after closing the dialog

        if (!this.isOk())
            return resource;

        if (resource == null)
            return RelatedResource.createRelatedResource(this.uri.getText(), this.type.getText());

        resource.setUri(this.uri.getText());
        resource.setType(this.type.getText());
        return resource;
    }
}
