package mpmToolbox.gui;

import com.alee.laf.WebLookAndFeel;
import com.alee.laf.button.WebButton;
import com.alee.laf.label.WebLabel;
import com.alee.laf.panel.WebPanel;
import com.alee.laf.window.WebFrame;
import com.alee.skin.dark.WebDarkSkin;
import meico.audio.Audio;
import meico.mei.Mei;
import meico.midi.Midi;
import meico.mpm.Mpm;
import meico.msm.Msm;
import meico.xml.XmlBase;
import mpmToolbox.Main;
import mpmToolbox.supplementary.Tools;
import nu.xom.ParsingException;
import org.xml.sax.SAXException;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.xml.parsers.ParserConfigurationException;
import java.awt.*;
import java.io.File;
import java.io.IOException;

/**
 * This is the host application for all GUI and functionality.
 * @author Axel Berndt
 */
public class MpmToolbox {
    private WebFrame frame = null;              // the main window frame
    private ProjectPane projectPane = null;     // the gui and project data that is worked on here
    private WebPanel welcomeMessage;

    /**
     * constructor
     */
    public MpmToolbox() {
        // make the main window
        SwingUtilities.invokeLater (new Runnable() {
            public void run() {
                MpmToolbox self = MpmToolbox.this;

                WebLookAndFeel.install(WebDarkSkin.class);                                          // Install WebLaF as application L&F
//                WebLookAndFeel.install(WebLightSkin.class);
                self.frame = new WebFrame<>("MPM Toolbox v" + Main.version);
                self.frame.setIconImage(Toolkit.getDefaultToolkit().getImage(this.getClass().getResource(Settings.icon)));
                self.frame.setSize(1500, 1000);
//                frame.setSize(new Dimension(Toolkit.getDefaultToolkit().getScreenSize()));          // fullscreen
//                frame.setPreferredSize(new Dimension(Toolkit.getDefaultToolkit().getScreenSize()));
                self.frame.setResizable(true);
                self.frame.setLocationRelativeTo(null);
                Settings.foregroundColor = self.frame.getForeground();                              // place the foreground color info in Settings so other classes can read it from there

                Tools.initKeyboardShortcuts(self, self.frame);
                self.frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);                 // terminate everything, all windows
//                self.frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);             // close this window/frame, others remain open
                Runtime.getRuntime().addShutdownHook(new Thread(() -> shutdownHook()));             // what must be done on shutdown will be done via method shutdownHook()

                self.welcomeMessage = self.makeWelcomeMessage();
                self.frame.add(self.welcomeMessage);
                Tools.fileDrop(self, self.welcomeMessage);

//                self.frame.pack();                      // this fits the size of the frame to its content
                self.frame.setVisible(true);
            }
        });
    }

    /**
     * Do all that must be done before shutdown.
     */
    private void shutdownHook() {
        Settings.writeSettingsFile();   // Save the current state of the settings to the settings file
    }

    /**
     * a getter for the main window frame
     * @return
     */
    public WebFrame getFrame() {
        return this.frame;
    }

    /**
     * getter for the project pane holding the project data and all gui
     * @return
     */
    public ProjectPane getProjectPane() {
        return this.projectPane;
    }

    /**
     * the startup window
     * @return
     */
    private WebPanel makeWelcomeMessage() {
        WebPanel welcomeMessage = new WebPanel();
        BoxLayout boxLayout = new BoxLayout(welcomeMessage, BoxLayout.Y_AXIS);
        welcomeMessage.setLayout(boxLayout);
        welcomeMessage.setPadding(20);
        welcomeMessage.add(Box.createVerticalGlue());                                  // this is needed for vertical centering
        final WebLabel message = new WebLabel("<html><center><p>Welcome to MPM Toolkit.<br>Create a new project or open an existing one by dropping a file (.mpr, .mei, .msm) here or pressing CTRL+O.<br>In case of MEI, make sure to use expansions to encode repetitions, da capi etc.", WebLabel.CENTER);
        message.setAlignmentX(Component.CENTER_ALIGNMENT);
        message.setPadding(20, 20, 10, 20);
        message.setFontSize(16);
        message.setForeground(Color.LIGHT_GRAY);
        welcomeMessage.add(message);
        if (!Settings.recentOpened.isEmpty()) {
            message.setText(message.getText() + "<br><br>Or open a recent project:");
            for (int i = 0; i < Settings.recentOpened.size(); ++i) {
                if (Settings.recentOpened.get(i).exists()) {
                    File project = Settings.recentOpened.get(i);
                    WebButton openRecent = new WebButton("<html><center>" + Settings.recentOpened.get(i).getName() + "</center></html>");
                    openRecent.setAlignmentX(Component.CENTER_ALIGNMENT);
                    openRecent.setPadding(10);
                    openRecent.addActionListener(actionEvent -> loadFile(project));
                    welcomeMessage.add(openRecent);
                }
            }
        }
        message.setText(message.getText() + "</p></center></html>");
        welcomeMessage.add(Box.createVerticalGlue());                                  // this is needed for vertical centering
        return welcomeMessage;
    }

    /**
     * This triggers an open file dialog.
     */
    public void openFile() {
        final JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Open File");

        if (this.projectPane != null)
            fileChooser.setCurrentDirectory(this.projectPane.getMsm().getFile());

        if (fileChooser.showOpenDialog(this.frame) == 0) {                      // a file has been selected
            this.loadFile(fileChooser.getSelectedFile());
        }
    }

    /**
     * load a file into the project or open a new project (depends on the file)
     * @param file
     * @return success of opening the project
     */
    public boolean loadFile(File file) {
        System.out.println("Loading file " + file.getAbsolutePath());

        int index = file.getName().lastIndexOf(".");
        if (index < 0) {                                                                // if the file has no extension
            System.err.println("File type is not supported by MPM Toolbox.");
            return false;
        }

        String extension = file.getName().substring(index).toLowerCase();               // get the file extension
        ProjectPane projectPane = null;
        try {
            switch (extension) {
                case ".xml":                                                            // xml could be anything
                    XmlBase xml = new XmlBase(file);                                    // parse the xml file into an instance of XmlBase
                    switch (xml.getRootElement().getLocalName()) {                      // get the name of the root element in the xml tree
                        case "mpmToolkitProject":                                       // seems to be an MPM Toolkit project
                            projectPane = new ProjectPane(file, this.frame);
                            if (this.openProject(projectPane))
                                Tools.fileDrop(this, projectPane);
                            break;
                        case "mei":                                                     // seems to be an mei
                            Mei mei = new Mei(xml.getDocument());
                            mei.setFile(xml.getFile());
                            projectPane = new ProjectPane(mei, this.frame);
                            if (this.openProject(projectPane))
                                Tools.fileDrop(this, projectPane);
                            break;
                        case "msm":                                                     // seems to be an msm
                            Msm msm = new Msm(xml.getDocument());
                            msm.setFile(xml.getFile());
                            projectPane = new ProjectPane(msm, this.frame);
                            if (this.openProject(projectPane))
                                Tools.fileDrop(this, projectPane);
                            break;
                        case "mpm":                                                     // seems to be an mpm
                            if (this.projectPane == null)
                                System.err.println("No project loaded to add the MPM.");
                            else {
                                this.projectPane.setMpm(new Mpm(xml.getDocument()));
                                this.projectPane.getScore().cleanupDeadNodes();
                                this.projectPane.repaintScoreDisplay();
                            }
                            break;
                        default:
                            throw new IOException("MPM Toolbox cannot identify the type of data in this XML file as one of its supported types.");
                    }
                    break;
                case ".mpr":                                                            // this is the MPM Toolbox project extension
                    projectPane = new ProjectPane(file, this.frame);
                    if (this.openProject(projectPane))
                        Tools.fileDrop(this, projectPane);
                    break;
                case ".mei":
                    projectPane = new ProjectPane(new Mei(file), this.frame);
                    if (this.openProject(projectPane))
                        Tools.fileDrop(this, projectPane);
                    break;
                case ".msm":
                    projectPane = new ProjectPane(new Msm(file), this.frame);
                    if (this.openProject(projectPane))
                        Tools.fileDrop(this, projectPane);
                    break;
                case ".mpm":
                    if (this.projectPane == null)
                        System.err.println("No project loaded to add the MPM.");
                    else {
                        this.projectPane.setMpm(new Mpm(file));
                        this.projectPane.getScore().cleanupDeadNodes();
                        this.projectPane.repaintScoreDisplay();
                    }
                    break;
                case ".mid":
                    projectPane = new ProjectPane(new Midi(file), this.frame);
                    if (this.openProject(projectPane))
                        Tools.fileDrop(this, projectPane);
                    break;
                case ".wav":
                case ".mp3":
                    if (this.projectPane == null)
                        System.err.println("No project loaded to add the audio.");
                    else
                        this.projectPane.addAudio(new Audio(file));
                    break;
                case ".jpg":
                case ".jpeg":
                case ".png":
                case ".gif":
                case ".bmp":
//                case ".tif":    // TODO: can I support this format?
//                case ".svg":    // TODO: can I support this format?
                    if (this.projectPane == null)
                        System.err.println("No project loaded to add the image.");
                    else
                        this.projectPane.addScorePage(file);
                    break;
                case ".dls":
                case ".sf2":
                    Settings.setSoundbank(file);
                    break;
                default:
                    throw new IOException("File type " + extension + " is not supported by MPM Toolbox.");
            }
        } catch (SAXException | ParsingException | ParserConfigurationException | IOException | InvalidMidiDataException | UnsupportedAudioFileException e) {
            e.printStackTrace();
            return false;
        }

        System.out.print("\n");
        return true;
    }

    /**
     * the closing procedure for an open project
     */
    public void closeProject() {
        this.frame.remove(this.projectPane);    // remove the ProjectPane component from this frame
        this.projectPane = null;
    }

    /**
     * the opening procedure
     * @param project
     * @return did we open a new project?
     */
    private boolean openProject(ProjectPane project) {
        // opening a new project requires to close the current one, ask for closing
        if (this.projectPane != null) {
            Object[] options = {"Yes, close", "Yes, save first", "No"};
            switch (JOptionPane.showOptionDialog(this.frame, "Do want to close the current project and open a new one?", "Confirm to Close Current Project", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[2])) {
                case 0:     // yes
                    this.closeProject();
                    break;
                case 1:     // yes, but save first
                    this.saveProject();
                    this.closeProject();
                    break;
                case 2:     // no
                default:
                    return false;
            }
        }

        // open the new project
        this.projectPane = project;
        this.frame.remove(this.welcomeMessage);
        this.frame.add(this.projectPane);
        if (this.projectPane.getFile() != null)
            Settings.recentOpened.add(this.projectPane.getFile());
        return true;

        // here's a threaded alternative for the above code
//        MainApplication itsMe = this;       // needed in the following thread
//        ProjectPane newProject = project;
//        SwingUtilities.invokeLater (new Runnable() {
//            public void run() {
//                // opening a new project requires to close the current one, ask for closing
//                if (itsMe.project != null) {
//                    Object[] options = {"Yes, close", "Yes, save first", "No"};
//                    switch (JOptionPane.showOptionDialog(itsMe.frame, "Do want to close the current project and open a new one?", "Confirm to Close Current Project", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[2])) {
//                        case 0:     // yes
//                            itsMe.closeProject();
//                            break;
//                        case 1:     // yes, but save first
//                            itsMe.saveProject();
//                            itsMe.closeProject();
//                            break;
//                        case 2:     // no
//                        default:
//                            return;
//                    }
//                }
//                // open the new project
//                itsMe.project = newProject;
//                itsMe.frame.add(itsMe.project);
//                if (itsMe.project.getFile() != null)
//                    Settings.recentOpened.add(itsMe.project.getFile());
//            }
//        });
    }

    /**
     * save the project to the file system
     * @return
     */
    public boolean saveProject() {
        if (this.projectPane == null)
            return false;

        if (!this.projectPane.saveProject()) {
            return this.saveProjectAs();
        } else {
            Settings.recentOpened.add(this.projectPane.getFile());
        }
        return true;
    }

    /**
     * This method triggers the Save As dialog.
     * @return
     */
    public boolean saveProjectAs() {
        final JFileChooser fileChooser = new JFileChooser();
        FileFilter[] ff = fileChooser.getChoosableFileFilters();
        for (FileFilter f : ff)
            fileChooser.removeChoosableFileFilter(f);

        fileChooser.setDialogTitle("Save Project As");
        fileChooser.setMultiSelectionEnabled(false);
        fileChooser.addChoosableFileFilter(new FileNameExtensionFilter("MPM Toolbox Project", "mpr"));
        fileChooser.addChoosableFileFilter(new FileNameExtensionFilter("XML", "xml"));
        fileChooser.setCurrentDirectory(this.projectPane.getMsm().getFile());
        if (fileChooser.showSaveDialog(this.frame) == 0) {                      // a file has been selected
            if (this.projectPane.saveProjectAs(fileChooser.getSelectedFile())) {    // if save procedure succeeded
                Settings.recentOpened.add(this.projectPane.getFile());
                return true;
            }
        }
        return false;
    }
}
