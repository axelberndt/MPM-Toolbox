package mpmToolbox.gui.mpmEditingTools.editDialogs;

import com.alee.laf.label.WebLabel;
import com.alee.laf.spinner.WebSpinner;
import com.alee.laf.text.WebTextField;
import meico.mpm.elements.metadata.Author;
import mpmToolbox.gui.Settings;
import mpmToolbox.gui.mpmEditingTools.editDialogs.supplementary.EditDialogToggleButton;

import javax.swing.*;
import java.awt.*;

/**
 * This class represents the dialog for creating and editing an author entry in MPM.
 * @author Axel Berndt
 */
public class AuthorEditor extends EditDialog<Author> {
    private WebTextField name;
    private WebSpinner number;
    private EditDialogToggleButton noNumber;

    /**
     * constructor
     */
    public AuthorEditor() {
        super("Edit Author");
    }

    /**
     * the GUI
     */
    @Override
    public void makeContentPanel() {
        WebLabel nameLabel = new WebLabel("Author Name:");
        nameLabel.setHorizontalAlignment(WebLabel.RIGHT);
        nameLabel.setPadding(Settings.paddingInDialogs);
        this.addToContentPanel(nameLabel, 0, 0, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH);

        this.name = new WebTextField("author name");
        this.name.setHorizontalAlignment(WebTextField.LEFT);
        this.name.setPadding(Settings.paddingInDialogs);
        this.addToContentPanel(this.name, 1, 0, 3, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH);

        this.number = new WebSpinner(new SpinnerNumberModel(0, 0, Integer.MAX_VALUE, 1));
        int width = getFontMetrics(this.number.getFont()).stringWidth("999.999.999");
        this.number.setMinimumWidth(width);
        this.number.setMaximumWidth(width);
        this.addToContentPanel(this.number, 1, 1, 1, 1, 0.03, 1.0, 5, 5, GridBagConstraints.VERTICAL);

        this.noNumber = new EditDialogToggleButton("Number:", new JComponent[]{this.number}, false);
        this.addToContentPanel(this.noNumber, 0, 1, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH);

        this.addIdInput(2);
    }

    /**
     * Open the author editing dialog.
     * @param author the author to be edited or null if a new one should be created
     * @return the author or null
     */
    @Override
    public Author edit(Author author) {
        // if we edit an author read and set its initial values
        if (author != null) {
            this.name.setText(author.getName());
            this.id.setText(author.getId());

            if (author.getNumber() != null) {
                this.number.setValue(author.getNumber());
                this.noNumber.setSelected(true);
            }
        }

        this.name.selectAll();

        this.setVisible(true);      // start the dialog

        // after the dialog closed do the following

        if (!this.isOk())           // if input was canceled
            return author;          // return the author unchanged

//        if (this.name.isEmpty())                                  // make sure that the author does actually have a name
//            this.name.setText("random_name_" + UUID.randomUUID().toString());

        String id = this.id.getText();
        if (id.isEmpty())
            id = null;

        Integer number = this.number.isEnabled() ? (Integer) this.number.getValue() : null;

        if (author == null)                                         // if a new author is to be created
            return Author.createAuthor(this.name.getText(), number, id);    // create it

        author.setName(this.name.getText());
        author.setNumber(number);
        author.setId(id);

        return author;
    }
}
