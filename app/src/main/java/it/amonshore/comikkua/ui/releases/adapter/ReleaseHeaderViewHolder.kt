package it.amonshore.comikkua.ui.releases.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.selection.ItemDetailsLookup.ItemDetails
import com.bumptech.glide.RequestManager
import it.amonshore.comikkua.R
import it.amonshore.comikkua.RELEASE_DATED
import it.amonshore.comikkua.RELEASE_DATED_NEXT
import it.amonshore.comikkua.RELEASE_LOST
import it.amonshore.comikkua.RELEASE_MISSING
import it.amonshore.comikkua.RELEASE_NEW
import it.amonshore.comikkua.RELEASE_NOT_PURCHASED
import it.amonshore.comikkua.RELEASE_OTHER
import it.amonshore.comikkua.RELEASE_PURCHASED
import it.amonshore.comikkua.data.release.IReleaseViewModelItem
import it.amonshore.comikkua.data.release.ReleaseHeader
import it.amonshore.comikkua.databinding.ListitemReleaseHeaderBinding

class ReleaseHeaderViewHolder private constructor(val binding: ListitemReleaseHeaderBinding) :
    AReleaseViewModelItemViewHolder(binding.root) {

    private var _item: IReleaseViewModelItem? = null

    override val itemDetails: ItemDetails<Long>?
        get() = _item?.let { ReleaseItemDetails(layoutPosition, it.id) }

    override fun bind(
        item: IReleaseViewModelItem,
        selected: Boolean,
        glide: RequestManager?,
        onReleaseClick: OnReleaseClick?,
        onReleaseMenuClick: OnReleaseMenuClick?
    ) {
        _item = item

        val header = item as ReleaseHeader
        when (header.type) {
            RELEASE_LOST -> binding.txtTitle.setText(R.string.header_lost)
            RELEASE_MISSING -> binding.txtTitle.setText(R.string.header_missing)
            RELEASE_DATED -> binding.txtTitle.setText(R.string.header_current_period)
            RELEASE_DATED_NEXT -> binding.txtTitle.setText(R.string.header_next_period)
            RELEASE_NOT_PURCHASED -> binding.txtTitle.setText(R.string.header_not_purchased)
            RELEASE_PURCHASED -> binding.txtTitle.setText(R.string.header_purchased)
            RELEASE_NEW -> binding.txtTitle.setText(R.string.header_new_releases)
            RELEASE_OTHER -> binding.txtTitle.setText(R.string.header_other)
            else -> binding.txtTitle.setText(R.string.header_other)
        }
        binding.txtInfo.text = itemView.resources.getString(
            R.string.header_count,
            header.purchasedCount,
            header.totalCount
        )
        binding.separator.visibility = if (layoutPosition == 0) View.GONE else View.VISIBLE
    }

    override fun clear() {
        _item = null
        binding.txtTitle.text = ""
        binding.txtInfo.text = ""
    }

    companion object {
        @JvmStatic
        fun create(
            inflater: LayoutInflater,
            parent: ViewGroup
        ): ReleaseHeaderViewHolder {
            val binding = ListitemReleaseHeaderBinding.inflate(inflater, parent, false)
            return ReleaseHeaderViewHolder(binding)
        }
    }
}