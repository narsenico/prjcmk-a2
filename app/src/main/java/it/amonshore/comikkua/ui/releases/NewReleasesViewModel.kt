package it.amonshore.comikkua.ui.releases

import android.app.Application
import android.os.Bundle
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import it.amonshore.comikkua.data.release.*
import it.amonshore.comikkua.ui.SingleLiveEvent
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

sealed class UiNewReleasesEvent {
    data class Sharing(val releases: List<ComicsRelease>) : UiNewReleasesEvent()
    data class MarkedAsRemoved(val count: Int, val tag: String) : UiNewReleasesEvent()
}

class NewReleasesViewModel(application: Application) : AndroidViewModel(application) {

    private val _releaseRepository = ReleaseRepository(application)
    private val _events = SingleLiveEvent<UiNewReleasesEvent>()

    val states = Bundle()
    val events: LiveData<UiNewReleasesEvent> = _events

    fun getReleaseViewModelItems(tag: String): LiveData<List<IReleaseViewModelItem>> =
        _releaseRepository.getComicsReleasesByTag(tag)
            .map { releases -> releases.toReleaseViewModelItems(ComicsReleaseJoinType.None) }
            .asLiveData()

    fun updatePurchased(releaseId: Long, purchased: Boolean) = viewModelScope.launch {
        _releaseRepository.updatePurchased(listOf(releaseId), purchased)
    }

    fun updateOrdered(releaseId: Long, ordered: Boolean) = viewModelScope.launch {
        _releaseRepository.updateOrdered(listOf(releaseId), ordered)
    }

    fun togglePurchased(releaseIds: List<Long>) = viewModelScope.launch {
        _releaseRepository.togglePurchased(releaseIds)
    }

    fun toggleOrdered(releaseIds: List<Long>) = viewModelScope.launch {
        _releaseRepository.toggleOrdered(releaseIds)
    }

    fun markAsRemovedUsingTag(ids: List<Long>, tag: String) = viewModelScope.launch {
        val count = _releaseRepository.markedAsRemoved(ids, tag)
        _events.postValue(UiNewReleasesEvent.MarkedAsRemoved(count, tag))
    }

    fun getShareableComicsReleases(releaseIds: List<Long>) = viewModelScope.launch {
        val list = _releaseRepository.getComicsReleases(releaseIds)
        _events.postValue(UiNewReleasesEvent.Sharing(list))
    }
}