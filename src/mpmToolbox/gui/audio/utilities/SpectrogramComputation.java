package mpmToolbox.gui.audio.utilities;

import com.alee.extended.window.WebProgressDialog;
import com.alee.laf.button.WebButton;
import com.alee.laf.panel.WebPanel;
import com.tagtraum.jipes.math.WindowFunction;
import mpmToolbox.gui.Settings;
import mpmToolbox.supplementary.Tools;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

/**
 * @author Axel Berndt
 */
public class SpectrogramComputation extends WebProgressDialog {
    private final SpectrogramComputationWorker worker;
    protected final SpectrogramSpecs parent;

    /**
     * constructor
     */
    public SpectrogramComputation(WindowFunction windowFunction, int hopSize, float minFreq, float maxFreq, int bins, boolean normalize, SpectrogramSpecs parent) {
        super("Computing Spectrogram");

        this.parent = parent;
        this.worker = new SpectrogramComputationWorker(windowFunction, hopSize, minFreq, maxFreq, bins, normalize, this);

        this.setText("samples left");
        this.setShowProgressText(true);     // true by default, this is just to be sure
        this.setModal(true);                // block the rest of the program behind this dialog
        this.setResizable(true);
        this.setPreferredProgressWidth(getFontMetrics(this.getFont()).stringWidth("Computing Spectrogram") * 2);    // this is to ensure that the title text is fully visible
        this.cancelOnEsc();
        this.onClose(runnable -> this.cancel());

        this.setMinimum(0);
        this.setPadding(Settings.paddingInDialogs);
        this.getProgressBar().setPadding(Settings.paddingInDialogs);

        // add a cancel button to the dialog
        WebButton cancelButton = new WebButton("Cancel");
        cancelButton.setPadding(Settings.paddingInDialogs, Settings.paddingInDialogs*2, Settings.paddingInDialogs, Settings.paddingInDialogs*2);
        cancelButton.addActionListener(actionEvent -> this.cancel());
        WebPanel buttonPanel = new WebPanel(new GridBagLayout());
        Tools.addComponentToGridBagLayout(buttonPanel, (GridBagLayout) buttonPanel.getLayout(), cancelButton, 0, 0, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.NONE, GridBagConstraints.CENTER);
        this.getContentPane().add(buttonPanel, BorderLayout.SOUTH);

        this.pack();
//        this.setLocationRelativeTo(null);
    }

    /**
     * pressing ESC key cancels the computation
     */
    private void cancelOnEsc() {
        InputMap inputMap = this.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "Cancel");
        this.getRootPane().getActionMap().put("Cancel", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                cancel();
            }
        });
    }

    /**
     * the cancel procedure
     */
    private void cancel() {
        this.worker.cancel();
    }

    /**
     * triggers the computation and this progress dialog to show up
     */
    protected void execute() {
        assert this.worker != null;
        this.worker.execute();
        this.setVisible(true);  // will be closed/disposed by the worker
    }
}
