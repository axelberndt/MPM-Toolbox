package mpmToolbox.gui.score;

import meico.mei.Helper;
import meico.supplementary.KeyValue;
import mpmToolbox.supplementary.Tools;
import mpmToolbox.supplementary.orthantNeighborhoodGraph.ONGNode;
import mpmToolbox.supplementary.orthantNeighborhoodGraph.OrthantNeighborhoodGraph;
import nu.xom.Attribute;
import nu.xom.Element;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * This class represents one score page.
 * @author Axel Berndt
 */
public class ScorePage extends OrthantNeighborhoodGraph {
    private final File file;                                                    // the score file (image file) behind this score page
    private final BufferedImage image;                                          // the image of the score page
    private final HashMap<Element, ScoreNode> object2Node = new HashMap<>();    // this maps elements to ONGNodes

    /**
     * constructor
     * @param file
     */
    public ScorePage(File file) throws IOException {
        this.file = file;
        this.image = Tools.readImageFile(this.file);
    }

    /**
     * constructor to be used in class Score in the constructor that reads project data
     * @param pageElement
     * @param basePath
     * @param noteAnnotations
     * @param performanceAnnotations
     * @throws IOException
     */
    protected ScorePage(Element pageElement, String basePath, HashMap<String, KeyValue<ScorePage, KeyValue<Double, Double>>> noteAnnotations, HashMap<String, KeyValue<ScorePage, KeyValue<Double, Double>>> performanceAnnotations) throws IOException {
        this.file = new File(basePath + pageElement.getAttributeValue("file"));     // get the image file
        if (!file.exists())
            throw new IOException("Score image file " + this.file.getAbsolutePath() + " does not exist.");

        this.image = Tools.readImageFile(this.file);

        // see if the image resolution matches the data in the project
        Attribute widthPixels = Helper.getAttribute("width.pixels", pageElement);                           // get the width.pixels attribute from the project file
        int widthImage = this.image.getWidth();                                                             // get the actual image width
        int widthData = (widthPixels == null) ? widthImage : Integer.parseInt(widthPixels.getValue());      // if we had no attribute, we assume that the widths match, otherwise we read the attribute value
        Attribute heightPixels = Helper.getAttribute("height.pixels", pageElement);                         // get the height.pixels attribute from the project file
        int heightImage = this.image.getHeight();                                                           // get the actual image height
        int heightData = (heightPixels == null) ? heightImage : Integer.parseInt(heightPixels.getValue());  // if we had no attribute, we assume that the heights match, otherwise we read the attribute value
        double scaleX = ((double) widthImage) / widthData;                                                  // compute the scale factor for all x coordinates
        double scaleY = ((double) heightImage) / heightData;                                                // compute the scale factor for all y coordinates

        for (Element entry : pageElement.getChildElements()) {                      // for each entry on the score page make an entry in the ScorePage
            double x = scaleX * Double.parseDouble(entry.getAttributeValue("x"));   // get its x coordinate
            double y = scaleY * Double.parseDouble(entry.getAttributeValue("y"));   // get its y coordinate
            String id = entry.getAttributeValue("ref");                             // get its reference string

            // make appropriate entries in the ScorePage depending on the local name of the XML entry
            switch (entry.getLocalName()) {
                case "note":                                                        // store the note
                    noteAnnotations.put(id, new KeyValue<>(this, new KeyValue<>(x, y)));
                    break;
                case "accentuationPattern":
                case "articulation":
                case "asynchrony":
                case "distribution.correlated.brownianNoise":
                case "distribution.correlated.compensatingTriangle":
                case "distribution.gaussian":
                case "distribution.list":
                case "distribution.triangular":
                case "distribution.uniform":
                case "dynamics":
                case "rubato":
                case "style":
                case "tempo":
                default:                                                            // store the performance instruction
                    performanceAnnotations.put(id, new KeyValue<>(this, new KeyValue<>(x, y)));
                    break;
            }
        }
    }

    /**
     * get the image file of this score page
     * @return
     */
    public File getFile() {
        return this.file;
    }

    /**
     * get the image of the score page
     * @return
     */
    public BufferedImage getImage() {
        return this.image;
    }

