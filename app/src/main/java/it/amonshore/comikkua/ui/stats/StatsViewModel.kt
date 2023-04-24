package it.amonshore.comikkua.ui.stats

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.liveData
import it.amonshore.comikkua.data.comics.ComicsRepository
import it.amonshore.comikkua.data.release.ReleaseRepository
import java.time.ZonedDateTime

class StatsViewModel(application: Application) : AndroidViewModel(application) {

    private val _comicsRepository = ComicsRepository(application)
    private val _releaseRepository = ReleaseRepository(application)

    val comicsCount: LiveData<Int> = liveData {
        emit(_comicsRepository.count())
    }

    val lastUpdate: LiveData<ZonedDateTime?> = liveData {
        emit(_releaseRepository.getLastUpdate())
    }
}