package mpmToolbox.gui.mpmEditingTools.editDialogs.visualizers;

import com.alee.laf.panel.WebPanel;
import meico.supplementary.RandomNumberProvider;
import mpmToolbox.gui.Settings;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;

/**
 * This visualizes a distribution
 * @author Axel Berndt
 */
public class DistributionVisualizer extends WebPanel {
    private RandomNumberProvider rand = null;
    private long seed = (long) (Math.random() * 0x7fffffffffffffffL);

    /**
     * constructor
     */
    public DistributionVisualizer() {
        super();

        // on mouse click reseed and repaint the distribution
        this.addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (rand.getDistributionType() != RandomNumberProvider.DISTRIBUTION_LIST) {
                    reseed();
                    repaint();
                }
            }
            @Override
            public void mousePressed(MouseEvent e) {}
            @Override
            public void mouseReleased(MouseEvent e) {}
            @Override
            public void mouseEntered(MouseEvent e) {}
            @Override
            public void mouseExited(MouseEvent e) {}
        });

        this.setToolTip("Click to regenerate the random number series.");

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

        // draw the zero line
        final double zeroD = ((double) (this.getHeight() - 1)) / 2.0;
        final int zero = (int) zeroD;
        final BasicStroke centerLineStroke = new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER);
        g2.setStroke(centerLineStroke);
        g2.setColor(this.getBackground().brighter());
        g2.drawLine(0, zero, this.getWidth(), zero);

        // draw distribution
        if (this.rand == null)
            return;

        final BasicStroke stroke = new BasicStroke(1, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER);
        g2.setStroke(stroke);
        g2.setColor(this.getBackground().brighter().brighter());

        try {
            rand.getValue(0);   // make sure we have no empty distribution list and initial values for the correlated distributions
        } catch (ArithmeticException e) { // if not
            return;             // stop here
        }

        for (int x = 0; x < this.getWidth(); ++x) {
            int y = (int) -rand.getValue(x) + zero;
            g2.drawLine(x, y, x, y);
        }
    }

    /**
     * change the seed
     */
    public void reseed() {
        this.seed = (long) (Math.random() * 0x7fffffffffffffffL);

        if (this.rand == null)
            return;

        this.rand.setSeed(this.seed);
        this.setInitialValue();
    }

    /**
     * the correlated distributions need an initial value
     */
    private void setInitialValue() {
        switch (this.rand.getDistributionType()) {
            case RandomNumberProvider.DISTRIBUTION_CORRELATED_BROWNIANNOISE: {
                double scaleFactor = (rand.getUpperLimit() - rand.getLowerLimit()) * 0.5;       // the initial value should not be at the extremes, thus we limit the range of the initial value by 0.5
                double firstValue = (Math.random() * scaleFactor) + rand.getLowerLimit() + (scaleFactor * 0.5);
                this.rand.setInitialValue(firstValue);
                break;
            }
            case RandomNumberProvider.DISTRIBUTION_CORRELATED_COMPENSATING_TRIANGLE: {
                double low = Math.max(this.rand.getLowCut(), this.rand.getLowerLimit());
                double high = Math.min(this.rand.getHighCut(), this.rand.getUpperLimit());
                double scaleFactor = (high - low) * 0.5;                                        // the initial value should not be at the extremes, thus we limit the range of the initial value by 0.5
                double firstValue = (Math.random() * scaleFactor) + low + (scaleFactor * 0.5);
                this.rand.setInitialValue(firstValue);
                break;
            }
        }
    }

    /**
     * a helper method to scale the scope of the distribution to the available display height
     * @param lowerLimit
     * @param upperLimit
     * @return
     */
    private double scale(double lowerLimit, double upperLimit) {
        double scope = Math.max(Math.abs(upperLimit), Math.abs(lowerLimit));
        double halfHeight = ((double) this.getHeight()) / 2.0;
        return halfHeight / Math.max(scope, 0.1);   // prevent division by 0
    }

    /**
     * draw a uniform distribution
     * @param lowerLimit
     * @param upperLimit
     * @param seed a long value or null if no seed should be applied
     */
    public void setUniform(double lowerLimit, double upperLimit, Long seed) {
        double scale = this.scale(lowerLimit, upperLimit);
        this.rand = RandomNumberProvider.createRandomNumberProvider_uniformDistribution(lowerLimit * scale, upperLimit * scale);
        this.rand.setSeed((seed != null) ? seed : this.seed);
        this.repaint();
    }

    /**
     * draw a triangular distribution
     * @param lowerLimit
     * @param upperLimit
     * @param lowerClip
     * @param upperClip
     * @param mode
     * @param seed a long value or null if no seed should be applied
     */
    public void setTriangular(double lowerLimit, double upperLimit, double lowerClip, double upperClip, double mode, Long seed) {
        double scale = this.scale(lowerLimit, upperLimit);
        this.rand = RandomNumberProvider.createRandomNumberProvider_triangularDistribution(lowerLimit * scale, upperLimit * scale, mode * scale, lowerClip * scale, upperClip * scale);
        this.rand.setSeed((seed != null) ? seed : this.seed);
        this.repaint();
    }

    /**
     * draw a Gaussian distribution
     * @param lowerLimit
     * @param upperLimit
     * @param standardDeviation
     * @param seed a long value or null if no seed should be applied
     */
    public void setGaussian(double lowerLimit, double upperLimit, double standardDeviation, Long seed) {
        double scale = this.scale(lowerLimit, upperLimit);
        this.rand = RandomNumberProvider.createRandomNumberProvider_gaussianDistribution(standardDeviation * scale, lowerLimit * scale, upperLimit * scale);
        this.rand.setSeed((seed != null) ? seed : this.seed);
        this.repaint();
    }

    /**
     * draw a Brownian distribution
     * @param lowerLimit
     * @param upperLimit
     * @param maxStepWidth
     * @param seed a long value or null if no seed should be applied
     */
    public void setBrownian(double lowerLimit, double upperLimit, double maxStepWidth, Long seed) {
        double scale = this.scale(lowerLimit, upperLimit);
        this.rand = RandomNumberProvider.createRandomNumberProvider_brownianNoiseDistribution(maxStepWidth * scale, lowerLimit * scale, upperLimit * scale);
        this.rand.setSeed((seed != null) ? seed : this.seed);
        this.setInitialValue();
        this.repaint();
    }

    /**
     * draw a Compensating Triangle distribution
     * @param lowerLimit
     * @param upperLimit
     * @param lowerClip
     * @param upperClip
     * @param degreeOfCorrelation
     * @param seed a long value or null if no seed should be applied
     */
    public void setCompensatingTriangle(double lowerLimit, double upperLimit, double lowerClip, double upperClip, double degreeOfCorrelation, Long seed) {
        double scale = this.scale(lowerLimit, upperLimit);
        this.rand = RandomNumberProvider.createRandomNumberProvider_compensatingTriangleDistribution(degreeOfCorrelation, lowerLimit * scale, upperLimit * scale, lowerClip * scale, upperClip * scale);
        this.rand.setSeed((seed != null) ? seed : this.seed);
        this.setInitialValue();
        this.repaint();
    }

    /**
     * plot the specified distribution list
     * @param list
     */
    public void setDistributionList(ArrayList<Double> list) {
        // scale the list values to the available display height
        double max = 0;
        for (Double d : list) {
            if (Math.abs(d) > max)
                max = Math.abs(d);
        }
        double halfHeight = ((double) this.getHeight()) / 2.0;
        double scale = halfHeight / max;
        ArrayList<Double> newList = new ArrayList<>();
        for (Double d : list)
            newList.add(d * scale);

        // init random number provider and paint
        this.rand = RandomNumberProvider.createRandomNumberProvider_distributionList(newList);
        this.repaint();
    }
}
