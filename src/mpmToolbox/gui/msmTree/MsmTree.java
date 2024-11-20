package mpmToolbox.gui.msmTree;

import com.alee.api.annotations.NotNull;
import com.alee.api.annotations.Nullable;
import com.alee.api.data.CompassDirection;
import com.alee.extended.dock.WebDockableFrame;
import com.alee.extended.tree.WebExTree;
import com.alee.laf.scroll.WebScrollPane;
import com.alee.managers.icon.Icons;
import com.alee.managers.style.StyleId;
import mpmToolbox.gui.ProjectPane;
import nu.xom.Attribute;
import nu.xom.Element;
import nu.xom.Node;

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
import java.util.Enumeration;

/**
 * A custom WebAsncTree for MSM data.
 * @author Axel Berndt
 */
public class MsmTree extends WebExTree<MsmTreeNode> implements MouseListener, TreeSelectionListener, TreeModelListener {
    @NotNull private final ProjectPane projectPane;                         // a link to the parent project pane to access its data, midi player etc.
    private WebDockableFrame dockableFrame = null;                          // a WebDockableFrame instance that displays this MSM tree, to be used in class ProjectPane


    /**
     * constructor
     * @param projectPane
     */
    public MsmTree(@NotNull ProjectPane projectPane) {
        super(new MsmTreeDataProvider(projectPane.getMsm().getRootElement(), projectPane.getProjectData()));
        this.projectPane = projectPane;

        this.setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);     // this sets that only one node can be selected at a time
        this.setCellRenderer(new MsmTreeCellRenderer());                     // a custom tree cell renderer
        this.setToolTipProvider(new MsmTreeTooltipProvider());               // set a tooltip provider so we can print tooltips on mouse over of tree cells
//        msmTree.setEditable(true);
//        msmTree.setCellEditor(new MsmTreeCellEditor());
//        msmTree.setStyleId(StyleId.treeTransparent);

        this.addMouseListener(this);
        this.addTreeSelectionListener(this);
        this.treeModel.addTreeModelListener(this);
    }

