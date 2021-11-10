package mpmToolbox.projectData.alignment;

import com.sun.media.sound.InvalidDataException;
import meico.mei.Helper;
import nu.xom.Attribute;
import nu.xom.Element;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

/**
 * An instance of this is a Hashmap of all MSM notes, accessed via their XML ID.
 * The notes are augmented with performance information.
 * @author Axel Berndt
 */
public class Part extends HashMap<String, Note> {
    private final Element xml;          // a reference to the original MSM part element
    private ArrayList<Note> sequence = new ArrayList<>();

    /**
     * constructor
     * @param msmPart
     */
    protected Part(Element msmPart) throws InvalidDataException {
        super();

        this.xml = msmPart;

        // get the dated environment
        Element dated = msmPart.getFirstChildElement("dated");
        if (dated == null)
            throw new InvalidDataException("Invalid MSM part element " + Helper.cloneElement(msmPart).toXML() + "; missing child element dated.");

        // parse the score and fill the HashMap with (ID, Note) tuples
        Element score = dated.getFirstChildElement("score");
        if (score != null) {
            for (Element e : score.getChildElements("note")) {
                try {
                    this.put(new Note(e));
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
            Note note = this.get(ref);

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
    public Note put(Note note) {
        Note out = super.put(note.getId(), note);     // this will add the id-note pair to the hashmap and overwrite any other note behind the same id; out will hold that previous note or null

        if (out != null)                    // if there was a previous note that we replaced with the above line
            this.sequence.remove(out);      // remove it also from the sequence

        // add the note at the right position to the sequence
        int i = this.sequence.size()-1;
        for (; i >= 0; --i) {
            double date = this.sequence.get(i).getMillisecondsDate();
            if (date <= note.getMillisecondsDate())
                break;
        }
        this.sequence.add(i+1, note);            // insert note also to the sequence

        return out;
    }

    @Override
    public Note put(String id, Note note) {
        throw new UnsupportedOperationException("Operation put(String id, Note note) is not supported in class Part. Use put(Note note) instead.");
    }

    /**
     * add a number of key value pairs
     * @param list
     */
    @Override
    public void putAll(Map<? extends String,? extends Note> list) {
        for (Entry<? extends String, ? extends Note> entry : list.entrySet()) {
            this.put(entry.getValue());
        }
    }

    /**
     * If the specified key is not already associated with a value (or is mapped to null) associates it with the given value and returns null, else returns the current value.
     * @param note
     * @return
     */
    public Note putIfAbsent(Note note) {
        if (this.get(note.getId()) != null)
            return this.put(note);
        return null;
    }

    @Override
    public Note putIfAbsent(String id, Note note) {
        throw new UnsupportedOperationException("Operation putIfAbsent(String id, Note note) is not supported in class Part. Use putIfAbsent(Note note) instead.");
    }

    /**
     * remove a note from the part
     * @param id
     * @return
     */
    public Note remove(String id) {
        Note out = super.remove(id);
        if (out != null)
            this.sequence.remove(out);
        return out;
    }

    /**
     * remove the specified entry only if the given key is associated with the specified value
     * @param id
     * @param note
     * @return
     */
    public boolean remove(String id, Note note) {
        if (super.remove(id, note)) {
            this.sequence.remove(note);
            return true;
        }
        return false;
    }

    /**
     * remove the specified note from the part
     * @param note
     * @return
     */
    public Note remove(Note note) {
        return this.remove(note.getId());
    }

    @Override
    public Note remove(Object o) {
        throw new UnsupportedOperationException("Operation remove(Object o) is not supported in class Part.");
    }

    @Override
    public boolean remove(Object o, Object o1) {
        throw new UnsupportedOperationException("Operation remove(Object o, Object o1) is not supported in class Part.");
    }

    /**
     * Replaces the entry for the specified key only if currently mapped to the specified value.
     * @param oldNote
     * @param newNote
     * @return
     */
    public boolean replace(Note oldNote, Note newNote) {
        boolean didReplace = this.get(oldNote.getId()) != null;
        if (didReplace) {
            this.remove(oldNote.getId());
            this.put(newNote);
        }
        return didReplace;
    }

    @Override
    public boolean replace(String id, Note oldNote, Note newNote) {
        throw new UnsupportedOperationException("Operation replace(String id, Note oldNote, Note newNote) is not supported in class Part. Use replace(Note oldNote, Note newNote) instead.");
    }

    /**
     * Replaces the entry for the specified key only if it is currently mapped to some value.
     * @param id
     * @param note
     * @return
     */
    @Override
    public Note replace(String id, Note note) {
        if (this.get(id) != null)
            return this.put(note);
        return null;
    }

    @Override
    public Note merge(String s, Note note, BiFunction<? super Note, ? super Note, ? extends Note> biFunction) {
        throw new UnsupportedOperationException("Operation merge(String s, Note note, BiFunction<? super Note, ? super Note, ? extends Note> biFunction) is not supported in class Part.");
    }

    @Override
    public void replaceAll(BiFunction<? super String, ? super Note, ? extends Note> biFunction) {
        throw new UnsupportedOperationException("Operation replaceAll(BiFunction<? super String, ? super Note, ? extends Note> biFunction) is not supported in class Part.");
    }

    /**
     * create a clone of this
     * @return
     */
    @Override
    public Object clone() {
        Part clone;
        try {
            clone = new Part(this.getXml());
        } catch (InvalidDataException e) {
            e.printStackTrace();
            return null;
        }

        clone.sequence = (ArrayList<Note>) this.sequence.clone();

        return clone;
    }

    /**
     * creates a piano roll visualization of this part in the interval [from, to] (inclusive).
     * @param fromMilliseconds
     * @param toMilliseconds
     * @param imgWidth
     * @param imgHeight
     * @return
     */
    protected BufferedImage getPianoRoll(double fromMilliseconds, double toMilliseconds, int imgWidth, int imgHeight) {
        if (fromMilliseconds > toMilliseconds) {
            double temp = fromMilliseconds;
            fromMilliseconds = toMilliseconds;
            toMilliseconds = temp;
        }

        BufferedImage out = new BufferedImage(imgWidth, imgHeight, BufferedImage.TYPE_INT_ARGB);
        // TODO: better make my own BufferedImage derivative that does the pixel-to-milliseconds mapping
        // TODO: fill it with content

        return out;
    }

    /**
     * access the original MSM element
     * @return
     */
    protected Element getXml() {
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
        for (Note note : this.sequence)
                out.appendChild(note.toXml());
//        for (Map.Entry<String, Note> entry : this.entrySet())
//            out.appendChild(entry.getValue().toXml());

        return out;
    }
}
