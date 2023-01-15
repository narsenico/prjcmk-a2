package it.amonshore.comikkua;

import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.os.Looper;
import android.text.TextUtils;
import android.view.View;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import com.google.android.material.textfield.TextInputLayout;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class Utility {

    public static boolean isMainLoop() {
        return Looper.getMainLooper() == Looper.myLooper();
    }

    public static <T> boolean isEquals(T a, T b) {
        if (a == b) {
            return true;
        } else if (a == null || b == null) {
            return false;
        } else {
            return a.equals(b);
        }
    }

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
     * @param text              testo da interpretare
     * @param separator         separatore di intervalli
     * @param sequenceSeparator separatore di sequenze
     * @return elenco ordinato di interi
     */
    public static int[] parseInterval(@NonNull String text,
                                      @NonNull String separator,
                                      @NonNull String sequenceSeparator) {
        final TreeSet<Integer> list = new TreeSet<>();
        for (String token : text.split(separator)) {
            final String[] range = token.split(sequenceSeparator);
            if (range.length == 1) {
                list.add(Integer.parseInt(range[0].trim()));
            } else if (range.length == 2) {
                final int to = Integer.parseInt(range[1].trim());
                int from = Integer.parseInt(range[0].trim());
                do {
                    list.add(from);
                } while (++from <= to);
            }
        }
        return toIntArray(list.iterator(), new int[list.size()]);
    }


    private static int[] toIntArray(@NonNull Iterator<Integer> src, int[] dst) {
        for (int ii = 0; src.hasNext(); ii++) {
            dst[ii] = src.next();
        }
        return dst;
    }

    /**
     * @param text
     * @param def
     * @return
     */
    public static String nvl(String text, String def) {
        return isNullOrEmpty(text) ? def : text;
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

    /**
     * @return true se l'external storage è accessibile in scrittura
     */
    public static boolean isExternalStorageWritable() {
        return Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState());
    }

    /**
     * Restituisce una istanza di file il cui percorso è dato dall'external storage
     * se accessibile in scrittura, altrimenti dalla cartella dell'app interna.
     *
     * @param context  contesto
     * @param fileName nome del file (senza il percorso)
     * @return una istanza di File
     */
    public static File getExternalFile(Context context, String fileName) {
        if (isExternalStorageWritable()) {
            return new File(context.getExternalFilesDir(null), fileName);
        } else {
            return new File(context.getFilesDir(), fileName);
        }
    }

    /**
     * @param folderType il tipo della cartella esterna in cui risiede il file (vedi Enviroment.DIRECTORY_xxx)
     * @param fileName   nome del file
     * @return una istanza di File
     */
    public static File getExternalFile(String folderType, String fileName) {
        return new File(Environment.getExternalStoragePublicDirectory(folderType), fileName);
    }

    public static boolean moveFile(final File srcFile, final File dstFile) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Files.move(srcFile.toPath(), dstFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                return true;
            } else {
                return srcFile.renameTo(dstFile);
            }
        } catch (IOException ex) {
            LogHelper.e("moveFile error", ex);
            return false;
        }
    }

    @NonNull
    @SuppressWarnings("unchecked")
    public static <T extends EditText> T requireTextInputLayoutEditText(@NonNull View parent, @IdRes int textInputLayoutId) {
        final TextInputLayout til = parent.findViewById(textInputLayoutId);
        if (til == null) {
            throw new IllegalStateException("ID does not reference a View inside this View");
        }
        final EditText editText = til.getEditText();
        if (editText == null) {
            throw new IllegalStateException("EditText null inside TextInputLayout");
        }
        return (T) editText;
    }
}
