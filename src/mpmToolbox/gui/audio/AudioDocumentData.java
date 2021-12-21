package mpmToolbox.gui.audio;

import com.alee.api.annotations.NotNull;
import com.alee.api.data.Orientation;
import com.alee.extended.split.WebMultiSplitPane;
import com.alee.extended.tab.DocumentData;
import com.alee.laf.button.WebButton;
import com.alee.laf.combobox.WebComboBox;
import com.alee.laf.panel.WebPanel;
import meico.mei.Helper;
import meico.mpm.elements.Performance;
import meico.supplementary.KeyValue;
import mpmToolbox.gui.ProjectPane;
import mpmToolbox.gui.Settings;
import mpmToolbox.projectData.Audio;
import mpmToolbox.projectData.alignment.Alignment;
import mpmToolbox.supplementary.Tools;
import nu.xom.Element;

import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;

/**
 * A custom DocumentData object for the audio analysis component.
 * @author Axel Berndt
 */
public class AudioDocumentData extends DocumentData<WebPanel> {
    protected final ProjectPane parent;
    private final WebPanel audioPanel = new WebPanel(new GridBagLayout());  // the panel that contains everything in this tab

    private final WaveformPanel waveform;
    private final SpectrogramPanel spectrogram;
//    private final PianoRollPanel pianoRoll;

    private int channelNumber = -1;                             // index of the waveform/channel to be rendered to image; -1 means all channels
    private int leftmostSample = -1;                            // index of the first sample to be rendered to image
    private int rightmostSample = -1;                           // index of the last sample to be rendered to image

    private Alignment alignment;                                // this is the alignment with the piano roll overlay used in the sub-panels

    private final WebComboBox partChooser = new WebComboBox();  // with this combobox the user can select whether all musical part or only on individual part should be displayed in the piano roll overlay
//    private final WebSwitch globalLocalSwitch = new WebSwitch();
    private final WebButton resetAlignment = new WebButton("Reset Alignment");

    /**
     * constructor
     * @param parent
     */
    public AudioDocumentData(@NotNull ProjectPane parent) {
        super("Audio", "Audio", null);
        this.parent = parent;

        this.makePartChooser();
//        this.makeGlobalLocalSwitch();
        this.makeResetButton();

        this.waveform = new WaveformPanel(this);
        this.spectrogram = new SpectrogramPanel(this);
//        this.pianoRoll = new PianoRollPanel(this);

        this.setComponent(this.audioPanel);
        this.setClosable(false);

        this.updateAudio(false);
        this.updateAlignment(false);
        this.updateAudioTools();

        this.draw();
    }

    /**
     * helper method for the constructor; it creates the contents of the part chooser
     */
    private void makePartChooser() {
        this.partChooser.setPadding(Settings.paddingInDialogs);
        this.partChooser.setToolTip("Select the part to be displayed in the piano roll overlay.");

        this.partChooser.addItem(new PartChooserItem("All parts", null));

        for (Element partElt : this.getParent().getMsm().getParts()) {
            int number = Integer.parseInt(Helper.getAttributeValue("number", partElt));
            String name = "Part " + number + " " + Helper.getAttributeValue("name", partElt);
            this.partChooser.addItem(new PartChooserItem(name, number));
        }

        this.partChooser.addItemListener(itemEvent -> {
            if (itemEvent.getStateChange() == ItemEvent.SELECTED) {
                this.repaintAllComponents();
            }
        });
    }

//    /**
//     * helper method for the constructor; it creates the switch between local and global editing mode
//     */
//    private void makeGlobalLocalSwitch() {
//        WebLabel global = new WebLabel("Global");
//        global.setPadding(Settings.paddingInDialogs);
//
//        WebLabel local = new WebLabel("Part");
//        local.setPadding(Settings.paddingInDialogs);
//
//        this.globalLocalSwitch.setSwitchComponents(global, local);
//
//        WebLabel gripperLabel = new WebLabel("Edit");
//        gripperLabel.setPadding(Settings.paddingInDialogs);
//        gripperLabel.setHorizontalAlignment(WebLabel.CENTER);
//        this.globalLocalSwitch.getGripper().add(gripperLabel);
//
//        this.globalLocalSwitch.setSelected(true);
//    }

