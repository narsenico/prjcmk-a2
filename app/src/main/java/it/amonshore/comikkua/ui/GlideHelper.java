package it.amonshore.comikkua.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.ColorDrawable;
import android.util.TypedValue;

import com.bumptech.glide.load.DecodeFormat;
import com.bumptech.glide.load.MultiTransformation;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.bumptech.glide.request.RequestOptions;

import androidx.annotation.NonNull;
import it.amonshore.comikkua.LogHelper;
import it.amonshore.comikkua.R;
import jp.wasabeef.glide.transformations.BlurTransformation;
import jp.wasabeef.glide.transformations.CropCircleTransformation;
import jp.wasabeef.glide.transformations.CropCircleWithBorderTransformation;

public class GlideHelper {

    private static RequestOptions mSquareOptions;
    private static RequestOptions mCircleOptions;

    private final static int BLUR_RADIUS = 4;

    /**
     * Prepara le opzioni per Glide.
     * Si possono recuperare con {@link GlideHelper#getCircleOptions()} e {@link GlideHelper#getSquareOptions()}.
     *
     * @param context contesto
     */
    public static void prepareOptions(@NonNull Context context) {
        final int size = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                100,
                context.getResources().getDisplayMetrics());

//        final int borderSize = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
//                4,
//                context.getResources().getDisplayMetrics());

        final BlurTransformation blurTransformation = new BlurTransformation(BLUR_RADIUS);
        final MultiTransformation<Bitmap> multiTransformation = new MultiTransformation<>(
                blurTransformation,
//                new ColorFilterTransformationEx(context.getResources().getColor(R.color.colorText), PorterDuff.Mode.ADD),
//                new CropCircleWithBorderTransformation(borderSize,
//                        context.getResources().getColor(R.color.colorBackground))
                new CircleCrop()
        );
        final ColorDrawable backgroundColorDrawable = new ColorDrawable(context.getResources().getColor(R.color.colorItemBackgroundAlt));

        mSquareOptions = new RequestOptions()
                .override(size)
                .transform(blurTransformation)
                .placeholder(backgroundColorDrawable)
                .error(backgroundColorDrawable);

        mCircleOptions = new RequestOptions()
                .override(size)
                .apply(RequestOptions.bitmapTransform(multiTransformation))
//                .apply(RequestOptions.circleCropTransform())
                .placeholder(R.drawable.background_comics_initial_noborder)
                .error(R.drawable.background_comics_initial_noborder);
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
