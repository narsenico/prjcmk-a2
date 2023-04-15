package it.amonshore.comikkua.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import it.amonshore.comikkua.LogHelper
import it.amonshore.comikkua.data.release.ReleaseRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UpdateReleasesWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) context@{
        val repository = ReleaseRepository(applicationContext)
        val result = repository.loadNewReleases()

        return@context result.map {
            LogHelper.i("Added ${it.count} new releases with tag='${it.tag}'")
            Result.success()
        }.recover {
            LogHelper.e("Error updating releases", it)
            Result.failure()
        }.getOrThrow()
    }

    companion object {
        const val CHANNEL_ID = "it.amonshore.comikkua.CHANNEL_AUTO_UPDATE";
        val WORK_NAME: String = UpdateReleasesWorker::class.java.name
    }
}