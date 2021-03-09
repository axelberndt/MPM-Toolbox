package mpmToolbox.gui.audio;

import com.alee.extended.tab.DocumentData;
import com.alee.laf.panel.WebPanel;
import mpmToolbox.ProjectData;

/**
 * ...
 * @author Axel Berndt
 */
public class AudioDocumentData extends DocumentData<WebPanel> {
    private final ProjectData parentProject;

    /**
     * constructor
     * @param id
     * @param title
     * @param parentProject
     */
    public AudioDocumentData(String id, String title, ProjectData parentProject) {
        super(id, title, new WebPanel());       // this.getComponent() returns the WebPanel
        this.parentProject = parentProject;
        this.setClosable(false);
    }
}
