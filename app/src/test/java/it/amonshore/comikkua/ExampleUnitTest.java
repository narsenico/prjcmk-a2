package it.amonshore.comikkua;

import org.junit.Test;

import java.text.NumberFormat;
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
}