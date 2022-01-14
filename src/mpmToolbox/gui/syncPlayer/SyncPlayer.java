package mpmToolbox.gui.syncPlayer;

import com.alee.laf.button.WebButton;
import com.alee.laf.combobox.WebComboBox;
import com.alee.laf.label.WebLabel;
import com.alee.laf.panel.WebPanel;
import com.alee.laf.slider.WebSlider;
import com.alee.laf.spinner.WebSpinner;
import meico.audio.AudioPlayer;
import meico.midi.Midi;
import meico.midi.MidiPlayer;
import meico.mpm.elements.Performance;
import mpmToolbox.gui.ProjectPane;
import mpmToolbox.gui.Settings;
import mpmToolbox.gui.syncPlayer.utilities.AudioChooserItem;
import mpmToolbox.gui.syncPlayer.utilities.PerformanceChooserItem;
import mpmToolbox.projectData.Audio;
import mpmToolbox.supplementary.Tools;

import javax.sound.midi.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

/**
 * This class implements the Audio and MIDI player for MPM Toolbox.
 * It is able to play both media in parallel.
 * @author Axel Berndt
 */
public class SyncPlayer extends WebPanel {
    protected final ProjectPane parent;                                   // a link to the parent project pane to access its data, midi player etc.

    protected final WebButton playButton = new WebButton("\u25B6");       //  ◼ "\u25FC", ▶ "\u25B6"
    protected static final int sliderMax = 1000000;
    protected final WebSlider playbackSlider = new WebSlider(WebSlider.HORIZONTAL, 0, sliderMax, 0);  // the slider that indicates playback position

    private final AudioPlayer audioPlayer = new AudioPlayer();
    private final MidiPlayer midiPlayer = new MidiPlayer();

    protected final WebComboBox performanceChooser = new WebComboBox();
    private final PerformanceChooserItem noPerformanceRendering = new PerformanceChooserItem("No performance rendering");
    private final PerformanceChooserItem rawPerformance = new PerformanceChooserItem(Performance.createPerformance("Play raw notes, no performance"));
    protected final PerformanceChooserItem alignmentPerformance = new PerformanceChooserItem("Alignment of currently chosen audio");

    private final WebComboBox midiPortChooser = new WebComboBox();

    protected final WebComboBox audioChooser = new WebComboBox();
    protected final WebSpinner skipMillisecondsInAudioPlayback = new WebSpinner(new SpinnerNumberModel(0L, -9999999999L, 9999999999L, 1L));

    protected PlaybackRunnable runnable = null;

    /**
     * constructor
     */
    public SyncPlayer(ProjectPane parent) throws MidiUnavailableException {
        super(new GridBagLayout());
        this.parent = parent;

        if (Settings.getSoundbank() != null)
            this.midiPlayer.loadSoundbank(Settings.getSoundbank());
        else
            this.midiPlayer.loadDefaultSoundbank();

        this.makeGui();
    }

    /**
     * a getter for the MIDI player
     * @return
     */
    public synchronized MidiPlayer getMidiPlayer() {
        return this.midiPlayer;
    }

    /**
     * a getter for the audio player
     * @return
     */
    public synchronized AudioPlayer getAudioPlayer() {
        return this.audioPlayer;
    }

