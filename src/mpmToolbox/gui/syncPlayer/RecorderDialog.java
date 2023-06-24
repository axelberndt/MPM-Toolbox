package mpmToolbox.gui.syncPlayer;

import com.alee.laf.button.WebButton;
import com.alee.laf.combobox.WebComboBox;
import com.alee.laf.label.WebLabel;
import com.alee.laf.panel.WebPanel;
import com.alee.laf.progressbar.WebProgressBar;
import com.alee.laf.window.WebDialog;
import mpmToolbox.gui.Settings;
import mpmToolbox.gui.syncPlayer.utilities.RecordThread;
import mpmToolbox.gui.syncPlayer.utilities.RecordingDeviceChooserItem;
import mpmToolbox.supplementary.Tools;

import javax.sound.sampled.*;
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
    protected final GridBagLayout contentPanelLayout = new GridBagLayout();
    protected final WebPanel contentPanel = new WebPanel(this.contentPanelLayout);
    private final WebComboBox deviceChooser = new WebComboBox();
    private final WebButton recordButton = new WebButton("<html><p style=\"color: " + Settings.errorColorHex + "; font-size:  x-large\">\u26AB</p></html>");
    private final WebProgressBar vuMeter = new WebProgressBar(WebProgressBar.HORIZONTAL, 0, 100); // orientation, min, max
    private final AudioFormat format = new AudioFormat(44100.0f, 16, 1, true, false); // sampleRate, sampleSizeInBits, channels, signed, bigEndian
    private RecordThread recordThread = null;
    private AudioInputStream recording = null;             // the audio recording to be made

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
                stopRecording();
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

        WebButton store = new WebButton("Store", actionEvent -> {
            this.stopRecording();
            this.dispose();
        });
        store.setHorizontalAlignment(WebButton.CENTER);
        store.setPadding(Settings.paddingInDialogs*2, Settings.paddingInDialogs, Settings.paddingInDialogs*2, Settings.paddingInDialogs);
        Tools.addComponentToGridBagLayout(okPanel, runPanelLayout, store, 0, 0, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH, GridBagConstraints.LINE_START);

        WebButton cancel = new WebButton("Cancel", actionEvent -> {
            this.stopRecording();
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

        // put all available recording devices in a combobox; see Java reference: https://docs.oracle.com/javase/tutorial/sound/capturing.html
        this.deviceChooser.setToolTip("Choose Recording Device.");
        this.deviceChooser.setPadding(Settings.paddingInDialogs);
        DataLine.Info targetDataLineInfo = new DataLine.Info(TargetDataLine.class, this.format);    // create the info from the required format
        for (Mixer.Info mixerInfo : AudioSystem.getMixerInfo()) {                                   // from each available mixer
            Mixer mixer = AudioSystem.getMixer(mixerInfo);
//            System.out.println(mixerInfo.getName() + ": " + mixerInfo.getDescription());
            for (Line.Info lineInfo : mixer.getTargetLineInfo(targetDataLineInfo)) {                // from each available TargetDataLine (audio input line) that supports the targetDataLineInfo
//                System.out.println("    " + lineInfo);
                try {
                    this.deviceChooser.addItem(new RecordingDeviceChooserItem(mixerInfo.getName(), (TargetDataLine) mixer.getLine(lineInfo)));   // obtain the TargetDataLine and add the entry to the combobox
                } catch (LineUnavailableException ignored) {
                }
            }
        }
        this.addToContentPanel(this.deviceChooser, 1, 0, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH);

        // VU meter for input monitoring
        this.vuMeter.setString("");
        this.vuMeter.setBoldFont(true);
        this.vuMeter.setForeground(Settings.errorColor);
        this.vuMeter.setStringPainted(true);
        this.vuMeter.setPadding(Settings.paddingInDialogs);
        this.addToContentPanel(this.vuMeter, 0, 1, 2, 1, 1.0, 1.0, 0, 10, GridBagConstraints.BOTH);

        // record button
        this.recordButton.setToolTip("<html><center>Start/Stop Recording<br>Overwrites previous take!</center></html>");
        this.recordButton.setPadding(Settings.paddingInDialogs);
        this.recordButton.addActionListener(actionEvent -> {
            if (this.recordThread == null) {                                                                // if no recording running
                if (this.startRecording()) {                                                                // start recording; if success
                    this.recordButton.setText("<html><p style=\"font-size:  x-large\">\u25FC</p></html>");  // set the recordButton's symbol to ◼
                    this.deviceChooser.setEnabled(false);
                }
            } else {                                                                                                                            // if recording in progress
                this.recordButton.setText("<html><p style=\"color: " + Settings.errorColorHex + "; font-size:  x-large\">\u26AB</p></html>");   // set the recordButton's symbol to ⚫
                this.deviceChooser.setEnabled(true);
                this.stopRecording();
                this.vuMeter.setString("");
                this.vuMeter.setValue(0);
            }
        });
        this.addToContentPanel(this.recordButton, 0, 2, 2, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH);
    }

    /**
     * this opens the dialog window
     * @return the recorded audio data or null
     */
    public AudioInputStream openDialog() {
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
                stopRecording();
                recording = null;
                dispose();
            }
        });

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "Store");
        this.getRootPane().getActionMap().put("Store", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                stopRecording();
                dispose();
            }
        });
    }

    /**
     * start the audio recording
     * @return success
     */
    private boolean startRecording() {
        if (this.deviceChooser.getSelectedItem() == null)
            return false;

        TargetDataLine line = ((RecordingDeviceChooserItem) this.deviceChooser.getSelectedItem()).getValue();
        this.recordThread = new RecordThread(line, this.vuMeter);
        this.recordThread.start();

        return this.recordThread.isAlive();
    }

    /**
     * terminate the recording
     */
    private void stopRecording() {
        if (this.recordThread == null)
            return;

        this.recordThread.terminate();      // this invocation blocks until the thread terminates, so the next call works properly
        this.recording = this.recordThread.getRecording();
        this.recordThread = null;
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
