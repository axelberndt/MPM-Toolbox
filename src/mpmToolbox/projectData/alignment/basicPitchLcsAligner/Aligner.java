package mpmToolbox.projectData.alignment.basicPitchLcsAligner;

import com.google.common.base.Stopwatch;
import com.google.common.collect.*;
import meico.supplementary.KeyValue;
import mpmToolbox.projectData.alignment.Alignment;
import mpmToolbox.projectData.alignment.Note;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * Code for aligning a score contained in an {@link Alignment} object (and updating the alignment accordingly)
 * with a given sequence of notes.
 */
public class Aligner {

    /**
     * The main transcription method.
     *
     * @param alignment  The {@link Alignment} object that is to be updated*
     * @param ns         The list of {@link Transcriber.NoteEventWithTime} objects
     * @param onLabel    A callback function that is called to display status information
     * @param onProgress A callback function that is called to update the progress bar
     * @param onDone     A callback function that is called on transcription completion
     */
    public void processWithProgress(Alignment alignment,
                                    List<Transcriber.NoteEventWithTime> ns,
                                    Function<String, Void> onLabel,
                                    Function<Double, Void> onProgress,
                                    Function<Void, Void> onDone) {

        Stopwatch stopwatch = Stopwatch.createStarted();

        if (onLabel == null) {
            onLabel = (com.google.common.base.Function<String, Void>) input -> null;
        }
        if (onProgress == null) {
            onProgress = (com.google.common.base.Function<Double, Void>) input -> null;
        }
        if (onDone == null) {
            onDone = (com.google.common.base.Function<Void, Void>) input -> null;
        }

        List<Note> scoreNotes = alignment.getNoteSequenceInTicks();
        Stopwatch transcriptionToNotesStopwatch = Stopwatch.createStarted();

        List<Integer> scorePitches = Lists.newArrayList();

        // only allow one unique pitch at a given time
        Set<String> timePitches = Sets.newHashSet();
        List<Note> newScoreNotes = Lists.newArrayList();
        for (Note note : scoreNotes) {
            String kv = note.getMillisecondsDate() + "_ " + (int) note.getPitch();
            if (timePitches.contains(kv)) {
                continue;
            }
            timePitches.add(kv);

            newScoreNotes.add(note);
            scorePitches.add((int) note.getPitch());
        }
        scoreNotes = newScoreNotes;

        Stopwatch alignmentStopwatch = Stopwatch.createStarted();

        List<Integer> perfPitches = Lists.newArrayList();
        for (Transcriber.NoteEventWithTime note : ns) {
            perfPitches.add(note.pitchMidi);
        }

        onLabel.apply("aligning score and performance");

        AlignmentPair a = longestCommonSubsequence(scorePitches, perfPitches, onProgress);

        onLabel.apply("filtering note list");

        Map<Double, Integer> noteTimePitch;
        Multimap<Long, Double> chordNoteTimes;

        perfPitches = Lists.newArrayList();
        for (Transcriber.NoteEventWithTime note : ns) {
            perfPitches.add(note.pitchMidi);
        }

        onLabel.apply("aligning score and performance");

        noteTimePitch = Maps.newTreeMap();
        chordNoteTimes = HashMultimap.create();

        for (int i = 0; i < a.first.size(); i++) {
            int scoreOffset = a.first.get(i);
            int perfOffset = a.second.get(i);

            if (scoreOffset < 0 || perfOffset < 0) continue;

            Note scoreNote = scoreNotes.get(scoreOffset);
            double perfTime = ns.get(perfOffset).startTime;

            int prevScoreTimePitch = noteTimePitch.getOrDefault(scoreNote.getInitialMillisecondsDate(), 0);
            noteTimePitch.put(scoreNote.getInitialMillisecondsDate(), Math.max(prevScoreTimePitch, (int) scoreNote.getPitch()));

            chordNoteTimes.put((long) (scoreNote.getInitialMillisecondsDate()), perfTime);
        }

        TreeMap<Long, Double> chordCenters = new TreeMap<>();
        for (Long noteTimeMs : chordNoteTimes.keys()) {
            List<Double> times = new ArrayList<>(chordNoteTimes.get(noteTimeMs));
            Collections.sort(times);
            double minDist = Double.MAX_VALUE;
            double center = times.get(0);
            for (int i = 0; i < times.size() - 1; i++) {
                double dist = times.get(i + 1) - times.get(i);
                if (dist < minDist) {
                    minDist = dist;
                    center = times.get(i) + dist / 2;
                }
            }
            chordCenters.put(noteTimeMs, center);
        }

        Set<Long> badChordTimes = Sets.newHashSet();
        List<Long> noteTimes = Lists.newArrayList(chordCenters.keySet());
        double avgRatio =
                (chordCenters.lastEntry().getValue() - chordCenters.firstEntry().getValue())
                        / (noteTimes.get(noteTimes.size() - 1) / 1000f - noteTimes.get(0) / 1000f);

        for (int i = 6; i < noteTimes.size() - 1; i++) {
            long chordScoreTime = noteTimes.get(i);
            long prevChordScoreTime = noteTimes.get(i - 6);
            long nextChordScoreTime = noteTimes.get(i + 1);

            double chordPerfTime = chordCenters.get(chordScoreTime);
            double prevChordPerfTime = chordCenters.get(prevChordScoreTime);
            double nextChordPerfTime = chordCenters.get(nextChordScoreTime);

            int radius = 20;
            double localAvgPerfScoreRatio = getLocalAvgPerfScoreRatio(noteTimes, chordCenters, noteTimes, i, radius);

            double r = (chordPerfTime - prevChordPerfTime) / (chordScoreTime / 1000f - prevChordScoreTime / 1000f);
            System.out.println(r + "\t" + localAvgPerfScoreRatio);

            if (r / localAvgPerfScoreRatio > 2 || localAvgPerfScoreRatio / r > 2 ||
                    (chordPerfTime - prevChordPerfTime < 1.0 && !badChordTimes.contains(prevChordScoreTime))) {
                badChordTimes.add(chordScoreTime);
            }
        }

        onLabel.apply("updating performance timings");


        ArrayList<KeyValue<Note, Double>> fixedNotes = new ArrayList<>();
        for (int i = 0; i < a.first.size(); i++) {
            int scoreOffset = a.first.get(i);
            int perfOffset = a.second.get(i);

            if (scoreOffset < 0 || perfOffset < 0) continue;

            Note scoreNote = scoreNotes.get(scoreOffset);
            double perfTime = ns.get(perfOffset).startTime;
            if (chordCenters.containsKey((long) (scoreNote.getInitialMillisecondsDate())) &&
                    !badChordTimes.contains((long) (scoreNote.getInitialMillisecondsDate()))) {
                perfTime = chordCenters.get((long) (scoreNote.getInitialMillisecondsDate()));
            } else {
                continue;
            }

            fixedNotes.add(new KeyValue<>(scoreNote, perfTime * 1000));
        }

        alignment.reset();

        alignment.repositionAll(fixedNotes);

        alignment.recomputePianoRoll();

        System.out.println("all done, fixed notes = " + fixedNotes.size() +
                ", done in " + stopwatch.elapsed(TimeUnit.SECONDS) +
                "s, note extraction done in " + transcriptionToNotesStopwatch.elapsed(TimeUnit.SECONDS) +
                "s, alignment done in " + alignmentStopwatch.elapsed(TimeUnit.SECONDS) + "s");

        onDone.apply(null);
    }

