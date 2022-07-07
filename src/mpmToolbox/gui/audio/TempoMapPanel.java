package mpmToolbox.gui.audio;

import com.alee.laf.menu.WebPopupMenu;
import mpmToolbox.gui.Settings;
import mpmToolbox.projectData.alignment.Alignment;
import mpmToolbox.projectData.alignment.Note;
import mpmToolbox.projectData.alignment.PianoRoll;

import java.awt.*;
import java.awt.event.*;

/**
 * This panel is an interactive tempoMap visualization.
 * @author Axel Berndt
 */
public class TempoMapPanel extends PianoRollPanel implements ComponentListener, MouseListener, MouseMotionListener, MouseWheelListener {
    private final Alignment alignment;
    private long leftmostSample;    // just a copy of the eponymous value in the parent to keep track of whether it changed and the tick values have to be computed anew
    private long rightmostSample;   // just a copy of the eponymous value in the parent to keep track of whether it changed and the tick values have to be computed anew

    /**
     * constructor
     * @param parent
     */
    protected TempoMapPanel(AudioDocumentData parent) {
        this(parent, "<html><center>Select a performance to visualize its tempomap here.<p style='margin-top:7'>In contrast to the audio-related panels, the information here is displayed along the musical, i.e. symbolic, time domain, not milliseconds.</p></center></html>");
    }

    /**
     * constructor
     * @param parent
     * @param noDataText
     */
    protected TempoMapPanel(AudioDocumentData parent, String noDataText) {
        super(parent, noDataText);

        this.alignment = new Alignment(parent.getParent().getMsm(), null);
        this.leftmostSample = parent.getLeftmostSample();
        this.rightmostSample = parent.getRightmostSample();

        this.setOpaque(false);
        this.setBackground(Color.BLACK);
    }

