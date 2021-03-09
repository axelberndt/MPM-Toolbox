package mpmToolbox.gui.mpmTree;

import com.alee.api.annotations.NotNull;
import meico.mpm.elements.styles.GenericStyle;

import java.util.HashMap;

/**
 * This class groups MPM style definitions of a specified type, e.g. articulationStyles.
 * @author Axel Berndt
 */
public class MpmStyleCollection {
    @NotNull protected final String type;
    @NotNull protected final HashMap<String, GenericStyle> collection;

    /**
     * constructor
     * @param type
     * @param collection
     */
    protected MpmStyleCollection(String type, HashMap<String, GenericStyle> collection) {
        this.type = type;
        this.collection = collection;
    }

    /**
     * get the type of the collection
     * @return
     */
    public String getType() {
        return this.type;
    }
}
