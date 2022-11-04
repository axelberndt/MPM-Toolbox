package mpmToolbox.projectData.alignment;

import com.sun.media.sound.InvalidDataException;
import meico.mei.Helper;
import nu.xom.Attribute;
import nu.xom.Element;
import nu.xom.ParentNode;

/**
 * An MSM note element plus some performance data such as milliseconds.date, milliseconds.date.end and velocity.
 * @author Axel Berndt
 */
public class Note {
    private final Element xml;                          // a reference to the original MSM element

    private final double tickDate;                      // the tick date of the note, remains unaltered
    private final double initialMillisecondsDate;       // the initial date of the note will remain unaltered throughout any other transformations
    private final double initialMillisecondsDateEnd;    // the initial end date of the note will remain unaltered throughout any other transformations

    private double millisecondsDate;                    // the date of the note, subject to alteration
    private double millisecondsDateEnd;                 // the end date of the note, subject to alteration

    private double velocity;
    private double pitch;
    private boolean fixed = false;                      // signals that the values of this note should not be scaled when another note is edited; this note is fixed

    /**
     * constructor
     * @param xml a note element from an Msm object; can also be a performed Msm with the additional performance data
     * @throws InvalidDataException
     * @throws NumberFormatException
     */
    protected Note(Element xml) throws InvalidDataException, NumberFormatException {
        // parse the note's tick date
        Attribute tickDate = Helper.getAttribute("date", xml);
        if (tickDate == null)
            throw new InvalidDataException("Invalid MSM element " + xml.toXML() + "; missing attribute date.");
        this.tickDate = Double.parseDouble(tickDate.getValue());

        // parse the note's onset date
        Attribute millisDate = Helper.getAttribute("milliseconds.date", xml);
        if (millisDate == null)
            millisDate = tickDate;
        this.initialMillisecondsDate = Double.parseDouble(millisDate.getValue());
        this.millisecondsDate = this.initialMillisecondsDate;

        // parse the note's offset date
        Attribute millisEnd = Helper.getAttribute("milliseconds.date.end", xml);
        if (millisEnd == null) {
            millisEnd = Helper.getAttribute("duration", xml);
            if (millisEnd == null)
                throw new InvalidDataException("Invalid MSM element " + xml.toXML() + "; missing attribute duration.");
            this.initialMillisecondsDateEnd = this.millisecondsDate + Double.parseDouble(millisEnd.getValue());
        }
        else {
            this.initialMillisecondsDateEnd = Double.parseDouble(millisEnd.getValue());
        }
        this.millisecondsDateEnd = this.initialMillisecondsDateEnd;

        // parse the note's MIDI pitch
        Attribute ptch = Helper.getAttribute("midi.pitch", xml);
        if (ptch == null)
            throw new InvalidDataException("Invalid MSM element " + xml.toXML() + "; missing attribute midi.pitch.");
        this.pitch = Double.parseDouble(ptch.getValue());

        // parse the note's velocity
        Attribute vel = Helper.getAttribute("velocity", xml);
        if (vel == null)                // if no velocity given
            this.velocity = 100.0;      // set default velocity
        else
            this.velocity = Double.parseDouble(vel.getValue());

        // keep the reference to the original MSM note element
        this.xml = xml;
    }

    /**
     * This method is only used when loading an MPM Toolbox project.
     * It sets the data from the project file in this note.
     * @param alignmentData
     */
    protected void syncWith(Element alignmentData) {
        Attribute a = Helper.getAttribute("milliseconds.date", alignmentData);
        if (a != null)
            this.setMillisecondsDate(Double.parseDouble(a.getValue()));

        a = Helper.getAttribute("milliseconds.date.end", alignmentData);
        if (a != null)
            this.setMillisecondsDateEnd(Double.parseDouble(a.getValue()));

        a = Helper.getAttribute("velocity", alignmentData);
        if (a != null)
            this.setVelocity(Double.parseDouble(a.getValue()));

        a = Helper.getAttribute("midi.pitch", alignmentData);
        if (a != null)
            this.setPitch(Double.parseDouble(a.getValue()));

        a = Helper.getAttribute("fixed", alignmentData);
        if (a != null)
            this.setFixed(Boolean.parseBoolean(a.getValue()));
    }

