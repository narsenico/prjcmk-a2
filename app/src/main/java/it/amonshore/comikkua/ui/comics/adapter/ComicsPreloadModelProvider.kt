package it.amonshore.comikkua.ui.comics.adapter

import android.content.Context
import androidx.core.net.toUri
import com.bumptech.glide.ListPreloader.PreloadModelProvider
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.RequestManager
import it.amonshore.comikkua.data.comics.ComicsWithReleases
import it.amonshore.comikkua.ui.ImageHelperKt
import it.amonshore.comikkua.ui.createDrawableRequestListener

internal class ComicsPreloadModelProvider(
    val context: Context,
    val adapter: PagedListComicsAdapter,
    private val glide: RequestManager
) : PreloadModelProvider<ComicsWithReleases> {

    private val _imageHelperKt: ImageHelperKt = ImageHelperKt.getInstance(context)

    override fun getPreloadItems(position: Int): List<ComicsWithReleases> {
        if (position >= adapter.itemCount) {
            return emptyList()
        }

        val item = adapter.comicsAt(position)
        return if (item == null) {
            emptyList()
        } else {
            listOf(item)
        }
    }

    override fun getPreloadRequestBuilder(item: ComicsWithReleases): RequestBuilder<*>? {
        return if (item.comics.hasImage()) {
            glide
                .load(item.comics.image.toUri())
                .listener(createDrawableRequestListener())
                .apply(_imageHelperKt.circleOptions)
        } else {
            null
        }
    }
}