    /**
     * this creates the Sync Player GUI
     */
    private void makeGui() {
        // make the performance chooser
        this.updatePerformanceList();
        this.performanceChooser.setPadding(Settings.paddingInDialogs / 4);
        this.performanceChooser.setToolTip("Select the performance rendering to be played.");
        this.performanceChooser.addItemListener(itemEvent -> {
            if (itemEvent.getStateChange() == ItemEvent.SELECTED) {
//                System.out.println(itemEvent.toString());
                if (this.parent.getAudioFrame() != null) {
                    this.parent.getAudioFrame().updateAlignment(true);
                }
                this.parent.getAudioFrame().updateAudioTools();
            }
        });
        Tools.addComponentToGridBagLayout(this, (GridBagLayout) this.getLayout(), this.performanceChooser, 0, 0, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH, GridBagConstraints.LINE_START);

        // make the MIDI port label
        WebLabel midiPortLabel = new WebLabel("MIDI Out:");
        midiPortLabel.setToolTip("Select the MIDI port to output performance rendering. Default is \"Gervill\".");
        midiPortLabel.setHorizontalAlignment(WebLabel.RIGHT);
        midiPortLabel.setPadding(Settings.paddingInDialogs / 4);
        Tools.addComponentToGridBagLayout(this, (GridBagLayout) this.getLayout(), midiPortLabel, 1, 0, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH, GridBagConstraints.LINE_START);

        // make the MIDI port chooser
        this.updateMidiPortList();
        this.midiPortChooser.setPadding(Settings.paddingInDialogs / 4);
        this.midiPortChooser.setToolTip("Select the MIDI port to output performance rendering. Default is \"Gervill\".");
        this.midiPortChooser.addActionListener(actionEvent -> {
            MidiDevice.Info item = (MidiDevice.Info) this.midiPortChooser.getSelectedItem();// get the device info from the chooser; it is required to get the corresponding device from the MidiSystem
            if (item == null)                                                               // if nothing meaningful is chosen
                return;                                                                     // done
            try {
                if (item == this.midiPlayer.getSynthesizer().getDeviceInfo())               // ensure that we do not instantiate a new Gervill synth if that is chosen, but use the one that midiPlayer is already holding
                    this.midiPlayer.setMidiOutputPort(this.midiPlayer.getSynthesizer());    // Gervill was chosen, hence, use the midiPlayer's native one instead of a new instance
                else                                                                        // something else was chosen
                    this.midiPlayer.setMidiOutputPort(MidiSystem.getMidiDevice(item));      // switch to it
            } catch (MidiUnavailableException e) {
                e.printStackTrace();
            }
        });
        Tools.addComponentToGridBagLayout(this, (GridBagLayout) this.getLayout(), this.midiPortChooser, 2, 0, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH, GridBagConstraints.LINE_START);

        // make the audio chooser
        this.updateAudioList();
        this.audioChooser.setPadding(Settings.paddingInDialogs / 4);
        this.audioChooser.setToolTip("Select the audio recording to be played.");
        this.audioChooser.addItemListener(itemEvent -> {
            if (itemEvent.getStateChange() == ItemEvent.SELECTED) {
//                System.out.println(itemEvent.toString());
                Audio audio = ((AudioChooserItem) itemEvent.getItem()).getValue();
                if (audio == null) {                                                            // audio is unselected
                    if (this.performanceChooser.getSelectedItem() == this.alignmentPerformance) // if alignment performance option was selected
                        this.performanceChooser.setSelectedIndex(0);                            // jump to the first option
                    this.performanceChooser.removeItem(this.alignmentPerformance);              // remove the alignment performance option from the performance chooser
                } else {
                    this.updatePerformanceList();                                               // update the performance chooser list to add/delete the alignment performance option
                }
                this.parent.getAudioFrame().updateAudio(true);                                  // communicate the selection to the audio analysis frame as this should also display it
            }
        });
        Tools.addComponentToGridBagLayout(this, (GridBagLayout) this.getLayout(), this.audioChooser, 0, 1, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH, GridBagConstraints.LINE_START);

        // make the skip label, spinner and ms label
        WebLabel skipLabel = new WebLabel("skip:");
        skipLabel.setToolTip("<html><center>Skip initial time in the audio recording.<br>Negative values are skipped from the MIDI performance rendering.</center></html>");
        skipLabel.setHorizontalAlignment(WebLabel.RIGHT);
        skipLabel.setPadding(Settings.paddingInDialogs / 4);
        Tools.addComponentToGridBagLayout(this, (GridBagLayout) this.getLayout(), skipLabel, 1, 1, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH, GridBagConstraints.LINE_START);

        Tools.addComponentToGridBagLayout(this, (GridBagLayout) this.getLayout(), this.skipMillisecondsInAudioPlayback, 2, 1, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH, GridBagConstraints.LINE_START);

        WebLabel millisecondsLabel = new WebLabel("ms");
        millisecondsLabel.setToolTip("Milliseconds");
        millisecondsLabel.setHorizontalAlignment(WebLabel.LEFT);
        millisecondsLabel.setPadding(Settings.paddingInDialogs / 4);
        Tools.addComponentToGridBagLayout(this, (GridBagLayout) this.getLayout(), millisecondsLabel, 3, 1, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH, GridBagConstraints.LINE_START);

        // make the play button
        this.playButton.setPadding((int) (Settings.paddingInDialogs * 1.5));
        this.playButton.addActionListener(actionEvent -> this.triggerPlayback());
        Tools.addComponentToGridBagLayout(this, (GridBagLayout) this.getLayout(), this.playButton, 4, 0, 1, 2, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH, GridBagConstraints.LINE_START);

        // make the slider
        this.makeSlider();
    }

