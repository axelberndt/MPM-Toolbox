package mpmToolbox.gui.score;

import com.alee.extended.window.WebPopup;
import com.alee.laf.label.WebLabel;
import com.alee.laf.menu.WebPopupMenu;
import com.alee.laf.panel.WebPanel;
import meico.mpm.elements.Performance;
import meico.supplementary.KeyValue;
import mpmToolbox.gui.Settings;
import mpmToolbox.gui.mpmEditingTools.MpmEditingTools;
import mpmToolbox.gui.mpmEditingTools.PlaceAndCreateContextMenu;
import mpmToolbox.gui.mpmTree.MpmTree;
import mpmToolbox.gui.mpmTree.MpmTreeNode;
import mpmToolbox.gui.msmTree.MsmTree;
import mpmToolbox.gui.msmTree.MsmTreeNode;
import mpmToolbox.projectData.score.Score;
import mpmToolbox.projectData.score.ScoreNode;
import mpmToolbox.projectData.score.ScorePage;
import mpmToolbox.supplementary.orthantNeighborhoodGraph.ONGNode;
import nu.xom.Element;
import nu.xom.Node;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Map;

/**
 * This class displays the score pages and defines interaction with them.
 * @author Axel Berndt
 */
public class ScoreDisplayPanel extends WebPanel implements MouseWheelListener, MouseListener, MouseMotionListener, KeyListener {
    protected final ScoreDocumentData parent;                               // a link to the parent ScoreFrame
    protected ScorePage scorePage;                                          // the score page currently displayed
    private int pageIndex = 0;                                              // the page number (array index) currently displayed
    private final WebPopup noNoteSelected = new WebPopup<>(new WebLabel("You should select a note in the Musical Sequence Markup to be associated with a position here."));
    private final WebPopup noMpm = new WebPopup<>(new WebLabel("This project has no MPM. First create an MPM and a performance in it."));
    private final WebPopup noPerformance = new WebPopup<>(new WebLabel("This project's MPM has no Performance. First create a performance."));

    // variables for pan and zoom
    private final AffineTransform affineTransform = new AffineTransform();  // the pan and zoom transformation of the image is stored in here
    private AffineTransform inverseAffineTransform = new AffineTransform(); // this holds the inverse of the affine transform, it is computed together with the affine transform so it is quickly available for paint operations
    private Double zoomFactor = null;                                       // the zoom factor of the displayed score page image it is initialized the first time the panel is drawn, see paintComponent()
    private Point dragStartPoint = null;                                    // the start point of a drag gesture
    private final Point diff = new Point(0, 0);                             // this keeps track of drag gestures
    private final Point2D.Double offset = new Point2D.Double(0.0, 0.0);     // this stores the offset after all transforms so the image does not jump back to its initial position

    // variables for the overlay
    private Point mousePositionInImage = null;                              // this is used to keep track of the pixel position of the mouse cursor within the image (required to draw the "annotation preview overlay")
    private double overlayElementScaleFactor = 5.0;                         // this scales the note annotation size
    private int xOffset = (int)(this.overlayElementScaleFactor * 3.0);      // horizontal offset to center the overlay symbols around its position
    private int yOffset = (int)(this.overlayElementScaleFactor * 2.0);      // vertical offset to center the overlay symbols around its position
    private int xWidth = (int)(this.overlayElementScaleFactor * 6.0);       // the width of the overlay symbols
    private int yWidth = (int)(this.overlayElementScaleFactor * 4.0);       // the height of the overlay symbols

    protected ScoreNode anchorNode = null;                                  // in some modes (edit performance mode) we track the nearest neighboring overlay node to the mouse position and store it in this variable

    /**
     * constructor
     *
     * @param parent
     */
    public ScoreDisplayPanel(ScoreDocumentData parent) {
        super();

        this.parent = parent;                                                       // store the link to the parent project
        this.scorePage = this.parent.parent.getScore().getPage(this.pageIndex);     // load the score page to be displayed

        this.updateOverlayElementsScaleFactor();                                    // compute the initial size of overlay elements

        // prepare the popup message that shows up in mark notes mode when no note is selected in the MSM tree
        this.noNoteSelected.setPadding(3);
//        this.noNoteSelected.onMouseClick(e -> this.noNoteSelected.hidePopup());   // disappear when clicked
//        this.noNoteSelected.setResizable(false);
//        this.noNoteSelected.setDraggable(false);
        this.noMpm.setPadding(3);
        this.noPerformance.setPadding(3);

        // initialize the input listeners needed for interaction
        this.addMouseWheelListener(this);
        this.addMouseMotionListener(this);
        this.addMouseListener(this);
        this.addKeyListener(this);
//        this.addKeyboardInput();
    }

    /**
     * Get access to the ScoreDocumentData object that instantiated this.
     * @return
     */
    public ScoreDocumentData getParentScoreDocumentData() {
        return this.parent;
    }

    /**
     * Retrieve the current anchor node.
     * @return
     */
    public ScoreNode getAnchorNode() {
        return this.anchorNode;
    }

