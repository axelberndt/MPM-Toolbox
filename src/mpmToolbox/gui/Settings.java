package mpmToolbox.gui;

import com.alee.laf.WebLookAndFeel;
import mpmToolbox.supplementary.RecentOpened;

import java.awt.*;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * This class helps reading, storing and accessing some general settings of the application.
 * @author Axel Berndt
 */
public class Settings {
    public static boolean debug = false;                                // set true to run MPM Toolbox in debug mode
    public static boolean makeLogfile = true;                           // make a logfile
    public static String logfile = "mpmToolbox.log";                    // filename of log file
    public static String settingsFile = "mpmToolbox.cfg";               // filename of the settings file

    private static final ArrayList<Image> iconsImages = new ArrayList<>();     // this arraylist will be filled with the icons loaded from the below array
    private static final ArrayList<String> iconPaths = new ArrayList<>(Arrays.asList("/resources/icons/icon5.png",
                                                                                    "/resources/icons/icon5-1.png",
                                                                                    "/resources/icons/icon5-2.png",
                                                                                    "/resources/icons/icon5-3.png",
                                                                                    "/resources/icons/icon5-4.png",
                                                                                    "/resources/icons/icon5-5.png",
                                                                                    "/resources/icons/icon5-6.png",
                                                                                    "/resources/icons/icon5-7.png",
                                                                                    "/resources/icons/icon5-8.png",
                                                                                    "/resources/icons/icon5-9.png"));     // the app icon in different resolutions
    protected static int windowWidth = 1200;                            // initial window width
    protected static int windowHeight = 800;                            // initial window height
    public static Color foregroundColor = SystemColor.text;             // the foreground/text color, this will be set during initialization according to the underlying style and can be accessed by other classes
    public static int paddingInDialogs = 10;                            // this value is used in dialog elements (buttons, textfields etc.) for padding

    public static Color errorColor = new Color(255, 120, 120);          // the color for deleting a note in a score image

    public static Color scoreNoteColor = new Color(0f, 0.7f, 0f, 0.4f);                 // the color of note symbols that are annotated in a score image
    public static Color scoreNoteColorHighlighted = new Color(0.2f, 1f, 0.2f, 0.6f);    // the highlight color of note symbols that are annotated in a score image
    public static Color scoreNoteDeleteColor = new Color(1.0f, 0.15f, 0.2f, 0.6f);      // the color for deleting a note in a score image

    public static Color scorePerformanceColor = new Color(0.0f, 0.7f, 0.7f, 0.5f);              // the color of performance symbols that are annotated in a score image
    public static Color scorePerformanceColorFaded = new Color(0.0f, 0.7f, 0.7f, 0.17f);        // the color of performance symbols when faded out because the MPM tree cursor is in another performance
    public static Color scorePerformanceColorHighlighted = new Color(0.2f, 1.0f, 1.0f, 0.6f);   // the highlight color of performance symbols that are annotated in a score image

//    protected static String symbolFontPath = "/resources/fonts/fa-solid-900.ttf";
//    public static Font symbolFont = null;                               // a handle to the font to be used for most of the symbols/icons

    public static double anchorSwitchOvershootThreshold = 0.3;          // in the score display, to switch the anchor from one nearest node to another the distance ratio (distance to nearest / distance to current anchorNode) must be at most this value, so the user has to overshoot, i.e. get much closer to the desired nearest node, to switch the anchor to it

    protected static File soundbank = null;                             // set this null to use the default soundbank

    public static RecentOpened recentOpened = new RecentOpened(10);     // this is the list of the last 10 recently opened files