    /**
     * define the button to reset the alignment
     */
    private void makeResetButton() {
        this.resetAlignment.setPadding(Settings.paddingInDialogs);
        this.resetAlignment.addActionListener(actionEvent -> {
            this.alignment.reset();

            // in case of audio alignment being reset, scale the initial alignment to the milliseconds length of the audio; so all notes are visible and in a good starting position
            if (this.getParent().getSyncPlayer().isAudioAlignmentSelected()) {
                Audio audio = this.getParent().getSyncPlayer().getSelectedAudio();
                double milliseconds = ((double) audio.getNumberOfSamples() / audio.getFrameRate()) * 1000.0;
                this.alignment.scaleOverallTiming(milliseconds);
            }

            this.alignment.recomputePianoRoll();
            this.repaintAllComponents();
        });
    }

    /**
     * this enables or disables the part chooser according to whether there is a performance selected in the syncPlayer
     */
    public void updateAudioTools() {
        this.partChooser.setEnabled(this.alignment != null);
        this.resetAlignment.setEnabled(this.alignment != null);
    }

    /**
     * get the number of the selected part
     * @return the number or null if all parts are selected
     */
    protected Integer getPianoRollPartNumber() {
        if (this.partChooser.getSelectedItem() == null)
            return null;

        return ((PartChooserItem) this.partChooser.getSelectedItem()).getValue();
    }



    /**
     * this draws the content of the audio analysis frame
     */
    private void draw() {
        WebMultiSplitPane splitPane = new WebMultiSplitPane(Orientation.vertical);  // the vertical split pane contains the different visualizations that are going to be aligned (waveform, spectrogram etc.)
        splitPane.setOneTouchExpandable(true);                                      // dividers have buttons for maximizing a component
        splitPane.setContinuousLayout(true);                                        // when the divider is moved the content is continuously redrawn
        splitPane.add(this.waveform);
        splitPane.add(this.spectrogram);
//        splitPane.add(this.pianoRoll);    // TODO: this pane will be used to display and edit timing curves

        GridBagLayout gridBagLayout = (GridBagLayout) this.audioPanel.getLayout();
        Tools.addComponentToGridBagLayout(this.audioPanel, gridBagLayout, splitPane, 0, 0, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH, GridBagConstraints.CENTER);

        // the panel  with the buttons
        WebPanel buttonPanel = new WebPanel(new GridBagLayout());
        GridBagLayout buttonLayout = (GridBagLayout) buttonPanel.getLayout();
        Tools.addComponentToGridBagLayout(buttonPanel, buttonLayout, this.partChooser, 0, 1, 1, 1, 1.0, 0.0, 0, 0, GridBagConstraints.NONE, GridBagConstraints.CENTER);
//        Tools.addComponentToGridBagLayout(buttonPanel, buttonLayout, this.globalLocalSwitch, 1, 1, 1, 1, 1.0, 0.0, 0, 0, GridBagConstraints.NONE, GridBagConstraints.CENTER);
        Tools.addComponentToGridBagLayout(buttonPanel, buttonLayout, this.resetAlignment, 2, 1, 1, 1, 1.0, 0.0, 0, 0, GridBagConstraints.NONE, GridBagConstraints.CENTER);
        Tools.addComponentToGridBagLayout(this.audioPanel, gridBagLayout, buttonPanel, 0, 1, 1, 1, 1.0, 0.0, 0, 0, GridBagConstraints.NONE, GridBagConstraints.CENTER);
    }

    /**
     * getter for the waveform panel
     * @return
     */
    protected WaveformPanel getWaveformPanel() {
        return this.waveform;
    }

    /**
     * a getter for the spectrogram panel
     * @return
     */
    protected SpectrogramPanel getSpectrogramPanel() {
        return this.spectrogram;
    }

//    /**
//     * a getter for the piano roll panel
//     * @return
//     */
//    protected PianoRollPanel getPianoRollPanel() {
//        return this.pianoRoll;
//    }

