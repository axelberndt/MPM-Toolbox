package mpmToolbox.gui.audio.utilities;

import mpmToolbox.gui.audio.AudioDocumentData;

/**
 * This class represents all information about mouse and playback cursor positions in the audio analysis component.
 * All domain conversions are done here automatically.
 * @author Axel Berndt
 */
public class CursorPositions {
    private final AudioDocumentData parent; // the container of all audio analysis related panels
    private Long sample = null;             // audio position in samples
    private Double milliseconds = null;     // audio position in milliseconds
    private Double ticks = null;            // tick position in the non-performed music (MSM)
    private Integer audioX = null;          // horizontal pixel position of the cursor in the audio domain panels
    private int audioXSpread = 1;           // When the cursor is in a tick domain panel we have to convert its position to milliseconds. The result can vary over a certain interval due to asynchronies, which is represented by this spread. So we can draw a thicker stroke.
    private Integer ticksX = null;          // horizontal pixel position of the cursor in the tick domain panels
    private int ticksXSpread = 1;           // When the cursor is in a milliseconds domain panel we have to convert its position to ticks. The result can vary over a certain interval due to asynchronies, which is represented by this spread. So we can draw a thicker stroke.

    /**
     * constructor
     * @param parent
     */
    public CursorPositions(AudioDocumentData parent) {
        this.parent = parent;
    }

    /**
     * access the sample position of the cursor
     * @return
     */
    public long getSample() {
        if (this.sample == null) {
            this.sample = 0L;   // just to be sure that it is not null
            if (this.milliseconds != null) {
                this.sample = this.millisecondsToSample(this.milliseconds);
            } else if (this.audioX != null) {
                this.sample = this.audioXToSample(this.audioX);
            } else if (this.ticks != null) {
                this.ticksToAudioValues(this.ticks);
            } else if (this.ticksX != null) {
                this.ticks = this.ticksXToTicks(this.ticksX);
                this.ticksToAudioValues(this.ticks);
            }
        }
        return this.sample;
    }

    /**
     * set the cursor position via sample value
     * @param sample
     */
    public void setSample(long sample) {
        this.allNull();
        this.sample = sample;
//        this.audioXSpread = 1;
//        this.milliseconds = this.sampleToMilliseconds(sample);
//        this.audioX = this.sampleToAudioX(sample);
//        this.millisecondsTocTickValues(this.milliseconds);
    }

    /**
     * access the milliseconds position of the cursor
     * @return
     */
    public double getMilliseconds() {
        if (this.milliseconds == null) {
            this.milliseconds = 0.0;    // just to be sure that it is not null
            if (this.sample != null) {
                this.milliseconds = this.sampleToMilliseconds(this.sample);
            } else if (this.audioX != null) {
                this.sample = this.audioXToSample(this.audioX);
                this.milliseconds = this.sampleToMilliseconds(this.sample);
            } else if (this.ticks != null) {
                this.ticksToAudioValues(this.ticks);
            } else if (this.ticksX != null) {
                this.ticks = this.ticksXToTicks(this.ticksX);
                this.ticksToAudioValues(this.ticks);
            }
        }
        return this.milliseconds;
    }

    /**
     * set the cursor position via millisecond value
     * @param milliseconds
     */
    public void setMilliseconds(double milliseconds) {
        this.allNull();
        this.milliseconds = milliseconds;
//        this.audioXSpread = 1;
//        this.sample = this.millisecondsToSample(milliseconds);
//        this.audioX = this.sampleToAudioX(this.sample);
//        this.millisecondsToTickValues(milliseconds);
    }

    /**
     * access the tick position of the cursor
     * @return
     */
    public double getTicks() {
        if (this.ticks == null) {
            this.ticks = 0.0;   // just to be sure that it is not null
            if (this.ticksX != null) {
                this.ticks = this.ticksXToTicks(this.ticksX);
            } else if (this.milliseconds != null) {
                this.millisecondsToTickValues(this.milliseconds);
            } else if (this.sample != null) {
                this.milliseconds = this.sampleToMilliseconds(this.sample);
                this.millisecondsToTickValues(this.milliseconds);
            } else if (this.audioX != null) {
                this.sample = this.audioXToSample(this.audioX);
                this.milliseconds = this.sampleToMilliseconds(this.sample);
                this.millisecondsToTickValues(this.milliseconds);
            }
        }
        return this.ticks;
    }

