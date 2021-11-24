package mpmToolbox.gui.audio;

import com.alee.laf.label.WebLabel;
import com.alee.laf.menu.WebMenu;
import com.alee.laf.menu.WebMenuItem;
import com.alee.laf.panel.WebPanel;
import meico.mei.Helper;
import mpmToolbox.projectData.alignment.PianoRoll;
import nu.xom.Element;

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

        // TODO: draw piano roll overlay; this is just test code, yet
        double fromMilliseconds = ((double) this.parent.getLeftmostSample() / this.parent.getAudio().getFrameRate()) * 1000.0;
        double toMilliseconds = ((double) this.parent.getRightmostSample() / this.parent.getAudio().getFrameRate()) * 1000.0;
        PianoRoll pianoRoll = this.parent.getAlignment().getPianoRoll(fromMilliseconds, toMilliseconds, this.getWidth(), 128);
//        PianoRoll pianoRoll = this.parent.getAudio().getAlignment().getPianoRoll(fromMilliseconds, toMilliseconds, this.getWidth(), 128);
//        PianoRoll pianoRoll = (new Alignment(this.parent.parent.getSyncPlayer().getSelectedPerformance().perform(this.parent.parent.getMsm()), null)).getPianoRoll(fromMilliseconds, toMilliseconds, this.getWidth(), 128);
        g2d.drawImage(pianoRoll, 0, this.getHeight(), this.getWidth(), -this.getHeight(), this);

    }

    /**
     * create context menu submenu of the part to be displayed in the piano roll overlay
     * @return
     */
    protected WebMenu getPianoRollTools() {
        WebMenu pianoRollTools = new WebMenu("Piano Roll");

        if (this.parent.getAlignment() == null) {
            pianoRollTools.setEnabled(false);
        } else {
            pianoRollTools.setEnabled(true);

            WebMenu choosePart = new WebMenu("Choose Part");
            choosePart.setToolTipText("Select the part displayed in the piano roll overlay.");

            WebMenuItem perfAllParts = new WebMenuItem("All Parts");
            choosePart.add(perfAllParts);
            perfAllParts.addActionListener(actionEvent -> {
                // TODO ...
            });

            for (Element partElt : this.parent.getParent().getMsm().getParts()) {
                int number = Integer.parseInt(Helper.getAttributeValue("number", partElt));
                String name = "Part " + number + " " + Helper.getAttributeValue("name", partElt);
                WebMenuItem partItem = new WebMenuItem(name);
                choosePart.add(partItem);
                partItem.addActionListener(actionEvent -> {
                    // TODO ...
                });
            }

            pianoRollTools.add(choosePart);
        }

        return pianoRollTools;
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
