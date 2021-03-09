package mpmToolbox.gui.mpmEditingTools.editDialogs.visualizers;

import com.alee.laf.panel.WebPanel;
import meico.mpm.elements.maps.data.TempoData;
import mpmToolbox.gui.Settings;

import java.awt.*;
import java.util.ArrayList;

/**
 * The visualizer for the TempoEditor.
 * @author Axel Berndt
 */
public class TempoVisualizer extends WebPanel {
    private double bpm;
    private double transitionTo;
    private double meanTempoAt;

    /**
     * constructor
     * @param bpm
     * @param transitionTo
     * @param meanTempoAt
     */
    public TempoVisualizer(double bpm, double transitionTo, double meanTempoAt) {
        super();

        this.bpm = bpm;
        this.transitionTo = transitionTo;
        this.meanTempoAt = meanTempoAt;

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

        Graphics2D g2 = (Graphics2D) g;  // make g a Graphics2D object so we can use its extended drawing features
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int zero = this.getHeight() - 1;

        // set the line properties
        BasicStroke lineStroke = new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER);
        g2.setStroke(lineStroke);
        g2.setColor(this.getBackground().brighter().brighter());

        // if no transition, just draw a horizontal line
        if (this.bpm == this.transitionTo) {
            int zero2 = zero / 2;
            Polygon polygon = new Polygon();
            polygon.addPoint(0, zero2);
            polygon.addPoint(this.getWidth(), zero2);
            polygon.addPoint(this.getWidth(), zero);
            polygon.addPoint(0, zero);
            g2.fillPolygon(polygon);
            return;
        }

        if (this.meanTempoAt == 1.0) {
            int y = (this.bpm > this.transitionTo) ? 0 : (this.getWidth() + 1);
            Polygon polygon = new Polygon();
            polygon.addPoint(0, y);
            polygon.addPoint(this.getWidth(), y);
            polygon.addPoint(this.getWidth(), zero);
            polygon.addPoint(0, zero);
            g2.fillPolygon(polygon);
            return;
        }

        if (this.meanTempoAt == 0.0) {
            int y = (this.bpm > this.transitionTo) ? (this.getWidth() + 1) : 0;
            Polygon polygon = new Polygon();
            polygon.addPoint(0, y);
            polygon.addPoint(this.getWidth(), y);
            polygon.addPoint(this.getWidth(), zero);
            polygon.addPoint(0, zero);
            g2.fillPolygon(polygon);
            return;
        }

        TempoData data = new TempoData();
        data.startDate = 0.0;
        data.endDate = (double) this.getWidth();
        data.meanTempoAt = this.meanTempoAt;
        data.exponent = Math.log(0.5) / Math.log(meanTempoAt);
        if (this.bpm < this.transitionTo) {
            data.bpm = (double) this.getHeight();
            data.transitionTo = 0.0;
        } else {
            data.bpm = 0.0;
            data.transitionTo = (double) this.getHeight();
        }

        ArrayList<double[]> points = new ArrayList<>();
        double step = data.endDate / 90.0;                  // here we can set the division of the curve
        for (double d = 0.0; d < data.endDate; d += step) {
            points.add(new double[] {d, getTempoAt(d, data)});
        }
        points.add(new double[] {data.endDate, data.transitionTo});

        Polygon polygon = new Polygon();
        polygon.addPoint(0, zero);
        for (double[] p : points)
            polygon.addPoint((int) p[0], (int) p[1]);
        polygon.addPoint(this.getWidth(), zero);
        g2.fillPolygon(polygon);
    }

    /**
     * set the initial tempo
     * @param bpm
     */
    public void setBpm(double bpm) {
        this.bpm = bpm;
        this.repaint();
    }

    /**
     * set the target tempo
     * @param transitionTo
     */
    public void setTransitionTo(double transitionTo) {
        this.transitionTo = transitionTo;
        this.repaint();
    }

    /**
     * set initial and target tempo at once
     * @param bpm
     * @param transitionTo
     */
    public void setBpmTransitionTo(double bpm, double transitionTo) {
        this.bpm = bpm;
        this.transitionTo = transitionTo;
        this.repaint();
    }

    /**
     * set the relative position where the transition reaches the mean tempo
     * @param meanTempoAt
     */
    public void setMeanTempoAt(double meanTempoAt) {
        this.meanTempoAt = meanTempoAt;
        this.repaint();
    }

    /**
     * compute the tempo in bpm from a given TempoData object and a date that should fall into the scope of the tempoData
     * @param date
     * @param tempoData the application should make sure that date is in the scope of tempoData
     * @return the tempo or 100.0 bpm if date lies out of scope or tempo data is insufficient
     */
    private static double getTempoAt(double date, TempoData tempoData) {
        double result = (date - tempoData.startDate) / (tempoData.endDate - tempoData.startDate);
        result = Math.pow(result, tempoData.exponent);
        result = result * (tempoData.transitionTo - tempoData.bpm) + tempoData.bpm;
        return result;
    }
}
