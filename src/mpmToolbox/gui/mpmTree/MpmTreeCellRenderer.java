package mpmToolbox.gui.mpmTree;

import com.alee.api.annotations.NotNull;
import com.alee.api.annotations.Nullable;
import com.alee.extended.tree.WebExTree;
import com.alee.laf.tree.TreeNodeParameters;
import com.alee.laf.tree.WebTreeCellRenderer;

import javax.swing.*;

/**
 * A custom tree cell renderer for MPM trees.
 * @author Axel Berndt
 */
public class MpmTreeCellRenderer  extends WebTreeCellRenderer<MpmTreeNode, WebExTree<MpmTreeNode>, TreeNodeParameters<MpmTreeNode, WebExTree<MpmTreeNode>>> {
    /**
     * This returns the text to be written for node.
     * @param parameters
     * @return
     */
    @Override
    @Nullable
    protected String textForValue(@NotNull final TreeNodeParameters<MpmTreeNode, WebExTree<MpmTreeNode>> parameters) {
        return parameters.node().getText(parameters);
    }

    /**
     * this returns the icon of the node
     * @param parameters
     * @return
     */
    @Override
    @Nullable
    protected Icon iconForValue (@NotNull final TreeNodeParameters<MpmTreeNode, WebExTree<MpmTreeNode>> parameters ) {
        return parameters.node().getNodeIcon(parameters);
    }
}
