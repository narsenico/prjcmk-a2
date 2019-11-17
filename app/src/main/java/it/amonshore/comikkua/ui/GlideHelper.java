package it.amonshore.comikkua.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.ColorDrawable;

import com.bumptech.glide.load.MultiTransformation;
import com.bumptech.glide.request.RequestOptions;

import androidx.annotation.NonNull;
import it.amonshore.comikkua.R;
import jp.wasabeef.glide.transformations.BlurTransformation;
import jp.wasabeef.glide.transformations.CropCircleWithBorderTransformation;

public class GlideHelper {

    private static RequestOptions mSquareOptions;
    private static RequestOptions mCircleOptions;

    private final static int BLUR_RADIUS = 2;

    /**
     * Prepara le opzioni per Glide.
     * Si possono recuperare con {@link GlideHelper#getCircleOptions()} e {@link GlideHelper#getSquareOptions()}.
     *
     * @param context contesto
     */
    public static void prepareOptions(@NonNull Context context) {
        final BlurTransformation blurTransformation = new BlurTransformation(BLUR_RADIUS);
        final MultiTransformation<Bitmap> multiTransformation = new MultiTransformation<>(
                blurTransformation,
//                new ColorFilterTransformationEx(context.getResources().getColor(R.color.colorText), PorterDuff.Mode.ADD),
                new CropCircleWithBorderTransformation(4,
                        context.getResources().getColor(R.color.colorBackground))
        );
        final ColorDrawable backgroundColorDrawable = new ColorDrawable(context.getResources().getColor(R.color.colorItemBackgroundAlt));

        mSquareOptions = new RequestOptions()
                .override(100)
                .transform(blurTransformation)
                .placeholder(backgroundColorDrawable)
                .error(backgroundColorDrawable);

        mCircleOptions = new RequestOptions()
                .override(100)
                .apply(RequestOptions.bitmapTransform(multiTransformation))
                .placeholder(R.drawable.background_comics_initial)
                .error(R.drawable.background_comics_initial);
    }

    /**
     * Opzioni per una immagine rotonda.
     *
     * @return opzioni per Glide
     */
    public static RequestOptions getCircleOptions() {
        return mCircleOptions;
    }

    /**
     * Opzioni per una immagine quadrata.
     *
     * @return opzioni per Glide
     */
    public static RequestOptions getSquareOptions() {
        return mSquareOptions;
    }
}