    /**
     * set the cursor position via tick value
     * @param ticks
     */
    public void setTicks(double ticks) {
        this.allNull();
        this.ticks = ticks;
//        this.ticksXSpread = 1;
//        this.ticksX = this.ticksToTicksX(ticks);
//        this.ticksToAudioValues(ticks);
    }

    /**
     * access the horizontal pixel position of the cursor in the waveform panel
     * @return
     */
    public int getAudioX() {
        if (this.audioX == null) {
            this.audioX = 0;    // just to be sure that it is not null
            if (this.sample != null) {
                this.audioX = this.sampleToAudioX(this.sample);
            } else if (this.milliseconds != null) {
                this.sample = this.millisecondsToSample(this.milliseconds);
                this.audioX = this.sampleToAudioX(this.sample);
            } else if (this.ticks != null) {
                this.ticksToAudioValues(this.ticks);
            } else if (this.ticksX != null) {
                this.ticks = this.ticksXToTicks(this.ticksX);
                this.ticksToAudioValues(this.ticks);
            }
        }
        return this.audioX;
    }

    /**
     * set the cursor position via its horizontal pixel position in the waveform panel
     * @param audioX
     */
    public void setAudioX(int audioX) {
        this.allNull();
        this.audioX = audioX;
//        this.audioXSpread = 1;
//        this.sample = this.audioXToSample(audioX);
//        this.milliseconds = this.sampleToMilliseconds(this.sample);
//        this.millisecondsToTickValues(this.milliseconds);
    }

    /**
     * access the pixel width of the cursor in the waveform panel
     * @return
     */
    public int getAudioXSpread() {
        this.getAudioX();
        return this.audioXSpread;
    }

    /**
     * access the horizontal pixel position of the cursor in the tempomap panel
     * @return
     */
    public int getTicksX() {
        if (this.ticksX == null) {
            this.ticksX = 0;    // just to be sure that it is not null
            if (this.ticks != null) {
                this.ticksX = this.ticksToTicksX(this.ticks);
            } else if (this.milliseconds != null) {
                this.millisecondsToTickValues(this.milliseconds);
            } else if (this.sample != null) {
                this.milliseconds = this.sampleToMilliseconds(this.sample);
                this.millisecondsToTickValues(this.milliseconds);
            } else if (this.audioX != null) {
                this.sample = this.audioXToSample(this.audioX);
                this.milliseconds = this.sampleToMilliseconds(this.sample);
                this.millisecondsToTickValues(this.milliseconds);
            }
        }
        return this.ticksX;
    }

    /**
     * set the cursor position via its horizontal pixel position in the tempomap panel
     * @param ticksX
     */
    public void setTicksX(int ticksX) {
        this.allNull();
        this.ticksX = ticksX;
//        this.ticksXSpread = 1;
//        this.ticks = this.ticksXToTicks(ticksX);
//        this.ticksToAudioValues(this.ticks);
    }

    /**
     * access the pixel width of the cursor in the tempomap panel
     * @return
     */
    public int getTicksXSpread() {
        this.getTicksX();
        return this.ticksXSpread;
    }

    /**
     * sets all values to their initial state
     */
    public void allNull() {
        this.sample = null;
        this.milliseconds = null;
        this.ticks = null;
        this.audioX = null;
        this.audioXSpread = 1;
        this.ticksX = null;
        this.ticksXSpread = 1;
    }

    /**
     * conversion from sample number to milliseconds
     * @param sample
     * @return
     */
    public double sampleToMilliseconds(long sample) {
        return (this.parent.getAudio() != null) ? (sample * 1000.0) / this.parent.getAudio().getFrameRate() : sample;
    }

    /**
     * conversion from milliseconds to sample number
     * @param milliseconds
     * @return
     */
    public long millisecondsToSample(double milliseconds) {
        return (this.parent.getAudio() != null) ? Math.round((milliseconds * this.parent.getAudio().getFrameRate()) / 1000.0) : (long) milliseconds;
    }

