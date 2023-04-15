package it.amonshore.comikkua.ui.releases.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.selection.ItemDetailsLookup.ItemDetails
import com.bumptech.glide.RequestManager
import it.amonshore.comikkua.Constants
import it.amonshore.comikkua.R
import it.amonshore.comikkua.data.release.DatedRelease
import it.amonshore.comikkua.data.release.IReleaseViewModelItem
import it.amonshore.comikkua.data.release.LostRelease
import it.amonshore.comikkua.data.release.MissingRelease
import it.amonshore.comikkua.data.release.NotPurchasedRelease
import it.amonshore.comikkua.data.release.PurchasedRelease
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
            LostRelease.TYPE -> binding.txtTitle.setText(R.string.header_lost)
            MissingRelease.TYPE -> binding.txtTitle.setText(R.string.header_missing)
            DatedRelease.TYPE -> binding.txtTitle.setText(R.string.header_current_period)
            DatedRelease.TYPE_NEXT -> binding.txtTitle.setText(R.string.header_next_period)
            NotPurchasedRelease.TYPE -> binding.txtTitle.setText(R.string.header_not_purchased)
            PurchasedRelease.TYPE -> binding.txtTitle.setText(R.string.header_purchased)
            Constants.RELEASE_NEW -> binding.txtTitle.setText(R.string.header_new_releases)
            DatedRelease.TYPE_OTHER -> binding.txtTitle.setText(R.string.header_other)
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