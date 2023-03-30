package it.amonshore.comikkua.data.release

import android.app.Application
import android.os.Bundle
import androidx.lifecycle.*
import it.amonshore.comikkua.ui.SingleLiveEvent
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

sealed class UiReleaseEvent {
    data class Sharing(val releases: List<ComicsRelease>) : UiReleaseEvent()
    data class MarkedAsRemoved(val count: Int) : UiReleaseEvent()
}

class ReleaseViewModelKt(application: Application) : AndroidViewModel(application) {

    private val _releaseRepository = ReleaseRepositoryKt(application)
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
        // prima elimino eventuali release ancora in fase di undo
        _releaseRepository.deleteRemoved()
        val count = _releaseRepository.updateRemoved(ids, removed = true)
        _events.postValue(UiReleaseEvent.MarkedAsRemoved(count))
    }

    fun deleteRemoved() = viewModelScope.launch {
        _releaseRepository.deleteRemoved()
    }

    fun undoRemoved() = viewModelScope.launch {
        _releaseRepository.undoRemoved()
    }

    fun getShareableComicsReleases(releaseIds: List<Long>) = viewModelScope.launch {
        val list = _releaseRepository.getComicsReleases(releaseIds)
        _events.postValue(UiReleaseEvent.Sharing(list))
    }
}