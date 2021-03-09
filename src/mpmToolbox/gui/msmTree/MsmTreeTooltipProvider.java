package mpmToolbox.gui.msmTree;

import com.alee.laf.tree.TreeCellArea;
import com.alee.laf.tree.TreeToolTipProvider;

import javax.swing.*;

/**
 * A tooltip provider for MsmTreeNode.
 * @author Axel Berndt
 */
public class MsmTreeTooltipProvider extends TreeToolTipProvider<MsmTreeNode> {
    /**
     * the tooltip text
     * @param component
     * @param area
     * @return
     */
    @Override
    protected String getToolTipText(JTree component, TreeCellArea<MsmTreeNode, JTree> area) {
        return this.getValue(component, area).getTooltipText();
    }
}
