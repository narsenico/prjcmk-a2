package it.amonshore.comikkua.ui.releases.adapter

import android.view.View
import com.bumptech.glide.RequestManager
import it.amonshore.comikkua.data.release.IReleaseViewModelItem
import it.amonshore.comikkua.ui.IViewHolderWithDetails

abstract class AReleaseViewModelItemViewHolder(itemView: View) :
    IViewHolderWithDetails<Long>(itemView) {

    abstract fun bind(
        item: IReleaseViewModelItem,
        selected: Boolean = false,
        glide: RequestManager? = null,
        onReleaseClick: OnReleaseClick? = null,
        onReleaseMenuClick: OnReleaseMenuClick? = null
    )

    abstract fun clear()
}