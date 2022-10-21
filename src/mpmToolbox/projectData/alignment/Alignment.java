package mpmToolbox.projectData.alignment;

import com.alee.api.annotations.NotNull;
import com.sun.media.sound.InvalidDataException;
import meico.mei.Helper;
import meico.mpm.elements.Performance;
import meico.mpm.elements.maps.ArticulationMap;
import meico.mpm.elements.maps.AsynchronyMap;
import meico.mpm.elements.maps.GenericMap;
import meico.mpm.elements.maps.TempoMap;
import meico.mpm.elements.maps.data.ArticulationData;
import meico.msm.Msm;
import nu.xom.*;

import java.util.ArrayList;
import java.util.UUID;

/**
 * This class links all MSM note elements with expression data (milliseconds.date, milliseconds.date.end, velocity).
 * It further provides a routine to render a visual representation of the piano roll.
 * @author Axel Berndt
 */
public class Alignment {
    private final ArrayList<mpmToolbox.projectData.alignment.Part> parts = new ArrayList<>();
    private PianoRoll pianoRoll = null;
    private final Msm msm;
    private final ArrayList<double[]> timingTransformation = new ArrayList<>();   // each element provides the following values {startDate, endDate, toStartDate, toEndDate}, all in milliseconds
    private Note lastNoteSounding = null;

