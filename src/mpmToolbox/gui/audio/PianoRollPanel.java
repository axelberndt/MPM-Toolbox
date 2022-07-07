package mpmToolbox.gui.audio;

import com.alee.laf.label.WebLabel;
import com.alee.laf.menu.WebCheckBoxMenuItem;
import com.alee.laf.menu.WebMenuItem;
import com.alee.laf.menu.WebPopupMenu;
import com.alee.laf.panel.WebPanel;
import meico.mpm.elements.Performance;
import mpmToolbox.gui.Settings;
import mpmToolbox.gui.audio.utilities.ArticulationMenu;
import mpmToolbox.gui.audio.utilities.CursorPositions;
import mpmToolbox.gui.msmTree.MsmTree;
import mpmToolbox.gui.msmTree.MsmTreeNode;
import mpmToolbox.projectData.alignment.Note;
import mpmToolbox.projectData.alignment.PianoRoll;

import java.awt.*;
import java.awt.event.*;

/**
 * This implements the piano roll display of MSM data.
 * It is also the basis of classes WaveformPanel and SpectrogramPanel.
 * @author Axel Berndt
 */
public class PianoRollPanel extends WebPanel implements ComponentListener, MouseListener, MouseMotionListener, MouseWheelListener {
    public final AudioDocumentData parent;                  // the container
    protected final WebLabel noData;                        // to be displayed when no data is there to be visualized
    protected Integer mousePositionY = null;                  // if the mouse is in this panel, this is set to its y pixel coordinate
    protected final NoteDrag dragGesture = new NoteDrag();  // this is set true when a track gesture is started, so that even iv the mouse moves over other notes or free space, only the initial note is dragged

    /**
     * constructor
     * @param parent
     */
    protected PianoRollPanel(AudioDocumentData parent) {
        this(parent, "Select a performance in the SyncPlayer.");
    }

    /**
     * constructor
     * @param parent
     * @param noDataText
     */
    protected PianoRollPanel(AudioDocumentData parent, String noDataText) {
        super();
        this.parent = parent;

        this.noData = new WebLabel(noDataText, WebLabel.CENTER);
        this.noData.setOpaque(false);
        this.add(this.noData);

        this.addComponentListener(this);
        this.addMouseListener(this);
        this.addMouseMotionListener(this);
        this.addMouseWheelListener(this);
    }

