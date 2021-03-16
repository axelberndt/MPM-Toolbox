package mpmToolbox.gui.syncPlayer;

import com.alee.api.annotations.NotNull;
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
import meico.supplementary.KeyValue;
import mpmToolbox.gui.ProjectPane;
import mpmToolbox.gui.Settings;
import mpmToolbox.supplementary.Tools;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiUnavailableException;
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
    private boolean sliderBlocked = false;

    private final AudioPlayer audioPlayer = new AudioPlayer();
    private final MidiPlayer midiPlayer = new MidiPlayer();

    private final WebComboBox performanceChooser = new WebComboBox();
    private final PerformanceChooserItem rawPerformance = new PerformanceChooserItem(Performance.createPerformance("Play raw notes, no performance"));

    private final WebComboBox audioChooser = new WebComboBox();
    private final WebSpinner skipMillisecondsInAudioPlayback = new WebSpinner(new SpinnerNumberModel(0L, 0L, 9999999999L, 1L));

    private PlaybackRunnable runnable = null;

    /**
     * constructor
     */
    public SyncPlayer(ProjectPane parent) throws MidiUnavailableException {
        super(new GridBagLayout());
        this.parent = parent;
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
        Tools.addComponentToGridBagLayout(this, (GridBagLayout) this.getLayout(), this.performanceChooser, 0, 0, 4, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH, GridBagConstraints.LINE_START);

        this.updateAudioList();
        this.audioChooser.setPadding(Settings.paddingInDialogs / 4);
        Tools.addComponentToGridBagLayout(this, (GridBagLayout) this.getLayout(), this.audioChooser, 0, 1, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH, GridBagConstraints.LINE_START);

        WebLabel skipLabel = new WebLabel("skip");
        skipLabel.setToolTip("Skip initial silence in the audio recording.");
        skipLabel.setHorizontalAlignment(WebLabel.CENTER);
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
     * This fills the performance chooser list
     * It method should be called when the the available performances have changed.
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
                sliderBlocked = true;   // don't allow the runnable to change the slider position while the user interacts with it
            }
            @Override
            public void mouseReleased(MouseEvent mouseEvent) {
                if (runnable != null) {                             // if music is already playing
                    runnable.jumpTo(((double) playbackSlider.getValue()) / sliderMax);
                }
                sliderBlocked = false;  // now the runnable is allowed again to change the slider position
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

                if (!sliderBlocked)
                    playbackSlider.setValue((int) (relativePlaybackPosition * sliderMax));

                try {
                    Thread.sleep(200L);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            // playback stops, this thread must terminate, but before that, do some housekeeping
            SwingUtilities.invokeLater(() -> {      // GUI operations must be done on the Event Dispatch Thread
                if (!terminate && !sliderBlocked)   // if we reached the end of the music, i.e. playback was not terminated by interaction
                    playbackSlider.setValue(0);     // set slider to start position ... in any other case just keep the slider position

                playButton.setText("\u25B6");       //  ▶ "\u25B6"
            });
        }
    }

    /**
     * This class represents an item in the performance chooser combobox of the SyncPlayer.
     * @author Axel Berndt
     */
    private class PerformanceChooserItem extends KeyValue<String, Performance> {
        /**
         * This constructor creates a performance chooser item (String, Performance) pair out of a non-null performance.
         * @param performance
         */
        private PerformanceChooserItem(@NotNull Performance performance) {
            super(performance.getName(), performance);
        }

        /**
         * This constructor creates a performance chooser item with the specified name key but null performance.
         * Basically, this is used to communicate to the SyncPlayer not to play a performance rendering.
         * The string is typically something like "No performance rendering".
         * @param string
         */
        private PerformanceChooserItem(String string) {
            super(string, null);
        }

        /**
         * All combobox items require this method. The overwrite here makes sure that the string being returned
         * is the performance's name instead of some Java Object ID.
         * @return
         */
        @Override
        public String toString() {
            return this.getKey();
        }
    }

    /**
     * This class represents an item in the audio chooser combobox of the SyncPlayer.
     * @author Axel Berndt
     */
    private class AudioChooserItem extends KeyValue<String, Audio> {
        /**
         * This constructor creates a audio chooser item (String, Audio) pair out of a non-null audio object.
         * @param audio
         */
        private AudioChooserItem(@NotNull Audio audio) {
            super(audio.getFile().getName(), audio);
        }

        /**
         * This constructor creates a audio chooser item with the specified name key but null audio object.
         * Basically, this is used to communicate to the SyncPlayer not to play audio.
         * The string is typically something like "No audio recording".
         * @param string
         */
        private AudioChooserItem(String string) {
            super(string, null);
        }

        /**
         * All combobox items require this method. The overwrite here makes sure that the string being returned
         * is the audio file's name instead of some Java Object ID.
         * @return
         */
        @Override
        public String toString() {
            return this.getKey();
        }
    }
}
