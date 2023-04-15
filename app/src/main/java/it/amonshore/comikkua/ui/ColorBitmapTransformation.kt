package it.amonshore.comikkua.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import androidx.annotation.ColorInt
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import jp.wasabeef.glide.transformations.BitmapTransformation
import java.security.MessageDigest

class ColorBitmapTransformation(
    @param:ColorInt private val color: Int,
    private val mode: PorterDuff.Mode
) :
    BitmapTransformation() {

    override fun transform(
        context: Context,
        pool: BitmapPool,
        toTransform: Bitmap,
        outWidth: Int,
        outHeight: Int
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