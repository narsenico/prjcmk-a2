package it.amonshore.comikkua.ui.releases.adapter

import it.amonshore.comikkua.data.release.IReleaseViewModelItem

interface IReleaseViewHolderCallback {
    fun onReleaseClick(item: IReleaseViewModelItem, position: Int)
    fun onReleaseMenuSelected(item: IReleaseViewModelItem, position: Int)
}