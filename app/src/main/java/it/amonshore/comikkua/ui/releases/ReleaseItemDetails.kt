package it.amonshore.comikkua.ui.releases

import androidx.recyclerview.selection.ItemDetailsLookup.ItemDetails

data class ReleaseItemDetails(
    private val position: Int,
    private val selectionKey: Long
) : ItemDetails<Long>() {

    override fun getPosition(): Int {
        return position
    }

    override fun getSelectionKey(): Long {
        return selectionKey
    }
}