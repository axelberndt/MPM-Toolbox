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
     * Scale the complete music to the specified length, i.e. the last milliseconds.date.end.
     * This transformation is also applied to fixed notes!
     * @param milliseconds
     */
    public void scaleTiming(double milliseconds) {
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
            part.scaleTiming(factor);
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
