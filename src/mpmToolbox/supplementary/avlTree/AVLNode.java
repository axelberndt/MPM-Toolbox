package mpmToolbox.supplementary.avlTree;

/**
 * A node in an AVL Tree.
 * @author Axel Berndt
 */
public class AVLNode<T> {
    T key;
    int height = 0;
    AVLNode<T> leftChild = null;
    AVLNode<T> rightChild = null;

    /**
     * default constructor to create null node
     */
    public AVLNode() {
        key = null;
    }

    /**
     * parameterized constructor
     * @param key
     */
    public AVLNode(T key) {
        this.key = key;
    }
}
