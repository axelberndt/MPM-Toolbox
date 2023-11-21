package mpmToolbox.projectData;

import meico.mei.Helper;
import meico.mei.Mei;
import meico.midi.Midi;
import meico.mpm.Mpm;
import meico.msm.Msm;
import meico.supplementary.KeyValue;
import meico.xml.XmlBase;
import mpmToolbox.projectData.audio.Audio;
import mpmToolbox.projectData.score.Score;
import mpmToolbox.projectData.score.ScorePage;
import mpmToolbox.supplementary.Tools;
import nu.xom.*;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.xml.sax.SAXException;

import javax.imageio.ImageIO;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.xml.parsers.ParserConfigurationException;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * The data structure of an MPM Toolbox project.
 * @author Axel Berndt
 */
public class ProjectData {
    private XmlBase xml = null;                                 // the XML representation of the project data
    private final Msm msm;                                      // the MSM document
    private Mpm mpm = null;                                     // the MPM document
    private final Score score;                                  // the music sheets
    private final ArrayList<Audio> audio = new ArrayList<>();   // a list of audio recordings

    /**
     * constructor
     * @param msm
     */
    public ProjectData(Msm msm) {
        this.msm = msm;
        this.msmPreprocessing();
        this.score = new Score(this);
        this.init();
    }

    /**
     * constructor, the MIDI input is converted to MSM
     * @param midi
     */
    public ProjectData(Midi midi) {
        this(midi.exportMsm());
    }

    /**
     * constructor, the MEI input is converted to MSM and MPM;
     * if the MEI has more than one mdiv, only the first is converted to MSM and MPM;
     * use mei.exportMsmMpm() directly and choose the desired MSM from the output to instantiate a ProgramData object.
     * @param mei
     */
    public ProjectData(Mei mei) {
        KeyValue<List<Msm>, List<Mpm>> msmMpm = mei.exportMsmMpm(720);
        this.msm = msmMpm.getKey().get(0);
        this.msmPreprocessing();
        this.setMpm(msmMpm.getValue().get(0));
        this.score = new Score(this);
        this.init();
    }