    /**
     * The sequence at which the child components update their visualizations is important.
     * This method takes care of it.
     */
    protected void repaintAllComponents() {
        this.waveform.repaint();
        this.spectrogram.repaint();
//        this.pianoRoll.repaint();
    }

    /**
     * this communicates the mouse event/position to all child components
     * @param e
     */
    protected void communicateMouseEventToAllComponents(MouseEvent e) {
        this.waveform.setMousePosition(e);
        this.spectrogram.setMousePosition(e);
//        this.pianoRoll.setMousePosition(e);
    }

    /**
     * Get the ProjectPane object that this belongs to.
     * @return
     */
    public ProjectPane getParent() {
        return this.parent;
    }

    /**
     * update the alignment data according to the currently selected performance in the SyncPlayer
     */
    public void updateAlignment(boolean doRepaint) {
        if (this.parent.getSyncPlayer().isAudioAlignmentSelected())
            this.alignment = this.getAudio().getAlignment();
        else {
            Performance performance = this.parent.getSyncPlayer().getSelectedPerformance();
            this.alignment = (performance == null) ? null : new Alignment(performance.perform(this.parent.getMsm()), null);
        }

        if (doRepaint)
            this.repaintAllComponents();
    }

    /**
     * access the current alignment
     * @return
     */
    public Alignment getAlignment() {
        return this.alignment;
    }

    /**
     * update the audio data according to the currently selected performance in the SyncPlayer
     * @param doRepaint
     */
    public void updateAudio(boolean doRepaint) {
        Audio audio = this.parent.getSyncPlayer().getSelectedAudio();
        if (audio != null) {
            this.channelNumber = -1;                                // all channels
            this.leftmostSample = 0;                                // first sample
            this.rightmostSample = audio.getNumberOfSamples() - 1;  // sample count
        }

        this.waveform.setAudio();
        this.spectrogram.setAudio();

        if (doRepaint)
            this.repaintAllComponents();
    }

    /**
     * a getter for the audio data that are currently displayed in the audio tab
     * @return
     */
    protected Audio getAudio() {
        return this.parent.getSyncPlayer().getSelectedAudio();
    }

    /**
     * return the audio's waveform image
     * @param width
     * @param height
     * @return
     */
    protected Audio.WaveformImage getWaveformImage(int width, int height) {
        if (this.getAudio() == null)
            return null;
        Audio audio = this.getAudio();
        audio.computeWaveformImage(this.channelNumber, this.leftmostSample, this.rightmostSample, width, height);
        return audio.getWaveformImage();
    }

    /**
     * return the audio's spectrogram image
     * @return
     */
    protected Audio.SpectrogramImage getSpectrogramImage() {
        if (this.getAudio() == null)
            return null;
        return this.parent.getSyncPlayer().getSelectedAudio().getSpectrogramImage();
    }

    /**
     * a getter for the index of the channel to be displayed; -1 means all channels
     * @return
     */
    protected int getChannelNumber() {
        return this.channelNumber;
    }

    /**
     * sets the number of the channel to be displayed; -1 means all channels
     * @param channelNumber
     */
    protected void setChannelNumber(int channelNumber) {
        this.channelNumber = channelNumber;
    }

    /**
     * a getter for the leftmost sample index to be displayed
     * @return
     */
    protected int getLeftmostSample() {
        return this.leftmostSample;
    }

    /**
     * sets the index of the leftmost sample to be displayed
     * @param leftmostSample
     */
    protected void setLeftmostSample(int leftmostSample) {
        this.leftmostSample = leftmostSample;
    }

    /**
     * a getter for the rightmost sample index to be displayed
     * @return
     */
    protected int getRightmostSample() {
        return this.rightmostSample;
    }

    /**
     * sets the index of the rightmost sample to be displayed
     * @param rightmostSample
     */
    protected void setRightmostSample(int rightmostSample) {
        this.rightmostSample = rightmostSample;
    }

