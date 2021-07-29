package mpmToolbox.gui.syncPlayer;

import com.alee.laf.button.WebButton;
import com.alee.laf.combobox.WebComboBox;
import com.alee.laf.label.WebLabel;
import com.alee.laf.panel.WebPanel;
import com.alee.laf.slider.WebSlider;
import com.alee.laf.spinner.WebSpinner;
import meico.audio.Audio;
import meico.audio.AudioPlayer;
import meico.midi.Midi;
import meico.midi.MidiPlayer;
import meico.mpm.elements.Performance;
import mpmToolbox.gui.ProjectPane;
import mpmToolbox.gui.Settings;
import mpmToolbox.supplementary.Tools;

import javax.sound.midi.*;
import javax.sound.sampled.Clip;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

/**
 * This class implements the Audio and MIDI player for MPM Toolbox.
 * It is able to play both media in parallel.
 * @author Axel Berndt
 */
public class SyncPlayer extends WebPanel {
    private final ProjectPane parent;                                   // a link to the parent project pane to access its data, midi player etc.

    private final WebButton playButton = new WebButton("\u25B6");       //  ◼ "\u25FC", ▶ "\u25B6"
    private static final int sliderMax = 1000000;
    private final WebSlider playbackSlider = new WebSlider(WebSlider.HORIZONTAL, 0, sliderMax, 0);  // the slider that indicates playback position

    private final AudioPlayer audioPlayer = new AudioPlayer();
    private final MidiPlayer midiPlayer = new MidiPlayer();

    private final WebComboBox performanceChooser = new WebComboBox();
    private final PerformanceChooserItem rawPerformance = new PerformanceChooserItem(Performance.createPerformance("Play raw notes, no performance"));

    private final WebComboBox midiPortChooser = new WebComboBox();

    private final WebComboBox audioChooser = new WebComboBox();
    private final WebSpinner skipMillisecondsInAudioPlayback = new WebSpinner(new SpinnerNumberModel(0L, 0L, 9999999999L, 1L));

    private PlaybackRunnable runnable = null;

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
        this.updatePerformanceList();
        this.performanceChooser.setPadding(Settings.paddingInDialogs / 4);
        this.performanceChooser.setToolTip("Select the performance rendering to be played.");
        Tools.addComponentToGridBagLayout(this, (GridBagLayout) this.getLayout(), this.performanceChooser, 0, 0, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH, GridBagConstraints.LINE_START);

        WebLabel midiPortLabel = new WebLabel("MIDI Out:");
        midiPortLabel.setToolTip("Select the MIDI port to output performance rendering. Default is \"Gervill\".");
        midiPortLabel.setHorizontalAlignment(WebLabel.RIGHT);
        midiPortLabel.setPadding(Settings.paddingInDialogs / 4);
        Tools.addComponentToGridBagLayout(this, (GridBagLayout) this.getLayout(), midiPortLabel, 1, 0, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH, GridBagConstraints.LINE_START);

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

        this.updateAudioList();
        this.audioChooser.setPadding(Settings.paddingInDialogs / 4);
        this.audioChooser.setToolTip("Select the audio recording to be played.");
        this.audioChooser.addActionListener(actionEvent -> {    // when an audio recording is selected
            if (this.audioChooser.getSelectedItem() != null)
                this.parent.getAudioFrame().setAudio(((AudioChooserItem) this.audioChooser.getSelectedItem()).getValue());  // communicate the selection to the audio analysis frame as this should also display it
        });
        Tools.addComponentToGridBagLayout(this, (GridBagLayout) this.getLayout(), this.audioChooser, 0, 1, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH, GridBagConstraints.LINE_START);

        WebLabel skipLabel = new WebLabel("skip:");
        skipLabel.setToolTip("Skip initial silence in the audio recording.");
        skipLabel.setHorizontalAlignment(WebLabel.RIGHT);
        skipLabel.setPadding(Settings.paddingInDialogs / 4);
        Tools.addComponentToGridBagLayout(this, (GridBagLayout) this.getLayout(), skipLabel, 1, 1, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH, GridBagConstraints.LINE_START);

        Tools.addComponentToGridBagLayout(this, (GridBagLayout) this.getLayout(), this.skipMillisecondsInAudioPlayback, 2, 1, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH, GridBagConstraints.LINE_START);

