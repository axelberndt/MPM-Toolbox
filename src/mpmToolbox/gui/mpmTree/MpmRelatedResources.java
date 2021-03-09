package mpmToolbox.gui.mpmTree;

import meico.mpm.elements.metadata.RelatedResource;

import java.util.ArrayList;

/**
 * This class groups MPM resource elements.
 * @author Axel Berndt
 */
public class MpmRelatedResources {
    protected ArrayList<RelatedResource> relatedResources;

    /**
     * constructor
     * @param relatedResources
     */
    protected MpmRelatedResources(ArrayList<RelatedResource> relatedResources) {
        this.relatedResources = relatedResources;
    }
}
