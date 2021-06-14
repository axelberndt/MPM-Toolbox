package mpmToolbox.gui.mpmTree;

import com.alee.api.annotations.NotNull;
import com.alee.api.ui.TextBridge;
import com.alee.extended.tree.WebExTree;
import com.alee.laf.menu.WebPopupMenu;
import com.alee.laf.tree.TreeNodeParameters;
import com.alee.laf.tree.UniqueNode;
import meico.mei.Helper;
import meico.mpm.elements.*;
import meico.mpm.elements.maps.GenericMap;
import meico.mpm.elements.maps.ImprecisionMap;
import meico.mpm.elements.metadata.Author;
import meico.mpm.elements.metadata.Comment;
import meico.mpm.elements.metadata.Metadata;
import meico.mpm.elements.metadata.RelatedResource;
import meico.mpm.elements.styles.*;
import meico.mpm.elements.styles.defs.*;
import mpmToolbox.ProjectData;
import mpmToolbox.gui.mpmEditingTools.MpmEditingTools;
import nu.xom.Attribute;
import nu.xom.Element;

import javax.swing.*;
import java.util.Enumeration;

/**
 * This class represents a node in an MpmTree.
 * @author Axel Berndt
 */
public class MpmTreeNode extends UniqueNode<MpmTreeNode, Object> implements TextBridge<TreeNodeParameters<MpmTreeNode, WebExTree<MpmTreeNode>>> {
    @NotNull protected String name;                         // node name to display
    @NotNull protected final MpmTreeNode.MpmNodeType type;  // node type
    @NotNull private final ProjectData project;             // the parental project

    /**
     * constructor
     * @param id
     * @param object
     * @param project
     */
    public MpmTreeNode(@NotNull final String id, @NotNull final Object object, @NotNull ProjectData project) {
        this(object, project);
        this.setId(id);
    }

