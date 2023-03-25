package it.amonshore.comikkua.data.release

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import it.amonshore.comikkua.data.comics.ComicsWithReleases
import kotlinx.coroutines.launch

class ReleaseViewModelKt(application: Application) : AndroidViewModel(application) {

    private val _repository = ReleaseRepositoryKt(application)
    private val _releaseViewModelGroupHelper: ReleaseViewModelGroupHelper by lazy {
        ReleaseViewModelGroupHelper()
    }

    fun getReleaseViewModelItems(comicsId: Long): LiveData<List<IReleaseViewModelItem>> {
        return _repository.getComicsReleasesByComicsId(comicsId)
            .map { comicsReleases ->
                _releaseViewModelGroupHelper.createViewModelItems(
                    comicsReleases,
                    0
                )
            }
    }

    fun updatePurchased(releaseId: Long, purchased: Boolean) = viewModelScope.launch {
        _repository.updatePurchased(listOf(releaseId), purchased)
    }

    fun updateOrdered(releaseId: Long, ordered: Boolean) = viewModelScope.launch {
        _repository.updateOrdered(listOf(releaseId), ordered)
    }

    fun togglePurchased(releaseIds: List<Long>) = viewModelScope.launch {
        _repository.togglePurchased(releaseIds)
    }

    fun toggleOrdered(releaseIds: List<Long>) = viewModelScope.launch {
        _repository.toggleOrdered(releaseIds)
    }

    fun deleteRemoved() = viewModelScope.launch {
        _repository.deleteRemoved()
    }

    fun undoRemoved() = viewModelScope.launch {
        _repository.undoRemoved()
    }

    fun markAsRemoved(ids: List<Long>, callback: (Int) -> Unit) = viewModelScope.launch {
        // prima elimino eventuali release ancora in fase di undo
        _repository.deleteRemoved()
        val count = _repository.updateRemoved(ids, true)
        callback(count)
    }

    fun getComicsReleases(ids: List<Long>, callback: (List<ComicsRelease>) -> Unit) = viewModelScope.launch {
        val list = _repository.getComicsReleases(ids)
        callback(list)
    }

    fun refreshWithNewReleases(comics: ComicsWithReleases, callback: (Int) -> Unit) = viewModelScope.launch {
        val count = _repository.refreshWithNewReleases(comics)
        callback(count)
    }
}