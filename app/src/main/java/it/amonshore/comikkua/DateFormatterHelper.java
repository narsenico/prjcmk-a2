package it.amonshore.comikkua;

import android.content.Context;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Size;

public class DateFormatterHelper {

    private static DateFormat parser = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
    private static DateFormat fullFormatter = SimpleDateFormat.getDateInstance(DateFormat.FULL, Locale.getDefault());
    private static DateFormat shortFormatter = new SimpleDateFormat("EEE dd MMM", Locale.getDefault());

    @IntDef({STYLE_FULL, STYLE_SHORT})
    @Retention(RetentionPolicy.SOURCE)
    public @interface Style {}
    public final static int STYLE_FULL = DateFormat.FULL;
    public final static int STYLE_SHORT = DateFormat.SHORT;

    /**
     * Ritorna "oggi", "domani" oppure la data formattata.
     *
     * @param context contesto
     * @param date    data nel formato yyyyMMdd
     * @return data formattata
     */
    public static String toHumanReadable(@NonNull Context context, @NonNull @Size(6) String date, @Style int style) {
        try {
            final Calendar check = Calendar.getInstance(Locale.getDefault());
            check.setTime(parser.parse(date));
            final Calendar ref = Calendar.getInstance(Locale.getDefault());
            if (isSameDay(check, ref)) {
                return context.getString(R.string.today);
            } else {
                ref.add(Calendar.DAY_OF_MONTH, 1);
                if (isSameDay(check, ref)) {
                    return context.getString(R.string.tomorrow);
                } else {
                    if (style == STYLE_FULL) {
                        return fullFormatter.format(check.getTime());
                    } else {
                        return shortFormatter.format(check.getTime());
                    }
                }
            }
        } catch (ParseException pex) {
            return date;
        }
    }

    private static boolean isSameDay(Calendar c1, Calendar c2) {
        return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) &&
                c1.get(Calendar.MONTH) == c2.get(Calendar.MONTH) &&
                c1.get(Calendar.DAY_OF_MONTH) == c2.get(Calendar.DAY_OF_MONTH);
    }
}
