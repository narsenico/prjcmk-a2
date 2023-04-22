package it.amonshore.comikkua.ui.comics

import android.app.Application
import androidx.lifecycle.*
import it.amonshore.comikkua.LogHelper
import it.amonshore.comikkua.containsAll
import it.amonshore.comikkua.data.comics.ComicsRepository
import it.amonshore.comikkua.data.toComics
import it.amonshore.comikkua.data.web.AvailableComics
import it.amonshore.comikkua.data.web.CmkWebRepository
import it.amonshore.comikkua.splitToWords
import it.amonshore.comikkua.ui.SingleLiveEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
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
    private val _events = SingleLiveEvent<UiComicsSelectorEvent>()
    private val _filterFlow = MutableStateFlow("")

    val events: LiveData<UiComicsSelectorEvent> = _events

    var filter: String
        get() = _filterFlow.value
        set(value) {
            _filterFlow.value = value
        }

    @OptIn(FlowPreview::class)
    private val comicsFlow = _cmkWebRepository.getNotFollowedComics()
        .debounce(READ_DEBOUNCE)

    @OptIn(FlowPreview::class)
    val filteredNotFollowedComics: LiveData<List<AvailableComics>> =
        _filterFlow
            .map { it.trim() }
            .distinctUntilChanged()
            .debounce(FILTER_DEBOUNCE)
            .map { it.splitToWords() }
            .combine(comicsFlow) { filter, comicsList ->
                LogHelper.d { "Filtering available comics by $filter over ${comicsList.size} comics" }
                if (filter.isEmpty()) {
                    comicsList
                } else {
                    comicsList.filter { comics -> comics.searchableName.containsAll(filter) }
                }
            }
            .asLiveData(viewModelScope.coroutineContext)

    fun followComics(comics: AvailableComics) = viewModelScope.launch {
        _comicsRepository.insert(comics.toComics())
    }

    fun deleteAvailableComics() = viewModelScope.launch {
        _cmkWebRepository.deleteAvailableComics()
    }

    fun loadAvailableComics() = viewModelScope.launch(Dispatchers.IO) {
        _events.postValue(UiComicsSelectorEvent.AvailableComicsLoading)

        val result = _cmkWebRepository.refreshAvailableComics()
        val event = result.map { UiComicsSelectorEvent.AvailableComicsLoaded(it) }
            .getOrElse { UiComicsSelectorEvent.AvailableComicsError }
        _events.postValue(event)
    }
}