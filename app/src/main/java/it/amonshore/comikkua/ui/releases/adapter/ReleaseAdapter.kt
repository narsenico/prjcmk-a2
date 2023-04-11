package it.amonshore.comikkua.ui.releases.adapter

import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ViewGroup
import androidx.recyclerview.selection.ItemDetailsLookup
import androidx.recyclerview.selection.ItemKeyProvider
import androidx.recyclerview.selection.SelectionTracker
import androidx.recyclerview.selection.StorageStrategy
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.RequestManager
import com.bumptech.glide.integration.recyclerview.RecyclerViewPreloader
import com.bumptech.glide.util.FixedPreloadSizeProvider
import it.amonshore.comikkua.R
import it.amonshore.comikkua.data.release.ComicsRelease
import it.amonshore.comikkua.data.release.IReleaseViewModelItem
import it.amonshore.comikkua.data.release.ReleaseHeader
import it.amonshore.comikkua.ui.ImageHelperKt
import java.util.*

class ReleaseAdapter private constructor(
    private val useLite: Boolean,
    private val glide: RequestManager?,
    diffCallback: DiffUtil.ItemCallback<IReleaseViewModelItem>
) : ListAdapter<IReleaseViewModelItem, AReleaseViewModelItemViewHolder>(diffCallback) {

    private var _selectionTracker: SelectionTracker<Long>? = null
    val selectionTracker: SelectionTracker<Long>
        get() = _selectionTracker!!

    private var _releaseViewHolderCallback: IReleaseViewHolderCallback? = null

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): AReleaseViewModelItemViewHolder {
        if (viewType == ReleaseHeader.ITEM_TYPE) {
            return ReleaseHeaderViewHolder.create(LayoutInflater.from(parent.context), parent)
        }

        if (useLite) {
            return ReleaseLiteViewHolder.create(LayoutInflater.from(parent.context), parent)
        }

        return ReleaseViewHolder.create(LayoutInflater.from(parent.context), parent)
    }

    override fun onBindViewHolder(holder: AReleaseViewModelItemViewHolder, position: Int) {
        val item = getItem(position)

        when (item.itemType) {
            ReleaseHeader.ITEM_TYPE -> holder.bind(item)
            else -> holder.bind(
                item,
                // TODO: questo è sbagliato, dovrebbe essere il "dato" contenuto nel ViewHolder
                //  a mantenere l'informazione se è selezionato  è meno
                selectionTracker.isSelected(item.id),
                glide,
                _releaseViewHolderCallback
            )
        }
    }

    override fun getItemViewType(position: Int): Int {
        val item = getItem(position)
        return item.itemType
    }

    fun getSelectionKey(position: Int): Long {
        val item = getItem(position)
        return item.id
    }

    fun getPosition(selectionKey: Long): Int {
        return currentList.withIndex().find { it.value.id == selectionKey }?.index
            ?: RecyclerView.NO_POSITION
    }

    companion object {
        fun create(
            recyclerView: RecyclerView,
            useLite: Boolean = false,
            onSelectionChange: (size: Int) -> Unit = { },
            onReleaseClick: (release: ComicsRelease) -> Unit = { },
            onReleaseTogglePurchase: (release: ComicsRelease) -> Unit = { },
            onReleaseToggleOrder: (release: ComicsRelease) -> Unit = { },
            onReleaseMenuClick: (release: ComicsRelease) -> Unit = { },
            glide: RequestManager? = null
        ): ReleaseAdapter {
            val adapter = ReleaseAdapter(useLite, glide, diffCallback)
            recyclerView.adapter = adapter

            adapter._selectionTracker =
                createSelectionTracker(recyclerView, adapter, onSelectionChange)
            adapter._releaseViewHolderCallback = createReleaseViewHolderCallback(
                selectionTracker = adapter.selectionTracker,
                onReleaseClick = onReleaseClick,
                onReleaseMenuClick = onReleaseMenuClick
            )

            setupSwipe(
                recyclerView = recyclerView,
                releaseAdapter = adapter,
                onReleaseTogglePurchase = onReleaseTogglePurchase,
                onReleaseToggleOrder = onReleaseToggleOrder
            )

            if (glide != null) setupImagePreloader(
                recyclerView,
                adapter,
                glide
            )

            return adapter
        }
    }
}

