package mpmToolbox.projectData.alignment;

import com.alee.api.annotations.NotNull;
import com.sun.media.sound.InvalidDataException;
import meico.mei.Helper;
import nu.xom.Attribute;
import nu.xom.Element;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * An instance of this is a Hashmap of all MSM notes, accessed via their XML ID.
 * The notes are augmented with performance information.
 * @author Axel Berndt
 */
public class Part {
    private final Element xml;                                          // a reference to the original MSM part element
    private final int number;
    private final HashMap<String, Note> notes = new HashMap<>();        // access notes by id
    private final ArrayList<Note> noteSequence = new ArrayList<>();     // the notes in sequential order of their current date
    private final ArrayList<Note> initialSequence = new ArrayList<>();  // the notes in sequential order of their initial date
    private final ArrayList<Note> tickSequence = new ArrayList<>();     // the notes in sequential order of their tick date
    private PianoRoll pianoRoll = null;
    private Note lastNoteSounding = null;

    /**
     * constructor
     * @param msmPart
     */
    protected Part(Element msmPart) throws InvalidDataException {
        super();

        this.xml = msmPart;
        this.number = Integer.parseInt(Helper.getAttributeValue("number", this.xml));

        // get the dated environment
        Element dated = msmPart.getFirstChildElement("dated");
        if (dated == null)
            throw new InvalidDataException("Invalid MSM part element " + Helper.cloneElement(msmPart).toXML() + "; missing child element dated.");

        // parse the score and fill the HashMap with (ID, Note) tuples
        Element score = dated.getFirstChildElement("score");
        if (score != null) {
            for (Element e : score.getChildElements("note")) {
                try {
                    this.add(new Note(e));
                } catch (InvalidDataException | NumberFormatException exception) {
                    exception.printStackTrace();
                }
            }
        }
    }

    /**
     * This method is only used when loading an MPM Toolbox project.
     * It inputs the data from the project file to the notes in this part.
     * @param alignmentData
     */
    protected void syncWith(Element alignmentData) {
        for (Element e : alignmentData.getChildElements()) {
            String ref = Helper.getAttributeValue("ref", e);
            Note note = this.getNote(ref);

            if (note == null)
                continue;

            note.syncWith(e);
        }
    }

    /**
     * add an ID-note pair to the part
     * @param note
     * @return If there is already a note associated with the id, it will be replaced and returned. Otherwise (no note replaced), this returns null.
     */
    public Note add(Note note) {
        Note out = this.notes.put(note.getId(), note);  // this will add the id-note pair to the hashmap and overwrite any other note behind the same id; out will hold that previous note or null

        if (out != null) {                              // if there was a previous note that we replaced with the above line
            this.noteSequence.remove(out);              // remove it also from the sequences
            this.initialSequence.remove(out);
            this.tickSequence.remove(out);
        }
        this.addToInitialSequence(note);                // add the note to initial sequence
        this.addToSequence(note);                       // add the note to the sequence
        this.addToTickSequence(note);                   // add the note to the tick sequence

        return out;
    }

    /**
     * add the note to the note sequence
     * @param note
     */
    private void addToSequence(Note note) {
        // add note to noteSequence
        int i = this.noteSequence.size() - 1;
        for (; i >= 0; --i) {
            Note n = this.noteSequence.get(i);
            double date = n.getMillisecondsDate();
            if (date <= note.getMillisecondsDate())
                break;
        }
        this.noteSequence.add(i + 1, note);               // insert note also to the sequence

        if ((this.lastNoteSounding == null) || (this.lastNoteSounding.getMillisecondsDateEnd() < note.getMillisecondsDateEnd()))
            this.lastNoteSounding = note;
    }

    /**
     * add the note to the tick sequence
     * @param note
     */
    private void addToTickSequence(Note note) {
        // add note to noteSequence
        int i = this.tickSequence.size() - 1;
        for (; i >= 0; --i) {
            Note n = this.tickSequence.get(i);
            double date = n.getDate();
            if (date <= note.getDate())
                break;
        }
        this.tickSequence.add(i + 1, note);               // insert note also to the sequence
    }

    /**
     * add the note to the initial sequence
     * @param note
     */
    private void addToInitialSequence(Note note) {
        int i = this.initialSequence.size() - 1;
        for (; i >= 0; --i) {
            double date = this.initialSequence.get(i).getInitialMillisecondsDate();
            if (date <= note.getInitialMillisecondsDate())
                break;
        }
        this.initialSequence.add(i + 1, note);               // insert note also to the sequence
    }

