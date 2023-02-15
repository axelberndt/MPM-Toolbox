package mpmToolbox.projectData.alignment.basicPitchLcsAligner;

import com.alee.api.annotations.NotNull;
import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import mpmToolbox.projectData.alignment.Alignment;
import mpmToolbox.projectData.alignment.Note;

import javax.swing.*;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

import static mpmToolbox.projectData.alignment.basicPitchLcsAligner.Transcriber.exportToMidi;

/**
 * A usual instance of Thread blocks the GUI. So we have to execute code that takes a considerable amount of time in a SwingWorker,
 * see https://www.codementor.io/@isaib.cicourel/swingworker-in-java-du1084lyl for a tutorial.
 * @author Vladimir Viro
 */
class AlignmentComputationWorker extends SwingWorker<Void, Void> {
    private final AlignmentComputation parent;
    double[] audio;
    int sampleRate;
    String audioId;
    Alignment alignment;
    int minNoteLen;
    double onsetThresh;
    double frameThresh;
    int pitchShift;
    double smoothingWidth;
    double tolerance;
    boolean reuseModelOutput;

    boolean exportMidi;

    Transcriber transcriber;

    Aligner aligner;

    /**
     * constructor
     */
    public AlignmentComputationWorker(double[] audio, int sampleRate, String audioId, Alignment alignment,
                                      int minNoteLen, double onsetThresh, double frameThresh,
                                      int pitchShift, double smoothingWidth, double tolerance,
                                      boolean reuseModelOutput, @NotNull AlignmentComputation parent, Transcriber transcriber) {
        super();
        this.audio = audio;
        this.sampleRate = sampleRate;
        this.audioId = audioId;
        this.alignment = alignment;
        this.minNoteLen = minNoteLen;
        this.onsetThresh = onsetThresh;
        this.frameThresh = frameThresh;
        this.pitchShift = pitchShift;
        this.smoothingWidth = smoothingWidth;
        this.tolerance = tolerance;
        this.reuseModelOutput = reuseModelOutput;
        this.parent = parent;
        this.transcriber = transcriber;
        this.aligner = new Aligner(tolerance, pitchShift, smoothingWidth);
        this.exportMidi = false;
    }

    /**
     * this is where the work is done
     * @return the result is transmitted to method done()
     */
    @Override
    protected Void doInBackground() {

        parent.setMaximum(100);

        Function<Double, Void> onProgress = progress -> {
            parent.setProgress((int)(100 * progress));
            return null;
        };

        Function<String, Void> onLabel = input -> {
            parent.setText(input);
            return null;
        };

        Function<Void, Void> onDone = input -> null;


        List<Note> scoreNotes = alignment.getNoteSequenceInTicks();
        List<Integer> scorePitches = Lists.newArrayList();
        Set<String> timePitches = Sets.newHashSet();
        for (mpmToolbox.projectData.alignment.Note note : scoreNotes) {
            String kv = note.getMillisecondsDate() + "_ " + (int) note.getPitch();
            if (timePitches.contains(kv)) {
                continue;
            }
            timePitches.add(kv);
            scorePitches.add((int) note.getPitch());
        }
        int minPitch = scorePitches.stream().min(Comparator.naturalOrder()).orElse(21);
        int maxPitch = scorePitches.stream().max(Comparator.naturalOrder()).orElse(109);

        List<Transcriber.NoteEventWithTime> ns = transcriber.processWithProgress(audio, sampleRate, audioId,
                minNoteLen, onsetThresh, frameThresh, minPitch, maxPitch,
                true,
                onLabel, onProgress, onDone).noteEventWithTimes;

        if (exportMidi) {
            onLabel.apply("exporting MIDI file");

            String midiOutputPath = Paths.get(audioId).toFile().getName()
                    .replace(".mp3", ".end")
                    .replace(".wav", ".end")
                    .replace(".end", "_trans.mid");

            exportToMidi(ns, midiOutputPath);
        }

        aligner.processWithProgress(alignment, ns, onLabel, onProgress, onDone);

        return null;
    }


    /**
     * after the work is done, this "finish sequence" is executed
     */
    @Override
    protected void done() {
        try {
            this.get();
        } catch (InterruptedException | ExecutionException | CancellationException e) {
            e.printStackTrace();
        }
        this.parent.dispose();
    }

    /**
     * this will cancel the process
     */
    protected void cancel() {
        this.cancel(true);          // stop the worker
    }
}
