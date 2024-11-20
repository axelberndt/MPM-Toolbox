package mpmToolbox.gui.msmTree;

import com.alee.api.annotations.NotNull;
import com.alee.api.ui.TextBridge;
import com.alee.extended.tree.WebExTree;
import com.alee.laf.menu.WebPopupMenu;
import com.alee.laf.tree.TreeNodeParameters;
import com.alee.laf.tree.UniqueNode;
import meico.mei.Helper;
import meico.midi.EventMaker;
import meico.midi.MidiPlayer;
import mpmToolbox.gui.msmEditingTools.MsmEditingTools;
import mpmToolbox.projectData.ProjectData;
import nu.xom.Attribute;
import nu.xom.Element;
import nu.xom.Node;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.Sequence;
import javax.sound.midi.Track;
import javax.swing.*;

/**
 * Instances of this class represent MSM data (Element, Attribute or Node) in a WebAsyncTree
 * @author Axel Berndt
 */
public class MsmTreeNode extends UniqueNode<MsmTreeNode, Node> implements TextBridge<TreeNodeParameters<MsmTreeNode, WebExTree<MsmTreeNode>>> {
    @NotNull protected String name;              // node name to display
    @NotNull private final XmlNodeType type;     // node type
    @NotNull private final ProjectData project;  // the parental project

    /**
     * constructor with XML elements
     * @param id
     * @param xml
     * @param project
     */
    public MsmTreeNode(@NotNull final String id, @NotNull final Element xml, @NotNull ProjectData project) {
        this(xml, project);
        this.setId(id);
    }

    /**
     * constructor with XML elements
     * @param id
     * @param attribute
     * @param project
     */
    public MsmTreeNode(@NotNull final String id, @NotNull final Attribute attribute, @NotNull ProjectData project) {
        this(attribute, project);
        this.setId(id);
    }

    /**
     * constructor
     * @param xml
     * @param project
     */
    public MsmTreeNode(@NotNull final Element xml, @NotNull ProjectData project) {
        super(xml);
        this.name = xml.getLocalName();
        this.project = project;

        // determine type
        switch (xml.getLocalName()) {
            case "msm":
                this.type = XmlNodeType.msm;
                break;
            case "global":
                this.type = XmlNodeType.global;
                break;
            case "part":
                this.type = XmlNodeType.part;
                break;
            case "header":
                this.type = XmlNodeType.header;
                break;
            case "dated":
                this.type = XmlNodeType.dated;
                break;
            case "score":
                this.type = XmlNodeType.score;
                break;
            case "sequencingMap":
                this.type = XmlNodeType.sequencingMap;
                break;
            case "note":
                this.type = XmlNodeType.note;
                break;
            case "rest":
                this.type = XmlNodeType.rest;
                break;
            case "lyrics":
                this.type = XmlNodeType.lyrics;
                break;
            default:
                this.type = XmlNodeType.element;
        }
        this.generateMyName();

        // add subtree of this element
//        Elements es = xml.getChildElements();
//        for (int i = 0; i < es.size(); ++i) {
//            Element e = es.get(i);
//            MsmTreeNode node = new MsmTreeNode(UUID.randomUUID().toString(), e);
//            this.add(node);
//        }
    }

    /**
     * constructor
     * @param attribute
     * @param project
     */
    public MsmTreeNode(@NotNull final Attribute attribute, @NotNull ProjectData project) {
        super(attribute);

        this.project = project;
        this.type = XmlNodeType.attribute;
        this.generateMyName();
    }