    /**
     * constructor
     * @param object
     * @param project
     */
    public MpmTreeNode(@NotNull final Object object, @NotNull ProjectData project) {
        super(object);
        this.project = project;

        // determine type and set name
//        System.out.println(object.getClass().getCanonicalName());
        if (object.getClass().equals(meico.mpm.Mpm.class)) {
            this.type = MpmNodeType.mpm;
        } else if (object.getClass().equals(meico.mpm.elements.metadata.Metadata.class)) {
            this.type = MpmNodeType.metadata;
        } else if (object.getClass().equals(meico.mpm.elements.metadata.Author.class)) {
            this.type = MpmNodeType.author;
        } else if (object.getClass().equals(meico.mpm.elements.metadata.Comment.class)) {
            this.type = MpmNodeType.comment;
        } else if (object.getClass().equals(mpmToolbox.gui.mpmTree.MpmRelatedResources.class)) {
            this.type = MpmNodeType.relatedResources;
        } else if (object.getClass().equals(meico.mpm.elements.metadata.RelatedResource.class)) {
            this.type = MpmNodeType.relatedResource;
        } else if (object.getClass().equals(meico.mpm.elements.Performance.class)) {
            this.type = MpmNodeType.performance;
        } else if (object.getClass().equals(meico.mpm.elements.Global.class)) {
            this.type = MpmNodeType.global;
        } else if (object.getClass().equals(meico.mpm.elements.Part.class)) {
            this.type = MpmNodeType.part;
        } else if (object.getClass().equals(meico.mpm.elements.Header.class)) {
            this.type = MpmNodeType.header;
        } else if (object.getClass().equals(mpmToolbox.gui.mpmTree.MpmStyleCollection.class)) {
            this.type = MpmNodeType.styleCollection;
        } else if (object.getClass().equals(meico.mpm.elements.styles.ArticulationStyle.class)) {
            this.type = MpmNodeType.articulationStyle;
        } else if (object.getClass().equals(meico.mpm.elements.styles.defs.ArticulationDef.class)) {
            this.type = MpmNodeType.articulationDef;
        } else if (object.getClass().equals(meico.mpm.elements.styles.DynamicsStyle.class)) {
            this.type = MpmNodeType.dynamicsStyle;
        } else if (object.getClass().equals(meico.mpm.elements.styles.defs.DynamicsDef.class)) {
            this.type = MpmNodeType.dynamicsDef;
        } else if (object.getClass().equals(meico.mpm.elements.styles.GenericStyle.class)) {
            this.type = MpmNodeType.genericStyle;
        } else if (object.getClass().equals(meico.mpm.elements.styles.MetricalAccentuationStyle.class)) {
            this.type = MpmNodeType.metricalAccentuationStyle;
        } else if (object.getClass().equals(meico.mpm.elements.styles.defs.AccentuationPatternDef.class)) {
            this.type = MpmNodeType.accentuationPatternDef;
        } else if (object.getClass().equals(meico.mpm.elements.styles.RubatoStyle.class)) {
            this.type = MpmNodeType.rubatoStyle;
        } else if (object.getClass().equals(meico.mpm.elements.styles.defs.RubatoDef.class)) {
            this.type = MpmNodeType.rubatoDef;
        } else if (object.getClass().equals(meico.mpm.elements.styles.TempoStyle.class)) {
            this.type = MpmNodeType.tempoStyle;
        } else if (object.getClass().equals(meico.mpm.elements.styles.defs.TempoDef.class)) {
            this.type = MpmNodeType.tempoDef;
        } else if (object.getClass().equals(meico.mpm.elements.Dated.class)) {
            this.type = MpmNodeType.dated;
        } else if (object.getClass().equals(meico.mpm.elements.maps.ArticulationMap.class)) {
            this.type = MpmNodeType.articulationMap;
        } else if (object.getClass().equals(meico.mpm.elements.maps.AsynchronyMap.class)) {
            this.type = MpmNodeType.asynchronyMap;
        } else if (object.getClass().equals(meico.mpm.elements.maps.DynamicsMap.class)) {
            this.type = MpmNodeType.dynamicsMap;
        } else if (object.getClass().equals(meico.mpm.elements.maps.GenericMap.class)) {
            this.type = MpmNodeType.genericMap;
        } else if (object.getClass().equals(meico.mpm.elements.maps.ImprecisionMap.class)) {
            this.type = MpmNodeType.imprecisionMap;
        } else if (object.getClass().equals(meico.mpm.elements.maps.MetricalAccentuationMap.class)) {
            this.type = MpmNodeType.metricalAccentuationMap;
        } else if (object.getClass().equals(meico.mpm.elements.maps.OrnamentationMap.class)) {
            this.type = MpmNodeType.ornamentationMap;
        } else if (object.getClass().equals(meico.mpm.elements.maps.RubatoMap.class)) {
            this.type = MpmNodeType.rubatoMap;
        } else if (object.getClass().equals(meico.mpm.elements.maps.TempoMap.class)) {
            this.type = MpmNodeType.tempoMap;
        } else if (object.getClass().equals(nu.xom.Element.class)) {
            switch (((Element)object).getLocalName()) {
                case "accentuationPattern":
                    this.type = MpmNodeType.accentuationPattern;
                    break;
                case "accentuation":
                    this.type = MpmNodeType.accentuation;
                    break;
                case "articulation":
                    this.type = MpmNodeType.articulation;
                    break;
                case "asynchrony":
                    this.type = MpmNodeType.asynchrony;
                    break;
                case "distribution.uniform":
                    this.type = MpmNodeType.distributionUniform;
                    break;
                case "distribution.gaussian":
                    this.type = MpmNodeType.distributionGaussian;
                    break;
                case "distribution.triangular":
                    this.type = MpmNodeType.distributionTriangular;
                    break;
                case "distribution.correlated.brownianNoise":
                    this.type = MpmNodeType.distributionCorrelatedBrownianNoise;
                    break;
                case "distribution.correlated.compensatingTriangle":
                    this.type = MpmNodeType.distributionCorrelatedCompensatingTriangle;
                    break;
                case "distribution.list":
                    this.type = MpmNodeType.distributionList;
                    break;
                case "dynamics":
                    this.type = MpmNodeType.dynamics;
                    break;
                case "rubato":
                    this.type = MpmNodeType.rubato;
                    break;
                case "tempo":
                    this.type = MpmNodeType.tempo;
                    break;
                case "style":
                    this.type = MpmNodeType.style;
                    break;
                default:
                    this.type = MpmNodeType.xmlElement;
            }
        } else {
            this.type = MpmNodeType.unknown;
        }

        this.generateMyName();
    }

