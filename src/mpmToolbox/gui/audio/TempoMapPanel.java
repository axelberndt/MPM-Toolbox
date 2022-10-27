package mpmToolbox.gui.audio;

import com.alee.laf.menu.WebPopupMenu;
import meico.mpm.Mpm;
import meico.mpm.elements.Part;
import meico.mpm.elements.Performance;
import meico.mpm.elements.maps.GenericMap;
import meico.mpm.elements.maps.TempoMap;
import meico.mpm.elements.maps.data.TempoData;
import meico.supplementary.KeyValue;
import mpmToolbox.gui.Settings;
import mpmToolbox.projectData.alignment.Alignment;
import mpmToolbox.projectData.alignment.Note;
import mpmToolbox.projectData.alignment.PianoRoll;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Point2D;
import java.util.ArrayList;

/**
 * This panel is an interactive tempoMap visualization.
 * @author Axel Berndt
 */
public class TempoMapPanel extends PianoRollPanel implements ComponentListener, MouseListener, MouseMotionListener, MouseWheelListener {
    private final Alignment alignment;
    private TempoMap tempoMap = null;
    private final ArrayList<KeyValue<TempoData, Point2D.Double>> tempoData = new ArrayList<>();
    private double minTempo = Double.MAX_VALUE; // used to properly scale the visualization; this gets a meaningful value when a tempomap is read
    private double maxTempo = 0.0;              // used to properly scale the visualization; this gets a meaningful value when a tempomap is read
    private long leftmostSample;                // just a copy of the eponymous value in the parent to keep track of whether it changed and the tick values have to be computed anew
    private long rightmostSample;               // just a copy of the eponymous value in the parent to keep track of whether it changed and the tick values have to be computed anew

    /**
     * constructor
     * @param parent
     */
    protected TempoMapPanel(AudioDocumentData parent) {
        this(parent, "<html><center>Select a performance to visualize its tempomap here.<p style='margin-top:7'>In contrast to the audio-related panels, the information here is displayed along the musical, i.e. symbolic, time domain, not milliseconds.</p></center></html>");
    }

    /**
     * constructor
     * @param parent
     * @param noDataText
     */
    protected TempoMapPanel(AudioDocumentData parent, String noDataText) {
        super(parent, noDataText);

        this.alignment = new Alignment(parent.getParent().getMsm(), null);
        this.leftmostSample = parent.getLeftmostSample();
        this.rightmostSample = parent.getRightmostSample();
        this.retrieveTempoMap();

        this.setOpaque(false);
        this.setBackground(Color.BLACK);
    }

