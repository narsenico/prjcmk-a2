package it.amonshore.comikkua.ui.releases

import android.app.Application
import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class ComicsAndRelease(val comics: ComicsWithReleases, val release: Release)

sealed class UiReleaseEditResult {
    object Inserted : UiReleaseEditResult()
}

private val Context.dataStore by preferencesDataStore(name = "settings")
private val LAST_USED_RELEASE_DATE_KEY = longPreferencesKey("last_used_release_date")

class ReleaseEditViewModel(application: Application) : AndroidViewModel(application) {

    private val _dataStore get() = getApplication<Application>().dataStore
    private val _comicsRepository = ComicsRepository(application)
    private val _releaseRepository = ReleaseRepository(application)
    private val _result = MutableLiveData<UiReleaseEditResult>()

    val result: LiveData<UiReleaseEditResult> = _result

    fun getComicsAndRelease(comicsId: Long, releaseId: Long) = liveData {
        val comics = _comicsRepository.getComicsWithReleases(comicsId)
        val release = if (releaseId == Release.NEW_RELEASE_ID) {
            val lastUsedReleaseDate = _dataStore.data.map { preferences ->
                    preferences[LAST_USED_RELEASE_DATE_KEY]?.let {
                        Instant.ofEpochMilli(it)
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate()
                    }
                }.first()
            comics.getNextRelease(lastUsedReleaseDate)
        } else {
            _releaseRepository.getRelease(releaseId)
        }
        emit(ComicsAndRelease(comics, release))
    }

    fun insertReleases(releases: List<Release>) = viewModelScope.launch {
        _releaseRepository.insertReleases(releases)
        _dataStore.edit {
            val lastUsedReleaseDate = releases.last().date
            if (lastUsedReleaseDate == null) {
                it.remove(LAST_USED_RELEASE_DATE_KEY)
            } else {
                it[LAST_USED_RELEASE_DATE_KEY] = lastUsedReleaseDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            }
        }
        _result.postValue(UiReleaseEditResult.Inserted)
    }

    private fun ComicsWithReleases.getNextRelease(lastUsedReleaseDate: LocalDate?): Release {
        return lastRelease?.let {
            val period = Period.from(comics.periodicity)
            val nextDate = if (period == Period.None) lastUsedReleaseDate else it.date?.plusPeriod(period)
            Release.create(comics.id, it.number + 1, nextDate)
        } ?: Release.create(comics.id, 1, lastUsedReleaseDate)
    }
}