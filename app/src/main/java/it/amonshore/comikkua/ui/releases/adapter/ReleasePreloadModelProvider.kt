package it.amonshore.comikkua.ui.releases.adapter

import android.content.Context
import androidx.core.net.toUri
import com.bumptech.glide.ListPreloader.PreloadModelProvider
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.RequestManager
import it.amonshore.comikkua.data.release.ComicsRelease
import it.amonshore.comikkua.data.release.IReleaseViewModelItem
import it.amonshore.comikkua.ui.ImageHelperKt
import it.amonshore.comikkua.ui.createDrawableRequestListener

internal class ReleasePreloadModelProvider(
    val context: Context,
    val adapter: ReleaseAdapter,
    private val glide: RequestManager
) : PreloadModelProvider<IReleaseViewModelItem> {

    private val _imageHelperKt: ImageHelperKt = ImageHelperKt.getInstance(context)

    override fun getPreloadItems(position: Int): List<IReleaseViewModelItem> {
        if (position >= adapter.itemCount) {
            return emptyList()
        }

        val item = adapter.currentList[position]
        return if (item == null || item.itemType != ComicsRelease.ITEM_TYPE) {
            emptyList()
        } else {
            listOf(item)
        }
    }

    override fun getPreloadRequestBuilder(item: IReleaseViewModelItem): RequestBuilder<*>? {
        val comicsRelease = item as ComicsRelease
        return if (comicsRelease.comics.hasImage()) {
            glide
                .load(comicsRelease.comics.image.toUri())
                .listener(createDrawableRequestListener())
                .apply(_imageHelperKt.squareOptions)
        } else {
            null
        }
    }
}