    /**
     * undo all changes and set the note's initial values
     */
    public void reset() {
        this.millisecondsDate = this.initialMillisecondsDate;
        this.millisecondsDateEnd = this.initialMillisecondsDateEnd;
        this.fixed = false;
    }

    /**
     * get the xml:id of the note
     * @return
     */
    public String getId() {
        return this.xml.getAttributeValue("id", "http://www.w3.org/XML/1998/namespace");
    }

    /**
     * read the initial milliseconds date of the note
     * @return
     */
    public double getInitialMillisecondsDate() {
        return this.initialMillisecondsDate;
    }

    /**
     * read the initial milliseconds end date of the note
     * @return
     */
    public double getInitialMillisecondsDateEnd() {
        return this.initialMillisecondsDateEnd;
    }

    /**
     * read the milliseconds date of the note
     * @return
     */
    public double getMillisecondsDate() {
        return this.millisecondsDate;
    }

    /**
     * setter for the note's milliseconds.date
     * @param date
     */
    public void setMillisecondsDate(double date) {
        this.millisecondsDate = date;
    }

    /**
     * read the milliseconds end date of the note
     * @return
     */
    public double getMillisecondsDateEnd() {
        return this.millisecondsDateEnd;
    }

    /**
     * setter for the note's milliseconds.date.end
     * @param date
     */
    public void setMillisecondsDateEnd(double date) {
        this.millisecondsDateEnd = date;
    }

    /**
     * read the tick date of the note
     * @return
     */
    public double getDate() {
        return this.tickDate;
    }

    /**
     * read the tick duration of the note
     * @return
     */
    public Double getDuration() {
        Attribute dur = Helper.getAttribute("duration", xml);
        if (dur == null)
            return null;
        return Double.parseDouble(dur.getValue());
    }

    /**
     * retrieve the MSM part that this note belongs to
     * @return
     */
    public Element getMsmPart() {
        for (ParentNode parent = this.getXml().getParent().getParent().getParent(); parent != null; parent = parent.getParent()) {
            Element p = (Element) parent;
            if (p.getLocalName().equals("part"))
                return p;
        }
        return null;
    }

    /**
     * read the velocity of the note
     * @return
     */
    public double getVelocity() {
        return this.velocity;
    }

    /**
     * setter for the note's velocity
     * @param velocity
     */
    public void setVelocity(double velocity) {
        this.velocity = velocity;
    }

    /**
     * read the pitch of the note
     * @return
     */
    public double getPitch() {
        return this.pitch;
    }

    /**
     * setter for the note's pitch
     * @param pitch
     */
    public void setPitch(double pitch) {
        this.pitch = pitch;
    }

    /**
     * is the note fixed (it keeps it values even if surrounding notes change) or floating (adapts to changes of the surrounding notes' values)
     * @return
     */
    public boolean isFixed() {
        return this.fixed;
    }

    /**
     * setter to fix the note's values
     * @param fixed
     */
    public void setFixed(boolean fixed) {
        this.fixed = fixed;
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
        Element out = new Element("note");
        out.addAttribute(new Attribute("ref", this.xml.getAttributeValue("id", "http://www.w3.org/XML/1998/namespace")));
        out.addAttribute(new Attribute("midi.pitch", Double.toString(this.pitch)));
        out.addAttribute(new Attribute("milliseconds.date", Double.toString(this.millisecondsDate)));
        out.addAttribute(new Attribute("milliseconds.date.end", Double.toString(this.millisecondsDateEnd)));
        out.addAttribute(new Attribute("velocity", Double.toString(this.velocity)));
        out.addAttribute(new Attribute("fixed", Boolean.toString(this.fixed)));
        return out;
    }

    /**
     * print the note data
     * @return
     */
    @Override
    public String toString() {
        return "Note: " + this.getXml().toXML() + "\n      pitch " + this.getPitch() + ", velocity " + this.getVelocity() + ", onset (" + this.getInitialMillisecondsDate() + " ms -> " + this.getMillisecondsDate() + " ms), offset (" + this.getInitialMillisecondsDateEnd() + " ms -> " + this.getMillisecondsDateEnd() + " ms)";
    }
}
