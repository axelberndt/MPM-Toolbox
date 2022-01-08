package mpmToolbox.gui.audio;

import com.alee.laf.button.WebButton;
import com.alee.laf.combobox.WebComboBox;
import com.alee.laf.label.WebLabel;
import com.alee.laf.menu.WebCheckBoxMenuItem;
import com.alee.laf.menu.WebMenuItem;
import com.alee.laf.menu.WebPopupMenu;
import com.alee.laf.panel.WebPanel;
import com.alee.laf.spinner.WebSpinner;
import com.tagtraum.jipes.math.WindowFunction;
import mpmToolbox.gui.Settings;
import mpmToolbox.projectData.Audio;
import mpmToolbox.projectData.alignment.Note;
import mpmToolbox.supplementary.Tools;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Objects;

/**
 * This class represents the spectrogram display in the audio tab.
 * @author Axel Berndt
 */
public class SpectrogramPanel extends PianoRollPanel {
    private final SpectrogramSpecs spectrogramSpecs;
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

        Audio.SpectrogramImage spectrogramImage = this.parent.getSpectrogramImage();
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
    protected void updateZoom() {
        this.updateZoom = true;
        this.updateScroll();
    }

    /**
     * recalculate the horizontal offset of the spectrogram image
     */
    protected void updateScroll() {
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
                    this.spectrogramSpecs.updateSpectrogramImage(this);
                });
                menu.add(normalize);

                menu.show(this, e.getX() - 25, e.getY());
                break;
        }
    }

    /**
     * This is the interface to specify the spectrogram attributes.
     * @author Axel Berndt
     */
    private static class SpectrogramSpecs extends WebPanel {
        private final WebButton computeButton = new WebButton("Compute CQT Spectrogram (takes some time!)");
        private WebComboBox windowFunctionChooser = new WebComboBox();
        private WebSpinner windowLength = new WebSpinner(new SpinnerNumberModel(2048, 1, Integer.MAX_VALUE, 1));
        private WebSpinner hopSize = new WebSpinner(new SpinnerNumberModel(1024, 1, Integer.MAX_VALUE, 1));
        private WebSpinner minFreq = new WebSpinner(new SpinnerNumberModel(8.0, 5.0, 100000.0, 1.0));
        private WebSpinner maxFreq = new WebSpinner(new SpinnerNumberModel(12544.0, 5.0, 100000.0, 1.0));
        private WebSpinner binsPerSemitone = new WebSpinner(new SpinnerNumberModel(1, 1, Integer.MAX_VALUE, 1));
        private boolean normalize = true;

        public SpectrogramSpecs(SpectrogramPanel parent) {
            super(new GridBagLayout());

            this.setPadding(Settings.paddingInDialogs);
//            int width = getFontMetrics(this.windowLength.getFont()).stringWidth("999.999"); // the width of number spinners

            // some space around the GUI
            Tools.addComponentToGridBagLayout(this, (GridBagLayout) this.getLayout(), new WebPanel(), 0, 0, 1, 1, 7.0, 6.0, 0, 0, GridBagConstraints.BOTH, GridBagConstraints.LINE_START);  // above and left
            Tools.addComponentToGridBagLayout(this, (GridBagLayout) this.getLayout(), new WebPanel(), 5, 8, 1, 1, 7.0, 6.0, 0, 0, GridBagConstraints.BOTH, GridBagConstraints.LINE_START);  // below and right

            // window function
            WebLabel windowFunctionLabel = new WebLabel("Window Function:", WebLabel.RIGHT);
            windowFunctionLabel.setPadding(Settings.paddingInDialogs);
            Tools.addComponentToGridBagLayout(this, (GridBagLayout) this.getLayout(), windowFunctionLabel, 1, 1, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH, GridBagConstraints.LINE_START);

            this.windowFunctionChooser.addItem("Hamming");
            this.windowFunctionChooser.addItem("Hann");
            this.windowFunctionChooser.addItem("Triangle");
            this.windowFunctionChooser.addItem("Welch");
            this.windowFunctionChooser.addItem("Inverse Hamming");
            this.windowFunctionChooser.addItem("Inverse Hann");
            this.windowFunctionChooser.addItem("Inverse Triangle");
            this.windowFunctionChooser.addItem("Inverse Welch");
            this.windowFunctionChooser.setSelectedItem("Hamming");
//            this.windowFunctionChooser.setPadding(Settings.paddingInDialogs);
            Tools.addComponentToGridBagLayout(this, (GridBagLayout) this.getLayout(), this.windowFunctionChooser, 2, 1, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH, GridBagConstraints.LINE_START);

            // window length
            WebLabel windowLengthLabel = new WebLabel("Window Length:", WebLabel.RIGHT);
            windowLengthLabel.setPadding(Settings.paddingInDialogs);
            Tools.addComponentToGridBagLayout(this, (GridBagLayout) this.getLayout(), windowLengthLabel, 1, 2, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH, GridBagConstraints.LINE_START);

//            this.windowLength.setMinimumWidth(width);
//            this.windowLength.setMaximumWidth(width);
            Tools.addComponentToGridBagLayout(this, (GridBagLayout) this.getLayout(), this.windowLength, 2, 2, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH, GridBagConstraints.LINE_START);

            WebLabel windowLengthUnit = new WebLabel("samples", WebLabel.LEFT);
            windowLengthUnit.setPadding(Settings.paddingInDialogs);
            Tools.addComponentToGridBagLayout(this, (GridBagLayout) this.getLayout(), windowLengthUnit, 3, 2, 1, 1, 0.1, 1.0, 0, 0, GridBagConstraints.BOTH, GridBagConstraints.LINE_START);

            // hop size
            WebLabel hopSizeLabel = new WebLabel("Hop Size:", WebLabel.RIGHT);
            hopSizeLabel.setPadding(Settings.paddingInDialogs);
            Tools.addComponentToGridBagLayout(this, (GridBagLayout) this.getLayout(), hopSizeLabel, 1, 3, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH, GridBagConstraints.LINE_START);

//            this.hopSize.setMinimumWidth(width);
//            this.hopSize.setMaximumWidth(width);
            Tools.addComponentToGridBagLayout(this, (GridBagLayout) this.getLayout(), this.hopSize, 2, 3, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH, GridBagConstraints.LINE_START);

            WebLabel hopSizeUnit = new WebLabel("samples", WebLabel.LEFT);
            hopSizeUnit.setPadding(Settings.paddingInDialogs);
            Tools.addComponentToGridBagLayout(this, (GridBagLayout) this.getLayout(), hopSizeUnit, 3, 3, 1, 1, 0.1, 1.0, 0, 0, GridBagConstraints.BOTH, GridBagConstraints.LINE_START);

            // max frequency
            WebLabel maxFreqLabel = new WebLabel("Max. Frequency:", WebLabel.RIGHT);
            maxFreqLabel.setPadding(Settings.paddingInDialogs);
            Tools.addComponentToGridBagLayout(this, (GridBagLayout) this.getLayout(), maxFreqLabel, 1, 4, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH, GridBagConstraints.LINE_START);

//            this.maxFreq.setMinimumWidth(width);
//            this.maxFreq.setMaximumWidth(width);
            Tools.addComponentToGridBagLayout(this, (GridBagLayout) this.getLayout(), this.maxFreq, 2, 4, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH, GridBagConstraints.LINE_START);

            WebLabel maxFreqUnit = new WebLabel("Hz", WebLabel.LEFT);
            maxFreqUnit.setPadding(Settings.paddingInDialogs);
            Tools.addComponentToGridBagLayout(this, (GridBagLayout) this.getLayout(), maxFreqUnit, 3, 4, 1, 1, 0.1, 1.0, 0, 0, GridBagConstraints.BOTH, GridBagConstraints.LINE_START);

            // min frequency
            WebLabel minFreqLabel = new WebLabel("Min. Frequency:", WebLabel.RIGHT);
            minFreqLabel.setPadding(Settings.paddingInDialogs);
            Tools.addComponentToGridBagLayout(this, (GridBagLayout) this.getLayout(), minFreqLabel, 1, 5, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH, GridBagConstraints.LINE_START);

//            this.minFreq.setMinimumWidth(width);
//            this.minFreq.setMaximumWidth(width);
            Tools.addComponentToGridBagLayout(this, (GridBagLayout) this.getLayout(), this.minFreq, 2, 5, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH, GridBagConstraints.LINE_START);

            WebLabel minFreqUnit = new WebLabel("Hz", WebLabel.LEFT);
            minFreqUnit.setPadding(Settings.paddingInDialogs);
            Tools.addComponentToGridBagLayout(this, (GridBagLayout) this.getLayout(), minFreqUnit, 3, 5, 1, 1, 0.1, 1.0, 0, 0, GridBagConstraints.BOTH, GridBagConstraints.LINE_START);

            WebButton alignFreq2Midi = new WebButton("<html><center><p>Align Frequencies</p><p style='margin-top:7'>with MIDI Pitches</p></center></html>");
            alignFreq2Midi.setPadding(Settings.paddingInDialogs);
            alignFreq2Midi.setToolTip("Sets min. and max. frequency of the CQT so that the piano roll pitches align with it.");
            alignFreq2Midi.addActionListener(actionEvent -> {
                maxFreq.setValue(12543.8539514160);
                minFreq.setValue(8.1757989156);
            });
            Tools.addComponentToGridBagLayout(this, (GridBagLayout) this.getLayout(), alignFreq2Midi, 4, 4, 1, 2, 0.1, 1.0, 0, 0, GridBagConstraints.BOTH, GridBagConstraints.LINE_START);

            // bins per semitone
            WebLabel binsLabel = new WebLabel("Bins per Semitone:", WebLabel.RIGHT);
            binsLabel.setPadding(Settings.paddingInDialogs);
            Tools.addComponentToGridBagLayout(this, (GridBagLayout) this.getLayout(), binsLabel, 1, 6, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH, GridBagConstraints.LINE_START);

//            this.binsPerSemitone.setMinimumWidth(width);
//            this.binsPerSemitone.setMaximumWidth(width);
            Tools.addComponentToGridBagLayout(this, (GridBagLayout) this.getLayout(), this.binsPerSemitone, 2, 6, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH, GridBagConstraints.LINE_START);

            // compute button
            this.computeButton.addActionListener(actionEvent -> {
                this.updateSpectrogramImage(parent);
            });
            this.computeButton.setPadding(Settings.paddingInDialogs);
            Tools.addComponentToGridBagLayout(this, (GridBagLayout) this.getLayout(), this.computeButton, 2, 7, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH, GridBagConstraints.LINE_START);
        }

        private void updateSpectrogramImage(SpectrogramPanel parent) {
            parent.getRootPane().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));   // change mouse cursor to busy
            // TODO: instead of the above, start a new thread where the computation is done, and change the compute button to CANCEL for as long as the computation takes

            WindowFunction windowFunction;
            int windowLength = (int) this.windowLength.getValue();
            switch ((String) Objects.requireNonNull(this.windowFunctionChooser.getSelectedItem())) {
                case "Hamming":
                    windowFunction = new WindowFunction.Hamming(windowLength);
                    break;
                case "Hann":
                    windowFunction = new WindowFunction.Hann(windowLength);
                    break;
                case "Triangle":
                    windowFunction = new WindowFunction.Triangle(windowLength);
                    break;
                case "Welch":
                    windowFunction = new WindowFunction.Welch(windowLength);
                    break;
                case "Inverse Hamming":
                    windowFunction = new WindowFunction.InverseWindowFunction(new WindowFunction.Hamming(windowLength));
                    break;
                case "Inverse Hann":
                    windowFunction = new WindowFunction.InverseWindowFunction(new WindowFunction.Hann(windowLength));
                    break;
                case "Inverse Triangle":
                    windowFunction = new WindowFunction.InverseWindowFunction(new WindowFunction.Triangle(windowLength));
                    break;
                case "Inverse Welch":
                    windowFunction = new WindowFunction.InverseWindowFunction(new WindowFunction.Welch(windowLength));
                    break;
                default:
                    return;
            }
            int hopSize = (int) this.hopSize.getValue();
            float minFreq = (float) ((double) this.minFreq.getValue());
            float maxFreq = (float) ((double) this.maxFreq.getValue());
            int bins = (int) this.binsPerSemitone.getValue();
            parent.parent.getAudio().computeSpectrogram(windowFunction, hopSize, minFreq, maxFreq, bins, this.normalize);
            if (parent.parent.getSpectrogramImage() != null) {
                parent.remove(this);
                parent.updateZoom();
                parent.updateScroll();
                parent.repaint();
            }
            parent.getRootPane().setCursor(Cursor.getDefaultCursor());  // change mouse cursor back to default
        }
    }
}
