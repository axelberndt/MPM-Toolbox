package mpmToolbox.gui.mpmEditingTools.editDialogs.ornamentDef;

import com.alee.api.annotations.NotNull;
import com.alee.laf.button.WebButton;
import com.alee.laf.combobox.WebComboBox;
import com.alee.laf.label.WebLabel;
import com.alee.laf.panel.WebPanel;
import com.alee.laf.spinner.WebSpinner;
import com.alee.laf.text.WebTextField;
import meico.mpm.elements.styles.defs.OrnamentDef;
import meico.supplementary.KeyValue;
import mpmToolbox.gui.Settings;
import mpmToolbox.supplementary.Tools;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.util.UUID;

/**
 * This class represents the temporalSpread sub-panel in the editor dialog of ornamentsDef.
 * @author Axel Berndt
 */
public class TemporalSpreadPanel extends WebPanel {
    private final int SPINNER_WIDTH = this.getFontMetrics(this.getFont()).stringWidth("999.999.99");
    private final WebTextField id = new WebTextField();
    private final WebButton generateId = new WebButton("Generate");
    private final WebLabel idLabel= new WebLabel("ID");
    private final WebLabel noteOffShiftLabel = new WebLabel("NoteOff Shift");
    private final WebComboBox noteOffShift = new WebComboBox(new NoteOffShiftItem[]{new NoteOffShiftItem(OrnamentDef.TemporalSpread.NoteOffShift.False), new NoteOffShiftItem(OrnamentDef.TemporalSpread.NoteOffShift.True), new NoteOffShiftItem(OrnamentDef.TemporalSpread.NoteOffShift.Monophonic)}, 0);   // "false" is selected by default
    private final WebLabel timeDomainLabel = new WebLabel("Time Domain");
    private final WebComboBox timeDomain = new WebComboBox(new TimeDomainItem[]{new TimeDomainItem(OrnamentDef.TemporalSpread.FrameDomain.Ticks), new TimeDomainItem(OrnamentDef.TemporalSpread.FrameDomain.Milliseconds)}, 0);
    private final WebLabel frameStartLabel = new WebLabel("Frame Start");
    private final WebSpinner frameStart = new WebSpinner(new SpinnerNumberModel(0.0, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, 1.0));
    private final WebLabel frameLengthLabel = new WebLabel("Frame Length");
    private final WebSpinner frameLength = new WebSpinner(new SpinnerNumberModel(0.0, 0.0, Double.POSITIVE_INFINITY, 1.0));
    private final WebLabel intensityLabel = new WebLabel("Intensity");
    private final WebSpinner intensity = new WebSpinner(new SpinnerNumberModel(0.0, 0.0, Double.POSITIVE_INFINITY, 0.1));