    /**
     * constructor
     * @param msm
     */
    private Alignment(Msm msm) {
        super();
        this.msm = msm;

        for (Element part : msm.getParts()) {
            try {
                this.add(new Part(part));
            } catch (InvalidDataException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * constructor
     * @param msm the MSM to be aligned
     * @param alignmentData the alignment element from the project file or null if the audio is newly imported into the project
     */
    public Alignment(Msm msm, Element alignmentData) {
        this(msm);

        if (alignmentData == null)
            return;

        // edit the data just generated according to the input alignment data
        for (Element inputPart : alignmentData.getChildElements("part")) {  // for each alignment part in the input data
            String name = Helper.getAttributeValue("name", inputPart);
            String number = Helper.getAttributeValue("number", inputPart);
            String midiChannel = Helper.getAttributeValue("midi.channel", inputPart);
            String midiPort = Helper.getAttributeValue("midi.port", inputPart);
            String id = Helper.getAttributeValue("ref", inputPart);

            mpmToolbox.projectData.alignment.Part part = null;

            // find the corresponding initially generated alignment part
            for (Part p : this.parts) {
                if (Helper.getAttributeValue("name", p.getXml()).equals(name)
                        && Helper.getAttributeValue("number", p.getXml()).equals(number)
                        && Helper.getAttributeValue("ref", p.getXml()).equals(id)
                        && Helper.getAttributeValue("midi.channel", p.getXml()).equals(midiChannel)
                        && Helper.getAttributeValue("midi.port", p.getXml()).equals(midiPort)) {    // if the corresponding MSM part is found
                    part = p;           // get the alignment part where we will edit the alignment data according to the input alignment data
                    break;
                }
            }

            if (part == null)           // if no corresponding part was found
                continue;               // we seem to have odd input data, so we do not import it into the project and it will be removed at the next save operation

            part.syncWith(inputPart);   // let the part synchronize with the input alignment data
        }
    }

    /**
     * add a part to the alignment data
     * @param part
     * @return
     */
    private boolean add(@NotNull Part part) {
        int number = part.getNumber();
        int index = 0;
        for (int i = this.parts.size() - 1; i >= 0; --i) {
            if (this.parts.get(i).getNumber() <= number) {
                index = i + 1;
                break;
            }
        }
        this.parts.add(index, part);
        this.lastNoteSounding = null;
        return true;
    }

    /**
     * access a part by number
     * @param number
     * @return
     */
    public Part getPart(int number) {
        for (Part part : this.parts) {
            if (part.getNumber() == number) {
                return part;
            }
        }
        return null;
    }

    /**
     * access part by reference to the XML source
     * @param partElement
     * @return
     */
    public Part getPart(Element partElement) {
        for (Part part : this.parts) {
            if (part.getXml() == partElement) {
                return part;
            }
        }
        return null;
    }

    /**
     * retrieve the part that contains the given note
     * @param note
     * @return the part or null if no part contains the note
     */
    public Part getPart(Note note) {
        for (Part part : this.parts) {
            if (part.contains(note))
                return part;
        }
        return null;
    }

    /**
     * access the list of all parts
     * @return
     */
    public ArrayList<Part> getParts() {
        return this.parts;
    }

    /**
     * generate a project data XML element
     * @return
     */
    public Element toXml() {
        Element out = new Element("alignment");

        // get the XML of each part's alignment
        for (Part part : this.parts)
            out.appendChild(part.toXml());

        return out;
    }

    /**
     * Use this method when notes are un-fixed or fixed notes are silently repositioned.
     * It recomputes the milliseconds timing of all notes.
     */
    public void updateTiming() {
        this.updateTimingTransformation();
        this.renderTiming();                                    // compute the new milliseconds timing
        this.lastNoteSounding = null;
    }

    /**
     * initialize the timing transformation based on the fixed notes of all the parts;
     * invoke this method whenever up-to-date timing transformation data are to be processed
     */
    private void updateTimingTransformation() {
        // create an ordered list of all fixed notes
        ArrayList<Note> fixedNotes = new ArrayList<>();
        for (Part part : this.getParts()) {
            ArrayList<Note> f = part.getAllFixedNotes(false);   // get the part's list of fixed notes in the order of the current timing

            if (fixedNotes.isEmpty()) {                         // if the fixedNotes list is empty
                fixedNotes.addAll(f);                           // we can simply add the part's list, it is already ordered
                continue;                                       // go on with the next part
            }

            // add the fixed notes to the list and keep the temporal order
            int i = 0;
            for (Note n : f) {
                boolean added = false;
                for (; i < fixedNotes.size(); ++i) {
                    if (fixedNotes.get(i).getMillisecondsDate() > n.getMillisecondsDate()) {
                        fixedNotes.add(i, n);
                        added = true;
                        i++;
                        break;
                    }
                }
                if (!added)
                    fixedNotes.add(n);
            }
        }

        if (fixedNotes.isEmpty())                               // if no fixed notes
            return;                                             // we are done

        this.timingTransformation.clear();                      // we compute the timing transformation data anew
        Note beginner = null;                                   // the section to be scaled begins with this note's millisecondsDate and initialMillisecondsDate
        Note stopper = null;                                    // the section is scaled into [beginner.millisecondsDate; stopper.millisecondsDate)
        for (int i=0; i < fixedNotes.size(); ++i) {
            Note n = fixedNotes.get(i);

            if (stopper == null)                                // if we seek a stopper
                stopper = n;                                    // we take the first note we find

            // check if this is a beginner note, i.e. a note that was not shifted before another fixed note
            boolean isEnder = true;
            for (int j=i+1; j < fixedNotes.size(); ++j) {       // check if another fixed note follows in the sequence that was initially before the current note
                if (fixedNotes.get(j).getInitialMillisecondsDate() < n.getInitialMillisecondsDate()) {  // if we found one
                    isEnder = false;                            // set the flag false
                    break;
                }
            }

            if (isEnder) {                                      // if we found the next beginner = "ender" of the previous section
                if (beginner != null)                           // usually we have a beginner
                    this.timingTransformation.add(new double[]{beginner.getInitialMillisecondsDate(), n.getInitialMillisecondsDate(), beginner.getMillisecondsDate(), stopper.getMillisecondsDate()});
                else                                            // but right at the beginning we have no beginner but have to handle the notes before the first fixed note
                    this.timingTransformation.add(new double[]{0.0, n.getInitialMillisecondsDate(), Math.max(0.0, stopper.getMillisecondsDate() - n.getInitialMillisecondsDate()), stopper.getMillisecondsDate()});

                beginner = n;
                stopper = null;
            }
        }

        // after the above loop there is the last beginner left for which we have to add an entry in the timingTransformation list; it will keep the tempo of the previous segment until the end of the last sounding note
        assert beginner != null;
        double[] previous = null;
        for (int i = this.timingTransformation.size() - 1; i >= 0; --i) {   // find the last transformation segment before the final one that has an initial length > 0
            previous = this.timingTransformation.get(i);
            if (previous[1] - previous[0] > 0.0)
                break;
        }
        if (previous == null) {                                             // if we did not find a previous segment with a substantial length (should never be the case)
            this.timingTransformation.add(new double[]{beginner.getInitialMillisecondsDate(), Double.MAX_VALUE, beginner.getMillisecondsDate(), Double.MAX_VALUE}); // this is our fallback
        } else {                                                            // we found a substantial segment; now we take its tempo also for the last/open-ended segment
            Note last = this.getLastNoteSounding();
            this.timingTransformation.add(new double[]{beginner.getInitialMillisecondsDate(),
                    last.getInitialMillisecondsDateEnd(),
                    beginner.getMillisecondsDate(),
                    beginner.getMillisecondsDate() + ((previous[3] - previous[2]) * (last.getInitialMillisecondsDateEnd() - beginner.getInitialMillisecondsDate())) / (previous[1] - previous[0])});
        }
    }

    /**
     * apply the current timing transformation to all parts, so their notes get new millisecondsDates and millisecondsDateEnds
     */
    private void renderTiming() {
        for (Part part : this.getParts())   // apply the timing transform to each part
            part.transformTiming(this.timingTransformation);
    }

    /**
     * derive an MPM performance from the alignment data
     * @param performance add the data to this performance, or set this null to generate a new performance
     * @return the performance
     */
    public Performance exportPerformance(Performance performance) {
        if (performance == null)
            performance = Performance.createPerformance(UUID.randomUUID().toString(), this.msm.getPulsesPerQuarter());

        this.importMsmPartsToPerformance(performance);

        this.updateTimingTransformation();      // read the positioning of all the fixed notes into the timing transformation list

        // export the timing into MPM tempo and asynchrony maps and add them to the performance
        for (GenericMap map : this.exportTiming())
            performance.getGlobal().getDated().addMap(map);

        this.exportArticulation(performance);   // check all notes and correct those with divergent onsets/offsets via articulation

        return performance;
    }

    /**
     * helper method to export this alignment to an MPM performance
     * @param performance
     */
    private void importMsmPartsToPerformance(Performance performance) {
        for (Element part : this.msm.getParts()) {
            String name = Helper.getAttributeValue("name", part);
            int number = Integer.parseInt(Helper.getAttributeValue("number", part));
            int midiChannel = Integer.parseInt(Helper.getAttributeValue("midi.channel", part));
            int midiPort = Integer.parseInt(Helper.getAttributeValue("midi.port", part));

            meico.mpm.elements.Part mpmPart = performance.getPart(number);
            if ((mpmPart != null) && mpmPart.getName().equals(name) && (mpmPart.getMidiChannel() == midiChannel) && (mpmPart.getMidiPort() == midiPort)) // if there is already a part in the MPM with identical attributes
                continue;                                                                           // we do not allow creating another one

            String id = Helper.getAttributeValue("id", part);
            performance.addPart(meico.mpm.elements.Part.createPart(name, number, midiChannel, midiPort, id.isEmpty() ? null : id));
        }
    }

    /**
     * Derives a global tempo and asynchrony maps from the alignment's timing transformation data.
     * Be sure to execute this.updateTimingTransformation() before invoking this method!
     * @return a list containing tempomap, asynchronymap
     */
    private ArrayList<GenericMap> exportTiming() {
        // create all required timing maps
        TempoMap tempoMap = TempoMap.createTempoMap();
        AsynchronyMap asynchronyMap = AsynchronyMap.createAsynchronyMap();

        int ppq = this.msm.getPPQ();
        Element timeSignatureMap = this.msm.getGlobal().getFirstChildElement("dated").getFirstChildElement("timeSignatureMap");

        // for each segment of the timing transformation we have a double array with values {startDate, endDate, toStartDate, toEndDate} to be transformed to tempo instructions
        double prevMsEndDate = 0.0;                                                         // this is to keep track of the toEndDate of the previous segment when processing the next
        double offset = 0.0;                                                                // keep track of how nay offset has been added up throughout the process
        boolean generateAsyncOnNextRegularTransform = false;
        for (double[] transform : this.timingTransformation) {
//            System.out.print(Arrays.toString(transform));

            // special case: notes in the sequence are interchanged
            if (transform[2] != prevMsEndDate) {                                            // if there is a gap between the previous tEndDate and this segment's toStartDate, the sequence of notes has changed
                offset += transform[2] - prevMsEndDate;                                     // compute the milliseconds size of the gap
                asynchronyMap.addAsynchrony(transform[0], offset);                          // we create an asynchrony instruction right here; it may include offset slices
                generateAsyncOnNextRegularTransform = false;                                // we do not want to add another asynchrony instruction in this iteration as it would overwrite the one we just created
            }

            // special case: notes at the same date are displaced
            if (transform[0] >= transform[1]) {                                             // if two notes at the same time were displaced, we have an undefined situation
                offset += transform[3] - transform[2];                                      // the milliseconds offset to be added to the timing
                generateAsyncOnNextRegularTransform = true;                                 // signal that there is offset to be added as an asynchrony instruction in a subsequent loop iteration
                prevMsEndDate = transform[3];                                               // keep track of the toEndDate in the next iteration, so we are able to detect gaps (see the previous special case)
                continue;                                                                   // done with this segment
            }

            // implement pending offsets as asynchrony instruction
            if (generateAsyncOnNextRegularTransform) {                                      // if offset is pending
                asynchronyMap.addAsynchrony(transform[0] + 0.01, offset);                   // create a corresponding asynchrony instruction; but set its date right after the start date of this transform segment, so its events at the start date are not affected
                generateAsyncOnNextRegularTransform = false;                                // we are done with the offset
            }

            // process the regular timing transformation segments
            Element timeSig = Msm.getElementBeforeAt(transform[0], timeSignatureMap);       // get the time signature at the transform's position
            int denominator = (timeSig == null) ? 4 : Integer.parseInt(Helper.getAttributeValue("denominator", timeSig));
            double beatLength = 1.0 / denominator;
            double beatLengthTicks = ppq * 4 * beatLength;
            double beatCountInTransformSegment = (transform[1] - transform[0]) / beatLengthTicks;
            double tempo = (beatCountInTransformSegment * 60000.0) / (transform[3] - transform[2]);
            tempoMap.addTempo(transform[0], Double.toString(tempo), beatLength);

            prevMsEndDate = transform[3];
        }


        // put all non-empty maps in the resultant list
        ArrayList<GenericMap> maps = new ArrayList<>();
        if (!tempoMap.isEmpty())
            maps.add(tempoMap);
        if (!asynchronyMap.isEmpty())
            maps.add(asynchronyMap);

        return maps;
    }

    /**
     * This method checks the millisecond on- and offsets of all notes in the alignment against their timing
     * in the performance rendering. If correction is needed, it is done via a local articulation.
     * @param performance the performance that will be refined with local articulation maps, if necessary
     */
    private void exportArticulation(Performance performance) {
        for (Element part : performance.perform(this.msm).getParts()) {
            Element dated = Helper.getFirstChildElement("dated", part);
            if (dated == null)
                continue;
            Element score = Helper.getFirstChildElement("score", dated);
            if (score == null)
                continue;
            Part alignPart = this.getPart(Integer.parseInt(Helper.getAttributeValue("number", part)));
            if (alignPart == null)
                continue;

            ArticulationMap articulationMap = ArticulationMap.createArticulationMap();

            for (Element note : Helper.getAllChildElements("note", score)) {
                String id = note.getAttributeValue("id", "http://www.w3.org/XML/1998/namespace");
                if ((id == null) || (id.isEmpty()))
                    continue;

                Note alignNote = alignPart.getNote(id);
                if (alignNote == null)
                    continue;

                Attribute a = Helper.getAttribute("milliseconds.date", note);
                if (a == null)
                    continue;
                double onset = Double.parseDouble(a.getValue());

                a = Helper.getAttribute("milliseconds.date.end", note);
                if (a == null)
                    continue;
                double offset = Double.parseDouble(a.getValue());

                // if the onset deviates by more than a millisecond, we have to add a delay to the note
                ArticulationData articulation = null;
                double onsetDif = alignNote.getMillisecondsDate() - onset;
                if (Math.abs(onsetDif) > 1.0) {
                    articulation = new ArticulationData();
                    articulation.date = alignNote.getInitialMillisecondsDate();
                    articulation.noteid = "#" + alignNote.getId();
                    articulation.absoluteDelayMs = onsetDif;
                }

                // if the offset deviates by more than a millisecond, we have to correct it
                double offsetDif = alignNote.getMillisecondsDateEnd() - offset;
                if (Math.abs(offsetDif) > 1.0) {
                    if (articulation == null) {
                        articulation = new ArticulationData();
                        articulation.date = alignNote.getInitialMillisecondsDate();
                        articulation.noteid = "#" + alignNote.getId();
                    }
                    articulation.absoluteDurationMs = alignNote.getMillisecondsDateEnd() - alignNote.getMillisecondsDate();
                }

                if (articulation != null)
                    articulationMap.addArticulation(articulation);
            }

            // if the articulation map is not empty, add it to the performance's respective part
            if (!articulationMap.isEmpty()) {
                meico.mpm.elements.Part perfPart = performance.getPart(alignPart.getNumber());
                perfPart.getDated().addMap(articulationMap);
            }
        }
    }

    /**
     * Sets a new milliseconds date and end date of the note and places it in the note sequence accordingly.
     * The note is made fixed. The non-fixed notes are repositioned subsequently.
     * @param note the note to be moved
     * @param toMilliseconds the new onset position of the note
     */
    public void reposition(@NotNull Note note, double toMilliseconds) {
        note.setFixed(true);                                // pin the note to its position, so it will not be affected by timing interpolations when another note is dragged

        if (note.getMillisecondsDate() == toMilliseconds)   // the note does not move
            return;                                         // done

        // we do not allow shifting the note to negative timing
        if (toMilliseconds < 0.0)
            toMilliseconds = 0.0;

        for (Part part : this.getParts())
            if (part.contains(note))
                part.reposition(note, toMilliseconds);

        this.updateTiming();
    }

    /**
     * resets the values of each note in each part to their initial values;
     * the invoking application should also run recomputePianoRoll() to update the piano roll image
     */
    public void reset() {
        for (Part part : this.getParts()) {
            part.reset();
        }
        this.lastNoteSounding = null;
    }

    /**
     * retrieve the last sounding note; if there are several, only one is returned
     * @return the last sounding note or null
     */
    public Note getLastNoteSounding() {
        if (this.lastNoteSounding == null) {
            for (Part part : this.parts) {
                Note note = part.getLastNoteSounding();
                if (note == null)
                    continue;
                if ((this.lastNoteSounding == null) || (note.getMillisecondsDateEnd() > this.lastNoteSounding.getMillisecondsDateEnd())) {
                    this.lastNoteSounding = note;
                }
            }
        }
        return this.lastNoteSounding;
    }

    /**
     * get the first note in the alignment
     * @return
     */
    public Note getFirstNote() {
        Note first = null;
        for (Part part : this.parts) {
            if (part.getNoteSequence().size() == 0)
                continue;
            Note candidate = part.getNoteSequence().get(0);
            if (candidate == null)
                continue;
            if (first == null) {
                first = candidate;
                continue;
            }
            if (first.getDate() > candidate.getDate())
                first = candidate;
        }
        return first;
    }

    /**
     * Given a milliseconds date, compute the non-performed date from it according to this alignment. This is only an approximation.
     * @param milliseconds
     * @return since the parts can differ in timing, we can only return a range, i.e. a tuple [min, max]
     */
    public double[] getCorrespondingTickDate(double milliseconds) {
        // range value are initialized with inverted extreme values, so the subsequently computed values span the range correctly
        double min = this.getLastNoteSounding().getDate() + this.getLastNoteSounding().getDuration();
        double max = 0.0;

        // compute the approximate date for each part and span the range
        for (Part part : this.getParts()) {
            double candidate = part.getCorrespondingTickDate(milliseconds);
            if (candidate < min)
                min = candidate;
            if (candidate > max)
                max = candidate;
        }

        // make sure the min and max values are in correct order and return the range
        return (min > max) ? new double[]{max, min} : new double[]{min, max};
    }

    /**
     * Given a tick date, compute the respective milliseconds date from it according to this alignment. This is only an approximation.
     * @param ticks
     * @return since the parts can differ in timing, we can only return a range, i.e. a tuple [min, max]
     */
    public double[] getCorrespondingMillisecondsDate(double ticks) {
        // range value are initialized with inverted extreme values, so the subsequently computed values span the range correctly
        double min = Double.MAX_VALUE;
        double max = 0.0;

        // compute the approximate date for each part and span the range
        for (Part part : this.getParts()) {
            double candidate = part.getCorrespondingMillisecondsDate(ticks);
            if (candidate < min)
                min = candidate;
            if (candidate > max)
                max = candidate;
        }

        // make sure the min and max values are in correct order and return the range
        return (min > max) ? new double[]{max, min} : new double[]{min, max};
    }

    /**
     * compute the length of the alignment in milliseconds
     * @return
     */
    public double getMillisecondsLength() {
        return (this.getLastNoteSounding() == null) ? 0.0 : this.getLastNoteSounding().getMillisecondsDateEnd();
    }

    /**
     * Scale the complete music to the specified length, i.e. the last milliseconds.date.end.
     * This transformation is also applied to fixed notes!
     * @param milliseconds the milliseconds length to which the notes are scaled or null if no scaling is set
     */
    public void scaleOverallTiming(double milliseconds) {
        Note lastNoteSounding = this.getLastNoteSounding();
        if (lastNoteSounding == null)
            return;

        lastNoteSounding.setFixed(true);

        double factor = milliseconds / lastNoteSounding.getMillisecondsDateEnd();

        for (Part part : this.parts) {
            part.scaleOverallTiming(factor);
        }
    }

    /**
     * compile a PianoRoll object from the Alignment's Parts
     * @param fromMilliseconds
     * @param toMilliseconds
     * @param imgWidth
     * @param imgHeight
     * @return
     */
    public PianoRoll getPianoRoll(double fromMilliseconds, double toMilliseconds, int imgWidth, int imgHeight) {
        // do not compute a new piano roll if the metrics did not change
        if ((this.pianoRoll != null) && this.pianoRoll.sameMetrics(fromMilliseconds, toMilliseconds, imgWidth, imgHeight))
            return this.pianoRoll;

        this.pianoRoll = new PianoRoll(fromMilliseconds, toMilliseconds, imgWidth, imgHeight);

        // combine the piano rolls of the parts into this one
        for (Part p : this.parts)
           this.pianoRoll.add(p.getPianoRoll(fromMilliseconds, toMilliseconds, imgWidth, imgHeight));

        return this.pianoRoll;
    }

    /**
     * get the latest PianoRoll instance without recomputing it
     * @return the piano roll or null
     */
    public PianoRoll getPianoRoll() {
        return this.pianoRoll;
    }

    /**
     * recomputes the piano roll image with the same metrics as the current one
     * @return
     */
    public PianoRoll recomputePianoRoll() {
        if (this.pianoRoll == null)
            return null;

        for (Part p : this.parts)
            p.recomputePianoRoll();

        double fromMilliseconds = this.pianoRoll.getFromMilliseconds();
        double toMilliseconds = this.pianoRoll.getToMilliseconds();
        int imgWidth = this.pianoRoll.getWidth();
        int imgHeight = this.pianoRoll.getHeight();

        this.pianoRoll = null;
        return this.getPianoRoll(fromMilliseconds, toMilliseconds, imgWidth, imgHeight);
    }

    /**
     * put all alignment data (milliseconds.date, milliseconds.date.end, velocity) into a clone Msm object
     * @return
     */
    public Msm getExpressiveMsm() {
        Msm expMsm = this.msm.clone();
        expMsm.setFile(Helper.getFilenameWithoutExtension(expMsm.getFile().getPath()) + "_alignment.msm");

        expMsm.getGlobal().getFirstChildElement("dated").removeChildren();  // only the notes have meaningful dates, other dated stuff must be removed in order not to clash with the duration of the midi sequence

        for (Element partElt : expMsm.getParts()) {
            Part part = this.getPart(Integer.parseInt(Helper.getAttributeValue("number", partElt)));
            if (part == null)
                continue;

            Element dated = partElt.getFirstChildElement("dated");
            Element score = null;

            // only the notes have meaningful dates, other dated stuff must be removed in order not to clash with the duration of the midi sequence
            for (Element rem : dated.getChildElements()) {
                if (rem.getLocalName().equals("score")) {   // if we stumbled over the score map
                    score = rem;                            // we keep it; it will be processed afterwards
                    continue;
                }
                dated.removeChild(rem);
            }

            if (score == null)
                continue;

            for (Element noteElt : score.getChildElements("note")) {
                Attribute id = noteElt.getAttribute("id", "http://www.w3.org/XML/1998/namespace");
                if (id == null)
                    continue;

                Note note = part.getNote(id.getValue());
                if (note == null)
                    continue;

                noteElt.addAttribute(new Attribute("milliseconds.date", Double.toString(note.getMillisecondsDate())));
                noteElt.addAttribute(new Attribute("milliseconds.date.end", Double.toString(note.getMillisecondsDateEnd())));
                noteElt.addAttribute(new Attribute("velocity", Double.toString(note.getVelocity())));
            }
        }

        return expMsm;
    }
}
