package it.amonshore.comikkua.data.web

import android.app.Application
import androidx.lifecycle.*
import it.amonshore.comikkua.LogHelper
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*

class CmkWebViewModelKt(application: Application) : AndroidViewModel(application) {

    private val _repository = CmkWebRepositoryKt(application)
    private var _lastFilter: String? = null
    private val _filter = MutableLiveData<String?>()
//    private var _filteredAvailableComics: LiveData<PagingData<AvailableComics>>? = null

    @FlowPreview
    private val _filterFlow: Flow<String?> = flow {
        _filter.asFlow()
                .debounce(250)
                // TODO: map String -> String[]
                .distinctUntilChanged()
                .collect { emit(it) }
    }

    /**
     * Filtro applicato ai comics letti con [getAvailableComics]
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
     * LiveData con i comics disponibili filtrati grazie alla proprietà [CmkWebViewModelKt.filter]
     */
    @FlowPreview
    @ExperimentalCoroutinesApi
    fun getAvailableComics(): LiveData<List<AvailableComics>> = liveData {
        _filterFlow
                .collectLatest { filter ->
                    _repository.getAvailableComicsFlow()
                            .map { data ->
                                data.filter {
                                    filter == null ||
                                            it.name.contains(filter, true) ||
                                            it.publisher.contains(filter, true)
                                }
                            }
                            .catch { ex ->
                                LogHelper.e("Error filtering available comics", ex)
                            }
                            .collectLatest { emit(it) }
                }
    }
}