    /**
     * This is a helper method for method update() to be called when the user data has changed and, thus, also the name of the node must be updated.
     */
    private void generateMyName() {
        switch (this.type) {
            case mpm:
                this.name = "<html><font size=\"-2\" color=\"silver\">&lt;/&gt;</font>  mpm</html>";
                break;

            case metadata:
                this.name = "metadata";
                break;

            case author:
                Author author = (Author) this.getUserObject();
                this.name = "author" + ((author.getNumber() == null) ? "" : (" " + author.getNumber())) + (" " + (author.getName()));
                break;

            case comment:
                this.name = "comment";
                break;

            case relatedResources:
                this.name = "related resources";
                break;

            case relatedResource:
                RelatedResource resource = (RelatedResource) this.getUserObject();
                this.name = resource.getType() + ": " + resource.getUri();
                break;

            case performance:
                Performance performance = (Performance) this.getUserObject();
                this.name = "<html>performance <i>" + performance.getName() + "</i>, " + performance.getPulsesPerQuarter() + " ppq</html>";
                break;

            case global:
                this.name = "global";
                break;

            case part:
                Part part = (Part) this.getUserObject();
                this.name = "part " + part.getNumber() + " " + part.getName();
                break;

            case header:
                this.name = "header";
                break;

            case styleCollection:
                this.name = ((MpmStyleCollection) this.getUserObject()).type;
                break;

            case articulationStyle:
                this.name = ((ArticulationStyle) this.getUserObject()).getName();
                break;

            case articulationDef:
                this.name = ((ArticulationDef) this.getUserObject()).getName();
                // TODO visualize the attributes/modifiers
                break;

            case dynamicsStyle:
                this.name = ((DynamicsStyle) this.getUserObject()).getName();
                break;

            case dynamicsDef:
                DynamicsDef dynamicsDef = (DynamicsDef) this.getUserObject();
                this.name = dynamicsDef.getName() + " = " + dynamicsDef.getValue();
                break;

            case genericStyle:
                this.name = ((GenericStyle) this.getUserObject()).getName();
                break;

            case metricalAccentuationStyle:
                this.name = ((MetricalAccentuationStyle) this.getUserObject()).getName();
                break;

            case accentuationPatternDef:
                AccentuationPatternDef accentuationPatternDef = (AccentuationPatternDef) this.getUserObject();
                this.name = accentuationPatternDef.getName() + ", length = " + accentuationPatternDef.getLength();
                // TODO: visualize the contents/the accentuation pattern
                break;

            case accentuation:
                this.name = "accentuation";
                // TODO
                break;

            case rubatoStyle:
                this.name = ((RubatoStyle) this.getUserObject()).getName();
                break;

            case rubatoDef:
                this.name = ((RubatoDef) this.getUserObject()).getName();
                // TODO: visualize the other attributes
                break;

            case tempoStyle:
                this.name = ((TempoStyle) this.getUserObject()).getName();
                break;

            case tempoDef:
                TempoDef tempoDef = (TempoDef) this.getUserObject();
                this.name = tempoDef.getName() + " = " + tempoDef.getValue();
                break;

            case dated:
                this.name = "dated";
                break;

            case articulationMap:
                this.name = "articulationMap";
                break;

            case articulation: {
                this.name = "articulation";
                Attribute nameRef = ((Element) this.getUserObject()).getAttribute("name.ref");
                if (nameRef != null)
                    this.name += " " + nameRef.getValue();
                // TODO
                break;
            }
            case asynchronyMap:
                this.name = "asynchronyMap";
                break;

            case asynchrony:
                this.name = "asynchrony " + ((Element) this.getUserObject()).getAttributeValue("milliseconds.offset") + " ms";
                break;

            case dynamicsMap:
                this.name = "dynamicsMap";
                break;

            case dynamics: {
                this.name = ((Element) this.getUserObject()).getAttributeValue("volume");
                Attribute transitionTo = ((Element) this.getUserObject()).getAttribute("transition.to");
                if (transitionTo != null)
                    this.name += " &#8594; " + transitionTo.getValue();
                break;
            }
            case genericMap:
                this.name = ((GenericMap) this.getUserObject()).getType();
                break;

            case metricalAccentuationMap:
                this.name = "metricalAccentuationMap";
                break;

            case accentuationPattern:
                this.name = "accentuationPattern " + ((Element) this.getUserObject()).getAttributeValue("name.ref");
                break;

            case imprecisionMap:
                ImprecisionMap imprecisionMap = (ImprecisionMap) this.getUserObject();
                String detuneUnit = imprecisionMap.getDetuneUnit();
                this.name = "imprecisionMap." + imprecisionMap.getDomain() + (detuneUnit.isEmpty() ? "" : (" (in " + detuneUnit + ")"));
                break;

            case distributionUniform:
                this.name = "distribution.uniform";
                // TODO
                break;

            case distributionGaussian:
                this.name = "distribution.gaussian";
                // TODO
                break;

            case distributionTriangular:
                this.name = "distribution.triangular";
                // TODO
                break;

            case distributionCorrelatedBrownianNoise:
                this.name = "distribution.correlated.brownianNoise";
                // TODO
                break;

            case distributionCorrelatedCompensatingTriangle:
                this.name = "distribution.correlated.compensatingTriangle";
                // TODO
                break;

            case distributionList:
                this.name = "distribution.list";
                // TODO
                break;

            case ornamentationMap:
                this.name = "ornamentationMap";
                break;

            case rubatoMap:
                this.name = "rubatoMap";
                break;

            case rubato:
                this.name = "rubato";
                Attribute nameRef = ((Element) this.getUserObject()).getAttribute("name.ref");
                if (nameRef != null)
                    this.name += " " + nameRef.getValue();
                // TODO
                break;

            case tempoMap:
                this.name = "tempoMap";
                break;

            case tempo: {
                this.name = ((Element) this.getUserObject()).getAttributeValue("bpm");
                Attribute transitionTo = ((Element) this.getUserObject()).getAttribute("transition.to");
                if (transitionTo != null)
                    this.name += " &#8594; " + transitionTo.getValue();
                break;
            }

            case style:
                Element style = (Element) this.getUserObject();
                Attribute defaultArticulation = Helper.getAttribute("defaultArticulation", style);
                this.name = "style = \"" + Helper.getAttributeValue("name.ref", style) + "\"" + ((defaultArticulation == null) ? "" : (", default articulation = \"" + defaultArticulation.getValue() + "\""));
                break;

            case xmlElement:
                this.name = "<html><font size=\"-2\" color=\"silver\">&lt;/&gt;</font>  " + ((Element)this.getUserObject()).getLocalName() + "</html>";
                break;

            case unknown:
            default:
                this.name = "unknown object of type " + getUserObject().getClass().getCanonicalName();
        }

        // all dated MPM elements, i.e. performance instructions , get an <html> environment and an indication that they are located in the graphical score
        switch (this.type) {
            case accentuationPattern:
            case articulation:
            case asynchrony:
            case distributionCorrelatedBrownianNoise:
            case distributionCorrelatedCompensatingTriangle:
            case distributionGaussian:
            case distributionList:
            case distributionTriangular:
            case distributionUniform:
            case dynamics:
            case rubato:
            case style:
            case tempo:
                this.name = "<html>" + this.name + "&nbsp;&nbsp;&nbsp;"
                        + (this.project.getScore().contains((Element) this.getUserObject()) ? "<font color=\"aqua\">&#9679;</font>" : "")   // indicate whether the note is associated with a pixel position in an autograph image
                        + "</html>";
        }
    }

