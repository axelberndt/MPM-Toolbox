package mpmToolbox.projectData.alignment;

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
    private final Element xml;                                      // a reference to the original MSM part element
    private final int number;
    private final HashMap<String, Note> notes = new HashMap<>();
    private final ArrayList<Note> noteSequence = new ArrayList<>(); // the notes in sequential order
    private PianoRoll pianoRoll = null;

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

        if (out != null)                                // if there was a previous note that we replaced with the above line
            this.noteSequence.remove(out);              // remove it also from the sequence

        // add the note at the right position to the sequence
        int i = this.noteSequence.size()-1;
        for (; i >= 0; --i) {
            double date = this.noteSequence.get(i).getMillisecondsDate();
            if (date <= note.getMillisecondsDate())
                break;
        }
        this.noteSequence.add(i+1, note);               // insert note also to the sequence

        return out;
    }

    /**
     * remove a note from the part
     * @param id
     * @return
     */
    public Note remove(String id) {
        Note out = this.notes.remove(id);
        if (out != null)
            this.noteSequence.remove(out);
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
     * find the note with the latest milliseconds.date.end
     * @return
     */
    public Note getLastNoteSounding() {
        if (this.isEmpty())
            return null;

        Note out = null;

        for (int i = this.noteSequence.size() - 1; i >= 0; --i) {
            Note note = this.noteSequence.get(i);
            if ((out == null) || (note.getMillisecondsDateEnd() > out.getMillisecondsDateEnd())) {
                out = note;
            }
        }

        return out;
    }

    /**
     * Scales all notes' milliseconds dates by the specified factor.
     * This transformation is also applied to fixed notes!
     * @param factor
     */
    public void scaleTiming(double factor) {
        for (Note note : this.noteSequence) {
            note.setMillisecondsDate(note.getMillisecondsDate() * factor);
            note.setMillisecondsDateEnd(note.getMillisecondsDateEnd() * factor);
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
    protected PianoRoll getPianoRoll(double fromMilliseconds, double toMilliseconds, int imgWidth, int imgHeight) {
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
