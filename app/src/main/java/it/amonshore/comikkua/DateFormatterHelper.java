package it.amonshore.comikkua;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.Size;

public class DateFormatterHelper {

    private static final DateFormat parser = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());

    /**
     * Crea una nuova data (yyyyMMdd) sommando il periodo di tempo specificato da periodicity.
     * Periodicity è una stringa composta da un carattere che esprime l'unità di tempo che può essere
     * W (settimane), M (mesi) e Y (anni).
     * I restanti caratteri indicano la quandità di tempo.
     * Es: M1 => mensile, M2 => bimestrale, W1 => settimanale, etc.
     *
     * @param date        data nel formato yyyyMMdd di riferimento
     * @param periodicity periodicità di uscita del comics
     * @return la data sommata al periodo, oppure null se il parsing di date non è riuscicto
     */
    @Nullable
    public static String toNextPeriod(@NonNull @Size(8) String date, @NonNull String periodicity) {
        try {
            final Calendar cdate = Calendar.getInstance(Locale.getDefault());
            cdate.setTime(parser.parse(date));
            final char type = periodicity.charAt(0);
            final int amount = Integer.parseInt(periodicity.substring(1));
            if (type == 'W') {
                cdate.add(Calendar.DAY_OF_MONTH, 7 * amount);
            } else if (type == 'M') {
                cdate.add(Calendar.MONTH, amount);
            } else if (type == 'Y') {
                cdate.add(Calendar.YEAR, amount);
            }
            return parser.format(cdate.getTime());
        } catch (ParseException pex) {
            if (BuildConfig.DEBUG) {
                LogHelper.e(String.format("Error parsing date \"%s\"", date), pex);
            }
            return null;
        }
    }
}