    /**
     * This fills the performance chooser list.
     * The method should be called when the available performances have changed.
     */
    public void updatePerformanceList() {
        PerformanceChooserItem selectedItem = (PerformanceChooserItem) this.performanceChooser.getSelectedItem();   // store the previously selected item
        PerformanceChooserItem selectThis = null;

        // temporarily switch the ItemListener off; otherwise it would always fire when an item is added
        ItemListener itemListener = this.performanceChooser.getItemListeners()[0];
        this.performanceChooser.removeItemListener(itemListener);

        this.performanceChooser.removeAllItems();
        this.performanceChooser.addItem(this.noPerformanceRendering);
        if (selectedItem == this.noPerformanceRendering)
            selectThis = this.noPerformanceRendering;

        if (this.getSelectedAudio() != null) {
            this.performanceChooser.addItem(this.alignmentPerformance);
            if (selectedItem == this.alignmentPerformance)
                selectThis = this.alignmentPerformance;
        }

        this.performanceChooser.addItem(this.rawPerformance);
        if (selectedItem == this.rawPerformance)
            selectThis = this.rawPerformance;

        if (this.parent.getMpm() != null) {
            for (Performance performance : this.parent.getMpm().getAllPerformances()) {
                PerformanceChooserItem item = new PerformanceChooserItem(performance);
                this.performanceChooser.addItem(item);

                if ((selectThis == null)
                        && (selectedItem != null)
                        && item.toString().equals(selectedItem.toString()))  // if this is the item that was previously selected
                    selectThis = item;
            }
        }

        this.performanceChooser.addItemListener(itemListener);      // switch ItemListener back on

        if (selectThis != null)
            this.performanceChooser.setSelectedItem(selectThis);
        else                                                        // if the previously selected item could not be found in the newly assembled list
            this.performanceChooser.setSelectedIndex(0);            // select item 0 by default
    }

    /**
     * add a performance to the performance chooser
     * @param performance
     */
    public void addPerformance(Performance performance) {
        this.performanceChooser.addItem(new PerformanceChooserItem(performance));
    }

    /**
     * this updates an entry in the performance chooser, e.g. when the performance was edited
     * and its name (the key of the performance chooser item) has changed
     * @param performance
     */
    public void updatePerformance(Performance performance) {
        for (int i=0; i < this.performanceChooser.getItemCount(); ++i) {
            PerformanceChooserItem item = (PerformanceChooserItem) this.performanceChooser.getItemAt(i);
            if (item.getValue() == performance) {
                item.setKey(performance.getName());
                return;
            }
        }
    }

    /**
     * remove a performance from the performance chooser
     * @param performance
     */
    public void removePerformance(Performance performance) {
        for (int i=0; i < this.performanceChooser.getItemCount(); ++i) {
            PerformanceChooserItem item = (PerformanceChooserItem) this.performanceChooser.getItemAt(i);
            if (item.getValue() == performance) {
                if (this.performanceChooser.getSelectedItem() == item) {
                    this.performanceChooser.setSelectedIndex(0);
                }
                this.performanceChooser.removeItem(item);
                return;
            }
        }
    }

    /**
     * This fills the MIDI port chooser list.
     * The method should be called to initialize and update the list.
     * See: https://humanwritescode.wordpress.com/2017/11/22/java-midi-basics-how-to-access-a-midi-device-or-midi-port-in-java/
     */
    public void updateMidiPortList() {
        this.midiPortChooser.removeAllItems();

        for (MidiDevice.Info info : MidiSystem.getMidiDeviceInfo()) {   // iterate the info of each device
            // get the corresponding device
            MidiDevice device;
            try {
                device = MidiSystem.getMidiDevice(info);
            } catch (MidiUnavailableException e) {
                continue;
            }

            // the device should be a MIDI port with receiver or a synthesizer (Gervill)
            if (!(device instanceof Sequencer) && (device.getMaxReceivers() != 0))
                this.midiPortChooser.addItem(info);
        }
    }

    /**
     * This fills the audio chooser list.
     * It should be called when the list of audio recordings changes.
     */
    public void updateAudioList() {
        AudioChooserItem selectedItem = (AudioChooserItem) this.audioChooser.getSelectedItem(); // store the previously selected item for reference

        this.audioChooser.removeAllItems();
        this.audioChooser.addItem(new AudioChooserItem("No audio recording"));
        for (mpmToolbox.projectData.Audio audio : this.parent.getAudio()) {
            AudioChooserItem item = new AudioChooserItem(audio);
            this.audioChooser.addItem(item);

            if ((selectedItem != null) && item.toString().equals(selectedItem.toString()))      // if this is the item that was previously selected
                this.audioChooser.setSelectedItem(item);                                        // keep this selection
        }
    }

    /**
     * add the entry to the SyncPlayer's audio chooser
     * @param audio
     */
    public void addAudio(Audio audio) {
        this.audioChooser.addItem(new AudioChooserItem(audio));
    }

    /**
     * remove an entry from the audio chooser
     * @param audio
     */
    public void removeAudio(Audio audio) {
        for (int i=0; i < this.audioChooser.getItemCount(); ++i) {
            AudioChooserItem item = (AudioChooserItem) this.audioChooser.getItemAt(i);
            if (item.getValue() == audio) {
                if (this.audioChooser.getSelectedItem() == item) {
                    this.audioChooser.setSelectedIndex(0);
                }
                this.audioChooser.removeItem(item);
                return;
            }
        }
    }

