package it.amonshore.comikkua.ui.releases

import android.app.Application
import androidx.lifecycle.*
import it.amonshore.comikkua.data.comics.ComicsRepository
import it.amonshore.comikkua.data.comics.ComicsWithReleases
import it.amonshore.comikkua.data.release.Release
import it.amonshore.comikkua.data.release.ReleaseRepository
import kotlinx.coroutines.launch

data class ComicsAndRelease(val comics: ComicsWithReleases, val release: Release)

sealed class UiReleaseEditResult {
    object Inserted : UiReleaseEditResult()
}

class ReleaseEditViewModel(application: Application) : AndroidViewModel(application) {

    private val _comicsRepository = ComicsRepository(application)
    private val _releaseRepository = ReleaseRepository(application)
    private val _result = MutableLiveData<UiReleaseEditResult>()

    val result: LiveData<UiReleaseEditResult> = _result

    fun getComicsAndRelease(comicsId: Long, releaseId: Long) = liveData {
        val comics = _comicsRepository.getComicsWithReleases(comicsId)
        val release = if (releaseId == Release.NEW_RELEASE_ID) {
            comics.createNextRelease()
        } else {
            _releaseRepository.getRelease(releaseId)
        }
        emit(ComicsAndRelease(comics, release))
    }

    fun insertReleases(releases: List<Release>) = viewModelScope.launch {
        _releaseRepository.insertReleases(releases)
        _result.postValue(UiReleaseEditResult.Inserted)
    }
}