package mpmToolbox.gui.score;

import com.alee.api.annotations.NotNull;
import meico.mei.Helper;
import meico.supplementary.KeyValue;
import mpmToolbox.projectData.ProjectData;
import nu.xom.Attribute;
import nu.xom.Element;
import nu.xom.Node;
import nu.xom.Nodes;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * This class represents score data and note pixel position associations.
 * @author Axel Berndt
 */
public class Score {
    private final ProjectData parentProject;                        // a link to the parent project
    private final ArrayList<ScorePage> pages = new ArrayList<>();   // the list of score pages
    private int overlayElementSize = 0;                             // this keeps track of the size setting for overlay elements in the score display

    /**
     * constructor, this one does not parse the project, it only adds the specified score files to its structure
     * @param project
     */
    public Score(@NotNull ArrayList<File> files, @NotNull ProjectData project) {
        this.parentProject = project;

        for (File file : files)
            this.addPage(file);
    }

    /**
     * constructor, this one parses the project's XML to generate its data structure
     * @param project
     */
    public Score(@NotNull ProjectData project) {
        this.parentProject = project;

        // is there XML data to parse?
        if (this.parentProject.getXml() == null)
            return;

        // read the score data from the project's XML
        Element e = this.parentProject.getXml().getRootElement().getFirstChildElement("score");
        if (e == null)          // no score data found
            return;             // done

        Attribute overlayElementSizeAtt = e.getAttribute("overlayElementSize");
        if (overlayElementSizeAtt != null)
            this.overlayElementSize = Integer.parseInt(overlayElementSizeAtt.getValue());

        // parse the score data
        HashMap<String, KeyValue<ScorePage, KeyValue<Double, Double>>> noteAnnotations = new HashMap<>();
        HashMap<String, KeyValue<ScorePage, KeyValue<Double, Double>>> performanceAnnotations = new HashMap<>();
        String basePath = this.parentProject.getXml().getFile().getParent() + File.separator;   // the project directory is required to resolve relative paths
        for (Element pageElement : e.getChildElements("page")) {                                    // for each score page
            ScorePage page;
            try {
                page = new ScorePage(pageElement, basePath, noteAnnotations, performanceAnnotations);
            } catch (IOException ioException) {
                ioException.printStackTrace();
                continue;
            }
            this.pages.add(page);
        }

        // find the MSM note elements that correspond to the above ID strings
        if (!noteAnnotations.isEmpty()) {
            HashMap<String, Element> notes = new HashMap<>();
            for (Element part : this.parentProject.getMsm().getParts()) {
                Element score = part.getFirstChildElement("dated").getFirstChildElement("score");   // get the part's score
                if (score == null)                                                          // if it has none
                    continue;                                                               // we are done with this part

                for (Element note : score.getChildElements("note"))                         // for each note in the MSM part
                    notes.put(Helper.getAttributeValue("id", note), note);                  // add it to the hashmap
            }

            // for each note annotation make an entry in the corresponding ScorePage data structure
            for (Map.Entry<String, KeyValue<ScorePage, KeyValue<Double, Double>>> noteAnnotation : noteAnnotations.entrySet()) {    // for each note annotation
                Element note = notes.get(noteAnnotation.getKey());                          // get the corresponding note element
                if (note == null)                                                           // if there is none
                    continue;                                                               // go on with the next association
                ScorePage page = noteAnnotation.getValue().getKey();                        // get the page where the note association should be added
                page.addEntry(noteAnnotation.getValue().getValue().getKey(), noteAnnotation.getValue().getValue().getValue(), note);    // add the note association to the page
            }
        }

        // find the MPM elements that correspond to the above ID strings
        if (!performanceAnnotations.isEmpty()) {
            HashMap<String, Element> perfs = new HashMap<>();
            Nodes nodes = this.parentProject.getMpm().getRootElement().query("descendant::*[@date and @xml:id]"); // get all children with attributes date and xml:id, i.e. all performance instructions with an id
            for (Node node : nodes) {
                Element n = (Element) node;
                String id = Helper.getAttributeValue("id", n);
                perfs.put(id, n);
            }

            // for each performance annotation make an entry in the corresponding ScorePage data structure
            for (Map.Entry<String, KeyValue<ScorePage, KeyValue<Double, Double>>> perfAssociation : performanceAnnotations.entrySet()) {
                Element perf = perfs.get(perfAssociation.getKey());
                if (perf == null)
                    continue;
                ScorePage page = perfAssociation.getValue().getKey();
                page.addEntry(perfAssociation.getValue().getValue().getKey(), perfAssociation.getValue().getValue().getValue(), perf);
            }
        }
    }

