package it.amonshore.comikkua.data.web

import android.app.Application
import androidx.lifecycle.*
import it.amonshore.comikkua.LogHelper
import it.amonshore.comikkua.containsAll
import it.amonshore.comikkua.data.comics.ComicsRepositoryKt
import it.amonshore.comikkua.data.toComics
import it.amonshore.comikkua.splitToWords
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

private const val FILTER_DEBOUNCE = 300L;

class ComicsSelectorViewModel(application: Application) : AndroidViewModel(application) {

    private val _cmkWebRepository = CmkWebRepositoryKt(application)
    private val _comicsRepository = ComicsRepositoryKt(application)
    private val _filter = MutableLiveData<String>()

    private var _lastFilter: String = ""
    private var _filteringJob: Job? = null

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
    fun getFilteredAvailableComics(): LiveData<List<AvailableComics>> = liveData {
        _filterFlow
            .onStart { emit(emptyList()) }
            .collectLatest { filter ->
                LogHelper.d("Filtering available comics by $filter")
                // eventuali precedenti operazioni di filtering vengono annullate
                _filteringJob?.cancel();
                // eseguo il filtering in una coroutine in modo da poter annullare la,
                // e con lei anche il FLow creato al suo interno
                _filteringJob = viewModelScope.launch {
                    if (filter.isEmpty()) {
                        _cmkWebRepository.getAvailableComicsFlow()
                            .catch { LogHelper.e("Error reading available comics", it) }
                            .collectLatest { emit(it) }
                    } else {
                        _cmkWebRepository.getAvailableComicsFlow()
                            .map { data ->
                                data.filter { comics ->
                                    comics.searchableName.containsAll(
                                        filter
                                    )
                                }
                            }
                            .catch { LogHelper.e("Error filtering available comics", it) }
                            .collectLatest { emit(it) }
                    }
                }
            }
    }

    fun followComics(comics: AvailableComics) = viewModelScope.launch {
        _comicsRepository.insert(comics.toComics())
        // TODO: come aggiornare l'ui perché rifletta il cambiamento?
    }
}