        WebLabel millisecondsLabel = new WebLabel("ms");
        millisecondsLabel.setToolTip("Milliseconds");
        millisecondsLabel.setHorizontalAlignment(WebLabel.LEFT);
        millisecondsLabel.setPadding(Settings.paddingInDialogs / 4);
        Tools.addComponentToGridBagLayout(this, (GridBagLayout) this.getLayout(), millisecondsLabel, 3, 1, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH, GridBagConstraints.LINE_START);

        this.playButton.setPadding((int) (Settings.paddingInDialogs * 1.5));
        this.playButton.addActionListener(actionEvent -> this.triggerPlayback());
        Tools.addComponentToGridBagLayout(this, (GridBagLayout) this.getLayout(), this.playButton, 4, 0, 1, 2, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH, GridBagConstraints.LINE_START);

        this.makeSlider();
    }

    /**
     * This fills the performance chooser list.
     * The method should be called when the the available performances have changed.
     */
    public void updatePerformanceList() {
        PerformanceChooserItem selectedItem = (PerformanceChooserItem) this.performanceChooser.getSelectedItem();   // store the previously selected item

        this.performanceChooser.removeAllItems();
        this.performanceChooser.addItem(new PerformanceChooserItem("No performance rendering"));
        this.performanceChooser.addItem(this.rawPerformance);
        if (this.parent.getMpm() != null) {
            for (Performance performance : this.parent.getMpm().getAllPerformances()) {
                PerformanceChooserItem item = new PerformanceChooserItem(performance);
                this.performanceChooser.addItem(item);

                if ((selectedItem != null) && item.toString().equals(selectedItem.toString()))  // if this is the item that was previously selected
                    this.performanceChooser.setSelectedItem(item);                              // keep this selection
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
        if ((this.runnable != null) && (this.runnable.isPlaying())) {   // if music is already playing, we only want to stop it
            this.playButton.setText("\u25B6");                          // set the playButton's symbol to ▶
            this.runnable.stop();                                       // terminate the current runnable/thread, this will also stop the players
            this.runnable = null;
            return;
        }

        // we want to start a new playback
        this.runnable = new PlaybackRunnable();
        this.runnable.start(((double) this.playbackSlider.getValue()) / sliderMax); // start the new runnable
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
     * query the Audio instance that is currently selected
     * @return the Audio instance or null
     */
    public synchronized Audio getSelectedAudio() {
        if (audioChooser.getSelectedItem() == null)
            return null;
        return ((AudioChooserItem) audioChooser.getSelectedItem()).getValue();
    }

    /**
     * This class represents the Runnable instance that the thread is running when
     * aligning the playback slider position with the music and switching off the
     * play button at the end of the playback.
     * @author Axel Berndt
     */
    private class PlaybackRunnable implements Runnable {
        private volatile Thread thread = null;
        private volatile boolean terminate = false;

        private double relativeMidiPlaybackPosition = 0.0;
        private long audioPlaybackPosition = 0;
        private long microsecAudioOffset = 0;

        private final Midi midi;
        private final Clip audio;
        private final boolean midiIsLonger;

        /**
         * constructor
         */
        protected PlaybackRunnable() {
            if ((performanceChooser.getSelectedItem() == null) || (((PerformanceChooserItem) performanceChooser.getSelectedItem()).getValue() == null))
                this.midi = null;
            else
                this.midi = parent.getMsm().exportExpressiveMidi(((PerformanceChooserItem) performanceChooser.getSelectedItem()).getValue(), true);

            getAudioPlayer().stop();
            if ((audioChooser.getSelectedItem() != null) && getAudioPlayer().setAudioData(((AudioChooserItem) audioChooser.getSelectedItem()).getValue())) {
                this.audio = getAudioPlayer().getAudioClip();
                this.microsecAudioOffset = (long) ((double) skipMillisecondsInAudioPlayback.getValue()) * 1000;
            }
            else
                this.audio = null;

            this.midiIsLonger = (this.audio == null) || ((this.midi != null) && (this.midi.getMicrosecondLength() > (this.audio.getMicrosecondLength() - this.microsecAudioOffset)));
        }

        /**
         * initialize and start a new thread with this runnable
         * @param relativeSliderPosition
         */
        protected void start(double relativeSliderPosition) {
            if (((this.audio == null) && (this.midi == null)) || (this.thread != null))
                return;

            playButton.setText("\u25FC");                      // set the playButton's symbol to ◼
            this.setPlaybackPositions(relativeSliderPosition);
            this.thread = new Thread(this);
            this.thread.start();
        }

        /**
         * start those players that have data
         */
        private void startPlayers() {
            if ((this.midi != null) && (this.relativeMidiPlaybackPosition < 1.0)) {
                try {
                    getMidiPlayer().play(this.midi, this.relativeMidiPlaybackPosition);   // start MIDI playback at the slider position
                } catch (InvalidMidiDataException e) {
                    e.printStackTrace();
                }
            }

            if ((this.audio != null) && (this.audioPlaybackPosition < getAudioPlayer().getMicrosecondLength())) {
                getAudioPlayer().setMicrosecondPosition(this.audioPlaybackPosition);
                getAudioPlayer().play();
            }
        }

        /**
         * invoke this method to terminate the runnable and, thus, the thread
         */
        protected synchronized void stop() {
            getMidiPlayer().stop();
            getAudioPlayer().stop();
            this.terminate = true;
        }

        /**
         * signal the runnable to jump to another playback position
         * @param relativeSliderPosition
         */
        protected synchronized void jumpTo(double relativeSliderPosition) {
            if (!this.isPlaying())
                return;

            this.setPlaybackPositions(relativeSliderPosition);

            if (this.audio != null) {
                getAudioPlayer().pause();
                getAudioPlayer().setAudioData(this.audio);
                getAudioPlayer().setMicrosecondPosition(this.audioPlaybackPosition);
                getAudioPlayer().play();
            }

            if (this.midi != null) {
                try {
                    getMidiPlayer().play(this.midi, this.relativeMidiPlaybackPosition);
                } catch (InvalidMidiDataException e) {
                    e.printStackTrace();
                }
            }
        }

        /**
         * set the playback positions before starting playback
         * @param relativeSliderPosition
         */
        private void setPlaybackPositions(double relativeSliderPosition) {
            if (this.midiIsLonger) {
                this.relativeMidiPlaybackPosition = relativeSliderPosition;
                if (this.audio != null)
                    this.audioPlaybackPosition = (long) (relativeSliderPosition * this.midi.getMicrosecondLength()) + this.microsecAudioOffset;
            }
            else {    // audio is longer
                long effectiveAudioLength = getAudioPlayer().getMicrosecondLength() - this.microsecAudioOffset;
                this.audioPlaybackPosition = (long) (relativeSliderPosition * effectiveAudioLength) + this.microsecAudioOffset;
                if (this.midi != null)
                    this.relativeMidiPlaybackPosition = ((double) this.audioPlaybackPosition - this.microsecAudioOffset) / this.midi.getMicrosecondLength();
            }
        }

        /**
         * indicates whether the playback ist still running
         * @return
         */
        protected boolean isPlaying() {
            return getAudioPlayer().isPlaying() || getMidiPlayer().isPlaying();
        }

        /**
         * this defines what the thread actually does while running
         */
        @Override
        public void run() {
            this.startPlayers();

            while (!this.terminate) {  // while the music plays
//                this.playButton.setText("\u25FC");

                // update playbackSlider
                double relativePlaybackPosition;
                if (this.midiIsLonger)
                    relativePlaybackPosition = getMidiPlayer().getRelativePosition();
                else
                    relativePlaybackPosition = (double) (getAudioPlayer().getMicrosecondPosition() - this.microsecAudioOffset) / (double) (getAudioPlayer().getMicrosecondLength() - this.microsecAudioOffset);

                if ((playbackSlider.getValue() == sliderMax) || (!getAudioPlayer().isPlaying() && !getMidiPlayer().isPlaying())) {
                    runnable = null;
                    break;
                }

                if (!playbackSlider.getValueIsAdjusting())
                    playbackSlider.setValue((int) (relativePlaybackPosition * sliderMax));

                try {
                    Thread.sleep(200L);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            // playback stops, this thread must terminate, but before that, do some housekeeping
            SwingUtilities.invokeLater(() -> {      // GUI operations must be done on the Event Dispatch Thread
                if (!terminate && !playbackSlider.getValueIsAdjusting())   // if we reached the end of the music, i.e. playback was not terminated by interaction
                    playbackSlider.setValue(0);     // set slider to start position ... in any other case just keep the slider position

                playButton.setText("\u25B6");       //  ▶ "\u25B6"
            });
        }
    }
}
