package it.amonshore.comikkua.ui.comics.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.selection.ItemDetailsLookup
import it.amonshore.comikkua.R
import it.amonshore.comikkua.data.release.formatVersion
import it.amonshore.comikkua.data.web.AvailableComics
import it.amonshore.comikkua.databinding.ListitemComicsAvailableBinding
import it.amonshore.comikkua.fromISO8601Date
import it.amonshore.comikkua.toHumanReadable
import it.amonshore.comikkua.ui.IViewHolderWithDetails

class AvailableComicsViewHolder private constructor(val binding: ListitemComicsAvailableBinding) :
    IViewHolderWithDetails<String>(binding.root) {

    private var _comics: AvailableComics? = null

    override val itemDetails: ItemDetailsLookup.ItemDetails<String>?
        get() = _comics?.let { ComicsItemDetails(layoutPosition, it.sourceId) }

    fun bind(
        comics: AvailableComics,
        onAvailableComicsFollow: OnAvailableComicsFollow,
        onAvailableComicsMenuClick: OnAvailableComicsMenuClick
    ) {
        _comics = comics

        with(binding) {
            btnFollow.setOnClickListener {
                onAvailableComicsFollow(comics)
            }
            imgComicsMenu.setOnClickListener {
                onAvailableComicsMenuClick(comics)
            }
            imgComicsMenu.visibility = View.INVISIBLE
            txtComicsName.text = comics.name
            txtComicsPublisher.text = comics.publisher
            txtComicsInitial.text = comics.initial
            txtComicsInitial.setBackgroundResource(R.drawable.background_comics_initial_noborder)
            txtComicsReleaseLast.text = comics.formatLastRelease()
            txtReprint.visibility = if (comics.version > 0) View.VISIBLE else View.GONE
            txtReprint.text = comics.formatVersion(binding.root.context)
        }
    }

    fun clear() {
        _comics = null
        with(binding) {
            txtComicsInitial.text = ""
            txtComicsName.text = ""
            txtComicsPublisher.text = ""
            txtComicsAuthors.text = ""
            txtComicsNotes.text = ""
            txtComicsReleaseLast.text = ""
        }
    }

    private fun AvailableComics.formatLastRelease(): String {
        val context = binding.root.context
        val lastReleaseDateHumanReadable =
            lastReleaseDate?.fromISO8601Date()?.toLocalDate()?.toHumanReadable(context)

        return when {
            lastReleaseDateHumanReadable != null && lastNumber != null ->
                context.getString(
                    R.string.release_last_dated,
                    lastNumber,
                    lastReleaseDateHumanReadable
                )

            lastNumber != null ->
                context.getString(R.string.release_last, lastNumber)

            else -> ""
        }
    }

    companion object {

        @JvmStatic
        fun create(
            inflater: LayoutInflater,
            parent: ViewGroup
        ): AvailableComicsViewHolder {
            val binding = ListitemComicsAvailableBinding.inflate(inflater, parent, false)
            return AvailableComicsViewHolder(binding)
        }
    }
}