package it.amonshore.comikkua.ui.releases

import it.amonshore.comikkua.data.release.IReleaseViewModelItem

interface IReleaseViewHolderCallback {
    fun onReleaseClick(item: IReleaseViewModelItem, position: Int)
    fun onReleaseMenuSelected(item: IReleaseViewModelItem, position: Int)
}