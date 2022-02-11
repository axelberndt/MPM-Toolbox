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
import mpmToolbox.projectData.audio.Audio;
import mpmToolbox.supplementary.Tools;

import javax.sound.midi.*;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
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

    protected static final int PLAYBACK_SLIDER_MAX = 1000000000;
    protected final WebSlider playbackSlider = new WebSlider(WebSlider.HORIZONTAL, 0, PLAYBACK_SLIDER_MAX, 0);  // the slider that indicates playback position

    protected static final int MIDI_MASTER_VOLUME_MAX = 16383;
    private final WebSlider midiMasterVolume = new WebSlider(WebSlider.VERTICAL, 0, MIDI_MASTER_VOLUME_MAX, MIDI_MASTER_VOLUME_MAX);

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
     * getter for the performance chooser
     * @return
     */
    public WebComboBox getPerformanceChooser() {
        return this.performanceChooser;
    }

    /**
     * getter for the audio chooser
     * @return
     */
    public WebComboBox getAudioChooser() {
        return this.audioChooser;
    }

    /**
     * getter for the offset spinner
     * @return
     */
    public WebSpinner getOffsetSpinner() {
        return this.skipMillisecondsInAudioPlayback;
    }

    /**
     * getter for the playback slider
     * @return
     */
    public WebSlider getPlaybackSlider() {
        return this.playbackSlider;
    }

    /**
     * the relative position of the playback slider
     * @return value in [0.0, 1.0]
     */
    public double getRelativePlaybackSliderPosition() {
        return ((double) playbackSlider.getValue()) / PLAYBACK_SLIDER_MAX;
    }

    /**
     * get the currently set milliseconds offset between MIDI and audio
     * @return
     */
    public double getMillisecondsOffset() {
        return (double) this.skipMillisecondsInAudioPlayback.getValue();
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
        Tools.addComponentToGridBagLayout(this, (GridBagLayout) this.getLayout(), this.performanceChooser, 0, 0, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH, GridBagConstraints.LINE_START);

        // make the MIDI port label
        WebLabel midiPortLabel = new WebLabel("MIDI Out:");
        midiPortLabel.setToolTip("Select the MIDI port to output performance rendering. Default is \"Gervill\".");
        midiPortLabel.setHorizontalAlignment(WebLabel.RIGHT);
        midiPortLabel.setPadding(Settings.paddingInDialogs / 4);
        Tools.addComponentToGridBagLayout(this, (GridBagLayout) this.getLayout(), midiPortLabel, 1, 0, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH, GridBagConstraints.LINE_START);

        // make the MIDI port chooser
        this.updateMidiPortList();
        this.sendMidiMasterVolume(this.midiMasterVolume.getValue());                        // ensure that the device plays with the correct volume
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
                this.sendMidiMasterVolume(this.midiMasterVolume.getValue());                // ensure that the device plays with the correct master volume (if supported)
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
                // any updates in the audio frame (AudioDocumentData etc.) are done by a separate listener that is defined there
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
        Tools.addComponentToGridBagLayout(this, (GridBagLayout) this.getLayout(), this.playButton, 5, 0, 1, 2, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH, GridBagConstraints.LINE_START);

        // make the sliders
        this.makeMidiMasterVolumeSlider();
        this.makePlaybackSlider();
    }

    /**
     * This fills the performance chooser list.
     * The method should be called when the available performances have changed.
     */
    public void updatePerformanceList() {
        PerformanceChooserItem selectedItem = (PerformanceChooserItem) this.performanceChooser.getSelectedItem();   // store the previously selected item
        PerformanceChooserItem selectThis = null;

        // temporarily switch the ItemListeners off; otherwise it would always fire when an item is added
        ItemListener[] itemListeners = this.performanceChooser.getItemListeners();
        for (ItemListener il : itemListeners)
            this.performanceChooser.removeItemListener(il);

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

        for (ItemListener il : itemListeners)
            this.performanceChooser.addItemListener(il);            // switch ItemListener back on

        if (selectThis != null)
            this.performanceChooser.setSelectedItem(selectThis);
        else                                                        // if the previously selected item could not be found in the newly assembled list
            this.performanceChooser.setSelectedIndex(0);            // select item 0 by default
    }

    /**
     * add a performance to the performance chooser
     * @param performance
     * @param select set true to select the newly added item
     */
    public void addPerformance(Performance performance, boolean select) {
        PerformanceChooserItem item = new PerformanceChooserItem(performance);
        this.performanceChooser.addItem(item);
        if (select)
            this.performanceChooser.setSelectedItem(item);
    }

    /**
     * select a performance programmatically
     * @param performance
     */
    public void selectPerformance(Performance performance) {
        for (int i = 0; i < this.performanceChooser.getItemCount(); ++i) {
            PerformanceChooserItem item = (PerformanceChooserItem) this.performanceChooser.getItemAt(i);
            if (item.getValue() == performance) {
                this.performanceChooser.setSelectedItem(item);
            }
        }
    }

    /**
     * select the alignment to the currently chosen audio object; if no audio is selected, this has no effect
     */
    public void selectAlignmentPerformance() {
        this.performanceChooser.setSelectedItem(this.alignmentPerformance);
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
                this.performanceChooser.repaint();
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
        for (Audio audio : this.parent.getAudio()) {
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
     * reads the current value of the MIDI Master Volume slider and sends it to the currently chosen MIDI player's synthesizer
     * @param volume value in [0, 16383]
     */
    private void sendMidiMasterVolume(int volume) {
        // create a SysEx Master Volume message, see http://midi.teragonaudio.com/tech/midispec/mastrvol.htm
        byte[] data = new byte[] {
                0x7F, 0x7F, 0x04, 0x01,
                (byte) (volume & 0x7f),                 // Bits 0 to 6 of a 14-bit volume
                (byte) (volume >> 7)};                  // Bits 7 to 13 of a 14-bit volume
        try {
            SysexMessage message = new SysexMessage(0xF0, data, data.length);
            this.getMidiPlayer().getSynthesizer().getReceiver().send(message, -1);  // send the message now
        } catch (InvalidMidiDataException | MidiUnavailableException e) {
            e.printStackTrace();
        }
    }

    /**
     * sets the volume of the current audio clip
     * @param level value in [0.0, 1.0]
     */
    private void setAudioVolume(float level) {
        Clip clip = this.getAudioPlayer().getAudioClip();
        if (clip == null)
            return;

        FloatControl volume = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
        if (volume == null)
            return;

        volume.setValue((level * (volume.getMaximum() - volume.getMinimum()) + volume.getMinimum()));
    }

    /**
     * create a MIDI Master Volume slider that sends SysEx messages when changed
     */
    private void makeMidiMasterVolumeSlider() {
        this.midiMasterVolume.setPaintTicks(false);
        this.midiMasterVolume.setPaintLabels(false);
        this.midiMasterVolume.setMinimumHeight(1);
        this.midiMasterVolume.setPreferredHeight(1);
        this.midiMasterVolume.setMaximumHeight(1);
        this.midiMasterVolume.setPadding(4);
        this.midiMasterVolume.setToolTip("<html><center>MIDI Master Volume<br>Not supported by some MIDI devices.</center></html>");
        this.midiMasterVolume.addChangeListener(changeEvent -> {
            int midiVolume = this.midiMasterVolume.getValue();
            this.sendMidiMasterVolume(midiVolume);
//            float audioVolume = (float) (MIDI_MASTER_VOLUME_MAX - midiVolume) / MIDI_MASTER_VOLUME_MAX;   // TODO: this might be used to fade between MIDI and audio
//            this.setAudioVolume(audioVolume);
        });
        Tools.addComponentToGridBagLayout(this, (GridBagLayout) this.getLayout(), this.midiMasterVolume, 3, 0, 1, 1, 0.1, 0.1, 0, 0, GridBagConstraints.BOTH, GridBagConstraints.LINE_START);
    }

    /**
     * customize the playback slider
     */
    private void makePlaybackSlider() {
        this.playbackSlider.setMajorTickSpacing(PLAYBACK_SLIDER_MAX / 4);
        this.playbackSlider.setMinorTickSpacing(PLAYBACK_SLIDER_MAX / 16);
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
                    runnable.jumpTo(((double) playbackSlider.getValue()) / PLAYBACK_SLIDER_MAX);
                }
            }
            @Override
            public void mouseEntered(MouseEvent mouseEvent) {
            }
            @Override
            public void mouseExited(MouseEvent mouseEvent) {
            }
        });

        Tools.addComponentToGridBagLayout(this, (GridBagLayout) this.getLayout(), this.playbackSlider, 6, 0, 1, 2, 100.0, 1.0, 0, 0, GridBagConstraints.BOTH, GridBagConstraints.LINE_START);
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

        this.triggerPlayback(((double) this.playbackSlider.getValue()) / PLAYBACK_SLIDER_MAX);
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
        Audio audio = this.getSelectedAudio();
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
    public synchronized Audio getSelectedAudio() {
        if (this.audioChooser.getSelectedItem() == null)
            return null;

        return ((AudioChooserItem) this.audioChooser.getSelectedItem()).getValue();
    }
}
