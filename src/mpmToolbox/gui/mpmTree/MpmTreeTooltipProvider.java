package mpmToolbox.gui.mpmTree;

import com.alee.api.annotations.NotNull;
import com.alee.laf.tree.TreeCellArea;
import com.alee.laf.tree.TreeToolTipProvider;
import com.alee.managers.tooltip.TooltipWay;

import javax.swing.*;

/**
 * A tooltip provider for MpmTreeNode.
 * @author Axel Berndt
 */
public class MpmTreeTooltipProvider extends TreeToolTipProvider<MpmTreeNode> {
    /**
     * the tooltip text
     * @param component
     * @param area
     * @return
     */
    @Override
    protected String getToolTipText(JTree component, TreeCellArea<MpmTreeNode, JTree> area) {
        return this.getValue(component, area).getTooltipText();
    }

    /**
     * This overwrites the AbstractTooltipProvider's default getDirection() method which creates trailing (at the right) tooltips.
     * Since the default position of the MPM tree is at the right, there is more space to the left. So this method sets the
     * tooltips to be leading (left of the tree node).
     * @param component
     * @param area
     * @return
     */
    @Override
    @NotNull
    protected TooltipWay getDirection(JTree component, TreeCellArea<MpmTreeNode, JTree> area) {
        return TooltipWay.leading;
    }
}
