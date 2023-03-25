package it.amonshore.comikkua.data.release

import android.app.Application
import androidx.lifecycle.*
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

    fun insertReleases(releases: List<Release>, callback: () -> Unit) = viewModelScope.launch {
        _repository.insertReleases(releases)
        callback()
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

    fun getComicsReleases(ids: List<Long>, callback: (List<ComicsRelease>) -> Unit) =
        viewModelScope.launch {
            val list = _repository.getComicsReleases(ids)
            callback(list)
        }

    fun refreshWithNewReleases(comics: ComicsWithReleases, callback: (Result<Int>) -> Unit) =
        viewModelScope.launch {
            val result = try {
                val count = _repository.refreshWithNewReleases(comics)
                Result.success(count)
            } catch (ex: Exception) {
                Result.failure(ex)
            }
            callback(result)
        }

    fun getPreferredRelease(comics: ComicsWithReleases, id: Long) = liveData {
        val release = if (id == Release.NEW_RELEASE_ID) {
            comics.createNextRelease()
        } else {
            _repository.getRelease(id)
        }
        emit(release)
    }
}