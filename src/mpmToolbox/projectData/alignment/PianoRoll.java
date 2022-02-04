package mpmToolbox.projectData.alignment;

import com.alee.api.annotations.NotNull;
import mpmToolbox.gui.Settings;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * This represents a piano roll image including dedicated functionality.
 * @author Axel Berndt
 */
public class PianoRoll extends BufferedImage {
    private Note[][] noteReferences;
    private final double fromMilliseconds;
    private final double toMilliseconds;

    /**
     * constructor
     * @param width
     * @param height
     */
    public PianoRoll(double fromMilliseconds, double toMilliseconds, int width, int height) {
        super(width, height, BufferedImage.TYPE_INT_ARGB);
        this.noteReferences = new Note[width][height];
        this.fromMilliseconds = fromMilliseconds;
        this.toMilliseconds = toMilliseconds;
//        this.init();    // not necessary
    }

    /**
     * initialize each pixel to be transparent and have note association null
     */
    private void init() {
        for (int x=0; x < this.getWidth(); ++x) {
            for (int y = 0; y < this.getHeight(); ++y) {
                this.set(x, y, null);
            }
        }
    }

    /**
     * get the note at the specified pixel position
     * @param x
     * @param y
     * @return a reference to a Note object or null
     */
    public Note getNoteAt(int x, int y) {
        if ((x < 0) || (x >= this.getWidth()) || (y < 0) || (y >= this.getHeight()))
            return null;
        return this.noteReferences[x][y];
    }

    /**
     * set a specific pixel position
     * @param x
     * @param y
     * @param note a reference to a Note object or null
     * @return success
     */
    private boolean set(int x, int y, Note note) {
        if ((x < 0) || (x >= this.getWidth()) || (y < 0) || (y >= this.getHeight()))
            return false;

        // if note is null, make the pixel completely transparent, otherwise set the performance color for fixed notes and note color for non-fixed notes
        Color color;
        if (note == null)
            color = new Color(0, 0, 0, 0);
        else if (note.isFixed())
            color = Settings.scorePerformanceColorHighlighted;
        else
            color = Settings.scoreNoteColor;

        this.setRGB(x, y, color.getRGB());      // set the note color
        this.noteReferences[x][y] = note;       // set reference
        return true;
    }

    /**
     * Paint a note to the specified position. If there is already a note, the colors will be added.
     * But only the last added note will be associated.
     * The invoking method should ensure the following (x < 0) || (x >= this.getWidth()) || (y < 0) || (y >= this.getHeight())
     * as this method does not check this!
     * @param x
     * @param y
     * @param alphaFade a value in [0, 1] that is multiplied with the default alpha value; with this, the fading of the note throughout its duration is realized
     * @param note
     * @return
     */
    private void add(int x, int y, float alphaFade, @NotNull Note note) {
        // this first check is not necessary as far as the invoking method does this already
//        if ((x < 0) || (x >= this.getWidth()) || (y < 0) || (y >= this.getHeight()) || (note == null))
//            return;

//        Color color = new Color(this.getRGB(x, y));     // get the current color at (x, y)
//        float[] rgba = color.getRGBComponents(null);
        // the following is more efficient than the above two lines
        int[] rgba = new int[4];
        rgba[0] = (this.getRGB(x, y) >> 16) & 0xFF;
        rgba[1] = (this.getRGB(x, y) >> 8) & 0xFF;
        rgba[2] = (this.getRGB(x, y)) & 0xFF;
        rgba[3] = (this.getRGB(x, y) >> 24) & 0xff;

        Color color = (note.isFixed()) ? Settings.scorePerformanceColorHighlighted : Settings.scoreNoteColor;

        // add the color of a note so that it becomes brighter, but avoid numbers greater than 255
        int r = Math.min(255, Math.round((((255f - rgba[0]) * color.getRed()) / 255) + rgba[0]));
        int g = Math.min(255, Math.round((((255f - rgba[1]) * color.getGreen()) / 255) + rgba[1]));
        int b = Math.min(255, Math.round((((255f - rgba[2]) * color.getBlue()) / 255) + rgba[2]));
        int a = Math.min(255, Math.round((((255f - rgba[3]) * color.getAlpha() * alphaFade) / 255) + rgba[3]));
//        int a = Settings.scoreNoteColor.getAlpha();         // for a constant transparency

        this.setRGB(x, y, new Color(r, g, b, a).getRGB());  // add the color
        this.noteReferences[x][y] = note;                   // set reference
    }

