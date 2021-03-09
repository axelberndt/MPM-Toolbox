package mpmToolbox.gui.mpmEditingTools.editDialogs.visualizers;

import com.alee.laf.panel.WebPanel;
import meico.mpm.elements.maps.data.DynamicsData;
import mpmToolbox.gui.Settings;

import java.awt.*;
import java.util.ArrayList;

/**
 * The visualizer for the DynamicsEditor.
 * @author Axel Berndt
 */
public class DynamicsVisualizer extends WebPanel {
    private double volume;
    private double transitionTo;
    private double curvature;
    private double protraction;

    /**
     * constructor
     * @param volume
     * @param transitionTo
     * @param curvature
     * @param protraction
     */
    public DynamicsVisualizer(double volume, double transitionTo, double curvature, double protraction) {
        super();

        this.volume = volume;
        this.transitionTo = transitionTo;
        this.curvature = curvature;
        this.protraction = protraction;

        // we have to set an initial non-zero preferred size so the panel will actually take room in the gridbaglayout, even though it will be stretched later on; without this it won't show up
        int size = (this.getFontMetrics(this.getFont()).getHeight() + Settings.paddingInDialogs) * 6; // some rather arbitrary size value that is derived from the font height and padding
        this.setPreferredSize(size, size);

//        this.setBorder(new LineBorder(this.getBackground(), Settings.paddingInDialogs));

        this.setBackground(this.getBackground().brighter());
    }

    /**
     * this method paints the visualization
     * @param g
     */
    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D)g;  // make g a Graphics2D object so we can use its extended drawing features
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int zero = this.getHeight() - 1;

        // set the line properties
        BasicStroke lineStroke = new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER);
        g2.setStroke(lineStroke);
        g2.setColor(this.getBackground().brighter().brighter());

        // if no transition, just draw a horizontal line
        if (this.volume == this.transitionTo) {
            int zero2 = zero / 2;
            Polygon polygon = new Polygon();
            polygon.addPoint(0, zero2);
            polygon.addPoint(this.getWidth(), zero2);
            polygon.addPoint(this.getWidth(), zero);
            polygon.addPoint(0, zero);
            g2.fillPolygon(polygon);

            return;
        }

        DynamicsData data = new DynamicsData();
        data.startDate = 0.0;
        data.endDate = (double) this.getWidth();
        data.curvature = this.curvature;
        data.protraction = this.protraction;
        if (this.volume < this.transitionTo) {
            data.volume = (double) this.getHeight();
            data.transitionTo = 0.0;
        } else {
            data.volume = 0.0;
            data.transitionTo = (double) this.getHeight();
        }

        ArrayList<double[]> points = data.getSubNoteDynamicsSegment(3);    // since I set volume and transitionTo to pixel coordinates, the argument here is also in pixels

        Polygon polygon = new Polygon();
        polygon.addPoint(0, zero);
        for (double[] p : points)
            polygon.addPoint((int) p[0], (int) p[1]);
        polygon.addPoint(this.getWidth(), zero);
        g2.fillPolygon(polygon);
    }

    /**
     * set the start volume
     * @param volume
     */
    public void setVolume(double volume) {
        this.volume = volume;
        this.repaint();
    }

    /**
     * set the target volume
     * @param transitionTo
     */
    public void setTransitionTo(double transitionTo) {
        this.transitionTo = transitionTo;
        this.repaint();
    }

    /**
     * set initial and target volume at once
     * @param volume
     * @param transitionTo
     */
    public void setVolumeTransitionTo(double volume, double transitionTo) {
        this.volume = volume;
        this.transitionTo = transitionTo;
        this.repaint();
    }

    /**
     * set the curvature parameter of the dynamics transition
     * @param curvature
     */
    public void setCurvature(double curvature) {
        this.curvature = curvature;
        this.repaint();
    }

    /**
     * set the protraction parameter of the dynamics transition
     * @param protraction
     */
    public void setProtraction(double protraction) {
        this.protraction = protraction;
        this.repaint();
    }
}
