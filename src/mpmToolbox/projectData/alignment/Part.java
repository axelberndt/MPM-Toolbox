package mpmToolbox.projectData.alignment;

import com.sun.media.sound.InvalidDataException;
import meico.mei.Helper;
import meico.supplementary.KeyValue;
import nu.xom.Attribute;
import nu.xom.Element;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * An instance of this is a Hashmap of all MSM notes, accessed via their XML ID.
 * The notes are augmented with performance information.
 * @author Axel Berndt
 */
public class Part extends HashMap<String, Note> {
    private final Element xml;  // a reference to the original MSM part element
    private final ArrayList<KeyValue<Double, Note>> sequence = new ArrayList<>();

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
                Note note;
                try {
                    note = new Note(e);
                } catch (InvalidDataException | NumberFormatException exception) {
                    exception.printStackTrace();
                    continue;
                }
                String id = note.getId();
                this.put(id, note);
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
        for (Map.Entry<String, Note> entry : this.entrySet())
            out.appendChild(entry.getValue().toXml());

        return out;
    }
}
