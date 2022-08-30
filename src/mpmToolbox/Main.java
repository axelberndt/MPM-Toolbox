package mpmToolbox;

import com.alee.laf.WebLookAndFeel;
import meico.Meico;
import mpmToolbox.gui.MpmToolbox;
import mpmToolbox.gui.Settings;
import mpmToolbox.supplementary.Tools;

import java.io.IOException;

/**
 * This class provides the current version number of MPM Toolbox.
 * @author Axel Berndt
 */
public class Main {
    public static final String version = "0.1.18";

    public static void main(String[] args) {
        // read the application settings from file
        try {
            Settings.readSettings();
        } catch (IOException e) {
            System.err.println("File " + Settings.settingsFile + " not found.");
        }

        // redirect commandline output to a logfile
        if (Settings.makeLogfile)
            Tools.commandlineToLogfile(Settings.logfile);

        System.out.println("MPM Toolkit version " + Main.version + "\nmeico version " + Meico.version + "\nrunning on " + System.getProperty("os.name") + " version " + System.getProperty("os.version") + ", " + System.getProperty("os.arch") + "\nJava version " + System.getProperty("java.version"));

        WebLookAndFeel.setForceSingleEventsThread(true);    // make sure that GUI defining operations are performed in an Event Dispatch Thread (EDT) or throw an exception, TODO: set false for deployment

        new MpmToolbox();
    }
}