    /**
     * Calculates the local average performance score ratio.
     *
     * @param noteTimes    a list of note times
     * @param chordCenters a tree map of chord centers
     * @param times        a list of times
     * @param i            the index to calculate the ratio at
     * @param radius       the radius to use for the calculation
     * @return the local average performance score ratio
     */
    private double getLocalAvgPerfScoreRatio(List<Long> noteTimes, TreeMap<Long, Double> chordCenters, List<Long> times, int i, int radius) {
        long prevChordScoreTime = times.get(Math.max(0, i - radius));
        long nextChordScoreTime = times.get(Math.min(noteTimes.size() - 1, i + radius));
        double prevChordPerfTime = chordCenters.get(prevChordScoreTime);
        double nextChordPerfTime = chordCenters.get(nextChordScoreTime);

        return (nextChordPerfTime - prevChordPerfTime) / (nextChordScoreTime / 1000f - prevChordScoreTime / 1000f);
    }

    /**
     * Calculates the longest common subsequence of two lists of integers.
     *
     * @param a          the first list of integers
     * @param b          the second list of integers
     * @param onProgress a function to call with the progress of the calculation (0.0 - 1.0)
     * @return an alignment pair containing the longest common subsequence
     */
    private static AlignmentPair longestCommonSubsequence(List<Integer> a, List<Integer> b,
                                                          Function<Double, Void> onProgress) {
        int m = a.size(), n = b.size();
        int[][] dp = new int[m + 1][n + 1];
        for (int i = 1; i <= m; ++i) {
            for (int j = 1; j <= n; ++j) {
                if (a.get(i - 1).equals(b.get(j - 1))) {
                    dp[i][j] = dp[i - 1][j - 1] + 1 + (int) Math.pow(a.get(i - 1), 2);
                } else {
                    dp[i][j] = Math.max(dp[i - 1][j], dp[i][j - 1]);
                }
            }
            if (onProgress != null) onProgress.apply(1. * i / m);
        }
        List<Integer> resultA = new ArrayList<>();
        List<Integer> resultB = new ArrayList<>();
        int i = m, j = n;
        while (i > 0 && j > 0) {
            if (a.get(i - 1).equals(b.get(j - 1))) {
                resultA.add(i - 1);
                resultB.add(j - 1);
                --i;
                --j;
            } else if (dp[i - 1][j] > dp[i][j - 1]) {
                --i;
            } else {
                --j;
            }
        }
        Collections.reverse(resultA);
        Collections.reverse(resultB);
        return new AlignmentPair(resultA, resultB);
    }

    /**
     * Represents a pair of alignments.
     */
    private static class AlignmentPair {
        public List<Integer> first, second;

        public AlignmentPair(List<Integer> first, List<Integer> second) {
            this.first = first;
            this.second = second;
        }
    }
}