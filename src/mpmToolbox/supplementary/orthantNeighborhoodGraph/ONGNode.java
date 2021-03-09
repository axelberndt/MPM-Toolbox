package mpmToolbox.supplementary.orthantNeighborhoodGraph;

import meico.supplementary.KeyValue;

import java.awt.geom.Point2D;
import java.util.*;

/**
 * This represents a node in an Orthant Neighborhood Graph. The implementation is for the 2D case.
 * This is based on Tobias Germer's Orthant Neighborhood Graph, but the algorithms are different.
 * @author Axel Berndt
 */
public class ONGNode extends Point2D.Double {
    public static final int NORTHEAST = 0;
    public static final int SOUTHEAST = 1;
    public static final int SOUTHWEST = 2;
    public static final int NORTHWEST = 3;

    public final ONGNode[] neighbors = {null, null, null, null};  // the nearest neighbors in quadrant NE, SE, SW, NW

    /**
     * the comparator is needed to make TreeSets of ONGNodes
     */
    public static final Comparator<ONGNode> comparator = new Comparator<ONGNode>() {
        @Override
        public int compare(ONGNode node1, ONGNode node2) {
            int cx = 0, cy= 0;
            double x1, x2, y1, y2;
            if (node1 == null)
                x1 = y1 = java.lang.Double.MIN_VALUE;
            else {
                x1 = node1.getX();
                y1 = node1.getY();
            }
            if (node2 == null)
                x2 = y2 = java.lang.Double.MIN_VALUE;
            else {
                x2 = node2.getX();
                y2 = node2.getY();
            }
            if (x1 < x2)
                cx = -1;
            if (x1 > x2)
                cx = 1;
            if (y1 < y2)
                cy = -10;
            if (y1 > y2)
                cy = 10;
            return cx + cy;
        }
    };

    /**
     * constructor
     * @param x
     * @param y
     */
    public ONGNode(double x, double y) {
        this.setLocation(x, y);
    }

    /**
     * Does this node have the specified node as neighbor?
     * @param node
     * @return
     */
    public boolean hasNeighbor(ONGNode node) {
        return (this.neighbors[ONGNode.NORTHEAST] == node) || (this.neighbors[ONGNode.SOUTHEAST] == node) || (this.neighbors[ONGNode.SOUTHWEST] == node) || (this.neighbors[ONGNode.NORTHWEST] == node);
    }

    /**
     * get the quadrant index of the current node in which the specified node is to be found
     * @param point2D
     * @return
     */
    public int getQuadrant(Point2D point2D) {
        return this.getQuadrant(point2D.getX(), point2D.getY());
    }

    /**
     * get the quadrant index of the current node in which the specified point is to be found
     * @param x
     * @param y
     * @return the quadrant index of the coordinates in question or -1 if (x, y) equals this node's coordinates
     */
    public int getQuadrant(double x, double y) {
        if (x > this.getX()) {
            if (y >= this.getY())
                return ONGNode.NORTHEAST;
            return ONGNode.SOUTHEAST;
        }
        if (x < this.getX()) {
            if (y <= this.getY())
                return ONGNode.SOUTHWEST;
            return ONGNode.NORTHWEST;
        }
        // x == this.x
        if (y > this.getY())
            return ONGNode.NORTHEAST;
        if (y < this.getY())
            return ONGNode.SOUTHWEST;

        return -1;
    }

    /**
     * set the neighbor
     * @param node
     */
    protected void setNorthEastNeighbor(ONGNode node) {
        this.neighbors[ONGNode.NORTHEAST] = node;
    }

    /**
     * set the neighbor
     * @param node
     */
    protected void setSouthEastNeighbor(ONGNode node) {
        this.neighbors[ONGNode.SOUTHEAST] = node;
    }

    /**
     * set the neighbor
     * @param node
     */
    protected void setSouthWestNeighbor(ONGNode node) {
        this.neighbors[ONGNode.SOUTHWEST] = node;
    }

    /**
     * set the neighbor
     * @param node
     */
    protected void setNorthWestNeighbor(ONGNode node) {
        this.neighbors[ONGNode.NORTHWEST] = node;
    }

    /**
     * get the neighbor
     * @param node
     */
    public ONGNode getNorthEastNeighbor(ONGNode node) {
        return this.neighbors[ONGNode.NORTHEAST];
    }

