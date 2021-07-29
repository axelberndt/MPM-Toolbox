package mpmToolbox.gui.audio;

import com.alee.laf.label.WebLabel;
import com.alee.laf.menu.WebMenu;
import com.alee.laf.menu.WebMenuItem;
import com.alee.laf.menu.WebPopupMenu;
import com.alee.laf.panel.WebPanel;
import meico.audio.Audio;
import mpmToolbox.gui.Settings;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

/**
 * This class represents the waveform display in the audio tab.
 * @author Axel Berndt
 */
public class WaveformPanel extends WebPanel implements ComponentListener, MouseListener, MouseMotionListener, MouseWheelListener {
    private final WebLabel noData = new WebLabel("Select the audio recording to be displayed here via the SyncPlayer.", WebLabel.CENTER);
    private ArrayList<double[]> channels = new ArrayList<>();       // contains the waveform data for each audio channel as doubles in [-1.0, 1.0]
    private BufferedImage waveform = null;                          // the waveform image to be displayed
    private int leftmostSample = 0;                                 // the leftmost sample displayed in the image
    private int rightmostSample = 0;                                // the rightmost sample displayed in the image
    private int displayChannel = -1;                                // indicate which channel should be displayed; set < 0 to display all channels
    private Point mousePosition = null;                             // this is to keep track of the mouse position and draw a cursor on the panel


    /**
     * constructor
     */
    protected WaveformPanel() {
        super();
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
        super.paintComponent(g);                                    // this ensures that the background is filled with the standard background color

        if (this.channels.isEmpty())                                // if no audio data
            return;                                                 // display nothing, done

        Graphics2D g2 = (Graphics2D)g;                              // make g a Graphics2D object so we can use its extended drawing features

        // draw the waveform
        if (this.waveform == null) {                                                                            // if waveform != null, we have an image that can be drawn, otherwise (i.e. waveform == null) we have to draw one
            if (this.displayChannel < 0) {                                                                      // if all channels should be displayed
                ArrayList<BufferedImage> waveforms = new ArrayList<>();
                int heightSubdivision = (int) Math.floor((float) this.getHeight() / this.channels.size());      // the pixel height of the sub-images
                int maxWidth = 0;

                // make a horizontal slice of the image for each channel
                for (double[] channel : this.channels) {
                    BufferedImage waveform = Audio.convertWaveform2Image(channel, this.leftmostSample, this.rightmostSample, this.getWidth(), heightSubdivision);    // draw the waveform in the image
                    waveforms.add(waveform);
                    if (waveform.getWidth() > maxWidth)
                        maxWidth = waveform.getWidth();
                }

                // write the waveform images into this.waveform
                this.waveform = new BufferedImage(maxWidth, this.getHeight(), BufferedImage.TYPE_INT_RGB);      // we start with an empty image, all black
                for (int waveformNum = 0; waveformNum < waveforms.size(); ++waveformNum) {
                    BufferedImage waveform = waveforms.get(waveformNum);
                    int yOffset = waveformNum * heightSubdivision;                                              // the y pixel offset when writing the waveform into this.waveform
                    for (int y = 0; y < waveform.getHeight(); ++y) {                                            // for each pixel row
                        for (int x = 0; x < waveform.getWidth(); ++x) {                                         // go through each pixel
                            this.waveform.setRGB(x, y + yOffset, waveform.getRGB(x, y));                        // set the pixel color
                        }
                    }
                }
            }
            else {                                                                                              // if only one channel should be displayed
                this.waveform = Audio.convertWaveform2Image(this.channels.get(this.displayChannel), this.leftmostSample, this.rightmostSample, this.getWidth(), this.getHeight());   // draw the waveform in the image
            }
        }

        g2.drawImage(this.waveform, 0, 0, this);

        // draw the mouse cursor
        if (this.mousePosition != null) {
            g2.setColor(Settings.scoreNoteColorHighlighted);
            g2.drawLine(this.mousePosition.x, 0, this.mousePosition.x, this.getHeight());
        }
    }

    /**
     * compute which sample the mouse cursor is pointing at
     * @param mousePosition
     * @return
     */
    private int getSampleIndex(Point mousePosition) {
        double relativePosition = mousePosition.getX() / this.getWidth();
        return (int) Math.round((relativePosition * (this.rightmostSample - this.leftmostSample)) + this.leftmostSample);
    }

