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

public class ImageHelper {

    private static RequestOptions mSquareOptions;
    private static RequestOptions mCircleOptions;
    private static int mDefaultSize;

    private final static int BLUR_RADIUS = 4;

    /**
     * Prepara le opzioni per Glide.
     * Si possono recuperare con {@link ImageHelper#getGlideCircleOptions()} e {@link ImageHelper#getGlideSquareOptions()}.
     *
     * @param context contesto
     */
    public static void prepareGlideOptions(@NonNull Context context) {
        final int size = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                100,
                context.getResources().getDisplayMetrics());

//        final int borderSize = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
//                4,
//                context.getResources().getDisplayMetrics());

        final ColorFilterTransformationEx colorFilterTransformation =
                new ColorFilterTransformationEx(context.getResources().getColor(R.color.colorReleaseImageTint), PorterDuff.Mode.MULTIPLY);
//        final BlurTransformation blurTransformation = new BlurTransformation(BLUR_RADIUS);
//        final MultiTransformation<Bitmap> multiTransformation = new MultiTransformation<>(
//                blurTransformation,
////                new ColorFilterTransformationEx(context.getResources().getColor(R.color.colorText), PorterDuff.Mode.ADD),
////                new CropCircleWithBorderTransformation(borderSize,
////                        context.getResources().getColor(R.color.colorBackground))
//                new CircleCrop()
//        );
        final ColorDrawable backgroundColorDrawable = new ColorDrawable(context.getResources().getColor(R.color.colorItemBackgroundAlt));

        mSquareOptions = new RequestOptions()
                .override(size)
//                .transform(blurTransformation)
                .transform(colorFilterTransformation)
                .placeholder(backgroundColorDrawable)
                .error(backgroundColorDrawable);

        mCircleOptions = new RequestOptions()
                .override(size)
//                .apply(RequestOptions.bitmapTransform(multiTransformation))
                .apply(RequestOptions.circleCropTransform())
                .placeholder(R.drawable.background_comics_initial_noborder)
                .error(R.drawable.background_comics_initial_noborder);

        mDefaultSize = size;
    }

    /**
     * Si presume che l'immagine abbia la stessa altezza e larghezza.
     *
     * @return la dimensione di default dell'immagine in pixel
     */
    public static int getDefaultSize() {
        return mDefaultSize;
    }

    /**
     * Opzioni per una immagine rotonda.
     *
     * @return opzioni per Glide
     */
    public static RequestOptions getGlideCircleOptions() {
        return mCircleOptions;
    }

    /**
     * Opzioni per una immagine quadrata.
     *
     * @return opzioni per Glide
     */
    public static RequestOptions getGlideSquareOptions() {
        return mSquareOptions;
    }

    public static RequestListener<Drawable> drawableRequestListener = new RequestListener<Drawable>() {
        @Override
        public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
            LogHelper.e(e, "GLIDE LOAD FAILED with url=\"%s\"", model);

            // important to return false so the error placeholder can be placed
            return false;
        }

        @Override
        public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
            // everything worked out, so probably nothing to do
            return false;
        }
    };

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

    private static boolean isValidImageFileName(String fileName, Long... sortedComicsIds) {
        if (fileName != null) {
            final Matcher matcher = rgImage.matcher(fileName);
            if (matcher.find()) {
                long cid = Long.parseLong(matcher.group(1));
                return Arrays.binarySearch(sortedComicsIds, cid) >= 0;
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

    public static void deleteImageFiles(@NonNull Context context, @NonNull Long... comicsIds) {
        // mi servono gli id ordinati per poterli passare a isValidImageFileName
        Arrays.sort(comicsIds);
        // elimino i file
        for (File file : context.getFilesDir().listFiles((dir, name) -> isValidImageFileName(name, comicsIds))) {
            if (!file.delete()) {
                LogHelper.w("Cannot delete '%s'", file);
            }
        }
    }

    public static void deleteImageFiles(@NonNull Context context, boolean clearCache) {
        if (clearCache) {
            // pulisco la cache di Glide e tutti i file immagine che si trovano sotto files/
            Glide.get(context).clearDiskCache();
        }
        // elimino tutti i file immagine in files/
        for (File file : context.getFilesDir().listFiles((dir, name) -> ImageHelper.isValidImageFileName(name))) {
            if (!file.delete()) {
                LogHelper.w("Cannot delete '%s'", file);
            }
        }
    }
}