    /**
     * a setter for the size of overlay elements
     * @param size
     */
    public void setOverlayElementSize(int size) {
        this.overlayElementSize = size;
    }

    /**
     * get the size of overlay elements
     * @return
     */
    public int getOverlayElementSize() {
        return this.overlayElementSize;
    }

    /**
     * are there files in the score
     * @return
     */
    public boolean isEmpty() {
        return this.pages.isEmpty();
    }

    /**
     * get the page/file count
     * @return
     */
    public int size() {
        return this.pages.size();
    }

    /**
     * check whether the specified file is contained in the score
     * @param file
     * @return
     */
    public boolean contains(File file) {
        for (ScorePage page : this.pages) {
            if (page.getFile() == file)
                return true;
        }
        return false;
    }

    /**
     * check whether the specified element is associated to a score position on any page
     * @param element
     * @return
     */
    public boolean contains(Element element) {
        for (ScorePage page : this.pages) {
            if (page.contains(element))
                return true;
        }
        return false;
    }

    /**
     * add a score file
     * @param file
     * @return
     */
    public ScorePage addPage(@NotNull File file) {
        if (!file.exists())
            return null;

        if (this.contains(file))
            return null;

        ScorePage page = null;
        try {
            page = new ScorePage(file);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        if (!this.pages.add(page))
            return null;

        return page;
    }

    /**
     * access the list of files
     * @return
     */
    public ArrayList<File> getFiles() {
        ArrayList<File> files = new ArrayList<>();
        for (ScorePage page : this.pages)
            files.add(page.getFile());
        return files;
    }

    /**
     * get all the pages in this score
     * @return
     */
    public ArrayList<ScorePage> getAllPages() {
        return this.pages;
    }

    /**
     * get a score page by index
     * @param index
     * @return
     */
    public ScorePage getPage(int index) {
        if (this.isEmpty())
            return null;

        index = Math.max(0, Math.min(this.size(), index));
        return this.pages.get(index);
    }

    /**
     * retrieve a file by file path
     * @param filePath
     * @return
     */
    public ScorePage getPage(String filePath) {
        for (ScorePage page : this.pages)
            if (page.getFile().getAbsolutePath().equals(filePath))
                return page;
        return null;
    }

    /**
     * find the index of a file
     * @param file
     * @return the index or -1 if not found
     */
    public int getPageIndex(File file) {
        for (int index = 0; index < this.pages.size(); ++index) {
            if (this.pages.get(index).getFile().equals(file))
                return index;
        }
        return -1;
    }

    /**
     * remove a page from the score
     * @param index
     */
    public void removePage(int index) {
        if (index >= this.pages.size())
            return;
        this.pages.remove(index);
    }

    /**
     * This method checks all nodes on all pages whether the corresponding elements still
     * exist in the project's data structure. If not, they are removed.
     * @return the number of removals
     */
    public int cleanupDeadNodes() {
        int removals = 0;
        for (ScorePage page : this.pages) {
            ArrayList<Element> toBeRemoved = new ArrayList<>();
            for (Element element : page.getAllEntries().keySet()) {     // for each element that is linked in this score page
                if (element.getLocalName().equals("note"))              // notes cannot be removed since we are not allowed to edit the MSM
                    continue;
                if ((this.parentProject.getMpm() != null)               // if we have an MPM document
                        && (element.getDocument() == this.parentProject.getMpm().getDocument())) // if the element is still linked in the MPM document
                    continue;                                           // we keep it
                toBeRemoved.add(element);                               // otherwise it is dd and should be removed from the score
            }
            removals += toBeRemoved.size();
            for (Element element : toBeRemoved) {
                page.removeEntry(element);
            }
        }
        return removals;
    }

    /**
     * export the XML code for the project data file
     * @return
     */
    public Element toXml() {
        Element scores = new Element("score");
        scores.addAttribute(new Attribute("overlayElementSize", Integer.toString(this.overlayElementSize)));

        for (ScorePage page : this.pages) {         // for each page
            Element pageElt = page.toXml();         // make an XML element for this page
            Helper.getAttribute("file", pageElt).setValue(Paths.get(this.parentProject.getFile().getParent()).relativize(page.getFile().toPath()).toString());  // replace the absolute file path attribute by a relative path
            scores.appendChild(pageElt);            // add it to the score element
        }

        return scores;
    }
}