    /**
     * get the neighbor
     * @param node
     */
    public ONGNode getSouthEastNeighbor(ONGNode node) {
        return this.neighbors[ONGNode.SOUTHEAST];
    }

    /**
     * get the neighbor
     * @param node
     */
    public ONGNode getSouthWestNeighbor(ONGNode node) {
        return this.neighbors[ONGNode.SOUTHWEST];
    }

    /**
     * get the neighbor
     * @param node
     */
    public ONGNode getNorthWestNeighbor(ONGNode node) {
        return this.neighbors[ONGNode.NORTHWEST];
    }

    /**
     * finds the nearest neighbor in the graph and returns it together with the square distance
     * @return the nearest neighbor or a tuple (null, Double.MAX_VALUE) if there is no neighbor
     */
    public KeyValue<ONGNode, java.lang.Double> getNearestNeighbor() {
        KeyValue<ONGNode, java.lang.Double> nearest = new KeyValue<>(null, java.lang.Double.MAX_VALUE);

        for (ONGNode neighbor : this.neighbors) {
            if (neighbor == null)
                continue;

            double distance = this.distanceSq(neighbor);
            if (distance >= nearest.getValue())
                continue;

            nearest.setKey(neighbor);
            nearest.setValue(distance);
        }

        return nearest;
    }

    /**
     * Given a set of ONGNodes, find the one that is (a) closest to this and (b) in the specified quadrant of this.
     * @param quadrant
     * @param nodes
     * @return
     */
    public ONGNode getClosestInQuadrant(int quadrant, ArrayList<ONGNode> nodes) {
        ONGNode result = null;
        double distance = java.lang.Double.MAX_VALUE;
        for (ONGNode n : nodes) {
            if (this.getQuadrant(n) != quadrant)
                continue;
            if (this.distanceSq(n) < distance)
                result = n;
        }
        return result;
    }

    /**
     * this method returns the second nearest neighbor of this node in the specified quadrant
     * @param quadrant
     * @return the second nearest neighbor in the specified quadrant or null
     */
    public ONGNode getSecondNearestNeighbor(int quadrant) {
        ONGNode neighbor = this.neighbors[quadrant];                        // get the nearest neighbor in the quadrant
        if (neighbor == null)                                               // if there is no neighbor in the quadrant
            return null;                                                    // done

        ArrayList<ONGNode> candidates = new ArrayList<>();                  // candidates for second nearest neighbor will be added to this list

        // compute some useful quadrant indices so we don't need to do it later again
        int rightQuad = (quadrant + 1) % this.neighbors.length;
        int backQuad = (quadrant + 2) % this.neighbors.length;
        int leftQuad = (quadrant + 3) % this.neighbors.length;

        // check the right neighbor's neighbor
        ONGNode right = neighbor.neighbors[rightQuad];
        if (right != null)
            right.findSecondNearestNeighborCandidates(this, quadrant, backQuad, rightQuad, leftQuad, candidates);

        // check the neighbor's neighbor that is behind the neighbor
        ONGNode away = neighbor.neighbors[quadrant];
        if (away != null)
            away.findSecondNearestNeighborCandidates(this, quadrant, backQuad, rightQuad, leftQuad, candidates);

        // check the left neighbor's neighbor
        ONGNode left = neighbor.neighbors[leftQuad];
        if (left != null)
            left.findSecondNearestNeighborCandidates(this, quadrant, backQuad, rightQuad, leftQuad, candidates);

        if (candidates.isEmpty())                   // if we found no candidates
            return null;                            // nothing to return

        // find the closest among the candidates for second nearest neighbor
        ONGNode result = candidates.remove(0);      // take the first candidate and remove it from the list so we don't test it against itself
        double dist = this.distanceSq(result);      // compute the distance to this
        for (ONGNode can : candidates) {            // check all other candidates
            double canDist = this.distanceSq(can);  // compute the candidate's distance to this
            if (canDist < dist) {                   // if that distance is less than the one of the result node so far
                result = can;                       // take the candidate as result
                dist = canDist;                     // and keep its distance
            }
        }

        return result;
    }