    /**
     * add an entry to the score page
     * @param x the x position of the entry
     * @param y the y position of the entry
     * @param element the element that is associated with the node
     * @return the ONGNode added and associated with element
     */
    public ScoreNode addEntry(double x, double y, Element element) {
        this.removeEntry(element);                              // this just makes sure that there is not already an entry for element, and if there is one it has to be removed anyway

        // make sure that the element has an xml:id
        Attribute id = element.getAttribute("id", "http://www.w3.org/XML/1998/namespace");  // get the element's XML ID
        if (id == null) {                                       // if there is none
            String uuid = "mpmToolbox_" + UUID.randomUUID().toString();                // generate new ids for them
            Attribute a = new Attribute("id", uuid);                        // create an attribute
            a.setNamespace("xml", "http://www.w3.org/XML/1998/namespace");  // set its namespace to xml
            element.addAttribute(a);                                        // add attribute to the element
        }

        ScoreNode node = new ScoreNode(x, y, element);
        node = (ScoreNode) this.add(node);                      // add the node to the graph, if there is already a node at the specified position we get that node
        if (!node.getAssociatedElements().contains(element))
            node.addAssociatedElement(element);
        this.object2Node.put(element, node);                    // add the entry to the hashmap
        return node;                                            // return the node
    }

    /**
     * remove an entry from the score page
     * @param element
     */
    public void removeEntry(Element element) {
        ScoreNode node = this.object2Node.get(element);
        if (node == null)
            return;

        // if more than one object is associated with node, we should not remove node from the graph
        try {
            node.removeAssociatedElement(element);  // throws exception if node is empty/has no further elements associated with it
        } catch (Exception e) {
            this.remove(node);
        }

        this.object2Node.remove(element);    // remove entry from hashmap
    }

    /**
     * get the node associated with the specified element or null
     * @param element
     * @return
     */
    public ScoreNode getNode(Element element) {
        return this.object2Node.get(element);
    }

    /**
     * get a list of the elements associated with the nearest node to the specified coordinates
     * @param x
     * @param y
     * @return
     */
    public ArrayList<Element> getNearestElements(double x, double y) {
        KeyValue<ONGNode, Double> node = this.findNearestNeighborOf(x, y);
        if (node == null)
            return new ArrayList<>();
        return ((ScoreNode) node.getKey()).getAssociatedElements();
    }

    /**
     * check if an element is contained in this score page
     * @param element
     * @return
     */
    public boolean contains(Element element) {
        return this.object2Node.containsKey(element);
    }

    /**
     * get the hashmap with all entries on this score page
     * @return
     */
    public HashMap<Element, ScoreNode> getAllEntries() {
        return this.object2Node;
    }

    /**
     * Export the score page's data as XML data to be stored in an MPM Toolbox project file (.mpr).
     * Be aware that the image file is linked via an absolute path! Edit the file attribute of the
     * output element to set a relative path.
     * @return
     */
    public Element toXml() {
        // make an XML element for this page
        Element pageElt = new Element("page");
        Path absolutePath = this.getFile().toPath();
        pageElt.addAttribute(new Attribute("file", absolutePath.toString()));

        // store the measurements of the image, so if the resolution changes, we can still scale the pixel positions
        pageElt.addAttribute(new Attribute("width.pixels", String.valueOf(this.getImage().getWidth())));
        pageElt.addAttribute(new Attribute("height.pixels", String.valueOf(this.getImage().getHeight())));

        // add the entry on this page to the XML
        for (Map.Entry<Element, ScoreNode> entry : this.getAllEntries().entrySet()) {
            Element element = entry.getKey();
            String id = Helper.getAttributeValue("id", element);
            Element elementAssociation = new Element(element.getLocalName());
            elementAssociation.addAttribute(new Attribute("ref", id));
            elementAssociation.addAttribute(new Attribute("x", "" + entry.getValue().getX()));
            elementAssociation.addAttribute(new Attribute("y", "" + entry.getValue().getY()));
            pageElt.appendChild(elementAssociation);
        }

        return pageElt;
    }
}
