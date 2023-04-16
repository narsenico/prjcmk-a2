package it.amonshore.comikkua;

import java.util.Objects;

import androidx.annotation.NonNull;

public class Utility {

    public static <T> int indexOf(@NonNull T[] array, T value) {
        if (array.length == 0) return -1;

        for (int ii = 0; ii < array.length; ii++) {
            final T entry = array[ii];
            if (Objects.equals(entry, value)) {
                return ii;
            }
        }
        return -1;
    }
}
