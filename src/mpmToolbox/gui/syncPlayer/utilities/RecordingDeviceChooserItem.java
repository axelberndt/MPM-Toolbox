package mpmToolbox.gui.syncPlayer.utilities;

import meico.supplementary.KeyValue;

import javax.sound.sampled.Mixer;
import javax.sound.sampled.TargetDataLine;

/**
 * This represents a recording device entry in a combobox.
 * @author Axel Berndt
 */
public class RecordingDeviceChooserItem extends KeyValue<String, TargetDataLine> {
    /**
     * constructor
     * @param string
     * @param device
     */
    public RecordingDeviceChooserItem(String string, TargetDataLine device) {
        super(string, device);
    }

    /**
     * All combobox items require this method. The override here makes sure that the string being returned
     * is the device name instead of some Java Object ID.
     * @return
     */
    @Override
    public String toString() {
        return this.getKey();
    }
}