    /**
     * a helper method to determine the second nearest neighbor of a node
     * @param pivot the node for which we seek the second nearest neighbor
     * @param quadrant the quadrant of pivot in which we seek the second nearest neighbor
     * @param backQuad a quadrant index we don't want to compute again
     * @param rightQuad a quadrant index we don't want to compute again
     * @param leftQuad a quadrant index we don't want to compute again
     * @param candidates a list of candidate nodes for second nearest neighbor, it gets filled in the process
     */
    private void findSecondNearestNeighborCandidates(ONGNode pivot, int quadrant, int backQuad, int rightQuad, int leftQuad, ArrayList<ONGNode> candidates) {
        if ((this == pivot) || (this == pivot.neighbors[quadrant]))         // if this is the pivot node or its direct neighbor
            return;                                                         // done

        int thisQuad = pivot.getQuadrant(this);

        if (thisQuad == backQuad)                                           // if we are on the reverse side of pivot
            return;                                                         // done, we are by far too far away

        if (thisQuad == quadrant) {                                         // we are within the quadrant where we want to find candidates
            for (ONGNode can : candidates)                                  // dead zone check
                if ((can == this) || (can.getQuadrant(this) == quadrant))   // if this is already in candidates or this is behind a node in candidates (the dead zone)
                    return;                                                 // we are done with this node

            candidates.add(this);                                           // add this to the candidates

            // check the nodes in reverse, right and left direction
            ONGNode back = this.neighbors[backQuad];
            back.findSecondNearestNeighborCandidates(pivot, quadrant, backQuad, rightQuad, leftQuad, candidates);

            ONGNode right = this.neighbors[rightQuad];
            if (right != null)
                right.findSecondNearestNeighborCandidates(pivot, quadrant, backQuad, rightQuad, leftQuad, candidates);

            ONGNode left = this.neighbors[leftQuad];
            if (left != null)
                left.findSecondNearestNeighborCandidates(pivot, quadrant, backQuad, rightQuad, leftQuad, candidates);

            return;                                                         // done
        }

        // if we are in another quadrant of pivot (left or right)
        // go on in the quadrant that leads away from pivot bat perhaps back into the desired quadrant
        ONGNode away = this.neighbors[quadrant];
        if (away != null)
            away.findSecondNearestNeighborCandidates(pivot, quadrant, backQuad, rightQuad, leftQuad, candidates);

        // if we are right to the desired quadrant, check the quadrant in the direction of pivot
        if (thisQuad == rightQuad) {
            ONGNode left = this.neighbors[leftQuad];
            if (left != null)
                left.findSecondNearestNeighborCandidates(pivot, quadrant, backQuad, rightQuad, leftQuad, candidates);

            return;
        }

        // if we are left to the desired quadrant, check the quadrant in the direction of pivot
        // if (thisQuad == leftQuad)
        ONGNode right = this.neighbors[rightQuad];
        if (right != null)
            right.findSecondNearestNeighborCandidates(pivot, quadrant, backQuad, rightQuad, leftQuad, candidates);
    }

    /**
     * navigate to the node nearest to the specified coordinates and return it
     * @param x
     * @param y
     * @return the nearest node to the coordinates in question and its squared distance to the coordinates
     */
    public KeyValue<ONGNode, java.lang.Double> findNearestNeighborOf(double x, double y) {
        return this.findNearestNeighborOf(new ONGNode(x, y), new TreeSet<>(ONGNode.comparator), java.lang.Double.MAX_VALUE);
    }

