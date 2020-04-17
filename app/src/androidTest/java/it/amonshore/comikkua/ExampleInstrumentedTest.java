package it.amonshore.comikkua;

import android.content.Context;
import android.net.Uri;

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.nio.file.Paths;

import static org.junit.Assert.*;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class ExampleInstrumentedTest {
    @Test
    public void useAppContext() {
        // Context of the app under test.
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();

        assertEquals("it.amonshore.comikkua", appContext.getPackageName());
    }



    @Test
    public void playWithFile() {
        final Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();

        final String imgPath = "file:///data/data/it.amonshore.comikkua.neon/files/cropped-981531864.jpg";
        final Uri uri = Uri.parse(imgPath);
        assertNotNull(uri);
        LogHelper.d("uri " + uri.getPath());

        final File file = new File(uri.getPath());
        assertTrue(file.exists());
        final File dst = new File(appContext.getFilesDir(), file.getName());
        LogHelper.d("move to " + dst);
    }
}
