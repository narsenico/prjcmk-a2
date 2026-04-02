package it.amonshore.comikkua.ui.comics

import android.app.Application
import android.os.Bundle
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import it.amonshore.comikkua.LogHelper
import it.amonshore.comikkua.data.comics.ComicsRepository
import it.amonshore.comikkua.data.comics.ComicsWithReleases
import it.amonshore.comikkua.ui.SingleLiveEvent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.UUID

private const val KEY_FILTER = "filter"
private const val FILTER_DEBOUNCE = 300L
private val SPACE_REGEX = "\\s+".toRegex()

sealed class UiComicsEvent {
    data class MarkedAsRemoved(val count: Int, val tag: String) : UiComicsEvent()
}

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class ComicsViewModel(
    application: Application,
    private val savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {

    private val _repository = ComicsRepository(application)

    private val _events = SingleLiveEvent<UiComicsEvent>()
    val events: LiveData<UiComicsEvent> = _events

    private val _filter = MutableStateFlow(savedStateHandle.get<String>(KEY_FILTER).orEmpty())
    val filter = _filter.asStateFlow()

    val states = Bundle()

    val comicsWithReleasesPaged: LiveData<PagingData<ComicsWithReleases>> = _filter
        .debounce(FILTER_DEBOUNCE)
        .map { it.toLikeOrNull() }
        .distinctUntilChanged()
        .flatMapLatest { filter ->
            Pager(
                config = PagingConfig(
                    pageSize = 10,
                    prefetchDistance = 10,
                    enablePlaceholders = true
                ),
                pagingSourceFactory = {
                    LogHelper.d { "get paged comics with filter=$filter" }
                    _repository.getComicsWithReleasesPagingSource(filter)
                }
            ).flow
        }
        .cachedIn(viewModelScope)
        .asLiveData()

    fun onFilterChanged(filter: String) {
        _filter.value = filter
        savedStateHandle[KEY_FILTER] = filter
    }

    /**
     * Notifca l'avvenuta operazione inviando [UiComicsEvent.MarkedAsRemoved] a [ComicsViewModel.events].
     */
    fun markAsRemoved(comicsIds: List<Long>) = viewModelScope.launch {
        val tag = UUID.randomUUID().toString()
        val count = _repository.markedAsRemoved(comicsIds, tag)
        _events.postValue(UiComicsEvent.MarkedAsRemoved(count, tag))
    }

    private fun String.toLikeOrNull(): String? {
        return if (isBlank()) {
            null
        } else {
            "%" + this.replace(SPACE_REGEX, "%") + "%"
        }
    }
}