    /**
     * set the data that this panel should visualize
     * @param audio
     */
    protected void setAudio(Audio audio) {
        if (audio == null) {
            this.add(this.noData);
            this.channels.clear();
        } else {
            this.remove(this.noData);
            this.channels = Audio.convertByteArray2DoubleArray(audio.getAudio(), audio.getFormat());
            this.leftmostSample = 0;
            this.rightmostSample = this.channels.get(0).length - 1;
        }

        this.waveform = null;
        this.repaint();
    }

    /**
     * the action to be performed on component resize
     * @param e
     */
    @Override
    public void componentResized(ComponentEvent e) {
        this.waveform = null;
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
        switch (e.getButton()) {
            case MouseEvent.BUTTON1:                                    // left click
                break;
            case MouseEvent.BUTTON3:                                    // right click = context menu
                WebPopupMenu menu = new WebPopupMenu();

                // choose the channel(s) to be displayed
                WebMenu chooseChannel = new WebMenu("Display Channel");
                WebMenuItem allChannels = new WebMenuItem("All Channels");
                allChannels.addActionListener(actionEvent -> {
                    this.displayChannel = -1;
                    this.waveform = null;
                    this.repaint();
                });
                chooseChannel.add(allChannels);
                for (int channel = 0; channel < this.channels.size(); ++channel) {
                    WebMenuItem channelItem = new WebMenuItem(String.valueOf(channel));
                    int finalChannel = channel;
                    channelItem.addActionListener(actionEvent -> {
                        this.displayChannel = finalChannel;
                        this.waveform = null;
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
        this.mousePosition = e.getPoint();
        this.repaint();
    }

    /**
     * on mouse exit event
     * @param e
     */
    @Override
    public void mouseExited(MouseEvent e) {
        this.mousePosition = null;
        this.repaint();
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

        double sampleOffset = (double)((this.rightmostSample - this.leftmostSample) * (this.mousePosition.x - e.getPoint().x)) / this.getWidth();   // this computes how many horizontal pixels the mouse has moved, than scales it by the amount of samples per horizontal pixel so we know how many pixels we want to move the leftmost and rightmost sample index
        sampleOffset = (sampleOffset > 0) ? Math.min(this.channels.get(0).length - 1 - this.rightmostSample, Math.round(sampleOffset)) : Math.max(-this.leftmostSample, Math.round(sampleOffset));  // we have to check that we don't go beyond the first and last sample; as we move those indices only in integer steps there is a certain numeric error causing the samples moving with a bit different speed than the mouse was moved, but it is not problematic

        if (sampleOffset == 0.0)                    // if no change
            return;                                 // done, we don't update the mouse position so we can check next time if in sum the mouse moved far enough

        // move the sample indices
        this.leftmostSample += sampleOffset;
        this.rightmostSample += sampleOffset;

        this.mousePosition = e.getPoint();          // update the mouse position
        this.waveform = null;                       // trigger a re-rendering of the waveform image at the next repaint
        this.repaint();                             // trigger repaint
    }

    /**
     * on mouse move event
     * @param e
     */
    @Override
    public void mouseMoved(MouseEvent e) {
        this.mousePosition = e.getPoint();
        this.repaint();
    }

    /**
     * on mouse wheel event
     * @param e
     */
    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        int pivotSample = this.getSampleIndex(e.getPoint());

        if (e.getWheelRotation() < 0) {         // zoom in
            int leftmostSample = pivotSample - (int) ((pivotSample - this.leftmostSample) * 0.9);
            int rightmostSample = (int) ((this.rightmostSample - pivotSample) * 0.9) + pivotSample;
            if ((rightmostSample - leftmostSample) > 1) {                  // make sure there are at least two samples to be drawn, if we zoom too far in, left==right, we cannot zoom out again
                this.leftmostSample = leftmostSample;
                this.rightmostSample = rightmostSample;
            }
        }
        else if (e.getWheelRotation() > 0) {    // zoom out
            this.leftmostSample = pivotSample - (int) Math.ceil((pivotSample - this.leftmostSample) * 1.1);
            if (this.leftmostSample < 0)
                this.leftmostSample = 0;
            this.rightmostSample = (int) Math.ceil((this.rightmostSample - pivotSample) * 1.1) + pivotSample;
            if (this.rightmostSample >= this.channels.get(0).length)
                this.rightmostSample = this.channels.get(0).length - 1;
        }
        else                                    // in any other case
            return;                             // cancel

        this.waveform = null;
        this.repaint();
    }
}
