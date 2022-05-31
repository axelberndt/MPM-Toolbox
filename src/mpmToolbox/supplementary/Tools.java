package mpmToolbox.supplementary;

import com.alee.laf.slider.WebSlider;
import com.alee.laf.slider.WebSliderUI;
import meico.supplementary.KeyValue;
import mpmToolbox.gui.MpmToolbox;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;
import java.util.regex.Matcher;

/**
 * Some useful functionality to be added here and there.
 * @author Axel Berndt
 */
public class Tools {
    public static Cursor blankCursor = Toolkit.getDefaultToolkit().createCustomCursor(new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB), new Point(0, 0), "blank cursor"); // this effectively hides the mouse cursor

    /**
     * adds keyboard shortcuts
     * @param application
     * @param frame the frame that exits
     */
    public static void initKeyboardShortcuts(MpmToolbox application, JFrame frame) {
        // keyboard input via key binding
        InputMap inputMap = frame.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "Exit");            // close the window when ESC pressed
        frame.getRootPane().getActionMap().put("Exit", new AbstractAction(){            // define the "Exit" action
            @Override
            public void actionPerformed(ActionEvent e) {
                frame.dispose();                                                        // close the window (if this is the only window, this will terminate the JVM)
                System.exit(0);                                                         // the program may still run, enforce exit
            }
        });

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_MASK), "Save");  // CTRL+S
        frame.getRootPane().getActionMap().put("Save", new AbstractAction(){
            @Override
            public void actionPerformed(ActionEvent e) {
                application.saveProject();
            }
        });

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_MASK), "Open");  // CTRL+O
        frame.getRootPane().getActionMap().put("Open", new AbstractAction(){
            @Override
            public void actionPerformed(ActionEvent e) {
                application.openFile();
            }
        });

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), "Trigger Playback");     // start/stop playback with SPACE
        frame.getRootPane().getActionMap().put("Trigger Playback", new AbstractAction(){
            @Override
            public void actionPerformed(ActionEvent e) {
                if ((application.getProjectPane() == null) || (application.getProjectPane().getSyncPlayer() == null))
                    return;
                application.getProjectPane().getSyncPlayer().triggerPlayback();
            }
        });
    }

    /**
     * file drop functionality
     * @param component the component that receives file drops
     */
    public static void fileDrop(MpmToolbox app, JComponent component) {
        // for debugging information replace the null argument by System.out
        new FileDrop(null, component.getRootPane(), true, files -> {    // this is the fileDrop listener
//        new FileDrop(null, component.getRootPane(), false, files -> {   // this is the fileDrop listener TODO: use this to re-enable repositioning of GUI elements, but file drop will not work on those elements
            for (File file : files) {                                   // for each file that has been dropped
                app.loadFile(file);                                     // load the file in teh application
            }
            app.getFrame().toFront();                                   // after the file drop force this window to have the focus
        });
    }

    /**
     * redirect all commandline output to a logfile, it will be generated in the program folder
     * @param filename the name of the logfile, e.g. myLogfile.log
     */
    public static void commandlineToLogfile(String filename) {
        // all the command line output and error messages are redirected to a log file, if a filename is given in Settings
        if (!filename.isEmpty()) {                                      // is there a nonempty string?
            try {
                FileOutputStream log = new FileOutputStream(filename);  // use the string as filename
                PrintStream out = new PrintStream(log);                 // make a PrintStream that outputs to the FileOutputStream that fills the log file
                System.setOut(out);                                     // redirect the System.out stream to the PrintStream so that all console output goes to the log file
                System.setErr(out);                                     // redirect the System.err stream to the PrintStream so that all console output goes to the log file
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * read an image file into a BufferedImage object
     * @param file the file to be read
     * @return the BufferedImage object or null
     * @throws IOException
     */
    public static BufferedImage readImageFile(File file) throws IOException {
//        int index = file.getName().lastIndexOf(".");
//        String extension = file.getName().substring(index).toLowerCase();               // get the file extension
        // TODO: other image formats such as TIFF, SVG, PDF require different treatment

        return ImageIO.read(file);
    }

    /**
     * this method implements a brute force approach to finding the nearest point in a hashmap of points
     * @param pointCloud the point cloud as hashmap, so we can return the key object as reference to the point
     * @param pivot the pivotal point for which we seek the nearest in the hashmap
     * @return a tuple of the point's key and distance, if there are more than one nearest points only the first is returned
     */
    public static KeyValue<Point, Double> findNearestPoint(ArrayList<Point> pointCloud, Point pivot) {
        if ((pointCloud == null) || pointCloud.isEmpty())
            return null;

        KeyValue<Point, Double> result = new KeyValue<>(null, Double.MAX_VALUE);   // at first the value will hold the square distance, later the square root will be applied to it

        // check all points in pointCloud and keep the nearest in result
        for (Point entry : pointCloud) {                    // for each point in the point cloud
            double distance = pivot.distanceSq(entry);      // compute distance to pivot point
            if (distance >= result.getValue())
                continue;

            result.setKey(entry);                           // write key to result
            result.setValue(distance);                      // write distance to result
        }

        if (result.getKey() == null)                        // if we found nothing useful
            return null;                                    // we return nothing useful

        result.setValue(Math.sqrt(result.getValue()));      // apply square root to get the actual distance
        return result;
    }

    /**
     * a helper method to add components to a gridbag layouted container
     * @param container
     * @param gridBagLayout
     * @param component
     * @param x
     * @param y
     * @param width
     * @param height
     * @param weightx
     * @param weighty
     * @param ipadx
     * @param ipady
     * @param fill
     */
    public static void addComponentToGridBagLayout(Container container, GridBagLayout gridBagLayout, Component component, int x, int y, int width, int height, double weightx, double weighty, int ipadx, int ipady, int fill, int anchor) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = fill;
        gbc.gridx = x;
        gbc.gridy = y;
        gbc.gridwidth = width;
        gbc.gridheight = height;
        gbc.weightx = weightx;
        gbc.weighty = weighty;
        gbc.ipadx = ipadx;
        gbc.ipady = ipady;
        gbc.anchor = anchor;
        gridBagLayout.setConstraints(component, gbc);
        container.add(component);
    }

    /**
     * round a double value to the given number of decimal points
     * @param value
     * @param decimalPoints
     */
    public static double round(double value, int decimalPoints) {
        double d = Math.pow(10, decimalPoints);
        return Math.round(value * d) / d;
    }

    /**
     * When clicking the slider, the tick should be set to the click position
     * instead of doing only a step in that direction (default behaviour).
     * @param slider the slider to be changed
     */
    public static void makeSliderSetToClickPosition(WebSlider slider) {
        slider.setUI(new WebSliderUI(slider) {
            protected void scrollDueToClickInTrack(int direction) {
                //scrollByBlock(direction); // this is the default behaviour, let's comment that out
                int value = this.slider.getValue();
                if (this.slider.getOrientation() == WebSlider.HORIZONTAL)
                    value = this.valueForXPosition(this.slider.getMousePosition().x);
                else if (this.slider.getOrientation() == WebSlider.VERTICAL)
                    value = this.valueForYPosition(this.slider.getMousePosition().y);
                this.slider.setValue(value);
            }
        });
    }

    /**
     * helper method to handle file paths with different types of separators
     * @param path
     * @return
     */
    public static String uniformPath(String path) {
        return path.replaceAll("(/+)|((\\\\)+)", Matcher.quoteReplacement(File.separator));
    }
}
