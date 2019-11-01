package it.amonshore.comikkua;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import androidx.annotation.NonNull;

public class Utility {

    public static List<Long> toList(@NonNull Iterable<Long> iterable) {
        final ArrayList<Long> list = new ArrayList<>();
        for (Long id : iterable) {
            list.add(id);
        }
        return list;
    }

    public static Long[] toArray(@NonNull Iterable<Long> iterable) {
        return toList(iterable).toArray(new Long[0]);
    }

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
