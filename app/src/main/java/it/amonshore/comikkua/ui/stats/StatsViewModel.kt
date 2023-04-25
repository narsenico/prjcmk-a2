package it.amonshore.comikkua.ui.stats

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkInfo
import androidx.work.WorkInfo.State
import androidx.work.WorkManager
import it.amonshore.comikkua.LogHelper
import it.amonshore.comikkua.data.comics.ComicsRepository
import it.amonshore.comikkua.data.release.ReleaseRepository
import it.amonshore.comikkua.workers.ImportFromOldDatabaseWorker
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.reduce
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.ZonedDateTime

data class StatsUiState(
    val comicsCount: Int = 0,
    val lastUpdate: ZonedDateTime? = null,
    val isLoading: Boolean = false,
    val errorCode: String? = null,
) {
    val hasError = errorCode != null
}

fun StatsUiState.withWorkInfo(workInfo: WorkInfo?): StatsUiState {
    if (workInfo == null) return this

    return when (workInfo.state) {
        State.ENQUEUED, State.RUNNING -> copy(isLoading = true, errorCode = null)
        State.BLOCKED, State.FAILED -> copy(isLoading = false, errorCode = "failed")
        State.CANCELLED -> copy(isLoading = false, errorCode = "cancelled")
        State.SUCCEEDED -> copy(isLoading = false, errorCode = null)
    }
}

private const val UNIQUE_WORK_TAG: String = "MYTAG"

class StatsViewModel(application: Application) : AndroidViewModel(application) {

    private val _comicsRepository = ComicsRepository(application)
    private val _releaseRepository = ReleaseRepository(application)
    private val _workManager = WorkManager.getInstance(application)

    private val _uiState = MutableStateFlow(StatsUiState(isLoading = true))
    private val _workInfoState: Flow<WorkInfo?> =
        _workManager.getWorkInfosByTagLiveData(UNIQUE_WORK_TAG)
            .asFlow()
            .filter { it.isNotEmpty() }
            .map { it.last() } // so che c'è un solo worker attivo alla volta (e come???)
            .onStart { emit(null) }
            .onEach { LogHelper.d { "WI: $it" } }

    val uiState = _uiState
        .combine(_workInfoState) { ui, wi -> ui.withWorkInfo(wi) }
        .onEach { LogHelper.d { "ST: $it" } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = StatsUiState()
        )

    init {
        viewModelScope.launch {
            refreshState()
        }
    }

    private suspend fun refreshState() {
        val state = StatsUiState(
            comicsCount = _comicsRepository.count(),
            lastUpdate = _releaseRepository.getLastUpdate()
        )
        _uiState.value = state
    }

    private fun loading() = _uiState.update { state ->
        state.copy(isLoading = true, errorCode = null)
    }

    private fun notifyError(errorCode: String) = _uiState.update { state ->
        state.copy(isLoading = false, errorCode = errorCode)
    }

    fun deleteAll() = viewModelScope.launch {
        loading()
        try {
            _comicsRepository.deleteAll(deleteImages = true)
            refreshState()
        } catch (ex: Exception) {
            LogHelper.e("Error deleting comics and images", ex)
            notifyError("delete-error")
        }
    }

    fun importFromPreviousVersion() {
        val request = OneTimeWorkRequest.Builder(ImportFromOldDatabaseWorker::class.java)
            .addTag(UNIQUE_WORK_TAG)
            .build()

        _workManager.enqueueUniqueWork(
            ImportFromOldDatabaseWorker.WORK_NAME,
            ExistingWorkPolicy.KEEP,
            request
        )
    }

    fun exportBackup() {
        TODO()
    }
}