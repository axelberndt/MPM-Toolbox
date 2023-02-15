package mpmToolbox.projectData.alignment.basicPitchLcsAligner;

import com.google.common.base.Stopwatch;
import com.google.common.collect.*;
import com.google.common.primitives.Doubles;
import meico.supplementary.KeyValue;
import mpmToolbox.projectData.alignment.Alignment;
import mpmToolbox.projectData.alignment.Note;
import mpmToolbox.projectData.alignment.Part;
import org.apache.commons.math3.analysis.interpolation.LinearInterpolator;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;


/**
 * Code for aligning a score contained in an {@link Alignment} object (and updating the alignment accordingly)
 * with a given sequence of notes.
 * @author Vladimir Viro
 */
class Aligner {

    /**
     * tolerance used in the Douglas-Peucker alignment simplification algorithm
     */
    private double simplificationToleranceSeconds;

    /**
     * shift the transcription pitches by this amount in order to match a differently pitched score
     */
    private int pitchShift = 0;

    private double smoothingWidth = 0;

    /**
     * Aligner constructor
     * @param simplificationToleranceSeconds tolerance used in the Douglas-Peucker alignment simplification algorithm
     */
    public Aligner(double simplificationToleranceSeconds, int pitchShift, double smoothingWidth) {
        this.simplificationToleranceSeconds = simplificationToleranceSeconds;
        this.pitchShift = pitchShift;
        this.smoothingWidth = smoothingWidth;
    }

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
        SortedMultiset<Note> scoreNotesSorted = TreeMultiset.create((o1, o2) -> ComparisonChain.start().compare(o1.getMillisecondsDate(), o2.getMillisecondsDate()).compare(o1.getPitch(), o2.getPitch()).result());
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

        scoreNotesSorted.addAll(scoreNotes);

        Stopwatch alignmentStopwatch = Stopwatch.createStarted();


        // get fixed points

        /* times are in milliseconds */
        List<KeyValue<Note, Double>> fixedNotes = Lists.newArrayList();

        SortedMultiset<Note> fixedNotesSorted = TreeMultiset.create((o1, o2) -> ComparisonChain.start().compare(o1.getMillisecondsDate(), o2.getMillisecondsDate()).compare(o1.getPitch(), o2.getPitch()).result());
        TreeMap<Long, Double> scoreToPerfTimesInSec = new TreeMap<>();
        List<Note> allFixedNotes = Lists.newArrayList();
        SortedSet<Long> fixedScoreTimes = Sets.newTreeSet();
        for (Part p : alignment.getParts()) {
            List<Note> fNotes = p.getAllFixedNotes(false);
            allFixedNotes.addAll(fNotes);
        }
        for (Note note : allFixedNotes) {
            scoreToPerfTimesInSec.put((long)note.getInitialMillisecondsDate(), note.getMillisecondsDate() / 1000);
            if (!fixedScoreTimes.contains((long)note.getInitialMillisecondsDate())) {
                fixedNotesSorted.add(note);
                fixedNotes.add(new KeyValue<>(note, note.getMillisecondsDate()));
            }
            fixedScoreTimes.add((long)note.getInitialMillisecondsDate());
        }
        System.out.println("Got " + scoreToPerfTimesInSec.size() + " fixed notes");


        if (!fixedNotesSorted.isEmpty() && !scoreNotesSorted.isEmpty()) {
            // get intervals between fixed points
            Note firstNote = scoreNotesSorted.firstEntry().getElement();
            for (Note lastNote : fixedNotesSorted) {
                // System.out.println("aligning interval from " + firstNote + " to " + lastNote);
                SortedMultiset<Note> segmentNotes = scoreNotesSorted.subMultiset(firstNote, BoundType.CLOSED, lastNote, BoundType.CLOSED);
                List<Note> segmentScoreNotes = Lists.newArrayList(segmentNotes);
                Note finalFirstNote = firstNote;
                List<Transcriber.NoteEventWithTime> segmentPerfNotes = ns.stream().filter(
                        note -> note.startTime >= finalFirstNote.getMillisecondsDate() / 1000 &&
                                note.startTime <= lastNote.getMillisecondsDateEnd() / 1000
                ).sorted().collect(Collectors.toList());

                if (segmentScoreNotes.size() > 1 && segmentScoreNotes.get(0).getMillisecondsDate() !=
                        segmentScoreNotes.get(segmentScoreNotes.size() - 1).getMillisecondsDate() && segmentPerfNotes.size() > 1) {
                    fixedNotes.addAll(computeSegmentAlignment(segmentScoreNotes, segmentPerfNotes, firstNote, lastNote, onLabel, onProgress));
                }

                firstNote = lastNote;
            }
            Note lastNote = scoreNotesSorted.lastEntry().getElement();
            List<Note> segmentScoreNotes = Lists.newArrayList(scoreNotesSorted.subMultiset(firstNote, BoundType.CLOSED, lastNote, BoundType.CLOSED));
            Note finalFirstNote1 = firstNote;
            List<Transcriber.NoteEventWithTime> segmentPerfNotes = ns.stream().filter(
                    note -> note.startTime >= finalFirstNote1.getMillisecondsDate() / 1000 &&
                            note.startTime <= lastNote.getMillisecondsDateEnd() / 1000
            ).collect(Collectors.toList());

            if (segmentScoreNotes.size() > 1 && segmentScoreNotes.get(0).getMillisecondsDate() !=
                    segmentScoreNotes.get(segmentScoreNotes.size() - 1).getMillisecondsDate() && segmentPerfNotes.size() > 1) {
                // System.out.println("aligning last interval from " + firstNote + " to " + lastNote);
                fixedNotes.addAll(computeSegmentAlignment(segmentScoreNotes, segmentPerfNotes, firstNote, lastNote, onLabel, onProgress));
            }

        } else {
            fixedNotes = computeSegmentAlignment(scoreNotes, ns, null, null, onLabel, onProgress);
        }

