package it.amonshore.comikkua;

import android.text.TextUtils;

import java.util.Iterator;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class Utility {

    public static <T> boolean isEquals(T a, T b) {
        if (a == b) {
            return true;
        } else if (a == null || b == null) {
            return false;
        } else {
            return a.equals(b);
        }
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

    public interface ReplaceWithRegexCallback {
        @NonNull
        String replace(@NonNull Matcher matcher);
    }

    public static String replaceWithRegex(@NonNull String regex, @NonNull String input, @NonNull ReplaceWithRegexCallback callback) {
        return replaceWithRegex(Pattern.compile(regex), input, callback);
    }

    public static String replaceWithRegex(@NonNull Pattern pattern, @NonNull String input, @NonNull ReplaceWithRegexCallback callback) {
        final Matcher matcher = pattern.matcher(input);
        final StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(sb, callback.replace(matcher));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }
}