    /**
     * This is a helper method for method update() to be called when the user data has changed and, thus, also the name of the node must be updated.
     */
    private void generateMyName() {
        switch (this.type) {
            case msm:
                this.name = "<html><font size=\"-2\" color=\"silver\">&lt;/&gt;</font>  msm</html>";
                break;
            case part:
                this.name = ((Element)this.getUserObject()).getLocalName().concat(" " + ((Element)this.getUserObject()).getAttributeValue("number")).concat(" " + ((Element)this.getUserObject()).getAttributeValue("name"));
                break;
            case score:
                this.name = "<html><font size=\"+0\" color=\"silver\">&#9835;</font>  " + ((Element)this.getUserObject()).getLocalName() + "</html>";
                break;
            case sequencingMap:
                this.name = "sequencingMap";
                break;
            case note: {
                int ppq = this.project.getMsm().getPPQ();
                double duration = Double.parseDouble(((Element)this.getUserObject()).getAttributeValue("duration"));

                Attribute pitchname = ((Element)this.getUserObject()).getAttribute("pitchname");
                String pitchString = "note";
                if (pitchname != null) {
                    pitchString = pitchname.getValue();
                    Attribute accid = ((Element)this.getUserObject()).getAttribute("accidentals");
                    if (accid != null) {
                        pitchString = pitchString.concat(Helper.accidDecimal2unicodeString(Double.parseDouble(accid.getValue())));
                    }
                    Attribute octave = ((Element)this.getUserObject()).getAttribute("octave");
                    if (octave != null) {
                        String oct = octave.getValue();
                        if (oct.endsWith(".0"))
                            oct = oct.replace(".0", "");
                        pitchString = pitchString.concat(" " + oct);
                    }
                }
                Element note = (Element) this.getUserObject();
//                boolean hasMillisecondsDate = (note.getAttribute("milliseconds.date") != null);                             // this flag indicates if the note has a milliseconds date (is aligned with audio)
                this.name = "<html><font size=\"+1\">" + Helper.decimalDuration2HtmlUnicode(Helper.pulseDuration2decimal(duration, ppq), false) + "</font> "    // print a note symbol according to the note's value
                        + pitchString                                                                                       // print the note's name
                        + "&nbsp;&nbsp;&nbsp;"
//                        + (hasMillisecondsDate ? "<font color=\"lime\">&#9679;</font>" : "")                                // indicate whether the note has a milliseconds date (is aligned with audio)
                        + (this.project.getScore().contains(note) ? "<font color=\"lime\">&#9679;</font>" : "")             // indicate whether the note is associated with a pixel position in an autograph image
                        + "</html>";
                break;
            }
            case rest: {
                int ppq = this.project.getMsm().getPPQ();
                double duration = Double.parseDouble(((Element)this.getUserObject()).getAttributeValue("duration"));
                this.name = "<html><font size=\"+1\">" + Helper.decimalDuration2HtmlUnicode(Helper.pulseDuration2decimal(duration, ppq), true)+"</font></html>";
                break;
            }
            case lyrics:
                this.name = ((Element)this.getUserObject()).getLocalName() + " \"" + ((Element)this.getUserObject()).getValue() + "\"";
                break;
            case element:
                break;
            case attribute:
                this.name = "<html><font size=\"+0\" color=\"silver\">@</font>  " + ((Attribute)this.getUserObject()).getLocalName() + " " + this.getUserObject().getValue() + "</html>";
//                this.name = "@ " + ((Attribute)this.getUserObject()).getLocalName() + ": " + this.getUserObject().getValue();
                break;
            case global:
            case dated:
            case header:
            default:
                this.name = ((Element)this.getUserObject()).getLocalName();
        }
    }

    /**
     * This method is to be invoked when the user data has changed and the appearance/name of the node requires to be updated.
     * This does not update the visual appearance! Therefore, invoke the tree's updateNode() method.
     */
    protected void update() {
        this.generateMyName();
    }

    /**
     * This method creates the context menu when the node is right-clicked.
     * @param msmTree the MsmTree instance that this node belongs to
     */
    public WebPopupMenu getContextMenu(@NotNull MsmTree msmTree) {
        return MsmEditingTools.makeMsmTreeContextMenu(this, msmTree);
    }

    /**
     * This method will trigger the editor dialog for the node, provided it has one.
     * It is meant to be invoked by a double click event in class MsmTree.
     * @param msmTree
     */
    public void openEditorDialog(@NotNull MsmTree msmTree) {
        MsmEditingTools.quickOpenEditor(this, msmTree);
    }