    /**
     * remove a note from the part
     * @param id
     * @return
     */
    public Note remove(String id) {
        Note out = this.notes.remove(id);
        if (out != null) {
            this.noteSequence.remove(out);
            this.initialSequence.remove(out);
            if (this.lastNoteSounding == out)
                this.lastNoteSounding = null;
        }
        return out;
    }

    /**
     * remove the specified note from the part
     * @param note
     * @return
     */
    public Note remove(Note note) {
        return this.remove(note.getId());
    }

    /**
     * does this part have any notes?
     * @return
     */
    public boolean isEmpty() {
        return this.notes.isEmpty();
    }

    /**
     * get the part's number
     * @return
     */
    public int getNumber() {
        return this.number;
    }

    /**
     * access a note by id
     * @param id
     * @return
     */
    public Note getNote(String id) {
        return this.notes.get(id);
    }

    /**
     * get the sequence of all notes
     * @return
     */
    public ArrayList<Note> getNoteSequence() {
        return this.noteSequence;
    }

    /**
     * get the note sequence according to their notation, i.e. in order of their tick date
     * @return
     */
    public ArrayList<Note> getNoteSequenceInTicks() {
        return this.tickSequence;
    }

    /**
     * checks if the part contains the given note
     * @param note
     * @return
     */
    public boolean contains(Note note) {
        return this.notes.containsValue(note);
    }

    /**
     * find the first note at or after milliseconds and return its index in noteSequence
     * @param milliseconds
     * @return
     */
    public int getNoteIndexAtAfter(double milliseconds) {
        if (this.isEmpty() || (this.noteSequence.get(this.noteSequence.size()-1).getMillisecondsDate() < milliseconds)) // if the part is empty or all its notes are before the specified date
            return -1;                                                                                                  // done

        if (this.noteSequence.get(0).getMillisecondsDate() >= milliseconds)     // if the first note is already at or after the date
            return 0;                                                           // return 0

        // binary search
        int first = 0;
        int last = this.noteSequence.size() - 1;
        int mid = last / 2;
        while (first <= last) {
            if (this.noteSequence.get(mid).getMillisecondsDate() >= milliseconds)
                last = mid - 1;
            else if (this.noteSequence.get(mid + 1).getMillisecondsDate() >= milliseconds)
                return mid + 1;
            else
                first = mid + 1;
            mid = (first + last) / 2;
        }
        return -1;
    }

    /**
     * this method retrieves the index of the last note before or at the specified milliseconds date
     * @param milliseconds
     * @return the index or -1 if there is no note before or at the milliseconds date
     */
    private int getNoteIndexBeforeAtMilliseconds(double milliseconds) {
        if (this.isEmpty() || this.noteSequence.get(0).getMillisecondsDate() > milliseconds)            // if no notes available or all note are after the requested date
            return -1;

        int last = this.noteSequence.size() - 1;
        if (this.noteSequence.get(last).getMillisecondsDate() <= milliseconds)  // if the last note in the sequence is already before or at the requested date
            return last;                                                        // return its index

        // binary search
        int first = 0;
        int mid = last / 2;
        while (first <= last) {
            if (this.noteSequence.get(mid).getMillisecondsDate() > milliseconds)
                last = mid - 1;
            else if (this.noteSequence.get(mid + 1).getMillisecondsDate() > milliseconds)
                return mid;
            else
                first = mid + 1;
            mid = (first + last) / 2;
        }
        return -1;
    }

    /**
     * this method retrieves the index of the last note before or at the specified tick date
     * @param ticks
     * @return the index or -1 if there is no note before or at the tick date
     */
    private int getNoteIndexBeforeAtTicks(double ticks) {
        if (this.isEmpty() || this.tickSequence.get(0).getDate() > ticks)   // if no notes available or all note are after the requested date
            return -1;

        int last = this.tickSequence.size() - 1;
        if (this.tickSequence.get(last).getDate() <= ticks)                 // if the last note in the sequence is already before or at the requested date
            return last;                                                    // return its index

        // binary search
        int first = 0;
        int mid = last / 2;
        while (first <= last) {
            if (this.tickSequence.get(mid).getDate() > ticks)
                last = mid - 1;
            else if (this.tickSequence.get(mid + 1).getDate() > ticks)
                return mid;
            else
                first = mid + 1;
            mid = (first + last) / 2;
        }
        return -1;
    }

