package it.amonshore.comikkua;

import org.junit.Test;

import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;
import java.time.temporal.IsoFields;
import java.time.temporal.TemporalField;
import java.time.temporal.WeekFields;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import static org.junit.Assert.*;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {
    @Test
    public void addition_isCorrect() {
        assertEquals(4, 2 + 2);
    }

    @Test
    public void parseDouble() throws Exception {
        NumberFormat format = NumberFormat.getInstance(Locale.getDefault());
        double d = 0d;
        String s = format.format(d);
        double d1 = format.parse(s).doubleValue();
        assertEquals(d, d1, 0.001d);
    }

    @Test
    public void firstDayOfWeek() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
        LocalDateTime dt = LocalDateTime.now();
        TemporalField field = WeekFields.of(Locale.getDefault()).dayOfWeek();
        dt = dt.with(field, 1);

        String s = dt.format(formatter);
        System.out.println("first day of week " + s);

        assertTrue(String.format("%s != %s", s, "20191028"), s.equals("20191028"));
    }

    @Test
    public void firstDayOfWeekWithCalendar() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        Calendar calendar = Calendar.getInstance(Locale.getDefault());
        calendar.set(Calendar.DAY_OF_WEEK, calendar.getFirstDayOfWeek());

        String s = sdf.format(calendar.getTime());
        System.out.println("first day of week " + s);

        assertTrue(String.format("%s != %s", s, "20191028"), s.equals("20191028"));
    }

    @Test
    public void addDaysToCalendar() throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        Calendar calendar = Calendar.getInstance(Locale.getDefault());
        calendar.setTime(sdf.parse("20191028"));
        calendar.add(Calendar.DAY_OF_MONTH, 7);

        String s = sdf.format(calendar.getTime());
        System.out.println("20191028 +7 " + s);

        assertTrue(String.format("%s != %s", s, "20191104"), s.equals("20191104"));
    }
}