package mpmToolbox.supplementary.orthantNeighborhoodGraph;

import meico.supplementary.KeyValue;

import java.util.ArrayList;
import java.util.Random;
import java.util.TreeSet;

/**
 * This is an implementation of Tobias Germer's Orthant Neighborhood Graph for 2D.
 * The algorithms, however, are different.
 * @author Axel Berndt
 */
public class OrthantNeighborhoodGraph {
    private final ArrayList<ONGNode> nodes = new ArrayList<>();         // the list of nodes in the graph
    private ONGNode lastNodeInteractedWith = null;                      // a link to the last node that we have interacted with, it will be the starting point for the next search

    /**
     * constructor, generates an empty graph
     */
    public OrthantNeighborhoodGraph() {
    }

    /**
     * constructor
     * @param nodes nodes to be added to the graph
     */
    public OrthantNeighborhoodGraph(ONGNode... nodes) {
        for (ONGNode node : nodes) {
            this.add(node);
        }
    }

    /**
     * find the nearest neighboring node for an (x, y) position
     * @param x
     * @param y
     * @return the node found and its square distance or null if the graph is empty
     */
    public KeyValue<ONGNode, Double> findNearestNeighborOf(double x, double y) {
        if (this.isEmpty())
            return null;

        KeyValue<ONGNode, Double> found;

        if (this.lastNodeInteractedWith != null)                                // start searching at the last node we have interacted with as it is potentially close
            found = this.lastNodeInteractedWith.findNearestNeighborOf(x, y);
        else                                                                    // if we have no last node, start searching at a random node
            found = this.nodes.get((new Random()).nextInt(this.nodes.size())).findNearestNeighborOf(x, y);

        this.lastNodeInteractedWith = found.getKey();                           // set the found node the last one we interacted with

        return found;
    }

    /**
     * This method determines the four neighbors that a node at position (x, y) would get in the graph and returns it as ONGNode.
     * @param x
     * @param y
     * @return
     */
    public ONGNode findAllNearestNeighborsOf(double x, double y) {
        if (this.isEmpty())
            return new ONGNode(x, y);

        if (this.lastNodeInteractedWith != null)                                // start searching at the last node we have interacted with as it is potentially close
            return this.lastNodeInteractedWith.setAllNearestNeighborsOf(x, y);
        else
            return this.nodes.get((new Random()).nextInt(this.nodes.size())).setAllNearestNeighborsOf(x, y);
    }

    /**
     * insert a new node at the specified position in the graph
     * @param x
     * @param y
     * @return the newly generated node or the one that was already at the specified position
     */
    public ONGNode add(double x, double y) {
        ONGNode node = new ONGNode(x, y);
        return this.add(node);
    }

    /**
     * insert the given node in the graph
     * @param node
     * @return the node just added or the one that was already at the same position
     */
    public ONGNode add(ONGNode node) {
        if (this.isEmpty()) {                                   // if this is the first node we add to the graph
            this.nodes.add(node);                               // just add it
            this.lastNodeInteractedWith = node;
            return node;
        }

//        return this.add(node, this.nodes.get((new Random()).nextInt(this.nodes.size())));   // add the node to the graph, for localization start at a random node inside the graph

        if (this.lastNodeInteractedWith == null)                // start searching at the last node we have interacted with as it is potentially close, if no last node is given
            this.lastNodeInteractedWith = this.nodes.get(this.nodes.size() - 1);    // choose the last node in the list

        return this.add(node, this.lastNodeInteractedWith);     // add the node to the graph, for localization start at last node we interacted with
    }

    /**
     * this method actually does the insertion into the (non-empty!) graph
     * @param node
     * @param startNodeForSearching
     * @return the node just added or the one that was already at the same position
     */
    private ONGNode add(ONGNode node, ONGNode startNodeForSearching) {
        ONGNode collision = startNodeForSearching.setAllNearestNeighborsOf(node, new TreeSet<>(ONGNode.comparator));    // find and set the node's nearest neighbors in each quadrant
        if (collision != null) {
            this.lastNodeInteractedWith = collision;
            return collision;
        }

        // find and update all nodes that get node as new neighbor
        ArrayList<KeyValue<ONGNode, Integer>> inverseNeighborhood = node.findMyInverseNeighbors();
        for (KeyValue<ONGNode, Integer> neighbor : inverseNeighborhood)
            neighbor.getKey().neighbors[neighbor.getValue()] = node;

        this.nodes.add(node);                                   // add node to the graph's node list
        this.lastNodeInteractedWith = node;
        return node;
    }

