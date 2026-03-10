package it.amonshore.comikkua.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import androidx.annotation.ColorInt
import com.bumptech.glide.Glide
import com.bumptech.glide.load.Key.CHARSET
import com.bumptech.glide.load.Transformation
import com.bumptech.glide.load.engine.Resource
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import com.bumptech.glide.load.resource.bitmap.BitmapResource
import com.bumptech.glide.util.Util
import it.amonshore.comikkua.LogHelper
import java.security.MessageDigest

class ColorBitmapTransformation(
    @param:ColorInt private val color: Int,
    private val mode: PorterDuff.Mode
) : Transformation<Bitmap> {

    override fun transform(
        context: Context,
        resource: Resource<Bitmap>,
        outWidth: Int,
        outHeight: Int
    ): Resource<Bitmap> {
        if (!Util.isValidDimensions(outWidth, outHeight)) {
            LogHelper.w("Cannot apply transformation on width: $outWidth or height: $outHeight less than or equal to zero and not Target.SIZE_ORIGINAL")
            return resource
        }

        val bitmapPool = Glide.get(context).bitmapPool
        val toTransform = resource.get()
        val transformed = transform(
            bitmapPool,
            toTransform
        )

        return if (toTransform == transformed) {
            resource
        } else {
            BitmapResource.obtain(transformed, bitmapPool) ?: resource
        }
    }

    fun transform(
        pool: BitmapPool,
        toTransform: Bitmap
    ): Bitmap {
        val width = toTransform.width
        val height = toTransform.height
        val config = toTransform.config ?: Bitmap.Config.ARGB_8888
        val bitmap = pool[width, height, config].apply {
            density = toTransform.density
        }
        val paint = Paint().apply {
            isAntiAlias = true
            colorFilter = PorterDuffColorFilter(this@ColorBitmapTransformation.color, mode)
        }
        Canvas(bitmap).drawBitmap(toTransform, 0f, 0f, paint)
        return bitmap
    }

    override fun toString(): String {
        return "ColorFilterTransformationEx(color=$color, mode$mode)"
    }

    override fun equals(other: Any?): Boolean {
        if (other === this) return true
        return if (other is ColorBitmapTransformation) {
            other.color == color && other.mode == mode
        } else {
            false
        }
    }

    override fun hashCode(): Int = ID.hashCode() + color * 10 + mode.ordinal

    override fun updateDiskCacheKey(messageDigest: MessageDigest) {
        messageDigest.update((ID + color + mode.ordinal).toByteArray(CHARSET))
    }

    companion object {
        private const val VERSION = 1
        private val ID = ColorBitmapTransformation::class.java.canonicalName!! + VERSION
    }
}