    /**
     * get the name of this
     * @param parameters
     * @return
     */
    public String getText(TreeNodeParameters<MpmTreeNode, WebExTree<MpmTreeNode>> parameters) {
        return this.name;
    }

    /**
     * read the node type
     * @return
     */
    public MpmTreeNode.MpmNodeType getType() {
        return this.type;
    }

    /**
     * This is a quick way to find out if a node represents an entry in an MPM map of some type.
     * @return
     */
    public boolean isMapEntryType() {
        switch (this.type) {
            case accentuationPattern:
            case articulation:
            case asynchrony:
            case distributionCorrelatedBrownianNoise:
            case distributionCorrelatedCompensatingTriangle:
            case distributionGaussian:
            case distributionList:
            case distributionTriangular:
            case distributionUniform:
            case dynamics:
            case rubato:
            case style:
            case tempo:
                return true;
        }
        return false;
    }

    /**
     * This method determines which icon is shown at which type of node.
     * @param parameters
     * @return null, because it slows down performance when too many icons are drawn, instead I put Unicode icons in the name string
     */
    public Icon getNodeIcon(TreeNodeParameters<MpmTreeNode, WebExTree<MpmTreeNode>> parameters) {
        return null;
    }

    /**
     * this returns the node's tooltip text
     * @return
     */
    public String getTooltipText() {
        String s = "";

        switch (this.type) {
            case mpm:
                return "<mpm>";

            case metadata:
                s = ((Metadata) this.getUserObject()).toXml();
                break;
            case author:
                return ((Author) this.getUserObject()).toXml().trim();
            case comment:
                return ((Comment) this.getUserObject()).toXml().trim();
            case relatedResources:
                return "<relatedResources>";
            case relatedResource:
                return ((RelatedResource) this.getUserObject()).toXml().trim();

            case performance:
                s = ((Performance) this.getUserObject()).toXml();
                break;

            case global:
                s = ((Global) this.getUserObject()).toXml();
                break;

            case part:
                s = ((Part) this.getUserObject()).toXml();
                break;

            case header:
                s = ((Header) this.getUserObject()).toXml();
                break;
            case dated:
                s = ((Dated) this.getUserObject()).toXml();
                break;

            case styleCollection:
                return "<" + ((MpmStyleCollection) this.getUserObject()).type + ">";

            case articulationStyle:
            case metricalAccentuationStyle:
            case dynamicsStyle:
            case genericStyle:
            case rubatoStyle:
            case tempoStyle:
                s = ((GenericStyle) this.getUserObject()).toXml();
                break;

            case articulationMap:
            case asynchronyMap:
            case metricalAccentuationMap:
            case dynamicsMap:
            case genericMap:
            case imprecisionMap:
            case ornamentationMap:
            case rubatoMap:
            case tempoMap:
                s = ((GenericMap) this.getUserObject()).toXml();
                break;

            case articulationDef:
                return ((ArticulationDef) this.getUserObject()).toXml().trim();
            case accentuationPatternDef:
                s = ((AccentuationPatternDef) this.getUserObject()).toXml().trim();
                break;
            case dynamicsDef:
                return ((DynamicsDef) this.getUserObject()).toXml().trim();
            case rubatoDef:
                return ((RubatoDef) this.getUserObject()).toXml().trim();
            case tempoDef:
                return ((TempoDef) this.getUserObject()).toXml().trim();

            case articulation:
            case accentuationPattern:
            case accentuation:
            case asynchrony:
            case dynamics:
            case distributionUniform:
            case distributionGaussian:
            case distributionTriangular:
            case distributionCorrelatedBrownianNoise:
            case distributionCorrelatedCompensatingTriangle:
            case rubato:
            case tempo:
            case style:
                return ((Element) this.getUserObject()).toXML().trim();

            case distributionList:
            case xmlElement:
                s = ((Element) this.getUserObject()).toXML();
                break;

            case unknown:
                return this.getUserObject().getClass().getCanonicalName();
        }

        int i = s.indexOf(">") + 1;                         // get the index of the first ">"
        if (i > 0)                                          // if the element has children and the tooltip text would show the whole subtree (which is usually far too much, we want to see only the element/attribute itself)
            s = s.substring(0, i);                          // print only the substring that belongs to the element code itself, not it's children

        return s;                                           // if it is just a one-liner, print it
    }

