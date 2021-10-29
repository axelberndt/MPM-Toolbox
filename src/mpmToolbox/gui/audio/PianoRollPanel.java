package mpmToolbox.gui.audio;

import com.alee.laf.label.WebLabel;
import com.alee.laf.panel.WebPanel;

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
    protected Point mousePosition = null;               // this is to keep track of the mouse position and draw a cursor on the panel
    protected boolean mouseInThisPanel = false;         // this is set true when the mouse enters this panel and false if the mouse exits

    protected PianoRollPanel(AudioDocumentData parent) {
        this(parent, "Select a part in the Musical Sequence Markup tree.");
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
     * set the data that this panel should visualize
     */
    protected void setAudio() {
        if (this.parent.getAudio() == null) {
            this.add(this.noData);
            return;
        }

        this.remove(this.noData);
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
    }

    /**
     * on mouse click event
     * @param e
     */
    @Override
    public void mouseClicked(MouseEvent e) {

    }

    /**
     * on mouse drag event
     * @param e
     */
    @Override
    public void mouseDragged(MouseEvent e) {
        this.parent.mouseDragged(e);
    }

    /**
     * on mouse wheel event
     * @param e
     */
    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        this.parent.mouseWheelMoved(e);
    }
}