    /**
     * constructor, instantiate a project from a project file
     * @param file
     */
    public ProjectData(File file) throws SAXException, ParsingException, ParserConfigurationException, IOException {
        this.xml = new XmlBase(file);
        String basePath = this.xml.getFile().getParent() + File.separator;

        String localMsmPath = this.xml.getRootElement().getFirstChildElement("msm").getAttributeValue("file");
        this.msm = new Msm(new File(Tools.uniformPath(basePath + localMsmPath)));
        this.msmPreprocessing();

        Element e = this.xml.getRootElement().getFirstChildElement("mpm");
        if (e != null) {
            String localMpmPath = e.getAttributeValue("file");
            this.setMpm(new Mpm(new File(Tools.uniformPath(basePath + localMpmPath))));
        }

        this.score = new Score(this);

        e = this.xml.getRootElement().getFirstChildElement("audios");
        if (e != null) {
            Elements audios = e.getChildElements("audio");
            for (int i=0; i < audios.size(); ++i) {
                Element projectAudioData = audios.get(i);
                try {
                    this.addAudio(new Audio(projectAudioData, basePath, this.getMsm()));
                } catch (UnsupportedAudioFileException | IOException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    /**
     * This method is a helper to initialize a ProjectData instance with file paths pointing to
     * the source directory and filename from where it was initialized.
     */
    private void init() {
        if ((this.msm == null) || (this.msm.getFile() == null))
            return;

        String projectFilePath = Helper.getFilenameWithoutExtension(this.msm.getFile().getAbsolutePath()) + ".mpr";

        this.xml = new XmlBase();
        this.xml.setFile(projectFilePath);

        Element root = new Element("mpmToolboxProject");
        Document xml = new Document(root);
        this.xml.setDocument(xml);

        // link msm
        Element msmElt = new Element("msm");
        Path relativeMsmPath = Paths.get(this.xml.getFile().getParent()).relativize(this.msm.getFile().toPath());
        msmElt.addAttribute(new Attribute("file", relativeMsmPath.toString()));
        root.appendChild(msmElt);

        String basePath = this.xml.getFile().getAbsoluteFile().getParent() + File.separator;

        // link mpm
        if (this.mpm != null) {
            // if necessary, also set the path of the mpm file
            if (this.mpm.getFile() == null)
                this.mpm.setFile(Helper.getFilenameWithoutExtension(this.xml.getFile().getAbsolutePath()) + ".mpm");
            else if (!this.mpm.getFile().exists())
                this.mpm.setFile(basePath + this.mpm.getFile().getName());
            Element mpmElt = new Element("mpm");
            Path relativeMpmPath = Paths.get(this.xml.getFile().getParent()).relativize(mpm.getFile().toPath());
            mpmElt.addAttribute(new Attribute("file", relativeMpmPath.toString()));
            root.appendChild(mpmElt);
        }

        // link the score sources
        if (!this.score.isEmpty()) {
            root.appendChild(this.score.toXml());
        }

        // link the audio sources
        if (!this.audio.isEmpty()) {
            Element audios = new Element("audios");
            root.appendChild(audios);
            for (Audio aud : this.audio) {
//                Path src = aud.getFile().toPath();
//                Path dest = Paths.get(path + "\\data\\score\\" + aud.getFile().getName());
//                try {
//                    Files.copy(src, dest, StandardCopyOption.REPLACE_EXISTING);
//                } catch (IOException e) {
//                    e.printStackTrace();
//                    continue;
//                }

//                Element audioElt = new Element("audio");
//                Path relativeAudioPath = Paths.get(file.getParent()).relativize(aud.getFile().toPath());
//                audioElt.addAttribute(new Attribute("file", relativeAudioPath.toString()));

                Element audioElt = aud.toXml(Paths.get(this.xml.getFile().getParent()));
                audios.appendChild(audioElt);
            }
        }
    }

    /**
     * get the project file
     * @return
     */
    public File getFile() {
        if (this.xml == null)
            return null;
        return this.xml.getFile();
    }

    /**
     * access the XML code of the project data structure
     * @return
     */
    public XmlBase getXml() {
        return this.xml;
    }

    /**
     * some cleanup to optimize the MSM data
     */
    private void msmPreprocessing() {
        this.msm.removeRests();
        this.msm.deleteEmptyMaps();
//        this.msm.resolveSequencingMaps();   // if repetitions, da capi etc. were encoded as expansions, this should not be invoked
        this.msm.addIds();
    }

    /**
     * access the Msm object
     * @return
     */
    public synchronized Msm getMsm() {
        return msm;
    }

    /**
     * add an MPM to the Project
     * @param mpm
     */
    public synchronized void setMpm(Mpm mpm) {
        if (this.mpm != null) {
            this.mpm = null;
            this.score.cleanupDeadNodes();
        }
        this.mpm = mpm;
    }

    /**
     * delete the MPM from this project
     */
    public synchronized void removeMpm() {
        this.mpm = null;
        this.score.cleanupDeadNodes();
    }

    /**
     * access the Mpm object
     * @return
     */
    public synchronized Mpm getMpm() {
        return this.mpm;
    }

    /**
     * access the list of score pages
     * @return
     */
    public synchronized Score getScore() {
        return score;
    }

    /**
     * add a file to the list of score pages
     * @param file
     */
    public synchronized ScorePage addScorePage(File file) {
        return this.score.addPage(file);
    }

    /**
     * This extracts each page of the input PDF file, stores them as PNG in the input file's directory
     * and adds those PNGs to the score.
     * @param pdf
     * @return an arraylist of the score pages just added
     */
    public ArrayList<ScorePage> addScorePdf(File pdf) {
        PDDocument document;
        try {
            document = PDDocument.load(pdf);
        } catch (IOException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }

        // create a subfolder with the filename that will contain the PNG files named page_000.png, page_001.png and so on
        String directory = Helper.getFilenameWithoutExtension(pdf.getAbsolutePath()) + File.separator;
        if (!Files.exists(Paths.get(directory))) {                                      // if the path does not exist, yet
            try {
                Files.createDirectory(Paths.get(directory));                            // create it in the file system
            } catch (IOException e) {                                                   // if creation failed
                e.printStackTrace();
                directory = Helper.getFilenameWithoutExtension(pdf.getAbsolutePath()) + "-";    // place the image files in the folder of the PDF file and name them accordingly
            }
        }
        String imagePath = directory + "page";

        ArrayList<ScorePage> pages = new ArrayList<>();
        PDFRenderer renderer = new PDFRenderer(document);

        for (int pageNumber = 0; pageNumber < document.getNumberOfPages(); ++pageNumber) {
            String imageFilePath = imagePath + "_" +  String.format("%03d", pageNumber) + ".png";   // for the page numbering use number formatting with leading zeros
            File image;
            try {
                BufferedImage bim = renderer.renderImageWithDPI(pageNumber, 300);   // with 300dpi we follow the DFG practical guidelines on digitisation (https://www.dfg.de/formulare/12_151/12_151_en.pdf)
                image = new File(imageFilePath);
                ImageIO.write(bim, "png", image);
            } catch (IOException e) {
                e.printStackTrace();
                continue;
            }
            ScorePage page = this.score.addPage(image);
            if (page != null)
                pages.add(page);
        }

        try {
            document.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return pages;
    }

    /**
     * remove a score file from the project
     * @param index
     */
    public synchronized void removeScorePage(int index) {
        this.score.removePage(index);
    }

    /**
     * access the list of Audio objects
     * @return
     */
    public synchronized ArrayList<Audio> getAudio() {
        return audio;
    }

    /**
     * add an Audio object to the list of audios
     * @param audio
     */
    public synchronized boolean addAudio(Audio audio) {
        if (audio == null)
            return false;
        if (this.audio.contains(audio))
            return false;
        return this.audio.add(audio);
    }

    /**
     * remove an audio file from the project
     * @param index
     */
    public synchronized void removeAudio(int index) {
        this.audio.remove(index);
    }

    /**
     * Save the project under its already defined filename. If it is not defined (in this.xml) it returns false.
     * @return
     */
    public boolean saveProject() {
        return this.saveProjectAs(this.getFile());
    }

    /**
     * This saves the project in an .mpr file, basically an xml file which stores relative paths to all other project files.
     * If the MSM or MPM file was not existent in the file system, they will be created in the directory.
     * @param file
     * @return
     */
    public boolean saveProjectAs(File file) {
        if (file == null)
            return false;

        this.xml = new XmlBase();
        this.xml.setFile(file);

        Element root = new Element("mpmToolboxProject");
        Document xml = new Document(root);
        this.xml.setDocument(xml);

        String basePath = file.getAbsoluteFile().getParent() + File.separator;

        // store MSM
        if (!this.msm.getFile().exists())
            this.msm.setFile(basePath + this.msm.getFile().getName());
        this.msm.writeMsm();
        Element msmElt = new Element("msm");
        Path relativeMsmPath = Paths.get(file.getParent()).relativize(this.msm.getFile().toPath());
        msmElt.addAttribute(new Attribute("file", relativeMsmPath.toString()));
        root.appendChild(msmElt);

        // store MPM
        if (this.mpm != null) {
            if (this.mpm.getFile() == null)
                this.mpm.setFile(Helper.getFilenameWithoutExtension(file.getAbsolutePath()) + ".mpm");
            else if (!this.mpm.getFile().exists())
                this.mpm.setFile(basePath + this.mpm.getFile().getName());
            this.mpm.writeMpm();
            Element mpmElt = new Element("mpm");
            Path relativeMpmPath = Paths.get(file.getParent()).relativize(mpm.getFile().toPath());
            mpmElt.addAttribute(new Attribute("file", relativeMpmPath.toString()));
            root.appendChild(mpmElt);
        }

        // store the score sources
        if (!this.score.isEmpty()) {
            root.appendChild(this.score.toXml());
        }

        // store the audio sources
        if (!this.audio.isEmpty()) {
            Element audios = new Element("audios");
            root.appendChild(audios);
            for (Audio aud : this.audio) {
//                Path src = aud.getFile().toPath();
//                Path dest = Paths.get(path + "\\data\\score\\" + aud.getFile().getName());
//                try {
//                    Files.copy(src, dest, StandardCopyOption.REPLACE_EXISTING);
//                } catch (IOException e) {
//                    e.printStackTrace();
//                    continue;
//                }

//                Element audioElt = new Element("audio");
//                Path relativeAudioPath = Paths.get(file.getParent()).relativize(aud.getFile().toPath());
//                audioElt.addAttribute(new Attribute("file", relativeAudioPath.toString()));

                Element audioElt = aud.toXml(Paths.get(file.getParent()));
                audios.appendChild(audioElt);
            }
        }

        return this.xml.writeFile();
    }
}
