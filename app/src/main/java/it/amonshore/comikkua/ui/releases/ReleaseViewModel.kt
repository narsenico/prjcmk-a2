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
import java.util.*

sealed class UiReleaseEvent {
    data class Sharing(val releases: List<ComicsRelease>) : UiReleaseEvent()
    data class MarkedAsRemoved(val count: Int, val tag: String) : UiReleaseEvent()
    data class NewReleasesLoaded(val count: Int, val tag: String) : UiReleaseEvent()
    object NewReleasesError : UiReleaseEvent()
}

class ReleaseViewModel(application: Application) : AndroidViewModel(application) {

    private val _releaseRepository = ReleaseRepository(application)
    private val _events = SingleLiveEvent<UiReleaseEvent>()

    val states = Bundle()
    val events: LiveData<UiReleaseEvent> = _events

    val notableReleaseItems: LiveData<List<IReleaseViewModelItem>> =
        _releaseRepository.getNotableComicsReleasesFlow()
            .map { releases -> releases.toReleaseViewModelItems(ComicsReleaseJoinType.MissingReleases) }
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

    fun markAsRemoved(ids: List<Long>) = viewModelScope.launch {
        val tag = UUID.randomUUID().toString()
        val count = _releaseRepository.markedAsRemoved(ids, tag)
        _events.postValue(UiReleaseEvent.MarkedAsRemoved(count, tag))
    }

    fun getShareableComicsReleases(releaseIds: List<Long>) = viewModelScope.launch {
        val list = _releaseRepository.getComicsReleases(releaseIds)
        _events.postValue(UiReleaseEvent.Sharing(list))
    }

    fun loadNewReleases() = viewModelScope.launch {
        val result = _releaseRepository.loadNewReleases()
        val event = result.map { UiReleaseEvent.NewReleasesLoaded(it.count, it.tag) }
            .getOrElse { UiReleaseEvent.NewReleasesError }
        _events.postValue(event)
    }
}