    /**
     * Given a milliseconds date, compute the non-performed tick date from it according to this alignment. This is only an approximation.
     * @param milliseconds
     * @return
     */
    public double getCorrespondingTickDate(double milliseconds) {
        int beforeAt = this.getNoteIndexBeforeAtMilliseconds(milliseconds);
        int after = beforeAt + 1;

        double beforeAtMillis;
        double beforeAtTicks;

        if (beforeAt < 0) {
            beforeAtMillis = 0.0;
            beforeAtTicks = 0.0;
        } else {
            Note note = this.noteSequence.get(beforeAt);
            beforeAtMillis = note.getMillisecondsDate();
            beforeAtTicks = note.getDate();
        }

        if (beforeAtMillis == milliseconds)
            return beforeAtTicks;

        double afterMillis;
        double afterTicks;

        if (after >= this.noteSequence.size()) {
            afterMillis = this.getMillisecondsLength();
            afterTicks = this.getLastNoteSounding().getDate() + this.getLastNoteSounding().getDuration();
        } else {
            Note note = this.noteSequence.get(after);
            afterMillis = note.getMillisecondsDate();
            afterTicks = note.getDate();
        }

        // compute the relative position of milliseconds between beforeAtMillis and afterMillis and get the corresponding initial position from this; since timing is usually more complex, this is only an approximation!
        double relativePosition = (milliseconds - beforeAtMillis) / (afterMillis - beforeAtMillis);
        return ((afterTicks - beforeAtTicks) * relativePosition) + beforeAtTicks;
    }

    /**
     * Given a tick date, compute the respective milliseconds date from it according to this alignment. This is only an approximation.
     * @param ticks
     * @return
     */
    public double getCorrespondingMillisecondsDate(double ticks) {
        int beforeAt = this.getNoteIndexBeforeAtTicks(ticks);
        int after = beforeAt + 1;

        double beforeAtMillis;
        double beforeAtTicks;

        if (beforeAt < 0) {
            beforeAtMillis = 0.0;
            beforeAtTicks = 0.0;
        } else {
            Note note = this.tickSequence.get(beforeAt);
            beforeAtMillis = note.getMillisecondsDate();
            beforeAtTicks = note.getDate();
        }

        if (beforeAtMillis == ticks)
            return beforeAtTicks;

        double afterMillis;
        double afterTicks;

        if (after >= this.tickSequence.size()) {
            afterMillis = this.getMillisecondsLength();
            afterTicks = this.getLastNoteSounding().getDate() + this.getLastNoteSounding().getDuration();
        } else {
            Note note = this.tickSequence.get(after);
            afterMillis = note.getMillisecondsDate();
            afterTicks = note.getDate();
        }

        // compute the relative position of ticks between beforeAtTicks and afterTicks and get the corresponding initial position from this; since timing is usually more complex, this is only an approximation!
        double relativePosition = (ticks - beforeAtTicks) / (afterTicks - beforeAtTicks);
        return ((afterMillis - beforeAtMillis) * relativePosition) + beforeAtMillis;
    }

    /**
     * retrieve the fixed notes at and around the specified milliseconds date
     * @param milliseconds
     * @return array of 3 notes {before, at, after}; may contain null values!
     */
    public Note[] getFixedNoteBeforeAtAfter(double milliseconds) {
        Note before = null;
        Note at = null;
        Note after = null;

        for (Note note : this.noteSequence) {
            if (!note.isFixed())
                continue;

            if (note.getMillisecondsDate() > milliseconds) {
                after = note;
                break;
            }

            if (note.getMillisecondsDate() == milliseconds)
                at = note;
            else    // if (note.getMillisecondsDate() < milliseconds)
                before = note;
        }

        return new Note[] {before, at, after};
    }

    /**
     * collect all fixed notes from this part's note sequence and return them in timely order
     * @param initialOrder get the fixed notes in their initial or current order
     * @return
     */
    public ArrayList<Note> getAllFixedNotes(boolean initialOrder) {
        ArrayList<Note> fixed = new ArrayList<>();
        for (Note note : ((initialOrder) ? this.initialSequence : this.noteSequence)) {
            if (note.isFixed())
                fixed.add(note);
        }
        return fixed;
    }

