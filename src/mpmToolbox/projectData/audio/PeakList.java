package mpmToolbox.projectData.audio;

import com.sun.media.sound.InvalidDataException;
import meico.supplementary.KeyValue;

import java.util.ArrayList;

/**
 * an ordered list of all peaks' indexes in the related sequence of values.
 * @author Axel Berndt
 */
public class PeakList extends ArrayList<Integer> {
    /**
     * constructor
     * @param sequence the sequence of values
     */
    public PeakList(double[] sequence) throws InvalidDataException {
        if (sequence.length < 2)
            throw new InvalidDataException("To initialize a PeakList the sequence must contain at least 2 entries.");

        this.add(0);

        int lastIndex = sequence.length - 1;
        double prev = sequence[0];
        double value = sequence[1];
        double next;
        int i = 1;
        while (i < lastIndex) {
            int index = i;
            next = sequence[++i];
            if (((prev < value) && (next <= value)) || ((prev > value) && (next >= value)))
                this.add(index);
            prev = value;
            value = next;
        }

        this.add(lastIndex);
    }

    /**
     * quick binary search for the entry index with a value equal or greater than the provided value
     * @param value
     * @return index and value of entry or null if none was found
     */
    public KeyValue<Integer, Integer> ceilingEntry(int value) {
        // binary search
        int first = 0;
        int last = this.size() - 1;
        int mid = last / 2;

        while (first <= last) {
            if (this.get(mid) >= value)
                last = mid - 1;
            else if (this.get(++mid) >= value)
                return new KeyValue<>(mid, this.get(mid));
            else
                first = mid;
            mid = (first + last) / 2;
        }

        return null;
    }
}
