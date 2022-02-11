package mpmToolbox.gui.audio;

import com.alee.laf.menu.WebMenu;
import com.alee.laf.menu.WebMenuItem;
import com.alee.laf.menu.WebPopupMenu;
import mpmToolbox.gui.Settings;
import mpmToolbox.projectData.audio.WaveformImage;
import mpmToolbox.supplementary.Tools;

import java.awt.*;
import java.awt.event.MouseEvent;

/**
 * This class represents the waveform display in the audio tab.
 * @author Axel Berndt
 */
public class WaveformPanel extends PianoRollPanel {
    /**
     * constructor
     */
    protected WaveformPanel(AudioDocumentData parent) {
        super(parent, "Select an audio recording and performance via the SyncPlayer.");
    }

    /**
     * draw the waveform
     * @param g
     */
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);                        // this ensures that the background is filled with the standard background color

        WaveformImage waveformImage = this.parent.getWaveformImage(this.getWidth(), this.getHeight());
        if (waveformImage == null)
            return;

        Graphics2D g2 = (Graphics2D)g;                  // make g a Graphics2D object, so we can use its extended drawing features

        g2.drawImage(waveformImage, 0, 0, this);        // draw the waveform
        this.drawPianoRoll(g2);
        this.drawPlaybackCursor(g2);

        // draw the mouse cursor
        if (this.mousePosition != null) {
            g2.setColor(Settings.scoreNoteColorHighlighted);
            g2.drawLine(this.mousePosition.x, 0, this.mousePosition.x, this.getHeight());
//            g2.drawLine(0, this.mousePosition.y, this.getWidth(), this.mousePosition.y);

            // print info text
            int sampleIndex = this.getSampleIndex(this.mousePosition.getX());
            double millisec = Tools.round(((double) sampleIndex / this.parent.getAudio().getFrameRate()) * 1000.0, 2);
            g2.setColor(Color.LIGHT_GRAY);
            g2.drawString("Sample No.: " + sampleIndex, 2, Settings.getDefaultFontSize());
            g2.drawString("Milliseconds: " + millisec, 2, Settings.getDefaultFontSize() * 2.25f);
        }
    }

    /**
     * on mouse enter event
     * @param e
     */
    @Override
    public void mouseEntered(MouseEvent e) {
        if (this.parent.getAudio() == null)
            return;

        super.mouseEntered(e);
    }

    /**
     * on mouse exit event
     * @param e
     */
    @Override
    public void mouseExited(MouseEvent e) {
        if (this.parent.getAudio() == null)
            return;

        super.mouseExited(e);
    }

    /**
     * on mouse exit event
     * @param e
     */
    @Override
    public void mouseMoved(MouseEvent e) {
        if (this.parent.getAudio() == null)
            return;

        super.mouseMoved(e);
    }

    /**
     * on mouse click event
     * @param e
     */
    @Override
    public void mouseClicked(MouseEvent e) {
        if (this.parent.getAudio() == null)             // if there is no audio data
            return;                                     // nothing to do here

        switch (e.getButton()) {
            case MouseEvent.BUTTON1:                    // left click
                super.mouseClicked(e);                  // select a note
                break;
            case MouseEvent.BUTTON3:                    // right click = context menu
                WebPopupMenu menu = this.getContextMenu(e);

                // choose the channel(s) to be displayed
                WebMenu chooseChannel = new WebMenu("Display Audio Channel");
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
}
