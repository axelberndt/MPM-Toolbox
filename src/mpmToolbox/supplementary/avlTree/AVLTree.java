package mpmToolbox.supplementary.avlTree;

import java.util.Comparator;

/**
 * An AVL Tree data structure, based on https://www.baeldung.com/java-avl-trees.
 * @author Axel Berndt
 */
public class AVLTree<T> {
    private final Comparator<? super T> comparator;
    private AVLNode<T> root = null;

    /**
     * constructor
     * @param comparator
     */
    public AVLTree(Comparator<? super T> comparator) {
        this.comparator = comparator;
    }

    public AVLNode<T> getRoot() {
        return this.root;
    }

    public int height() {
        return this.root == null ? -1 : this.root.height;
    }

    private int height(AVLNode<T> n) {
        return n == null ? -1 : n.height;
    }

    private void updateHeight(AVLNode<T> n) {
        n.height = 1 + Math.max(this.height(n.leftChild), this.height(n.rightChild));
    }

    public AVLNode<T> find(T key) {
        AVLNode<T> current = this.root;
        while (current != null) {
            if (current.key == key)
                return current;

            int comparison = this.comparator.compare(current.key, key);
            current = (comparison < 0) ? current.rightChild : current.leftChild;    // if (current.key < key) ...
        }
        return null;
    }

    public void insert(T key) {
        this.root = insert(this.root, key);
    }

    private AVLNode<T> insert(AVLNode<T> node, T key) {
        if (node == null)
            return new AVLNode<T>(key);

        int comparison = this.comparator.compare(node.key, key);

        if (comparison == 0)
            throw new RuntimeException("duplicate key!");

        if (comparison > 0)
            node.leftChild = this.insert(node.leftChild, key);
        else // if (comparison < 0)
            node.rightChild = this.insert(node.rightChild, key);

        return this.rebalance(node);
    }

    public void delete(T key) {
        this.root = this.delete(this.root, key);
    }

    private AVLNode<T> delete(AVLNode<T> node, T key) {
        if (node == null)
            return node;

        int comparison = this.comparator.compare(node.key, key);

        if (comparison == 0) {  // found the node to be deleted
            if (node.leftChild == null || node.rightChild == null) {
                node = (node.leftChild == null) ? node.rightChild : node.leftChild;
            } else {
                AVLNode<T> mostLeftChild = this.mostLeftChild(node.rightChild);
                node.key = mostLeftChild.key;
                node.rightChild = this.delete(node.rightChild, node.key);
            }
        }
        else if (comparison > 0)
            node.leftChild = this.delete(node.leftChild, key);
        else // if (node.key < key)
            node.rightChild = this.delete(node.rightChild, key);

        return this.rebalance(node);
    }

    private AVLNode<T> mostLeftChild(AVLNode<T> node) {
        AVLNode<T> current = node;
        /* loop down to find the leftmost leaf */
        while (current.leftChild != null) {
            current = current.leftChild;
        }
        return current;
    }

    public int getBalance(AVLNode<T> n) {
        return (n == null) ? 0 : this.height(n.rightChild) - this.height(n.leftChild);
    }
    private AVLNode<T> rebalance(AVLNode<T> z) {
        updateHeight(z);
        int balance = this.getBalance(z);
        if (balance > 1) {
            if (this.height(z.rightChild.rightChild) > this.height(z.rightChild.leftChild)) {
                z = this.rotateLeft(z);
            } else {
                z.rightChild = this.rotateRight(z.rightChild);
                z = this.rotateLeft(z);
            }
        } else if (balance < -1) {
            if (this.height(z.leftChild.leftChild) > this.height(z.leftChild.rightChild)) {
                z = this.rotateRight(z);
            } else {
                z.leftChild = this.rotateLeft(z.leftChild);
                z = this.rotateRight(z);
            }
        }
        return z;
    }

    private AVLNode<T> rotateRight(AVLNode<T> y) {
        AVLNode<T> x = y.leftChild;
        AVLNode<T> z = x.rightChild;
        x.rightChild = y;
        y.leftChild = z;
        this.updateHeight(y);
        this.updateHeight(x);
        return x;
    }

    private AVLNode<T> rotateLeft(AVLNode<T> y) {
        AVLNode<T> x = y.rightChild;
        AVLNode<T> z = x.leftChild;
        x.leftChild = y;
        y.rightChild = z;
        this.updateHeight(y);
        this.updateHeight(x);
        return x;
    }
}