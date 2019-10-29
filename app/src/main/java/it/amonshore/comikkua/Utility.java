package it.amonshore.comikkua;

import java.util.ArrayList;
import java.util.List;

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

}
