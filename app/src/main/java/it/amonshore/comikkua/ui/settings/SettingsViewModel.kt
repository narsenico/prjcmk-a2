package it.amonshore.comikkua.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkInfo
import androidx.work.WorkManager
import it.amonshore.comikkua.LogHelper
import it.amonshore.comikkua.data.comics.ComicsRepository
import it.amonshore.comikkua.workers.BackupWorker
import it.amonshore.comikkua.workers.ImportFromOldDatabaseWorker
import kotlinx.coroutines.launch
import java.util.UUID

sealed class UiSettingsResult {
    object ComicsDeleted : UiSettingsResult()
    object ComicsDeletingError: UiSettingsResult()
}

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val _comicsRepository = ComicsRepository(application)
    private val _workManager = WorkManager.getInstance(application)
    private val _result = MutableLiveData<UiSettingsResult>()
    private val _backupStatus = MediatorLiveData<WorkInfo>()
    private val _importOldDatabaseStatus = MediatorLiveData<WorkInfo>()

    val result: LiveData<UiSettingsResult> = _result
    val backupStatus: LiveData<WorkInfo> = _backupStatus
    val importOldDatabaseStatus: LiveData<WorkInfo> = _importOldDatabaseStatus

//    fun deleteAllComicsWithoutImages() = viewModelScope.launch {
//        try {
//            _comicsRepository.deleteAll(deleteImages = false)
//            _result.postValue(UiSettingsResult.ComicsDeleted)
//        } catch (ex: Exception) {
//            LogHelper.e("Error deleting comics", ex)
//            _result.postValue(UiSettingsResult.ComicsDeletingError)
//        }
//    }

    fun deleteAllComicsAndImages() = viewModelScope.launch {
        try {
            _comicsRepository.deleteAll(deleteImages = true)
            _result.postValue(UiSettingsResult.ComicsDeleted)
        } catch (ex: Exception) {
            LogHelper.e("Error deleting comics and images", ex)
            _result.postValue(UiSettingsResult.ComicsDeletingError)
        }
    }

    fun startBackupExport() {
        val request = OneTimeWorkRequest.from(BackupWorker::class.java)

        _workManager.enqueueUniqueWork(
            BackupWorker.WORK_NAME,
            ExistingWorkPolicy.KEEP,
            request
        )

        _workManager.applyWorkInfoByIdLiveData(request.id, _backupStatus)
    }

    fun cancelBackupExport() {
        _workManager.cancelUniqueWork(BackupWorker.WORK_NAME)
    }

    fun startOldDatabaseImport() {
        val request = OneTimeWorkRequest.from(ImportFromOldDatabaseWorker::class.java)

        _workManager.enqueueUniqueWork(
            ImportFromOldDatabaseWorker.WORK_NAME,
            ExistingWorkPolicy.KEEP,
            request
        )

        _workManager.applyWorkInfoByIdLiveData(request.id, _importOldDatabaseStatus)
    }

    fun cancelOldDatabaseImport() {
        _workManager.cancelUniqueWork(ImportFromOldDatabaseWorker.WORK_NAME)
    }

    private fun WorkManager.applyWorkInfoByIdLiveData(id: UUID, mediator: MediatorLiveData<WorkInfo>) {
        val liveData = getWorkInfoByIdLiveData(id)

        mediator.addSource(liveData) {
            mediator.value = it
            if (it.state.isFinished) {
                mediator.removeSource(liveData)
            }
        }
    }
}