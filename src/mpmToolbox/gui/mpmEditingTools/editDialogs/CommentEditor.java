package mpmToolbox.gui.mpmEditingTools.editDialogs;

import com.alee.laf.label.WebLabel;
import com.alee.laf.scroll.WebScrollPane;
import com.alee.laf.text.WebTextArea;
import meico.mpm.elements.metadata.Comment;
import mpmToolbox.gui.Settings;

import java.awt.*;

/**
 * This class represents the dialog for creating and editing a comment in MPM.
 * @author Axel Berndt
 */
public class CommentEditor extends EditDialog<Comment> {
    private WebTextArea text;

    /**
     * constructor
     */
    public CommentEditor() {
        super("Edit Comment");
    }

    /**
     * the GUI
     */
    @Override
    public void makeContentPanel() {
        WebLabel textboxLabel = new WebLabel("Comment Text");
        textboxLabel.setHorizontalAlignment(WebLabel.LEFT);
        textboxLabel.setVerticalAlignment(WebLabel.BOTTOM);
        textboxLabel.setPadding(Settings.paddingInDialogs);
        this.addToContentPanel(textboxLabel, 0, 0, 4, 1, 1.0, 1.0, 0, 0, GridBagConstraints.VERTICAL);

        this.text = new WebTextArea();
        this.text.setLineWrap(true);
        this.text.setWrapStyleWord(true);
        this.text.setInputPrompt("Enter Text");
        this.text.setMinimumHeight(getFontMetrics(this.text.getFont()).getHeight() * 10);
        this.text.setPadding(Settings.paddingInDialogs);
        WebScrollPane textScroller = new WebScrollPane(this.text);
        textScroller.setMinimumHeight(this.text.getMinimumHeight());
        this.addToContentPanel(textScroller, 0, 1, 4, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH);

        this.addIdInput(2);
    }

    /**
     * Open the comment editing dialog.
     * @param comment the comment to be edited or null if a new one should be created
     * @return the comment or null
     */
    @Override
    public Comment edit(Comment comment) {
        // if we edit a comment read and set its initial values
        if (comment != null) {
            this.text.setText(comment.getText());
            this.text.selectAll();
            this.id.setText(comment.getId());
        }

        this.setVisible(true);      // start the dialog

        // after the dialog closed do the following

        if (!this.isOk())           // if input was canceled
            return comment;         // return the data unchanged

        String id = this.id.getText();
        if (id.isEmpty())
            id = null;

        if (comment == null) {
            comment = Comment.createComment(this.text.getText(), id);
        } else {
            comment.setText(this.text.getText());
            comment.setId(id);
        }

        return comment;
    }
}
