package it.amonshore.comikkua;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;

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

    /**
     * Nasconde la tasteriera.
     * <p>
     * Da una activity può essere chiamata con {@link Activity#getWindow()} => {@link Window#getDecorView()}
     *
     * @param view vista di riferimento per recuperare {@link View#getWindowToken()}
     */
    public static void hideKeyboard(@NonNull View view) {
        final InputMethodManager imm = (InputMethodManager) view.getContext().getSystemService(Activity.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }
}