    /**
     * Draw the score page and apply all transformations. This method is invoked by the paint() method.
     * @param g
     */
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);                                    // this ensures that the background is filled with the standard background color

        if (this.zoomFactor == null)
            this.reset();

        Graphics2D g2 = (Graphics2D)g;                              // make g a Graphics2D object so we can use its extended drawing features
        g2.transform(this.affineTransform);                         // do the transform on the graphics

        if (!this.parent.hideScore)
            g2.drawImage(this.scorePage.getImage(), 0, 0, this);    // draw image

        // draw the overlay information
        if (this.parent.hideOverlay)
            return;

        // set the stroke style
        float strokeWidth = this.yWidth / 3.0f;
        BasicStroke stroke = new BasicStroke(strokeWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
        g2.setStroke(stroke);

        // find the currently selected node in the MSM and MPM tree so it can get a highlight color
        Object selectedMsmNode = this.parent.parent.getMsmTree().getSelectedNode();
        if (selectedMsmNode != null)
            selectedMsmNode = ((MsmTreeNode) selectedMsmNode).getUserObject();

        MpmTreeNode selectedMpmNode = (this.parent.parent.getMpmTree() == null) ? null : this.parent.parent.getMpmTree().getSelectedNode();

        // draw an "annotation preview" at the mouse position
        if (this.getMousePositionInImage() != null) {                    // this is only possible if we have a mouse position
            switch (this.parent.currentInteractionMode) {
                case panAndZoom:
                    break;

                case markNotes:
                    g2.setColor(Settings.scoreNoteColor);
                    g2.fillOval(this.getMousePositionInImage().x - this.xOffset, this.getMousePositionInImage().y - this.yOffset, this.xWidth, this.yWidth);
                    break;

                case editPerformance:
                    // draw the line between mouse pointer and anchor node
                    if (this.anchorNode != null) {
                        g2.setColor(Settings.scorePerformanceColorHighlighted);
                        g2.drawLine((int) this.anchorNode.getX(), (int) this.anchorNode.getY(), this.getMousePositionInImage().x, this.getMousePositionInImage().y);
                    }

                    // draw the performance annotation symbol at the mouse position (kind of preview)
                    g2.setColor(Settings.scorePerformanceColor);
                    g2.fillRect(this.getMousePositionInImage().x - this.xOffset, this.getMousePositionInImage().y - this.xOffset, this.xWidth, this.xWidth);
//                    g2.fill(ScoreDisplayPanel.drawDiamond(this.getMousePositionInImage().x, this.getMousePositionInImage().y, this.xWidth, this.xWidth));
                    break;

                default:
                    break;
            }
        }

        // draw the overlay with note annotations
//        Score score = this.parent.parent.getScore();
        for (Map.Entry<Element, ScoreNode> overlayElement : this.scorePage.getAllEntries().entrySet()) {    // go through all overlay elements on the score page
            Element element = overlayElement.getKey();                                                      // get the data of the overlay element
            ONGNode p = overlayElement.getValue();                                                          // get the ONGNode ()

            if (element.getLocalName().equals("note")) {    // draw a note overlay
                if (element == selectedMsmNode) {
                    g2.setColor(Settings.scoreNoteColorHighlighted);
                } else {
                    g2.setColor(Settings.scoreNoteColor);
                }
                g2.fillOval(((int)p.getX()) - this.xOffset, ((int)p.getY()) - this.yOffset, this.xWidth, this.yWidth);  // paint the note
            }
            else {                                                                  // draw a performance overlay
                // set the color
                if ((selectedMpmNode != null) && (element == selectedMpmNode.getUserObject())) {    // if the node is selected
                    g2.setColor(Settings.scorePerformanceColorHighlighted);                         // use the highlight color
                } else {                                                                            // node is not selected
                    boolean samePerformance = ScoreDisplayPanel.samePerformance(element, selectedMpmNode);
                    if (samePerformance) {                                                          // node is in the same performance as the cursor in the MPM tree
                        g2.setColor(Settings.scorePerformanceColor);                                // use normal performance symbol color
                    } else {                                                                        // cursor is in another performance than the node to be painted
                        g2.setColor(Settings.scorePerformanceColorFaded);                           // use the faded color
                    }
                }
                if (element.getLocalName().equals("style")) {       // style elements get a different symbol then ...
                    GeneralPath diamond = ScoreDisplayPanel.drawDiamond(p.getX(), p.getY(), this.xWidth, this.xWidth);
                    g2.fill(diamond);

                    // global MPM nodes get an additional outline
                    if (ScoreDisplayPanel.isGlobal(element)) {
                        float outlineWidth = this.yWidth / 5.0f;
                        BasicStroke outlineStroke = new BasicStroke(outlineWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
                        g2.setStroke(outlineStroke);
                        g2.setColor(g2.getColor().brighter());
                        g2.draw(diamond);
                    }
                } else {                                            // a performance instruction
                    g2.fillRect(((int) p.getX()) - this.xOffset, ((int) p.getY()) - this.xOffset, this.xWidth, this.xWidth);  // paint the note

                    // global MPM nodes get an additional outline
                    if (ScoreDisplayPanel.isGlobal(element)) {
                        float outlineWidth = this.yWidth / 5.0f;
                        BasicStroke outlineStroke = new BasicStroke(outlineWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
                        g2.setStroke(outlineStroke);
                        g2.setColor(g2.getColor().brighter());
                        g2.drawRect(((int) p.getX()) - this.xOffset, ((int) p.getY()) - this.xOffset, this.xWidth, this.xWidth);
                    }
                }
            }

            // this switch code might be used instead of the above if-else to differentiate between more than just note and no note
//            switch (element.getLocalName()) {
//                case "note":
//                    break;
//                case "accentuationPattern":
//                case "articulation":
//                case "asynchrony":
//                case "distribution.correlated.brownianNoise":
//                case "distribution.correlated.compensatingTriangle":
//                case "distribution.gaussian":
//                case "distribution.list":
//                case "distribution.triangular":
//                case "distribution.uniform":
//                case "dynamics":
//                case "rubato":
//                case "style":
//                case "tempo":
//                    break;
//                default:
//                    break;
//            }

            // Debug output: this draws the connections of the Orthant Neighborhood Graph
            if (Settings.debug) {
                for (ONGNode neighbor : p.neighbors) {
                    if (neighbor != null)
                        g2.drawLine((int) p.getX(), (int) p.getY(), (int) neighbor.getX(), (int) neighbor.getY());
                }
            }
        }
    }

    /**
     * A helper method to draw a diamond shape.
     * @param x
     * @param y
     * @param xWidth
     * @param yWidth
     * @return
     */
    private static GeneralPath drawDiamond(double x, double y, double xWidth, double yWidth) {
        double xOffset = xWidth * 0.5;
        double yOffset = yWidth * 0.5;

        GeneralPath diamond = new GeneralPath();
        diamond.moveTo(x, y - yOffset);
        diamond.lineTo(x + xOffset, y);
        diamond.lineTo(x, y + yOffset);
        diamond.lineTo(x - xOffset, y);
        diamond.closePath();

        return diamond;
    }

    /**
     * the panel's paint method
     * @param g
     */
    @Override
    public void paint(Graphics g) {
        super.paint(g);
    }

    /**
     * invoke this method to reset the image zoom to match the panel size and translate the image to the initial position
     */
    private void reset() {
        this.affineTransform.setToIdentity();
        this.zoomFactor = ((double) this.getHeight()) / this.scorePage.getImage().getHeight();
        this.affineTransform.scale(this.zoomFactor, this.zoomFactor);

        try {
            this.inverseAffineTransform = this.affineTransform.createInverse();
        } catch (NoninvertibleTransformException e) {
            e.printStackTrace();
        }

        this.repaint();
    }

    /**
     * This method checks if two input objects have the same parent performance.
     * This is a helper method for paintComponent() to determine which ONGNodes from the MPM should be painted.
     * We want to paint only performance overlays from the performance in which the MpmTree cursor currently is. We do not want to mix elements from different performances.
     * @param mpmElement the element to be painted (or not if it is not in the same performance as e2); the element must be part of the MPM!
     * @param e2 the currently selected MpmTreeNode
     * @return true if the parent performance is the same for both, false in every other case
     */
    private static boolean samePerformance(Element mpmElement, MpmTreeNode e2) {
        if ((mpmElement == null) || (e2 == null))                       // if any of the input objects is null
            return false;                                               // the result is false

        // determine the performance of mpmElement
        Node p1;
        for (p1 = mpmElement.getParent(); p1 != null; p1 = p1.getParent()) {
            if (((Element) p1).getLocalName().equals("performance"))
                break;
        }
        if (p1 == null)
            return false;

        // determine the performance within which the currently selected MpmTreeNode is and check whether it is the same performance as p1
        MpmTreeNode p2;
        for (p2 = e2; !p2.isRoot(); p2 = p2.getParent()) {
            if (p2.getType().equals(MpmTreeNode.MpmNodeType.performance)) {     // found the parent performance
                if (p1 == ((Performance) p2.getUserObject()).getXml())          // if it is the same performance as p1
                    return true;                                                // return true
                break;                                                          // no need to check further parental nodes, there can be no performance within a performance
            }
        }

        return false;
    }

    /**
     * Determine whether an MPM element is in the global environment.
     * @param mpmElement
     * @return
     */
    private static boolean isGlobal(Element mpmElement) {
        if (mpmElement == null)
            return false;

        for (Node parent = mpmElement.getParent(); parent != null; parent = parent.getParent()) {
            Element p = (Element) parent;
            switch (p.getLocalName()) {
                case "part":
                    return false;
                case "global":
                    return true;
            }
        }

        return false;
    }

    /**
     * a getter for the mouse position in the image space
     * @return
     */
    public synchronized Point getMousePositionInImage() {
        return this.mousePositionInImage;
    }

    /**
     * set the mouse position in image space
     * @param point
     */
    public synchronized void setMousePositionInImage(Point point) {
        this.mousePositionInImage = point;
    }

    /**
     * get the index of the page currently displayed
     * @return
     */
    public int getPageIndex() {
        return this.pageIndex;
    }

    /**
     * get the score page currently displayed
     * @return
     */
    public ScorePage getScorePage() {
        return this.scorePage;
    }

    /**
     * display the next page in the list
     */
    public void nextPage() {
        Score score = this.parent.parent.getScore();

        if ((this.pageIndex + 1) >= score.size())
            return;

        this.pageIndex++;
        this.scorePage = score.getPage(this.pageIndex);

        this.repaint();
    }

    /**
     * display the previous page in the list
     */
    public void previousPage() {
        if (this.pageIndex == 0)
            return;

        this.pageIndex--;
        this.scorePage = this.parent.parent.getScore().getPage(this.pageIndex);

        this.repaint();
    }

    /**
     * jump to a page indicated by index
     * @param index the page index
     */
    public void showPage(int index) {
        Score score = this.parent.parent.getScore();

        if (index >= score.size())
            return;

        this.pageIndex = index;
        this.scorePage = score.getPage(this.pageIndex);

        this.repaint();
    }

    /**
     * invoke this method when the size of the overlay elements needs to be updated
     */
    protected void updateOverlayElementsScaleFactor() {
        int exp = (int)this.parent.annotationSizeSpinner.getValue();
        this.overlayElementScaleFactor = 5.0 * Math.pow(1.05, exp);

        this.xOffset = (int)(this.overlayElementScaleFactor * 3.0);      // horizontal offset to center the ellipsis around its position
        this.yOffset = (int)(this.overlayElementScaleFactor * 2.0);      // vertical offset to center the ellipsis around its position
        this.xWidth = (int)(this.overlayElementScaleFactor * 6.0);       // the width of the ellipsis
        this.yWidth = (int)(this.overlayElementScaleFactor * 4.0);       // the height of the ellipsis

        this.repaint();
    }

    /**
     * this implements the dragging of the image
     * @param mousePosition
     */
    private void dragImage(Point mousePosition) {
        if (this.dragStartPoint == null) {
            this.dragStartPoint = mousePosition;        // the drag gesture did not start yet, hence, we store the start position of the gesture first
            return;
        }

        this.diff.setLocation(mousePosition.x - this.dragStartPoint.x, mousePosition.y - this.dragStartPoint.y);        // get difference to start position (when mouse press began)
        this.affineTransform.setToIdentity();                                                                           // initialize the new transform
        this.affineTransform.translate(this.offset.getX() + this.diff.getX(), this.offset.getY() + this.diff.getY());   // add translation to the transform

        // add scaling to the transform
        if (this.zoomFactor == null)
            this.affineTransform.scale(1.0, 1.0);
        else
            this.affineTransform.scale(this.zoomFactor, this.zoomFactor);

        try {
            this.inverseAffineTransform = this.affineTransform.createInverse();
        } catch (NoninvertibleTransformException e) {
            e.printStackTrace();
        }

        this.repaint();     // repaint the contents with this new transform
    }

    /**
     * this implements the end of an image drag action
     */
    private void dragEnded() {
        if ((this.diff.getX() != 0) && (this.diff.getY() != 0)) {               // if we had a drag interaction, we keep the image offset so it does not jump back to its initial position
            this.offset.setLocation(this.offset.getX() + this.diff.getX(), this.offset.getY() + this.diff.getY());
        }
        this.dragStartPoint = null;
    }

    /**
     * this implements the logic when a drag or select gesture is performed and the mouse button has been released
     * @param mouseEvent
     */
    private void dragOrSelectGesture(MouseEvent mouseEvent) {
        if (this.dragStartPoint != null) {                                      // user has actually performed a drag gesture
            this.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
            this.dragEnded();
            return;
        }

        // it was a click into the score (mouseClicked() also fires) and the user might have selected something
        Element selectedElement = this.getOverlayElementAt(mouseEvent);         // get the overlay element that the mouse click selects
        if (selectedElement == null) {                                          // click was over nothing
            this.parent.parent.getMsmTree().clearSelection();                   // deselect anything in the MSM tree
            this.parent.parent.getMpmTree().clearSelection();                   // deselect anything in the MPM tree
            this.repaint();
            return;
        }

        switch (selectedElement.getLocalName()) {
            case "note": {
                MsmTree msmTree = this.parent.parent.getMsmTree();              // a handle to the msm tree
                MsmTreeNode msmTreeNode = msmTree.findNode(selectedElement, true);    // get the msm tree's node that corresponds with the selected note
                if (msmTreeNode == null)                                        // if nothing has been selected
                    return;                                                     // done
                msmTree.setSelectedNode(msmTreeNode);                           // select the node in the msm tree
                msmTree.scrollPathToVisible(msmTreeNode.getTreePath());         // scroll the tree so the node is visible

                switch (mouseEvent.getButton()) {
                    case MouseEvent.BUTTON1:                                    // left click
                        break;
                    case MouseEvent.BUTTON3:                                    // right click = context menu
                        WebPopupMenu menu = MpmEditingTools.makeScoreContextMenu(msmTreeNode, msmTree, scorePage);
                        menu.show(this, mouseEvent.getX() - 25, mouseEvent.getY());
                        break;
                }
                break;
            }
            default: {                                                          // a performance instruction
                MpmTree mpmTree = this.parent.parent.getMpmTree();              // a handle to the mpm tree
                MpmTreeNode mpmTreeNode = mpmTree.findNode(selectedElement, true);    // get the msm tree's node that corresponds with the selected note
                if (mpmTreeNode == null)                                        // if nothing has been selected
                    return;                                                     // done
                mpmTree.setSelectedNode(mpmTreeNode);                           // select the node in the mpm tree
                mpmTree.scrollPathToVisible(mpmTreeNode.getTreePath());         // scroll the tree so the node is visible

                switch (mouseEvent.getButton()) {
                    case MouseEvent.BUTTON1:                                    // left click
                        if (mouseEvent.getClickCount() > 1)                     // if double (or more) click
                            MpmEditingTools.quickOpenEditor(mpmTreeNode, mpmTree);  // open editor dialog
                        break;
                    case MouseEvent.BUTTON3:                                    // right click = context menu
                        WebPopupMenu menu = MpmEditingTools.makeScoreContextMenu(mpmTreeNode, mpmTree, scorePage);
                        menu.show(this, mouseEvent.getX() - 25, mouseEvent.getY());
                        break;
                }
            }
        }
    }

    /**
     * A helper method to keep the mouse listener methods clear. It implements the procedure to annotate a note position on the current page
     * @param mouseEvent
     */
    private void makeNoteAssociation(MouseEvent mouseEvent) {
        MsmTreeNode currentNode = this.parent.parent.getMsmTree().getSelectedNode();            // get the currently selected node
        if ((currentNode == null) || (currentNode.getType() != MsmTreeNode.XmlNodeType.note)) { // but there is no node of type note selected in the MSM tree to be associated with the pixel position
            this.noNoteSelected.showPopup(this, mouseEvent.getPoint());                         // display the popup message at the mouse position
            return;
        }

//        Point p = this.mouse2PixelPosition(mouseEvent);                                         // transform the mouse click position to image pixel coordinates via inverting the affine transform of the image ... this has already been done and stored in this.mousePositionInImage when this method is invoked

        // update the note association data in the project data structure
        Element note = (Element) currentNode.getUserObject();
        ScoreNode noteNode = this.scorePage.addEntry(this.getMousePositionInImage().getX(), this.getMousePositionInImage().getY(), note);

        repaint();                                                                              // the overlay has been updated, so we need to repaint

        this.parent.parent.getMsmTree().updateNode(currentNode);                                // update the indication that the note is associated to a pixel position now

        // in the MSM tree find and select the next node of type note
        for (MsmTreeNode nextNode = currentNode.getNextNode(); nextNode != null; nextNode = nextNode.getNextNode()) {
            if (nextNode.getType() == MsmTreeNode.XmlNodeType.note) {                           // found the next note
                this.parent.parent.getMsmTree().setSelectedNode(nextNode);                      // select it
                this.parent.parent.getMsmTree().scrollPathToVisible(nextNode.getTreePath());    // scroll the tree so the node is visible
                return;
            }
        }
        this.parent.parent.getMsmTree().clearSelection();       // no note node was found (null), clear the selection so the next click won't overwrite the last note's coordinates
    }

    /**
     * this helper method creates a popup menu for placing and creating performance instructions in the score
     * @return
     */
    private WebPopupMenu makePlaceAndCreateContextMenu() {
        Point mousePosInImage = this.getMousePositionInImage();
        return new PlaceAndCreateContextMenu(mousePosInImage, this); // create popup menu for the creation and placement of MPM nodes
    }

    /**
     * the routine to select an element from the overlay
     * @param mouseEvent
     * @return
     */
    private Element getOverlayElementAt(MouseEvent mouseEvent) {
        if (this.scorePage.isEmpty())                               // this score page has no elements
            return null;

        // retrieve the nearest point to the mouse position
        Point mousePoint = this.mouse2PixelPosition(mouseEvent);    // convert mouse position to pixel position on the score page
//        KeyValue<ONGNode, Double> nearest = Tools.findNearestPoint(this.overlayElementsForCurrentPage, mousePoint);       // find the nearest point, the brute force method
        KeyValue<ONGNode, Double> nearest = this.scorePage.findNearestNeighborOf(mousePoint.getX(), mousePoint.getY());
        if (nearest == null)
            return null;

        // check whether the mouse is close enough to the nearest point so its element is selected
        if ((Math.sqrt(nearest.getValue()) * 2) > this.xWidth)      // if the mouse is too far to select the element
            return null;                                            // done

        // return the first Element in the list of elements
        ArrayList<Element> elements = ((ScoreNode) nearest.getKey()).getAssociatedElements();
        for (Element element : elements) {
            return element;
        }

        return null;
    }

    /**
     * this is a shortcut to get the neighbors of the position
     * @param point2D
     * @return
     */
    private ONGNode getNeighboringNodes(Point2D point2D) {
        return this.scorePage.findAllNearestNeighborsOf(point2D.getX(), point2D.getY());   // get the four neighbors to the point via a new ScoreNode
    }

    /**
     * for a given point, determine its four nearest neighbors and inverse nearest neighbors
     * @param point2D
     * @return
     */
    private ArrayList<ONGNode> getAllDirectAndInverseNeighbors(Point2D point2D) {
        ArrayList<ONGNode> neighbors = new ArrayList<>();      // put the neighboring nodes to the mouse position in the connectMeWithMouse list which is later used to highlight those nodes

        neighbors.add(this.scorePage.findNearestNeighborOf(point2D.getX(), point2D.getY()).getKey());    // get the nearest neighbor to the mouse position

        // get the mouse node's nearest neighbors
        ONGNode pivotNode = this.scorePage.findAllNearestNeighborsOf(point2D.getX(), point2D.getY());   // get the four neighbors to it via a new ONGNode
        for (ONGNode n : pivotNode.neighbors) {                     // for each of the neighbors
            if (n != null) {                                        // if it is not null
                neighbors.add(n);                                   // add it to the connectMeWithMouse list
            }
        }

        // get the mouse node's inverse nearest neighbors
        for (KeyValue<ONGNode, Integer> n : pivotNode.findMyInverseNeighbors()) {  // find all inverse nearest neighbors
            neighbors.add(n.getKey());
        }

        return  neighbors;
    }

    /**
     * set the anchorNode according to its current value and the nearest neighbor to the specified point
     * @param point2D
     */
    private void updateAnchor(Point2D point2D) {
        KeyValue<ONGNode, Double> nearest = this.scorePage.findNearestNeighborOf(point2D.getX(), point2D.getY());   // find the nearest neighboring overlay node
        if (this.anchorNode == null) {
            this.anchorNode = (ScoreNode) nearest.getKey();
            return;
        }
        if (this.anchorNode != nearest.getKey()) {
            double anchorDistance = this.anchorNode.distanceSq(point2D);
            if ((nearest.getValue() / anchorDistance) <= Settings.anchorSwitchOvershootThreshold) {     // We switch the anchor to the nearest node only if we get much closer than we are to the current anchor. The threshold ratio is defined in Settings.
                this.anchorNode = (ScoreNode) nearest.getKey();
            }
        }
    }

    /**
     * convert the mouse position to the pixel position on the image
     * @param mouseEvent
     * @return
     */
    private Point mouse2PixelPosition(MouseEvent mouseEvent) {
        double x = mouseEvent.getX() * this.inverseAffineTransform.getScaleX() + this.inverseAffineTransform.getTranslateX();
        double y = mouseEvent.getY() * this.inverseAffineTransform.getScaleY() + this.inverseAffineTransform.getTranslateY();
        return new Point((int)x, (int)y);
    }

    /**
     * mouse clicked
     * @param mouseEvent
     */
    @Override
    public void mouseClicked(MouseEvent mouseEvent) {
    }

    /**
     * mouse pressed
     * @param mouseEvent
     */
    @Override
    public void mousePressed(MouseEvent mouseEvent) {
        this.requestFocusInWindow();    // obtain focus on mouse press

//        switch (this.parent.currentInteractionMode) {
//            case markNotes:
//                if (mouseEvent.isControlDown())                                         // if CTRL is pressed we are in pan & zoom mode
//                    break;                                                              // done
//                switch (mouseEvent.getButton()) {
////                    case MouseEvent.BUTTON1:                                            // left click
////                        this.noteAnnotationCursorColor = Settings.scoreNoteColor;
////                        break;
//                    case MouseEvent.BUTTON3:                                            // right click deletes (at mouseRelease) an entry in the score, so change the cursor color accordingly
//                    default:
//                        break;
//                }
//                this.repaint();
//                break;
//
//            case editPerformance:
////                if (mouseEvent.isControlDown())                                         // if CTRL is pressed we are in pan & zoom mode
////                    break;                                                              // done
////                switch (mouseEvent.getButton()) {
//////                    case MouseEvent.BUTTON1:                                            // left click
//////                        this.noteAnnotationCursorColor = Settings.scoreNoteColor;
//////                        break;
////                    case MouseEvent.BUTTON3:                                            // right click deletes (at mouseRelease) an entry in the score, so change the cursor color accordingly
////                        this.noteAnnotationCursorColor = Settings.scoreNoteDeleteColor;
////                        break;
////                    default:
////                        break;
////                }
////                this.repaint();
//                break;
//
//            case panAndZoom:
//            default:
//                break;
//        }
    }

    /**
     * mouse released
     * @param mouseEvent
     */
    @Override
    public void mouseReleased(MouseEvent mouseEvent) {
        if (mouseEvent.isControlDown() || (this.parent.currentInteractionMode == ScoreDocumentData.InteractionMode.panAndZoom)) {   // if we are in pan mode
            this.dragOrSelectGesture(mouseEvent);
            return;                                                                     // done
        }

        switch (this.parent.currentInteractionMode) {
            case markNotes:                                                             // we are in mark notes mode
                this.setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
                this.setMousePositionInImage(this.mouse2PixelPosition(mouseEvent));

                switch (mouseEvent.getButton()) {
                    case MouseEvent.BUTTON1:                                            // left click = make note association
                        this.makeNoteAssociation(mouseEvent);                           // create the note association
                        break;
                    case MouseEvent.BUTTON3:                                            // right click = context menu
                        Element selectedElement = this.getOverlayElementAt(mouseEvent); // get the overlay element that the mouse click selects
                        MsmTree msmTree = this.parent.parent.getMsmTree();              // a handle to the msm tree
                        MsmTreeNode msmTreeNode = msmTree.findNode(selectedElement, true);    // get the msm tree's node that corresponds with the selected note
                        if (msmTreeNode != null) {                                      // if something has been selected
                            msmTree.setSelectedNode(msmTreeNode);                       // select the node in the msm tree
                            msmTree.scrollPathToVisible(msmTreeNode.getTreePath());     // scroll the tree so the node is visible
                            WebPopupMenu menu = MpmEditingTools.makeScoreContextMenu(msmTreeNode, msmTree, scorePage);
                            menu.show(this, mouseEvent.getX() - 25, mouseEvent.getY());
                        }
                        break;
                    default:
                        break;
                }
                break;

            case editPerformance:
                this.setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
                this.setMousePositionInImage(this.mouse2PixelPosition(mouseEvent));

                switch (mouseEvent.getButton()) {
                    case MouseEvent.BUTTON1:                                            // left click = open performance popup menu
                        if (this.parent.parent.getMpm() == null) {
                            this.noMpm.showPopup(this, mouseEvent.getPoint());          // display the popup message at the mouse position
                            return;
                        }
                        if (this.parent.parent.getMpm().size() == 0) {
                            this.noPerformance.showPopup(this, mouseEvent.getPoint());  // display the popup message at the mouse position
                            return;
                        }
                        this.makePlaceAndCreateContextMenu().show(this, mouseEvent.getX() - 25, mouseEvent.getY());  // the -25 x offset is to place the upper excluded corner at the mouse position
                        break;
                    case MouseEvent.BUTTON3: {                                          // right click
                        Element selectedElement = this.getOverlayElementAt(mouseEvent); // get the overlay element that the mouse click selects
                        MpmTree mpmTree = this.parent.parent.getMpmTree();              // a handle to the mpm tree
                        MpmTreeNode mpmTreeNode = mpmTree.findNode(selectedElement, true);    // get the mpm tree's element that corresponds with the selected node
                        if (mpmTreeNode != null) {                                      // if something has been selected
                            mpmTree.setSelectedNode(mpmTreeNode);                       // select the node in the mpm tree
                            mpmTree.scrollPathToVisible(mpmTreeNode.getTreePath());     // scroll the tree so the node is visible
                            WebPopupMenu editMenu = MpmEditingTools.makeScoreContextMenu(mpmTreeNode, mpmTree, scorePage);
                            editMenu.show(this, mouseEvent.getX() - 25, mouseEvent.getY());
                        }
                        break;
                    }
                    default:
                        break;
                }
                break;

//            case panAndZoom:
//            default:
//                this.dragOrSelectGesture(mouseEvent);
//                break;
        }
    }

    /**
     * mouse enters the panel
     * @param mouseEvent
     */
    @Override
    public void mouseEntered(MouseEvent mouseEvent) {
        if (mouseEvent.isControlDown()) {
            this.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
            return;
        }

        switch (this.parent.currentInteractionMode) {
            case markNotes:
                this.setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
                this.setMousePositionInImage(this.mouse2PixelPosition(mouseEvent));
                this.anchorNode = null;
                this.repaint();
                break;
            case editPerformance:
                this.setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
                this.setMousePositionInImage(this.mouse2PixelPosition(mouseEvent));
                if (this.scorePage.isEmpty())
                    break;
                this.updateAnchor(this.getMousePositionInImage());
                this.repaint();
                break;

            case panAndZoom:
            default:
                this.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                break;
        }
    }

    /**
     * mouse leaves the panel
     * @param mouseEvent
     */
    @Override
    public void mouseExited(MouseEvent mouseEvent) {
        if ((this.parent.currentInteractionMode != ScoreDocumentData.InteractionMode.panAndZoom) && !mouseEvent.isControlDown()) {
            this.setMousePositionInImage(null);
            this.anchorNode = null;
            this.repaint();
        }
    }

    /**
     * drag gesture
     * @param mouseEvent
     */
    @Override
    public void mouseDragged(MouseEvent mouseEvent) {
        // in pan and zoom mode do this
        if ((this.parent.currentInteractionMode == ScoreDocumentData.InteractionMode.panAndZoom) || mouseEvent.isControlDown()) {
            this.setMousePositionInImage(null);
            this.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));                         // set the drag cursor
            this.dragImage(mouseEvent.getLocationOnScreen());
            return;
        }

        // in performance mode, the anchor note stays fixed and we can drag the position even closer to other nodes
        if (this.parent.currentInteractionMode == ScoreDocumentData.InteractionMode.editPerformance) {
            this.setMousePositionInImage(this.mouse2PixelPosition(mouseEvent));
            this.repaint();
            return;
        }

//        this.mousePositionInImage = this.mouse2PixelPosition(mouseEvent);
//        this.repaint();
    }

    /**
     * mouse moves
     * @param mouseEvent
     */
    @Override
    public void mouseMoved(MouseEvent mouseEvent) {
        if (mouseEvent.isControlDown()) {   // if CTRL is pressed we are in pan & zoom mode and there is nothing to be done
            Element selectedElement = this.getOverlayElementAt(mouseEvent);         // get the overlay element at the mouse position
            this.setCursor((selectedElement == null) ? Cursor.getDefaultCursor() : new Cursor(Cursor.HAND_CURSOR)); // change mouse cursor to hand if there is an overlay element
            return;
        }

        switch (this.parent.currentInteractionMode) {
            case markNotes:
                this.setMousePositionInImage(this.mouse2PixelPosition(mouseEvent));
                this.repaint();
                break;

            case editPerformance:        // in this mode, we track the nearest node to the mouse position to set the anchor node where a performance instruction will be associated (the date of the element behind the anchor node)
                this.setMousePositionInImage(this.mouse2PixelPosition(mouseEvent));
                if (!this.scorePage.isEmpty())
                    this.updateAnchor(this.getMousePositionInImage());
                this.repaint();
                break;

            case panAndZoom:
                Element selectedElement = this.getOverlayElementAt(mouseEvent);         // get the overlay element at the mouse position
                this.setCursor((selectedElement == null) ? Cursor.getDefaultCursor() : new Cursor(Cursor.HAND_CURSOR)); // change mouse cursor to hand if there is an overlay element
            default:
                break;
        }
    }

    /**
     * mouse wheel interaction
     * @param mouseWheelEvent
     */
    @Override
    public void mouseWheelMoved(MouseWheelEvent mouseWheelEvent) {
        if (!((this.parent.currentInteractionMode == ScoreDocumentData.InteractionMode.markNotes) || (this.parent.currentInteractionMode == ScoreDocumentData.InteractionMode.editPerformance)) || mouseWheelEvent.isControlDown()) {     // we are in pan and zoom mode
            if (this.zoomFactor == null)                        // make sure zoomFactor is initialized
                this.zoomFactor = 1.0;

            double prevZoomFactor = this.zoomFactor;            // store the zoom factor so far, needed later

            if (mouseWheelEvent.getWheelRotation() < 0)         // zoom in
                this.zoomFactor *= 1.1;
            else if (mouseWheelEvent.getWheelRotation() > 0)    // zoom out
                this.zoomFactor /= 1.1;
            else                                                // in any other case
                return;                                         // cancel

            // compute the transform
            double zoomDiv = this.zoomFactor / prevZoomFactor;
            this.offset.setLocation((zoomDiv) * (this.offset.getX()) + (1.0 - zoomDiv) * mouseWheelEvent.getX(), (zoomDiv) * (this.offset.getY()) + (1.0 - zoomDiv) * mouseWheelEvent.getY());
            this.affineTransform.setToIdentity();
            this.affineTransform.translate(this.offset.getX(), this.offset.getY());
            this.affineTransform.scale(this.zoomFactor, this.zoomFactor);

            try {
                this.inverseAffineTransform = this.affineTransform.createInverse();
            } catch (NoninvertibleTransformException e) {
                e.printStackTrace();
            }

            this.repaint();     // repaint the contents with this new transform
        }
    }