    /**
     * constructor
     */
    public TemporalSpreadPanel() {
        super();
        GridBagLayout layout = new GridBagLayout();
        this.setLayout(layout);

//        this.setPadding(Settings.paddingInDialogs);
        this.setBorder(BorderFactory.createCompoundBorder(new LineBorder(this.getBackground(), Settings.paddingInDialogs/2), new EmptyBorder(Settings.paddingInDialogs, Settings.paddingInDialogs, Settings.paddingInDialogs, Settings.paddingInDialogs)));
        this.setBackground(Tools.brighter(this.getBackground(), 0.07));

        // TODO visualizer

        // frame.start
        this.frameStartLabel.setHorizontalAlignment(WebLabel.RIGHT);
        this.frameStartLabel.setPadding(0, 0, 0, Settings.paddingInDialogs);
        Tools.addComponentToGridBagLayout(this, layout, this.frameStartLabel, 0, 0, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH, GridBagConstraints.LINE_START);
        this.frameStart.setMinimumWidth(this.SPINNER_WIDTH);
        this.frameStart.setMaximumWidth(this.SPINNER_WIDTH);
        Tools.addComponentToGridBagLayout(this, layout, this.frameStart, 1, 0, 1, 1, 20.0, 1.0, 0, 0, GridBagConstraints.BOTH, GridBagConstraints.LINE_START);

        // frame.length
        this.frameLengthLabel.setHorizontalAlignment(WebLabel.LEFT);
        this.frameLengthLabel.setPadding(0, Settings.paddingInDialogs, 0, 0);
        Tools.addComponentToGridBagLayout(this, layout, this.frameLengthLabel, 3, 0, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH, GridBagConstraints.LINE_START);
        this.frameLength.setMinimumWidth(this.SPINNER_WIDTH);
        this.frameLength.setMaximumWidth(this.SPINNER_WIDTH);
        Tools.addComponentToGridBagLayout(this, layout, this.frameLength, 2, 0, 1, 1, 20.0, 1.0, 0, 0, GridBagConstraints.BOTH, GridBagConstraints.LINE_START);

        // time domain / frame domain / time.unit
        this.timeDomainLabel.setHorizontalAlignment(WebLabel.RIGHT);
        this.timeDomainLabel.setPadding(0, 0, 0, Settings.paddingInDialogs);
        Tools.addComponentToGridBagLayout(this, layout, this.timeDomainLabel, 0, 1, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH, GridBagConstraints.LINE_START);
        this.timeDomain.setMaximumWidth(this.SPINNER_WIDTH);
        Tools.addComponentToGridBagLayout(this, layout, this.timeDomain, 1, 1, 1, 1, 20.0, 1.0, 0, 0, GridBagConstraints.BOTH, GridBagConstraints.LINE_START);

        // intensity
        this.intensityLabel.setHorizontalAlignment(WebLabel.LEFT);
        this.intensityLabel.setPadding(0, Settings.paddingInDialogs, 0, 0);
        Tools.addComponentToGridBagLayout(this, layout, this.intensityLabel, 3, 1, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH, GridBagConstraints.LINE_START);
        this.intensity.setMinimumWidth(this.SPINNER_WIDTH);
        this.intensity.setMaximumWidth(this.SPINNER_WIDTH);
        Tools.addComponentToGridBagLayout(this, layout, this.intensity, 2, 1, 1, 1, 20.0, 1.0, 0, 0, GridBagConstraints.BOTH, GridBagConstraints.LINE_START);

        // noteoff.shift
        this.noteOffShiftLabel.setHorizontalAlignment(WebLabel.RIGHT);
        this.noteOffShiftLabel.setPadding(0, 0, 0, Settings.paddingInDialogs);
        Tools.addComponentToGridBagLayout(this, layout, this.noteOffShiftLabel, 0, 2, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH, GridBagConstraints.LINE_START);
        Tools.addComponentToGridBagLayout(this, layout, this.noteOffShift, 1, 2, 2, 1, 20.0, 1.0, 0, 0, GridBagConstraints.BOTH, GridBagConstraints.LINE_START);

        // id input
        this.idLabel.setHorizontalAlignment(WebLabel.RIGHT);
        this.idLabel.setPadding(0, 0, 0, Settings.paddingInDialogs);
        Tools.addComponentToGridBagLayout(this, layout, this.idLabel, 0, 3, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH, GridBagConstraints.LINE_START);
        this.id.setMaximumWidth(this.getFontMetrics(this.id.getFont()).stringWidth("wwwwwwwwwww"));
        this.id.setMinimumWidth(this.getFontMetrics(this.id.getFont()).stringWidth("wwwwwwwwwww"));
        Tools.addComponentToGridBagLayout(this, layout, this.id, 1, 3, 2, 1, 20.0, 1.0, 0, 0, GridBagConstraints.BOTH, GridBagConstraints.LINE_START);
        this.generateId.setHorizontalAlignment(WebButton.CENTER);
        this.generateId.addActionListener(actionEvent -> this.id.setText(UUID.randomUUID().toString()));
        Tools.addComponentToGridBagLayout(this, layout, this.generateId, 3, 3, 1, 1, 1.0, 1.0, 0, 0, GridBagConstraints.BOTH, GridBagConstraints.LINE_START);
    }

    /**
     * input the value of frame.start
     * @param value
     */
    public void setFrameStart(double value) {
        this.frameStart.setValue(value);
    }

    /**
     * get the value of frame.start
     * @return
     */
    public double getFrameStart() {
        return Tools.round(((double) this.frameStart.getValue()), 10);
    }

    /**
     * input the frameLength
     * @param value
     */
    public void setFrameLength(double value) {
        this.frameLength.setValue(value);
    }

    /**
     * get the frameLength
     * @return
     */
    public double getFrameLength() {
        return Tools.round(((double) this.frameLength.getValue()), 10);
    }

    /**
     * input the time.unit value
     * @param timeDomain
     */
    public void setTimeDomain(OrnamentDef.TemporalSpread.FrameDomain timeDomain) {
        for (int i = 0; i < this.timeDomain.getItemCount(); ++i) {
            if (((TimeDomainItem) this.timeDomain.getItemAt(i)).getKey().equals(timeDomain)) {
                this.timeDomain.setSelectedIndex(i);
                break;
            }
        }
    }

    /**
     * returns the value of the time domain combobox
     * @return if null, Ticks will be returned, instead
     */
    public OrnamentDef.TemporalSpread.FrameDomain getTimeDomain() {
        if (this.timeDomain.getSelectedItem() == null)
            return OrnamentDef.TemporalSpread.FrameDomain.Ticks;

        return ((TimeDomainItem) this.timeDomain.getSelectedItem()).getKey();
    }

    /**
     * input the intensity value
     * @param value
     */
    public void setIntensity(double value) {
        this.intensity.setValue(Math.max(0.0, value));
    }

    /**
     * get the intensity value
     * @return
     */
    public double getIntensity() {
        return Tools.round(((double) this.intensity.getValue()), 10);
    }