    /**
     * read the settings file mpmToolbox.cfg
     * @throws IOException
     */
    public static void readSettings() throws IOException {
        File file = new File(Settings.settingsFile);
        ArrayList<File> recentOpenedFiles = new ArrayList<>();
        BufferedReader br = new BufferedReader(new FileReader(file));

        String attribute = "";
        for(String line = br.readLine(); line != null; line = br.readLine()) {  // read all the lines in mpmToolkit.cfg
            if (line.isEmpty()                                                  // an empty line
                    || (line.charAt(0) == '%'))                                 // this is a comment line
                continue;                                                       // ignore it

            if (line.charAt(0) == '#') {                                        // this is an attribute name
                attribute = line.substring(2);                                  // get attribute name
                continue;
            }

            // read the attribute value
            switch (attribute) {
                case "windowWidth":
                    Settings.windowWidth = Integer.parseInt(line);
                    break;
                case "windowHeight":
                    Settings.windowHeight = Integer.parseInt(line);
                    break;
                case "debug":
                    Settings.debug = line.equals("1");
                    break;
                case "logfile":
                    Settings.makeLogfile = line.equals("1");
                    break;
                case "anchorSwitchOvershootThreshold":
                    Settings.anchorSwitchOvershootThreshold = Double.parseDouble(line);
                    break;
//                case "symbolFont":
//                      The .cfg file entry looks like this:
//                      # symbolFont
//                      /resources/fonts/fa-solid-900.ttf
//
//                    Settings.symbolFontPath = line;
//                    InputStream is = file.getClass().getResourceAsStream(Settings.symbolFontPath);
//                    try {
//                        Settings.symbolFont = Font.createFont(Font.TRUETYPE_FONT, is).deriveFont(Font.PLAIN, Settings.getDefaultFontSize());
//                        is.close();
//                    } catch (FontFormatException | IOException e) {
//                        e.printStackTrace();
//                    }
//                    break;
                case "soundbank":
                    Settings.setSoundbank((line.equals("default")) ? null : new File(line));
                    if ((Settings.soundbank != null) && !Settings.soundbank.exists())
                        Settings.setSoundbank(null);
                    break;
                case "recentOpened":
                    File recent = new File(line);
                    if (recent.exists())
                        recentOpenedFiles.add(recent);
                    break;
                default:
                    break;
            }
        }

        // fill the recently opened files list
        for (int i = Math.min(Settings.recentOpened.getMaxSize(), recentOpenedFiles.size()) - 1; i >= 0; --i)   // in reverse order of reading from the settings file to have the most recent file at the first position in the list
            Settings.recentOpened.add(recentOpenedFiles.get(i));

        br.close(); // close reader
    }

    /**
     * store the current settings in file mpmToolbox.cfg
     */
    protected static void writeSettingsFile() {
        // build the output string of the settings file
        String output = "% MPM Toolbox settings"
                + "\n\n# windowWidth\n" + Settings.windowWidth
                + "\n\n# windowHeight\n" + Settings.windowHeight
                + "\n\n# debug\n" + (Settings.debug ? "1" : "0")
                + "\n\n# logfile\n" + (Settings.makeLogfile ? "1" : "0")
                + "\n\n# anchorSwitchOvershootThreshold\n" + Settings.anchorSwitchOvershootThreshold
//                + "\n\n# symbolFont\n" + Settings.symbolFontPath
                + "\n\n# soundbank\n" + ((Settings.soundbank == null) ? "default" : Settings.soundbank.getAbsolutePath())
                + "\n\n# recentOpened\n" + Settings.recentOpened.toString()
                +"\n";

        PrintWriter writer;
        try {
            writer = new PrintWriter("mpmToolbox.cfg", "UTF-8");
        } catch (FileNotFoundException | UnsupportedEncodingException e) {
            e.printStackTrace();
            return;
        }
        writer.print(output);
        writer.close();
    }

    /**
     * set the soundbank to be used for MIDI synthesis
     * @param soundbank
     */
    protected static synchronized void setSoundbank(File soundbank) {
        Settings.soundbank = soundbank;
    }

    /**
     * a getter for the soundbank
     * @return
     */
    public static synchronized File getSoundbank() {
        return Settings.soundbank;
    }

    /**
     * get the default font size
     * @return
     */
    public static int getDefaultFontSize() {
//        Font defaultFont = UIManager.getDefaults().getFont("TextPane.font");
//        return defaultFont.getSize();
        return WebLookAndFeel.globalWindowFont.getSize();
    }

    /**
     * reads the icons to be used in the title bar of the window
     * @param mpmToolbox
     * @return
     */
    public static ArrayList<Image> getIcons(MpmToolbox mpmToolbox) {
        if (Settings.iconsImages.isEmpty() && (mpmToolbox != null))
            for (String resource : Settings.iconPaths)
                Settings.iconsImages.add(Toolkit.getDefaultToolkit().getImage(mpmToolbox.getClass().getResource(resource)));

        return Settings.iconsImages;
    }
}
