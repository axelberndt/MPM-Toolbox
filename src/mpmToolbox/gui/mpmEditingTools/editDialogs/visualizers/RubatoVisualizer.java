package mpmToolbox.gui.mpmEditingTools.editDialogs.visualizers;

import com.alee.laf.panel.WebPanel;
import mpmToolbox.gui.Settings;

import java.awt.*;

/**
 * The rubato visualizer. To be used by RubatoEditor and RubatoDefEditor.
 * @author Axel Berndt
 */
public class RubatoVisualizer extends WebPanel {
    double intensity;
    double lateStart;
    double earlyEnd;

    /**
     * constructor
     * @param intensity
     * @param lateStart
     * @param earlyEnd
     */
    public RubatoVisualizer(double intensity, double lateStart, double earlyEnd) {
        super();

        this.intensity = intensity;
        this.lateStart = lateStart;
        this.earlyEnd = earlyEnd;

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

        // set the line properties
        int strokeWeight = (int) ((double) this.getWidth() / 150);
        BasicStroke lineStroke = new BasicStroke(strokeWeight, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER);
        g2.setStroke(lineStroke);
        g2.setColor(this.getBackground().brighter().brighter());

        // draw timing indicators without rubato
        int beats = 8;
        int xInterval = (int) ((double) this.getWidth() / beats);
        int y1 = (int) ((double) this.getHeight() / 6.0);
        int y21 = y1 + y1;
        int y22 = y1 + y1 + y1;
        for (int i = 0; i < beats; ++i) {
            int y = ((i % 2) == 0) ? y1 : y21;
            g2.drawLine(i * xInterval, y, i * xInterval, y22);
        }
        g2.drawLine(this.getWidth()-1, y1, this.getWidth()-1, y22);

        // draw timing indicators with rubato
        int yDiff = y1;
        y1 = y22;
        y21 = y22 + yDiff;
        y22 = y21 + yDiff;
        for (int i = 0; i < beats; ++i) {
            int y = ((i % 2) == 0) ? y22 : y21;
            int x = (int) this.computeRubato(i * xInterval);
            g2.drawLine(x, y1, x, y);
        }
        g2.drawLine(this.getWidth()-1, y1, this.getWidth()-1, y22);
    }

    /**
     * set intensity value
     * @param intensity
     */
    public void setIntensity(double intensity) {
        this.intensity = intensity;
        this.repaint();
    }

    /**
     * set lateStart value
     * @param lateStart
     */
    public void setLateStart(double lateStart) {
        this.lateStart = lateStart;
        this.repaint();
    }

    /**
     * set early end value
     * @param earlyEnd
     */
    public void setEarlyEnd(double earlyEnd) {
        this.earlyEnd = earlyEnd;
        this.repaint();
    }

    /**
     * set all parameters at once
     * @param intensity
     * @param lateStart
     * @param earlyEnd
     */
    public void setAll(double intensity, double lateStart, double earlyEnd) {
        this.intensity = intensity;
        this.lateStart = lateStart;
        this.earlyEnd = earlyEnd;
        this.repaint();
    }

    /**
     * compute the x position of a beat indicator with rubato daviation
     * @param date
     * @return
     */
    private double computeRubato(double date) {
        double frameLength = this.getWidth();
        return (Math.pow(date / frameLength, this.intensity) * (this.earlyEnd - this.lateStart) + this.lateStart) * frameLength;
    }
}
