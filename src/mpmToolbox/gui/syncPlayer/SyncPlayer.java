package mpmToolbox.gui.syncPlayer;

import com.alee.laf.button.WebButton;
import com.alee.laf.checkbox.WebCheckBox;
import com.alee.laf.combobox.WebComboBox;
import com.alee.laf.panel.WebPanel;
import com.alee.laf.slider.WebSlider;
import com.alee.laf.spinner.WebSpinner;
import meico.audio.Audio;
import meico.audio.AudioPlayer;
import meico.midi.Midi;
import meico.midi.MidiPlayer;
import mpmToolbox.gui.ProjectPane;
import mpmToolbox.gui.Settings;
import mpmToolbox.supplementary.Tools;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiUnavailableException;
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
    private final ProjectPane parent;                               // a link to the parent project pane to access its data, midi player etc.

    private WebButton playButton = new WebButton("\u25B6");         //  ◼ "\u25FC", ▶ "\u25B6"
    private static final int sliderMax = 1000000;
    private WebSlider playbackSlider = new WebSlider(WebSlider.HORIZONTAL, 0, sliderMax, 0);  // the slider that indicates playback position

    private WebCheckBox playMidi = new WebCheckBox("Play Performance Rendering");   // TODO: to be deleted
    private WebCheckBox playAudio = new WebCheckBox("Play Audio");                  // TODO: to be deleted

    private WebComboBox performanceChooser = new WebComboBox();     // TODO: should include "None" and "Raw MIDI/No Performance"
    private WebComboBox audioChooser = new WebComboBox();           // TODO: should include "None"

    private WebSpinner skipMillisecondsInAudioPlayback = new WebSpinner();
    private AudioPlayer audioPlayer = new AudioPlayer();
    private MidiPlayer midiPlayer = new MidiPlayer();

    private PlaybackRunnable runnable = new PlaybackRunnable();

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
        this.makeCheckboxes();
        this.makeSlider();
        this.makePlayButton();
    }

    /**
     * add the cehckboxes for choosing the playback source to the GUI
     */
    private void makeCheckboxes() {
        Tools.addComponentToGridBagLayout(this, (GridBagLayout) this.getLayout(), this.playMidi, 0, 0, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH, GridBagConstraints.LINE_START);
        Tools.addComponentToGridBagLayout(this, (GridBagLayout) this.getLayout(), this.playAudio, 0, 1, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH, GridBagConstraints.LINE_START);
    }

    /**
     * customize the playback slider
     */
    private void makeSlider() {
        this.playbackSlider.setMajorTickSpacing(sliderMax / 4);
        this.playbackSlider.setMinorTickSpacing(sliderMax / 16);
        this.playbackSlider.setPaintTicks(true);

        this.playbackSlider.addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent mouseEvent) {
                // TODO: set playback position in midi and audio
            }
            @Override
            public void mousePressed(MouseEvent mouseEvent) {
            }
            @Override
            public void mouseReleased(MouseEvent mouseEvent) {
            }
            @Override
            public void mouseEntered(MouseEvent mouseEvent) {
            }
            @Override
            public void mouseExited(MouseEvent mouseEvent) {
            }
        });

        Tools.addComponentToGridBagLayout(this, (GridBagLayout) this.getLayout(), this.playbackSlider, 2, 0, 1, 2, 100.0, 1.0, 0, 0, GridBagConstraints.BOTH, GridBagConstraints.LINE_START);
    }

    /**
     * helper method to define the play button
     */
    private void makePlayButton() {
//        this.playButton = new WebButton(this.getMidiPlayer().isPlaying() ? "\u25FC" : "\u25B6");    //  ◼ "\u25FC", ▶ "\u25B6"
        this.playButton.setPadding(Settings.paddingInDialogs);
//        this.playButton.setFontSize(Settings.getDefaultFontSize());
        this.playButton.addActionListener(actionEvent -> this.triggerPlayback());   // set the button's action
        Tools.addComponentToGridBagLayout(this, (GridBagLayout) this.getLayout(), this.playButton, 1, 0, 1, 2, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH, GridBagConstraints.LINE_START);
    }

    /**
     * start the playback or stop it if it is already running
     */
    private void triggerPlayback() {
        // if music is already playing, we only want to stop playback
        if (this.getMidiPlayer().isPlaying() || this.getAudioPlayer().isPlaying()) {
            this.playButton.setText("\u25B6");              // set the playButton's symbol to ▶
            this.runnable.terminate();                      // terminate the current runnable/thread, this will also stop the players
            return;
        }

        // we want to start a new playback
        double relativeSliderValue = ((double) this.playbackSlider.getValue()) / sliderMax;
        long playbackPosition = 0;

        Midi midi = null;
        if (this.playMidi.isSelected()) {
            midi = (this.parent.getMpm() == null) ? this.parent.getMsm().exportMidi(100) : this.parent.getMsm().exportExpressiveMidi(this.parent.getMpm().getPerformance(0), true); // render the MIDI performance, TODO: choose the right performance
            playbackPosition = (long) ((double) midi.getMicrosecondLength() * relativeSliderValue);
        }

        if (this.playAudio.isSelected() && !this.parent.getAudio().isEmpty()) {
            Audio audio = this.parent.getAudio().get(0);                                                        // set the audio data to be played back, // TODO: choose the right audio
            if (this.getAudioPlayer().setAudioData(audio)) {                                                    // TODO: make sure we have non-null audio data
                if ((midi == null) || (midi.getMicrosecondLength() < this.getAudioPlayer().getMicrosecondLength())) {// the slider's length must cover the longer of both, MIDI and Audio, so if Audio is longer, update the playback position
                    playbackPosition = (long) ((double) this.getAudioPlayer().getMicrosecondLength() * relativeSliderValue);
                }
                this.getAudioPlayer().setMicrosecondPosition(playbackPosition);
                this.getAudioPlayer().play();
            }
        }

        if (midi != null) {
            double relativeMidiPlaybackPosition = (double) playbackPosition / midi.getMicrosecondLength();
            if (relativeMidiPlaybackPosition < 1.0) {
                try {
                    this.getMidiPlayer().play(midi, relativeMidiPlaybackPosition);   // start MIDI playback at the slider position
                } catch (InvalidMidiDataException e) {
                    e.printStackTrace();
                }
            }
        }

        if (this.getMidiPlayer().isPlaying() || this.getAudioPlayer().isPlaying()) {
            this.playButton.setText("\u25FC");                  // set the playButton's symbol

            this.runnable.terminate();                          // terminate the current runnable/thread (if there was none running, this is superficial but it does not hurt either)
            this.runnable = new PlaybackRunnable();             // create a new runnable
            (new Thread(this.runnable)).start();                // initialize and start a new thread with that new runnable
        }
    }

    /**
     * This class represents the Runnable instance that the thread is running when
     * aligning the playback slider position with the music and switching off the
     * play button at the end of the playback.
     * @author Axel Berndt
     */
    private class PlaybackRunnable implements Runnable {
        private volatile boolean terminate = false;

        /**
         * invoke this method to terminate the runnable and, thus, the thread
         */
        protected synchronized void terminate() {
            this.terminate = true;
        }

        /**
         * If the audio playback reached the end, it does not stop the player automatically.
         * So we have to do it explicitly. That is what this method does.
         * @return true when stopped
         */
        private boolean audioAutoStop() {
            if (getAudioPlayer().getRelativePosition() >= 1.0) {
                getAudioPlayer().stop();
                return true;
            }
            return false;
        }

        /**
         * this defines what the thread actually does while running
         */
        @Override
        public void run() {
            while (!terminate) {  // while the music plays
//                this.playButton.setText("\u25FC");

                // update playbackSlider
                double relativePlaybackPosition;
                if (getAudioPlayer().isPlaying() && getMidiPlayer().isPlaying()) {
                    relativePlaybackPosition = (getAudioPlayer().getMicrosecondLength() > getMidiPlayer().getMicrosecondLength()) ? getAudioPlayer().getRelativePosition() : getMidiPlayer().getRelativePosition();
                    this.audioAutoStop();
                } else if (getAudioPlayer().isPlaying()) {
                    if (this.audioAutoStop())
                        break;
                    relativePlaybackPosition = getAudioPlayer().getRelativePosition();
                } else if (getMidiPlayer().isPlaying())
                    relativePlaybackPosition = getMidiPlayer().getRelativePosition();
                else break;
                playbackSlider.setValue((int) (relativePlaybackPosition * sliderMax));

                try {
                    Thread.sleep(200L);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            // playback stops, this thread must terminate, but before that, do some housekeeping
            SwingUtilities.invokeLater(() -> {      // GUI operations must be done on the Event Dispatch Thread
                getMidiPlayer().stop();
                getAudioPlayer().stop();

                if (!terminate)                     // if we reached the end of the music, i.e. playback was not terminated by interaction
                    playbackSlider.setValue(0);     // set slider to start position ... in any other case just keep the slider position

                playButton.setText("\u25B6");       //  ▶ "\u25B6"
            });
        }
    }
}
