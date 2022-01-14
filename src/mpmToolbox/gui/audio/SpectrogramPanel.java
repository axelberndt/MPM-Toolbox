package mpmToolbox.gui.audio;

import com.alee.laf.menu.WebCheckBoxMenuItem;
import com.alee.laf.menu.WebMenuItem;
import com.alee.laf.menu.WebPopupMenu;
import mpmToolbox.gui.Settings;
import mpmToolbox.gui.audio.utilities.SpectrogramImage;
import mpmToolbox.gui.audio.utilities.SpectrogramSpecs;

import java.awt.*;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseEvent;

/**
 * This class represents the spectrogram display in the audio tab.
 * @author Axel Berndt
 */
public class SpectrogramPanel extends PianoRollPanel {
    private final SpectrogramSpecs spectrogramSpecs;// the panel to specify the spectrogram
    private double samplesPerPixel = 0.0;           // this value is part of the transformation process of the spectrogram image
    private int imageWidth = 1;                     // the width of the spectrogram image, part of the transformation process of the spectrogram image
    private int horizontalOffset = 0;               // the x-offset of the spectrogram image, part of the transformation process of the spectrogram image
    private boolean updateZoom = true;              // this is set true to trigger a recomputing of the above variables during repaint
    private boolean updateScroll = true;            // this is set true to trigger recomputing of horizontalOffset during repaint

    /**
     * constructor
     */
    protected SpectrogramPanel(AudioDocumentData parent) {
        super(parent, "Select an audio recording and performance via the SyncPlayer.");
        this.spectrogramSpecs = new SpectrogramSpecs(this);
    }

    /**
     * draw the waveform
     * @param g
     */
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);            // this ensures that the background is filled with the standard background color

        if (this.noData.isShowing() || this.spectrogramSpecs.isShowing())
            return;

        SpectrogramImage spectrogramImage = this.parent.getSpectrogramImage();
        if (spectrogramImage == null)
            return;

        Graphics2D g2 = (Graphics2D) g;     // make g a Graphics2D object so we can use its extended drawing features

        if (this.updateZoom) {
            this.samplesPerPixel = (double) (this.parent.getRightmostSample() - this.parent.getLeftmostSample() + 1) / this.getWidth();
            this.imageWidth = (int) Math.round((double) this.parent.getAudio().getNumberOfSamples() / this.samplesPerPixel);
            this.updateZoom = false;
        }
        if (this.updateScroll) {
            this.horizontalOffset = (int) Math.round((double) -this.parent.getLeftmostSample() / this.samplesPerPixel);
            this.updateScroll = false;
        }

        g2.drawImage(spectrogramImage, this.horizontalOffset, 0, this.imageWidth, this.getHeight(), this);
        this.drawPianoRoll(g2);

        // draw the mouse cursor
        if (this.mousePosition != null) {
            g2.setColor(Settings.scoreNoteColorHighlighted);
            g2.drawLine(this.mousePosition.x, 0, this.mousePosition.x, this.getHeight());

            if (this.mouseInThisPanel) {
                g2.drawLine(0, this.mousePosition.y, this.getWidth(), this.mousePosition.y);

                // print info text
                // TODO compute and display frequency of mouse y position
//                g2.setColor(Color.LIGHT_GRAY);
//                double relativeYPos = (double)(this.getHeight() - this.mousePosition.y) / this.getHeight();
//                g2.drawString("Frequency: " + spectrogramImage.getFrequency(relativeYPos) + " Hz", 2, Settings.getDefaultFontSize());
            }
        }
    }

    /**
     * signal that the display metrics for the spectrogram image have to be re-computed
     */
    public void updateZoom() {
        this.updateZoom = true;
        this.updateScroll();
    }

    /**
     * recalculate the horizontal offset of the spectrogram image
     */
    public void updateScroll() {
        this.updateScroll = true;
    }

    /**
     * set the data that this panel should visualize
     */
    @Override
    protected void setAudio() {
        if (this.parent.getAudio() == null) {
            this.add(this.noData);
            return;
        }

        this.remove(this.noData);
        if (this.parent.getSpectrogramImage() == null)
            this.add(this.spectrogramSpecs);
        else
            this.remove(this.spectrogramSpecs);
    }

    /**
     * the action to be performed on component resize
     *
     * @param e
     */
    @Override
    public void componentResized(ComponentEvent e) {
        this.updateZoom();
        super.componentResized(e);
    }

    /**
     * on mouse enter event
     * @param e
     */
    @Override
    public void mouseEntered(MouseEvent e) {
        if (this.noData.isShowing() || this.spectrogramSpecs.isShowing())
            return;

        super.mouseEntered(e);
    }

    /**
     * on mouse exit event
     * @param e
     */
    @Override
    public void mouseExited(MouseEvent e) {
        if (this.noData.isShowing() || this.spectrogramSpecs.isShowing())
            return;

        super.mouseExited(e);
    }

    /**
     * on mouse exit event
     * @param e
     */
    @Override
    public void mouseMoved(MouseEvent e) {
        if (this.noData.isShowing() || this.spectrogramSpecs.isShowing())
            return;

        super.mouseMoved(e);
    }

    /**
     * on mouse click event
     *
     * @param e
     */
    @Override
    public void mouseClicked(MouseEvent e) {
        if (this.noData.isShowing() || this.spectrogramSpecs.isShowing())
//        if (this.parent.getSpectrogramImage() == null)  // if no spectrogram (or even no audio)
            return;                                     // nothing to click on

        switch (e.getButton()) {
            case MouseEvent.BUTTON1:                    // left click
                super.mouseClicked(e);                  // select a note
                break;
            case MouseEvent.BUTTON3:                    // right click = context menu
                WebPopupMenu menu = this.getContextMenu(e);

                // play from here
                WebMenuItem playFromHere = new WebMenuItem("Play from here");
                playFromHere.addActionListener(actionEvent -> {
                    this.parent.getParent().getSyncPlayer().triggerPlayback(this.parent.getWaveformPanel().getSampleIndex(e.getPoint().getX()));
                });
                menu.add(playFromHere);

                // specify new spectrogram
                WebMenuItem newSpectrogram = new WebMenuItem("New Spectrogram");
                newSpectrogram.addActionListener(actionEvent -> {
                    this.add(this.spectrogramSpecs);
                    this.repaint();
                });
                menu.add(newSpectrogram);

                // normalize or denormalize the spectrogram image
                WebCheckBoxMenuItem normalize = new WebCheckBoxMenuItem("Normalize", this.spectrogramSpecs.normalize);
                normalize.addChangeListener(changeEvent -> {
                    this.spectrogramSpecs.normalize = normalize.isSelected();
                    this.spectrogramSpecs.updateSpectrogramImage();
                });
                menu.add(normalize);

                menu.show(this, e.getX() - 25, e.getY());
                break;
        }
    }
}
