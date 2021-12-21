package mpmToolbox.projectData.alignment;

import com.alee.api.annotations.NotNull;
import com.sun.media.sound.InvalidDataException;
import meico.mei.Helper;
import meico.msm.Msm;
import nu.xom.Attribute;
import nu.xom.Element;

import java.util.ArrayList;

/**
 * This class links all MSM note elements with expression data (milliseconds.date, milliseconds.date.end, velocity).
 * It further provides a routine to render a visual representation of the piano roll.
 * @author Axel Berndt
 */
public class Alignment {
    private final ArrayList<mpmToolbox.projectData.alignment.Part> parts = new ArrayList<>();
    private PianoRoll pianoRoll = null;
    private final Msm msm;
    private ArrayList<double[]> timingTransformation = new ArrayList<>();   // each element provides the following values {startDate, endDate, toStartDate, toEndDate}, all in milliseconds

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
    public boolean add(@NotNull Part part) {
        int number = part.getNumber();
        int index = 0;
        for (int i = this.parts.size() - 1; i >= 0; --i) {
            if (this.parts.get(i).getNumber() <= number) {
                index = i + 1;
                break;
            }
        }
        this.parts.add(index, part);
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
        // create an ordered list of all fixed notes
        ArrayList<Note> fixedNotes = new ArrayList<>();
        for (Part part : this.getParts()) {
            ArrayList<Note> f = part.getAllFixedNotes(false);   // get the part's list of fixed notes in the order of the current timing

            if (fixedNotes.isEmpty()) {                         // if the fixedNotes list is empty
                fixedNotes.addAll(f);                           // we can simply add the part's list, it is already ordered
                continue;                                       // go on with the next part
            }

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
        Note stopper = null;                                    // the section is scaled into [beginner.millisecondsDate; this note's millisecondsDate)
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

        // after the above loop there is the last beginner left for which we have to add an entry in the timingTransformation list
        assert beginner != null;
        this.timingTransformation.add(new double[]{beginner.getInitialMillisecondsDate(), Double.MAX_VALUE, beginner.getMillisecondsDate(), Double.MAX_VALUE});

        this.renderTiming();                                    // compute the new milliseconds timing
    }

    /**
     * apply the current timing transformation to all parts, so their note get new millisecondsDates and millisecondsDateEnds
     */
    private void renderTiming() {
        for (Part part : this.getParts())   // apply the timing transform to each part
            part.transformTiming(this.timingTransformation);
    }

    /**
     * Sets a new milliseconds date and end date of the note and places it in the note sequence accordingly.
     * The note is made fixed. The non-fixed notes are not repositioned by this method! This has to be done subsequently.
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
    }

    /**
     * resets the values of each note in each part to their initial values;
     * the invoking application should also run recomputePianoRoll() to update the piano roll image
     */
    public void reset() {
        for (Part part : this.getParts()) {
            part.reset();
        }
    }

    /**
     * Scale the complete music to the specified length, i.e. the last milliseconds.date.end.
     * This transformation is also applied to fixed notes!
     * @param milliseconds the milliseconds length to which the notes are scaled or null if no scaling is set
     */
    public void scaleOverallTiming(double milliseconds) {
        Note lastNoteSounding = null;

        for (Part part : this.parts) {
            Note note = part.getLastNoteSounding();
            if ((lastNoteSounding == null) || (note.getMillisecondsDateEnd() > lastNoteSounding.getMillisecondsDateEnd())) {
                lastNoteSounding = note;
            }
        }

        if (lastNoteSounding == null)
            return;

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

        for (Element partElt : expMsm.getParts()) {
            Part part = this.getPart(Integer.parseInt(Helper.getAttributeValue("number", partElt)));
            if (part == null)
                continue;

            Element score = partElt.getFirstChildElement("dated").getFirstChildElement("score");
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
