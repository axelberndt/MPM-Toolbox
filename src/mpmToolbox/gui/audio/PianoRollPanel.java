package mpmToolbox.gui.audio;

import com.alee.laf.label.WebLabel;
import com.alee.laf.menu.WebCheckBoxMenuItem;
import com.alee.laf.menu.WebMenuItem;
import com.alee.laf.menu.WebPopupMenu;
import com.alee.laf.panel.WebPanel;
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
    protected final AudioDocumentData parent;
    protected final WebLabel noData;
    protected Point mousePosition = null;                   // this is to keep track of the mouse position and draw a cursor on the panel
    protected boolean mouseInThisPanel = false;             // this is set true when the mouse enters this panel and false if the mouse exits
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
     */
    protected PianoRollPanel(AudioDocumentData parent, String noDataText) {
        super();
        this.parent = parent;

        this.noData = new WebLabel(noDataText, WebLabel.CENTER);
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
        // TODO: something meaningful?
    }

    /**
     * draw the piano roll of the currently chosen audio data's alignment into the specified Graphics2D object
     * @param g2d
     */
    protected void drawPianoRoll(Graphics2D g2d) {
        if (this.parent.getAlignment() == null)
            return;

        // draw piano roll overlay
        double fromMilliseconds = ((double) this.parent.getLeftmostSample() / this.parent.getAudio().getFrameRate()) * 1000.0;
        double toMilliseconds = ((double) this.parent.getRightmostSample() / this.parent.getAudio().getFrameRate()) * 1000.0;

        PianoRoll pianoRoll = this.retrievePianoRoll(fromMilliseconds, toMilliseconds, this.getWidth(), 128);
        g2d.drawImage(pianoRoll, 0, this.getHeight(), this.getWidth(), -this.getHeight(), this);
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
            return this.parent.getAlignment().getPart(partNumber).getPianoRoll(fromMilliseconds, toMilliseconds, this.getWidth(), 128);
        }

        // draw all musical parts
        return this.parent.getAlignment().getPianoRoll(fromMilliseconds, toMilliseconds, width, height);
//        return this.parent.getAudio().getAlignment().getPianoRoll(fromMilliseconds, toMilliseconds, width, height);
//        return (new Alignment(this.parent.parent.getSyncPlayer().getSelectedPerformance().perform(this.parent.parent.getMsm()), null)).getPianoRoll(fromMilliseconds, toMilliseconds, width, height);
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
     * compute which sample the mouse cursor is pointing at
     * @param x horizontal pixel position in the panel
     * @return
     */
    protected int getSampleIndex(double x) {
        double relativePosition = x / this.getWidth();
        return (int) Math.round((relativePosition * (this.parent.getRightmostSample() - this.parent.getLeftmostSample())) + this.parent.getLeftmostSample());
    }

    /**
     * create a context menu for the position of the mouse click;
     * further entries to the menu may be added by inheritances
     * @param e
     * @return
     */
    protected WebPopupMenu getContextMenu(MouseEvent e) {
        WebPopupMenu menu = new WebPopupMenu();

        // make "note fixed" entry
        Note note = this.getNoteAt(e.getPoint().getX() / this.getWidth(), e.getPoint().getY() / this.getHeight());
        if (note != null) {
            WebCheckBoxMenuItem setFixed = new WebCheckBoxMenuItem("Note fixed", note.isFixed());
            setFixed.addActionListener(actionEvent -> {
                note.setFixed(!note.isFixed());
                this.parent.getAlignment().updateTiming();
                this.parent.getAlignment().recomputePianoRoll();
                this.parent.repaintAllComponents();
            });
            setFixed.setToolTipText("Pins the note at its position.");
            menu.add(setFixed);
        }

        return menu;
    }

    /**
     * set the mouse position
     * @param e
     */
    protected void setMousePosition(MouseEvent e) {
        if (e == null)
            this.mousePosition = null;
        else
            this.mousePosition = e.getPoint();
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
        this.mouseInThisPanel = true;
        this.parent.communicateMouseEventToAllComponents(e);
        this.parent.repaintAllComponents();
    }

    /**
     * on mouse exit event
     * @param e
     */
    @Override
    public void mouseExited(MouseEvent e) {
        this.mouseInThisPanel = false;
        this.parent.communicateMouseEventToAllComponents(null);
        this.parent.repaintAllComponents();
    }

    /**
     * on mouse move event
     *
     * @param e
     */
    @Override
    public void mouseMoved(MouseEvent e) {
        this.parent.communicateMouseEventToAllComponents(e);
        this.parent.repaintAllComponents();

        Note note = this.getNoteAt(this.mousePosition.getX() / this.getWidth(), this.mousePosition.getY() / this.getHeight());
        this.setCursor((note == null) ? Cursor.getDefaultCursor() : new Cursor(Cursor.HAND_CURSOR));
    }

    /**
     * on mouse click event
     * @param e
     */
    @Override
    public void mouseClicked(MouseEvent e) {
        Note note = this.getNoteAt(this.mousePosition.getX() / this.getWidth(), this.mousePosition.getY() / this.getHeight());
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
        if (this.mousePosition == null)
            return;

        if (!this.dragGesture.lockedOnNote) {               // if no drag gesture currently running, we start a new one
            this.dragGesture.lockedOnNote = true;           // lock the drag gesture on ...
            this.dragGesture.note = this.getNoteAt(this.mousePosition.getX() / this.getWidth(), this.mousePosition.getY() / this.getHeight());  // the note that the mouse is currently at or null
        }

        if (this.dragGesture.note == null) {                // if we perform a drag event with no note, it should be interpreted as scrolling
            this.parent.scroll(e);
            return;
        }

        // we perform a drag event on a note
        this.setCursor(new Cursor(Cursor.W_RESIZE_CURSOR));

        double pixelOffset = e.getX() - this.mousePosition.getX();
        int samplesInFrame = this.parent.getRightmostSample() - this.parent.getLeftmostSample();
        double sampleOffset = (pixelOffset * samplesInFrame) / this.getWidth();
        double millisecOffset = (sampleOffset * 1000.0) / this.parent.getAudio().getFrameRate();

        this.parent.getAlignment().reposition(this.dragGesture.note, this.dragGesture.note.getMillisecondsDate() + millisecOffset);    // move the note and do the timing transform
        this.parent.getAlignment().updateTiming();
        this.parent.getAlignment().recomputePianoRoll();

        this.parent.communicateMouseEventToAllComponents(e);
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

    protected static class NoteDrag {
        protected boolean lockedOnNote = false;
        protected Note note = null;
    }
}
