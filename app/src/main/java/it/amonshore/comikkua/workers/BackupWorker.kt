package it.amonshore.comikkua.workers

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.google.gson.ExclusionStrategy
import com.google.gson.FieldAttributes
import com.google.gson.GsonBuilder
import it.amonshore.comikkua.BackupExclude
import it.amonshore.comikkua.BuildConfig
import it.amonshore.comikkua.LogHelper
import it.amonshore.comikkua.data.comics.ComicsRepository
import it.amonshore.comikkua.toFileNamePart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStreamWriter
import java.time.LocalDateTime

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
            val now = LocalDateTime.now().toFileNamePart()
            val fileName = "comikku_v${BuildConfig.VERSION_CODE}_$now.bck.json"
            writeBackupToDownloadsFolder(applicationContext, fileName) {
                gson.toJson(data, it)
            }

            LogHelper.i("Backup saved to $fileName")
            return@context Result.success(workDataOf("backup_name" to fileName))
        } catch (ex: Exception) {
            LogHelper.e("Error during backup", ex)
            return@context Result.failure()
        }
    }

    private fun writeBackupToDownloadsFolder(context: Context, fileName: String, block: (writer: OutputStreamWriter) -> Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, "application/json")
                put(MediaStore.Downloads.IS_PENDING, 1)
            }

            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                ?: throw Error("Cannot retrieve content uri for backup file")

            resolver.openOutputStream(uri)?.use { stream ->
                OutputStreamWriter(stream).use {
                    block(it)
                }
            }

            contentValues.apply {
                clear()
                put(MediaStore.Downloads.IS_PENDING, 0)
            }
            resolver.update(uri, contentValues, null, null)
            return
        }

        TODO("writeBackupToDownloadsFolder not implemented for API < ${Build.VERSION_CODES.Q}")
    }

    companion object {
        val WORK_NAME: String = BackupWorker::class.java.name
    }
}