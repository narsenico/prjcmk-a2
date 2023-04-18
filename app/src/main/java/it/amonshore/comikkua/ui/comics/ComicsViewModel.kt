package it.amonshore.comikkua.ui.comics

import android.app.Application
import android.os.Bundle
import androidx.lifecycle.*
import androidx.paging.*
import it.amonshore.comikkua.LogHelper
import it.amonshore.comikkua.data.comics.ComicsRepository
import it.amonshore.comikkua.data.comics.ComicsWithReleases
import it.amonshore.comikkua.ui.SingleLiveEvent
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*

private const val FILTER_DEBOUNCE = 300L
private val SPACE_REGEX = "\\s+".toRegex()

sealed class UiComicsEvent {
    data class MarkedAsRemoved(val count: Int, val tag: String) : UiComicsEvent()
}

@OptIn(FlowPreview::class)
class ComicsViewModel(application: Application) : AndroidViewModel(application) {

    private val _repository = ComicsRepository(application)
    private val _filter = MutableLiveData<String>()
    private val _events = SingleLiveEvent<UiComicsEvent>()
    private var _lastFilter: String = ""

    var filter: String
        get() = _lastFilter
        set(value) {
            _lastFilter = value
            _filter.postValue(_lastFilter)
        }

    val lastFilter: String
        get() = _lastFilter

    val events: LiveData<UiComicsEvent> = _events

    val states = Bundle()

    val comicsWithReleasesPaged: LiveData<PagingData<ComicsWithReleases>>

    init {
        val filterAsLike: LiveData<String?> = liveData {
            _filter.asFlow()
                .onStart { emit("") }
                .debounce(FILTER_DEBOUNCE)
                .onEach { _lastFilter = it }
                .map { it.toLikeOrNull() }
                .distinctUntilChanged()
                .collectLatest { emit(it) }
        }

        comicsWithReleasesPaged = filterAsLike
            .switchMap { filter ->
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
                ).liveData
            }
            .cachedIn(viewModelScope)
    }

    fun useLastFilter() {
        filter = _lastFilter
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