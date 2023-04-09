package it.amonshore.comikkua.ui

import android.view.View
import androidx.recyclerview.selection.ItemDetailsLookup.ItemDetails
import androidx.recyclerview.widget.RecyclerView

abstract class IViewHolderWithDetails<K>(itemView: View) : RecyclerView.ViewHolder(itemView) {
    abstract val itemDetails: ItemDetails<K>?
}