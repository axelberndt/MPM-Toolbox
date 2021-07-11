package mpmToolbox.gui.audio;

import com.alee.laf.label.WebLabel;
import com.alee.laf.panel.WebPanel;
import meico.audio.Audio;

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
    private BufferedImage waveform = null;
    private double zoomFactor = 1.0;                                // the horizontal zoom factor of the displayed waveform, 1.0 means that the complete waveform is displayed over the full width
    private long leftSampleIndex = 0;                               // the sample index of the left most sample in the display window

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

        // TODO: placeholder code
        if (this.waveform == null) {
            this.waveform = new BufferedImage(this.getWidth(), this.getHeight(), BufferedImage.TYPE_INT_RGB);

            Color color = new Color(0, 255, 0);
            for (int x = 0; x < this.waveform.getWidth(); ++x) {
                for (int y = 0; y < this.waveform.getHeight(); ++y) {
                    this.waveform.setRGB(x, y, color.getRGB());
                }
            }
        }

        g2.drawImage(this.waveform, 0, 0, this);
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
    }

    /**
     * on mouse exit event
     * @param e
     */
    @Override
    public void mouseExited(MouseEvent e) {
    }

    /**
     * on mouse drag event
     * @param e
     */
    @Override
    public void mouseDragged(MouseEvent e) {
    }

    /**
     * on mouse move event
     * @param e
     */
    @Override
    public void mouseMoved(MouseEvent e) {
    }

    /**
     * on mouse wheel event
     * @param e
     */
    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
    }
}