    /**
     * draw the component
     * @param g
     */
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

//        Graphics2D g2 = (Graphics2D)g;                  // make g a Graphics2D object, so we can use its extended drawing features
//        this.drawPianoRoll(g2);
//        this.drawPlaybackCursor(g2);
//        this.drawMouseCursor(g2);
    }

    /**
     * draw the lines of the mouse cursor in the Graphics2D object
     * @param g2d
     * @return true if a valid mouse position was available and the drawing was successful
     */
    protected boolean drawMouseCursor(Graphics2D g2d) {
        if (this.parent.getMouseCursor() == null)
            return false;

        g2d.setColor(Settings.scoreNoteColorHighlighted);
        Stroke defaultStroke = g2d.getStroke();                                         // keep the previous stroke settings
        g2d.setStroke(new BasicStroke(this.parent.getMouseCursor().getAudioXSpread())); // set the stroke
        g2d.drawLine(this.parent.getMouseCursor().getAudioX(), 0, this.parent.getMouseCursor().getAudioX(), this.getHeight());
        g2d.setStroke(defaultStroke);                                                   // switch back to the previous stroke settings

        if (this.mouseInThisPanel())
            g2d.drawLine(0, this.mousePositionY, this.getWidth(), this.mousePositionY);

        return true;
    }

    /**
     * draw the playback cursor line in the Graphics2D object
     * @param g2d
     */
    protected void drawPlaybackCursor(Graphics2D g2d) {
        Double pos = this.parent.getRelativePlaybackPosInAudio();
        if (pos == null)
            return;

        int x = (int) Math.round(this.getWidth() * pos);
        g2d.setColor(Color.GRAY);
        g2d.drawLine(x, 0, x, this.getHeight());
    }

    /**
     * draw the piano roll of the currently chosen audio data's alignment into the specified Graphics2D object
     * @param g2d
     * @return success, i.e. false if there was no alignment available to be drawn, otherwise true
     */
    protected boolean drawPianoRoll(Graphics2D g2d) {
        if (this.parent.getAlignment() == null)
            return false;

        // draw piano roll overlay
        double fromMilliseconds = ((double) this.parent.getLeftmostSample() / this.parent.getAudio().getFrameRate()) * 1000.0;
        double toMilliseconds = ((double) this.parent.getRightmostSample() / this.parent.getAudio().getFrameRate()) * 1000.0;

        PianoRoll pianoRoll = this.retrievePianoRoll(fromMilliseconds, toMilliseconds, this.getWidth(), 128);
        g2d.drawImage(pianoRoll, 0, this.getHeight(), this.getWidth(), -this.getHeight(), this);

        return true;
    }

    /**
     * retrieve the piano roll from the parent
     * @param fromMilliseconds
     * @param toMilliseconds
     * @param width
     * @param height
     * @return
     */
    protected PianoRoll retrievePianoRoll(double fromMilliseconds, double toMilliseconds, int width, int height) {
        Integer partNumber = this.parent.getPianoRollPartNumber();
        if (partNumber != null) {       // draw only the selected musical part
            return this.parent.getAlignment().getPart(partNumber).getPianoRoll(fromMilliseconds, toMilliseconds, this.getWidth(), height);
        }

        // draw all musical parts
        return this.parent.getAlignment().getPianoRoll(fromMilliseconds, toMilliseconds, width, height);
//        return this.parent.getAudio().getAlignment().getPianoRoll(fromMilliseconds, toMilliseconds, width, height);
//        return (new Alignment(this.parent.parent.getSyncPlayer().getSelectedPerformance().perform(this.parent.parent.getMsm()), null)).getPianoRoll(fromMilliseconds, toMilliseconds, width, height);
    }

    public boolean mouseInThisPanel() {
        return this.mousePositionY != null;
    }

    /**
     * retrieve the note reference behind a certain pixel position in the current piano roll image
     * @param x horizontal pixel position in the piano roll image
     * @param y vertical pixel position in the piano roll image
     * @return the Note object or null
     */
    protected Note getNoteAt(int x, int y) {
        if (this.parent.getAlignment() == null)
            return null;

        Integer partNumber = this.parent.getPianoRollPartNumber();
        PianoRoll pianoRoll = (partNumber != null) ? this.parent.getAlignment().getPart(partNumber).getPianoRoll() : this.parent.getAlignment().getPianoRoll();
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
    protected Note getNoteAt(double x, double y) {
        if (this.parent.getAlignment() == null)
            return null;

        Integer partNumber = this.parent.getPianoRollPartNumber();
        PianoRoll pianoRoll = (partNumber != null) ? this.parent.getAlignment().getPart(partNumber).getPianoRoll() : this.parent.getAlignment().getPianoRoll();
        if (pianoRoll == null)
            return null;

        return pianoRoll.getNoteAt((int) (pianoRoll.getWidth() * x), (int) (pianoRoll.getHeight() * (-y + 1.0)));
    }

    /**
     * set the data that this panel should visualize
     */
    protected void setAudio() {
        if (this.parent.getAudio() == null)
            this.add(this.noData);
        else
            this.remove(this.noData);
    }

    /**
     * create a context menu for the position of the mouse click;
     * further entries to the menu may be added by inheritances
     * @param e
     * @return
     */
    protected WebPopupMenu getContextMenu(MouseEvent e) {
        WebPopupMenu menu = new WebPopupMenu();

        // determine the performance currently displayed, or null if it is an alignment
        Performance performance = this.parent.getParent().getSyncPlayer().getSelectedPerformance();
        if (/*(performance == null) ||*/ !this.parent.getParent().getMpm().getAllPerformances().contains(performance))  // we have no performance to create an articulation
            performance = null;

        // make "note fixed" entry
        Note note = this.getNoteAt(e.getPoint().getX() / this.getWidth(), e.getPoint().getY() / this.getHeight());
        if (note != null) {
            // "Note fixed" checkbox
            WebCheckBoxMenuItem setFixed = new WebCheckBoxMenuItem("Note fixed", (note.isFixed() || (performance != null)));
            setFixed.addActionListener(actionEvent -> {
                note.setFixed(!note.isFixed());
                this.parent.getAlignment().updateTiming();
                this.parent.getAlignment().recomputePianoRoll();
                this.parent.repaintAllComponents();
            });
            if (performance == null) {
                setFixed.setToolTipText("Pins the note at its position.");
                setFixed.setEnabled(true);
            } else {
                setFixed.setToolTipText("The position of the note is determined by the performance. Therefore, it cannot be dragged.");
                setFixed.setEnabled(false);
            }
            menu.add(setFixed);

            // articulate note
            menu.add(new ArticulationMenu(note, performance, this.parent.getParent().getMpmTree()));
        }

        // play from here
        WebMenuItem playFromHere = new WebMenuItem("Play from here");
        playFromHere.addActionListener(actionEvent -> {
            this.parent.getParent().getSyncPlayer().triggerPlayback(this.parent.getSampleIndex(e.getPoint().getX()));
        });
        menu.add(playFromHere);

        return menu;
    }

    /**
     * the action to be performed on component resize
     * @param e
     */
    @Override
    public void componentResized(ComponentEvent e) {
        this.repaint();
    }

    /**
     * the action to be performed on component move
     * @param e
     */
    @Override
    public void componentMoved(ComponentEvent e) {
    }

    /**
     * the action to be performed on component show
     * @param e
     */
    @Override
    public void componentShown(ComponentEvent e) {
    }

    /**
     * the action to be performed on component hide
     * @param e
     */
    @Override
    public void componentHidden(ComponentEvent e) {
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
        this.dragGesture.lockedOnNote = false;              // open the drag gesture lock
        this.dragGesture.note = null;                       // forget the note we might have been dragging
        this.mouseMoved(e);                                 // do the same as if the mouse was just moved
    }

    /**
     * on mouse enter event
     * @param e
     */
    @Override
    public void mouseEntered(MouseEvent e) {
        this.mousePositionY = e.getY();
        this.parent.communicateMousePositionToAllComponents(e);
        this.parent.repaintAllComponents();
    }

    /**
     * on mouse exit event
     * @param e
     */
    @Override
    public void mouseExited(MouseEvent e) {
        this.mousePositionY = null;
        this.parent.communicateMousePositionToAllComponents(null);
        this.parent.repaintAllComponents();
    }

    /**
     * on mouse move event
     * @param e
     */
    @Override
    public void mouseMoved(MouseEvent e) {
        this.mousePositionY = e.getY();
        this.parent.communicateMousePositionToAllComponents(e);
        this.parent.repaintAllComponents();

        if (this.parent.getAlignment() != null) {
            Note note = this.getNoteAt(e.getPoint().getX() / this.getWidth(), e.getPoint().getY() / this.getHeight());
            this.setCursor((note == null) ? Cursor.getDefaultCursor() : new Cursor(Cursor.HAND_CURSOR));
        }
    }

    /**
     * on mouse click event
     * @param e
     */
    @Override
    public void mouseClicked(MouseEvent e) {
        Note note = this.getNoteAt(e.getPoint().getX() / this.getWidth(), e.getPoint().getY() / this.getHeight());
        if (note == null)
            return;

        MsmTree msmTree = this.parent.getParent().getMsmTree();         // a handle to the msm tree
        MsmTreeNode msmTreeNode = this.parent.getParent().getMsmTree().findNode(note.getId(), true);
        if (msmTreeNode == null)                                        // if nothing has been selected
            return;                                                     // done

        msmTree.setSelectedNode(msmTreeNode);                           // select the node in the msm tree
        msmTree.scrollPathToVisible(msmTreeNode.getTreePath());         // scroll the tree so the node is visible
    }

    /**
     * on mouse drag event
     * @param e
     */
    @Override
    public void mouseDragged(MouseEvent e) {
        if (this.parent.getMouseCursor() == null)
            return;

        if (this.parent.getParent().getSyncPlayer().isAudioAlignmentSelected()  // if we display an audio alignment; a performance rendering cannot be altered by note dragging
                && !this.dragGesture.lockedOnNote) {                            // if no drag gesture currently running, we start a new one
            this.dragGesture.lockedOnNote = true;                               // lock the drag gesture on ...
            this.dragGesture.note = this.getNoteAt(((double) this.parent.getMouseCursor().getAudioX()) / this.getWidth(), ((double) this.mousePositionY) / this.getHeight());  // the note that the mouse is currently at or null
        }

        if (this.dragGesture.note == null) {                                    // if we perform a drag event with no note, it should be interpreted as scrolling
            this.parent.scroll(e);
            return;
        }

        // we perform a drag event on a note
        this.setCursor(new Cursor(Cursor.W_RESIZE_CURSOR));

        double pixelOffset = e.getX() - this.parent.getMouseCursor().getAudioX();
        long samplesInFrame = this.parent.getRightmostSample() - this.parent.getLeftmostSample();
        double sampleOffset = (pixelOffset * samplesInFrame) / this.getWidth();
        double millisecOffset = (sampleOffset * 1000.0) / this.parent.getAudio().getFrameRate();

        this.parent.getAlignment().reposition(this.dragGesture.note, this.dragGesture.note.getMillisecondsDate() + millisecOffset);    // move the note and do the timing transform
        this.parent.getAlignment().recomputePianoRoll();

        this.parent.communicateMousePositionToAllComponents(e);
        this.parent.repaintAllComponents();
    }

    /**
     * on mouse wheel event
     * @param e
     */
    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        this.parent.mouseWheelMoved(e);
    }

    /**
     * this class is used for drag gestures and provides context data
     * @author Axel Berndt
     */
    protected static class NoteDrag {
        protected boolean lockedOnNote = false;
        protected Note note = null;
    }
}
