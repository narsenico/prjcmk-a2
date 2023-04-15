package it.amonshore.comikkua.ui.comics.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import it.amonshore.comikkua.data.web.AvailableComics

class AvailableComicsAdapter private constructor() :
    ListAdapter<AvailableComics, AvailableComicsViewHolder>(diffCallback) {

    private lateinit var _onAvailableComicsFollow: OnAvailableComicsFollow
    private lateinit var _onAvailableComicsMenuClick: OnAvailableComicsMenuClick

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AvailableComicsViewHolder {
        return AvailableComicsViewHolder.create(LayoutInflater.from(parent.context), parent)
    }

    override fun onBindViewHolder(holder: AvailableComicsViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(
            comics = item,
            onAvailableComicsFollow = _onAvailableComicsFollow,
            onAvailableComicsMenuClick = _onAvailableComicsMenuClick
        )
    }

    companion object {
        fun create(
            recyclerView: RecyclerView,
            onAvailableComicsFollow: OnAvailableComicsFollow,
            onAvailableComicsMenuClick: OnAvailableComicsMenuClick = { },
        ): AvailableComicsAdapter {
            val adapter = AvailableComicsAdapter()
            recyclerView.adapter = adapter

            adapter._onAvailableComicsFollow = onAvailableComicsFollow
            adapter._onAvailableComicsMenuClick = onAvailableComicsMenuClick

            return adapter
        }
    }
}

private val diffCallback = object : DiffUtil.ItemCallback<AvailableComics>() {
    override fun areItemsTheSame(
        oldItem: AvailableComics,
        newItem: AvailableComics
    ): Boolean {
        return oldItem.sourceId == newItem.sourceId
    }

    override fun areContentsTheSame(
        oldItem: AvailableComics,
        newItem: AvailableComics
    ): Boolean {
        return oldItem == newItem
    }
}