    /**
     * Access the XML object behind this node.
     * @return
     */
    @NotNull
    @Override
    public Node getUserObject() {
//        final Element object = super.getUserObject();
//        if (object == null) {
//            throw new RuntimeException("Node object must be specified");
//        }
//        return object;
        return super.getUserObject();
    }

    /**
     * This method determines which icon is shown at which type of node.
     * @param parameters
     * @return null, because it slows down performance when too many icons are drawn, instead I put Unicode icons in the name string
     */
//    @Override
    public Icon getNodeIcon(TreeNodeParameters<MsmTreeNode, WebExTree<MsmTreeNode>> parameters) {
        return null;
    }

//    /**
//     * This creates a flat copy of the node. The XML element is a real clone and not just a pointer to the same object.
//     * @return
//     */
//    @NotNull
//    @Override
//    public MsmTreeNode clone() {
//        Element clone = Helper.cloneElement(this.getUserObject());
//        return new MsmTreeNode(UUID.randomUUID().toString(), clone);
//    }

    /**
     * get the local name of the XML element
     * @param parameters
     * @return
     */
    public String getText(TreeNodeParameters<MsmTreeNode, WebExTree<MsmTreeNode>> parameters) {
        return this.name;
    }

    /**
     * read the node type
     * @return
     */
    public XmlNodeType getType() {
        return this.type;
    }

    /**
     * get the local name of the XML element
     * @return
     */
    @NotNull
    @Override
    public String toString() {
        return this.name;
    }

    /**
     * this returns the node's tooltip text
     * @return
     */
    public String getTooltipText() {
        if (this.type == XmlNodeType.attribute)
            return ((Attribute)this.getUserObject()).toXML();

        String s = ((Element)this.getUserObject()).toXML();
        int i = s.indexOf(">") + 1;                         // get the index of the first ">"
        if (i > 0)                                          // if the element has children and the tooltip text would show the whole subtree (which is usually far too much, we want to see only the element/attribute itself)
            s = s.substring(0, i);                          // print only the substring that belongs to the element code itself, not it's children

        return s;                                           // if it is just a one-liner, print it
    }

    /**
     * if this node is of type note, this method can be invoked to play the note
     */
    public void play(MidiPlayer midiPlayer) {
        if ((this.type != XmlNodeType.note) || (midiPlayer == null))
            return;

        Sequence sequence;
        try {
            sequence = new Sequence(Sequence.PPQ, 720);
        } catch (InvalidMidiDataException e) {
            e.printStackTrace();
            return;
        }

        Track track = sequence.createTrack();
        int pitch = Math.max(Math.min((int)Math.round(Double.parseDouble(((Element)this.getUserObject()).getAttributeValue("midi.pitch"))), 127), 0);
        track.add(EventMaker.createNoteOn(0, 0, pitch, 100));
        track.add(EventMaker.createNoteOff(0, 720, pitch, 0));

        try {
            midiPlayer.play(sequence);
        } catch (InvalidMidiDataException e) {
            e.printStackTrace();
        }
    }

    /**
     * Find the MSM part element that this note belongs to.
     * @return the part element or null if this node is no part and is not a child of a part
     */
    public Element getPart() {
        switch (this.getType()) {
            case msm:
            case global:
                return null;
            default: {
                for (MsmTreeNode parent = this; parent != null; parent = parent.getParent())
                    if (parent.getType() == MsmTreeNode.XmlNodeType.part)
                        return (Element) parent.getUserObject();
            }
        }
        return null;
    }

    /**
     * an enumeration of the node types
     */
    public enum XmlNodeType {
        msm,                // an msm node
        element,            // an element
        attribute,          // an attribute
        global,             // a global element
        part,               // a part element
        header,             // a header element
        dated,              // a dated element
        score,              // a score element
        sequencingMap,      // a sequencingMap element
        note,               // a note element
        rest,               // a rest element
        lyrics              // a lyrics element
    }
}
