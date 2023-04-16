package it.amonshore.comikkua.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.gson.ExclusionStrategy
import com.google.gson.FieldAttributes
import com.google.gson.GsonBuilder
import it.amonshore.comikkua.BackupExclude
import it.amonshore.comikkua.LogHelper
import it.amonshore.comikkua.data.comics.ComicsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class BackupWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) context@{
        try {
            val repository = ComicsRepository(applicationContext)
            val gson = GsonBuilder()
                .serializeNulls()
                .setExclusionStrategies(object : ExclusionStrategy {
                    override fun shouldSkipField(f: FieldAttributes?): Boolean =
                        f?.getAnnotation(BackupExclude::class.java) != null

                    override fun shouldSkipClass(clazz: Class<*>?): Boolean = false
                })
                .create()

            val data = repository.getAllComicsWithReleases()
            val output = File(applicationContext.filesDir, "backup.json")
            output.writer().use {
                gson.toJson(data, it)
            }

            LogHelper.i("Backup complete output=$output")
            return@context Result.success()
        } catch (ex: Exception) {
            LogHelper.e("Error during backup", ex)
            return@context Result.failure()
        }
    }

    companion object {
        val WORK_NAME: String = BackupWorker::class.java.name
    }
}