    /**
     * This method is to be invoked when the user data has changed and the appearance/name of the node requires to be updated.
     * This does not update the visual appearance! Therefore, invoke the tree's updateNode() method.
     */
    public void update() {
        this.generateMyName();
    }

    /**
     * This method works only for performance instructions, i.e. elements in an MPM map.
     * It sets its date to the specified value and triggers the parent map to reorder its list of elements accordingly.
     * This does not update the visual appearance! Therefore, invoke the tree's updateNode() method with the parent tree node.
     * @param date
     */
    public void setDate(double date) {
        switch (this.getType()) {       // if the node is of a dated type
            case accentuationPattern:
            case articulation:
            case asynchrony:
            case distributionCorrelatedBrownianNoise:
            case distributionCorrelatedCompensatingTriangle:
            case distributionGaussian:
            case distributionList:
            case distributionTriangular:
            case distributionUniform:
            case dynamics:
            case rubato:
            case style:
            case tempo:
                break;
            default:
                return;
        }
        Attribute dateAtt = Helper.getAttribute("date", (Element) this.getUserObject());    // get the date attribute
        dateAtt.setValue(Double.toString(date));                        // change date attribute

        GenericMap map = (GenericMap) this.getParent().getUserObject(); // get the map
        map.sort();                                                     // after changing the date it mus reorder its elements
    }

