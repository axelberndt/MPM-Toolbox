package mpmToolbox.projectData.alignment;

import com.alee.api.annotations.NotNull;
import com.alee.laf.button.WebButton;
import com.alee.laf.panel.WebPanel;
import com.alee.laf.window.WebDialog;
import mpmToolbox.gui.Settings;
import mpmToolbox.projectData.audio.Audio;
import mpmToolbox.supplementary.Tools;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

/**
 * Abstract class for implementation of audio-to-symbolic music alignment algorithms.
 * It also extends WebDialog, so a dialog window can be opened that gives access to all
 * parameters. The dialog also contains the Run button to trigger the execution via method compute().
 * If the class is used without the dialog, the execution can be triggered directly by invoking compute().
 * Once a new class is defined and ready for use, add an instance of it to
 * AudioDocumentData's alignmentComputationChooser list. This registers it in the GUI.
 * @author Axel Berndt
 */
public abstract class AbstractAlignmentComputation extends WebDialog<AbstractAlignmentComputation> {
    protected GridBagLayout contentPanelLayout = new GridBagLayout();
    protected WebPanel contentPanel = new WebPanel(this.contentPanelLayout);
    private boolean run = false;     // this is set true by clicking the Run button or pressing ENTER

    /**
     * Constructor, the constructor method of the implementation should start with super(...) to
     * make sure that the implementation has a meaningful name.
     * @param name name your implementation of this abstract class, e.g. "FastDTW" or "UltraFastMPSearch" or "Smith-Waterman" or "Needleman-Wunsch" or "Longest Common Subsequence" ...
     */
    public AbstractAlignmentComputation(String name) {
        super();

        this.setTitle(name);
        this.setIconImages(Settings.getIcons(null));
        this.setResizable(Settings.debug);
        this.setModal(true);
        this.initKeyboardShortcuts();

        // content panel
        this.contentPanel.setPadding(Settings.paddingInDialogs);
        this.makeContentPanel();

        // Run / Cancel panel
        GridBagLayout runPanelLayout = new GridBagLayout();
        WebPanel okPanel = new WebPanel(runPanelLayout);
        okPanel.setPadding(Settings.paddingInDialogs);

        WebButton run = new WebButton("Run", actionEvent -> {
            this.run = true;
            this.dispose();
        });
        run.setHorizontalAlignment(WebButton.CENTER);
        run.setPadding(Settings.paddingInDialogs*2, Settings.paddingInDialogs, Settings.paddingInDialogs*2, Settings.paddingInDialogs);
        Tools.addComponentToGridBagLayout(okPanel, runPanelLayout, run, 0, 0, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH, GridBagConstraints.LINE_START);

        WebButton cancel = new WebButton("Cancel", actionEvent -> {
            this.run = false;
            this.dispose();
        });
        cancel.setHorizontalAlignment(WebButton.CENTER);
        cancel.setPadding(Settings.paddingInDialogs*2, Settings.paddingInDialogs, Settings.paddingInDialogs*2, Settings.paddingInDialogs);
        Tools.addComponentToGridBagLayout(okPanel, runPanelLayout, cancel, 1, 0, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH, GridBagConstraints.LINE_START);

        // put all together
        GridBagLayout layout = new GridBagLayout();
        this.setLayout(layout);
        Tools.addComponentToGridBagLayout(this, layout, this.contentPanel, 0, 0, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH, GridBagConstraints.LINE_START);
        Tools.addComponentToGridBagLayout(this, layout, okPanel, 0, 1, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH, GridBagConstraints.LINE_START);

        this.pack();                                    // fit the size to the content

        this.setLocationRelativeTo(null);
    }

    /**
     * This method must be implemented to create the real contents of the dialog. Consider that the content panel's layout manager is of type GridBagLayout.
     */
    public abstract void makeContentPanel(); //{
//        WebLabel placeholder = new WebLabel("This is a placeholder. You have to implement method makeContentPanel()!");
//        placeholder.setHorizontalAlignment(WebLabel.RIGHT);
//        placeholder.setPadding(Settings.paddingInDialogs);
//        this.addToContentPanel(placeholder, 0, 0, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH);
//    }

    /**
     * This method triggers the alignment computation for the specified Audio instance.
     * The result of the computation should be encoded in the Alignment component of the Audio instance.
     * @param audio
     * @return the input audio with altered alignment component
     */
    public abstract Audio compute(@NotNull Audio audio);

    /**
     * this opens the dialog window
     * @param audio
     * @return input object with altered alignment data
     */
    public Audio openDialog(@NotNull Audio audio) {
        this.setVisible(true);          // start the dialog

        // after the dialog closed do the following

        if (!this.isRun())              // if cancelled
            return audio;               // return the input unaltered

        return this.compute(audio);     // if cancelled
    }

    /**
     * a helper method to add components to the content panel
     * @param component
     * @param x horizontal position in grid
     * @param y vertical position in grid
     * @param width how many horizontal grid boxes are covered by the component
     * @param height how many vertical grid boxes are covered by the component
     * @param weightx default is 1.0
     * @param weighty default is 1.0
     * @param ipadx default is 0
     * @param ipady default is 0
     * @param fill default is GridBagConstraints.BOTH
     */
    public void addToContentPanel(Component component, int x, int y, int width, int height, double weightx, double weighty, int ipadx, int ipady, int fill) {
        Tools.addComponentToGridBagLayout(this.contentPanel, this.contentPanelLayout, component, x, y, width, height, weightx, weighty, ipadx, ipady, fill, GridBagConstraints.LINE_START);
    }

    /**
     * this initializes the keyboard shortcuts (ESC, ENTER)
     */
    private void initKeyboardShortcuts() {
        InputMap inputMap = this.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "Cancel");
        this.getRootPane().getActionMap().put("Cancel", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                run = false;
                dispose();
            }
        });

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "Run");
        this.getRootPane().getActionMap().put("Run", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                run = true;
                dispose();
            }
        });
    }

    /**
     * read the run value
     * @return
     */
    public boolean isRun() {
        return this.run;
    }

    /**
     * get the name of this implementation
     * @return
     */
    @Override
    public String toString() {
        return this.getTitle();
    }
}
