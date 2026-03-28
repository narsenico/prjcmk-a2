package it.amonshore.comikkua.workers

import android.content.Context
import android.net.Uri
import android.os.CancellationSignal
import androidx.core.net.toUri
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import it.amonshore.comikkua.LogHelper
import it.amonshore.comikkua.data.comics.ComicsRepository
import it.amonshore.comikkua.data.release.ReleaseRepository
import it.amonshore.comikkua.toReleaseDate
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.dropWhile
import kotlinx.coroutines.flow.fold
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.job
import kotlinx.coroutines.withContext
import java.io.InputStreamReader

class RestoreWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    private val _cancellationSignal = CancellationSignal()
    private val _comicsRepository = ComicsRepository(applicationContext)

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) context@{

        coroutineContext.job.invokeOnCompletion { cause ->
            when (cause) {
                null -> {}
                is CancellationException -> {
                    LogHelper.w("Restore backup canceled!")
                    _cancellationSignal.cancel()
                }

                else -> LogHelper.e("Job failed", cause)
            }
        }

        // TODO: si può pensare di andare in aggiornamento?
        if (_comicsRepository.count() > 0) {
            LogHelper.w("restore failed: current db not empty")
            return@context Result.failure(workDataOf("reason" to "not-empty"))
        }

        val backupUri = inputData.getString("BACKUP_URI")?.toUri() ?: return@context Result.failure(
            workDataOf("reason" to "backup-uri-empty")
        )

        return@context restore(backupUri)
    }

    private suspend fun restore(backupUri: Uri): Result = try {
        LogHelper.d { "restoring backup..." }
        val type = object : TypeToken<List<ComicsWithReleasesDto>>() {}.type
        val gson = GsonBuilder()
            .serializeNulls()
            .create()
        val releaseRepository = ReleaseRepository(applicationContext)

        val counter = applicationContext.contentResolver.openInputStream(backupUri)?.use { input ->
            JsonReader(InputStreamReader(input)).use {
                gson.fromJson<List<ComicsWithReleasesDto>>(it, type)
                    .asFlow()
                    .dropWhile {
                        _cancellationSignal.isCanceled
                    }
                    .map { cr -> cr.toEntity() }
                    .onEach { cr ->
                        _comicsRepository.insert(cr.comics)
                        releaseRepository.insertReleases(cr.releases)
                    }
                    .fold(ImportFromOldDatabaseCounter()) { acc, cr ->
                        acc.count(cr)
                    }
            }
        } ?: throw Error("Cannot open backup file $backupUri: restore failed")

        LogHelper.i("restore complete total=${counter.total} (sourced=${counter.sourced} oldest=${counter.oldestLastReleaseDate})")
        Result.success(
            workDataOf(
                "total" to counter.total,
                "sourced" to counter.sourced,
                "oldest_last_release" to counter.oldestLastReleaseDate?.toReleaseDate()
            )
        )
    } catch (error: Error) {
        LogHelper.e("Error restoring backup", error)
        Result.failure(workDataOf("reason" to "error"))
    }

    companion object {
        val WORK_NAME: String = RestoreWorker::class.java.name
    }
}