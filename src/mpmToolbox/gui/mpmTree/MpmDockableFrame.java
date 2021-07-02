package mpmToolbox.gui.mpmTree;

import com.alee.api.data.CompassDirection;
import com.alee.extended.dock.WebDockableFrame;
import com.alee.laf.button.WebButton;
import com.alee.managers.icon.Icons;
import meico.mei.Helper;
import meico.mpm.Mpm;
import mpmToolbox.gui.ProjectPane;

/**
 * This dockable frame displays the MPM tree.
 * @author Axel Berndt
 */
public class MpmDockableFrame extends WebDockableFrame {
    private final ProjectPane parent;
    private MpmTreePane mpmTreePane;
    private final WebButton newMpmButton = new WebButton("Create New MPM"); // this button is displayed in the MPM tree pane when there is no MPM in the project, yet

    public MpmDockableFrame(ProjectPane parent) {
        super("mpmFrame", "Music Performance Markup");

        this.setIcon(Icons.table);
        this.setClosable(false);                   // when closed the frame disappears and cannot be reopened by the user, thus, this is set false
        this.setMaximizable(false);                // it is also set to not maximizable
        this.setPosition(CompassDirection.east);

        this.parent = parent;

        if (this.getParentProjectPane().getMpm() == null) {
            this.mpmTreePane = null;
            this.add(this.newMpmButton);
            this.minimize();
        } else {
            this.mpmTreePane = new MpmTreePane(this.getParentProjectPane());
            this.add(this.mpmTreePane);            // create the contents of the frame
        }

        // define the button for creating a new MPM document
        this.newMpmButton.addActionListener(actionEvent -> {
            Mpm newMpm = Mpm.createMpm();
            newMpm.setFile(Helper.getFilenameWithoutExtension(this.getParentProjectPane().getMsm().getFile().getAbsolutePath()) + ".mpm");
            newMpm.addPerformance("empty performance");                             // a valid MPM document has to have at least one performance, even if it is empty; so we add one here
            this.getParentProjectPane().setMpm(newMpm);
        });
    }

    /**
     * add an MPM to the Project
     * @param mpm
     */
    public synchronized void setMpm(Mpm mpm) {
        if (mpm == null)
            return;
        if (this.mpmTreePane != null)
            this.remove(this.mpmTreePane);
        else
            this.remove(this.newMpmButton);

        this.mpmTreePane = new MpmTreePane(this.getParentProjectPane());
        this.add(this.mpmTreePane);
        this.restore();    // open the frame
        this.validate();   // this is necessary so the component display gets updated
        this.repaint();    // update the component display
    }

    /**
     * delete the MPM from this project
     */
    public synchronized void removeMpm() {
        if (this.mpmTreePane == null)
            return;

//        this.minimize();
        this.remove(this.mpmTreePane);
        this.mpmTreePane = null;
        this.add(this.newMpmButton);
        this.validate();   // this is necessary so the component display gets updated
        this.repaint();    // update the component display
    }

    /**
     * a getter for the MPM tree
     * @return
     */
    public synchronized MpmTree getMpmTree() {
        if (this.mpmTreePane == null)
            return null;
        return this.mpmTreePane.getMpmTree();
    }

    /**
     * get the parental ProjectPane
     * @return
     */
    public ProjectPane getParentProjectPane() {
        return parent;
    }
}
