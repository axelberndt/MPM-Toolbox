package mpmToolbox.projectData.alignment.basicPitchLcsAligner;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import ai.onnxruntime.OrtSession.Result;
import com.google.common.base.Stopwatch;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import mpmToolbox.Main;

import javax.sound.midi.*;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.Stream;


/**
 * An implementation of the Basic Pitch audio-to-MIDI transcription algorithm that uses model weights exported to ONNX format.
 * <p>
 * Original <a href="https://github.com/spotify/basic-pitch/">source code</a>.
 * <p>
 * Citation: @inproceedings{2022_BittnerBRME_LightweightNoteTranscription_ICASSP,
 * author= {Bittner, Rachel M. and Bosch, Juan Jos\'e and Rubinstein, David and Meseguer-Brocal, Gabriel and Ewert, Sebastian},
 * title= {A Lightweight Instrument-Agnostic Model for Polyphonic Note Transcription and Multipitch Estimation},
 * booktitle= {Proceedings of the IEEE International Conference on Acoustics, Speech, and Signal Processing (ICASSP)},
 * address= {Singapore},
 * year= 2022,
 * }
 * @author Vladimir Viro
 */
class Transcriber {
    /**
     * The frame size of the FFT algorithm used in the transcription process.
     */
    private static final int FFT_HOP = 256;
    /**
     * The base frequency for annotations in the transcription process.
     */
    private static final float ANNOTATIONS_BASE_FREQUENCY = 27.5f;
    /**
     * The sample rate of the audio files used in the transcription process.
     */
    private static final int AUDIO_SAMPLE_RATE = 22050;
    /**
     * The length of the audio window used in the transcription process.
     */
    private static final float AUDIO_WINDOW_LENGTH = 2.0f;
    /**
     * The frames per second of the annotations used in the transcription process.
     */
    private static final int ANNOTATIONS_FPS = AUDIO_SAMPLE_RATE / FFT_HOP;
    /**
     * The number of frames in the annotations used in the transcription process.
     */
    private static final int ANNOT_N_FRAMES = (int) (ANNOTATIONS_FPS * AUDIO_WINDOW_LENGTH);
    /**
     * The number of samples in the audio window used in the transcription process.
     */
    private static final int AUDIO_N_SAMPLES = (int) (AUDIO_SAMPLE_RATE * AUDIO_WINDOW_LENGTH) - FFT_HOP;
    /**
     * The MIDI offset used in the transcription process.
     */
    private static final int MIDI_OFFSET = 21;
    /**
     * The number of overlapping frames in the audio window used in the transcription process.
     */
    private static final int N_OVERLAPPING_FRAMES = 30;
    /**
     * The length of the overlap in the audio window used in the transcription process.
     */
    private static final int OVERLAP_LENGTH = N_OVERLAPPING_FRAMES * FFT_HOP;
    /**
     * The hop size of the audio window used in the transcription process.
     */
    private static final int WINDOW_HOP_SIZE = AUDIO_N_SAMPLES - OVERLAP_LENGTH;

    /**
     * The cache used in the transcription process.
     */
    private static ObjectCache cache = null;

    /**
     * Constructor for the Transcriber class.
     * This constructor creates a transcriber with a default in-memory cache.
     */
    public Transcriber() {
        this(":memory:");
    }

    /**
     * Constructor for the Transcriber class.
     * This constructor creates a transcriber with the specified cache path and default cache size.
     *
     * @param cachePath the path to the cache
     */
    public Transcriber(String cachePath) {
        this(cachePath, (long) Math.pow(2, 29));
    }

