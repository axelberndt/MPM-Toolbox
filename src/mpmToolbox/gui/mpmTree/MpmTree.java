package mpmToolbox.gui.mpmTree;

import com.alee.api.annotations.NotNull;
import com.alee.api.annotations.Nullable;
import com.alee.extended.tree.WebExTree;
import mpmToolbox.gui.ProjectPane;

import javax.swing.*;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;

/**
 * This class represents an MPM tree based on the WebExTree class.
 * @author Axel Berndt
 */
public class MpmTree extends WebExTree<MpmTreeNode> implements MouseListener, TreeSelectionListener, TreeModelListener {
    @NotNull private final ProjectPane projectPane;                         // a link to the parent project pane to access its data, midi player etc.

    /**
     * constructor
     * @param projectPane
     */
    public MpmTree(@NotNull ProjectPane projectPane) {
        super(new MpmTreeDataProvider(projectPane.getProjectData()));
        this.projectPane = projectPane;

        this.setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);    // this sets that only one node can be selected at a time
        this.setCellRenderer(new MpmTreeCellRenderer());                    // a custom tree cell renderer
        this.setToolTipProvider(new MpmTreeTooltipProvider());              // set a tooltip provider so we can print tooltips on mouse over of tree cells
//        this.setEditable(true);
//        this.setCellEditor(new MpmTreeCellEditor());
//        this.setStyleId(StyleId.treeTransparent);

