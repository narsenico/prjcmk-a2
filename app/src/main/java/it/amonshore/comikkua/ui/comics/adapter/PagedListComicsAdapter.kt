package it.amonshore.comikkua.ui.comics.adapter

import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.selection.ItemDetailsLookup
import androidx.recyclerview.selection.ItemKeyProvider
import androidx.recyclerview.selection.SelectionTracker
import androidx.recyclerview.selection.StorageStrategy
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.RequestManager
import com.bumptech.glide.integration.recyclerview.RecyclerViewPreloader
import com.bumptech.glide.util.FixedPreloadSizeProvider
import it.amonshore.comikkua.data.comics.ComicsWithReleases
import it.amonshore.comikkua.ui.ImageHelperKt

class PagedListComicsAdapter(
    private val glide: RequestManager?,
) : PagingDataAdapter<ComicsWithReleases, ComicsViewHolder>(diffCallback) {

    lateinit var selectionTracker: SelectionTracker<Long>

    private lateinit var _onComicsClick: OnComicsClick
    private lateinit var _onComicsMenuClick: OnComicsMenuClick

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ComicsViewHolder {
        return ComicsViewHolder.create(LayoutInflater.from(parent.context), parent)
    }

    override fun onBindViewHolder(holder: ComicsViewHolder, position: Int) {
        val item = getItem(position)
        if (item == null) {
            holder.clear()
        } else {
            holder.bind(
                item,
                selectionTracker.isSelected(item.comics.id),
                glide,
                _onComicsClick,
                _onComicsMenuClick
            )
        }
    }

    fun getSelectionKey(position: Int): Long {
        val item = getItem(position)
        return item?.comics?.id ?: RecyclerView.NO_ID
    }

    fun getPosition(selectionKey: Long): Int {
        for (index in 0..itemCount) {
            val id = getItem(index)?.comics?.id
            if (id == null) {
                break
            } else if (id == selectionKey) {
                return index
            }
        }
        return RecyclerView.NO_POSITION
    }

    fun comicsAt(position: Int): ComicsWithReleases? = getItem(position)

    companion object {
        fun create(
            recyclerView: RecyclerView,
            onSelectionChange: (size: Int) -> Unit = { },
            onComicsClick: OnComicsClick = { },
            onComicsMenuClick: OnComicsMenuClick = { },
            glide: RequestManager? = null
        ): PagedListComicsAdapter {
            val adapter = PagedListComicsAdapter(glide)
            recyclerView.adapter = adapter

            adapter.selectionTracker =
                createSelectionTracker(recyclerView, adapter, onSelectionChange)
            adapter._onComicsClick = { if (!adapter.selectionTracker.hasSelection()) onComicsClick(it) }
            adapter._onComicsMenuClick = onComicsMenuClick

            glide?.applyImagePreloader(recyclerView, adapter)

            return adapter
        }
    }
}

private fun createSelectionTracker(
    recyclerView: RecyclerView,
    adapter: PagedListComicsAdapter,
    onSelectionChange: (size: Int) -> Unit
): SelectionTracker<Long> {
    val itemKeyProvider = object : ItemKeyProvider<Long>(SCOPE_MAPPED) {
        override fun getKey(position: Int): Long {
            return adapter.getSelectionKey(position)
        }

        override fun getPosition(key: Long): Int {
            return adapter.getPosition(key)
        }
    }

    val itemDetailsLookup = object : ItemDetailsLookup<Long>() {
        override fun getItemDetails(e: MotionEvent): ItemDetails<Long>? {
            return recyclerView.findChildViewUnder(e.x, e.y)?.let {
                val holder = recyclerView.getChildViewHolder(it)
                if (holder is ComicsViewHolder) {
                    holder.itemDetails
                } else {
                    null
                }
            }
        }
    }

    val selectionTracker = SelectionTracker.Builder(
        "comics-selection",
        recyclerView,
        itemKeyProvider,
        itemDetailsLookup,
        StorageStrategy.createLongStorage()
    ).build()

    selectionTracker.addObserver(object : SelectionTracker.SelectionObserver<Long>() {
        override fun onSelectionChanged() {
            if (selectionTracker.hasSelection()) {
                onSelectionChange(selectionTracker.selection.size())
            } else {
                onSelectionChange(0)
            }
        }

        override fun onSelectionRestored() {
            if (selectionTracker.hasSelection()) {
                onSelectionChange(selectionTracker.selection.size())
            } else {
                onSelectionChange(0)
            }
            super.onSelectionRestored()
        }
    })

    return selectionTracker
}

private fun RequestManager.applyImagePreloader(
    recyclerView: RecyclerView,
    releaseAdapter: PagedListComicsAdapter
) {
    val context = recyclerView.context
    val defaultSize = ImageHelperKt.getInstance(context).defaultSize
    val sizeProvider = FixedPreloadSizeProvider<ComicsWithReleases>(defaultSize, defaultSize)
    val modelProvider = ComicsPreloadModelProvider(context, releaseAdapter, this)
    val preloader = RecyclerViewPreloader(this, modelProvider, sizeProvider, 10)

    recyclerView.addOnScrollListener(preloader)
}

private val diffCallback = object : DiffUtil.ItemCallback<ComicsWithReleases>() {
    override fun areItemsTheSame(
        oldItem: ComicsWithReleases,
        newItem: ComicsWithReleases
    ): Boolean {
        return oldItem.comics.id == newItem.comics.id
    }

    override fun areContentsTheSame(
        oldItem: ComicsWithReleases,
        newItem: ComicsWithReleases
    ): Boolean {
        return oldItem == newItem
    }
}