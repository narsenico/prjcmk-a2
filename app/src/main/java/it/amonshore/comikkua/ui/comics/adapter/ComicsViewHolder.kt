package it.amonshore.comikkua.ui.comics.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.net.toUri
import androidx.recyclerview.selection.ItemDetailsLookup
import com.bumptech.glide.RequestManager
import it.amonshore.comikkua.R
import it.amonshore.comikkua.data.comics.Comics
import it.amonshore.comikkua.data.comics.ComicsWithReleases
import it.amonshore.comikkua.data.release.Release
import it.amonshore.comikkua.databinding.ListitemComicsBinding
import it.amonshore.comikkua.toHumanReadable
import it.amonshore.comikkua.ui.DrawableTextViewTarget
import it.amonshore.comikkua.ui.IViewHolderWithDetails
import it.amonshore.comikkua.ui.ImageHelperKt

class ComicsViewHolder private constructor(val binding: ListitemComicsBinding) :
    IViewHolderWithDetails<Long>(binding.root) {

    private var _comics: ComicsWithReleases? = null

    override val itemDetails: ItemDetailsLookup.ItemDetails<Long>?
        get() = _comics?.let { ComicsItemDetails(layoutPosition, it.comics.id) }

    fun bind(
        comics: ComicsWithReleases,
        selected: Boolean,
        glide: RequestManager?,
        onComicsClick: OnComicsClick,
        onComicsMenuClick: OnComicsMenuClick
    ) {
        _comics = comics

        with(itemView) {
            isActivated = selected
            setOnClickListener {
                onComicsClick(comics)
            }
        }

        with(binding) {
            imgComicsMenu.visibility = View.VISIBLE
            imgComicsMenu.setOnClickListener {
                onComicsMenuClick(comics)
            }

            imgSourced.visibility = if (comics.comics.isSourced) View.VISIBLE else View.GONE 

            with(comics.comics) {
                txtComicsName.text = name
                txtComicsPublisher.text = publisher
                txtComicsAuthors.text = authors
                txtComicsNotes.text = notes
            }

            val context = itemView.context
            txtComicsReleaseLast.text =
                comics.lastPurchasedRelease.getLastPurchasedReleaseText(context)
            txtComicsReleaseNext.text =
                comics.nextToPurchaseRelease.getNextToPurchaseRelease(context)
            txtComicsReleaseMissing.text =
                context.getString(R.string.release_missing, comics.notPurchasedReleaseCount)
            txtComicsInitial.setImage(glide, comics.comics)
        }
    }

    fun clear() {
        _comics = null
        itemView.isActivated = false
        with(binding) {
            txtComicsInitial.text = ""
            txtComicsName.text = ""
            txtComicsPublisher.text = ""
            txtComicsAuthors.text = ""
            txtComicsNotes.text = ""
            txtComicsReleaseLast.text = ""
            txtComicsReleaseNext.text = ""
            txtComicsReleaseMissing.text = ""
        }
    }

    private fun Release?.getLastPurchasedReleaseText(context: Context): String =
        if (this == null) {
            context.getString(R.string.release_last_none)
        } else {
            context.getString(R.string.release_last, this.number)
        }

    private fun Release?.getNextToPurchaseRelease(context: Context): String =
        if (this == null) {
            context.getString(R.string.release_next_none)
        } else if (date == null) {
            context.getString(R.string.release_next, number)
        } else {
            context.getString(
                R.string.release_next_dated,
                number,
                date.toHumanReadable(context)
            )
        }

    private fun TextView.setImage(glide: RequestManager?, comics: Comics) {
        if (glide == null || comics.image == null) {
            text = comics.initial
            setBackgroundResource(R.drawable.background_comics_initial_noborder)
        } else {
            text = ""
            glide
                .load(comics.image.toUri())
                .apply(ImageHelperKt.getInstance(context).circleOptions)
                .into(DrawableTextViewTarget(this))
        }
    }

    companion object {

        @JvmStatic
        fun create(
            inflater: LayoutInflater,
            parent: ViewGroup
        ): ComicsViewHolder {
            val binding = ListitemComicsBinding.inflate(inflater, parent, false)
            return ComicsViewHolder(binding)
        }
    }
}