    /**
     * remove a node from the graph
     * @param node
     */
    public void remove(ONGNode node) {
        if (!this.contains(node))
            return;

        // all nodes that have node as neighbor should set their second nearest neighbor in the quadrant as neighbor
        ArrayList<KeyValue<ONGNode, Integer>> toBeUpdated = node.findMyInverseNeighbors();
        // this is the same as above, the brute force approach
//        ArrayList<KeyValue<ONGNode, Integer>> toBeUpdated = new ArrayList<>();
//        for (ONGNode neigh : this.nodes) {
//            for (int quad = 0; quad < 4; ++quad) {
//                if (neigh.neighbors[quad] == node)
//                    toBeUpdated.add(new KeyValue<>(neigh, quad));
//            }
//        }

        // find the second nearest neighbors of the corresponding nodes
        ONGNode[] newNeighbors = new ONGNode[toBeUpdated.size()];
        for (int i = 0; i < toBeUpdated.size(); ++i) {
            ONGNode updateMe = toBeUpdated.get(i).getKey();
            int quadrant = toBeUpdated.get(i).getValue();

            newNeighbors[i] = updateMe.getSecondNearestNeighbor(quadrant);
            // this is the same as above, the brute force approach
//            for (ONGNode sec : this.nodes) {
//                if ((sec == node) || (sec == toBeUpdated.get(i).getKey()) || (toBeUpdated.get(i).getKey().getQuadrant(sec) != quadrant)) {
//                    continue;
//                }
//                if (newNeighbors[i] == null) {
//                    newNeighbors[i] = sec;
//                    continue;
//                }
//                if (sec.distanceSq(toBeUpdated.get(i).getKey()) < newNeighbors[i].distanceSq(toBeUpdated.get(i).getKey())) {
//                    newNeighbors[i] = sec;
//                }
//            }
        }

        // set the new neighbors
        for (int i = 0; i < toBeUpdated.size(); ++i) {
            ONGNode updateMe = toBeUpdated.get(i).getKey();
            int quadrant = toBeUpdated.get(i).getValue();
            ONGNode newNeighbor = newNeighbors[i];
            updateMe.neighbors[quadrant] = newNeighbor;
        }

        this.lastNodeInteractedWith = (toBeUpdated.isEmpty()) ? null : toBeUpdated.get(0).getKey();

        this.nodes.remove(node);    // remove node from the list
    }

    /**
     * Move a node to a different xy position. In fact, this method removes the node and adds a new one at the position.
     * @param node the node to be moved, it must be in the graph already! However, it will be removed and a new one generated at the specified position
     * @param x
     * @param y
     * @return the newly created node at the specified position or a preexisting node that is already ate that position
     */
    public ONGNode move(ONGNode node, double x, double y) {
        this.lastNodeInteractedWith = node;

        // handle the trivial cases
        if (this.size() == 0)
            return this.add(x, y);

        if (this.size() == 1) {
            this.remove(node);
            return this.add(x, y);
        }

        int quadrant = node.getQuadrant(x, y);                      // quadrant index
        if (quadrant == -1)                                         // if the quadrant of the target position is -1, it is the exact same position as node already has
            return node;                                            // so we just return it and are done

        // if node movement is rather local we can increase the performance of the add() procedure by setting an old neighbor of the node as starting point for the localization
        for (int q = 0; q < node.neighbors.length; quadrant = (quadrant + q) % node.neighbors.length) {     // find an old neighbor of node in one of its quadrants, start with the quadrant in the direction of the new position
            if (node.neighbors[quadrant] != null) {                 // found one
                break;
            }
        }

        // remove and add the node
        this.remove(node);
        return this.add(new ONGNode(x, y), node.neighbors[quadrant]);
    }

    /**
     * check if the graph contains any nodes
     * @return
     */
    public boolean isEmpty() {
        return this.nodes.isEmpty();
    }

    /**
     * the number of nodes in the graph
     * @return
     */
    public int size() {
        return this.nodes.size();
    }

    /**
     * check whether the specified node is in this graph
     * @param node
     * @return
     */
    public boolean contains(ONGNode node) {
        return this.nodes.contains(node);
    }
}
