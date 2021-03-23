package mpmToolbox.gui;

import com.alee.extended.image.WebImage;
import com.alee.laf.WebLookAndFeel;
import com.alee.laf.label.WebLabel;
import com.alee.laf.menu.WebMenu;
import com.alee.laf.menu.WebMenuBar;
import com.alee.laf.menu.WebMenuItem;
import com.alee.laf.panel.WebPanel;
import com.alee.laf.window.WebFrame;
import com.alee.skin.dark.WebDarkSkin;
import meico.audio.Audio;
import meico.mei.Mei;
import meico.midi.Midi;
import meico.mpm.Mpm;
import meico.mpm.elements.Performance;
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
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;

/**
 * This is the host application for all GUI and functionality.
 * @author Axel Berndt
 */
public class MpmToolbox {
    private WebFrame frame = null;              // the main window frame
    private ProjectPane projectPane = null;     // the gui and project data that is worked on here
    private WebPanel welcomeMessage;
    private WebMenu openRecent;
    private WebMenuItem close;
    private WebMenuItem save;
    private WebMenuItem saveAs;
    private WebMenu export;
    private WebMenuItem playStop;

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

                // assign the icon set to the frame
//                self.frame.setIconImage(Toolkit.getDefaultToolkit().getImage(this.getClass().getResource(Settings.icon)));
                ArrayList<Image> icons = new ArrayList<>();
                for (String resource : Settings.icons)
                    icons.add(Toolkit.getDefaultToolkit().getImage(this.getClass().getResource(resource)));
                self.frame.setIconImages(icons);

                self.frame.setSize(1500, 1000);                                                     // initial size in windowed mode
                self.frame.setExtendedState(Frame.MAXIMIZED_BOTH);                                  // toggle fullscreen mode
//                self.frame.setSize(new Dimension(Toolkit.getDefaultToolkit().getScreenSize()));     // an alternative way to set the window size to fullscreen
//                frame.setPreferredSize(new Dimension(Toolkit.getDefaultToolkit().getScreenSize()));
                self.frame.setResizable(true);
                self.frame.setLocationRelativeTo(null);                                             // place window in the center of the screen
                Settings.foregroundColor = self.frame.getForeground();                              // place the foreground color info in Settings so other classes can read it from there

//                Tools.initKeyboardShortcuts(self, self.frame);                                      // don't need this anymore as the keyboard shortcuts are implemented via the menubar
                self.frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);                 // terminate everything, all windows
