package mpmToolbox.gui.audio;

import com.alee.api.annotations.NotNull;
import com.alee.api.data.Orientation;
import com.alee.extended.split.WebMultiSplitPane;
import com.alee.extended.tab.DocumentData;
import com.alee.laf.label.WebLabel;
import com.alee.laf.panel.WebPanel;
import meico.audio.Audio;
import mpmToolbox.gui.ProjectPane;
import mpmToolbox.supplementary.Tools;

import java.awt.*;
import java.awt.event.*;

/**
 * A custom DocumentData object for the audio analysis component.
 * @author Axel Berndt
 */
public class AudioDocumentData extends DocumentData<WebPanel> implements MouseListener, MouseMotionListener, MouseWheelListener {
    protected final ProjectPane parent;
    private final WebPanel audioPanel = new WebPanel(new GridBagLayout());      // the panel that contains everything in this tab

    private Audio audio;

    private final WaveformPanel waveform = new WaveformPanel();
    private final SpectrogramPanel spectrogram = new SpectrogramPanel();
// TODO   private final TimingCurvePanel timingCurve = new TimingCurvePanel();
// TODO   private final SymbolicMusicPanel symbolicMusic = new SymbolicMusicPanel();

    /**
     * constructor
     * @param parent
     */
    public AudioDocumentData(@NotNull ProjectPane parent) {
        super("Audio", "Audio", null);

        this.setComponent(this.audioPanel);
        this.setClosable(false);
        this.parent = parent;
        this.audio = this.parent.getSyncPlayer().getSelectedAudio();
        this.setAudio(this.audio);
        // TODO: ...

        this.audioPanel.addMouseListener(this);
        this.audioPanel.addMouseMotionListener(this);
        this.audioPanel.addMouseWheelListener(this);


        this.draw();
    }

    /**
     * this draws the content of the audio analysis frame
     */
    private void draw() {
        // TODO ...

        WebMultiSplitPane splitPane = new WebMultiSplitPane(Orientation.vertical);  // the vertical split pane contains the different visualizations that are going to be aligned (waveform, spectrogram etc.)
        splitPane.setOneTouchExpandable(true);                                      // dividers have buttons for maximizing a component
        splitPane.setContinuousLayout(true);                                        // when the divider is moved the content is continuously redrawn
        splitPane.add(this.waveform);
        splitPane.add(this.spectrogram);
        splitPane.add(new WebLabel("Symbolic Music", WebLabel.CENTER));

        GridBagLayout gridBagLayout = (GridBagLayout) this.audioPanel.getLayout();
        Tools.addComponentToGridBagLayout(this.audioPanel, gridBagLayout, new WebLabel("Buttons go here"), 0, 1, 1, 1, 1.0, 0.0, 0, 0, GridBagConstraints.NONE, GridBagConstraints.SOUTH);
        Tools.addComponentToGridBagLayout(this.audioPanel, gridBagLayout, splitPane, 0, 0, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH, GridBagConstraints.CENTER);
    }

    /**
     * Get the ProjectPane object that this belongs to.
     * @return
     */
    public ProjectPane getParent() {
        return this.parent;
    }

    /**
     * set the data that is visualized here
     * @param audio
     */
    public void setAudio(Audio audio) {
        if (this.audio == audio)
            return;

        this.audio = audio;

        this.getParent().getRootPane().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        this.waveform.setAudio(this.audio);
        this.spectrogram.setAudio(this.audio);
        this.getParent().getRootPane().setCursor(Cursor.getDefaultCursor());
    }

    /**
     * on mouse click event
     * @param e
     */
    @Override
    public void mouseClicked(MouseEvent e) {
        if (this.waveform.contains(e.getPoint()))
            this.waveform.mouseClicked(e);
        else if (this.spectrogram.contains(e.getPoint()))
            this.spectrogram.mouseClicked(e);
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
        this.waveform.mouseEntered(e);
        this.spectrogram.mouseEntered(e);
    }

    /**
     * on mouse exit event
     * @param e
     */
    @Override
    public void mouseExited(MouseEvent e) {
        this.waveform.mouseExited(e);
        this.spectrogram.mouseExited(e);
    }

    /**
     * on mouse drag event
     * @param e
     */
    @Override
    public void mouseDragged(MouseEvent e) {
        this.spectrogram.mouseDragged(e, this.waveform.mouseDragged(e));
    }

    /**
     * on mouse move event
     * @param e
     */
    @Override
    public void mouseMoved(MouseEvent e) {
        this.waveform.mouseMoved(e);
        this.spectrogram.mouseMoved(e);
    }

    /**
     * on mouse wheel event
     * @param e
     */
    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        this.spectrogram.mouseWheelMoved(e, this.waveform.mouseWheelMoved(e));
    }
}
