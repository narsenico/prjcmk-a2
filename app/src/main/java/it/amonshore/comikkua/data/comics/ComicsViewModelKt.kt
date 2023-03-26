package it.amonshore.comikkua.data.comics

import android.app.Application
import android.os.Bundle
import androidx.lifecycle.*
import androidx.paging.*
import it.amonshore.comikkua.LogHelper
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

private const val FILTER_DEBOUNCE = 300L;
private val SPACE_REGEX = "\\s+".toRegex()

@OptIn(FlowPreview::class)
class ComicsViewModelKt(application: Application) : AndroidViewModel(application) {

    private val _repository = ComicsRepositoryKt(application)
    private val _filter = MutableLiveData<String>()
    private var _lastFilter: String = ""

    var filter: String
        get() = _lastFilter
        set(value) {
            _lastFilter = value
            _filter.postValue(_lastFilter)
        }

    val lastFilter: String
        get() = _lastFilter

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

    fun getComicsWithReleases(id: Long) = _repository.getComicsWithReleases(id)

    fun deleteRemoved() = viewModelScope.launch {
        _repository.deleteRemoved()
    }

    fun undoRemoved() = viewModelScope.launch {
        _repository.undoRemoved()
    }

    fun markAsRemoved(ids: List<Long>, callback: (Int) -> Unit) = viewModelScope.launch {
        // prima elimino eventuali comics ancora in fase di undo
        _repository.deleteRemoved()
        val count = _repository.updateRemoved(ids, true)
        callback(count)
    }

    private fun String.toLikeOrNull(): String? {
        return if (isBlank()) {
            null
        } else {
            "%" + this.replace(SPACE_REGEX, "%") + "%"
        }
    }
}