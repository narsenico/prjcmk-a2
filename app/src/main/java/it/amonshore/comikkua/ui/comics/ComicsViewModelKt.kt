package it.amonshore.comikkua.ui.comics

import android.app.Application
import android.os.Bundle
import androidx.lifecycle.*
import androidx.paging.*
import it.amonshore.comikkua.LogHelper
import it.amonshore.comikkua.data.comics.ComicsRepositoryKt
import it.amonshore.comikkua.data.comics.ComicsWithReleases
import it.amonshore.comikkua.ui.SingleLiveEvent
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

private const val FILTER_DEBOUNCE = 300L;
private val SPACE_REGEX = "\\s+".toRegex()

sealed class UiComicsEvent {
    data class MarkedAsRemoved(val comicsIds: List<Long>, val count: Int) : UiComicsEvent()
}

@OptIn(FlowPreview::class)
class ComicsViewModelKt(application: Application) : AndroidViewModel(application) {

    private val _repository = ComicsRepositoryKt(application)
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
                        LogHelper.d("get paged comics with filter=$filter")
                        _repository.getComicsWithReleasesPagingSource(filter)
                    }
                ).liveData
            }
            .cachedIn(viewModelScope)
    }

    fun useLastFilter() {
        filter = _lastFilter
    }

    fun deleteRemoved() = viewModelScope.launch {
        _repository.deleteRemoved()
    }

    fun undoRemoved() = viewModelScope.launch {
        _repository.undoRemoved()
    }

    /**
     * Notifca l'avvenuta operazione inviando [UiComicsEvent.MarkedAsRemoved] a [ComicsViewModelKt.events].
     */
    fun markAsRemoved(comicsIds: List<Long>) = viewModelScope.launch {
        // prima elimino eventuali comics ancora in fase di undo
        _repository.deleteRemoved()
        val count = _repository.setRemoved(comicsIds)
        _events.postValue(UiComicsEvent.MarkedAsRemoved(comicsIds, count))
    }

    private fun String.toLikeOrNull(): String? {
        return if (isBlank()) {
            null
        } else {
            "%" + this.replace(SPACE_REGEX, "%") + "%"
        }
    }
}