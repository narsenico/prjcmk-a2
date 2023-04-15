package it.amonshore.comikkua.ui.comics.adapter

import androidx.recyclerview.selection.ItemDetailsLookup.ItemDetails

class ComicsItemDetails<T>(
    private val position: Int,
    private val selectionKey: T
) :
    ItemDetails<T>() {
    override fun getPosition(): Int {
        return position
    }

    override fun getSelectionKey(): T {
        return selectionKey
    }
}