    /**
     * navigate to the node nearest to the specified node's coordinates and return it
     * @param node
     * @param visited
     * @param nearestDistance
     * @return the nearest node to the coordinates in question and its squared distance to the coordinates or null if this node has already been visited/excluded from the search space
     */
    private KeyValue<ONGNode, java.lang.Double> findNearestNeighborOf(ONGNode node, TreeSet<ONGNode> visited, java.lang.Double nearestDistance) {
        if (visited.contains(this))                                                     // if we have already visited this
            return null;                                                                // stop here
        visited.add(this);                                                              // put this to the list of visited nodes

        int quadrant = node.getQuadrant(this);                                          // get the quadrant of node that contains this
        if (quadrant == -1) {                                                           // quadrant == -1 can only occur when this is at the exact position of node
            return new KeyValue<>(this, 0.0);                                           // return this, it cannot get better
        }

        double distance = node.distanceSq(this);                                        // get the distance of this to node
        if (distance < nearestDistance)                                                 // check if we got closer to node
            nearestDistance = distance;                                                 // update bestDistance

        if (this.neighbors[quadrant] != null)                                           // if this has a neighbor in the quadrant that certainly does not have a nearer neighbor for node
            visited.add(this.neighbors[quadrant]);                                      // exclude it from the search space by adding it to the visited nodes, if it was already in visited that is no problem as we are closer anyway

        // first check the neighbor in the quadrant of this that contains node (the direct path to node)
        ONGNode direct = this.neighbors[(quadrant + 2) % this.neighbors.length];        // get the neighbor of this in the quadrant of this that contains node
        KeyValue<ONGNode, java.lang.Double> found;
        if (direct != null) {                                                           // if there is a neighbor in the same quadrant of this as node
            found = direct.findNearestNeighborOf(node, visited, nearestDistance);       // recursion, see what we can find there
            if ((found == null) || (distance < found.getValue()))                       // if this is closer to node than what direct has found
                found = new KeyValue<>(this, distance);                                 // set found to this
            if ((found.getValue() == 0.0) || (node.getQuadrant(direct) == quadrant))    // if we found the node at the exact position or direct was in the shared region between this and node, thus certainly closer than anything form this' other quadrants
                return found;                                                           // we can stop here
        } else {                                                                        // if no neighbor in the same quadrant as node
            found = new KeyValue<>(this, distance);                                     // set found to this
        }

        double maxOrthogonalDistance = node.maxOrthogonalDistance(this);
        if (maxOrthogonalDistance > nearestDistance) {                                  // if, after traversing direct, this is too far away so we cannot expect finding anything useful in the other quadrants then these can be ignored
            visited.add(this.neighbors[(quadrant + 1) % this.neighbors.length]);        // exclude this neighbor from the search space
            visited.add(this.neighbors[(quadrant + 3) % this.neighbors.length]);        // exclude this neighbor from the search space
            return found;                                                               // return what this has found
        }

        // seems like what we found so far must be checked against what we may find in the other two quadrants of this

        // first choose the quadrant that is the bigger search space, i.e. has potentially more and closer (to node) nodes
        int quadrantIndex;
        if ((quadrant == ONGNode.NORTHEAST) || (quadrant == ONGNode.SOUTHWEST)) {
            if (node.distanceX(this) > node.distanceY(this)) {
                quadrantIndex = (quadrant + 3) % this.neighbors.length;
            } else {
                quadrantIndex = (quadrant + 1) % this.neighbors.length;
            }
        } else {
            if (node.distanceX(this) > node.distanceY(this)) {
                quadrantIndex = (quadrant + 1) % this.neighbors.length;
            } else {
                quadrantIndex = (quadrant + 3) % this.neighbors.length;
            }
        }

        ONGNode q1 = this.neighbors[(quadrant + 1) % this.neighbors.length];                // get the neighbor in this' quadrant
        if (q1 != null) {
            KeyValue<ONGNode, java.lang.Double> q1Found = q1.findNearestNeighborOf(node, visited, nearestDistance); // recursion, see what we can find in this quadrant
            if ((q1Found != null) && (found.getValue() > q1Found.getValue()))               // if what we found in the quadrant is closer than what we have in variable found (what we found so far)
                found = q1Found;                                                            // set it to found

            if (maxOrthogonalDistance > nearestDistance) {                                  // if, after traversing the quadrant, this is too far away so there cannot be a closer node in the other quadrant, then we can skip it
                visited.add(this.neighbors[(quadrantIndex + 2) % this.neighbors.length]);   // exclude this neighbor from the search space
                return found;                                                               // stop here and return what we have found
            }
        }

        ONGNode q2 = this.neighbors[(quadrantIndex + 2) % this.neighbors.length];           // get the neighbor in this' other quadrant
        if (q2 != null) {
            KeyValue<ONGNode, java.lang.Double> q2Found = q2.findNearestNeighborOf(node, visited, nearestDistance); // recursion, see what we can find in this quadrant
            if ((q2Found != null) && (found.getValue() > q2Found.getValue()))               // if what we found in the quadrant is closer than what we have in variable found (what we found so far)
                found = q2Found;                                                            // set it to found
        }

        return found;       // return what we have found
    }

