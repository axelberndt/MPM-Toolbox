package mpmToolbox.gui.audio.utilities;

import meico.mpm.elements.maps.data.TempoData;
import mpmToolbox.gui.Settings;
import mpmToolbox.supplementary.Tools;

import java.awt.*;
import java.awt.geom.Point2D;
import java.util.ArrayList;

/**
 * This class represents a tempo instruction in the tempoMap panel.
 * @author Axel Berndt
 */
public class TempoMapPanelElement {
    public final TempoData tempoData;                                   // the tempo instruction

    public Point2D.Double relativeCoordinates;                          // the coordinates of the instruction's start point for the tempoMap visualization in the unity square (of the whole tempoMap, not just this instruction)
    public Point2D.Double relativeEnd;                                  // the coordinates of the instruction's end point for the tempoMap visualization in the unity square
    public ArrayList<Point2D.Double> relativeCurve = new ArrayList<>(); // the coordinates of the transitioning curve's points in the unity square; this remains empty in case of a constant tempo

    public Point absoluteCoordinates;                                   // the pixel coordinates of the instruction's start point for the tempoMap visualization
    public Point absoluteEnd;                                           // the pixel coordinates of the instruction's end point for the tempoMap visualization
    public ArrayList<Point> absoluteCurve = new ArrayList<>();          // the pixel coordinates of the transitioning curve points; this remains empty in case of a constant tempo

    private int scaleWidth = 1;                                         // buffer the scale factor
    private int scaleHeight = 1;                                        // buffer the scale factor
    private int xOffset = 1;                                            // buffer the horizontal offset
    private int yOffset = 1;                                            // buffer the vertical offset

    private int[] xCoords = new int[1];                                 // the x pixel coordinates of the tempo curve's points
    private int[] yCoords = new int[1];                                 // the y pixel coordinates of the tempo curve's points

    /**
     * constructor
     */
    public TempoMapPanelElement(TempoData tempoData, Point2D.Double relativeCoordinates, Point2D.Double relativeEnd) {
        this.tempoData = tempoData;

        this.relativeCoordinates = relativeCoordinates;
        this.relativeEnd = relativeEnd;

        this.absoluteCoordinates = new Point((int) relativeCoordinates.getX(), (int) relativeCoordinates.getY());
        this.absoluteEnd = new Point((int) relativeEnd.getX(), (int) relativeEnd.getY());

        // the curve is not yet computed; this.setRelativeEndX() must be invoked
//        this.setRelativeEndX(relativeEnd.getX());       // commented out for better efficiency in method TempoMapPanel.retrieveTempoMap()
    }

    /**
     * get the pixel position of the instruction in the tempoMap visualization
     * @return
     */
    public Point getPixelPosition() {
        return new Point(this.xCoords[0], this.yCoords[0]);
    }

    /**
     * set the x-coordinate of the relative end point and recompute the curve
     * @param x
     */
    public void setRelativeEndX(double x) {
       this.relativeEnd.x = x;
       this.absoluteEnd.x = (int) Math.round(x);

        if (!this.tempoData.isConstantTempo() && (this.tempoData.exponent != 1.0)) {
            this.relativeCurve.clear();
            this.absoluteCurve.clear();

            // compute relative curve
            double xScaleFactor = this.relativeEnd.getX() - this.relativeCoordinates.getX();
            double yScaleFactor = this.relativeEnd.getY() - this.relativeCoordinates.getY();

            for (int i = 1; i < Settings.tempoCurveTesselation; ++i) {
                double x1 = ((double) i) / Settings.tempoCurveTesselation;
                double y = Math.pow(x1, this.tempoData.exponent);

                x1 = (x1 * xScaleFactor) + this.relativeCoordinates.getX();
                y = (y * yScaleFactor) + this.relativeCoordinates.getY();

                this.relativeCurve.add(new Point2D.Double(x1, y));
                this.absoluteCurve.add(new Point((int) Math.round(x1), (int) Math.round(y)));  // copy result to absolute curve
            }
        }
    }

    /**
     * scale the width of the tempo curve
     * @param width of the unity square of the complete tempoMap
     */
    private void scaleWidth(int width) {
        this.scaleWidth = width;
        this.absoluteCoordinates.x = (int) Math.round(this.relativeCoordinates.getX() * width);
        this.absoluteEnd.x = (int) Math.round(this.relativeEnd.getX() * width);
        for (int i = 0; i < this.relativeCurve.size(); ++i)
            this.absoluteCurve.get(i).x = (int) Math.round(this.relativeCurve.get(i).getX() * width);
    }