    /**
     * get the MIDI rendition of the currently selected performance
     * @return
     */
    public Midi getPerformanceRendering() {
        PerformanceChooserItem selectedPerformanceItem = (PerformanceChooserItem) this.performanceChooser.getSelectedItem();

        if (selectedPerformanceItem == this.alignmentPerformance) {
            Audio audio = this.getSelectedAudio();
            if (audio != null) {
                return audio.getAlignment().getExpressiveMsm().exportExpressiveMidi();
            }
        }

        Performance performance = (selectedPerformanceItem == null) ? null : selectedPerformanceItem.getValue();
        return this.parent.getMsm().exportExpressiveMidi(performance, true);
    }

    /**
     * customize the playback slider
     */
    private void makeSlider() {
        this.playbackSlider.setMajorTickSpacing(sliderMax / 4);
        this.playbackSlider.setMinorTickSpacing(sliderMax / 16);
        this.playbackSlider.setPaintTicks(true);
        Tools.makeSliderSetToClickPosition(this.playbackSlider);

        // define interaction
        this.playbackSlider.addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent mouseEvent) {
            }
            @Override
            public void mousePressed(MouseEvent mouseEvent) {
            }
            @Override
            public void mouseReleased(MouseEvent mouseEvent) {
                if (runnable != null) {                             // if music is already playing
                    runnable.jumpTo(((double) playbackSlider.getValue()) / sliderMax);
                }
            }
            @Override
            public void mouseEntered(MouseEvent mouseEvent) {
            }
            @Override
            public void mouseExited(MouseEvent mouseEvent) {
            }
        });

        Tools.addComponentToGridBagLayout(this, (GridBagLayout) this.getLayout(), this.playbackSlider, 5, 0, 1, 2, 100.0, 1.0, 0, 0, GridBagConstraints.BOTH, GridBagConstraints.LINE_START);
    }

    /**
     * start/stop playback
     */
    public synchronized void triggerPlayback() {
        if ((this.runnable != null) && this.runnable.isPlaying()) {     // if music is already playing, we only want to stop it
            this.playButton.setText("\u25B6");                          // set the playButton's symbol to ▶
            this.runnable.stop();                                       // terminate the current runnable/thread, this will also stop the players
            this.runnable = null;
            return;
        }

        this.triggerPlayback(((double) this.playbackSlider.getValue()) / sliderMax);
    }

    /**
     * start/stop playback
     * @param relativePosition the relative start position
     */
    public synchronized void triggerPlayback(double relativePosition) {
        if ((this.runnable != null) && this.runnable.isPlaying()) {     // if playback is already running
            this.runnable.jumpTo(relativePosition);                     // jump to indicated position
        }
        else {
            this.runnable = new PlaybackRunnable(this);
            this.runnable.start(relativePosition);                      // start the new runnable
        }
    }

    /**
     * start/stop playback;
     * works only if an audio file is selected for playback because its sample rate is required
     * to relate the sample position to a relative playtime position
     * @param samplePosition the sample position to start playback
     */
    public synchronized void triggerPlayback(int samplePosition) {
        mpmToolbox.projectData.Audio audio = this.getSelectedAudio();
        if (audio == null)
            return;

        if (this.runnable == null)
            this.runnable = new PlaybackRunnable(this);

        // compute relative position
        double timePosition = ((double) samplePosition / audio.getSampleRate()) * 1000000.0;    // sample position in microseconds
        double relativePosition = timePosition - this.runnable.microsecAudioOffset;
        if (this.runnable.midiIsLonger)                                 // we have to relate the sample position to the MIDI length
            relativePosition /= this.runnable.midi.getMicrosecondLength();
        else                                                            // we relate the sample position to the audio length
            relativePosition /= (getAudioPlayer().getMicrosecondLength() - this.runnable.microsecAudioOffset);

        if (this.runnable.isPlaying())
            this.runnable.jumpTo(relativePosition);
        else
            this.runnable.start(relativePosition);                      // start the new runnable
    }

    /**
     * query which performance is currently selected
     * @return the performance or null
     */
    public synchronized Performance getSelectedPerformance() {
        PerformanceChooserItem selectedItem = (PerformanceChooserItem) this.performanceChooser.getSelectedItem();
        if (selectedItem == null)
            return null;
        return selectedItem.getValue();
    }

    /**
     * check if audio alignment is selected
     * @return
     */
    public synchronized boolean isAudioAlignmentSelected() {
        return this.performanceChooser.getSelectedItem() == this.alignmentPerformance;
    }

    /**
     * query the Audio instance that is currently selected
     * @return the Audio instance or null
     */
    public synchronized mpmToolbox.projectData.Audio getSelectedAudio() {
        if (this.audioChooser.getSelectedItem() == null)
            return null;
        return ((AudioChooserItem) this.audioChooser.getSelectedItem()).getValue();
    }
}
