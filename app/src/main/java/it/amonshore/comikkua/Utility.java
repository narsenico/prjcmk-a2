package it.amonshore.comikkua;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;
import android.view.View;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Objects;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

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
     * @param separator         separatore valori non sequenziali
     * @param sequenceSeparator separatore per sequenze
     * @param values            elenco valori
     * @return stringa formattata
     */
    public static StringBuffer formatInterval(@Nullable StringBuffer buffer,
                                              @NonNull String separator,
                                              @NonNull String sequenceSeparator,
                                              int... values) {
        if (buffer == null) buffer = new StringBuffer();
        if (values.length == 0) return buffer;

        int last = values[0];
        int count = 0;
        buffer.append(last);
        for (int ii = 1; ii < values.length; ii++) {
            if (values[ii] == last + 1) {
                last = values[ii];
                count++;
            } else {
                if (count > 0) {
                    buffer.append(sequenceSeparator).append(last);
                }
                last = values[ii];
                count = 0;
                buffer.append(separator).append(last);
            }
        }
        if (count > 0) {
            buffer.append(sequenceSeparator).append(last);
        }

        return buffer;
    }

    /**
     * @param str stringa da valutare
     * @return true se nulla o vuota, false altrimenti
     */
    public static boolean isNullOrEmpty(@Nullable CharSequence str) {
        return (str == null || TextUtils.getTrimmedLength(str) == 0);
    }

    /**
     * @param separator    separatore elenco
     * @param excludeEmpty se true esclude i valori nulli o vuoti
     * @param texts        i valori da concatenare
     * @return valori concatenati
     */
    public static String join(@NonNull String separator, boolean excludeEmpty, CharSequence... texts) {
        if (texts.length == 0) return "";
        StringBuilder sb = new StringBuilder();
        boolean firstTime = true;
        for (CharSequence text : texts) {
            if (excludeEmpty && isNullOrEmpty(text)) continue;

            if (firstTime) {
                firstTime = false;
            } else {
                sb.append(separator);
            }
            sb.append(text);
        }
        return sb.toString();
    }

    /**
     * Imposta il calendario con il primo giorno della settimana in cui è settato.
     *
     * @param calendar calendario
     * @return l'istanza di {@link Calendar} passata in input
     */
    public static Calendar gotoFirstDayOfWeek(@NonNull Calendar calendar) {
        calendar.set(Calendar.DAY_OF_WEEK, calendar.getFirstDayOfWeek());
        return calendar;
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
