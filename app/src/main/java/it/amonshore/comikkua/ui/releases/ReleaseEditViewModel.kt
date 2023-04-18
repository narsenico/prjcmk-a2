package it.amonshore.comikkua.ui.releases

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import it.amonshore.comikkua.Period
import it.amonshore.comikkua.data.comics.ComicsRepository
import it.amonshore.comikkua.data.comics.ComicsWithReleases
import it.amonshore.comikkua.data.release.Release
import it.amonshore.comikkua.data.release.ReleaseRepository
import it.amonshore.comikkua.plusPeriod
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
            comics.getNextRelease()
        } else {
            _releaseRepository.getRelease(releaseId)
        }
        emit(ComicsAndRelease(comics, release))
    }

    fun insertReleases(releases: List<Release>) = viewModelScope.launch {
        _releaseRepository.insertReleases(releases)
        _result.postValue(UiReleaseEditResult.Inserted)
    }

    private fun ComicsWithReleases.getNextRelease(): Release {
        return lastRelease?.let {
            val period = Period.from(comics.periodicity)
            val nextDate = it.date?.plusPeriod(period)
            Release.create(comics.id, it.number + 1, nextDate)
        } ?: Release.create(comics.id, 1)
    }
}