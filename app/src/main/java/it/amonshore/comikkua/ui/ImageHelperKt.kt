package it.amonshore.comikkua.ui

import android.content.Context
import android.graphics.PorterDuff
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.util.TypedValue
import androidx.annotation.ColorInt
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import it.amonshore.comikkua.LogHelperKt
import it.amonshore.comikkua.R
import it.amonshore.comikkua.data.comics.Comics

private val rgImageFileName = "(-?\\d+)-\\d+\\.image$".toRegex()

fun Comics.isValidImageFileName(fileName: String): Boolean = isValidImageFileName(fileName, id)

fun Comics.newImageFileName(): String = "$id-${System.currentTimeMillis()}.image"

fun isValidImageFileName(fileName: String, comicsId: Long): Boolean =
    rgImageFileName.find(fileName)?.groups?.get(1)?.value?.toLong() == comicsId

fun isValidImageFileName(fileName: String, comicsIds: List<Long>): Boolean {
    return rgImageFileName.find(fileName)?.groups?.get(1)?.value?.toLong()?.let {
        return comicsIds.contains(it)
    } ?: false
}

fun isValidImageFileName(fileName: String): Boolean = rgImageFileName.matches(fileName)

class ImageHelperKt private constructor(
    _defaultSize: Int,
    @ColorInt backgroundColor: Int,
    @ColorInt releaseImageTintColor: Int
) {

    val defaultSize: Int
    val squareOptions: RequestOptions
    val circleOptions: RequestOptions
    val drawableRequestListener: RequestListener<Drawable> = object : RequestListener<Drawable> {
        override fun onLoadFailed(
            e: GlideException?,
            model: Any?,
            target: Target<Drawable>?,
            isFirstResource: Boolean
        ): Boolean {
            LogHelperKt.e("GLIDE LOAD FAILED with url=$model", e)
            // important to return false so the error placeholder can be placed
            return false
        }

        override fun onResourceReady(
            resource: Drawable?,
            model: Any?,
            target: Target<Drawable>?,
            dataSource: DataSource?,
            isFirstResource: Boolean
        ): Boolean {
            // everything worked out, so probably nothing to do
            return false
        }
    }

    init {
        defaultSize = _defaultSize

        val backgroundColorDrawable = ColorDrawable(backgroundColor)
        val colorFilterTransformation =
            ColorFilterTransformationEx(releaseImageTintColor, PorterDuff.Mode.MULTIPLY)

        squareOptions = RequestOptions()
            .override(defaultSize)
            .transform(colorFilterTransformation)
            .placeholder(backgroundColorDrawable)
            .error(backgroundColorDrawable);

        circleOptions = RequestOptions()
            .override(defaultSize)
            .apply(RequestOptions.circleCropTransform())
            .placeholder(R.drawable.background_comics_initial_noborder)
            .error(R.drawable.background_comics_initial_noborder);
    }

    companion object {
        private var INSTANCE: ImageHelperKt? = null

        @JvmStatic
        fun getInstance(context: Context): ImageHelperKt {
            if (INSTANCE == null) {
                val defaultSize = TypedValue
                    .applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP,
                        100F,
                        context.resources.displayMetrics
                    )
                    .toInt()
                val backgroundColor = context.getColor(R.color.colorItemBackgroundAlt)
                val releaseImageTintColor = context.getColor(R.color.colorReleaseImageTint)

                INSTANCE = ImageHelperKt(defaultSize, backgroundColor, releaseImageTintColor)
            }

            return INSTANCE!!
        }
    }
}