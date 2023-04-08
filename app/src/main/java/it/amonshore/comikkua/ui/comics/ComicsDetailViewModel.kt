package it.amonshore.comikkua.ui.comics

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import it.amonshore.comikkua.data.comics.ComicsRepository
import it.amonshore.comikkua.data.comics.ComicsWithReleases
import it.amonshore.comikkua.data.release.ComicsRelease
import it.amonshore.comikkua.data.release.ComicsReleaseJoinType
import it.amonshore.comikkua.data.release.ReleaseRepository
import it.amonshore.comikkua.data.release.toReleaseViewModelItems
import it.amonshore.comikkua.ui.SingleLiveEvent
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.*

sealed class UiComicsDetailEvent {
    data class MarkedAsRemoved(val count: Int, val tag: String) : UiComicsDetailEvent()
    data class Sharing(val releases: List<ComicsRelease>) : UiComicsDetailEvent()
    data class NewReleasesLoaded(val count: Int, val tag: String) : UiComicsDetailEvent()
    object NewReleasesError : UiComicsDetailEvent()
}

class ComicsDetailViewModel(application: Application) : AndroidViewModel(application) {

    private val _comicsRepository = ComicsRepository(application)
    private val _releaseRepository = ReleaseRepository(application)
    private val _events = SingleLiveEvent<UiComicsDetailEvent>()

    val events: LiveData<UiComicsDetailEvent> = _events

    fun getComicsWithReleases(comicsId: Long) =
        _comicsRepository.getComicsWithReleasesFlow(comicsId)
            .asLiveData()

    fun getReleaseViewModelItems(comicsId: Long) =
        _releaseRepository.getComicsReleasesByComicsIdFLow(comicsId)
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

    fun markAsRemoved(ids: List<Long>) = viewModelScope.launch {
        val tag = UUID.randomUUID().toString()
        val count = _releaseRepository.markedAsRemoved(ids, tag)
        _events.postValue(UiComicsDetailEvent.MarkedAsRemoved(count, tag))
    }

    fun getShareableComicsReleases(releaseIds: List<Long>) = viewModelScope.launch {
        val list = _releaseRepository.getComicsReleases(releaseIds)
        _events.postValue(UiComicsDetailEvent.Sharing(list))
    }

    fun loadNewReleases(comics: ComicsWithReleases) = viewModelScope.launch {
        val result = _releaseRepository.loadNewReleases(comics)
        val event = result.map { UiComicsDetailEvent.NewReleasesLoaded(it.count, it.tag) }
            .getOrElse { UiComicsDetailEvent.NewReleasesError }
        _events.postValue(event)
    }
}