package it.amonshore.comikkua.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;

import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;

import java.security.MessageDigest;

import androidx.annotation.NonNull;
import jp.wasabeef.glide.transformations.BitmapTransformation;

public class ColorFilterTransformationEx extends BitmapTransformation {
    private static final int VERSION = 1;
    private static final String ID = ColorFilterTransformationEx.class.getCanonicalName() + VERSION;

    private int mColor;
    private PorterDuff.Mode mMode;

    public ColorFilterTransformationEx(int color, PorterDuff.Mode mode) {
        mColor = color;
        mMode = mode;
    }

    @Override
    protected Bitmap transform(@NonNull Context context, @NonNull BitmapPool pool,
                               @NonNull Bitmap toTransform, int outWidth, int outHeight) {
        int width = toTransform.getWidth();
        int height = toTransform.getHeight();

        Bitmap.Config config =
                toTransform.getConfig() != null ? toTransform.getConfig() : Bitmap.Config.ARGB_8888;
        Bitmap bitmap = pool.get(width, height, config);

        bitmap.setDensity(toTransform.getDensity());

        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setColorFilter(new PorterDuffColorFilter(mColor, mMode));
        canvas.drawBitmap(toTransform, 0, 0, paint);

        return bitmap;
    }

    @NonNull
    @Override
    public String toString() {
        return "ColorFilterTransformationEx(color=" + mColor + ", mode" + mMode + ")";
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj instanceof ColorFilterTransformationEx) {
            final ColorFilterTransformationEx other = (ColorFilterTransformationEx) obj;
            return other.mColor == mColor && other.mMode == mMode;
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return ID.hashCode() + mColor * 10 + mMode.ordinal();
    }

    @Override
    public void updateDiskCacheKey(@NonNull MessageDigest messageDigest) {
        messageDigest.update((ID + mColor + mMode.ordinal()).getBytes(CHARSET));
    }
}
