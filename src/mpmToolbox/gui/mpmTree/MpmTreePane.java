package mpmToolbox.gui.mpmTree;

import com.alee.api.annotations.NotNull;
import com.alee.laf.panel.WebPanel;
import com.alee.laf.scroll.WebScrollPane;
import com.alee.managers.style.StyleId;
import mpmToolbox.gui.ProjectPane;

import java.awt.*;

/**
 * This class holds all contents of the MPM Dockable Frame.
 * @author Axel Berndt
 */
public class MpmTreePane extends WebPanel {
    @NotNull private final ProjectPane projectPane;                 // a link to the parent project pane to access its data, midi player etc.
    @NotNull private final MpmTree mpmTree;

    /**
     * constructor
     * @param projectPane
     */
    public MpmTreePane(@NotNull ProjectPane projectPane) {
        this.projectPane = projectPane;
        this.mpmTree = new MpmTree(this.projectPane);

//        this.add(new WebLabel("placeholder north", WebLabel.CENTER), BorderLayout.NORTH);

        this.add(this.makeMpmTree(), BorderLayout.CENTER);

//        this.add(new WebLabel("placeholder south", WebLabel.CENTER), BorderLayout.SOUTH);
    }

    /**
     * create the MPM tree in a ScrollPane to be added to this
     * @return
     */
    private WebScrollPane makeMpmTree() {
        WebScrollPane scrollPane = new WebScrollPane(this.mpmTree);
        scrollPane.setStyleId(StyleId.scrollpaneUndecoratedButtonless);
        return scrollPane;
    }

    /**
     * a getter for the MPM tree
     * @return
     */
    public MpmTree getMpmTree() {
        return this.mpmTree;
    }
}
