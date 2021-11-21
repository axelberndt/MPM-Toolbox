package mpmToolbox.gui;

import com.alee.api.data.CompassDirection;
import com.alee.extended.dock.SidebarButtonVisibility;
import com.alee.extended.dock.WebDockableFrame;
import com.alee.extended.dock.WebDockablePane;
import com.alee.extended.tab.DocumentData;
import com.alee.extended.tab.WebDocumentPane;
import com.alee.laf.panel.WebPanel;
import com.alee.managers.style.StyleId;
import meico.mei.Mei;
import meico.midi.Midi;
import meico.midi.MidiPlayer;
import meico.mpm.Mpm;
import meico.mpm.elements.Performance;
import meico.msm.Msm;
import mpmToolbox.projectData.Audio;
import mpmToolbox.projectData.ProjectData;
import mpmToolbox.gui.audio.AudioDocumentData;
import mpmToolbox.gui.mpmTree.MpmDockableFrame;
import mpmToolbox.gui.mpmTree.MpmTree;
import mpmToolbox.gui.msmTree.MsmTree;
import mpmToolbox.projectData.score.Score;
import mpmToolbox.gui.score.ScoreDocumentData;
import mpmToolbox.projectData.score.ScorePage;
import mpmToolbox.gui.syncPlayer.SyncPlayer;
import nu.xom.ParsingException;
import org.xml.sax.SAXException;

import javax.sound.midi.MidiUnavailableException;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

/**
 * A GUI pane wrapper class around ProjectData.
 * @author Axel Berndt
 */
public class ProjectPane extends WebDockablePane {
    private final MpmToolbox parent;

    private MidiPlayer midiPlayer = null;
    private final ProjectData data;                                                         // the actual project data

    private final WebDocumentPane<DocumentData<WebPanel>> tabs = new WebDocumentPane<>();   // this contains the components displayed in the center under certain tabs

    private final MsmTree msmTree;
    private final MpmDockableFrame mpmDockableFrame;
    private final WebDockableFrame playerFrame = new WebDockableFrame("playerFrame", "Sync Player");
    private SyncPlayer syncPlayer = null;
    private ScoreDocumentData scoreFrame = null;
    private AudioDocumentData audioFrame = null;

    /**
     * constructor
     * @param msm
     * @param parent
     */
    public ProjectPane(Msm msm, MpmToolbox parent) {
        super();
        this.parent = parent;
        this.data = new ProjectData(msm);
        this.msmTree = new MsmTree(this);
        this.mpmDockableFrame = new MpmDockableFrame(this);
        this.initMidiPlayer();
        this.makeGUI();
    }

    /**
     * constructor, the MIDI input is converted to MSM
     * @param midi
     * @param parent
     */
    public ProjectPane(Midi midi, MpmToolbox parent) {
        super();
        this.parent = parent;
        this.data = new ProjectData(midi.exportMsm());
        this.msmTree = new MsmTree(this);
        this.mpmDockableFrame = new MpmDockableFrame(this);
        this.initMidiPlayer();
        this.makeGUI();
    }

    /**
     * constructor, the MEI input is converted to MSM and MPM;
     * if the MEI has more than one mdiv, only the first is converted to MSM and MPM;
     * use mei.exportMsmMpm() directly and choose the desired MSM from the output to instantiate a ProgramData object.
     * @param mei
     * @param parent
     */
    public ProjectPane(Mei mei, MpmToolbox parent) {
        super();
        this.parent = parent;
        this.data = new ProjectData(mei);
        this.msmTree = new MsmTree(this);
        this.mpmDockableFrame = new MpmDockableFrame(this);
        this.initMidiPlayer();
        this.makeGUI();
    }

    /**
     * constructor, instantiate a project from a project file
     * @param file
     * @param parent
     */
    public ProjectPane(File file, MpmToolbox parent) throws SAXException, ParsingException, ParserConfigurationException, IOException {
        super();
        this.parent = parent;
        this.data = new ProjectData(file);
        this.msmTree = new MsmTree(this);
        this.mpmDockableFrame = new MpmDockableFrame(this);
        this.initMidiPlayer();
        this.makeGUI();
    }

    /**
     * initialize MIDI player
     * @return
     */
    private boolean initMidiPlayer() {
        try {
            this.midiPlayer = new MidiPlayer();
        } catch (MidiUnavailableException e) {
            e.printStackTrace();
            return false;
        }

        if (Settings.getSoundbank() != null)
            this.midiPlayer.loadSoundbank(Settings.getSoundbank());
        else
            this.midiPlayer.loadDefaultSoundbank();

        return true;
    }

    /**
     * a getter for the parent MPM Toolbox
     * @return
     */
    public MpmToolbox getParentMpmToolbox() {
        return this.parent;
    }

    /**
     * a getter for the MIDI player
     * @return
     */
    public MidiPlayer getMidiPlayer() {
        return this.midiPlayer;
    }

    /**
     * a helper method for the constructors to generate the GUI
     */
    private void makeGUI() {
//        this.registerSettings(new Configuration("MyDockablePane", "state"));    // save (MyDockablePane.xml in user home directory if not set different) and reproduce the state on restart (https://github.com/mgarin/weblaf/wiki/How-to-use-WebDockablePane#saverestore-state)
        this.setStyleId(StyleId.dockablepaneCompact);                       // a more compact styling of the dockable pane elements
        this.setSidebarButtonVisibility(SidebarButtonVisibility.always);

        this.makePlayerFrame();                                             // the Sync Player in a dockable pane

        this.addFrame(this.msmTree.getDockableFrame());                     // add the MSM tree to the UI
        this.addFrame(this.mpmDockableFrame);                               // the MPM tree is displayed in a dockable pane

        // fill the content pane in the center
//        this.tabs.openDocument(new DocumentData<>("TestTab", "Test Tab", new WebButton("Test")));
        this.tabs.openDocument(this.makeScoreFrame());
        this.tabs.openDocument(this.makeAudioFrame());
        this.tabs.setSelected(this.scoreFrame);

        this.setContent(this.tabs);     // this will fill the free space of the docking pane that is not occupied by a WebDockableFrame, this can be anything JComponent-based
    }

