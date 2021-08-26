package mpmToolbox.gui.syncPlayer;

import com.alee.api.annotations.NotNull;
import meico.audio.Audio;
import meico.supplementary.KeyValue;

/**
 * This class represents an item in the audio chooser combobox of the SyncPlayer.
 * @author Axel Berndt
 */
class AudioChooserItem extends KeyValue<String, mpmToolbox.projectData.Audio> {
    /**
     * This constructor creates a audio chooser item (String, Audio) pair out of a non-null audio object.
     * @param audio
     */
    public AudioChooserItem(@NotNull mpmToolbox.projectData.Audio audio) {
        super(audio.getFile().getName(), audio);
    }

    /**
     * This constructor creates a audio chooser item with the specified name key but null audio object.
     * Basically, this is used to communicate to the SyncPlayer not to play audio.
     * The string is typically something like "No audio recording".
     * @param string
     */
    public AudioChooserItem(String string) {
        super(string, null);
    }

    /**
     * All combobox items require this method. The overwrite here makes sure that the string being returned
     * is the audio file's name instead of some Java Object ID.
     * @return
     */
    @Override
    public String toString() {
        return this.getKey();
    }
}