    /**
     * Add a note from the specified xStart position to the specified xEnd position
     * @param xStart
     * @param xEnd
     * @param y
     * @param note
     * @return
     */
    protected void add(int xStart, int xEnd, int y, Note note) {
        float duration = xEnd - xStart;
        for (int x = xStart; x < xEnd; ++x) {   // for each pixel from (xStar, y) to (xEnd, y)
            float alphaFade = (float) Math.pow((xEnd - x) / duration, 0.2);
            this.add(x, y, alphaFade, note);
//            this.add(x, y, 1f, note);
        }
    }

    /**
     * Add the given piano roll to this one
     * @param pianoRoll
     */
    protected void add(PianoRoll pianoRoll) {
        // the pixel operations in the for loop are more efficient than these lines
//        Graphics2D g2d = this.createGraphics();
//        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER));
//        g2d.drawImage(pianoRoll, 0, 0, null);
//        g2d.dispose();

        int width = Math.min(this.getWidth(), pianoRoll.getWidth());
        int height = Math.min(this.getHeight(), pianoRoll.getHeight());

        for (int x=0; x < width; ++x) {
            for (int y=0; y < height; ++y) {
                if (pianoRoll.getNoteAt(x, y) == null)      // if there is nothing to add
                    continue;                               // continue with the next pixel

                int[] rgba = new int[4];
                rgba[0] = (this.getRGB(x, y) >> 16) & 0xFF;
                rgba[1] = (this.getRGB(x, y) >> 8) & 0xFF;
                rgba[2] = (this.getRGB(x, y)) & 0xFF;
                rgba[3] = (this.getRGB(x, y) >> 24) & 0xff;

                int add = pianoRoll.getRGB(x, y);
                int[] addrgba = new int[4];
                addrgba[0] = (add >> 16) & 0xFF;
                addrgba[1] = (add >> 8) & 0xFF;
                addrgba[2] = add & 0xFF;
                addrgba[3] = (add >> 24) & 0xff;

                int r = Math.min(255, Math.round((((255f - rgba[0]) * addrgba[0]) / 255) + rgba[0]));
                int g = Math.min(255, Math.round((((255f - rgba[1]) * addrgba[1]) / 255) + rgba[1]));
                int b = Math.min(255, Math.round((((255f - rgba[2]) * addrgba[2]) / 255) + rgba[2]));
                int a = Math.min(255, Math.round((((255f - rgba[3]) * addrgba[3]) / 255) + rgba[3]));
//                int a = Settings.scoreNoteColor.getAlpha();         // for a constant transparency

                this.setRGB(x, y, new Color(r, g, b, a).getRGB());  // add the color

                if (this.noteReferences[x][y] == null)      // we do not overwrite a non-null value
                    this.noteReferences[x][y] = pianoRoll.getNoteAt(x, y);
            }
        }
    }

    /**
     * retrieve the milliseconds date where this image begins
     * @return
     */
    public double getFromMilliseconds() {
        return this.fromMilliseconds;
    }

    /**
     * retrieve the milliseconds date where this image ends
     * @return
     */
    public double getToMilliseconds() {
        return this.toMilliseconds;
    }

    /**
     * check if the specified metrics are equal to those of this piano roll
     * @param fromMilliseconds
     * @param toMilliseconds
     * @param width
     * @param height
     * @return
     */
    public boolean sameMetrics(double fromMilliseconds, double toMilliseconds, int width, int height) {
        return (fromMilliseconds == this.fromMilliseconds)
                && (toMilliseconds == this.toMilliseconds)
                && (width == this.getWidth())
                && (height == this.getHeight());
    }
}