    /**
     * conversion from sample number to horizontal pixel position in the waveform panel
     * @param sample
     * @return
     */
    public int sampleToAudioX(long sample) {
        return (int) Math.round(((double) (sample - this.parent.getLeftmostSample()) / (this.parent.getRightmostSample() - this.parent.getLeftmostSample())) * this.parent.getWaveformPanel().getWidth());
    }

    /**
     * conversion from ticks position to horizontal pixel position in the tempomap panel
     * @param ticks
     * @return
     */
    public int ticksToTicksX(double ticks) {
        return (int) Math.round(((ticks - this.parent.getLeftmostTick()) / (this.parent.getRightmostTick() - this.parent.getLeftmostTick())) * this.parent.getTempoMapPanel().getWidth());
    }

    /**
     * conversion from horizontal pixel position in the waveform panel to sample number
     * @param audioX
     * @return
     */
    public long audioXToSample(int audioX) {
        return (Math.round(((double) audioX / this.parent.getWaveformPanel().getWidth()) * (this.parent.getRightmostSample() - this.parent.getLeftmostSample())) + this.parent.getLeftmostSample());
    }

    /**
     * conversion from horizontal pixel position in the tempomap panel to ticks
     * @param ticksX
     * @return
     */
    public double ticksXToTicks(int ticksX) {
        return (((double) ticksX / this.parent.getTempoMapPanel().getWidth()) * (this.parent.getRightmostTick() - this.parent.getLeftmostTick())) + this.parent.getLeftmostTick();
    }

    /**
     * convert the milliseconds position to all symbolic time related values,
     * i.e. ticks, horizontal pixel position in the tempomap panel, pixel width
     * @param milliseconds
     */
    private void millisecondsToTickValues(double milliseconds) {
        if (this.parent.getAlignment() == null)
            return;

        double[] tickDate = this.parent.getAlignment().getCorrespondingTickDate(milliseconds);
        double[] relativePos = new double[]{(tickDate[0] - this.parent.getLeftmostTick()) / (this.parent.getRightmostTick() - this.parent.getLeftmostTick()), (tickDate[1] - this.parent.getLeftmostTick()) / (this.parent.getRightmostTick() - this.parent.getLeftmostTick())};
        double[] pixelPos = new double[]{this.parent.getTempoMapPanel().getWidth() * relativePos[0], this.parent.getTempoMapPanel().getWidth() * relativePos[1]};

        this.ticks = (tickDate[0] + tickDate[1]) / 2.0;
        this.ticksX = (int) Math.round((pixelPos[0] + pixelPos[1]) / 2.0);      // the vertical pixel position of the stroke
        this.ticksXSpread = (int) Math.round(pixelPos[1] - pixelPos[0]);        // the stroke weight should be set as wide as the temporal spread in the performance
    }

    /**
     * convert the ticks position to all physical time related values,
     * i.e. milliseconds, sample number, horizontal pixel position in the waveform panel, pixel width
     * @param ticks
     */
    private void ticksToAudioValues(double ticks) {
        if (this.parent.getAlignment() == null)
            return;

        double[] sampleDate = this.parent.getAlignment().getCorrespondingMillisecondsDate(ticks);   // first we get milliseconds dates
        this.milliseconds = (sampleDate[0] + sampleDate[1]) / 2.0;

        sampleDate[0] = this.millisecondsToSample(sampleDate[0]);               // convert to sample date
        sampleDate[1] = this.millisecondsToSample(sampleDate[1]);               // convert to sample date
        double[] relativePos = new double[]{(sampleDate[0] - this.parent.getLeftmostSample()) / (this.parent.getRightmostSample() - this.parent.getLeftmostSample()), (sampleDate[1] - this.parent.getLeftmostSample()) / (this.parent.getRightmostSample() - this.parent.getLeftmostSample())};
        double[] pixelPos = new double[]{this.parent.getWaveformPanel().getWidth() * relativePos[0], this.parent.getWaveformPanel().getWidth() * relativePos[1]};

        this.sample = Math.round((sampleDate[0] + sampleDate[1]) / 2.0);
        this.audioX = (int) Math.round((pixelPos[0] + pixelPos[1]) / 2.0);      // the vertical pixel position of the stroke
        this.audioXSpread = (int) Math.round(pixelPos[1] - pixelPos[0]);        // the stroke weight should be set as wide as the temporal spread in the performance
    }
}
