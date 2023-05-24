package mpmToolbox.gui.syncPlayer;

import com.alee.laf.button.WebButton;
import com.alee.laf.combobox.WebComboBox;
import com.alee.laf.label.WebLabel;
import com.alee.laf.panel.WebPanel;
import com.alee.laf.window.WebDialog;
import mpmToolbox.gui.Settings;
import mpmToolbox.gui.syncPlayer.utilities.RecordingDeviceChooserItem;
import mpmToolbox.projectData.audio.Audio;
import mpmToolbox.supplementary.Tools;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * This class represents an audio recorder. Create an instance and invoke openDialog().
 * This will return null or an Audio object with the recording.
 */
public class RecorderDialog extends WebDialog<RecorderDialog> {
    protected GridBagLayout contentPanelLayout = new GridBagLayout();
    protected WebPanel contentPanel = new WebPanel(this.contentPanelLayout);
    private Audio recording = null;             // the audio recording to be made

    /**
     * constructor
     */
    public RecorderDialog() {
        super();

        this.setTitle("Audio Recorder");
        this.setIconImages(Settings.getIcons(null));
        this.setResizable(Settings.debug);
        this.setModal(true);
        this.initKeyboardShortcuts();
        this.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        // close procedure when clicking on X
        this.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                recording = null;
                dispose();
            }
        });

        // content panel
        this.contentPanel.setPadding(Settings.paddingInDialogs);
        this.makeContentPanel();

        // Run / Cancel panel
        GridBagLayout runPanelLayout = new GridBagLayout();
        WebPanel okPanel = new WebPanel(runPanelLayout);
        okPanel.setPadding(Settings.paddingInDialogs);

        WebButton run = new WebButton("Store", actionEvent -> {
            this.dispose();
        });
        run.setHorizontalAlignment(WebButton.CENTER);
        run.setPadding(Settings.paddingInDialogs*2, Settings.paddingInDialogs, Settings.paddingInDialogs*2, Settings.paddingInDialogs);
        Tools.addComponentToGridBagLayout(okPanel, runPanelLayout, run, 0, 0, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH, GridBagConstraints.LINE_START);

        WebButton cancel = new WebButton("Cancel", actionEvent -> {
            this.recording = null;
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
    public void makeContentPanel() {
        WebLabel deviceLabel = new WebLabel("Recording Device:");
        deviceLabel.setHorizontalAlignment(WebLabel.RIGHT);
        deviceLabel.setPadding(Settings.paddingInDialogs);
        this.addToContentPanel(deviceLabel, 0, 0, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH);

        WebComboBox deviceChooser = new WebComboBox();
        deviceChooser.setToolTip("Choose recording device.");
        deviceChooser.setPadding(Settings.paddingInDialogs);
//        deviceChooser.add(new RecordingDeviceChooserItem("Choose Recording Device", null));

    }

    /**
     * this opens the dialog window
     * @return the recorded audio data or null
     */
    public Audio openDialog() {
        this.setVisible(true);          // start the dialog

        // after the dialog closed do the following

        return this.recording;
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
                recording = null;
                dispose();
            }
        });

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "Run");
        this.getRootPane().getActionMap().put("Run", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                dispose();
            }
        });
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
