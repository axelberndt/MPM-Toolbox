package mpmToolbox.gui.audio;

import com.alee.laf.button.WebButton;
import com.alee.laf.label.WebLabel;
import com.alee.laf.panel.WebPanel;
import com.tagtraum.jipes.audio.LogFrequencySpectrum;
import com.tagtraum.jipes.math.WindowFunction;
import meico.audio.Audio;
import mpmToolbox.gui.Settings;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;

/**
 * This class represents the spectrogram display in the audio tab.
 * @author Axel Berndt
 */
public class SpectrogramPanel extends WebPanel implements ComponentListener {
    private final WebLabel noData = new WebLabel("Select the audio recording to be displayed here via the SyncPlayer.", WebLabel.CENTER);
    private final WebButton computeButton = new WebButton("Compute Spectrogram (takes some time!)", actionEvent -> this.computeSpectrogram());
    private Audio audio = null;                                     // the audio data to be displayed
    private ArrayList<LogFrequencySpectrum> spectrogram = null;     // the spectrogram data of the audio
    private final DisplayMetrics displayMetrics = new DisplayMetrics();
    private Point mousePosition = null;                             // this is to keep track of the mouse position and draw a cursor on the panel

    /**
     * constructor
     */
    protected SpectrogramPanel() {
        super();
        this.add(this.noData);
        this.addComponentListener(this);
    }

    /**
     * draw the waveform
     *
     * @param g
     */
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);                                    // this ensures that the background is filled with the standard background color

        if (this.audio == null)                                     // if no audio
            return;                                                 // display nothing, done

        Graphics2D g2 = (Graphics2D) g;                             // make g a Graphics2D object so we can use its extended drawing features

        int imageWidth = (int) Math.round(((double) this.getWidth()) / (this.displayMetrics.relativeRightmostPosition - this.displayMetrics.relativeLeftmostPosition));
        int xOffset = (int) Math.round(-imageWidth * this.displayMetrics.relativeLeftmostPosition);

        g2.drawImage(this.displayMetrics.spectrogramImage, xOffset, 0, imageWidth-xOffset, this.getHeight(), this);

        // draw the mouse cursor
        if (this.mousePosition != null) {
            g2.setColor(Settings.scoreNoteColorHighlighted);
            g2.drawLine(this.mousePosition.x, 0, this.mousePosition.x, this.getHeight());
        }
    }

    /**
     * set the data that this panel should visualize
     *
     * @param audio
     */
    protected void setAudio(Audio audio) {
        this.audio = audio;

        if (this.audio == null) {
            this.add(this.noData);
        }
        else {
            this.remove(this.noData);
            this.add(this.computeButton);
            this.displayMetrics.relativeLeftmostPosition = 0.0;
            this.displayMetrics.relativeRightmostPosition = 1.0;
            this.displayMetrics.spectrogramImage = null;
        }

        this.repaint();
    }

    /**
     * This triggers the computation of the spectrogram and its display. It can take some time!
     */
    private void computeSpectrogram() {
        this.getRootPane().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));   // change mouse cursor to busy

        try {   // compute the spectrogram
            this.spectrogram = this.audio.exportConstantQTransformSpectrogram(new WindowFunction.Hamming(2048), 1024, 20.0f, 10000.0f, 3);
        } catch (IOException e) {
            this.spectrogram = null;
            this.displayMetrics.spectrogramImage = null;
            e.printStackTrace();
        }
        this.displayMetrics.spectrogramImage = Audio.convertSpectrogramToImage(this.spectrogram);

        this.getRootPane().setCursor(Cursor.getDefaultCursor());                        // change mouse cursor back to default

        this.remove(this.noData, this.computeButton);
        this.repaint();
    }

    /**
     * the action to be performed on component resize
     *
     * @param e
     */
    @Override
    public void componentResized(ComponentEvent e) {
        this.repaint();
    }

    /**
     * the action to be performed on component move
     *
     * @param e
     */
    @Override
    public void componentMoved(ComponentEvent e) {
    }

    /**
     * the action to be performed on component show
     *
     * @param e
     */
    @Override
    public void componentShown(ComponentEvent e) {
    }

    /**
     * the action to be performed on component hide
     *
     * @param e
     */
    @Override
    public void componentHidden(ComponentEvent e) {
    }

    /**
     * on mouse click event
     *
     * @param e
     */
    public void mouseClicked(MouseEvent e) {
        switch (e.getButton()) {
            case MouseEvent.BUTTON1:                                    // left click
                break;
            case MouseEvent.BUTTON3:                                    // right click = context menu
                // TODO: context menu: compute new spectrogram (with other settings)
                break;
        }
    }

    /**
     * on mouse press event
     *
     * @param e
     */
    public void mousePressed(MouseEvent e) {
    }

    /**
     * on mouse release event
     *
     * @param e
     */
    public void mouseReleased(MouseEvent e) {
    }

    /**
     * on mouse enter event
     *
     * @param e
     */
    public void mouseEntered(MouseEvent e) {
        this.mousePosition = e.getPoint();
        this.repaint();
    }

    /**
     * on mouse exit event
     *
     * @param e
     */
    public void mouseExited(MouseEvent e) {
        this.mousePosition = null;
        this.repaint();
    }

    /**
     * on mouse drag event
     *
     * @param e
     * @param displayMetrics
     */
    public void mouseDragged(MouseEvent e, WaveformPanel.DisplayMetrics displayMetrics) {
        if (displayMetrics == null)
            return;

        this.displayMetrics.relativeLeftmostPosition = displayMetrics.relativeLeftMostPosition();
        this.displayMetrics.relativeRightmostPosition = displayMetrics.relativeRightmostPosition();

        if (this.displayMetrics.spectrogramImage == null)
            return;

        this.repaint();
    }

    /**
     * on mouse move event
     *
     * @param e
     */
    public void mouseMoved(MouseEvent e) {
        this.mousePosition = e.getPoint();
        this.repaint();
    }

    /**
     * on mouse wheel event
     *
     * @param e
     * @param displayMetrics
     */
    public void mouseWheelMoved(MouseWheelEvent e, WaveformPanel.DisplayMetrics displayMetrics) {
        if (displayMetrics == null)
            return;

        this.displayMetrics.relativeLeftmostPosition = displayMetrics.relativeLeftMostPosition();
        this.displayMetrics.relativeRightmostPosition = displayMetrics.relativeRightmostPosition();

        if (this.displayMetrics.spectrogramImage == null)
            return;

        this.repaint();
    }

    /**
     * some metrics used to display/scale the spectrogram image
     *
     * @author Axel Berndt
     */
    private static class DisplayMetrics {
        private BufferedImage spectrogramImage = null;      // the spectrogram image to be displayed
        protected double relativeLeftmostPosition = 0.0;
        protected double relativeRightmostPosition = 1.0;
    }
}