    /**
     * find the note with the latest milliseconds.date.end
     * @return
     */
    public Note getLastNoteSounding() {
        if (this.isEmpty())
            return null;

        if (this.lastNoteSounding == null) {
            for (int i = this.noteSequence.size() - 1; i >= 0; --i) {
                Note note = this.noteSequence.get(i);
                if ((this.lastNoteSounding == null) || (note.getMillisecondsDateEnd() > this.lastNoteSounding.getMillisecondsDateEnd())) {
                    this.lastNoteSounding = note;
                }
            }
        }
        return this.lastNoteSounding;
    }

    /**
     * compute the length of the part in milliseconds
     * @return
     */
    public double getMillisecondsLength() {
        return (this.lastNoteSounding == null) ? 0.0 : this.lastNoteSounding.getMillisecondsDateEnd();
    }

    /**
     * resets the values of each note to their initial value
     * the invoking application should also run recomputePianoRoll() to update the piano roll image
     */
    protected void reset() {
        this.noteSequence.clear();

        for (Note note : this.initialSequence) {
            note.reset();
            this.noteSequence.add(note);
        }

        this.lastNoteSounding = null;
    }

    /**
     * Scales all notes' milliseconds dates by the specified factor.
     * This transformation is also applied to fixed notes!
     * @param factor
     */
    protected void scaleOverallTiming(double factor) {
        for (Note note : this.initialSequence) {
            note.setMillisecondsDate(note.getInitialMillisecondsDate() * factor);
            note.setMillisecondsDateEnd(note.getInitialMillisecondsDateEnd() * factor);
        }
    }

    /**
     * Sets a new milliseconds date and end date of the note and places it in the note sequence accordingly.
     * The non-fixed notes are not repositioned by this method! this has to be done subsequently.
     * @param note
     * @param toMilliseconds
     */
    protected void reposition(@NotNull Note note, double toMilliseconds) {
        note.setMillisecondsDateEnd(note.getInitialMillisecondsDateEnd() - note.getInitialMillisecondsDate() + toMilliseconds);
        note.setMillisecondsDate(toMilliseconds);
        this.noteSequence.remove(note);
        this.addToSequence(note);
    }

    /**
     * places all non-fixed notes according to the positions of the fixed notes
     * @param timingTransformation the transformation data; each element provides the following values {startDate, endDate, toStartDate, toEndDate}, all in milliseconds
     */
    protected void transformTiming(ArrayList<double[]> timingTransformation) {
        for (double[] segment : timingTransformation)
            this.transformTiming(segment[0], segment[1], segment[2], segment[3]);

        this.noteSequence.clear();
        for (Note note : this.initialSequence)
            this.addToSequence(note);
    }

    /**
     * helper method for transformTiming(fixedNotes);
     * transforms all note onsets in [startDate; endDate) to [toStartDate; toEndDate)
     * and offsets in (startDate; endDate] to (toStartDate; toEndDate]
     * @param startDate
     * @param endDate
     * @param toStartDate
     * @param toEndDate
     */
    private void transformTiming(double startDate, double endDate, double toStartDate, double toEndDate) {
        boolean shiftDontScale = (endDate == startDate) || (toStartDate >= toEndDate);
        double scaleFactor = shiftDontScale ? 1.0 : (toEndDate - toStartDate) / (endDate - startDate);  // if the section has 0 length, we do not scale anything

        ArrayList<Note> fixed = new ArrayList<>();
        int i = 0;
        for (; i < this.initialSequence.size(); ++i) {
            Note note = this.initialSequence.get(i);

            if (note.isFixed()) {                                   // if the note is fixed
                fixed.add(note);                                    // store for further treatment
                continue;
            }

            if (note.getInitialMillisecondsDate() >= endDate)       // we have to check only the notes that start before endDate
                break;

            // transform the note's onset
            double iDate = note.getInitialMillisecondsDate();
            if (iDate >= startDate)
                note.setMillisecondsDate(shiftDontScale ? toStartDate : ((iDate - startDate) * scaleFactor) + toStartDate);

            // transform the note's offset
            iDate = note.getInitialMillisecondsDateEnd();
            if ((iDate > startDate) && (iDate <= endDate))
                note.setMillisecondsDateEnd(shiftDontScale ? endDate - startDate + toStartDate : ((iDate - startDate) * scaleFactor) + toStartDate);
        }

        // now treat the fixed notes
        for (; i < this.initialSequence.size(); ++i) {
            Note note = this.initialSequence.get(i);
            if (note.isFixed())
                fixed.add(note);                                    // store for further treatment
        }
        for (Note f : fixed) {
            if ((f.getMillisecondsDate() >= toEndDate)              // if it is behind the target end date
                    || (f.getInitialMillisecondsDate() > startDate))// or it was behind the initial start date
                continue;                                           // we leave it unaltered

            // change the milliseconds offset date of the fixed note; its onset date is fixed
            double iDate = f.getInitialMillisecondsDateEnd();
            if ((iDate > startDate) && (iDate <= endDate))
                f.setMillisecondsDateEnd(shiftDontScale ? endDate - startDate + toStartDate : ((iDate - startDate) * scaleFactor) + toStartDate);
        }
    }