        alignment.reset();

        alignment.repositionAll(Lists.newArrayList(fixedNotes));

        alignment.recomputePianoRoll();

        System.out.println("all done, fixed notes = " + fixedNotes.size() +
                ", done in " + stopwatch.elapsed(TimeUnit.SECONDS) +
                "s, note extraction done in " + transcriptionToNotesStopwatch.elapsed(TimeUnit.SECONDS) +
                "s, alignment done in " + alignmentStopwatch.elapsed(TimeUnit.SECONDS) + "s");

        onDone.apply(null);
    }

    /**
     * Compute an alignment for a given section of a score and performance
     * @param scoreNotes Score notes
     * @param perfNotes Performance notes obtained using transcription
     * @param firstNote First fixed note
     * @param lastNote Last fixed note
     * @param onLabel
     * @param onProgress
     * @return List of pairs of notes and times they are aligned with in performance.
     */
    private List<KeyValue<Note, Double>> computeSegmentAlignment(List<Note> scoreNotes,
                                                                 List<Transcriber.NoteEventWithTime> perfNotes,
                                                                 Note firstNote,
                                                                 Note lastNote,
                                                                 Function<String, Void> onLabel,
                                                                 Function<Double, Void> onProgress) {

        boolean useSmoothing = this.smoothingWidth > 0;

        List<Transcriber.NoteEventWithTime> scorePerfNotes = scoreNotes.stream().map((com.google.common.base.Function<Note, Transcriber.NoteEventWithTime>) note ->
                new Transcriber.NoteEventWithTime(note.getMillisecondsDate() / 1000, note.getInitialMillisecondsDateEnd() / 1000, (int)note.getPitch(), 0.8)).collect(Collectors.toList());

        System.out.println("aligning segment with " + scoreNotes.size() + " score notes and " + perfNotes.size() + " perf notes");

        List<Integer> perfPitches = Lists.newArrayList();
        for (Transcriber.NoteEventWithTime note : perfNotes) {
            perfPitches.add(note.pitchMidi + this.pitchShift);
        }

        List<Integer> scorePitches = Lists.newArrayList();

        Set<String> timePitches = Sets.newHashSet();
        for (Note note : scoreNotes) {
            String kv = note.getMillisecondsDate() + "_ " + (int) note.getPitch();
            if (timePitches.contains(kv)) {
                continue;
            }
            timePitches.add(kv);
            scorePitches.add((int) note.getPitch());
        }

        onLabel.apply("aligning score and performance");

        AlignmentPair a = longestCommonSubsequence(scorePitches, perfPitches, onProgress);
        System.out.println("computed LCS alignment with " + a.first.size() + " pairs");

        onLabel.apply("filtering note list");

        Map<Double, Integer> noteTimePitch;
        Multimap<Long, Double> chordNoteTimes;

        perfPitches = Lists.newArrayList();
        for (Transcriber.NoteEventWithTime note : perfNotes) {
            perfPitches.add(note.pitchMidi + this.pitchShift);
        }

        onLabel.apply("aligning score and performance");

        noteTimePitch = Maps.newTreeMap();
        chordNoteTimes = HashMultimap.create();

        for (int i = 0; i < a.first.size(); i++) {
            int scoreOffset = a.first.get(i);
            int perfOffset = a.second.get(i);

            if (scoreOffset < 0 || perfOffset < 0) continue;

            Note scoreNote = scoreNotes.get(scoreOffset);
            double perfTime = perfNotes.get(perfOffset).startTime;

            int prevScoreTimePitch = noteTimePitch.getOrDefault(scoreNote.getInitialMillisecondsDate(), 0);
            noteTimePitch.put(scoreNote.getInitialMillisecondsDate(), Math.max(prevScoreTimePitch, (int) scoreNote.getPitch()));

            chordNoteTimes.put((long) (scoreNote.getInitialMillisecondsDate()), perfTime);
        }

        // compute chordCenters

        TreeMap<Long, Double> chordCenters = new TreeMap<>();
        for (Long scoreNoteTimeMs : chordNoteTimes.keys()) {
            List<Double> times = new ArrayList<>(chordNoteTimes.get(scoreNoteTimeMs));
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
            chordCenters.put(scoreNoteTimeMs, center);
        }

        // smoothen out
        if (useSmoothing) {
            LinearInterpolator li = new LinearInterpolator();
            double[] x = Doubles.toArray(chordCenters.keySet());
            double[] y = Doubles.toArray(chordCenters.values());
            PolynomialSplineFunction spline = li.interpolate(x, y);

            double segmentDuration = (scoreNotes.get(scoreNotes.size() - 1).getInitialMillisecondsDateEnd() - scoreNotes.get(0).getInitialMillisecondsDate()) / 1000;

            double stepHz = 4;
            int nSteps = (int)(stepHz * segmentDuration);

            double[] grid = DoubleStream.iterate(x[0], i -> i + 1.0 / nSteps * (x[x.length - 1] - x[0])).limit(nSteps + 1).toArray();
            grid[grid.length - 1] = grid[grid.length - 1] - 0.000001;

            double[] interpolatedOnGrid = DoubleStream.of(grid).map(spline::value).toArray();

            int kernSize = (int)Math.ceil(2 * this.smoothingWidth * stepHz);

            double[] smoothedChordCentersOnGrid = movingAverage(interpolatedOnGrid, kernSize);

            PolynomialSplineFunction splineSmoothed = li.interpolate(grid, smoothedChordCentersOnGrid);

            List<Double> smoothedChordCenters = Doubles.asList(x).subList(0, x.length - 1).stream().map(splineSmoothed::value).collect(Collectors.toList());

            int i = 0;
            for (Map.Entry<Long, Double> entry : chordCenters.entrySet()) {
                if (i < x.length - 1) {
                    entry.setValue(smoothedChordCenters.get(i));
                } else {
                    entry.setValue(smoothedChordCenters.get(i - 1));
                }
                i++;
            }
        }


        // exclude suspicious chords with timings inconsistent with the score

        double maxLocalScoreDeviationFactor = 2;
        double minTimeBetweenMarkedChords = 0.5;

        Set<Long> badChordTimes = Sets.newHashSet();
        List<Long> noteTimes = Lists.newArrayList(chordCenters.keySet());


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
            // System.out.println(r + "\t" + localAvgPerfScoreRatio);

            if (r / localAvgPerfScoreRatio > maxLocalScoreDeviationFactor || localAvgPerfScoreRatio / r > maxLocalScoreDeviationFactor ||
                    (chordPerfTime - prevChordPerfTime < minTimeBetweenMarkedChords && !badChordTimes.contains(prevChordScoreTime))) {
                badChordTimes.add(chordScoreTime);
            }
        }

        onLabel.apply("updating performance timings");

        ArrayList<KeyValue<Note, Double>> fixedNotes = new ArrayList<>();
        if (firstNote != null) {
            fixedNotes.add(new KeyValue<>(firstNote, firstNote.getMillisecondsDate()));
        }
        double markedPerfTime = 0; // used to only fix one note in a chord, as more would be unnecessary and more difficult to update currently.
        for (int i = 0; i < a.first.size(); i++) {
            int scoreOffset = a.first.get(i);
            int perfOffset = a.second.get(i);

            if (scoreOffset < 0 || perfOffset < 0) continue;

            Note scoreNote = scoreNotes.get(scoreOffset);
            double perfTime = perfNotes.get(perfOffset).startTime;
            if (perfTime != markedPerfTime &&
                    chordCenters.containsKey((long) (scoreNote.getInitialMillisecondsDate())) &&
                    !badChordTimes.contains((long) (scoreNote.getInitialMillisecondsDate()))) {
                perfTime = chordCenters.get((long) (scoreNote.getInitialMillisecondsDate()));
            } else {
                continue;
            }

            if ((firstNote == null || scoreNote.getInitialMillisecondsDate() > firstNote.getInitialMillisecondsDate()) &&
                    (lastNote == null || scoreNote.getInitialMillisecondsDate() < lastNote.getInitialMillisecondsDate())) {
                fixedNotes.add(new KeyValue<>(scoreNote, perfTime * 1000));
            }
            markedPerfTime = perfTime;
        }
        if (lastNote != null) {
            fixedNotes.add(new KeyValue<>(lastNote, lastNote.getMillisecondsDate()));
        }

        List<KeyValue<Note, Double>> simplifiedFixedNotes = DouglasPeucker.simplify(fixedNotes, simplificationToleranceSeconds);

        return simplifiedFixedNotes;
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
        double[][] dp = new double[m + 1][n + 1];
        for (int i = 1; i <= m; ++i) {
            for (int j = 1; j <= n; ++j) {
                if (a.get(i - 1).equals(b.get(j - 1))) {
                    dp[i][j] = dp[i - 1][j - 1] + 1.;
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
     * Compute moving average with a triangular weights centered around the given element.
     * @param input sequence, for which moving average is computed
     * @param windowSize diameter of the triangular convolution kernel.
     * @return
     */
    public static double[] movingAverage(double[] input, int windowSize) {
        double[] result = new double[input.length];
        int n = input.length;
        int halfWindow = windowSize / 2;

        for (int i = 0; i < n; i++) {
            int start = Math.max(0, i - halfWindow);
            int end = Math.min(n - 1, i + halfWindow);
            double[] vals = Arrays.copyOfRange(input, start, end + 1);

            int size = vals.length;
            int pastSize = i - start + 1;
            int futureSize = end - i;

            double[] pastWeights = DoubleStream.iterate(1. / pastSize, j -> j + 1.0 / pastSize).limit(pastSize).toArray();
            double[] futureWeights = DoubleStream.iterate(1. / futureSize, j -> j + 1.0 / futureSize).limit(futureSize).toArray();
            double norm = DoubleStream.of(pastWeights).sum() + DoubleStream.of(futureWeights).sum();
            for (int j = 0; j < pastSize; j++) {
                pastWeights[j] = pastWeights[j] / norm;
            }
            for (int j = 0; j < futureSize; j++) {
                futureWeights[j] = futureWeights[j] / norm;
            }

            double avg = 0;
            for (int j = 0; j < pastSize; j++) {
                avg += vals[j] * pastWeights[j];
            }
            for (int j = 0; j < futureSize; j++) {
                avg += vals[j + pastSize] * futureWeights[j];
            }
            result[i] = avg;
        }
        return result;
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

    /**
     * Douglas-Peucker algorithm for simplification of linear approximations of a function: remove unnecessary points
     * if the linear interpolation without them deviates from the original function less than a given \epsilon.
     */
    private static class DouglasPeucker {
        public static List<KeyValue<Note, Double>> simplify(List<KeyValue<Note, Double>> points, double epsilon) {
            int n = points.size();
            if (n <= 2) {
                return new ArrayList<>(points);
            }

            int index = 0;
            double dMax = 0;
            for (int i = 1; i < n - 1; i++) {
                double d = perpendicularDistance(new Point(points.get(i).getKey().getMillisecondsDate() / 1000, points.get(i).getValue()),
                        new Point(points.get(0).getKey().getMillisecondsDate() / 1000, points.get(0).getValue()),
                        new Point(points.get(n - 1).getKey().getMillisecondsDate() / 1000, points.get(n - 1).getValue()));
                if (d > dMax) {
                    index = i;
                    dMax = d;
                }
            }
            List<KeyValue<Note, Double>> result = new ArrayList<>();
            if (dMax > epsilon) {
                List<KeyValue<Note, Double>> recResults1 = simplify(points.subList(0, index + 1), epsilon);
                List<KeyValue<Note, Double>> recResults2 = simplify(points.subList(index, n), epsilon);
                result.addAll(recResults1.subList(0, recResults1.size() - 1));
                result.addAll(recResults2);
            } else {
                result.add(points.get(0));
                result.add(points.get(n - 1));
            }
            return result;
        }

        private static double perpendicularDistance(Point p, Point p1, Point p2) {
            double dx = p2.x - p1.x;
            double dy = p2.y - p1.y;
            return Math.abs(dy * p.x - dx * p.y + p2.x * p1.y - p2.y * p1.x) / Math.sqrt(dy * dy + dx * dx);
        }

        private static class Point {
            double x;
            double y;
            Point(double x, double y) {
                this.x = x;
                this.y = y;
            }
        }
    }
}