//    /**
//     * action on mouse click
//     * @param mouseEvent
//     */
//    @Override
//    public void mouseClicked(MouseEvent mouseEvent) {
//        MsmTreeNode n = this.getNodeForLocation(mouseEvent.getX(), mouseEvent.getY());
//        if (n != null)
//            n.play(this.projectPane.getParentMpmToolbox().getMidiPlayerForSingleNotes());
//    }

    /**
     * when a tree node is clicked, do this
     * @param treeSelectionEvent
     */
    @Override
    public void valueChanged(TreeSelectionEvent treeSelectionEvent) {
        TreePath path = treeSelectionEvent.getNewLeadSelectionPath();
        if (path == null)
            return;

        MsmTreeNode n = this.getNodeForPath(path);
        n.play(this.projectPane.getParentMpmToolbox().getMidiPlayerForSingleNotes());   // the node might be a node and should play its note via MIDI when selected
    }

    /**
     * a getter to access the project pane that this tree belongs to
     * @return
     */
    public ProjectPane getProjectPane() {
        return this.projectPane;
    }

    /**
     * find and open the path to the first node of type note
     */
    public void gotoFirstNoteNode() {
        TreePath path = (this.getFirstNodeOfType(this.getRootNode(), MsmTreeNode.XmlNodeType.note).getTreePath());
        this.setSelectionPath(path);
        this.scrollPathToVisible(path);
    }


    /**
     * find the first node of the specified type
     * @param parent
     * @param type
     * @return
     */
    public MsmTreeNode getFirstNodeOfType(MsmTreeNode parent, MsmTreeNode.XmlNodeType type) {
        Enumeration<MsmTreeNode> e = parent.depthFirstEnumeration();
//        Enumeration<MsmTreeNode> e = parent.breadthFirstEnumeration();
        while (e.hasMoreElements()) {
            MsmTreeNode node = e.nextElement();
            if (node.getType() == type) {
                return node;
            }
        }
        return null;
    }

    /**
     * find all nodes of the specified type
     * @param parent
     * @param type
     * @return
     */
    public ArrayList<MsmTreeNode> getAllNodesOfType(MsmTreeNode parent, MsmTreeNode.XmlNodeType type) {
        Enumeration<MsmTreeNode> e = parent.breadthFirstEnumeration();
        ArrayList<MsmTreeNode> results = new ArrayList<>();

        while (e.hasMoreElements()) {
            MsmTreeNode node = e.nextElement();
            if (node.getType() == type) {
                results.add(node);
            }
        }

        return results;
    }

    /**
     * find the first MsmTreeNode with the specified userObject as user object
     * @param userObject
     * @param depthFirstStrategy true for depth first search, false for breath first search
     * @return
     */
    public MsmTreeNode findNode(Node userObject, boolean depthFirstStrategy) {
        if (userObject == null)
            return null;

        Enumeration<MsmTreeNode> e = depthFirstStrategy ? this.getRootNode().depthFirstEnumeration() : this.getRootNode().breadthFirstEnumeration();

        while (e.hasMoreElements()) {
            MsmTreeNode treeNode = e.nextElement();
            Node userObj = treeNode.getUserObject();
            if (userObj == userObject) {
                return treeNode;
            }
        }
        return null;
    }

    /**
     * find  the first MsmTreeNode with the specified ID
     * @param id
     * @param depthFirstStrategy true for depth first search, false for breath first search
     * @return
     */
    public MsmTreeNode findNode(String id, boolean depthFirstStrategy) {
        if (id == null)
            return null;

        Enumeration<MsmTreeNode> e = depthFirstStrategy ? this.getRootNode().depthFirstEnumeration() : this.getRootNode().breadthFirstEnumeration();

        while (e.hasMoreElements()) {
            MsmTreeNode treeNode = e.nextElement();
            if (!(treeNode.getUserObject() instanceof Element))
                continue;
            Element xml = (Element) treeNode.getUserObject();

            Attribute idAtt = xml.getAttribute("id", "http://www.w3.org/XML/1998/namespace");
            if (idAtt == null)
                continue;

            if (idAtt.getValue().equals(id))
                return treeNode;
        }

        return null;
    }

    /**
     * this forces the node to update itself and then updates the node's appearance in the tree
     * @param node
     */
    @Override
    public void updateNode(@Nullable final MsmTreeNode node) {
        node.update();
        super.updateNode(node);
    }

    public WebDockableFrame getDockableFrame() {
        if (this.dockableFrame != null)
            return this.dockableFrame;

        this.dockableFrame = new WebDockableFrame("msmFrame", "Musical Sequence Markup");
        this.dockableFrame.setIcon(Icons.table);
        this.dockableFrame.setClosable(false);                                   // when closed the frame disappears and cannot be reopened by the user, thus, this is set false
        this.dockableFrame.setMaximizable(false);                                // it is also set to not maximizable
        this.dockableFrame.setPosition(CompassDirection.west);

        WebScrollPane scrollPane = new WebScrollPane(this);
        scrollPane.setStyleId(StyleId.scrollpaneUndecoratedButtonless);
        this.dockableFrame.add(scrollPane);

        return this.dockableFrame;
    }

    /**
     * When the user clicked in the tree, perform this action.
     * @param mouseEvent
     */
    @Override
    public void mouseClicked(MouseEvent mouseEvent) {
        // right click opens the context menu of the clicked node
        if (SwingUtilities.isRightMouseButton(mouseEvent)) {    // if right click
            MsmTreeNode node = this.getNodeForRow(this.getClosestRowForLocation(mouseEvent.getX(), mouseEvent.getY()));   // get the node that has been clicked
            node.getContextMenu(this).show(this, mouseEvent.getX() - 25, mouseEvent.getY()); // trigger its context menu
            return;
        }
        if (SwingUtilities.isLeftMouseButton(mouseEvent)) {     // if left click
            if (mouseEvent.getClickCount() > 1) {               // if double (or more) click -> open editor dialog
                MsmTreeNode node = this.getSelectedNode();      // get the node that has been double-clicked
                node.openEditorDialog(this);
            }
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