    /**
     * input the noteOffShift value
     * @param noteOffShift
     */
    public void setNoteOffShift(OrnamentDef.TemporalSpread.NoteOffShift noteOffShift) {
        for (int i = 0; i < this.noteOffShift.getItemCount(); ++i) {
            if (((NoteOffShiftItem) this.noteOffShift.getItemAt(i)).getKey().equals(noteOffShift)) {
                this.noteOffShift.setSelectedIndex(i);
                break;
            }
        }
    }

    /**
     * returns the value of the noteoff.shift combobox
     * @return if null, False will be returned, instead
     */
    public OrnamentDef.TemporalSpread.NoteOffShift getNoteOffShift() {
        if (this.noteOffShift.getSelectedItem() == null)
            return OrnamentDef.TemporalSpread.NoteOffShift.False;

        return ((NoteOffShiftItem) this.noteOffShift.getSelectedItem()).getKey();
    }

    /**
     * get the id
     * @return
     */
    public String getId() {
        return this.id.getText();
    }

    /**
     * set the id
     * @param id
     */
    public void setId(String id) {
        this.id.setText(id);
    }

    /**
     * enable/disable the components
     * @param enabled true if this component should be enabled, false otherwise
     */
    @Override
    public void setEnabled(boolean enabled) {
        this.id.setEnabled(enabled);
        this.generateId.setEnabled(enabled);
        this.idLabel.setEnabled(enabled);
        this.noteOffShiftLabel.setEnabled(enabled);
        this.noteOffShift.setEnabled(enabled);
        this.timeDomainLabel.setEnabled(enabled);
        this.timeDomain.setEnabled(enabled);
        this.frameStartLabel.setEnabled(enabled);
        this.frameStart.setEnabled(enabled);
        this.frameLengthLabel.setEnabled(enabled);
        this.frameLength.setEnabled(enabled);
        this.intensityLabel.setEnabled(enabled);
        this.intensity.setEnabled(enabled);
    }

    /**
     * Items for the noteoff.shift chooser.
     * @author Axel Berndt
     */
    private static class NoteOffShiftItem extends KeyValue<OrnamentDef.TemporalSpread.NoteOffShift, String> {
        /**
         * This constructor creates a noteOffShift chooser item (NoteOffShift, String) pair out of a non-null noteOffShift.
         * @param noteOffShift
         */
        public NoteOffShiftItem(@NotNull OrnamentDef.TemporalSpread.NoteOffShift noteOffShift) {
            super(noteOffShift, "");
            switch (noteOffShift) {
                case False:
                    this.setValue("false");
                    break;
                case True:
                    this.setValue("true");
                    break;
                case Monophonic:
                    this.setValue("monophonic");
                    break;
                default:
                    this.setValue("unknown");
            }
        }

        /**
         * This constructor creates a noteOffShift chooser item from the string.
         * @param string
         */
        public NoteOffShiftItem(String string) {
            super(null, string);
            switch (string.trim().toLowerCase()) {
                case "false":
                    this.setKey(OrnamentDef.TemporalSpread.NoteOffShift.False);
                    break;
                case "true":
                    this.setKey(OrnamentDef.TemporalSpread.NoteOffShift.True);
                    break;
                case "monophonic":
                    this.setKey(OrnamentDef.TemporalSpread.NoteOffShift.Monophonic);
                    break;
                default:
                    this.setKey(null);
            }
        }

        /**
         * All combobox items require this method. The overwrite here makes sure that the string being returned
         * is the value's name instead of some Java Object ID.
         * @return
         */
        @Override
        public String toString() {
            return this.getValue();
        }
    }

    /**
     * Items for the time.unit chooser.
     * @author Axel Berndt
     */
    private static class TimeDomainItem extends KeyValue<OrnamentDef.TemporalSpread.FrameDomain, String> {
        /**
         * This constructor creates a time.unit chooser item (frame domain, String) pair out of a non-null frame domain.
         * @param frameDomain
         */
        public TimeDomainItem(@NotNull OrnamentDef.TemporalSpread.FrameDomain frameDomain) {
            super(frameDomain, "");
            switch (frameDomain) {
                case Ticks:
                    this.setValue("ticks");
                    break;
                case Milliseconds:
                    this.setValue("milliseconds");
                    break;
                default:
                    this.setValue("unknown");
            }
        }

        /**
         * This constructor creates a time domain chooser item from the string.
         * @param string
         */
        public TimeDomainItem(String string) {
            super(null, string);
            switch (string.trim().toLowerCase()) {
                case "ticks":
                    this.setKey(OrnamentDef.TemporalSpread.FrameDomain.Ticks);
                    break;
                case "milliseconds":
                    this.setKey(OrnamentDef.TemporalSpread.FrameDomain.Milliseconds);
                    break;
                default:
                    this.setKey(null);
            }
        }

        /**
         * All combobox items require this method. The overwrite here makes sure that the string being returned
         * is the value's name instead of some Java Object ID.
         * @return
         */
        @Override
        public String toString() {
            return this.getValue();
        }
    }
}