        this.addTreeSelectionListener(this);
        this.treeModel.addTreeModelListener(this);
        this.addMouseListener(this);
    }

    /**
     * The TreeSelectionListener is connected to the MPM tree and fires when something is selected there.
     * So the score display can highlight notes if possible.
     * @param treeSelectionEvent
     */
    @Override
    public void valueChanged(TreeSelectionEvent treeSelectionEvent) {
        TreePath path = treeSelectionEvent.getNewLeadSelectionPath();
        if (path == null)
            return;

        this.projectPane.repaintScoreDisplay();    // repaint the score display so a selected MpmTreeNode gets highlighted and when switching to another performance we get to see its overlay
    }

    /**
     * this forces the node to update itself and then updates the node's appearance in the tree
     * @param node
     */
    @Override
    public void updateNode(@Nullable final MpmTreeNode node) {
        node.update();
        super.updateNode(node);
    }

    /**
     * a getter to access the project pane that this tree belongs to
     * @return
     */
    public ProjectPane getProjectPane() {
        return this.projectPane;
    }

    /**
     * get a list of all nodes of the specified types
     * @param parent
     * @param types
     * @return
     */
    public ArrayList<MpmTreeNode> getAllNodesOfTypes(MpmTreeNode parent, MpmTreeNode.MpmNodeType[] types) {
        List<MpmTreeNode.MpmNodeType> typeList = Arrays.asList(types);
        Enumeration<MpmTreeNode> e = parent.breadthFirstEnumeration();
        ArrayList<MpmTreeNode> results = new ArrayList<>();

        while (e.hasMoreElements()) {
            MpmTreeNode node = e.nextElement();
            if (typeList.contains(node.type))
                results.add(node);
        }

        return results;
    }

    /**
     * this method retrieves all map entries
     * @param parent
     * @return
     */
    public ArrayList<MpmTreeNode> getAllMapEntryNodes(MpmTreeNode parent) {
        ArrayList<MpmTreeNode.MpmNodeType> types = new ArrayList<>();
        types.add(MpmTreeNode.MpmNodeType.accentuationPattern);
        types.add(MpmTreeNode.MpmNodeType.articulation);
        types.add(MpmTreeNode.MpmNodeType.asynchrony);
        types.add(MpmTreeNode.MpmNodeType.distributionCorrelatedBrownianNoise);
        types.add(MpmTreeNode.MpmNodeType.distributionCorrelatedCompensatingTriangle);
        types.add(MpmTreeNode.MpmNodeType.distributionCorrelatedCompensatingTriangle);
        types.add(MpmTreeNode.MpmNodeType.distributionGaussian);
        types.add(MpmTreeNode.MpmNodeType.distributionList);
        types.add(MpmTreeNode.MpmNodeType.distributionTriangular);
        types.add(MpmTreeNode.MpmNodeType.distributionUniform);
        types.add(MpmTreeNode.MpmNodeType.dynamics);
        types.add(MpmTreeNode.MpmNodeType.rubato);
        types.add(MpmTreeNode.MpmNodeType.style);
        types.add(MpmTreeNode.MpmNodeType.tempo);
//        types.add(MpmTreeNode.MpmNodeType.unknown);
//        types.add(MpmTreeNode.MpmNodeType.xmlElement);

        Enumeration<MpmTreeNode> e = parent.depthFirstEnumeration();
        ArrayList<MpmTreeNode> results = new ArrayList<>();

        while (e.hasMoreElements()) {
            MpmTreeNode node = e.nextElement();
            if (types.contains(node.type))
                results.add(node);
        }

        return results;
    }

    /**
     * find the first node that represents an entry in a map
     * @return
     */
    public MpmTreeNode getFirstMapEntryNode() {
        ArrayList<MpmTreeNode.MpmNodeType> types = new ArrayList<>();
        types.add(MpmTreeNode.MpmNodeType.accentuationPattern);
        types.add(MpmTreeNode.MpmNodeType.articulation);
        types.add(MpmTreeNode.MpmNodeType.asynchrony);
        types.add(MpmTreeNode.MpmNodeType.distributionCorrelatedBrownianNoise);
        types.add(MpmTreeNode.MpmNodeType.distributionCorrelatedCompensatingTriangle);
        types.add(MpmTreeNode.MpmNodeType.distributionCorrelatedCompensatingTriangle);
        types.add(MpmTreeNode.MpmNodeType.distributionGaussian);
        types.add(MpmTreeNode.MpmNodeType.distributionList);
        types.add(MpmTreeNode.MpmNodeType.distributionTriangular);
        types.add(MpmTreeNode.MpmNodeType.distributionUniform);
        types.add(MpmTreeNode.MpmNodeType.dynamics);
        types.add(MpmTreeNode.MpmNodeType.rubato);
        types.add(MpmTreeNode.MpmNodeType.style);
        types.add(MpmTreeNode.MpmNodeType.tempo);
//        types.add(MpmTreeNode.MpmNodeType.unknown);
//        types.add(MpmTreeNode.MpmNodeType.xmlElement);

        Enumeration<MpmTreeNode> e = this.getRootNode().depthFirstEnumeration();
        while (e.hasMoreElements()) {
            MpmTreeNode node = e.nextElement();
            if (types.contains(node.type))
                return node;
        }
        return null;
    }

    /**
     * find and select the first map entry in the MPM tree
     */
    public void gotoFirstMapEntryNode() {
        TreePath path = this.getFirstMapEntryNode().getTreePath();
        this.setSelectionPath(path);
        this.scrollPathToVisible(path);
    }

    /**
     * find the first MpmTreeNode with the specified userObject as user object
     * @param userObject
     * @param depthFirstStrategy true for depth first search, false for breath first search
     * @return
     */
    public MpmTreeNode findNode(Object userObject, boolean depthFirstStrategy) {
        if (userObject == null)
            return null;

        Enumeration<MpmTreeNode> e = depthFirstStrategy ? this.getRootNode().depthFirstEnumeration() : this.getRootNode().breadthFirstEnumeration();

        while (e.hasMoreElements()) {
            MpmTreeNode treeNode = e.nextElement();
            Object userObj = treeNode.getUserObject();
            if (userObject == userObj) {
                return treeNode;
            }
        }
        return null;
    }

    /**
     * When the user clicked in the tree, perform this action.
     * @param mouseEvent
     */
    @Override
    public void mouseClicked(MouseEvent mouseEvent) {
        // right click opens the context menu of the clicked node
        if (SwingUtilities.isRightMouseButton(mouseEvent)) {
            MpmTreeNode node = this.getNodeForRow(this.getClosestRowForLocation(mouseEvent.getX(), mouseEvent.getY()));   // get the node that has been clicked
            node.getContextMenu(this).show(this, mouseEvent.getX() - 25, mouseEvent.getY()); // trigger its context menu
        }
    }

    /**
     * Perform this action when mouse button is pressed.
     * @param mouseEvent
     */
    @Override
    public void mousePressed(MouseEvent mouseEvent) {

    }

    /**
     * Perform this action when mouse button is released.
     * @param mouseEvent
     */
    @Override
    public void mouseReleased(MouseEvent mouseEvent) {

    }

    /**
     * Perform this action when the mouse cursor enters the MpmTree widget.
     * @param mouseEvent
     */
    @Override
    public void mouseEntered(MouseEvent mouseEvent) {

    }

    /**
     * perform this action when the mouse cursor exits the MpmTree widget.
     * @param mouseEvent
     */
    @Override
    public void mouseExited(MouseEvent mouseEvent) {

    }

    @Override
    public void treeNodesChanged(TreeModelEvent treeModelEvent) {
    }

    @Override
    public void treeNodesInserted(TreeModelEvent treeModelEvent) {
    }

    @Override
    public void treeNodesRemoved(TreeModelEvent treeModelEvent) {
    }

    /**
     * if anything in the tree structure changed the score display gets updated
     * @param treeModelEvent
     */
    @Override
    public void treeStructureChanged(TreeModelEvent treeModelEvent) {
        this.projectPane.repaintScoreDisplay();    // repaint the score display so a selected MpmTreeNode gets highlighted and when switching to another performance we get to see its overlay
    }
}
