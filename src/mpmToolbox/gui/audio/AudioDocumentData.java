package mpmToolbox.gui.audio;

import com.alee.api.annotations.NotNull;
import com.alee.extended.split.WebMultiSplitPane;
import com.alee.extended.tab.DocumentData;
import com.alee.laf.panel.WebPanel;
import mpmToolbox.gui.ProjectPane;

/**
 * A custom DocumentData object for the audio analysis component.
 * @author Axel Berndt
 */
public class AudioDocumentData extends DocumentData<WebPanel> {
    protected final ProjectPane parent;
    private final WebPanel audioPanel = new WebPanel();
    private final WebMultiSplitPane splitPane = new WebMultiSplitPane();

    /**
     * constructor
     * @param parent
     */
    public AudioDocumentData(@NotNull ProjectPane parent) {
        super("Audio", "Audio", null);

        this.setComponent(this.audioPanel);
        this.setClosable(false);
        this.parent = parent;
//        this.draw();
    }

    /**
     * Get the ProjectPane object that this belongs to.
     * @return
     */
    public ProjectPane getParent() {
        return this.parent;
    }
}
