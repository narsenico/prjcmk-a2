package it.amonshore.comikkua.data.web

import android.app.Application
import androidx.lifecycle.*
import it.amonshore.comikkua.LogHelper
import it.amonshore.comikkua.containsAll
import it.amonshore.comikkua.splitToWords
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

private const val FILTER_DEBOUNCE = 300L;

class CmkWebViewModel(application: Application) : AndroidViewModel(application) {

    private val _repository = CmkWebRepositoryKt(application)
    private val _filter = MutableLiveData<String>()

    private var _lastFilter: String = ""
    private var _filteringJob: Job? = null

    @FlowPreview
    private val _filterFlow: Flow<List<String>> = flow {
        // ogni volta che viene modificato il filtro emetto una lista con tutte le parole contenute nel filtro
        // se il filtro è vuoto emetto null
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

    /**
     * Filtro applicato ai comics letti con [getFilteredAvailableComics]
     * su [AvailableComics.name] e [AvailableComics.publisher]
     */
    var filter: String
        get() = _lastFilter
        set(value) {
            _lastFilter = value
            _filter.postValue(_lastFilter)
        }

    fun useLastFilter(): String {
        _filter.postValue(_lastFilter)
        return _lastFilter
    }

//    /**
//     * Ritorna un [PagingData] con i comics disponibii,
//     * eventualmente filtrati grazie a [CmkWebViewModelKt.filter]
//     */
//    fun getAvailableComics(): LiveData<PagingData<AvailableComics>> {
//        val currentData = _filteredAvailableComics
//        return if (currentData != null) {
//            currentData
//        } else {
//
//            // TODO: An instance of PagingSource was re-used when Pager expected to create a new
//
//            val data = Transformations.switchMap(_filter) {
//                // se il filtro non è nullo sostituisco gli spazi vuoti con "%" e racchiudo tutto tra "%"
//                val likeName = it?.let {
//                    "%${it.replace("\\s+".toRegex(), "%")}%"
//                }
//                _repository.getAvailableComicsPagingSourceLiveData(likeName)
//                        .cachedIn(viewModelScope)
//            }
//            _filteredAvailableComics = data
//            data
//        }
//    }

    /**
     * LiveData con i comics disponibili filtrati grazie alla proprietà [CmkWebViewModel.filter].
     * Se il filtro è vuoto viene emessa una lista vuota.
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
                        _repository.getAvailableComicsFlow()
                            .catch { LogHelper.e("Error reading available comics", it) }
                            .collectLatest { emit(it) }
                    } else {
                        _repository.getAvailableComicsFlow()
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
}