    /**
     * this shifts the visualisations left or right by the specified offset
     * @param sampleOffset offset in samples
     */
    private void scroll(double sampleOffset) {
        if (this.parent.getAudio() == null)
            return;

        sampleOffset = (sampleOffset > 0) ? Math.min(this.getAudio().getNumberOfSamples() - 1 - this.rightmostSample, Math.round(sampleOffset)) : Math.max(-this.leftmostSample, Math.round(sampleOffset));  // we have to check that we don't go beyond the first and last sample; as we move those indices only in integer steps there is a certain numeric error causing the samples moving with a bit different speed than the mouse was moved, but it is not problematic

        if (sampleOffset == 0.0)            // if no change
            return;                         // done, we don't update the mouse position so we can check next time if in sum the mouse moved far enough

        // move the sample indices
        this.setLeftmostSample((int) (this.leftmostSample + sampleOffset));
        this.setRightmostSample((int) (this.rightmostSample + sampleOffset));
        this.spectrogram.updateScroll();

        this.repaintAllComponents();
    }

    /**
     * this is used when the visualisations are zoomed
     * @param pivotSample
     * @param zoomFactor
     */
    private void zoom(int pivotSample, double zoomFactor) {
        if (zoomFactor == 0.0)
            return;

        if (zoomFactor < 0.0) {             // zoom in
            int leftmostSample = pivotSample - (int) ((pivotSample - this.leftmostSample) * zoomFactor);
            int rightmostSample = (int) ((this.rightmostSample - pivotSample) * zoomFactor) + pivotSample;
            if ((rightmostSample - leftmostSample) > 1) {   // make sure there are at least two samples to be drawn, if we zoom too far in, left==right, we cannot zoom out again
                this.setLeftmostSample(leftmostSample);
                this.setRightmostSample(rightmostSample);
            }
        }
        else if (zoomFactor > 0.0) {        // zoom out
            this.setLeftmostSample(pivotSample - (int) Math.ceil((pivotSample - this.leftmostSample) * zoomFactor));
            if (this.leftmostSample < 0)
                this.setLeftmostSample(0);
            this.setRightmostSample((int) Math.ceil((this.rightmostSample - pivotSample) * zoomFactor) + pivotSample);
            if (this.rightmostSample >= this.getAudio().getNumberOfSamples())
                this.setRightmostSample(this.getAudio().getNumberOfSamples() - 1);
        }

        this.spectrogram.updateZoom();

        this.repaintAllComponents();
    }

    /**
     * process a mouse drag event; to be invoked by sub-panels WaveformPanel, SpectrogramPanel
     * @param e
     */
    protected void scroll(MouseEvent e) {
        if (this.getWaveformPanel().mousePosition == null) {
            this.getWaveformPanel().setMousePosition(e);
            return;
        }

        int leftmost = this.getLeftmostSample();
        int rightmost = this.getRightmostSample();
        double sampleOffset = (double)((rightmost - leftmost) * (this.getWaveformPanel().mousePosition.x - e.getPoint().x)) / this.getWaveformPanel().getWidth();   // this computes how many horizontal pixels the mouse has moved, than scales it by the amount of samples per horizontal pixel so we know how many pixels we want to move the leftmost and rightmost sample index

        this.communicateMouseEventToAllComponents(e);
        this.scroll(sampleOffset);
    }

    /**
     * process a mouse wheel moved event; to be invoked by sub-panels WaveformPanel, SpectrogramPanel
     * @param e
     */
    protected void mouseWheelMoved(MouseWheelEvent e){
        if ((this.getAudio() == null) || (e.getWheelRotation() == 0))
            return;

        int pivotSample = this.getWaveformPanel().getSampleIndex(e.getPoint().getX());
        double zoomFactor = (e.getWheelRotation() < 0) ? 0.9 : 1.1;

        this.communicateMouseEventToAllComponents(e);
        this.zoom(pivotSample, zoomFactor);
    }

    /**
     * This class represents an item in the part chooser combobox of the audio analysis widget.
     * @author Axel Berndt
     */
    private static class PartChooserItem extends KeyValue<String, Integer> {
        /**
         * This constructor creates a part chooser item with the specified name key and part number.
         * @param string
         */
        private PartChooserItem(String string, Integer partNumber) {
            super(string, partNumber);
        }

        /**
         * All combobox items require this method. The override here makes sure that the string being returned
         * is the performance's name instead of some Java Object ID.
         * @return
         */
        @Override
        public String toString() {
            return this.getKey();
        }
    }

}