    /**
     * Find the specified position's nearest neighbors and write them to a new node's neighbors array. The node is returned but NOT added to the graph.
     * @param x
     * @param y
     * @return a node with the neighborhood set that belongs to the coordinates, the node is NOT added to the graph
     */
    protected ONGNode setAllNearestNeighborsOf(double x, double y) {
        ONGNode node = new ONGNode(x, y);
        /*ONGNode collider =*/ this.setAllNearestNeighborsOf(node, new TreeSet<>(ONGNode.comparator));
        return node;
    }

    /**
     * Recursively traverse the graph, find the specified node's nearest neighbors and write them to the node's neighbors array.
     * @param node the node can or cannot be in the graph
     * @param visited with this treeset we keep track of nodes already visited so we do not get into loops
     * @return if a node was found at the exact same position of node (could even be the node itself) it is returned to indicate "collision", otherwise null
     */
    protected ONGNode setAllNearestNeighborsOf(ONGNode node, TreeSet<ONGNode> visited) {
        if (visited.contains(this))                                     // if we have already visited this node
            return null;                                                // stop here
        visited.add(this);                                              // otherwise add this node to the set


        int quadrant = node.getQuadrant(this);                          // get the quadrant of node that contains this

        if (quadrant == -1) {                                           // if this is at the exact same position as node (might even be node itself if it is already in the graph)
            node.neighbors[0] = this.neighbors[0];                      // the neighborhood is the same as for this
            node.neighbors[1] = this.neighbors[1];
            node.neighbors[2] = this.neighbors[2];
            node.neighbors[3] = this.neighbors[3];
            return this;                                                // return this
        }

        // mark the neighbor in the back of this as visited, it will certainly not lead us closer to node
        if (this.neighbors[quadrant] != null)
            visited.add(this.neighbors[quadrant]);

        int inverseQuadrant = (quadrant + 2) % this.neighbors.length;   // the quadrant of this that contains node

        ONGNode direct = this.neighbors[inverseQuadrant];               // get the next neighbor in the quadrant of this that contains node
        if (direct != null) {                                           // if there is a neighbor
            ONGNode collider = direct.setAllNearestNeighborsOf(node, visited);  // recursion
            if (collider != null)                                       // if we found a node at the exact same position as node, then we cannot get closer and the neighbors of collider are already set in node
                return collider;                                        // we are done and return collider

            if (node.getQuadrant(direct) == quadrant)                   // we found a node in the region between this and node, it is certainly closer than this and the neighborhood of node has been set during the recursion, we also don't need to look left and right
                return null;                                            // so we are done here
        }

        // we need to check the quadrants left and right of the direct quadrant
        ONGNode left = this.neighbors[(quadrant + 3) % this.neighbors.length];
        if (left != null)
            left.setAllNearestNeighborsOf(node, visited);

        ONGNode right = this.neighbors[(quadrant + 1) % this.neighbors.length];
        if (right != null)
            right.setAllNearestNeighborsOf(node, visited);

        if ((node.neighbors[quadrant] == null) || (node.distanceSq(node.neighbors[quadrant]) > node.distanceSq(this)))
            node.neighbors[quadrant] = this;

        return null;
    }

    /**
     * This method finds the inverse neighbors of this, i.e. those nodes in the graph that (would) have this as nearest neighbor.
     * This can or cannot be in the graph, however, its nearest neighbors from the graph must already be set (setAllNearestNeighborsOf())!
     * @return all nodes that (would) have this as nearest neighbor together with the respective quadrant index in which this would lie
     */
    public ArrayList<KeyValue<ONGNode, Integer>> findMyInverseNeighbors() {
        ArrayList<KeyValue<ONGNode, Integer>> results = new ArrayList<>();  // put every node that has this as direct neighbor in here
        ArrayList<ONGNode>[] deadZones = new ArrayList[]{new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>()};

        for (ONGNode node : this.neighbors) {                               // start the search at the direct neighbors in each quadrant
            if (node == null)                                               // if there is no neighbor
                continue;                                                   // continue with the next quadrant
            node.findMeInverseNeighborsOf(this, results, deadZones);        // start a search traversal at the node
        }

        return results;
    }