    /**
     * creates a piano roll visualization of this part in the interval [from, to] (inclusive).
     * @param fromMilliseconds
     * @param toMilliseconds
     * @param imgWidth
     * @param imgHeight should correspond with the highest pitch class to be displayed, because one row of pixels is one diatonic pitch class, starting with MIDI's pitch 0; for MIDI-compliance use 128
     * @return
     */
    public PianoRoll getPianoRoll(double fromMilliseconds, double toMilliseconds, int imgWidth, int imgHeight) {
        if (fromMilliseconds == toMilliseconds)
            return null;

        if (fromMilliseconds > toMilliseconds) {
            double temp = fromMilliseconds;
            fromMilliseconds = toMilliseconds;
            toMilliseconds = temp;
        }

        // do not compute a new piano roll if the metrics did not change
        if ((this.pianoRoll != null) && this.pianoRoll.sameMetrics(fromMilliseconds, toMilliseconds, imgWidth, imgHeight))
            return this.pianoRoll;

        double scaleToHorizontalPixels = imgWidth / (toMilliseconds - fromMilliseconds);    // we need this value later quite often

        this.pianoRoll = new PianoRoll(fromMilliseconds, toMilliseconds, imgWidth, imgHeight);

        // fill the piano roll image with content
        for (Note note : this.noteSequence) {
            if ((note.getMillisecondsDate() >= toMilliseconds) || (note.getMillisecondsDateEnd() < fromMilliseconds))   // if note is beyond the interval to be rendered
                continue;                                                                                               // continue with the next note

            // compute the y coordinate of the note (one row of pixels = one pitch class)
            int y = (int) Math.round(note.getPitch());
            if ((y < 0) || y >= imgHeight)       // if the pitch is outside the MIDI pitch range
                continue;                        // we do not paint the note

            // compute the x coordinate where the note starts
            double millis = note.getMillisecondsDate() - fromMilliseconds;
            int xStart = (millis <= 0.0) ? 0 : (int) Math.round(millis * scaleToHorizontalPixels);

            // compute the x coordinate where the note ends
            millis = note.getMillisecondsDateEnd() - fromMilliseconds;
            int xEnd = (millis >= toMilliseconds) ? imgWidth : Math.min(imgWidth, (int) Math.round(millis * scaleToHorizontalPixels));

            this.pianoRoll.add(xStart, xEnd, y, note);
        }

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

        double fromMilliseconds = this.pianoRoll.getFromMilliseconds();
        double toMilliseconds = this.pianoRoll.getToMilliseconds();
        int imgWidth = this.pianoRoll.getWidth();
        int imgHeight = this.pianoRoll.getHeight();

        this.pianoRoll = null;
        return this.getPianoRoll(fromMilliseconds, toMilliseconds, imgWidth, imgHeight);
    }

    /**
     * access the original MSM element
     * @return
     */
    public Element getXml() {
        return this.xml;
    }

    /**
     * generate a project data XML element
     * @return
     */
    protected Element toXml() {
        Element out = Helper.cloneElement(this.xml);    // make a flat copy of the part element

        // rename the xml:id attribute, in case there is one, to ref
        Attribute ref = out.getAttribute("id", "http://www.w3.org/XML/1998/namespace");
        if (ref != null) {
            out.addAttribute(new Attribute("id", ref.getValue()));
            out.removeAttribute(ref);
        }

        // add all note entries
        for (Note note : this.noteSequence)
                out.appendChild(note.toXml());
//        for (Map.Entry<String, Note> entry : this.entrySet())
//            out.appendChild(entry.getValue().toXml());

        return out;
    }

    @Override
    public String toString() {
        return "Part " + this.number + " " + Helper.getAttributeValue("name", this.xml);
    }
}
