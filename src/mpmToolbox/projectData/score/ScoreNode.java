package mpmToolbox.projectData.score;

import mpmToolbox.supplementary.orthantNeighborhoodGraph.ONGNode;
import nu.xom.Element;

import java.util.ArrayList;

/**
 * This class extends ONGNode by associated content.
 * @author Axel Berndt
 */
public class ScoreNode extends ONGNode {
    private final ArrayList<Element> elements = new ArrayList<>();    // the elements that this node is associated with

    /**
     * constructor
     * @param x
     * @param y
     */
    public ScoreNode(double x, double y, Element element) {
        super(x, y);
        this.elements.add(element);
    }

    /**
     * getter for the elements associated with the node
     * @return
     */
    public ArrayList<Element> getAssociatedElements() {
        return this.elements;
    }

    /**
     * add an element association to the ONGNode
     * @param element
     * @return
     */
    public boolean addAssociatedElement(Element element) {
        if (!this.elements.contains(element))
            return this.elements.add(element);
        return false;
    }

    /**
     * remove an element association from the node
     * @param element
     * @throws Exception
     */
    public void removeAssociatedElement(Element element) throws Exception {
        this.elements.remove(element);
        if (this.elements.isEmpty())
            throw new Exception("The node is no longer associated with anything! It should be removed from the Graph.");
    }
}