    /**
     * scale the height of the tempo curve
     * @param height of the unity square of the complete tempoMap
     */
    private void scaleHeight(int height) {
        this.scaleHeight = height;
        this.absoluteCoordinates.y = (int) Math.round(this.relativeCoordinates.getY() * -height) + height;
        this.absoluteEnd.y = (int) Math.round(this.relativeEnd.getY() * -height) + height;
        for (int i = 0; i < this.relativeCurve.size(); ++i)
            this.absoluteCurve.get(i).y = (int) Math.round(this.relativeCurve.get(i).getY() * -height) + height;
    }

    /**
     * set the horizontal pixel offset
     * @param xOffset of the whole tempoMap
     */
    private void updateXOffset(int xOffset) {
        this.xOffset = xOffset;
        this.xCoords = new int[2 + this.absoluteCurve.size()];
        this.xCoords[0] = this.absoluteCoordinates.x + xOffset;
        for (int i = 0; i < this.absoluteCurve.size(); ++i)
            this.xCoords[i + 1] = this.absoluteCurve.get(i).x + xOffset;
        this.xCoords[this.xCoords.length - 1] = this.absoluteEnd.x + xOffset;
    }

    /**
     * set the vertical pixel offset
     * @param yOffset of the whole tempoMap
     */
    private void updateYOffset(int yOffset) {
        this.yOffset = yOffset;
        this.yCoords = new int[2 + this.absoluteCurve.size()];
        this.yCoords[0] = this.absoluteCoordinates.y + yOffset;
        for (int i = 0; i < this.absoluteCurve.size(); ++i)
            this.yCoords[i + 1] = this.absoluteCurve.get(i).y + yOffset;
        this.yCoords[this.yCoords.length - 1] = this.absoluteEnd.y + yOffset;
    }

    /**
     * adjust the geometry of the tempo curve according to scale and offset values of the tempoMap
     * @param width of the unity square of the complete tempoMap
     * @param height of the unity square of the complete tempoMap
     * @param xOffset of the whole tempoMap
     * @param yOffset of the whole tempoMap
     */
    public void setScalesAndOffsets(int width, int height, int xOffset, int yOffset) {
        if (this.scaleWidth != width) {
            this.scaleWidth(width);
            this.updateXOffset(xOffset);
        }
        else if (this.xOffset != xOffset) {
            this.updateXOffset(xOffset);
        }

        if (this.scaleHeight != height) {
            this.scaleHeight(height);
            this.updateYOffset(yOffset);
        }
        else if (this.yOffset != yOffset) {
            this.updateYOffset(yOffset);
        }
    }

    /**
     * draw the instruction into the Gaphics2D object
     * @param g2d
     * @param halfSize the size of a tempo instruction square should scale with the height of the panel; this is half that size
     * @param prevConnection the end point of the previous instruction
     * @return the end point of this instruction
     */
    public Point draw(Graphics2D g2d, int halfSize, Point prevConnection) {
        g2d.setColor(Settings.scorePerformanceColor);                               // use normal performance symbol color

        // draw connection line to the preceding tempo instruction
        if ((prevConnection != null) && (prevConnection.y != this.yCoords[0]))    // only necessary if there is a preceding one, and it ends on a different value than this instructions start value
            g2d.drawLine(prevConnection.x, prevConnection.y, this.xCoords[0], this.yCoords[0]);

        // draw the curve segment of the tempo instruction
        g2d.drawPolyline(this.xCoords, this.yCoords, this.xCoords.length);

        // draw the tempo instruction's square
        int size = halfSize + halfSize;
        g2d.fillRect(this.xCoords[0] - halfSize, this.yCoords[0] - halfSize, size, size);

        // draw bpm value
        g2d.setColor(Settings.scorePerformanceColorHighlighted);
        FontMetrics metrics = g2d.getFontMetrics();
        String bpm = String.valueOf(Tools.round(this.tempoData.bpm, 2));
        if (!this.isConstantTempo())                                                // if it is a continuous tempo transition
            bpm +=  " \u2192 " + Tools.round(this.tempoData.transitionTo, 2);
        int xFont = Math.max(0, this.xCoords[0] - halfSize + (size - metrics.stringWidth(bpm)) / 2); // Determine the X coordinate for the text
        int yFont = this.yCoords[0] - (int)(halfSize * 1.5);                             // Determine the Y coordinate for the text (should be placed above the tempo node)
        g2d.drawString(bpm, xFont, yFont);                                          // Draw the string

        return new Point(this.xCoords[this.xCoords.length - 1], this.yCoords[this.yCoords.length - 1]);
    }

    /**
     * check whether this represents a constant tempo instruction
     * @return
     */
    public boolean isConstantTempo() {
        return this.tempoData.isConstantTempo();
    }
}
