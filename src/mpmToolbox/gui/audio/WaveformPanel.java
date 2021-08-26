package mpmToolbox.gui.audio;

import com.alee.laf.label.WebLabel;
import com.alee.laf.menu.WebMenu;
import com.alee.laf.menu.WebMenuItem;
import com.alee.laf.menu.WebPopupMenu;
import com.alee.laf.panel.WebPanel;
import mpmToolbox.gui.Settings;
import mpmToolbox.projectData.Audio;
import mpmToolbox.supplementary.Tools;

import java.awt.*;
import java.awt.event.*;

/**
 * This class represents the waveform display in the audio tab.
 * @author Axel Berndt
 */
public class WaveformPanel extends WebPanel implements ComponentListener, MouseListener, MouseMotionListener, MouseWheelListener {
    private final AudioDocumentData parent;
    private final WebLabel noData = new WebLabel("Select the audio recording to be displayed here via the SyncPlayer.", WebLabel.CENTER);
    private Point mousePosition = null;                 // this is to keep track of the mouse position and draw a cursor on the panel
    private boolean mouseInThisPanel = false;           // this is set true when the mouse enters this panel and false if the mouse exits

    /**
     * constructor
     */
    protected WaveformPanel(AudioDocumentData parent) {
        super();
        this.parent = parent;
        this.add(this.noData);

        this.addComponentListener(this);
        this.addMouseListener(this);
        this.addMouseMotionListener(this);
        this.addMouseWheelListener(this);
    }

    /**
     * draw the waveform
     * @param g
     */
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);                        // this ensures that the background is filled with the standard background color

        Audio.WaveformImage waveformImage = this.parent.getWaveformImage(this.getWidth(), this.getHeight());
        if (waveformImage == null)
            return;

        Graphics2D g2 = (Graphics2D)g;                  // make g a Graphics2D object so we can use its extended drawing features

        g2.drawImage(waveformImage, 0, 0, this);        // draw the waveform

        // draw the mouse cursor
        if (this.mousePosition != null) {
            g2.setColor(Settings.scoreNoteColorHighlighted);
            g2.drawLine(this.mousePosition.x, 0, this.mousePosition.x, this.getHeight());
//            g2.drawLine(0, this.mousePosition.y, this.getWidth(), this.mousePosition.y);

            // print info text
            int sampleIndex = this.getSampleIndex(this.mousePosition);
            double millisec = Tools.round(((double) sampleIndex / this.parent.getAudio().getFrameRate()) * 1000.0, 2);
            g2.setColor(Color.LIGHT_GRAY);
            g2.drawString("Sample No.: " + sampleIndex, 2, Settings.getDefaultFontSize());
            g2.drawString("Milliseconds: " + millisec, 2, Settings.getDefaultFontSize() * 2);
        }
    }

    /**
     * compute which sample the mouse cursor is pointing at
     * @param mousePosition
     * @return
     */
    protected int getSampleIndex(Point mousePosition) {
        double relativePosition = mousePosition.getX() / this.getWidth();
        return (int) Math.round((relativePosition * (this.parent.getRightmostSample() - this.parent.getLeftmostSample())) + this.parent.getLeftmostSample());
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
     * on mouse click event
     * @param e
     */
    @Override
    public void mouseClicked(MouseEvent e) {
        if (this.parent.getAudio() == null)     // if there is no audio data
            return;                             // nothing to do here

        switch (e.getButton()) {
            case MouseEvent.BUTTON1:                    // left click
                // TODO: Place a marker
                break;
            case MouseEvent.BUTTON3:                    // right click = context menu
                WebPopupMenu menu = new WebPopupMenu();

                // play from here
                WebMenuItem playFromHere = new WebMenuItem("Play from here");
                playFromHere.addActionListener(actionEvent -> {
                    this.parent.getParent().getSyncPlayer().triggerPlayback(this.getSampleIndex(e.getPoint()));
                });
                menu.add(playFromHere);

                // choose the channel(s) to be displayed
                WebMenu chooseChannel = new WebMenu("Display Channel");
                WebMenuItem allChannels = new WebMenuItem("All Channels");
                allChannels.addActionListener(actionEvent -> {
                    this.parent.setChannelNumber(-1);
                    this.repaint();
                });
                chooseChannel.add(allChannels);
                for (int channel = 0; channel < this.parent.getAudio().getWaveforms().size(); ++channel) {
                    WebMenuItem channelItem = new WebMenuItem(String.valueOf(channel));
                    int finalChannel = channel;
                    channelItem.addActionListener(actionEvent -> {
                        this.parent.setChannelNumber(finalChannel);
                        this.repaint();
                    });
                    chooseChannel.add(channelItem);
                }
                menu.add(chooseChannel);

                menu.show(this, e.getX() - 25, e.getY());
                break;
        }
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
     * on mouse drag event
     * @param e
     */
    @Override
    public void mouseDragged(MouseEvent e) {
        if (this.mousePosition == null) {
            this.mousePosition = e.getPoint();
            return;
        }

        int leftmost = this.parent.getLeftmostSample();
        int rightmost = this.parent.getRightmostSample();
        double sampleOffset = (double)((rightmost - leftmost) * (this.mousePosition.x - e.getPoint().x)) / this.getWidth();   // this computes how many horizontal pixels the mouse has moved, than scales it by the amount of samples per horizontal pixel so we know how many pixels we want to move the leftmost and rightmost sample index

        this.parent.communicateMouseEventToAllComponents(e);
        this.parent.scroll(sampleOffset);
    }

    /**
     * on mouse move event
     * @param e
     */
    @Override
    public void mouseMoved(MouseEvent e) {
        this.parent.communicateMouseEventToAllComponents(e);
        this.parent.repaintAllComponents();
    }

    /**
     * on mouse wheel event
     * @param e
     */
    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        if ((this.parent.getAudio() == null) || (e.getWheelRotation() == 0))
            return;

        int pivotSample = this.getSampleIndex(e.getPoint());
        double zoomFactor = (e.getWheelRotation() < 0) ? 0.9 : 1.1;

        this.parent.communicateMouseEventToAllComponents(e);
        this.parent.zoom(pivotSample, zoomFactor);
    }
}
