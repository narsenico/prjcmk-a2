package it.amonshore.comikkua.ui.comics

import android.app.Application
import androidx.lifecycle.*
import it.amonshore.comikkua.LogHelperKt
import it.amonshore.comikkua.containsAll
import it.amonshore.comikkua.data.comics.ComicsRepository
import it.amonshore.comikkua.data.toComics
import it.amonshore.comikkua.data.web.AvailableComics
import it.amonshore.comikkua.data.web.CmkWebRepository
import it.amonshore.comikkua.splitToWords
import it.amonshore.comikkua.ui.SingleLiveEvent
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

private const val FILTER_DEBOUNCE = 300L
private const val READ_DEBOUNCE = 300L

sealed class UiComicsSelectorEvent {
    data class AvailableComicsLoaded(val count: Int) : UiComicsSelectorEvent()
    object AvailableComicsError : UiComicsSelectorEvent()
    object AvailableComicsLoading : UiComicsSelectorEvent()
}

class ComicsSelectorViewModel(application: Application) : AndroidViewModel(application) {

    private val _cmkWebRepository = CmkWebRepository(application)
    private val _comicsRepository = ComicsRepository(application)
    private val _filter = MutableLiveData<String>()
    private val _events = SingleLiveEvent<UiComicsSelectorEvent>()

    private var _lastFilter: String = ""
    private var _filteringJob: Job? = null
    val events: LiveData<UiComicsSelectorEvent> = _events

    var filter: String
        get() = _lastFilter
        set(value) {
            _lastFilter = value
            _filter.postValue(_lastFilter)
        }

    @FlowPreview
    private val _filterFlow: Flow<List<String>> = flow {
        _filter.asFlow()
            .debounce(FILTER_DEBOUNCE)
            .map { it.trim() }
            .distinctUntilChanged()
            .map {
                if (it.isEmpty()) {
                    emptyList()
                } else {
                    it.splitToWords()
                }
            }
            .collect { emit(it) }
    }

//    fun useLastFilter(): String {
//        _filter.postValue(_lastFilter)
//        return _lastFilter
//    }

    /**
     * LiveData con i comics disponibili filtrati grazie alla proprietà [ComicsSelectorViewModel.filter].
     */
    @OptIn(FlowPreview::class)
    fun getNotFollowedComics(): LiveData<List<AvailableComics>> = liveData {
        _filterFlow
            .onStart { emit(emptyList()) }
            .collectLatest { filter ->
                LogHelperKt.d { "Filtering available comics by $filter" }
                _filteringJob?.cancel()
                _filteringJob = viewModelScope.launch {
                    if (filter.isEmpty()) {
                        _cmkWebRepository.getNotFollowedComics()
                            .debounce(READ_DEBOUNCE)
                            .catch { LogHelperKt.e("Error reading available comics", it) }
                            .collectLatest { emit(it) }
                    } else {
                        _cmkWebRepository.getNotFollowedComics()
                            .debounce(READ_DEBOUNCE)
                            .map { data ->
                                data.filter { comics ->
                                    comics.searchableName.containsAll(
                                        filter
                                    )
                                }
                            }
                            .catch { LogHelperKt.e("Error filtering available comics", it) }
                            .collectLatest { emit(it) }
                    }
                }
            }
    }

    fun followComics(comics: AvailableComics) = viewModelScope.launch {
        _comicsRepository.insert(comics.toComics())
    }

    fun deleteAvailableComics() = viewModelScope.launch {
        _cmkWebRepository.deleteAvailableComics()
    }

    fun loadAvailableComics() = viewModelScope.launch {
        _events.postValue(UiComicsSelectorEvent.AvailableComicsLoading)

        val result = _cmkWebRepository.refreshAvailableComics()
        val event = result.map { UiComicsSelectorEvent.AvailableComicsLoaded(it) }
            .getOrElse { UiComicsSelectorEvent.AvailableComicsError }
        _events.postValue(event)
    }
}