@file:OptIn(FlowPreview::class, FlowPreview::class)

package it.amonshore.comikkua.ui.stats

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkInfo
import androidx.work.WorkManager
import it.amonshore.comikkua.LogHelper
import it.amonshore.comikkua.R
import it.amonshore.comikkua.data.comics.ComicsRepository
import it.amonshore.comikkua.data.release.ReleaseRepository
import it.amonshore.comikkua.toLocalDate
import it.amonshore.comikkua.workers.BackupWorker
import it.amonshore.comikkua.workers.ImportFromOldDatabaseWorker
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.ZonedDateTime


class StatsViewModel(private val application: Application) : AndroidViewModel(application) {

    private val _comicsRepository = ComicsRepository(application)
    private val _releaseRepository = ReleaseRepository(application)
    private val _workManager = WorkManager.getInstance(application)

    private val _isLoading = MutableStateFlow(false)
    private val _error = MutableStateFlow<String?>(null)
    private val _count = _comicsRepository.countFlow().debounce(250)
    private val _lastUpdate = _releaseRepository.getLastUpdateFlow().debounce(250)

    val counterState =
        combine(_isLoading, _error, _count, _lastUpdate) { loading, error, count, lastUpdate ->
            StatsCounterState(
                comicsCount = count,
                lastUpdate = lastUpdate,
                isLoading = loading,
                error = error
            )
        }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(),
                initialValue = StatsCounterState()
            )

    fun deleteAll() = viewModelScope.launch {
        _isLoading.value = true
        _error.value = null
        try {
            _comicsRepository.deleteAll(deleteImages = true)
        } catch (ex: Exception) {
            LogHelper.e("Error deleting comics and images", ex)
            _error.value = application.getString(R.string.comics_delete_error)
        } finally {
            _isLoading.value = false
        }
    }

    fun importFromPreviousVersion() {
        val request = OneTimeWorkRequest.from(ImportFromOldDatabaseWorker::class.java)

        _workManager.enqueueUniqueWork(
            ImportFromOldDatabaseWorker.WORK_NAME,
            ExistingWorkPolicy.KEEP,
            request
        )
    }

    fun exportBackup() {
        val request = OneTimeWorkRequest.from(BackupWorker::class.java)

        _workManager.enqueueUniqueWork(
            BackupWorker.WORK_NAME,
            ExistingWorkPolicy.KEEP,
            request
        )
    }

    fun getImportFromPreviousVersionState(): StateFlow<StatsWorkerState> {
        return _workManager.getWorkInfosForUniqueWorkLiveData(ImportFromOldDatabaseWorker.WORK_NAME)
            .asFlow()
            .distinctUntilChanged()
            .flatMapConcat { it.toImportFromPreviousVersionState(application).asFlow() }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(),
                initialValue = StatsWorkerState.None
            )
    }

    fun getExportBackupState(): StateFlow<StatsWorkerState> {
        return _workManager.getWorkInfosForUniqueWorkLiveData(BackupWorker.WORK_NAME)
            .asFlow()
            .distinctUntilChanged()
            .flatMapConcat { it.toExportBackupState(application).asFlow() }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(),
                initialValue = StatsWorkerState.None
            )
    }
}

data class StatsCounterState(
    val comicsCount: Int = 0,
    val lastUpdate: ZonedDateTime? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
)

interface StatsWorkerState {
    object None : StatsWorkerState
    object Running : StatsWorkerState
    data class Completed(val message: String) : StatsWorkerState
    data class Failed(val message: String) : StatsWorkerState
}

private fun List<WorkInfo>.toImportFromPreviousVersionState(context: Context): List<StatsWorkerState> =
    map {
        when (it.state) {
            WorkInfo.State.ENQUEUED, WorkInfo.State.RUNNING -> StatsWorkerState.Running
            WorkInfo.State.SUCCEEDED -> {
                val total = it.outputData.getInt("total", 0)
                val notSourced = total - it.outputData.getInt("sourced", 0)
                val oldestLastReleaseDate =
                    it.outputData.getString("oldest_last_release")?.toLocalDate()

                if (oldestLastReleaseDate == null) {
                    StatsWorkerState.Completed(
                        context.getString(
                            R.string.import_old_database_success,
                            total,
                            notSourced
                        )
                    )
                } else {
                    StatsWorkerState.Completed(
                        context.getString(
                            R.string.import_old_database_success_with_date,
                            total,
                            notSourced,
                            oldestLastReleaseDate
                        )
                    )
                }
            }

            WorkInfo.State.FAILED -> {
                val message = context.getString(it.outputData.getImportOldDatabaseErrorStringRes())
                StatsWorkerState.Failed(message)
            }

            WorkInfo.State.BLOCKED -> StatsWorkerState.Failed("work blocked")
            WorkInfo.State.CANCELLED -> StatsWorkerState.None
        }
    }

private fun Data.getImportOldDatabaseErrorStringRes() = when (getString("reason")) {
    "connection-error" -> R.string.import_old_database_connection_error
    "not-empty" -> R.string.import_old_database_not_empty_error
    "source-not-found" -> R.string.import_old_database_source_not_found_error
    else -> R.string.import_old_database_error
}

private fun List<WorkInfo>.toExportBackupState(context: Context): List<StatsWorkerState> = map {
    when (it.state) {
        WorkInfo.State.ENQUEUED, WorkInfo.State.RUNNING -> StatsWorkerState.Running
        WorkInfo.State.SUCCEEDED -> StatsWorkerState.Completed(context.getString(R.string.backup_completed))
        WorkInfo.State.FAILED -> StatsWorkerState.Failed(context.getString(R.string.backup_error))
        WorkInfo.State.BLOCKED -> StatsWorkerState.Failed("work blocked")
        WorkInfo.State.CANCELLED -> StatsWorkerState.None
    }
}