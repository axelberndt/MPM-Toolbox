package mpmToolbox.gui.msmTree;

import com.alee.extended.tree.WebExTree;
import com.alee.laf.text.WebTextField;
import com.alee.laf.tree.WebTreeCellEditor;

import javax.swing.*;
import java.awt.*;

/**
 * A tree cell editor. Not used, not properly implemented!
 * @author Axel Berndt
 */
@Deprecated
public class MsmTreeCellEditor extends WebTreeCellEditor<WebExTree<MsmTreeNode>> {
    protected MsmTreeNode node;

    @Override
    public Component getTreeCellEditorComponent(final JTree tree, final Object value, final boolean isSelected, final boolean expanded, final boolean leaf, final int row) {
        System.out.println("-");
        this.node = (MsmTreeNode) value;
        final WebTextField editor = (WebTextField)super.getTreeCellEditorComponent(tree, value, isSelected, expanded, leaf, row);
        editor.setText("debug");
        return editor;
    }

    @Override
    public Object getCellEditorValue() {
        System.out.println("|");
        node.name = delegate.getCellEditorValue().toString();
        return node;
    }
}