    /**
     * draw the component
     * @param g
     */
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);                // this ensures that the background is filled with the standard background color

        if (this.parent.getParent().getSyncPlayer().getSelectedPerformance() == null)
            return;

        Graphics2D g2d = (Graphics2D) g;         // make g a Graphics2D object, so we can use its extended drawing features
        this.drawPianoRoll(g2d);
        this.drawPlaybackCursor(g2d);

        if (this.drawMouseCursor(g2d)) {
            // TODO: print info text
        }
    }

    /**
     * invoke this method if the data to be displayed here has changed
     */
    protected void update() {
        if (this.parent.getParent().getSyncPlayer().getSelectedPerformance() == null) {
            this.setOpaque(false);
            this.add(this.noData);
            return;
        }

        this.setOpaque(true);
        this.remove(this.noData);
        this.repaint();
    }

    /**
     * helper method to draw the piano roll
     * @param g2d
     */
    @Override
    protected boolean drawPianoRoll(Graphics2D g2d) {
        if (this.getWidth() <= 0)
            return false;

        // compute the corresponding tick positions to the milliseconds positions of the (performed) alignment in the other panels, so we know the positions to scale this non-performed alignment
        if ((this.leftmostSample != this.parent.getLeftmostSample()) || (this.rightmostSample != this.parent.getRightmostSample())) {   // only necessary if the borders have changed
            this.leftmostSample = this.parent.getLeftmostSample();
            this.rightmostSample = this.parent.getRightmostSample();

            if (this.parent.getAudio() == null) {
                this.parent.setLeftmostTick(0.0);
                this.parent.setRightmostTick(this.parent.getParent().getMsm().getEndDate());
            } else {
                double sampleToMilliConst = 1000.0 / this.parent.getAudio().getFrameRate();
                double fromMilliseconds = ((double) this.leftmostSample * sampleToMilliConst);
                double toMilliseconds = ((double) this.rightmostSample * sampleToMilliConst);
                this.parent.setLeftmostTick(this.parent.getAlignment().getCorrespondingTickDate(fromMilliseconds)[0]);
                this.parent.setRightmostTick(this.parent.getAlignment().getCorrespondingTickDate(toMilliseconds)[1]);
            }
        }

        PianoRoll pianoRoll = this.retrievePianoRoll(this.parent.getLeftmostTick(), this.parent.getRightmostTick(), this.getWidth(), 128);
        g2d.drawImage(pianoRoll, 0, this.getHeight(), this.getWidth(), -this.getHeight(), this);
        return true;
    }

    /**
     * retrieve the piano roll from the parent
     * @param fromTicks
     * @param toTicks
     * @param width
     * @param height
     * @return
     */
    @Override
    protected PianoRoll retrievePianoRoll(double fromTicks, double toTicks, int width, int height) {
        Integer partNumber = this.parent.getPianoRollPartNumber();
        if (partNumber != null)         // draw only the selected musical part
            return this.alignment.getPart(partNumber).getPianoRoll(fromTicks, toTicks, width, height);

        // draw all musical parts
        return this.alignment.getPianoRoll(fromTicks, toTicks, width, height);
    }

    /**
     * draw the lines of the mouse cursor in the Graphics2D object
     * @param g2d
     * @return true if a valid mouse position was available and the drawing was successful
     */
    @Override
    protected boolean drawMouseCursor(Graphics2D g2d) {
        // the native domain of the super class PianoRollPanel is the audio domain; hence the mouse cursor position needs a different treatment here than in the super class
        if (this.mouseInThisPanel()) {                                // if the mouse is in this panel
            g2d.setColor(Settings.scoreNoteColorHighlighted);
            g2d.drawLine(this.parent.getMouseCursor().getTicksX(), 0, this.parent.getMouseCursor().getTicksX(), this.getHeight());
            g2d.drawLine(0, this.mousePositionY, this.getWidth(), this.mousePositionY);
            return true;
        }

        // the mouse cursor is in another panel, so we have to compute the corresponding tick position in this panel from the current milliseconds position of the cursor in another panel
        if ((this.parent.getAudio() == null) || (this.parent.getMouseCursor() == null))     // if no cursor data available
            return false;                                                                   // done

        g2d.setColor(Settings.scoreNoteColorHighlighted);           // stroke color
        Stroke defaultStroke = g2d.getStroke();                     // keep the previous stroke settings
        g2d.setStroke(new BasicStroke(this.parent.getMouseCursor().getTicksXSpread())); // set the stroke
        g2d.drawLine(this.parent.getMouseCursor().getTicksX(), 0, this.parent.getMouseCursor().getTicksX(), this.getHeight());    // draw the stroke
        g2d.setStroke(defaultStroke);                               // switch back to the previous stroke settings

        return true;
    }

    /**
     * draw the playback cursor line in the Graphics2D object
     * @param g2d
     */
    @Override
    protected void drawPlaybackCursor(Graphics2D g2d) {
        g2d.setColor(new Color(0.5f, 0.5f, 0.5f, 0.6f));            // stroke color
        Stroke defaultStroke = g2d.getStroke();                     // keep the previous stroke settings
        g2d.setStroke(new BasicStroke(this.parent.getPlaybackCursor().getTicksXSpread()));  // set the stroke
        g2d.drawLine(this.parent.getPlaybackCursor().getTicksX(), 0, this.parent.getPlaybackCursor().getTicksX(), this.getHeight());    // draw the stroke
        g2d.setStroke(defaultStroke);                               // switch back to the previous stroke settings
    }

    /**
     * retrieve the note reference behind a certain pixel position in the current piano roll image
     * @param x horizontal pixel position in the piano roll image
     * @param y vertical pixel position in the piano roll image
     * @return the Note object or null
     */
    @Override
    protected Note getNoteAt(int x, int y) {
        Integer partNumber = this.parent.getPianoRollPartNumber();
        PianoRoll pianoRoll = (partNumber != null) ? this.alignment.getPart(partNumber).getPianoRoll() : this.alignment.getPianoRoll();
        if (pianoRoll == null)
            return null;

        return pianoRoll.getNoteAt(x, y);
    }

    /**
     * retrieve the note reference behind a certain relative position in the current piano roll image
     * @param x relative horizontal position in the piano roll image (should be in [0, 1])
     * @param y relative vertical position in the piano roll image (should be in [0, 1])
     * @return the Note object or null
     */
    @Override
    protected Note getNoteAt(double x, double y) {
        Integer partNumber = this.parent.getPianoRollPartNumber();
        PianoRoll pianoRoll = (partNumber != null) ? this.alignment.getPart(partNumber).getPianoRoll() : this.alignment.getPianoRoll();
        if (pianoRoll == null)
            return null;

        return pianoRoll.getNoteAt((int) (pianoRoll.getWidth() * x), (int) (pianoRoll.getHeight() * (-y + 1.0)));
    }

    /**
     * set the audio data that this panel should visualize
     */
    @Override
    protected void setAudio() {
        throw new UnsupportedOperationException();
    }

    /**
     * process a mouse drag event; to be invoked when scrolling should be performed solely by this panel and not in sync with the other panels
     * @param e
     */
    private void scroll(MouseEvent e) {
        if (this.parent.getMouseCursor() == null)
            return;

        // do the scrolling only in this panel
        double leftmost = this.parent.getLeftmostTick();
        double rightmost = this.parent.getRightmostTick();
        double tickOffset = ((rightmost - leftmost) * (this.parent.getMouseCursor().getTicksX() - e.getPoint().x)) / this.getWidth();   // this computes how many horizontal pixels the mouse has moved, than scales it by the amount of tick per horizontal pixel so we know how many pixels we want to move the leftmost and rightmost tick index

        this.parent.communicateMousePositionToAllComponents(e);

        double numTicks = this.parent.getParent().getMsm().getEndDate();
        tickOffset = (tickOffset > 0) ? Math.min(numTicks - 1 - this.parent.getRightmostTick(), Math.round(tickOffset)) : Math.max(-this.parent.getLeftmostTick(), Math.round(tickOffset));  // we have to check that we don't go beyond the first and last tick; as we move those indices only in integer steps there is a certain numeric error causing the tick moving with a bit different speed than the mouse was moved, but it is not problematic

        if (tickOffset == 0.0)              // if no change
            return;                         // done

        // move the tick indices
        this.parent.setLeftmostTick((int) (this.parent.getLeftmostTick() + tickOffset));
        this.parent.setRightmostTick((int) (this.parent.getRightmostTick() + tickOffset));

        // update the cursor positions
//        this.parent.getMouseCursor().setSample(this.parent.getMouseCursor().getSample());
        this.parent.getPlaybackCursor().setSample(this.parent.getPlaybackCursor().getSample());

        this.repaint();
    }

    /**
     * this is used when solely this panel needs to perform a zoom operation
     * @param pivotTick
     * @param zoomFactor
     */
    private void zoom(int pivotTick, double zoomFactor) {
        if (zoomFactor == 0.0)
            return;

        if (zoomFactor < 0.0) {             // zoom in
            int leftmostTick = pivotTick - (int) ((pivotTick - this.parent.getLeftmostTick()) * zoomFactor);
            int rightmostTick = (int) ((this.parent.getRightmostTick() - pivotTick) * zoomFactor) + pivotTick;
            if ((rightmostTick - leftmostTick) > 1) {   // make sure there are at least two ticks to be drawn, if we zoom too far in, left==right, we cannot zoom out again
                this.parent.setLeftmostTick(leftmostTick);
                this.parent.setRightmostTick(rightmostTick);
            }
        }
        else if (zoomFactor > 0.0) {        // zoom out
            this.parent.setLeftmostTick(pivotTick - (int) Math.ceil((pivotTick - this.parent.getLeftmostTick()) * zoomFactor));
            if (this.parent.getLeftmostTick() < 0)
                this.parent.setLeftmostTick(0);
            this.parent.setRightmostTick((int) Math.ceil((this.parent.getRightmostTick() - pivotTick) * zoomFactor) + pivotTick);
            double numTicks = this.parent.getParent().getMsm().getEndDate();
            if (this.parent.getRightmostTick() >= numTicks)
                this.parent.setRightmostTick(numTicks - 1);
        }

        // update the cursor positions
//        this.parent.getMouseCursor().setSample(this.parent.getMouseCursor().getSample());
        this.parent.getPlaybackCursor().setSample(this.parent.getPlaybackCursor().getSample());

        this.repaint();
    }

    /**
     * create a context menu for the position of the mouse click;
     * further entries to the menu may be added by inheritances
     * @param e
     * @return
     */
    @Override
    protected WebPopupMenu getContextMenu(MouseEvent e) {
        // TODO ...
        return null;
    }

    /**
     * on mouse click event
     * @param e
     */
    @Override
    public void mouseClicked(MouseEvent e) {
        // TODO: interaction with the tempo curve

        super.mouseClicked(e);  // select the note that has been clicked, if any
    }

    /**
     * on mouse press event
     * @param e
     */
    @Override
    public void mousePressed(MouseEvent e) {

    }

    /**
     * on mouse release event
     * @param e
     */
    @Override
    public void mouseReleased(MouseEvent e) {

    }


    /**
     * on mouse enter event
     * @param e
     */
    @Override
    public void mouseEntered(MouseEvent e) {
        if (this.noData.isShowing())
            return;

        super.mouseEntered(e);
    }

    /**
     * on mouse exit event
     * @param e
     */
    @Override
    public void mouseExited(MouseEvent e) {
        if (this.noData.isShowing())
            return;

        super.mouseExited(e);
    }

    /**
     * on mouse move event
     * @param e
     */
    @Override
    public void mouseMoved(MouseEvent e) {
        if (this.noData.isShowing())
            return;

        super.mouseMoved(e);
    }

    /**
     * on mouse drag event
     * @param e
     */
    @Override
    public void mouseDragged(MouseEvent e) {
        if ((this.parent.getMouseCursor() == null)
                || (this.parent.getParent().getSyncPlayer().getSelectedPerformance() == null))
            return;

        // if audio data is available we do the drag/scrolling via the audio domain; in fact we do so as if the drag happens in the WaveformPanel
        if (this.parent.getAudio() != null) {
            long leftmost = this.parent.getLeftmostSample();
            long rightmost = this.parent.getRightmostSample();
            double sampleOffset = (double)((rightmost - leftmost) * (this.parent.getMouseCursor().getTicksX() - e.getPoint().x)) / this.parent.getWaveformPanel().getWidth();   // this computes how many horizontal pixels the mouse has moved, than scales it by the amount of samples per horizontal pixel so we know how many pixels we want to move the leftmost and rightmost sample index
            this.parent.communicateMousePositionToAllComponents(e);
            this.parent.scroll(sampleOffset);
            return;
        }

        this.scroll(e); // no audio data available, so we have to do the drag/scrolling exclusively in here
    }

    /**
     * on mouse wheel event
     * @param e
     */
    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        if (this.parent.getParent().getSyncPlayer().getSelectedPerformance() == null)
            return;

        // if audio data is available we do the zoom via the audio domain
        if (this.parent.getAudio() != null) {
            this.parent.mouseWheelMoved(e);
            return;
        }

        // no audio data available, so we have to do the zoom exclusively in here
        if (e.getWheelRotation() == 0)  // if there was no actual mouse wheel movement
            return;                     // done

        int pivotSample = (int) Math.round(this.parent.getTickIndex(e.getPoint().getX()));
        double zoomFactor = (e.getWheelRotation() < 0) ? 0.9 : 1.1;

        this.parent.communicateMousePositionToAllComponents(e);
        this.zoom(pivotSample, zoomFactor);
    }

}
