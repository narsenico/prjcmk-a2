package it.amonshore.comikkua.ui.stats

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import it.amonshore.comikkua.data.comics.ComicsRepository
import it.amonshore.comikkua.data.release.ReleaseRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.ZonedDateTime

data class StatsUiState(
    val comicsCount: Int = 0,
    val lastUpdate: ZonedDateTime? = null,
    val isLoading: Boolean = false
)

class StatsViewModel(application: Application) : AndroidViewModel(application) {

    private val _comicsRepository = ComicsRepository(application)
    private val _releaseRepository = ReleaseRepository(application)

    private val _uiState = MutableStateFlow(StatsUiState(isLoading = true))
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            refreshState()
        }
    }

    private suspend fun refreshState() {
        val state = StatsUiState(
            comicsCount = _comicsRepository.count(),
            lastUpdate = _releaseRepository.getLastUpdate(),
            isLoading = false
        )
        _uiState.value = state
    }

    private fun loading() = _uiState.update { state -> state.copy(isLoading = true) }

    fun deleteAll() = viewModelScope.launch {
        loading()
        // TODO
//        _comicsRepository.deleteAll(deleteImages = true)
        delay(2000)
        refreshState()
    }
}