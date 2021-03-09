package mpmToolbox.supplementary;

import java.io.File;
import java.util.ArrayList;

/**
 * This class is used to keep track of recently opened files.
 * @author Axel Berndt
 */

public class RecentOpened {
    private final int maxSize;                   // the maximum size
    private final ArrayList<File> list;          // a list of recently opened files

    /**
     * constructor
     * @param maxSize the maximum size of the list
     */
    public RecentOpened(int maxSize) {
        this.maxSize = maxSize;
        this.list = new ArrayList<>();
    }

    /**
     * getter for the maximum size of the list
     * @return
     */
    public int getMaxSize() {
        return this.maxSize;
    }

    /**
     * get the current size of the list
     * @return
     */
    public int size() {
        return this.list.size();
    }

    /**
     * are there recent opened projects?
     * @return
     */
    public boolean isEmpty() {
        return this.list.isEmpty();
    }

    /**
     * get the index'th file in the list
     * @param index
     * @return
     */
    public synchronized File get(int index) {
        return this.list.get(index);
    }

    /**
     * add a file to the list
     * @param file
     */
    public synchronized void add(File file) {
        // if the file is already in the list, remove it
        boolean containsFile;
        do {
            containsFile = this.list.remove(file);
        } while (containsFile);

        this.list.add(0, file);             // add file to the front of the list

        if (this.list.size() >= this.maxSize)  // if the size limit is broken
            this.list.remove(maxSize -1);       // remove the last element
    }

    /**
     * empty the list
     */
    public void clear() {
        this.list.clear();
    }

    /**
     * print the list
     * @return
     */
    public String toString() {
        String recentFiles = "";
        for (File recent : this.list) {
            if (recent.exists())
                recentFiles = recentFiles.concat(recent.getAbsolutePath()).concat("\n");
        }
        return recentFiles;
    }
}
