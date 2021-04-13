package mpmToolbox.gui.syncPlayer;

import com.alee.api.annotations.NotNull;
import meico.mpm.elements.Performance;
import meico.supplementary.KeyValue;

/**
 * This class represents an item in the performance chooser combobox of the SyncPlayer.
 * @author Axel Berndt
 */
class PerformanceChooserItem extends KeyValue<String, Performance> {
    /**
     * This constructor creates a performance chooser item (String, Performance) pair out of a non-null performance.
     * @param performance
     */
    public PerformanceChooserItem(@NotNull Performance performance) {
        super(performance.getName(), performance);
    }

    /**
     * This constructor creates a performance chooser item with the specified name key but null performance.
     * Basically, this is used to communicate to the SyncPlayer not to play a performance rendering.
     * The string is typically something like "No performance rendering".
     * @param string
     */
    public PerformanceChooserItem(String string) {
        super(string, null);
    }

    /**
     * All combobox items require this method. The overwrite here makes sure that the string being returned
     * is the performance's name instead of some Java Object ID.
     * @return
     */
    @Override
    public String toString() {
        return this.getKey();
    }
}
