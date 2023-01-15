package it.amonshore.comikkua.data.web

import android.app.Application
import android.text.TextUtils
import androidx.lifecycle.*
import it.amonshore.comikkua.LogHelper
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class CmkWebViewModelKt(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val FILTER_DEBOUNCE = 300L;
    }

    private val _repository = CmkWebRepositoryKt(application)
    private var _lastFilter: String? = null
    private val _filter = MutableLiveData<String?>()
    private val _splitRegex = "\\s+".toRegex()
    private var _filteringJob: Job? = null
//    private var _filteredAvailableComics: LiveData<PagingData<AvailableComics>>? = null

    @FlowPreview
    private val _filterFlow: Flow<List<String>?> = flow {
        // ogni volta che viene modificato il filtro emetto una lista con tutte le parole contenute nel filtro
        // se il filtro è vuoto emetto null
        _filter.asFlow()
                .debounce(FILTER_DEBOUNCE)
                .distinctUntilChanged()
                .map {
                    if (TextUtils.isEmpty(it)) {
                        null
                    } else {
                        it!!.trim().split(_splitRegex)
                    }
                }
                .collect { emit(it) }
    }

    /**
     * Filtro applicato ai comics letti con [getFilteredAvailableComics]
     * su [AvailableComics.name] e [AvailableComics.publisher]
     */
    var filter: String?
        get() = _lastFilter
        set(value) {
            _lastFilter = value
            _filter.postValue(_lastFilter)
        }

    fun useLastFilter(): String? {
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
     * LiveData con i comics disponibili filtrati grazie alla proprietà [CmkWebViewModelKt.filter].
     * Se il filtro è vuoto viene emessa una lista vuota.
     */
    @FlowPreview
    @ExperimentalCoroutinesApi
    fun getFilteredAvailableComics(): LiveData<List<AvailableComics>> = liveData {
        _filterFlow
                .onStart { emit(null) }
                .collectLatest { filter ->
                    // eventuali precedenti operazioni di filtering vengono annullate
                    _filteringJob?.cancel();
                    // eseguo il filtering in una coroutine in modo da poter annullarela,
                    // e con lei anche il FLow creato al suo interno
                    _filteringJob = viewModelScope.launch {
                        if (filter == null) {
                            emit(emptyList<AvailableComics>())
                        } else {
                            _repository.getAvailableComicsFlow()
                                    .map { data -> data.filter { comics -> containsAll(comics.searchableText, filter) } }
                                    .catch { LogHelper.e("Error filtering available comics", it) }
                                    .collectLatest { emit(it) }
                        }
                    }
                }
    }

    private fun containsAll(_this: String, values: List<String>): Boolean =
            values.find { !_this.contains(it, true) } == null

    private val AvailableComics.searchableText: String
        get() = "${this.name} ${this.publisher}"
}