    /**
     * the Sync Player frame
     */
    private void makePlayerFrame() {
//        this.playerFrame.setIcon(Icons.table);
        this.playerFrame.setClosable(false);                                   // when closed the frame disappears and cannot be reopened by the user, thus, this is set false
        this.playerFrame.setMaximizable(false);                                // it is also set to not maximizable
        this.playerFrame.setPosition(CompassDirection.south);

        try {
            this.syncPlayer = new SyncPlayer(this);
        } catch (MidiUnavailableException e) {
            e.printStackTrace();
            return;
        }

        this.playerFrame.add(this.syncPlayer);
        this.addFrame(this.playerFrame);
    }

    /**
     * this method sets up the scoreDockFrame
     * @return
     */
    private ScoreDocumentData makeScoreFrame() {
        this.scoreFrame = new ScoreDocumentData(this);
        return this.scoreFrame;
    }

    /**
     * this method sets up the audioDockFrame
     * @return
     */
    private AudioDocumentData makeAudioFrame() {
        this.audioFrame = new AudioDocumentData(this);
        return this.audioFrame;
    }

    /**
     * provides access to the audio analysis frame
     * @return
     */
    public AudioDocumentData getAudioFrame() {
        return this.audioFrame;
    }

    /**
     * access the project data
     * @return
     */
    public ProjectData getProjectData() {
        return this.data;
    }

    /**
     * get the project file
     * @return
     */
    public File getFile() {
        return this.data.getFile();
    }

    /**
     * access the Msm object
     * @return
     */
    public Msm getMsm() {
        return this.data.getMsm();
    }

    /**
     * access the MSM tree
     * @return
     */
    public MsmTree getMsmTree() {
        return this.msmTree;
    }

    /**
     * add an MPM to the Project
     * @param mpm
     */
    public void setMpm(Mpm mpm) {
        this.getProjectData().setMpm(mpm);
        this.mpmDockableFrame.setMpm(mpm);

        for (Performance performance : mpm.getAllPerformances())
            this.syncPlayer.addPerformance(performance);
    }

    /**
     * delete the MPM from this project
     */
    public void removeMpm() {
        if (this.getMpm() == null)
            return;
        this.mpmDockableFrame.removeMpm();
        this.data.removeMpm();
        this.repaintScoreDisplay();
    }

    /**
     * access the Mpm object
     * @return
     */
    public Mpm getMpm() {
        return this.data.getMpm();
    }

    /**
     * a getter for the MPM tree
     * @return
     */
    public MpmTree getMpmTree() {
        return this.mpmDockableFrame.getMpmTree();
    }

    /**
     * access the list of score pages
     * @return
     */
    public Score getScore() {
        return this.data.getScore();
    }

    /**
     * access the score frame
     * @return
     */
    public ScoreDocumentData getScoreFrame() {
        return scoreFrame;
    }

    /**
     * access the sync player
     * @return
     */
    public SyncPlayer getSyncPlayer() {
        return this.syncPlayer;
    }

    /**
     * repaint the score display
     */
    public void repaintScoreDisplay() {
        if (this.getScoreFrame().getScoreDisplay() != null)
            this.getScoreFrame().getScoreDisplay().repaint();
    }

    /**
     * add a file to the list of score pages
     * @param file
     */
    public void addScorePage(File file) {
        if (this.data.addScorePage(file) != null)
            this.scoreFrame.addScorePage(file);
    }

    /**
     * add the contents of a PDF file to the score pages; the PDF's pages will be extracted and stored as PNGs
     * @param pdf
     */
    public void addScorePdf(File pdf) {
        for (ScorePage scorePage : this.data.addScorePdf(pdf))
            this.scoreFrame.addScorePage(scorePage.getFile());
    }

    /**
     * remove a score file from the project
     * @param index
     */
    public void removeScorePage(int index) {
        if (this.getScore().isEmpty())
            return;

        this.data.removeScorePage(index);                       // delete the page from the project data structure
        this.scoreFrame.removeScorePage(index);
    }

    /**
     * access the list of Audio objects
     * @return
     */
    public ArrayList<mpmToolbox.projectData.Audio> getAudio() {
        return this.data.getAudio();
    }

    /**
     * add an Audio object to the list of audios
     * @param audio
     */
    public void addAudio(mpmToolbox.projectData.Audio audio) {
        if (this.data.addAudio(audio)) {
            this.syncPlayer.addAudio(audio);
        }
    }

    /**
     * remove an audio file from the project
     * @param index
     */
    public void removeAudio(int index) {
        Audio audio = this.getAudio().get(index);
        this.syncPlayer.removeAudio(audio);
        this.data.removeAudio(index);
    }

    /**
     * Save the project under its already defined filename. If it is not defined (in this.xml) it returns false.
     * @return
     */
    public boolean saveProject() {
        return this.data.saveProject();
    }

    /**
     * This saves the project in an .mpr file, basically an xml file which stores relative paths to all other project files.
     * If the MSM or MPM file was not existent in the file system, they will be created in the directory.
     * @param file
     * @return
     */
    public boolean saveProjectAs(File file) {
        return this.data.saveProjectAs(file);
    }
}
