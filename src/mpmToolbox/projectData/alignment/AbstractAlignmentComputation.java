package mpmToolbox.projectData.alignment;

import mpmToolbox.projectData.audio.Audio;

/**
 * Abstract class for implementation of audio-to-symbolic music alignment algorithms.
 */
public abstract class AbstractAlignmentComputation {
    /**
     * This method triggers the alignment computation for the specified Audio instance.
     * The result of the computation should be encoded in the Alignment component of the Audio instance.
     * @param audio
     * @return the input audio with altered alignment component
     */
    public abstract Audio compute(Audio audio);
}
