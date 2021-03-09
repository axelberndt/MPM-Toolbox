package mpmToolbox.gui.mpmEditingTools.editDialogs.visualizers;

import com.alee.laf.panel.WebPanel;
import mpmToolbox.gui.Settings;

import java.awt.*;

/**
 * A WebPanel that paints the visualization of an MPM accentuation.
 * @author Axel Berndt
 */
public class AccentuationVisualizer extends WebPanel {
    private double value;
    private double transitionFrom;
    private double transitionTo;

    /**
     * constructor
     * @param value
     * @param transitionFrom
     * @param transitionTo
     */
    public AccentuationVisualizer(double value, double transitionFrom, double transitionTo) {
        super();
        this.value = value;
        this.transitionFrom = transitionFrom;
        this.transitionTo = transitionTo;

        // we have to set an initial non-zero preferred size so the panel will actually take room in the gridbaglayout, even though it will be stretched later on; without this it won't show up
        int size = (this.getFontMetrics(this.getFont()).getHeight() + Settings.paddingInDialogs) * 2; // some rather arbitrary size value that is derived from the font height and padding
        this.setPreferredSize(size, size);

        this.setBackground(this.getBackground().brighter());
    }

    /**
     * this method paints the visualization
     * @param g
     */
    @Override
    public void paintComponent(Graphics g){
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D)g;  // make g a Graphics2D object so we can use its extended drawing features

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
//        g2.setColor(Color.CYAN);
//        g2.drawRect(0, 0, this.getWidth()-1, this.getHeight()-1);
//        System.out.println(this.getWidth() + ", " + this.getHeight());

        double zeroD = ((double) (this.getHeight() - 1)) / 2.0;
        int zero = (int) zeroD;
        int value = zero + (int) (zeroD * -this.value);
        int transitionFrom = zero + (int) (zeroD * -this.transitionFrom);
        int transitionTo = zero + (int) (zeroD * -this.transitionTo);
        int strokeWeight = (int) ((double) this.getHeight() / 10);
        int leftMost = (int) (strokeWeight * 0.5);

        // draw transitioning polygon
        g2.setColor(this.getBackground().brighter().brighter());
        Polygon polygon = new Polygon();
        polygon.addPoint(strokeWeight, zero);
        polygon.addPoint(this.getWidth(), zero);
        polygon.addPoint(this.getWidth(), transitionTo);
        polygon.addPoint(strokeWeight, transitionFrom);
        g2.fillPolygon(polygon);

        // draw on-beat accentuation
        BasicStroke stroke = new BasicStroke(strokeWeight, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER);
        g2.setStroke(stroke);
        g2.setColor(g2.getColor().brighter().brighter());
        g2.drawLine(leftMost, zero, leftMost, value);

        // draw the zero line
        BasicStroke centerLineStroke = new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER);
        g2.setStroke(centerLineStroke);
        g2.setColor(this.getBackground().brighter());
        g2.drawLine(0, zero, this.getWidth(), zero);
    }

    /**
     * set the accentuation value and update the visualization
     * @param value
     */
    public void setValue(double value) {
        this.value = value;
        this.repaint();
    }

    /**
     * set the accentuation's transition.from value and update the visualization
     * @param value
     */
    public void setTransitionFrom(double value) {
        this.transitionFrom = value;
        this.repaint();
    }

    /**
     * set the accentuation's transition.to value and update the visualization
     * @param value
     */
    public void setTransitionTo(double value) {
        this.transitionTo = value;
        this.repaint();
    }

    /**
     * get the on beat accentuation value
     * @return
     */
    public double getValue() {
        return this.value;
    }

    /**
     * get the accentuation's transition.from value
     * @return
     */
    public double getTransitionFrom() {
        return this.transitionFrom;
    }

    /**
     * get the accentuation's transition.to value
     * @return
     */
    public double getTransitionTo() {
        return this.transitionTo;
    }
}