    /**
     * This method creates the context menu when the node is right-clicked.
     * @param mpmTree the MpmTree instance that this node belongs to
     */
    public WebPopupMenu getContextMenu(@NotNull MpmTree mpmTree) {
        return MpmEditingTools.makeMpmTreeContextMenu(this, mpmTree);
    }

    /**
     * This method will trigger the editor dialog for the node, provided it has one.
     * It is meant to be invoked by a double click event in class MpmTree.
     * @param mpmTree
     */
    public void openEditorDialog(@NotNull MpmTree mpmTree) {
        MpmEditingTools.quickOpenEditor(this, mpmTree);
    }

    /**
     * Access the data behind this node.
     * @return
     */
    @NotNull
    @Override
    public Object getUserObject() {
//        final Element object = super.getUserObject();
//        if (object == null) {
//            throw new RuntimeException("Node object must be specified");
//        }
//        return object;
        return super.getUserObject();
    }

    /**
     * retrieve the child node of this that holds the specified user object
     * @param userObject
     * @param depthFirstStrategy true for depth first search, false for breath first search
     * @return
     */
    public MpmTreeNode findChildNode(Object userObject, boolean depthFirstStrategy) {
        if (userObject == null)
            return null;

        Enumeration<MpmTreeNode> e = depthFirstStrategy ? this.depthFirstEnumeration() : this.breadthFirstEnumeration();

        while (e.hasMoreElements()) {
            MpmTreeNode treeNode = e.nextElement();
            Object userObj = treeNode.getUserObject();
            if (userObject == userObj) {
                return treeNode;
            }
        }
        return null;
    }

    /**
     * get the performance that this node belongs to
     * @return the performance that this node belongs to, or null if it is not in a performance
     */
    public Performance getPerformance() {
        switch (this.getType()) {
            case mpm:
            case metadata:
            case author:
            case comment:
            case relatedResources:
            case relatedResource:
                return null;

//            case performance:
//            case global:
//            case part:
//            case header:
//            case dated:
//            case styleCollection:
//            case articulationStyle:
//            case metricalAccentuationStyle:
//            case dynamicsStyle:
//            case genericStyle:
//            case rubatoStyle:
//            case tempoStyle:
//            case articulationMap:
//            case asynchronyMap:
//            case metricalAccentuationMap:
//            case dynamicsMap:
//            case genericMap:
//            case imprecisionMap:
//            case ornamentationMap:
//            case rubatoMap:
//            case tempoMap:
//            case articulationDef:
//            case accentuationPatternDef:
//            case dynamicsDef:
//            case rubatoDef:
//            case tempoDef:
//            case articulation:
//            case accentuationPattern:
//            case accentuation:
//            case asynchrony:
//            case dynamics:
//            case distributionUniform:
//            case distributionGaussian:
//            case distributionTriangular:
//            case distributionCorrelatedBrownianNoise:
//            case distributionCorrelatedCompensatingTriangle:
//            case rubato:
//            case tempo:
//            case style:
//            case distributionList:
//            case xmlElement:
//            case unknown:
            default:
                for (MpmTreeNode parent = this; parent != null; parent = parent.getParent())
                    if (parent.getType() == MpmNodeType.performance)
                        return (Performance) parent.getUserObject();
        }
        return null;
    }

    /**
     * an enumeration of the node types
     */
    public enum MpmNodeType {
        mpm,
        metadata,
        author,
        comment,
        relatedResources,
        relatedResource,
        performance,
        global,
        part,
        header,
        styleCollection,
        articulationStyle,
        articulationDef,
        dynamicsStyle,
        dynamicsDef,
        genericStyle,
        metricalAccentuationStyle,
        accentuationPatternDef,
        accentuation,
        rubatoStyle,
        rubatoDef,
        tempoStyle,
        tempoDef,
        dated,
        articulationMap,
        articulation,
        asynchronyMap,
        asynchrony,
        dynamicsMap,
        dynamics,
        genericMap,
        imprecisionMap,
        distributionUniform,
        distributionGaussian,
        distributionTriangular,
        distributionCorrelatedBrownianNoise,
        distributionCorrelatedCompensatingTriangle,
        distributionList,
        metricalAccentuationMap,
        accentuationPattern,
        ornamentationMap,
        rubatoMap,
        rubato,
        tempoMap,
        tempo,
        style,
        xmlElement,
        unknown
    }
}
