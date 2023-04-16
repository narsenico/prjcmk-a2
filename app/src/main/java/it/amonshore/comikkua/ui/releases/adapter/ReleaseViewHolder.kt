package it.amonshore.comikkua.ui.releases.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.net.toUri
import androidx.recyclerview.selection.ItemDetailsLookup.ItemDetails
import com.bumptech.glide.RequestManager
import it.amonshore.comikkua.R
import it.amonshore.comikkua.data.release.ComicsRelease
import it.amonshore.comikkua.data.release.IReleaseViewModelItem
import it.amonshore.comikkua.data.release.info
import it.amonshore.comikkua.data.release.notes
import it.amonshore.comikkua.data.release.toHumanReadableDate
import it.amonshore.comikkua.data.release.toNumbersString
import it.amonshore.comikkua.databinding.ListitemReleaseBinding
import it.amonshore.comikkua.ui.DrawableTextViewTarget
import it.amonshore.comikkua.ui.ImageHelperKt.Companion.getInstance

class ReleaseViewHolder private constructor(val binding: ListitemReleaseBinding) :
    AReleaseViewModelItemViewHolder(binding.root) {

    private val _initialMainCardElevation: Float = binding.releaseMainCard.elevation
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
        itemView.isActivated = selected

        val release = item as ComicsRelease
        if (onReleaseClick != null) {
            itemView.setOnClickListener {
                onReleaseClick(release)
            }
        } else {
            itemView.setOnClickListener(null)
        }

        if (onReleaseMenuClick != null) {
            with(binding.imgReleaseMenu) {
                visibility = View.VISIBLE
                setOnClickListener {
                    onReleaseMenuClick(release)
                }
            }
        } else {
            with(binding.imgReleaseMenu) {
                visibility = View.INVISIBLE
                setOnClickListener(null)
            }
        }

        binding.txtReleaseNumbers.text = release.toNumbersString()
        binding.txtReleaseDate.text = release.toHumanReadableDate(itemView.context)
        binding.txtReleaseTitle.text = release.comics.name
        binding.txtReleaseInfo.text = release.info()
        binding.txtReleaseNotes.text = release.notes()
        binding.imgReleaseOrdered.visibility =
            if (release.release.ordered) View.VISIBLE else View.INVISIBLE
        if (release.release.purchased) {
            binding.imgReleasePurchased.visibility = View.VISIBLE
            binding.releaseMainCard.elevation = 0f
            binding.releaseBackground.setBackgroundColor(itemView.context.getColor(R.color.colorItemPurchased))
        } else {
            binding.imgReleasePurchased.visibility = View.INVISIBLE
            binding.releaseMainCard.elevation = _initialMainCardElevation
            binding.releaseBackground.setBackgroundColor(itemView.context.getColor(R.color.colorItemNotPurchased))
        }
        if (glide != null && release.comics.image != null) {
            glide
                .load(release.comics.image.toUri())
                .apply(getInstance(itemView.context).squareOptions)
                .into(DrawableTextViewTarget(binding.txtReleaseNumbers))
        } else {
            binding.txtReleaseNumbers.setBackgroundColor(itemView.context.getColor(R.color.colorItemBackgroundAlt))
        }
    }

    override fun clear() {
        _item = null
        itemView.isActivated = false
        binding.txtReleaseNumbers.text = ""
        binding.txtReleaseDate.text = ""
        binding.txtReleaseTitle.text = ""
        binding.txtReleaseInfo.text = ""
        binding.txtReleaseNotes.text = ""
        binding.imgReleasePurchased.visibility = View.INVISIBLE
        binding.imgReleaseOrdered.visibility = View.INVISIBLE
    }

    companion object {

        @JvmStatic
        fun create(
            inflater: LayoutInflater,
            parent: ViewGroup
        ): ReleaseViewHolder {
            val binding = ListitemReleaseBinding.inflate(inflater, parent, false)
            return ReleaseViewHolder(binding)
        }
    }
}