    /**
     * Constructor for the Transcriber class.
     * This constructor creates a transcriber with the specified cache path and cache size.
     *
     * @param cachePath      the path to the cache
     * @param cacheSizeBytes the size of the cache in bytes
     */
    public Transcriber(String cachePath, long cacheSizeBytes) {
        try {
            cache = new ObjectCache(cachePath, cacheSizeBytes);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * This method windows the audio file.
     *
     * @param audioOriginal the original audio file
     * @param hopSize       the hop size of the audio window
     * @return the windowed audio file
     */
    private static float[][] windowAudioFile(float[] audioOriginal, int hopSize) {
        int nWindows = (int) Math.ceil((double) audioOriginal.length / hopSize);
        float[][] audioWindowed = new float[nWindows][];
        for (int i = 0; i < nWindows; i++) {
            int start = i * hopSize;
            int end = Math.min(start + AUDIO_N_SAMPLES, audioOriginal.length);
            int pad = AUDIO_N_SAMPLES - (end - start);
            audioWindowed[i] = Arrays.copyOfRange(audioOriginal, start, end);
            if (pad > 0) {
                audioWindowed[i] = Arrays.copyOf(audioWindowed[i], AUDIO_N_SAMPLES);
                Arrays.fill(audioWindowed[i], end - start, AUDIO_N_SAMPLES, 0);
            }
        }
        return audioWindowed;
    }

    /**
     * Gets the audio input by copying the specified audio file, extending it, and windowing it.
     *
     * @param audioOriginal The original audio file to be copied and extended.
     * @param overlapLen    The length of the overlap.
     * @param hopSize       The size of the hop.
     * @return The audio input as a 2D array of floats.
     */
    private static float[][] getAudioInput(float[] audioOriginal, int overlapLen, int hopSize) {
        audioOriginal = Arrays.copyOfRange(audioOriginal, overlapLen / 2, audioOriginal.length);
        float[] audioExtended = new float[(overlapLen / 2) + audioOriginal.length];
        System.arraycopy(audioOriginal, 0, audioExtended, (overlapLen / 2), audioOriginal.length);
        return windowAudioFile(audioExtended, hopSize);
    }

    /**
     * Reads the specified wave file and converts it to a float array.
     *
     * @param filePath           The path of the wave file to be read.
     * @param targetSamplingRate The target sampling rate to which the audio should be converted.
     * @return The audio as a float array.
     * @throws IOException                   if an I/O error occurs.
     * @throws UnsupportedAudioFileException if the audio file is not supported.
     */
    private static float[] readWaveFile(String filePath, int targetSamplingRate) throws IOException, UnsupportedAudioFileException {
        AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(new File(filePath));
        AudioFormat audioFormat = audioInputStream.getFormat();
        int samplingRate = (int) audioFormat.getSampleRate();
        int numChannels = audioFormat.getChannels();
        int sampleSizeInBits = audioFormat.getSampleSizeInBits();
        int numSamples = (int) (audioInputStream.getFrameLength() * numChannels);
        byte[] samples = new byte[numSamples * (sampleSizeInBits / 8)];
        DataInputStream dataInputStream = new DataInputStream(audioInputStream);
        dataInputStream.readFully(samples);
        float[] samples_float = new float[numSamples / numChannels];
        if (sampleSizeInBits == 8) {
            for (int i = 0; i < samples.length; i += numChannels) {
                samples_float[i / numChannels] = (samples[i] & 0xff) / 128.0f;
            }
        } else {
            for (int i = 0; i < samples.length / 2; i += numChannels) {
                short curSample = getShort(samples[i * 2], samples[i * 2 + 1]);
                samples_float[i / numChannels] = curSample / 32768.0f;
            }
        }
        if (samplingRate != targetSamplingRate) {
            samples_float = resample(samples_float, samplingRate, targetSamplingRate);
        }
        return samples_float;
    }

    /**
     * Returns a short composed of the specified bytes.
     *
     * @param argB1 the first byte of the short
     * @param argB2 the second byte of the short
     * @return a short composed of the specified bytes
     */
    private static short getShort(byte argB1, byte argB2) {
        return (short) (argB1 | (argB2 << 8));
    }

    /**
     * Resamples the given array of samples using the specified old and new sampling rates.
     *
     * @param samples         the array of samples to resample
     * @param oldSamplingRate the old sampling rate
     * @param newSamplingRate the new sampling rate
     * @return the resampled array of samples
     */
    private static float[] resample(float[] samples, int oldSamplingRate, int newSamplingRate) {
        float[] resampled = new float[(int) Math.ceil(samples.length * ((double) newSamplingRate / (double) oldSamplingRate))];
        for (int i = 0; i < resampled.length; i++) {
            resampled[i] = samples[(int) Math.floor(i * ((double) oldSamplingRate / (double) newSamplingRate))];
        }
        return resampled;
    }

    private static Map<String, float[][]> runInference(
            String audioPath,
            String modelPath)
            throws IOException, UnsupportedAudioFileException {

        float[] audioOriginal = readWaveFile(audioPath, AUDIO_SAMPLE_RATE);

        return runInference(audioOriginal, modelPath, null, null);
    }

    private static Map<String, float[][]> runInference(
            float[] audioOriginal,
            String modelPath,
            Function<String, Void> onLabel, Function<Double, Void> onProgress) {

        System.out.println("audioOriginal shape = " + audioOriginal.length);

        if (onLabel != null) onLabel.apply("windowing audio");

        float[][] audioWindowed = getAudioInput(audioOriginal, OVERLAP_LENGTH, WINDOW_HOP_SIZE);

        System.out.println("audioWindowed shape = " + audioWindowed.length + " " + audioWindowed[0].length);

        if (onLabel != null) onLabel.apply("running transcription");

        Map<String, float[][][]> output = runModel(modelPath, audioWindowed, onLabel, onProgress);

        if (onLabel != null) onLabel.apply("unwrapping transcription results");

        return unwrapOutput(output, audioOriginal.length);
    }

    /**
     * Unwraps the output map by removing half of the overlapping frames from the beginning and end of each value.
     *
     * @param output              the output map to unwrap
     * @param audioOriginalLength the original length of the audio
     * @return a new map with the unwrapped output values
     */
    private static Map<String, float[][]> unwrapOutput(Map<String, float[][][]> output, int audioOriginalLength) {
        Map<String, float[][]> unwrappedOutput = new HashMap<>();
        for (Map.Entry<String, float[][][]> entry : output.entrySet()) {
            float[][][] value = entry.getValue();

            int nOlap = (int) (0.5 * N_OVERLAPPING_FRAMES);

            float[][][] rawOutput = new float[value.length][value[0].length - 2 * nOlap][value[0][0].length];

            if (nOlap > 0) {
                for (int i = 0; i < value.length; i++) {
                    // remove half of the overlapping frames from beginning and end
                    rawOutput[i] = Arrays.copyOfRange(value[i], nOlap, value[i].length - nOlap);
                }
            }

            float[][] _unwrappedOutput = new float[rawOutput.length * rawOutput[0].length][rawOutput[0][0].length];
            for (int i = 0; i < rawOutput.length; i++) {
                for (int j = 0; j < rawOutput[0].length; j++) {
                    System.arraycopy(rawOutput[i][j], 0, _unwrappedOutput[i * rawOutput[0].length + j], 0, rawOutput[0][0].length);
                }
            }

            unwrappedOutput.put(entry.getKey(), _unwrappedOutput);
        }
        return unwrappedOutput;
    }

    /**
     * Exports the given list of notes to a MIDI file.
     *
     * @param notes    the list of notes to export
     * @param fileName the name of the file to create
     */
    public static void exportToMidi(List<NoteEventWithTime> notes, String fileName) {
        try {
            Sequence sequence = new Sequence(Sequence.PPQ, 10);
            Track track = sequence.createTrack();
            for (NoteEventWithTime note : notes) {
                track.add(
                        new MidiEvent(
                                new ShortMessage(ShortMessage.NOTE_ON, 0, note.pitchMidi, (int) (note.amplitude * 127)),
                                (long) (note.startTime * 10 * 2)));
                track.add(
                        new MidiEvent(
                                new ShortMessage(ShortMessage.NOTE_OFF, 0, note.pitchMidi, 0),
                                (long) (note.endTime * 10 * 2)));
            }
            MidiSystem.write(sequence, 1, new File(fileName));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private static Map<String, float[][][]> runModel(String modelPath, float[][] input, Function<String, Void> onLabel, Function<Double, Void> onProgress) {
        Map<String, float[][][]> out = Maps.newHashMap();

        if (onLabel != null) onLabel.apply("initializing transcription runtime");

        try (OrtEnvironment env = OrtEnvironment.getEnvironment();
             OrtSession.SessionOptions options = new OrtSession.SessionOptions();
             OrtSession session = env.createSession(modelPath)) {
            Set<String> inputNames = session.getInputNames();
            Set<String> outputNames = session.getOutputNames();
            assert inputNames.size() == 1;
            assert outputNames.size() == 3;
            assert inputNames.iterator().next().equals("serving_default_input_2:0");
            assert outputNames.iterator().next().equals("StatefulPartitionedCall:0");

            // Create input tensor object from input float array
            float[][][] contour = new float[input.length][172][264];
            float[][][] frame = new float[input.length][172][88];
            float[][][] onset = new float[input.length][172][88];

            if (onLabel != null) onLabel.apply("running transcription");

            for (int i = 0; i < input.length; i++) {
                float[][][] in = new float[1][input[i].length][1];
                for (int j = 0; j < input[0].length; j++) {
                    in[0][j][0] = input[i][j];
                }
                OnnxTensor inputTensor = OnnxTensor.createTensor(env, in);

                // Run model with Tensor input and get the Tensor output
                Result result = session.run(ImmutableMap.of("serving_default_input_2:0", inputTensor));
                contour[i] = ((float[][][]) result.get("StatefulPartitionedCall:0").get().getValue())[0];
                frame[i] = ((float[][][]) result.get("StatefulPartitionedCall:1").get().getValue())[0];
                onset[i] = ((float[][][]) result.get("StatefulPartitionedCall:2").get().getValue())[0];

                if (onProgress != null) {
                    onProgress.apply(1. * i / input.length);
                }
            }
            out.put("contour", contour);
            out.put("frame", frame);
            out.put("onset", onset);
        } catch (OrtException e) {
            e.printStackTrace();
        }
        return out;
    }

    public class TranscriptionOutput {
        public List<NoteEventWithTime> noteEventWithTimes;
        public Map<String, float[][]> transcriptionModelOutput;
    }

    /**
     * The main transcription method.
     *
     * @param audio            The mono audio data
     * @param sampleRate       The sample rate of the audio file
     * @param minNoteLen       The minimum length (in seconds) of notes to transcribe
     * @param onsetThresh      The onset threshold to be passed to this.modelOutputToNotes
     * @param frameThresh      The frame threshold to be passed to this.modelOutputToNotes
     * @param reuseModelOutput Whether to re-use the raw transcription output if available
     * @param onLabel          A callback function that is called to display status information
     * @param onProgress       A callback function that is called to update the progress bar
     * @param onDone           A callback function that is called on transcription completion
     */
    public TranscriptionOutput processWithProgress(double[] audio,
                                                   int sampleRate,
                                                   String audioId,
                                                   int minNoteLen,
                                                   double onsetThresh,
                                                   double frameThresh,
                                                   int minPitch,
                                                   int maxPitch,
                                                   boolean reuseModelOutput,
                                                   Function<String, Void> onLabel,
                                                   Function<Double, Void> onProgress,
                                                   Function<Void, Void> onDone) {

        if (onLabel == null) {
            onLabel = (com.google.common.base.Function<String, Void>) input -> null;
        }
        if (onProgress == null) {
            onProgress = (com.google.common.base.Function<Double, Void>) input -> null;
        }
        if (onDone == null) {
            onDone = (com.google.common.base.Function<Void, Void>) input -> null;
        }


        float[] _audio = new float[audio.length];
        for (int i = 0; i < audio.length; i++) {
            _audio[i] = (float) audio[i];
        }
        if (sampleRate != AUDIO_SAMPLE_RATE) {
            onLabel.apply("resampling audio of " + _audio.length + " frames.");
            _audio = resample(_audio, sampleRate, AUDIO_SAMPLE_RATE);
        }

        Map<String, float[][]> transcriptionModelOutput;
        try {
            System.out.println("retrieving transcription results " + audioId + " from cache");
            transcriptionModelOutput = (Map<String, float[][]>) cache.get(audioId);
            if (transcriptionModelOutput != null) {
                System.out.println("transcription results retrieved");
            } else {
                System.out.println("transcription results not in cache");
            }
        } catch (SQLException | IOException | ClassNotFoundException e) {
            transcriptionModelOutput = null;
            e.printStackTrace();
        }

        if (transcriptionModelOutput == null || !reuseModelOutput) {
            onProgress.apply(0.);
            String modelPath = null;
            try {
                onLabel.apply("initializing model");

                Path destination = Files.createTempFile(Paths.get(""), "model", ".onnx");
                destination.toFile().deleteOnExit();
                Files.copy(Main.class.getResourceAsStream("/resources/basic_pitch.onnx"), destination, StandardCopyOption.REPLACE_EXISTING);
                modelPath = destination.toString();

                onLabel.apply("transcribing audio");

                transcriptionModelOutput = runInference(_audio, modelPath, onLabel, onProgress);
                try {
                    cache.put(audioId, transcriptionModelOutput);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } catch (Exception e) {
                onLabel.apply(e.toString());
                throw new RuntimeException(e);
            } finally {
                try {
                    if (modelPath != null) {
                        Files.deleteIfExists(Paths.get(modelPath));
                    }
                } catch (IOException ignored) {
                }
            }
        } else {
            onProgress.apply(1.);
        }

        onLabel.apply("transcription done");


        onLabel.apply("computing note events");

        List<NoteEventWithTime> ns = modelOutputToNotes(transcriptionModelOutput,
                onsetThresh, frameThresh,
                true, minNoteLen, midiToFreq(minPitch - 2), midiToFreq(maxPitch + 2), true,
                onProgress);

        // cache.close();

        onDone.apply(null);

        TranscriptionOutput out = new TranscriptionOutput();
        out.noteEventWithTimes = ns;
        out.transcriptionModelOutput = transcriptionModelOutput;

        return out;
    }


    private static int freqToMidi(double freq) {
        return (int) Math.round(12.0 * Math.log(freq / ANNOTATIONS_BASE_FREQUENCY) / Math.log(2.0) + MIDI_OFFSET);
    }

    private static double midiToFreq(double midi) {
        return 440.0 * Math.pow(2.0, (midi - 69.0) / 12.0);
    }

    protected static class NoteEventWithTime implements Comparable<NoteEventWithTime> {
        public double startTime;
        public double endTime;
        public int pitchMidi;

        // amplitude is normalized to [0, 1]
        public final double amplitude;

        public NoteEventWithTime(double startTime, double endTime, int pitchMidi, double amplitude) {
            this.startTime = startTime;
            this.endTime = endTime;
            this.pitchMidi = pitchMidi;
            this.amplitude = amplitude;
        }

        public NoteEventWithTime(NoteEventWithTime o) {
            this.startTime = o.startTime;
            this.endTime = o.endTime;
            this.pitchMidi = o.pitchMidi;
            this.amplitude = o.amplitude;
        }

        @Override
        public String toString() {
            return "(" + startTime + " " + pitchMidi + " " + amplitude + ")";
        }

        @Override
        public int compareTo(NoteEventWithTime o) {
            return ComparisonChain.start().compare(this.startTime, o.startTime).compare(this.pitchMidi, o.pitchMidi).result();
        }
    }

    private static class NoteEvent implements Comparable<NoteEvent> {
        private int startTime;
        private int endTime;
        private int pitchMidi;
        private double amplitude;

        public NoteEvent(int startTime, int endTime, int pitchMidi, double amplitude) {
            this.startTime = startTime;
            this.endTime = endTime;
            this.pitchMidi = pitchMidi;
            this.amplitude = amplitude;
        }

        public int getStartTime() {
            return startTime;
        }

        public int getEndTime() {
            return endTime;
        }

        public int getPitchMidi() {
            return pitchMidi;
        }

        public double getAmplitude() {
            return amplitude;
        }

        @Override
        public int compareTo(NoteEvent other) {
            return Integer.compare(this.startTime, other.startTime);
        }
    }


    static class PairComparator implements Comparator<AbstractMap.SimpleEntry<Long, Double>> {
        /**
         * Compares two pairs of long and double values.
         *
         * @param o1 the first pair to compare
         * @param o2 the second pair to compare
         * @return a negative integer, zero, or a positive integer as the first pair's value is greater than, equal to,
         * or less than the second pair's value
         */
        @Override
        public int compare(AbstractMap.SimpleEntry<Long, Double> o1, AbstractMap.SimpleEntry<Long, Double> o2) {
            return ComparisonChain.start()
                    .compare(o2.getValue(), o1.getValue())
                    .result();
        }
    }

    /**
     * Converts the output of the neural network into a list of notes.
     *
     * @param frames       the frames output by the neural network
     * @param onsets       the onsets output by the neural network
     * @param onsetThresh  the threshold for an onset to be considered valid
     * @param frameThresh  the threshold for a frame to be considered valid
     * @param minNoteLen   the minimum length a note must be to be considered valid
     * @param inferOnsets  whether or not to infer onsets from the frames
     * @param maxFreq      the maximum frequency of a note
     * @param minFreq      the minimum frequency of a note
     * @param melodiaTrick whether or not to use the Melodia trick to improve onset detection
     * @param onProgress   a function that is called each time the progress is updated
     * @return a list of notes
     */
    public static List<NoteEvent> outputToNotesPolyphonic(float[][] frames, float[][] onsets, double onsetThresh,
                                                          double frameThresh, int minNoteLen, boolean inferOnsets,
                                                          double maxFreq, double minFreq, boolean melodiaTrick,
                                                          Function<Double, Void> onProgress) {
        int energyTol = 11;
        double mnl = minNoteLen / 1000. * (1. * AUDIO_SAMPLE_RATE / FFT_HOP);
        int nFrames = frames.length;
        int nFreqs = frames[0].length;

        // zero out activations above or below the max/min frequencies
        if (maxFreq != -1) {
            int maxFreqIdx = Math.round(freqToMidi(maxFreq) - MIDI_OFFSET);
            for (int i = 0; i < nFrames; i++) {
                for (int j = maxFreqIdx; j < nFreqs; j++) {
                    onsets[i][j] = 0;
                    frames[i][j] = 0;
                }
            }
        }
        if (minFreq != -1) {
            int minFreqIdx = Math.round(freqToMidi(minFreq) - MIDI_OFFSET);
            for (int i = 0; i < nFrames; i++) {
                for (int j = 0; j < minFreqIdx; j++) {
                    onsets[i][j] = 0;
                    frames[i][j] = 0;
                }
            }
        }

        // use onsets inferred from frames in addition to the predicted onsets
        if (inferOnsets) {
            onsets = getInferedOnsets(onsets, frames);
        }

        // get the onsets
        double[][] remainingEnergy = new double[nFrames][nFreqs];
        for (int i = 0; i < nFrames; i++) {
            for (int j = 0; j < nFreqs; j++) {
                remainingEnergy[i][j] = 0;
            }
        }
        for (int i = 1; i < nFrames - 1; i++) {
            for (int j = 0; j < nFreqs; j++) {
                if (onsets[i][j] > onsets[i - 1][j] && onsets[i][j] > onsets[i + 1][j]) {
                    remainingEnergy[i][j] = onsets[i][j];
                }
            }
        }

        // get the notes
        List<NoteEvent> notes = new ArrayList<>();
        for (int i = 0; i < nFrames; i++) {
            for (int j = 0; j < nFreqs; j++) {
                if (remainingEnergy[i][j] >= onsetThresh) {
                    // if we're too close to the end of the audio, continue
                    if (i >= nFrames - 1) {
                        continue;
                    }

                    // find time index at this frequency band where the frames drop below an energy threshold
                    int k = i + 1;
                    int l = 0; // number of frames since energy dropped below threshold
                    while (k < nFrames - 1 && l < energyTol) {
                        if (frames[k][j] < frameThresh) {
                            l++;
                        } else {
                            l = 0;
                        }
                        k++;
                    }

                    k -= l; // go back to frame above threshold

                    // if the note is too short, skip it
                    if (k - i <= mnl) {
                        continue;
                    }

                    // add the note
                    double amplitude = 0;
                    for (int m = i; m < k; m++) {
                        amplitude += frames[m][j];
                    }
                    amplitude /= (k - i);
                    notes.add(new NoteEvent(i, k, j + MIDI_OFFSET, amplitude));

                    // clear the activations of the extracted note
                    for (int m = k; m >= i; m--) {
                        remainingEnergy[m][j] = 0;
                        if (j > 0) remainingEnergy[m][j - 1] = 0;
                        if (j < nFreqs - 1) remainingEnergy[m][j + 1] = 0;
                    }
                }
            }
            if (onProgress != null) onProgress.apply(1. * i / nFrames);
        }

        int maxFreqIdx = Math.round(freqToMidi(maxFreq) - MIDI_OFFSET);

        if (melodiaTrick) {

            Stopwatch stopwatch = Stopwatch.createStarted();

            double[] _reflat = Stream.of(remainingEnergy)
                    .flatMapToDouble(DoubleStream::of)
                    .toArray();
            int[] _sortedIdxs = IntStream.range(0, _reflat.length)
                    .boxed().sorted(Comparator.comparingDouble(i -> -_reflat[i]))
                    .mapToInt(ele -> ele).toArray();
            double[] _sortedVals = Arrays.stream(_reflat).boxed()
                    .sorted(Collections.reverseOrder())
                    .mapToDouble(Double::doubleValue)
                    .toArray();
            Map<Long, Double> frameActivations = Maps.newHashMap();
            TreeSet<AbstractMap.SimpleEntry<Long, Double>> treeSet = new TreeSet<>(new PairComparator());
            for (int i = 0; i < _sortedVals.length; i++) {
                long k = _sortedIdxs[i];
                double v = _sortedVals[i];
                if (v > frameThresh) {
                    treeSet.add(new AbstractMap.SimpleEntry<>(k, v));
                    frameActivations.put(k, v);
                }
            }

            System.out.println("got " + treeSet.size() + " elements, first is " + treeSet.first());

            while (!treeSet.isEmpty()) {

                AbstractMap.SimpleEntry<Long, Double> max = treeSet.pollFirst();
                long idx = max.getKey();
                frameActivations.remove(idx);

                int i_mid = (int) (idx / nFreqs);
                int freqIdx = (int) (idx % nFreqs);

                // forward pass
                long i = i_mid + 1;
                int k = 0;

                while (i < nFrames - 1 && k < energyTol) {

                    long newIdx = i * nFreqs + freqIdx;
                    if (!frameActivations.containsKey(newIdx)) {
                        k++;
                    } else {
                        k = 0;
                    }

                    remove(i, freqIdx, treeSet, frameActivations, nFreqs);

                    if (freqIdx < maxFreqIdx) {
                        remove(i, freqIdx + 1, treeSet, frameActivations, nFreqs);
                    }
                    if (freqIdx > 0) {
                        remove(i, freqIdx - 1, treeSet, frameActivations, nFreqs);
                    }

                    i++;
                }

                long iEnd = i - 1 - k; // go back to frame above threshold

                // backward pass
                long j = i_mid - 1;
                k = 0;

                while (j > 0 && k < energyTol) {

                    long newIdx = j * nFreqs + freqIdx;
                    if (!frameActivations.containsKey(newIdx)) {
                        k++;
                    } else {
                        k = 0;
                    }

                    remove(j, freqIdx, treeSet, frameActivations, nFreqs);

                    if (freqIdx < maxFreqIdx) {
                        remove(j, freqIdx + 1, treeSet, frameActivations, nFreqs);
                    }
                    if (freqIdx > 0) {
                        remove(j, freqIdx - 1, treeSet, frameActivations, nFreqs);
                    }

                    j--;
                }

                long iStart = j + 1 + k; // go back to frame above threshold
                assert iStart >= 0;
                assert iEnd < nFrames;

                if (iEnd - iStart <= mnl) {
                    // note is too short, skip it
                    continue;
                }

                // add the note
                double amplitude = 0;
                for (int frm = (int) iStart; frm < iEnd; frm++) {
                    amplitude += frames[frm][freqIdx];
                }
                amplitude = amplitude / (iEnd - iStart);

                NoteEvent note = new NoteEvent((int) iStart, (int) iEnd, freqIdx + MIDI_OFFSET, amplitude);

                notes.add(note);
            }

            System.out.println(stopwatch.elapsed(TimeUnit.SECONDS) + "s for melodia");
        }

        Collections.sort(notes);

        return notes;
    }

    private static void remove(long i, long freqIdx, TreeSet<AbstractMap.SimpleEntry<Long, Double>> treeSet, Map<Long, Double> kv, int nFreqs) {
        long newIdx = i * nFreqs + freqIdx;
        double newVal = kv.getOrDefault(newIdx, 0d);

        treeSet.remove(new AbstractMap.SimpleEntry<>(newIdx, newVal));
        kv.remove(newIdx);
    }

    /**
     * Calculates the inferred onsets of a given set of onsets and frames.
     *
     * @param onsets the onsets to infer
     * @param frames the frames to use for inference
     * @return the inferred onsets
     */
    private static float[][] getInferedOnsets(float[][] onsets, float[][] frames) {
        int nFrames = frames.length;
        int nFreqs = frames[0].length;

        float[][] maxOnsetsDiff = new float[nFrames][nFreqs];
        for (int i = 0; i < nFrames; i++) {
            for (int j = 0; j < nFreqs; j++) {
                maxOnsetsDiff[i][j] = 0;
            }
        }

        float[][] diffs = new float[2][nFrames * nFreqs];
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < nFrames * nFreqs; j++) {
                diffs[i][j] = 0;
            }
        }

        for (int i = 0; i < nFrames; i++) {
            System.arraycopy(frames[i], 0, diffs[0], i * nFreqs, nFreqs);
        }

        for (int i = 0; i < nFrames - 1; i++) {
            System.arraycopy(frames[i + 1], 0, diffs[1], i * nFreqs, nFreqs);
        }

        for (int i = 0; i < nFrames; i++) {
            for (int j = 0; j < nFreqs; j++) {
                maxOnsetsDiff[i][j] = Math.max(onsets[i][j], diffs[0][i * nFreqs + j] - diffs[1][i * nFreqs + j]);
            }
        }

        return maxOnsetsDiff;
    }

    private static float[][] deepCopy(float[][] matrix) {
        return java.util.Arrays.stream(matrix).map(el -> el.clone()).toArray($ -> matrix.clone());
    }

    /**
     * Converts model output to a list of notes with timestamps.
     *
     * @param output       the model output map
     * @param onsetThresh  the onset threshold
     * @param frameThresh  the frame threshold
     * @param inferOnsets  whether to infer onsets
     * @param minNoteLen   the minimum length of a note
     * @param minFreq      the minimum frequency of a note
     * @param maxFreq      the maximum frequency of a note
     * @param melodiaTrick whether to use the Melodia trick
     * @param onProgress   a function to call on progress updates
     * @return a list of notes with timestamps
     */
    private static List<NoteEventWithTime> modelOutputToNotes(
            Map<String, float[][]> output,
            double onsetThresh,
            double frameThresh,
            boolean inferOnsets,
            int minNoteLen,
            double minFreq,
            double maxFreq,
            boolean melodiaTrick,
            Function<Double, Void> onProgress) {

        float[][] frames = deepCopy(output.get("frame"));
        float[][] onsets = deepCopy(output.get("onset"));

        List<NoteEvent> estimatedNotes = outputToNotesPolyphonic(
                frames,
                onsets,
                onsetThresh,
                frameThresh,
                minNoteLen,
                inferOnsets,
                maxFreq,
                minFreq,
                melodiaTrick,
                onProgress);

        double[] times = modelFramesToTime(frames.length);
        List<NoteEventWithTime> estimatedNotesTimeSeconds = new ArrayList<>();
        for (NoteEvent note : estimatedNotes) {
            estimatedNotesTimeSeconds.add(
                    new NoteEventWithTime(
                            times[note.getStartTime()],
                            times[note.getEndTime()],
                            note.getPitchMidi(),
                            note.getAmplitude()));
        }

        return estimatedNotesTimeSeconds;
    }

    /**
     * Converts the number of model frames to time values.
     *
     * @param nFrames The number of model frames.
     * @return An array of time values.
     */
    private static double[] modelFramesToTime(int nFrames) {
        double[] originalTimes = new double[nFrames];
        double offsetInSec = 0.2;
        double windowOffset = (1. * FFT_HOP / AUDIO_SAMPLE_RATE) * (
                ANNOT_N_FRAMES - (1. * AUDIO_N_SAMPLES / FFT_HOP)
        ); // + 0.0018;
        for (int i = 0; i < nFrames; i++) {
            float windowHopSizeInSec = 1f * FFT_HOP / AUDIO_SAMPLE_RATE;
            originalTimes[i] = (double) i * windowHopSizeInSec - i * windowOffset / ANNOT_N_FRAMES + offsetInSec;
        }
        return originalTimes;
    }

}