//                self.frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);             // close this window/frame, others remain open
                Runtime.getRuntime().addShutdownHook(new Thread(() -> shutdownHook()));             // what must be done on shutdown will be done via method shutdownHook()

                // add a menu bar to the frame
                self.frame.setJMenuBar(self.makeMenuBar());

                // display the welcome message on startup, where the user can choose to open a recent project
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
     * prepare the menu bar for this frame
     * @return
     */
    private WebMenuBar makeMenuBar() {
        // file menu
        WebMenu file = new WebMenu("File");
        file.setMnemonic('f');

        WebMenuItem open = new WebMenuItem("Open Project / Import File", 'o');
        open.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_MASK));
        open.addActionListener(actionEvent -> this.openFile());
        file.add(open);

        this.openRecent = new WebMenu("Open Recent");
        this.openRecent.setMnemonic('r');
        this.updateOpenRecent();
        file.add(this.openRecent);

        this.save = new WebMenuItem("Save", 's');
        this.save.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_MASK));
        this.save.addActionListener(actionEvent -> this.saveProject());
        this.save.setEnabled(this.projectPane != null);
        file.add(this.save);

        this.saveAs = new WebMenuItem("Save As", 'a');
        this.saveAs.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_MASK + InputEvent.ALT_MASK));
        this.saveAs.addActionListener(actionEvent -> this.saveProjectAs());
        this.saveAs.setEnabled(this.projectPane != null);
        file.add(this.saveAs);

        WebMenuItem exportMidi = new WebMenuItem("MIDI", 'm');
        exportMidi.addActionListener(actionEvent -> {
            Performance performance = this.getProjectPane().getSyncPlayer().getSelectedPerformance();
            Midi midi = this.getProjectPane().getMsm().exportExpressiveMidi(performance, true);
            midi.writeMidi();
        });
        WebMenuItem exportWav = new WebMenuItem("Wave", 'w');
        exportWav.addActionListener(actionEvent -> {
            Performance performance = this.getProjectPane().getSyncPlayer().getSelectedPerformance();
            Midi midi = this.getProjectPane().getMsm().exportExpressiveMidi(performance, true);
            midi.exportAudio().writeAudio();
        });
        WebMenuItem exportMp3 = new WebMenuItem("MP3", '3');
        exportMp3.addActionListener(actionEvent -> {
            Performance performance = this.getProjectPane().getSyncPlayer().getSelectedPerformance();
            Midi midi = this.getProjectPane().getMsm().exportExpressiveMidi(performance, true);
            midi.exportAudio().writeMp3();
        });
        this.export = new WebMenu("Export Performance Rendering as");
        this.export.setMnemonic('e');
        this.export.add(exportMidi);
        this.export.add(exportWav);
        this.export.add(exportMp3);
        this.export.setEnabled(this.projectPane != null);
        file.add(export);

        this.close = new WebMenuItem("Close Project", 'c');
        this.close.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_W, InputEvent.CTRL_MASK));
        this.close.addActionListener(actionEvent -> this.closeProject());
        this.close.setEnabled(this.projectPane != null);
        file.add(this.close);

        file.addSeparator();

        WebMenuItem exit = new WebMenuItem("Exit", 'x');
        exit.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F4, InputEvent.ALT_MASK));
        exit.addActionListener(actionEvent -> {
            this.frame.dispose();                                                        // close the window (if this is the only window, this will terminate the JVM)
            System.exit(0);                                                         // the program may still run, enforce exit
        });
        file.add(exit);

        // edit menu
        WebMenu edit = new WebMenu("Edit");
        edit.setMnemonic('e');

        WebMenuItem useDefaultSoundfont = new WebMenuItem("Use Default Soundfont", 's');
        useDefaultSoundfont.setToolTipText("This unloads any currently active soundfont and sets the default soundfont to be used.");
        useDefaultSoundfont.addActionListener(actionEvent -> {
            Settings.setSoundbank(null);
            if (this.projectPane != null) {
                this.getProjectPane().getSyncPlayer().getMidiPlayer().loadDefaultSoundbank();
                this.getProjectPane().getMidiPlayer().loadDefaultSoundbank();
            }
        });
        edit.add(useDefaultSoundfont);

        this.playStop = new WebMenuItem("Play / Stop");
        this.playStop.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0));
        this.playStop.setEnabled(this.projectPane != null);
        this.playStop.addActionListener(actionEvent -> {
            if ((this.getProjectPane() == null) || (this.getProjectPane().getSyncPlayer() == null))
                return;
            this.getProjectPane().getSyncPlayer().triggerPlayback();
        });
        edit.add(this.playStop);

        // help menu
        WebMenu help = new WebMenu("Help");
        help.setMnemonic('h');

        WebMenuItem mpm = new WebMenuItem("Music Performance Markup", 'm');
        mpm.addActionListener(actionEvent -> {
            try {
                Desktop.getDesktop().browse(new URI("https://axelberndt.github.io/MPM/"));
            } catch (IOException | URISyntaxException e) {
                e.printStackTrace();
            }
        });
        help.add(mpm);

        WebMenuItem about = new WebMenuItem("About", 'a');
        about.addActionListener(actionEvent -> {
            try {
                Desktop.getDesktop().browse(new URI("https://github.com/axelberndt/MPM-Toolbox"));
            } catch (IOException | URISyntaxException e) {
                e.printStackTrace();
            }
        });
        // TODO: open info dialog
        help.add(about);

        // put them all together
        return (new WebMenuBar()).add(file, edit, help);
    }

    /**
     * update the content of the open recent submenu
     */
    private void updateOpenRecent() {
        this.openRecent.removeAll();
        for (File recent : Settings.recentOpened.getAll()) {
            WebMenuItem item = new WebMenuItem(recent.getAbsolutePath());
            item.addActionListener(actionEvent -> this.loadFile(recent));
            this.openRecent.add(item);
        }
    }

    /**
     * the startup window
     * @return
     */
    private WebPanel makeWelcomeMessage() {
        GridBagLayout layout = new GridBagLayout();
        WebPanel welcomeMessage = new WebPanel(layout);

        WebLabel imageLabel = new WebLabel(new ImageIcon(Toolkit.getDefaultToolkit().getImage(this.getClass().getResource("/resources/icons/icon5-1.png"))), WebLabel.RIGHT);
//        imageLabel.setAlignmentX(Component.RIGHT_ALIGNMENT);
        Tools.addComponentToGridBagLayout(welcomeMessage, layout, imageLabel, 0, 0, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH, GridBagConstraints.LINE_END);

        WebLabel message = new WebLabel("<html><font size='+2'>Welcome to the Music Performance Markup Toolbox," +
                                        "<p style='margin-top:7'>your tool to create and analyse expressive music performances.</p></font>" +
                                        "<p style='margin-top:28'>Open an existing project (MPR file) or initiate a new one by opening or dropping</p>" +
                                        "<p style='margin-top:7'>an MEI, MSM or MIDI file. If you start with an MEI file, it should use expansions</p>" +
                                        "<p style='margin-top:7'>to encode repetitions, da capi etc.</p>" +
                                        "<p style='margin-top:28'>Once the project has started, you can create or import an MPM file and import audio</p>" +
                                        "<p style='margin-top:7'>(WAV, MP3), score image (JPG, JPEG, PNG, GIF, BMP), and soundfont (DLS, SF2) data. </p></html>", WebLabel.LEFT);
//        message.setAlignmentX(Component.LEFT_ALIGNMENT);
        message.setPadding(0, 40, 0, 0);
        message.setFontSize(18);
        message.setForeground(Color.LIGHT_GRAY);
        Tools.addComponentToGridBagLayout(welcomeMessage, layout, message, 1, 0, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.NONE, GridBagConstraints.LINE_START);

        return welcomeMessage;
    }

    /**
     * This triggers an open file dialog.
     */
    public void openFile() {
        final JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Open File");

        FileFilter[] ff = fileChooser.getChoosableFileFilters();
        for (FileFilter f : ff)
            fileChooser.removeChoosableFileFilter(f);
        fileChooser.addChoosableFileFilter(new FileNameExtensionFilter("MPM Toolbox Project", "mpr"));
        fileChooser.addChoosableFileFilter(new FileNameExtensionFilter("Musical Sequence Markup", "msm"));
        fileChooser.addChoosableFileFilter(new FileNameExtensionFilter("Music Performance Markup", "mpm"));
        fileChooser.addChoosableFileFilter(new FileNameExtensionFilter("MEI", "mei"));
        fileChooser.addChoosableFileFilter(new FileNameExtensionFilter("XML", "xml"));
        fileChooser.addChoosableFileFilter(new FileNameExtensionFilter("MIDI", "mid"));
        fileChooser.addChoosableFileFilter(new FileNameExtensionFilter("Audio files", "wav", "mp3"));
        fileChooser.addChoosableFileFilter(new FileNameExtensionFilter("Images", "jpg", "jpeg", "png", "gif", "bmp"));
        fileChooser.addChoosableFileFilter(new FileNameExtensionFilter("Soundfonts", "dls", "sf2"));

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
        ProjectPane projectPane;
        try {
            switch (extension) {
                case ".xml":                                                            // xml could be anything
                    XmlBase xml = new XmlBase(file);                                    // parse the xml file into an instance of XmlBase
                    switch (xml.getRootElement().getLocalName()) {                      // get the name of the root element in the xml tree
                        case "mpmToolkitProject":                                       // seems to be an MPM Toolkit project
                            projectPane = new ProjectPane(file, this);
                            if (this.openProject(projectPane))
                                Tools.fileDrop(this, projectPane);
                            break;
                        case "mei":                                                     // seems to be an mei
                            Mei mei = new Mei(xml.getDocument());
                            mei.setFile(xml.getFile());
                            projectPane = new ProjectPane(mei, this);
                            if (this.openProject(projectPane))
                                Tools.fileDrop(this, projectPane);
                            break;
                        case "msm":                                                     // seems to be an msm
                            Msm msm = new Msm(xml.getDocument());
                            msm.setFile(xml.getFile());
                            projectPane = new ProjectPane(msm, this);
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
                    projectPane = new ProjectPane(file, this);
                    if (this.openProject(projectPane))
                        Tools.fileDrop(this, projectPane);
                    break;
                case ".mei":
                    projectPane = new ProjectPane(new Mei(file), this);
                    if (this.openProject(projectPane))
                        Tools.fileDrop(this, projectPane);
                    break;
                case ".msm":
                    projectPane = new ProjectPane(new Msm(file), this);
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
                    projectPane = new ProjectPane(new Midi(file), this);
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
                    if (this.projectPane != null) {
                        this.getProjectPane().getSyncPlayer().getMidiPlayer().loadSoundbank(file);
                        this.getProjectPane().getMidiPlayer().loadSoundbank(file);
                    }
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
        this.frame.add(this.welcomeMessage);

        this.save.setEnabled(false);
        this.saveAs.setEnabled(false);
        this.export.setEnabled(false);
        this.close.setEnabled(false);
        this.playStop.setEnabled(false);

        this.frame.repaint();
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
        this.frame.repaint();
        if (this.projectPane.getFile() != null) {
            Settings.recentOpened.add(this.projectPane.getFile());
            this.updateOpenRecent();
        }

        this.save.setEnabled(true);
        this.saveAs.setEnabled(true);
        this.export.setEnabled(true);
        this.close.setEnabled(true);
        this.playStop.setEnabled(true);

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
//                if (this.projectPane.getFile() != null) {
//                    Settings.recentOpened.add(this.projectPane.getFile());
//                    this.updateOpenRecent();
//                }
//                this.save.setEnabled(true);
//                this.saveAs.setEnabled(true);
//                this.close.setEnabled(true);
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
            this.updateOpenRecent();
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
                this.updateOpenRecent();
                return true;
            }
        }
        return false;
    }
}