//    /**
//     * define keyboard interaction
//     */
//    private void addKeyboardInput() {
//        InputMap inputMap = this.getInputMap(JComponent.WHEN_FOCUSED);          // this inputmap fires only if the panel is focussed
//
//        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0), "PrevPage");
//        this.getActionMap().put("PrevPage", new AbstractAction() {
//            @Override
//            public void actionPerformed(ActionEvent e) {
//                previousPage();
//            }
//        });
//
//        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0), "NextPage");
//        this.getActionMap().put("NextPage", new AbstractAction() {
//            @Override
//            public void actionPerformed(ActionEvent e) {
//                nextPage();
//            }
//        });
//
//        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "DeletePage");
//        this.getActionMap().put("DeletePage", new AbstractAction() {
//            @Override
//            public void actionPerformed(ActionEvent e) {
//                parent.parent.removeScorePage(getPageIndex());
//            }
//        });
//    }

    /**
     * actions to be triggered when a key is typed
     * @param keyEvent
     */
    @Override
    public void keyTyped(KeyEvent keyEvent) {
    }

    /**
     * actions to be triggered when a key is pressed
     * @param keyEvent
     */
    @Override
    public void keyPressed(KeyEvent keyEvent) {
    }

    /**
     * actions to be triggered when a key is released
     * @param keyEvent
     */
    @Override
    public void keyReleased(KeyEvent keyEvent) {
        switch (keyEvent.getKeyCode()) {
            case KeyEvent.VK_LEFT:
                this.previousPage();
                break;
            case KeyEvent.VK_RIGHT:
                this.nextPage();
                break;
//            case KeyEvent.VK_DELETE:
////                this.parent.parent.removeScorePage(getPageIndex());     // remove score page
//
//                 // remove the currently selected note entry
//                MsmTreeNode currentMsmTreeNode = this.parent.parent.getMsmTree().getSelectedNode();
//                if ((currentMsmTreeNode != null) && (currentMsmTreeNode.getType() == MsmTreeNode.XmlNodeType.note)) {
//                    this.scorePage.removeEntry((Element) currentMsmTreeNode.getUserObject());
//                    this.repaint();
//                    this.parent.parent.getMsmTree().updateNode(currentMsmTreeNode);
//                }
//                break;
            case KeyEvent.VK_CONTROL:
                if ((this.parent.currentInteractionMode == ScoreDocumentData.InteractionMode.markNotes) || (this.parent.currentInteractionMode == ScoreDocumentData.InteractionMode.editPerformance)) {
                    this.setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
                }
                break;
            default:
                break;
        }
    }
}