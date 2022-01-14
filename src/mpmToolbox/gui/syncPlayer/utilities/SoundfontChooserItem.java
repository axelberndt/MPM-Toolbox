package mpmToolbox.gui.syncPlayer.utilities;

import com.alee.api.annotations.NotNull;
import meico.supplementary.KeyValue;

import java.io.File;

/**
 * This class represents an item in the soundfont chooser combobox of the SyncPlayer.
 * @author Axel Berndt
 */
public class SoundfontChooserItem extends KeyValue<String, File> {
    /**
     * This constructor creates a soundfont chooser item (String, File) pair out of a non-null file.
     * @param soundfont
     */
    public SoundfontChooserItem(@NotNull File soundfont) {
        super(soundfont.getName(), soundfont);
    }

    /**
     * This constructor creates a soundfont chooser item with the specified name key but null soundfont.
     * Basically, this is used to communicate to the SyncPlayer to use the default soundfont.
     * @param string
     */
    public SoundfontChooserItem(String string) {
        super(string, null);
    }

    /**
     * All combobox items require this method. The overwrite here makes sure that the string being returned
     * is the file name instead of some Java Object ID.
     * @return
     */
    @Override
    public String toString() {
        return this.getKey();
    }
}
