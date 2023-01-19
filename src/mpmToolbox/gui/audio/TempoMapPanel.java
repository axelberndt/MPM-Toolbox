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
import mpmToolbox.gui.audio.utilities.TempoMapPanelElement;
import mpmToolbox.gui.mpmEditingTools.MpmEditingTools;
import mpmToolbox.gui.mpmTree.MpmTree;
import mpmToolbox.gui.mpmTree.MpmTreeNode;
import mpmToolbox.projectData.alignment.Alignment;
import mpmToolbox.projectData.alignment.Note;
import mpmToolbox.projectData.alignment.PianoRoll;
import mpmToolbox.supplementary.Tools;

import javax.swing.tree.TreePath;
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
    private final ArrayList<TempoMapPanelElement> tempoData = new ArrayList<>();   // a list of TempoDatas (the tempoMap entries)
    private double minTempo = Double.MAX_VALUE; // used to properly scale the visualization; this gets a meaningful value when a tempomap is read
    private double maxTempo = 0.0;              // used to properly scale the visualization; this gets a meaningful value when a tempomap is read
    private long leftmostSample;                // just a copy of the eponymous value in the parent to keep track of whether it changed and the tick values have to be computed anew
    private long rightmostSample;               // just a copy of the eponymous value in the parent to keep track of whether it changed and the tick values have to be computed anew
    private int halfSize = 1;

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

        this.parent.getParent().getMpmTree().addTreeSelectionListener(treeSelectionEvent -> {
            TreePath path = treeSelectionEvent.getNewLeadSelectionPath();
            TreePath oldPath = treeSelectionEvent.getOldLeadSelectionPath();
            if (path == oldPath)
                return;

            // if a tempo instruction is involved in a node selection in the MPM tree
            if (((path != null) && (this.parent.getParent().getMpmTree().getNodeForPath(path).getType() == MpmTreeNode.MpmNodeType.tempo))              // either as new selection
                    || ((oldPath != null) && (this.parent.getParent().getMpmTree().getNodeForPath(oldPath).getType() == MpmTreeNode.MpmNodeType.tempo))) {  // or as previous selection
                this.repaint();          // repaint the tempoMapPanel, so the highlighting gets updated
            }
        });
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

        this.halfSize = Math.max(2, Math.round((1.5f * this.getHeight()) / 128.0f));     // the size of a tempo instruction square should scale with the height of the panel

        Graphics2D g2d = (Graphics2D) g;        // make g a Graphics2D object, so we can use its extended drawing features
        if (this.drawPianoRoll(g2d)) {          // if we successfully draw the piano roll, we can also draw the other information
            this.drawTempoMap(g2d);
            this.drawPlaybackCursor(g2d);

            if (this.drawMouseCursor(g2d)) {
                // print info text
                g2d.setColor(Color.LIGHT_GRAY);
                g2d.drawString("Ticks: " + Tools.round(this.parent.getMouseCursor().getTicks(), 2), 2, Settings.getDefaultFontSize());
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

        // compute the size of the whole tempoMap
        double ticksPerPixel = (this.parent.getRightmostTick() - this.parent.getLeftmostTick()) / this.getWidth();

        int pixelWidth = (int) Math.round(this.alignment.getMillisecondsLength() / ticksPerPixel);  // in this particular alignment ticks = milliseconds
        int pixelHeight = (int) (this.getHeight() * 0.6);                               // scale down the height, so we have some room above and below

        // compute pixel offsets
        int xOffset = (-1 * (int) (this.parent.getLeftmostTick() / ticksPerPixel));
        int yOffset = (int) (this.getHeight() * 0.2);

        // draw tempoMap
        Stroke defaultStroke = g2d.getStroke();                                         // keep the previous stroke settings, so we can switch back to it afterwards
        g2d.setStroke(new BasicStroke(this.halfSize * 0.25f));                          // set new stroke

        Point prevConnection = null;
        for (TempoMapPanelElement tempoDatum : this.tempoData) {                        // for each tempo instruction
            if (tempoDatum.tempoData.endDate < this.parent.getLeftmostTick())           // the tempo instruction ends before the currently visualized frame
                continue;

            if (tempoDatum.tempoData.startDate > this.parent.getRightmostTick())        // tempo instruction is after the currently visualized frame
                break;                                                                  // done

            MpmTreeNode selectedMpmNode = this.parent.getParent().getMpmTree().getSelectedNode();
            Color color = ((selectedMpmNode == null) || (selectedMpmNode.getUserObject() != tempoDatum.tempoData.xml)) ? Settings.scorePerformanceColor : Settings.scorePerformanceColorHighlighted;
            tempoDatum.setScalesAndOffsets(pixelWidth, pixelHeight, xOffset, yOffset);  // adjust the scaling and offsets of the instruction's  points
            prevConnection = tempoDatum.draw(g2d, this.halfSize, prevConnection, color);            // draw the tempo instruction
        }

        g2d.setStroke(defaultStroke);
    }

    /**
     * find the tempoMap to be visualized here in the currently selected performance and part/global environment
     */
    private void retrieveTempoMap() {
        this.tempoData.clear();                                                 // clear the list of tempo instruction data from the previous performance

        Performance performance = this.parent.getParent().getSyncPlayer().getSelectedPerformance(); // retrieve the currently selected performance
        if (performance == null) {
            this.tempoMap = null;
            return;
        }

        GenericMap tmap = null;

        // if a specific musical part is selected, we should choose its local tempomap if it has one
        Integer partNumber = this.parent.getPianoRollPartNumber();
        if (partNumber != null) {                                               // if a specific part is selected
            Part part = performance.getPart(partNumber);                        // find the part
            if (part != null)                                                   // if the part exists in the performance
                tmap = part.getDated().getMap(Mpm.TEMPO_MAP);                   // try finding its local tempomap if it has one
        }
        if (tmap == null)                                                       // if no specific part is selected, or it had no local tempomap
            tmap = performance.getGlobal().getDated().getMap(Mpm.TEMPO_MAP);    // get global tempomap

        if (tmap == null) {                                                     // definitely no tempomap anywhere
            this.tempoMap = null;                                               // set it
            return;                                                             // done
        }

        this.tempoMap = (TempoMap) tmap;

        // collect tempo data for visualization and determine min and max tempo
        ArrayList<KeyValue<TempoData, Double[]>> tempTempoData = new ArrayList<>(); // a temporary list of tempo instructions with their normalised bpm and transitionTo value in the value array of the KeyValue pair
        this.minTempo = Double.MAX_VALUE;                           // in quarter note per minute
        this.maxTempo = 0.0;                                        // in quarter note per minute
        for (int i = 0; i < this.tempoMap.size(); ++i) {            // for each tempo instruction in the tempomap
            TempoData data = this.tempoMap.getTempoDataOf(i);       // get its data
            if (data == null)                                       // the tempomap entry could be a style switch or a malicious entry
                continue;                                           // go on with the next entry

            KeyValue<TempoData, Double[]> dataEntry = new KeyValue<>(data, new Double[]{data.bpm, data.transitionTo});

            // regularise tempo to the basis of quarter note per minute
            if (data.beatLength != 0.25) {
                if (data.bpm != null)
                    dataEntry.getValue()[0] *= data.beatLength * 4.0;
                if (data.transitionTo != null)
                    dataEntry.getValue()[1] *= data.beatLength * 4.0;
            }

            tempTempoData.add(dataEntry);                           // add it to the tempoData temporary list

            // update minimum and maximum tempo values
            if (dataEntry.getValue()[0] < this.minTempo)
                this.minTempo = dataEntry.getValue()[0];
            if (dataEntry.getValue()[0] > this.maxTempo)
                this.maxTempo = dataEntry.getValue()[0];

            if (data.transitionTo == null)
                continue;

            if (dataEntry.getValue()[1] < this.minTempo)
                this.minTempo = dataEntry.getValue()[1];
            if (dataEntry.getValue()[1] > this.maxTempo)
                this.maxTempo = dataEntry.getValue()[1];
        }

        // give the tempo instructions relative positions in a unity square (date, tempo)
        double lengthTicks = this.alignment.getMillisecondsLength();                        // in this particular alignment ticks = milliseconds
        for (KeyValue<TempoData, Double[]> tempoDatum : tempTempoData) {
            double relativeX = tempoDatum.getKey().startDate / lengthTicks;
            double relativeY = (tempoDatum.getValue()[0] - this.minTempo) / (this.maxTempo - this.minTempo);
            Point2D.Double startPoint = new Point2D.Double(relativeX, relativeY);

            if (!this.tempoData.isEmpty())                                                  // the end x-coordinate of the previous instruction is at the x-coordinate of this instruction
                this.tempoData.get(this.tempoData.size() - 1).setRelativeEndX(relativeX);

            double relativeEndY = (tempoDatum.getValue()[1] != null) ? (tempoDatum.getValue()[1] - this.minTempo) / (this.maxTempo - this.minTempo) : relativeY;
            Point2D.Double endPoint = new Point2D.Double(relativeX + 1.0, relativeEndY);   // relativeX + 1.0 must be replaced by the relativeX coordinate of the next instruction, as done above

            this.tempoData.add(new TempoMapPanelElement(tempoDatum.getKey(), startPoint, endPoint));
        }

        // the last instruction must be constant
        if (!this.tempoData.isEmpty()) {
            TempoMapPanelElement last = this.tempoData.get(this.tempoData.size() - 1);
            last.relativeCurve.clear();
            last.absoluteCurve.clear();
            last.relativeEnd.y = last.relativeCoordinates.y;
            last.absoluteEnd.y = last.absoluteCoordinates.y;
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
            g2d.drawLine(this.parent.getMouseCursor().getPixelsX(), 0, this.parent.getMouseCursor().getPixelsX(), this.getHeight());
            g2d.drawLine(0, this.mousePositionY, this.getWidth(), this.mousePositionY);
            return true;
        }

        // the mouse cursor is in another panel, so we have to compute the corresponding tick position in this panel from the current milliseconds position of the cursor in another panel
        if ((this.parent.getAudio() == null) || (this.parent.getMouseCursor() == null))     // if no cursor data available
            return false;                                                                   // done

        g2d.setColor(Settings.scoreNoteColorHighlighted);           // stroke color
        Stroke defaultStroke = g2d.getStroke();                     // keep the previous stroke settings
        g2d.setStroke(new BasicStroke(this.parent.getMouseCursor().getTicksXSpread())); // set the stroke
        g2d.drawLine(this.parent.getMouseCursor().getPixelsX(), 0, this.parent.getMouseCursor().getPixelsX(), this.getHeight());    // draw the stroke
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
        g2d.drawLine(this.parent.getPlaybackCursor().getPixelsX(), 0, this.parent.getPlaybackCursor().getPixelsX(), this.getHeight());    // draw the stroke
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
     * find the tempo instruction that was clicked at mouse position (x, y)
     * @param x
     * @param y
     * @return the tempo instruction or null
     */
    private TempoMapPanelElement getTempoInstructionAt(int x, int y) {
        if (this.tempoData.isEmpty())
            return null;

        TempoMapPanelElement before = null;
        TempoMapPanelElement after = null;
        for (TempoMapPanelElement tempoDatum : this.tempoData) {                        // for each tempo instruction
            if (tempoDatum.tempoData.endDate < this.parent.getLeftmostTick())           // the tempo instruction ends before the currently visualized frame
                continue;

            if (tempoDatum.tempoData.startDate > this.parent.getRightmostTick())        // tempo instruction is after the currently visualized frame
                break;                                                                  // done

            if (tempoDatum.getPixelPosition().x <= x)
                before = tempoDatum;
            else {
                after = tempoDatum;
                break;
            }
        }

        int beforeDist = (before != null) ? x - before.getPixelPosition().x : Integer.MAX_VALUE;
        int afterDist = (after != null) ? after.getPixelPosition().x - x : Integer.MAX_VALUE;

        TempoMapPanelElement candidate = (beforeDist <= afterDist)
                ? ((beforeDist > this.halfSize) ? null : before)
                : ((afterDist > this.halfSize) ? null : after);

        if ((candidate != null) && (Math.abs(y - candidate.getPixelPosition().y) > this.halfSize))
            candidate = null;

        return candidate;
    }

    /**
     * getter for the tempoMap that is currently displayed
     * @return
     */
    public TempoMap getTempoMap() {
        return this.tempoMap;
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
        double tickOffset = ((rightmost - leftmost) * (this.parent.getMouseCursor().getPixelsX() - e.getPoint().x)) / this.getWidth();   // this computes how many horizontal pixels the mouse has moved, then scales it by the amount of ticks per horizontal pixel, so we know how many pixels we want to move the leftmost and rightmost tick index

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
        TempoMapPanelElement tempo = this.getTempoInstructionAt(e.getX(), e.getY());
        if (tempo == null)
            return null;

        MpmTree mpmTree = this.parent.parent.getMpmTree();              // a handle to the mpm tree
        MpmTreeNode mpmTreeNode = mpmTree.findNode(tempo.tempoData.xml, true);    // get the mpm tree's node that corresponds with the selected node
        if (mpmTreeNode == null)                                        // if nothing has been selected
            return null;                                                // done
        mpmTree.setSelectedNode(mpmTreeNode);                           // select the node in the mpm tree
        mpmTree.scrollPathToVisible(mpmTreeNode.getTreePath());         // scroll the tree so the node is visible

        return MpmEditingTools.makeMpmTreeContextMenu(mpmTreeNode, mpmTree);
    }

    /**
     * on mouse click event
     * @param e
     */
    @Override
    public void mouseClicked(MouseEvent e) {
        // interaction with the tempo curve
        TempoMapPanelElement tempo = this.getTempoInstructionAt(e.getX(), e.getY());
        if (tempo != null) {
            MpmTree mpmTree = this.parent.parent.getMpmTree();              // a handle to the mpm tree
            MpmTreeNode mpmTreeNode = mpmTree.findNode(tempo.tempoData.xml, true);    // get the mpm tree's node that corresponds with the selected node
            if (mpmTreeNode == null)                                        // if nothing has been selected
                return;                                                     // done
            mpmTree.setSelectedNode(mpmTreeNode);                           // select the node in the mpm tree
            mpmTree.scrollPathToVisible(mpmTreeNode.getTreePath());         // scroll the tree so the node is visible

            switch (e.getButton()) {
                case MouseEvent.BUTTON1:                                    // left click
                    if (e.getClickCount() > 1)                              // if double (or more) click
                        MpmEditingTools.quickOpenEditor(mpmTreeNode, mpmTree);  // open editor dialog
                    break;
                case MouseEvent.BUTTON3:                                    // right click = context menu
                    WebPopupMenu menu = MpmEditingTools.makeMpmTreeContextMenu(mpmTreeNode, mpmTree);
                    menu.show(this, e.getX() - 25, e.getY());
                    break;
            }
        } else
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

        TempoMapPanelElement tempo = this.getTempoInstructionAt(e.getX(), e.getY());
        if (tempo != null) {                                        // if mouse is over a tempo instruction
            this.mousePositionY = e.getY();
            this.parent.communicateMousePositionToAllComponents(e);
            this.parent.repaintAllComponents();
            this.setCursor(new Cursor(Cursor.HAND_CURSOR));
        } else                                                      // else do the standard piano roll mouse-over work
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
            double sampleOffset = (double)((rightmost - leftmost) * (this.parent.getMouseCursor().getPixelsX() - e.getPoint().x)) / this.parent.getWaveformPanel().getWidth();   // this computes how many horizontal pixels the mouse has moved, than scales it by the amount of samples per horizontal pixel so we know how many pixels we want to move the leftmost and rightmost sample index
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