    /**
     * a helper method for finding the inverse direct neighbors
     * @param pivot
     * @param results the hashmap of all nodes that (would) have pivot as nearest neighbor together with the respective quadrant index in which pivot would lie
     * @param deadZones list of nodes already visited and their "dead space", so this node can check if it falls in one of those regions
     */
    private void findMeInverseNeighborsOf(ONGNode pivot, ArrayList<KeyValue<ONGNode, Integer>> results, ArrayList<ONGNode>[] deadZones) {
        int pivotQuadrant = this.getQuadrant(pivot);                                            // get the quadrant of this where pivot is
        int awayQuadrant = (pivotQuadrant + 2) % this.neighbors.length;                         // get the inverse quadrant

        ArrayList<ONGNode> toBeRemoved = new ArrayList<>();                                     // if we have to remove any nodes from deadZones[awayQuadrant] because they are in the dead zone of this, we will add them to this list and remove them after the following loop
        for (ONGNode check : deadZones[awayQuadrant]) {                                         // for each dead zone in the quadrant of pivot where this is
            if (check == this)                                                                  // if this is already in the dead zones list
                return;                                                                         // stop here
            int q = this.getQuadrant(check);                                                    // get the quadrant of this where the node lies against which we check
            if (q == pivotQuadrant)                                                             // if this is in the dead zone of the check node
                return;                                                                         // done here
            if (q == awayQuadrant)                                                              // if the check node is in the dead zone of this
                toBeRemoved.add(check);                                                         // mark the check node to be removed after the loop, but we don't break the loop because there can be more nodes to be removed
        }
        deadZones[awayQuadrant].removeAll(toBeRemoved);                                         // remove all nodes that fall into the dead zone of this
        deadZones[awayQuadrant].add(0, this);                                                   // add this to the dead zones in this' quadrant of pivot, right at the beginning of the list

        // check if this has/would have pivot as direct neighbor
        if ((this.neighbors[pivotQuadrant] == null)                                             // if there is no other neighbor where pivot would be placed
                || (this.neighbors[pivotQuadrant] == pivot)                                     // or this has pivot as direct neighbor
                || (this.distanceSq(this.neighbors[pivotQuadrant]) > this.distanceSq(pivot))) { // or the neighbor in that quadrant is further away, i.e., this would get pivot as direct neighbor
            results.add(new KeyValue<>(this, pivotQuadrant));                                   // add this to the list
        } else {                                                                                // seems like there is a neighbor in the direction of pivot that is closer
            this.neighbors[pivotQuadrant].findMeInverseNeighborsOf(pivot, results, deadZones);  // check it
        }

        // check the other two directions
        int nextQuadrant = (pivotQuadrant + 1) % this.neighbors.length;
        if (this.neighbors[nextQuadrant] != null)
            this.neighbors[nextQuadrant].findMeInverseNeighborsOf(pivot, results, deadZones);

        nextQuadrant = (pivotQuadrant + 3) % this.neighbors.length;
        if (this.neighbors[nextQuadrant] != null)
            this.neighbors[nextQuadrant].findMeInverseNeighborsOf(pivot, results, deadZones);
    }

    /**
     * compute Manhattan distance between this and the specified node
     * @param point2D
     * @return
     */
    public double distanceManhattan(Point2D point2D) {
        return this.distanceManhattan(point2D.getX(), point2D.getY());
    }

    /**
     * compute Manhattan distance between this and the given coordinates
     * @param x
     * @param y
     * @return
     */
    public double distanceManhattan(double x, double y) {
        return Math.abs(this.getX() - x) + Math.abs(this.getY() - y);
    }

    /**
     * horizontal distance of this node to the specified one
     * @param point2D
     * @return
     */
    public double distanceX(Point2D point2D) {
        return Math.abs(this.getX() - point2D.getX());
    }

    /**
     * vertical distance of this node to the specified one
     * @param point2D
     * @return
     */
    public double distanceY(Point2D point2D) {
        return Math.abs(this.getY() - point2D.getY());
    }

    /**
     * get the maximum orthogonal distance (either x distance or y distance) of this point to the specified one
     * @param point2D
     * @return
     */
    public double maxOrthogonalDistance(Point2D point2D) {
        return Math.max(this.distanceX(point2D), this.distanceY(point2D));
    }
}
