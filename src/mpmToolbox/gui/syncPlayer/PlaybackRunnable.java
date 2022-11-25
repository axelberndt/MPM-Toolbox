package mpmToolbox.gui.syncPlayer;

import meico.midi.Midi;
import meico.mpm.elements.Performance;
import mpmToolbox.gui.syncPlayer.utilities.AudioChooserItem;
import mpmToolbox.gui.syncPlayer.utilities.PerformanceChooserItem;
import mpmToolbox.projectData.audio.Audio;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.sampled.Clip;
import javax.swing.*;

/**
 * This class represents the Runnable instance that the thread is running when
 * aligning the playback slider position with the music and switching off the
 * play button at the end of the playback.
 * @author Axel Berndt
 */
public class PlaybackRunnable implements Runnable {
    private final SyncPlayer syncPlayer;
    private volatile Thread thread = null;
    private volatile boolean terminate = false;

    private double relativeMidiPlaybackPosition = 0.0;
    private long audioPlaybackPosition = 0;
    public long microsecAudioOffset = 0;

    public final Midi midi;
    private final Clip audio;
    public final boolean midiIsLonger;

    /**
     * constructor
     */
    public PlaybackRunnable(SyncPlayer syncPlayer) {
        this.syncPlayer = syncPlayer;
        long millisecOffset = (long) ((double) this.syncPlayer.skipMillisecondsInAudioPlayback.getValue());
        long millisecMidiOffset = 0;
        if (millisecOffset > 0)                                                 // positive offset will skip the beginning of the audio
            this.microsecAudioOffset = millisecOffset * 1000;
        else if (millisecOffset < 0)                                            // negative offset will skip the beginning of the MIDI
            millisecMidiOffset = millisecOffset;

        this.syncPlayer.getAudioPlayer().stop();
        Audio selectedAudio = null;
        if (this.syncPlayer.audioChooser.getSelectedItem() != null) {
            selectedAudio = ((AudioChooserItem) this.syncPlayer.audioChooser.getSelectedItem()).getValue();
            if (this.syncPlayer.getAudioPlayer().setAudioData(selectedAudio))
                this.audio = this.syncPlayer.getAudioPlayer().getAudioClip();
            else
                this.audio = null;
        } else
            this.audio = null;

        if (this.syncPlayer.performanceChooser.getSelectedItem() == null)                       // nothing selected
            this.midi = null;
        else {
            Performance selectedPerformance = ((PerformanceChooserItem) this.syncPlayer.performanceChooser.getSelectedItem()).getValue();

            if (selectedPerformance != null)                                    // a performance is selected
                this.midi = this.syncPlayer.parent.getMsm().exportExpressiveMidi(selectedPerformance, true);
            else if ((this.syncPlayer.performanceChooser.getSelectedItem() == this.syncPlayer.alignmentPerformance) && (selectedAudio != null))   // audio alignment is selected
                this.midi = selectedAudio.getAlignment().getExpressiveMsm().exportExpressiveMidi();
            else                                                                // no performance selected
                this.midi = null;

            if (this.midi != null) {
                try {
                    this.syncPlayer.getMidiPlayer().getSequencer().setSequence(this.midi.getSequence());    // load the midi sequence into the midi player
                    this.midi.addOffset(millisecMidiOffset);                    // in expressive MIDI a tick is equal to a millisecond, so we can just add the milliseconds offset to the MIDI tick timing
                } catch (InvalidMidiDataException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        this.midiIsLonger = (this.audio == null) || ((this.midi != null) && (this.midi.getMicrosecondLength() > (this.audio.getMicrosecondLength() - this.microsecAudioOffset)));
    }

    /**
     * initialize and start a new thread with this runnable
     * @param relativeSliderPosition
     */
    public void start(double relativeSliderPosition) {
        if (((this.audio == null) && (this.midi == null)) || (this.thread != null))
            return;

        this.syncPlayer.playButton.setText("\u25FC");                      // set the playButton's symbol to ◼
        this.setPlaybackPositions(relativeSliderPosition);
        this.thread = new Thread(this);
        this.thread.start();
    }

    /**
     * start those players that have data
     */
    private void startPlayers() {
        boolean playMidi = false;
        boolean playAudio = false;

        if ((this.midi != null) && (this.relativeMidiPlaybackPosition < 1.0)) {     // prepare the MIDI player
            long startDate = (long)((double) this.midi.getSequence().getTickLength() * this.relativeMidiPlaybackPosition);
            this.syncPlayer.getMidiPlayer().setTickPosition(startDate);
            playMidi = true;
        }

        if ((this.audio != null) && (this.audioPlaybackPosition < this.syncPlayer.getAudioPlayer().getMicrosecondLength())) {   // prepare the audio player
            this.syncPlayer.getAudioPlayer().setMicrosecondPosition(this.audioPlaybackPosition);
            playAudio = true;
        }

        if (playMidi)
            this.syncPlayer.getMidiPlayer().play();
        if (playAudio)
            this.syncPlayer.getAudioPlayer().play();
    }

    /**
     * invoke this method to terminate the runnable and, thus, the thread
     */
    public synchronized void stop() {
        this.syncPlayer.getMidiPlayer().stop();
        this.syncPlayer.getAudioPlayer().stop();
        this.terminate = true;
    }

    /**
     * signal the runnable to jump to another playback position
     * @param relativeSliderPosition
     */
    public synchronized void jumpTo(double relativeSliderPosition) {
        if (!this.isPlaying())
            return;

        this.setPlaybackPositions(relativeSliderPosition);

        if (this.audio != null) {
            this.syncPlayer.getAudioPlayer().pause();
            this.syncPlayer.getAudioPlayer().setAudioData(this.audio);
            this.syncPlayer.getAudioPlayer().setMicrosecondPosition(this.audioPlaybackPosition);
            this.syncPlayer.getAudioPlayer().play();
        }

        if (this.midi != null) {
            long startDate = (long)((double) this.midi.getSequence().getTickLength() * this.relativeMidiPlaybackPosition);
            this.syncPlayer.getMidiPlayer().setTickPosition(startDate);
            this.syncPlayer.getMidiPlayer().play();
        }

        if (this.audio != null)
            this.syncPlayer.getAudioPlayer().play();
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
            long effectiveAudioLength = this.syncPlayer.getAudioPlayer().getMicrosecondLength() - this.microsecAudioOffset;
            this.audioPlaybackPosition = (long) (relativeSliderPosition * effectiveAudioLength) + this.microsecAudioOffset;
            if (this.midi != null)
                this.relativeMidiPlaybackPosition = ((double) this.audioPlaybackPosition - this.microsecAudioOffset) / this.midi.getMicrosecondLength();
        }
    }

    /**
     * indicates whether the playback ist still running
     * @return
     */
    public boolean isPlaying() {
        return this.syncPlayer.getAudioPlayer().isPlaying() || this.syncPlayer.getMidiPlayer().isPlaying();
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
            if (this.midiIsLonger) {
                relativePlaybackPosition = this.syncPlayer.getMidiPlayer().getRelativePosition();
            } else {
                relativePlaybackPosition = (this.syncPlayer.getAudioPlayer().getMicrosecondLength() <= this.microsecAudioOffset) ? 1.0 : (double) (this.syncPlayer.getAudioPlayer().getMicrosecondPosition() - this.microsecAudioOffset) / (double) (this.syncPlayer.getAudioPlayer().getMicrosecondLength() - this.microsecAudioOffset);
            }

            if ((this.syncPlayer.playbackSlider.getValue() == SyncPlayer.PLAYBACK_SLIDER_MAX) || (!this.syncPlayer.getAudioPlayer().isPlaying() && !this.syncPlayer.getMidiPlayer().isPlaying())) {
                this.syncPlayer.runnable = null;
                break;
            }

            if (!this.syncPlayer.playbackSlider.getValueIsAdjusting())
                this.syncPlayer.playbackSlider.setValue((int) (relativePlaybackPosition * SyncPlayer.PLAYBACK_SLIDER_MAX));

            try {
                Thread.sleep(200L);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        // playback stops, this thread must terminate, but before that, do some housekeeping
        SwingUtilities.invokeLater(() -> {                      // GUI operations must be done on the Event Dispatch Thread
            if (!this.terminate && !this.syncPlayer.playbackSlider.getValueIsAdjusting())   // if we reached the end of the music, i.e. playback was not terminated by interaction
                this.syncPlayer.playbackSlider.setValue(0);     // set slider to start position ... in any other case just keep the slider position

            this.syncPlayer.playButton.setText("\u25B6");       //  ▶ "\u25B6"
        });
    }
}
