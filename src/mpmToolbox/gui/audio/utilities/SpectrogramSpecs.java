package mpmToolbox.gui.audio.utilities;

import com.alee.laf.button.WebButton;
import com.alee.laf.combobox.WebComboBox;
import com.alee.laf.label.WebLabel;
import com.alee.laf.panel.WebPanel;
import com.alee.laf.spinner.WebSpinner;
import com.tagtraum.jipes.math.WindowFunction;
import mpmToolbox.gui.Settings;
import mpmToolbox.gui.audio.SpectrogramPanel;
import mpmToolbox.supplementary.Tools;

import javax.swing.*;
import java.awt.*;
import java.util.Objects;

/**
 * This is the interface to specify the spectrogram attributes.
 * @author Axel Berndt
 */
public class SpectrogramSpecs extends WebPanel {
    protected final SpectrogramPanel parent;
    private final WebComboBox windowFunctionChooser = new WebComboBox();
    private final WebSpinner windowLength = new WebSpinner(new SpinnerNumberModel(2048, 1, Integer.MAX_VALUE, 1));
    private final WebSpinner hopSize = new WebSpinner(new SpinnerNumberModel(1024, 1, Integer.MAX_VALUE, 1));
    private final WebSpinner minFreq = new WebSpinner(new SpinnerNumberModel(8.0, 5.0, 100000.0, 1.0));
    private final WebSpinner maxFreq = new WebSpinner(new SpinnerNumberModel(12544.0, 5.0, 100000.0, 1.0));
    private final WebSpinner binsPerSemitone = new WebSpinner(new SpinnerNumberModel(1, 1, Integer.MAX_VALUE, 1));
    public boolean normalize = true;


    /**
     * constructor of the panel
     * @param parent
     */
    public SpectrogramSpecs(SpectrogramPanel parent) {
        super(new GridBagLayout());

        this.parent = parent;

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
        WebButton computeButton = new WebButton("Compute CQT Spectrogram (takes some time!)");
        computeButton.setPadding(Settings.paddingInDialogs);
        computeButton.addActionListener(actionEvent -> this.updateSpectrogramImage());
        Tools.addComponentToGridBagLayout(this, (GridBagLayout) this.getLayout(), computeButton, 2, 7, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH, GridBagConstraints.LINE_START);
    }

    /**
     * invoke this method to start the computation of a spectrogram image
     */
    public void updateSpectrogramImage() {
//            parent.getRootPane().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));   // change mouse cursor to busy

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

        SpectrogramComputation specCom = new SpectrogramComputation(windowFunction, hopSize, minFreq, maxFreq, bins, this.normalize, this);
        specCom.execute();

//            parent.getRootPane().setCursor(Cursor.getDefaultCursor());  // change mouse cursor back to default
    }
}
