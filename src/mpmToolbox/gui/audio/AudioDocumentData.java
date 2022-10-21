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
import mpmToolbox.gui.audio.utilities.CursorPositions;
import mpmToolbox.projectData.alignment.Note;
import mpmToolbox.projectData.audio.SpectrogramImage;
import mpmToolbox.projectData.audio.WaveformImage;
import mpmToolbox.gui.mpmEditingTools.MpmEditingTools;
import mpmToolbox.projectData.audio.Audio;
import mpmToolbox.projectData.alignment.Alignment;
import mpmToolbox.supplementary.Tools;
import nu.xom.Element;

import javax.swing.*;
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
    private final WebMultiSplitPane splitPane  = new WebMultiSplitPane(Orientation.vertical);  // the vertical split pane contains the different visualizations that are going to be aligned (waveform, spectrogram etc.);
    private final WaveformPanel waveform;
    private final SpectrogramPanel spectrogram;
    private final TempoMapPanel tempoMap;

    private int channelNumber = -1;                                 // index of the waveform/channel to be rendered to image; -1 means all channels
    private long leftmostSample = -1;                                // index of the first sample to be rendered to image
    private long rightmostSample = -1;                               // index of the last sample to be rendered to image
    private double leftmostTick = 0.0;
    private double rightmostTick;

    private final CursorPositions playbackCursor = new CursorPositions(this);
    private CursorPositions mouseCursor = null;

    private Alignment alignment;                                    // this is the alignment with the piano roll overlay used in the sub-panels; it points either to the audio alignment or the alignment derived from the currently selected performance

    private final WebComboBox partChooser = new WebComboBox();      // with this combobox the user can select whether all musical part or only on individual part should be displayed in the piano roll overlay
    private final WebButton resetButton = new WebButton("Reset");   // this button re-initializes the alignment
    private final WebButton perf2AlignConvert = new WebButton("<html>Alignment &rarr; Performance</html>"); // this is the button to convert a performance to an alignment and vice versa

    /**
     * constructor
     * @param parent
     */
    public AudioDocumentData(@NotNull ProjectPane parent) {
        super("Audio", "Audio", null);
        this.parent = parent;

        this.rightmostTick = this.getParent().getMsm().getEndDate();

        this.makePartChooser();
        this.makeResetButton();
        this.makePerf2AlignButton();

        this.waveform = new WaveformPanel(this);
        this.spectrogram = new SpectrogramPanel(this);
        this.tempoMap = new TempoMapPanel(this);

        this.setComponent(this.audioPanel);
        this.setClosable(false);

        this.updateAudio(false);
        this.updateAlignment(false);
        this.updatePlaybackPosSample();
        this.updateAudioTools();
        this.makeListeners();

        this.draw();
    }

    /**
     * The audio frame should listen to interactions in the SyncPlayer.
     * The corresponding listeners are started here.
     */
    private void makeListeners() {
        // a listener for the SyncPlayer's performance chooser
        this.getParent().getSyncPlayer().getPerformanceChooser().addItemListener(itemEvent -> {
            if (itemEvent.getStateChange() == ItemEvent.SELECTED) {
//                System.out.println(itemEvent.toString());
                this.updateAlignment(true);
                this.updateTempomapPanel();
                this.updateAudioTools();
            }
        });

        // a listener for the SyncPlayer's audio chooser
        this.getParent().getSyncPlayer().getAudioChooser().addItemListener(itemEvent -> {
            if (itemEvent.getStateChange() == ItemEvent.SELECTED) {
                this.updateAudio(true); // communicate the selection to the audio analysis frame as this should also display it
            }
        });

        // a listener for the milliseconds offset spinner in the SyncPlayer
        this.getParent().getSyncPlayer().getOffsetSpinner().addChangeListener(changeListener -> {
            SwingUtilities.invokeLater(() -> {
                this.updatePlaybackPosSample();
                this.repaintAllComponents();
            });
        });

        // a listener for the playback slider in the SyncPlayer to draw a playback position cursor
        this.getParent().getSyncPlayer().getPlaybackSlider().addChangeListener(changeEvent -> {
            if (this.getAudio() != null) {
                SwingUtilities.invokeLater(() -> {
                    this.updatePlaybackPosSample();
                    this.repaintAllComponents();
                });
            } else if (this.getParent().getSyncPlayer().getSelectedPerformance() != null) {
                SwingUtilities.invokeLater(() -> {
                    this.updatePlaybackPosSample();
                    this.tempoMap.repaint();
                });
            }
        });
    }

    /**
     * A helper method to react on changes of the SyncPlayer's playback slider.
     * Invoke only if (this.getAudio() != null)!
     */
    private void updatePlaybackPosSample() {
        if (this.getAudio() == null) {      // if no audio selected
            if (this.getParent().getSyncPlayer().getSelectedPerformance() != null) {    // if only a performance is selected in the SyncPlayer, no audio, we do this
                double relativePosition = this.getParent().getSyncPlayer().getRelativePlaybackSliderPosition();
                double offset = Math.min(0.0, this.getParent().getSyncPlayer().getMillisecondsOffset());
                this.playbackCursor.setMilliseconds((this.getAlignment().getMillisecondsLength() * relativePosition) - offset);
            }
            return;
        }

        // if the audio player is playing, we can get the playback position directly from there
        if (this.getParent().getSyncPlayer().getAudioPlayer().isPlaying()) {
            this.playbackCursor.setMilliseconds((double) this.getParent().getSyncPlayer().getAudioPlayer().getMicrosecondPosition() / 1000.0);
            return;
        }

        // if the midi player is playing (audio player may have finished already), get the playback position from it
        if (this.getParent().getSyncPlayer().getMidiPlayer().isPlaying()) {
            this.playbackCursor.setMilliseconds((double) this.getParent().getSyncPlayer().getMidiPlayer().getMicrosecondPosition() / 1000.0);
            return;
        }

        // if none of the players is playing we compute the position from the source data
        int audioLength = this.getAudio().getNumberOfSamples();
        int alignmentLength = (this.getAlignment() == null) ? 0 : (int)((this.getAlignment().getMillisecondsLength() / 1000.0) * this.getAudio().getFrameRate());   // compute the sample count of the MIDI

        double offset = (this.getParent().getSyncPlayer().getMillisecondsOffset() / 1000.0) * this.getAudio().getFrameRate();
        if (offset < 0.0)   // negative offset is added to midi length
            alignmentLength += offset;

        if (offset > 0.0)
            this.playbackCursor.setSample(Math.round((Math.max(audioLength, alignmentLength) * this.getParent().getSyncPlayer().getRelativePlaybackSliderPosition()) + offset));
        else
            this.playbackCursor.setSample(Math.round(Math.max(audioLength, alignmentLength) * this.getParent().getSyncPlayer().getRelativePlaybackSliderPosition()));
    }

    /**
     * a helper method to compute the position of the playback cursor in the audio visualizations
     * @return
     */
    protected Double getRelativePlaybackPosInAudio() {
        if ((this.getAudio() == null) || (this.playbackCursor.getSample() < this.leftmostSample) || (this.playbackCursor.getSample() > this.rightmostSample))
            return null;

        return (double)(this.playbackCursor.getSample() - this.leftmostSample) / (this.rightmostSample - this.leftmostSample);
    }

    /**
     * access the playback cursor
     * @return
     */
    protected CursorPositions getPlaybackCursor() {
        return this.playbackCursor;
    }

    /**
     * access the mouse cursor
     * @return
     */
    protected CursorPositions getMouseCursor() {
        return this.mouseCursor;
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
                this.updateTempomapPanel();
                this.repaintAllComponents();
            }
        });
    }

    /**
     * define the button to reset the alignment
     */
    private void makeResetButton() {
        this.resetButton.setPadding(Settings.paddingInDialogs);
        this.resetButton.setToolTip("Re-initialize the piano roll.");
        this.resetButton.addActionListener(actionEvent -> {
            this.alignment.reset();

            // in case of audio alignment being reset, scale the initial alignment to the milliseconds length of the audio; so all notes are visible and in a good starting position
            if (this.getParent().getSyncPlayer().isAudioAlignmentSelected()) {
                Audio audio = this.getParent().getSyncPlayer().getSelectedAudio();
                audio.initAlignment(this.getParent().getMsm());
            }

            this.alignment.recomputePianoRoll();
            this.repaintAllComponents();
        });
    }

    /**
     * define the button to convert a performance to an alignment and vice versa
     */
    private void makePerf2AlignButton() {
        this.perf2AlignConvert.setPadding(Settings.paddingInDialogs);
        this.perf2AlignConvert.setToolTip("<html>Convert audio alignment to performance or vice versa.</html>");
        this.perf2AlignConvert.addActionListener(actionEvent -> {
            if (this.getParent().getSyncPlayer().isAudioAlignmentSelected()) {          // if an audio alignment is selected, we create a performance from the current timing data
                Performance performance = MpmEditingTools.createPerformanceDialog();    // open dialog for performance creation
                if (!this.getParent().getMpm().addPerformance(performance))             // add the performance to the MPM
                    return;                                                             // if performance adding failed, cancel

                this.getAlignment().exportPerformance(performance);

                // update MPM tree and SyncPlayer, and select the performance in both
                this.getParent().getMpmTree().setSelectedNode(this.getParent().getMpmTree().reloadRootNode().findChildNode(performance, false));
                this.getParent().getSyncPlayer().addPerformance(performance, true);     // the SyncPlayer must update its performance chooser
            } else {                                                                    // if a performance is selected,
                this.getAudio().setAlignment(this.getAlignment());                      // we transfer the current timing data to the audio alignment
                this.getParent().getSyncPlayer().selectAlignmentPerformance();          // select the alignment in the SyncPlayer so any further interaction in the piano roll will be on the alignment and not on the performance
            }
        });
    }

    /**
     * this enables or disables the part chooser and other controls according to whether there is a performance selected in the syncPlayer
     */
    public void updateAudioTools() {
        boolean enable = (this.getAudio() != null) && (this.alignment != null);

//        this.partChooser.setEnabled(enable);
        this.resetButton.setEnabled(enable);

        this.perf2AlignConvert.setEnabled(enable);
        this.perf2AlignConvert.setText((this.getParent().getSyncPlayer().getSelectedPerformance() == null) ? "<html>Alignment &rarr; Performance</html>" : "<html>Performance &rarr; Alignment</html>");
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
//        WebMultiSplitPane splitPane = new WebMultiSplitPane(Orientation.vertical);  // the vertical split pane contains the different visualizations that are going to be aligned (waveform, spectrogram etc.)
        this.splitPane.setOneTouchExpandable(true);                                      // dividers have buttons for maximizing a component
        this.splitPane.setContinuousLayout(true);                                        // when the divider is moved the content is continuously redrawn
        this.splitPane.add(this.waveform);
        this.splitPane.add(this.spectrogram);
        this.splitPane.add(this.tempoMap);

        GridBagLayout gridBagLayout = (GridBagLayout) this.audioPanel.getLayout();
        Tools.addComponentToGridBagLayout(this.audioPanel, gridBagLayout, this.splitPane, 0, 0, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH, GridBagConstraints.CENTER);

        // the panel  with the buttons
        WebPanel buttonPanel = new WebPanel(new GridBagLayout());
        GridBagLayout buttonLayout = (GridBagLayout) buttonPanel.getLayout();
        Tools.addComponentToGridBagLayout(buttonPanel, buttonLayout, this.partChooser, 0, 1, 1, 1, 1.0, 0.0, 0, 0, GridBagConstraints.NONE, GridBagConstraints.CENTER);
        Tools.addComponentToGridBagLayout(buttonPanel, buttonLayout, this.resetButton, 1, 1, 1, 1, 1.0, 0.0, 0, 0, GridBagConstraints.NONE, GridBagConstraints.CENTER);
        Tools.addComponentToGridBagLayout(buttonPanel, buttonLayout, this.perf2AlignConvert, 2, 1, 1, 1, 1.0, 0.0, 0, 0, GridBagConstraints.NONE, GridBagConstraints.CENTER);
        Tools.addComponentToGridBagLayout(this.audioPanel, gridBagLayout, buttonPanel, 0, 1, 1, 1, 1.0, 0.0, 0, 0, GridBagConstraints.NONE, GridBagConstraints.CENTER);
    }

    /**
     * getter for the waveform panel
     * @return
     */
    public WaveformPanel getWaveformPanel() {
        return this.waveform;
    }

    /**
     * a getter for the spectrogram panel
     * @return
     */
    public SpectrogramPanel getSpectrogramPanel() {
        return this.spectrogram;
    }

    /**
     * a getter for the tempomap panel
     * @return
     */
    public TempoMapPanel getTempoMapPanel() {
        return this.tempoMap;
    }

    /**
     * The sequence at which the child components update their visualizations is important.
     * This method takes care of it.
     */
    protected void repaintAllComponents() {
        this.waveform.repaint();
        this.spectrogram.repaint();
        this.tempoMap.repaint();
    }

    /**
     * compute which sample the mouse cursor is pointing at
     * @param x horizontal pixel position in an audio domain panel
     * @return
     */
    protected long getSampleIndex(double x) {
        double relativePosition = x / this.getWaveformPanel().getWidth();
        return Math.round((relativePosition * (this.rightmostSample - this.leftmostSample)) + this.leftmostSample);
    }

    /**
     * compute which tick the mouse cursor is pointing at
     * @param x horizontal pixel position in a tick domain panel
     * @return
     */
    protected double getTickIndex(double x) {
        double relativePosition = x / this.getTempoMapPanel().getWidth();
        return (relativePosition * (this.rightmostTick - this.leftmostTick)) + this.leftmostTick;
    }

    /**
     * this communicates the mouse position to all child components;
     * basically it updates the mouse cursor data in this container class, so others can access it
     * @param e
     */
    protected void communicateMousePositionToAllComponents(MouseEvent e) {
        if (e == null) {
            this.mouseCursor = null;
            return;
        }

        if (this.mouseCursor == null)
            this.mouseCursor = new CursorPositions(this);

        if (this.getWaveformPanel().mouseInThisPanel() || this.getSpectrogramPanel().mouseInThisPanel()) {
            this.mouseCursor.setAudioX(e.getX());
            return;
        }
        // corresponds to: if (this.getTempoMapPanel().mouseInThisPanel)
        this.mouseCursor.setTicksX(e.getX());
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
     * @param doRepaint
     */
    public void updateAlignment(boolean doRepaint) {
        if (this.parent.getSyncPlayer().isAudioAlignmentSelected()) {
            this.alignment = this.getAudio().getAlignment();
        } else {
            Performance performance = this.parent.getSyncPlayer().getSelectedPerformance();

            if (performance != null) {
                this.alignment = new Alignment(performance.perform(this.parent.getMsm()), null);    // get the performance as an instance of Alignment
//                for (Part part : this.alignment.getParts()) {                                       // since the alignment is computed from a performance all notes should be marked as fixed
//                    for (Note note : part.getNoteSequence()) {
//                        note.setFixed(true);
//                    }
//                }
            } else {
                this.alignment = null;
            }
        }

        if (doRepaint) {
            SwingUtilities.invokeLater(() -> {
                this.updatePlaybackPosSample();
                this.repaintAllComponents();
            });
        }
    }

    /**
     * update the information that is displayed in the tempomap panel
     */
    public void updateTempomapPanel() {
        this.tempoMap.update();
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
        Audio audio = this.getAudio();
        if (audio != null) {
            this.channelNumber = -1;                                // all channels
            this.leftmostSample = 0;                                // first sample
            this.rightmostSample = audio.getNumberOfSamples() - 1;  // sample count
            this.updatePlaybackPosSample();
        }

        this.waveform.setAudio();
        this.spectrogram.setAudio();

        if (doRepaint) {
            this.repaintAllComponents();
        }
    }

    /**
     * a getter for the audio data that are currently displayed in the audio tab
     * @return
     */
    public Audio getAudio() {
        return this.parent.getSyncPlayer().getSelectedAudio();
    }

    /**
     * return the audio's waveform image
     * @param width
     * @param height
     * @return
     */
    public WaveformImage getWaveformImage(int width, int height) {
        if (this.getAudio() == null)
            return null;
        Audio audio = this.getAudio();
        audio.computeWaveformImage(this.channelNumber, (int) this.leftmostSample, (int) this.rightmostSample, width, height);
        return audio.getWaveformImage();
    }

    /**
     * return the audio's spectrogram image
     * @return
     */
    public SpectrogramImage getSpectrogramImage() {
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
    public long getLeftmostSample() {
        return this.leftmostSample;
    }

    /**
     * sets the index of the leftmost sample to be displayed
     * @param leftmostSample
     */
    public void setLeftmostSample(long leftmostSample) {
        this.leftmostSample = leftmostSample;
    }

    /**
     * a getter for the rightmost sample index to be displayed
     * @return
     */
    public long getRightmostSample() {
        return this.rightmostSample;
    }

    /**
     * sets the index of the rightmost sample to be displayed
     * @param rightmostSample
     */
    public void setRightmostSample(long rightmostSample) {
        this.rightmostSample = rightmostSample;
    }

    /**
     * access the leftmost tick
     * @return
     */
    public double getLeftmostTick() {
        return this.leftmostTick;
    }

    /**
     * set the leftmost tick
     * @param leftmostTick
     */
    public void setLeftmostTick(double leftmostTick) {
        this.leftmostTick = leftmostTick;
    }

    /**
     * access the rightmost tick
     * @return
     */
    public double getRightmostTick() {
        return this.rightmostTick;
    }

    /**
     * set the rightmost tick
     * @param rightmostTick
     */
    public void setRightmostTick(double rightmostTick) {
        this.rightmostTick = rightmostTick;
    }

    /**
     * process a mouse drag event; to be invoked by sub-panels WaveformPanel, SpectrogramPanel
     * @param e
     */
    protected void scroll(MouseEvent e) {
        if (this.getMouseCursor() == null) {
            this.getMouseCursor().setAudioX(e.getX());
            return;
        }

        long leftmost = this.getLeftmostSample();
        long rightmost = this.getRightmostSample();
        double sampleOffset = (double)((rightmost - leftmost) * (this.getMouseCursor().getAudioX() - e.getPoint().x)) / this.getWaveformPanel().getWidth();   // this computes how many horizontal pixels the mouse has moved, than scales it by the amount of samples per horizontal pixel so we know how many pixels we want to move the leftmost and rightmost sample index

        this.communicateMousePositionToAllComponents(e);
        this.scroll(sampleOffset);
    }

    /**
     * this shifts the visualisations left or right by the specified offset
     * @param sampleOffset offset in samples
     */
    protected void scroll(double sampleOffset) {
        if (this.parent.getAudio() == null)
            return;

        sampleOffset = (sampleOffset > 0) ? Math.min(this.getAudio().getNumberOfSamples() - 1 - this.rightmostSample, Math.round(sampleOffset)) : Math.max(-this.leftmostSample, Math.round(sampleOffset));  // we have to check that we don't go beyond the first and last sample; as we move those indices only in integer steps there is a certain numeric error causing the samples moving with a bit different speed than the mouse was moved, but it is not problematic

        if (sampleOffset == 0.0)            // if no change
            return;                         // done, we don't update the mouse position so we can check next time if in sum the mouse moved far enough

        // move the sample indices
        this.setLeftmostSample((int) (this.leftmostSample + sampleOffset));
        this.setRightmostSample((int) (this.rightmostSample + sampleOffset));
        this.spectrogram.updateScroll();

        // update the cursor positions
//        this.mouseCursor.setSample(this.mouseCursor.getSample());
        this.playbackCursor.setSample(this.playbackCursor.getSample());

        this.repaintAllComponents();
    }

    /**
     * this is used when the visualisations are zoomed
     * @param pivotSample
     * @param zoomFactor
     */
    private void zoom(long pivotSample, double zoomFactor) {
        if (zoomFactor == 0.0)
            return;

        if (zoomFactor < 0.0) {             // zoom in
            long leftmostSample = pivotSample - (int) ((pivotSample - this.leftmostSample) * zoomFactor);
            long rightmostSample = (int) ((this.rightmostSample - pivotSample) * zoomFactor) + pivotSample;
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

        // update the cursor positions
//        this.mouseCursor.setSample(this.mouseCursor.getSample());
        this.playbackCursor.setSample(this.playbackCursor.getSample());

        this.repaintAllComponents();
    }

    /**
     * process a mouse wheel moved event; to be invoked by sub-panels WaveformPanel, SpectrogramPanel
     * @param e
     */
    protected void mouseWheelMoved(MouseWheelEvent e){
        if ((this.getAudio() == null) || (e.getWheelRotation() == 0))
            return;

        long pivotSample = this.getSampleIndex(e.getPoint().getX());
        double zoomFactor = (e.getWheelRotation() < 0) ? 0.9 : 1.1;

        this.communicateMousePositionToAllComponents(e);
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
