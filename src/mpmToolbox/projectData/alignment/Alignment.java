package mpmToolbox.projectData.alignment;

import com.sun.media.sound.InvalidDataException;
import meico.mei.Helper;
import meico.msm.Msm;
import nu.xom.Element;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * This class links all MSM note elements with expression data (milliseconds.date, milliseconds.date.end, velocity).
 * It further provides a routine to render a visual representation of the piano roll.
 * @author Axel Berndt
 */
public class Alignment extends HashMap<Element, mpmToolbox.projectData.alignment.Part> {
    private final Msm originalMsm;

    /**
     * constructor
     * @param msm
     */
    private Alignment(Msm msm) {
        super();

        this.originalMsm = msm;

        for (Element p : msm.getParts()) {
            Part part;
            try {
                part = new Part(p);
            } catch (InvalidDataException e) {
                e.printStackTrace();
                continue;
            }
            this.put(p, part);
        }
    }

    /**
     * constructor
     * @param msm the MSM to be aligned
     * @param alignmentData the alignment element from the project file or null if the audio is newly imported into the project
     */
    public Alignment(Msm msm, Element alignmentData) {
        this(msm);

        // edit the data just generated according to the input alignment data
        if (alignmentData != null) {
            Set<Entry<Element, Part>> entryset = this.entrySet();               // whenever we found a corresponding entry we can remove it from this set and, thus, accelerate the next search

            for (Element inputPart : alignmentData.getChildElements("part")) {  // for each alignment part in the input data
                String name = Helper.getAttributeValue("name", inputPart);
                String number = Helper.getAttributeValue("number", inputPart);
                String midiChannel = Helper.getAttributeValue("midi.channel", inputPart);
                String midiPort = Helper.getAttributeValue("midi.port", inputPart);
                String id = Helper.getAttributeValue("ref", inputPart);

                mpmToolbox.projectData.alignment.Part part = null;

                // find the corresponding initially generated alignment part
                for (Map.Entry<Element, mpmToolbox.projectData.alignment.Part> entry : entryset) {
                    mpmToolbox.projectData.alignment.Part p = entry.getValue();

                    if (Helper.getAttributeValue("name", p.getXml()).equals(name)
                            && Helper.getAttributeValue("number", p.getXml()).equals(number)
                            && Helper.getAttributeValue("ref", p.getXml()).equals(id)
                            && Helper.getAttributeValue("midi.channel", p.getXml()).equals(midiChannel)
                            && Helper.getAttributeValue("midi.port", p.getXml()).equals(midiPort)) {    // if the corresponding MSM part is found
                        part = p;   // get the alignment part where we will edit the alignment data according to the input alignment data
                        entryset.remove(entry);
                        break;
                    }
                }

                if (part == null)           // if no corresponding part was found
                    continue;               // we seem to have odd input data, so we do not import it into the project and it will be removed at the next save operation

                part.syncWith(inputPart);   // let the part synchronize with the input alignment data
            }
        }
    }

    /**
     * generate a project data XML element
     * @return
     */
    public Element toXml() {
        Element out = new Element("alignment");

        // get the XML of each part's alignment
        for (Map.Entry<Element, mpmToolbox.projectData.alignment.Part> entry : this.entrySet())
            out.appendChild(entry.getValue().toXml());

        return out;
    }
}