    /**
     * draw the component
     * @param g
     */
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);                // this ensures that the background is filled with the standard background color

        if (this.parent.getParent().getSyncPlayer().getSelectedPerformance() == null)
            return;

        Graphics2D g2d = (Graphics2D) g;        // make g a Graphics2D object, so we can use its extended drawing features
        if (this.drawPianoRoll(g2d)) {          // if we successfully draw the piano roll, we can also draw the other information
            this.drawTempoMap(g2d);
            this.drawPlaybackCursor(g2d);

            if (this.drawMouseCursor(g2d)) {
                // TODO: print info text
            }
        }
    }

    /**
     * invoke this method if the data to be displayed here has changed
     */
    protected void update() {
        Performance performance = this.parent.getParent().getSyncPlayer().getSelectedPerformance(); // retrieve the currently selected performance

        if (performance == null) {
            this.setOpaque(false);
            this.add(this.noData);
            return;
        }

        this.setOpaque(true);
        this.remove(this.noData);
        this.retrieveTempoMap();
        this.repaint();
    }

    /**
     * helper method to draw the piano roll
     * @param g2d
     * @return success
     */
    @Override
    protected boolean drawPianoRoll(Graphics2D g2d) {
        if (this.getWidth() <= 0)
            return false;

        // compute the corresponding tick positions to the milliseconds positions of the (performed) alignment in the other panels, so we know the positions to scale this non-performed alignment
        if ((this.leftmostSample != this.parent.getLeftmostSample()) || (this.rightmostSample != this.parent.getRightmostSample())) {   // only necessary if the borders have changed
            this.leftmostSample = this.parent.getLeftmostSample();
            this.rightmostSample = this.parent.getRightmostSample();

            if (this.parent.getAudio() == null) {
                this.parent.setLeftmostTick(0.0);
                this.parent.setRightmostTick(this.parent.getParent().getMsm().getEndDate());
            } else {
                double sampleToMilliConst = 1000.0 / this.parent.getAudio().getFrameRate();
                double fromMilliseconds = ((double) this.leftmostSample * sampleToMilliConst);
                double toMilliseconds = ((double) this.rightmostSample * sampleToMilliConst);
                this.parent.setLeftmostTick(this.parent.getAlignment().getCorrespondingTickDate(fromMilliseconds)[0]);
                this.parent.setRightmostTick(this.parent.getAlignment().getCorrespondingTickDate(toMilliseconds)[1]);
            }
        }

        // scale the piano roll according to the difference between the PPQ value of the MSM and the performance
        double leftMostTick, rightMostTick;
        int ppqMsm = this.parent.getParent().getMsm().getPPQ();
        int ppqPerf = this.parent.getParent().getSyncPlayer().getSelectedPerformance().getPPQ();
        if (ppqMsm != ppqPerf) {
            leftMostTick = (this.parent.getLeftmostTick() * ppqMsm) / ppqPerf;
            rightMostTick = (this.parent.getRightmostTick() * ppqMsm) / ppqPerf;
        } else {
            leftMostTick = this.parent.getLeftmostTick();
            rightMostTick = this.parent.getRightmostTick();
        }

        // generate and draw image
        PianoRoll pianoRoll = this.retrievePianoRoll(leftMostTick, rightMostTick, this.getWidth(), 128);
        g2d.drawImage(pianoRoll, 0, this.getHeight(), this.getWidth(), -this.getHeight(), this);
        return true;
    }

    /**
     * retrieve the piano roll from the parent
     * @param fromTicks
     * @param toTicks
     * @param width
     * @param height
     * @return
     */
    @Override
    protected PianoRoll retrievePianoRoll(double fromTicks, double toTicks, int width, int height) {
        Integer partNumber = this.parent.getPianoRollPartNumber();
        if (partNumber != null)         // draw only the selected musical part
            return this.alignment.getPart(partNumber).getPianoRoll(fromTicks, toTicks, width, height);

        // draw all musical parts
        return this.alignment.getPianoRoll(fromTicks, toTicks, width, height);
    }

    /**
     * helper method to draw the tempoMap in this panel
     * @param g2d
     */
    private void drawTempoMap(Graphics2D g2d) {
        if (this.tempoData.isEmpty())
            return;

        double x1 = this.parent.getLeftmostTick() / this.alignment.getMillisecondsLength();
        double x2 = this.parent.getRightmostTick() / this.alignment.getMillisecondsLength();
        double c = ((double) this.getWidth()) / (x2 - x1);
        int halfSize = Math.max(2, Math.round((1.5f * this.getHeight()) / 128.0f));
        int size = halfSize + halfSize;

        Stroke defaultStroke = g2d.getStroke();                     // keep the previous stroke settings
        g2d.setStroke(new BasicStroke(halfSize * 0.25f));           // set the stroke
        g2d.setColor(Settings.scorePerformanceColor);               // use normal performance symbol color

        Point pendingTempoCurve = null;                             // this will hold the beginning of the previous tempo instruction's curve segment to be drawn when the next instruction is processed

        for (int i = 0; i < this.tempoData.size(); i++) {
            KeyValue<TempoData, Point2D.Double> tempoDatum = this.tempoData.get(i);
            TempoData data = tempoDatum.getKey();

            if (data.endDate < this.parent.getLeftmostTick())       // the tempo instruction ends before the currently visualized frame
                continue;

            // paint the square
            int x = (int) Math.round((tempoDatum.getValue().x - x1) * c);
            int y = (int) Math.round(tempoDatum.getValue().y * -this.getHeight()) + this.getHeight();

            // draw the curve segment of the previous tempo instruction
            if (pendingTempoCurve != null) {
                KeyValue<TempoData, Point2D.Double> previous = this.tempoData.get(i-1);
                if (previous.getKey().transitionTo == null) {       // constant tempo
//                    g2d.drawLine(pendingTempoCurve.x, pendingTempoCurve.y, x, pendingTempoCurve.y);
//                    g2d.drawLine(x, pendingTempoCurve.y, x, y);
                    g2d.drawPolyline(new int[]{pendingTempoCurve.x, x, x}, new int[]{pendingTempoCurve.y, pendingTempoCurve.y, y}, 3);
                } else {                                            // continuous tempo transition
                    int tesselation = 10;
                    int[] curveX = new int[tesselation];
                    int[] curveY = new int[tesselation];

                    curveX[0] = pendingTempoCurve.x;
                    curveY[0] = pendingTempoCurve.y;

                    int transY = (int) Math.round((previous.getKey().transitionTo / this.maxTempo) * -this.getHeight()) + this.getHeight();

                    for (int j = 1; j < curveX.length - 2; ++j) {
                        double xf = (double) j / curveX.length;
                        curveX[j] = (int) Math.round((x - pendingTempoCurve.x) * xf) + pendingTempoCurve.x;
                        curveY[j] = (int) Math.round(Math.pow(xf, previous.getKey().exponent) * (transY - pendingTempoCurve.y)) + pendingTempoCurve.y;
                    }

                    curveX[tesselation - 2] = x;
                    curveY[tesselation - 2] = transY;

                    curveX[tesselation - 1] = x;
                    curveY[tesselation - 1] = y;

                    g2d.drawPolyline(curveX, curveY, curveX.length);
//                    g2d.drawPolyline(new int[]{pendingTempoCurve.x, x, x}, new int[]{pendingTempoCurve.y, transY, y}, 3);
                }
            }

            if (data.startDate > this.parent.getRightmostTick()) {  // tempo instruction is after the currently visualized frame
                break;                                              // done
            }

            pendingTempoCurve = new Point(x, y);
            g2d.fillRect(x - halfSize, y - halfSize, size, size);

            // the last tempo instruction gets a constant curve segment to the frame end
            if ((i+1) == this.tempoData.size()) {
                g2d.drawLine(pendingTempoCurve.x, pendingTempoCurve.y, this.getWidth(), pendingTempoCurve.y);
            }
        }

        g2d.setStroke(defaultStroke);
    }

    /**
     * find the tempoMap to be visualized here in the currently selected performance and part/global environment
     */
    private void retrieveTempoMap() {
        Performance performance = this.parent.getParent().getSyncPlayer().getSelectedPerformance(); // retrieve the currently selected performance
        if (performance == null) {
            this.tempoMap = null;
            return;
        }

        GenericMap tmap;

        // if a specific musical part is selected, we should choose its local tempomap if it has one
        Integer partNumber = this.parent.getPianoRollPartNumber();
        if (partNumber == null) {                                               // if no specific part selected
            tmap = performance.getGlobal().getDated().getMap(Mpm.TEMPO_MAP);    // get global tempomap
        } else {
            Part part = performance.getPart(partNumber);
            if (part != null)                                                   // if the part exists in the performance
                tmap = part.getDated().getMap(Mpm.TEMPO_MAP);                   // try finding its local tempomap if it has one
            else                                                                // otherwise
                tmap = performance.getGlobal().getDated().getMap(Mpm.TEMPO_MAP);// get global tempomap
        }
        this.tempoMap = (tmap != null) ? (TempoMap) tmap : null;

        if (tempoMap == null)
            return;

        // collect tempo data for visualization and determine min and max tempo
        this.minTempo = Double.MAX_VALUE;
        this.maxTempo = 0.0;
        this.tempoData.clear();                                     // clear the list of tempo instruction data from the previous performance
        for (int i = 0; i < this.tempoMap.size(); ++i) {            // for each tempo instruction in the tempomap
            TempoData data = this.tempoMap.getTempoDataOf(i);       // get its data
            if (data == null)                                       // the tempomap entry could be a style switch or a malicious entry
                continue;                                           // go on with the next entry

            this.tempoData.add(new KeyValue<>(data, null));         // add it to the tempoData list

            // update minimum and maximum tempo values
            double tempo = data.bpm;
            if (tempo < this.minTempo)
                this.minTempo = tempo;
            if (tempo > this.maxTempo)
                this.maxTempo = tempo;

            if (data.transitionTo == null)
                continue;
            tempo = data.transitionTo;
            if (tempo < this.minTempo)
                this.minTempo = tempo;
            if (tempo > this.maxTempo)
                this.maxTempo = tempo;
        }

        // add and subtract some headroom from minTempo and maxTempo, so it does not go to the vertical extremes of the panel
        if (this.minTempo == this.maxTempo)
            this.minTempo = this.maxTempo / 2.0;
        double headroom = (this.maxTempo - this.minTempo) * 0.1;
        this.minTempo = Math.max(0.0, this.minTempo - headroom);
        this.maxTempo += headroom;

        // give the tempo instructions relative positions in a unity square (date, tempo)
        double lengthTicks = this.alignment.getMillisecondsLength();    // in this particular alignment ticks = milliseconds
        for (KeyValue<TempoData, Point2D.Double> tempoDatum : this.tempoData) {
            double relativeX = tempoDatum.getKey().startDate / lengthTicks;
            double relativeY = (tempoDatum.getKey().bpm - this.minTempo) / (this.maxTempo - this.minTempo);
            tempoDatum.setValue(new Point2D.Double(relativeX, relativeY));
        }
    }

    /**
     * draw the lines of the mouse cursor in the Graphics2D object
     * @param g2d
     * @return true if a valid mouse position was available and the drawing was successful
     */
    @Override
    protected boolean drawMouseCursor(Graphics2D g2d) {
        // the native domain of the super class PianoRollPanel is the audio domain; hence the mouse cursor position needs a different treatment here than in the super class
        if (this.mouseInThisPanel()) {                                // if the mouse is in this panel
            g2d.setColor(Settings.scoreNoteColorHighlighted);
            g2d.drawLine(this.parent.getMouseCursor().getTicksX(), 0, this.parent.getMouseCursor().getTicksX(), this.getHeight());
            g2d.drawLine(0, this.mousePositionY, this.getWidth(), this.mousePositionY);
            return true;
        }

        // the mouse cursor is in another panel, so we have to compute the corresponding tick position in this panel from the current milliseconds position of the cursor in another panel
        if ((this.parent.getAudio() == null) || (this.parent.getMouseCursor() == null))     // if no cursor data available
            return false;                                                                   // done

        g2d.setColor(Settings.scoreNoteColorHighlighted);           // stroke color
        Stroke defaultStroke = g2d.getStroke();                     // keep the previous stroke settings
        g2d.setStroke(new BasicStroke(this.parent.getMouseCursor().getTicksXSpread())); // set the stroke
        g2d.drawLine(this.parent.getMouseCursor().getTicksX(), 0, this.parent.getMouseCursor().getTicksX(), this.getHeight());    // draw the stroke
        g2d.setStroke(defaultStroke);                               // switch back to the previous stroke settings

        return true;
    }

    /**
     * draw the playback cursor line in the Graphics2D object
     * @param g2d
     */
    @Override
    protected void drawPlaybackCursor(Graphics2D g2d) {
        g2d.setColor(new Color(0.5f, 0.5f, 0.5f, 0.6f));            // stroke color
        Stroke defaultStroke = g2d.getStroke();                     // keep the previous stroke settings
        g2d.setStroke(new BasicStroke(this.parent.getPlaybackCursor().getTicksXSpread()));  // set the stroke
        g2d.drawLine(this.parent.getPlaybackCursor().getTicksX(), 0, this.parent.getPlaybackCursor().getTicksX(), this.getHeight());    // draw the stroke
        g2d.setStroke(defaultStroke);                               // switch back to the previous stroke settings
    }

    /**
     * retrieve the note reference behind a certain pixel position in the current piano roll image
     * @param x horizontal pixel position in the piano roll image
     * @param y vertical pixel position in the piano roll image
     * @return the Note object or null
     */
    @Override
    protected Note getNoteAt(int x, int y) {
        Integer partNumber = this.parent.getPianoRollPartNumber();
        PianoRoll pianoRoll = (partNumber != null) ? this.alignment.getPart(partNumber).getPianoRoll() : this.alignment.getPianoRoll();
        if (pianoRoll == null)
            return null;

        return pianoRoll.getNoteAt(x, y);
    }

    /**
     * retrieve the note reference behind a certain relative position in the current piano roll image
     * @param x relative horizontal position in the piano roll image (should be in [0, 1])
     * @param y relative vertical position in the piano roll image (should be in [0, 1])
     * @return the Note object or null
     */
    @Override
    protected Note getNoteAt(double x, double y) {
        Integer partNumber = this.parent.getPianoRollPartNumber();
        PianoRoll pianoRoll = (partNumber != null) ? this.alignment.getPart(partNumber).getPianoRoll() : this.alignment.getPianoRoll();
        if (pianoRoll == null)
            return null;

        return pianoRoll.getNoteAt((int) (pianoRoll.getWidth() * x), (int) (pianoRoll.getHeight() * (-y + 1.0)));
    }

    /**
     * set the audio data that this panel should visualize
     */
    @Override
    protected void setAudio() {
        throw new UnsupportedOperationException();
    }

    /**
     * process a mouse drag event; to be invoked when scrolling should be performed solely by this panel and not in sync with the other panels
     * @param e
     */
    private void scroll(MouseEvent e) {
        if (this.parent.getMouseCursor() == null)
            return;

        // do the scrolling only in this panel
        double leftmost = this.parent.getLeftmostTick();
        double rightmost = this.parent.getRightmostTick();
        double tickOffset = ((rightmost - leftmost) * (this.parent.getMouseCursor().getTicksX() - e.getPoint().x)) / this.getWidth();   // this computes how many horizontal pixels the mouse has moved, then scales it by the amount of ticks per horizontal pixel, so we know how many pixels we want to move the leftmost and rightmost tick index

        this.parent.communicateMousePositionToAllComponents(e);

        double numTicks = this.parent.getParent().getMsm().getEndDate();
        tickOffset = (tickOffset > 0) ? Math.min(numTicks - 1 - this.parent.getRightmostTick(), Math.round(tickOffset)) : Math.max(-this.parent.getLeftmostTick(), Math.round(tickOffset));  // we have to check that we don't go beyond the first and last tick; as we move those indices only in integer steps there is a certain numeric error causing the tick moving with a bit different speed than the mouse was moved, but it is not problematic

        if (tickOffset == 0.0)              // if no change
            return;                         // done

        // move the tick indices
        this.parent.setLeftmostTick((int) (this.parent.getLeftmostTick() + tickOffset));
        this.parent.setRightmostTick((int) (this.parent.getRightmostTick() + tickOffset));

        // update the cursor positions
//        this.parent.getMouseCursor().setSample(this.parent.getMouseCursor().getSample());
        this.parent.getPlaybackCursor().setSample(this.parent.getPlaybackCursor().getSample());

        this.repaint();
    }

    /**
     * this is used when solely this panel needs to perform a zoom operation
     * @param pivotTick
     * @param zoomFactor
     */
    private void zoom(int pivotTick, double zoomFactor) {
        if (zoomFactor == 0.0)
            return;

        if (zoomFactor < 0.0) {             // zoom in
            int leftmostTick = pivotTick - (int) ((pivotTick - this.parent.getLeftmostTick()) * zoomFactor);
            int rightmostTick = (int) ((this.parent.getRightmostTick() - pivotTick) * zoomFactor) + pivotTick;
            if ((rightmostTick - leftmostTick) > 1) {   // make sure there are at least two ticks to be drawn, if we zoom too far in, left==right, we cannot zoom out again
                this.parent.setLeftmostTick(leftmostTick);
                this.parent.setRightmostTick(rightmostTick);
            }
        }
        else if (zoomFactor > 0.0) {        // zoom out
            this.parent.setLeftmostTick(pivotTick - (int) Math.ceil((pivotTick - this.parent.getLeftmostTick()) * zoomFactor));
            if (this.parent.getLeftmostTick() < 0)
                this.parent.setLeftmostTick(0);
            this.parent.setRightmostTick((int) Math.ceil((this.parent.getRightmostTick() - pivotTick) * zoomFactor) + pivotTick);
            double numTicks = this.parent.getParent().getMsm().getEndDate();
            if (this.parent.getRightmostTick() >= numTicks)
                this.parent.setRightmostTick(numTicks - 1);
        }

        // update the cursor positions
//        this.parent.getMouseCursor().setSample(this.parent.getMouseCursor().getSample());
        this.parent.getPlaybackCursor().setSample(this.parent.getPlaybackCursor().getSample());

        this.repaint();
    }

    /**
     * create a context menu for the position of the mouse click;
     * further entries to the menu may be added by inheritances
     * @param e
     * @return
     */
    @Override
    protected WebPopupMenu getContextMenu(MouseEvent e) {
        // TODO ...
        return null;
    }

    /**
     * on mouse click event
     * @param e
     */
    @Override
    public void mouseClicked(MouseEvent e) {
        // TODO: interaction with the tempo curve

        super.mouseClicked(e);  // select the note that has been clicked, if any
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
        if (this.noData.isShowing())
            return;

        super.mouseEntered(e);
    }

    /**
     * on mouse exit event
     * @param e
     */
    @Override
    public void mouseExited(MouseEvent e) {
        if (this.noData.isShowing())
            return;

        super.mouseExited(e);
    }

    /**
     * on mouse move event
     * @param e
     */
    @Override
    public void mouseMoved(MouseEvent e) {
        if (this.noData.isShowing())
            return;

        super.mouseMoved(e);
    }

    /**
     * on mouse drag event
     * @param e
     */
    @Override
    public void mouseDragged(MouseEvent e) {
        if ((this.parent.getMouseCursor() == null)
                || (this.parent.getParent().getSyncPlayer().getSelectedPerformance() == null))
            return;

        // if audio data is available we do the drag/scrolling via the audio domain; in fact we do so as if the drag happens in the WaveformPanel
        if (this.parent.getAudio() != null) {
            long leftmost = this.parent.getLeftmostSample();
            long rightmost = this.parent.getRightmostSample();
            double sampleOffset = (double)((rightmost - leftmost) * (this.parent.getMouseCursor().getTicksX() - e.getPoint().x)) / this.parent.getWaveformPanel().getWidth();   // this computes how many horizontal pixels the mouse has moved, than scales it by the amount of samples per horizontal pixel so we know how many pixels we want to move the leftmost and rightmost sample index
            this.parent.communicateMousePositionToAllComponents(e);
            this.parent.scroll(sampleOffset);
            return;
        }

        this.scroll(e); // no audio data available, so we have to do the drag/scrolling exclusively in here
    }

    /**
     * on mouse wheel event
     * @param e
     */
    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        if (this.parent.getParent().getSyncPlayer().getSelectedPerformance() == null)
            return;

        // if audio data is available we do the zoom via the audio domain
        if (this.parent.getAudio() != null) {
            this.parent.mouseWheelMoved(e);
            return;
        }

        // no audio data available, so we have to do the zoom exclusively in here
        if (e.getWheelRotation() == 0)  // if there was no actual mouse wheel movement
            return;                     // done

        int pivotSample = (int) Math.round(this.parent.getTickIndex(e.getPoint().getX()));
        double zoomFactor = (e.getWheelRotation() < 0) ? 0.9 : 1.1;

        this.parent.communicateMousePositionToAllComponents(e);
        this.zoom(pivotSample, zoomFactor);
    }

}