private fun createSelectionTracker(
    recyclerView: RecyclerView,
    adapter: ReleaseAdapter,
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

    val selectionPredicate = object : SelectionTracker.SelectionPredicate<Long>() {
        override fun canSetStateForKey(key: Long, nextState: Boolean): Boolean {
            val position = itemKeyProvider.getPosition(key)
            if (position != RecyclerView.NO_POSITION) {
                return adapter.getItemViewType(position) == ComicsRelease.ITEM_TYPE
            }

            return key < ReleaseHeader.BASE_ID
        }

        override fun canSetStateAtPosition(position: Int, nextState: Boolean): Boolean = true

        override fun canSelectMultiple(): Boolean = true
    }

    val itemDetailsLookup = object : ItemDetailsLookup<Long>() {
        override fun getItemDetails(e: MotionEvent): ItemDetails<Long>? {
            return recyclerView.findChildViewUnder(e.x, e.y)?.let {
                val holder = recyclerView.getChildViewHolder(it)
                if (holder is AReleaseViewModelItemViewHolder) {
                    holder.itemDetails
                } else {
                    null
                }
            }
        }
    }

    val selectionTracker = SelectionTracker.Builder(
        "release-selection",
        recyclerView,
        itemKeyProvider,
        itemDetailsLookup,
        StorageStrategy.createLongStorage()
    )
        .withSelectionPredicate(selectionPredicate)
        .build()

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

private fun createReleaseViewHolderCallback(
    selectionTracker: SelectionTracker<Long>,
    onReleaseClick: (release: ComicsRelease) -> Unit,
    onReleaseMenuClick: (release: ComicsRelease) -> Unit,
): IReleaseViewHolderCallback {
    return object : IReleaseViewHolderCallback {
        override fun onReleaseClick(item: IReleaseViewModelItem, position: Int) {
            // se ci sono già degli elementi selezionati il click deve essere inibito perché voglio continuare con la selezione
            if (item.itemType != ReleaseHeader.ITEM_TYPE && !selectionTracker.hasSelection()) {
                onReleaseClick(item as ComicsRelease)
            }
        }

        override fun onReleaseMenuSelected(item: IReleaseViewModelItem, position: Int) {
            if (item.itemType != ReleaseHeader.ITEM_TYPE) {
                onReleaseMenuClick(item as ComicsRelease)
            }
        }
    }
}

private fun setupSwipe(
    recyclerView: RecyclerView,
    releaseAdapter: ReleaseAdapter,
    onReleaseTogglePurchase: (release: ComicsRelease) -> Unit,
    onReleaseToggleOrder: (release: ComicsRelease) -> Unit,
) {
    val itemTouchHelperCallback =
        object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT + ItemTouchHelper.LEFT) {
            override fun isLongPressDragEnabled(): Boolean = false

            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            override fun getSwipeDirs(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder
            ): Int {
                // inibisco lo swipe per gli header e le multi release e se è in corso una selezione
                if (releaseAdapter.selectionTracker.hasSelection()) return 0
                if (releaseAdapter.getItemViewType(viewHolder.layoutPosition) != ComicsRelease.ITEM_TYPE)
                    return 0
                return super.getSwipeDirs(recyclerView, viewHolder)
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val release = releaseAdapter.currentList[viewHolder.layoutPosition] as ComicsRelease
                if (direction == ItemTouchHelper.RIGHT) {
                    onReleaseTogglePurchase(release)
                } else if (direction == ItemTouchHelper.LEFT) {
                    onReleaseToggleOrder(release)
                }
            }
        }
    ItemTouchHelper(itemTouchHelperCallback).attachToRecyclerView(recyclerView)

    recyclerView.addItemDecoration(
        SwappableItemDecoration(
            context = recyclerView.context,
            drawableLeft = R.drawable.ic_purchased,
            drawableRight = R.drawable.ic_ordered,
            drawableColor = R.color.colorPrimary,
            lineColor = R.color.colorItemBackgroundLighterX2,
            drawableLeftPadding = 0,
            drawableRightPadding = 10,
            lineHeight = 1f,
            drawableSpeed = .85f,
            leftText = R.string.swipe_purchase,
            rightText = R.string.swipe_order
        )
    )
}

private fun setupImagePreloader(
    recyclerView: RecyclerView,
    releaseAdapter: ReleaseAdapter,
    glide: RequestManager
) {
    val context = recyclerView.context
    val defaultSize = ImageHelperKt.getInstance(context).defaultSize
    val sizeProvider = FixedPreloadSizeProvider<IReleaseViewModelItem>(defaultSize, defaultSize)
    val modelProvider = ReleasePreloadModelProvider(context, releaseAdapter, glide)
    val preloader = RecyclerViewPreloader(glide, modelProvider, sizeProvider, 10)

    recyclerView.addOnScrollListener(preloader)
}

private val diffCallback = object : DiffUtil.ItemCallback<IReleaseViewModelItem>() {
    override fun areItemsTheSame(
        oldItem: IReleaseViewModelItem,
        newItem: IReleaseViewModelItem
    ): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(
        oldItem: IReleaseViewModelItem,
        newItem: IReleaseViewModelItem
    ): Boolean {
        return Objects.equals(oldItem, newItem)
    }
}