package it.amonshore.comikkua.data.release;

import android.content.Context;

import java.util.Arrays;
import java.util.List;

import androidx.annotation.NonNull;
import it.amonshore.comikkua.R;

public class Periodicity {

    @NonNull
    public String key;
    @NonNull
    public String text;

    public Periodicity(@NonNull String key, @NonNull String text) {
        this.key = key;
        this.text = text;
    }

    @NonNull
    @Override
    public String toString() {
        return this.text;
    }

    @NonNull
    public static Periodicity[] createArray(@NonNull Context context) {
        final String[] keys = context.getResources().getStringArray(R.array.comics_periodicity_keys);
        final String[] entries = context.getResources().getStringArray(R.array.comics_periodicity_entries);
        final int length = Math.min(keys.length, entries.length);
        final Periodicity[] array = new Periodicity[length];
        for (int ii = 0; ii<length; ii++) {
            array[ii] = new Periodicity(keys[ii], entries[ii]);
        }
        return array;
    }

    @NonNull
    public static List<Periodicity> createList(@NonNull Context context) {
        return Arrays.asList(createArray(context));
    }

    public static int getIndexByKey(@NonNull List<Periodicity> list, String key) {
        if (key == null) return -1;
        for (int ii = 0; ii<list.size(); ii++) {
            if (list.get(ii).key.equals(key)) {
                return ii;
            }
        }
        return 0; // se non trovo ritorno 0: none/irregular
    }
}
