package mpmToolbox.gui.msmTree;

import com.alee.api.annotations.NotNull;
import com.alee.extended.tree.AbstractExTreeDataProvider;
import mpmToolbox.projectData.ProjectData;
import nu.xom.Element;
import nu.xom.Elements;

import java.util.ArrayList;
import java.util.List;

/**
 * TreeDataProvider for MSM tree
 * @author Axel Berndt
 */
public class MsmTreeDataProvider extends AbstractExTreeDataProvider<MsmTreeNode> {
    @NotNull protected Element msmRoot;
    @NotNull private final ProjectData project;

    /**
     * constructor
     * @param msmRoot
     * @param project
     */
    public MsmTreeDataProvider(@NotNull final Element msmRoot, @NotNull ProjectData project) {
        this.msmRoot = msmRoot;
        this.project = project;
    }

    /**
     * get the root node of the tree
     * @return
     */
    @Override
    public MsmTreeNode getRoot() {
        return new MsmTreeNode(this.msmRoot, this.project);
    }

    /**
     * this method is used to buffer the tree nodes
     * @param parent
     * @return
     */
    @Override
    public List<MsmTreeNode> getChildren(MsmTreeNode parent) {
        ArrayList<MsmTreeNode> childNodes = new ArrayList<>();          // fill this list with child nodes of the specified parent
        if (parent.getType() == MsmTreeNode.XmlNodeType.attribute) {    // attributes have no children
            return childNodes;                                                     // done
        }

        Element p = (Element)parent.getUserObject();

        // make attributes to nodes
        for (int i = 0; i < p.getAttributeCount(); ++i)
            childNodes.add(new MsmTreeNode(p.getAttribute(i), this.project));

        // make child elements to nodes
        Elements children = ((Element)parent.getUserObject()).getChildElements();
        for (int i = 0; i < children.size(); ++i) {
            childNodes.add(new MsmTreeNode(children.get(i), this.project));
        }

        return childNodes;
    }
}
