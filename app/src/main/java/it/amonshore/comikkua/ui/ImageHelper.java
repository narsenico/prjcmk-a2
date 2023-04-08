package it.amonshore.comikkua.ui;

import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;

import java.io.File;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import it.amonshore.comikkua.LogHelper;
import it.amonshore.comikkua.R;

@Deprecated
public class ImageHelper {

    // espressione regolare per il nome del file immagine <id comics>-<timestamp>.image
    private final static Pattern rgImage = Pattern.compile("(-?\\d+)-\\d+\\.image$");

    public static boolean isValidImageFileName(String fileName, long comicsId) {
        if (fileName != null) {
            final Matcher matcher = rgImage.matcher(fileName);
            if (matcher.find()) {
                return matcher.group(1).equals(Long.toString(comicsId));
            }
        }
        return false;
    }

    public static boolean isValidImageFileName(String fileName) {
        return fileName != null && rgImage.matcher(fileName).matches();
    }

    public static String newImageFileName(long comicsId) {
        return String.format("%s-%s.image", comicsId, System.currentTimeMillis());
    }

    public static void deleteImageFiles(@NonNull Context context, boolean clearCache) {
        if (clearCache) {
            // pulisco la cache di Glide e tutti i file immagine che si trovano sotto files/
            Glide.get(context).clearDiskCache();
        }
        // elimino tutti i file immagine in files/
        for (File file : context.getFilesDir().listFiles((dir, name) -> isValidImageFileName(name))) {
            if (!file.delete()) {
                LogHelper.w("Cannot delete '%s'", file);
            }
        }
    }
}
