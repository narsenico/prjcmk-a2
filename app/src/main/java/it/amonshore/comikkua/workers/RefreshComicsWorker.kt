package it.amonshore.comikkua.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import it.amonshore.comikkua.LogHelper
import it.amonshore.comikkua.data.web.CmkWebRepositoryKt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Aggiorna l'elenco dei comics disponibili.
 * L'utente può attingere da questo elenco per scegliere i comics da tenere monitorati.
 *
 * Si può recuperare il numero di comics disponibili con il parametro REFRESHING_COUNT.
 */
class RefreshComicsWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {

    companion object {
        const val REFRESHING_COUNT = "refreshing_count"
    }

    private val WORK_NAME = RefreshComicsWorker::class.simpleName

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val cmkWebRepository = CmkWebRepositoryKt(applicationContext)
            val count = cmkWebRepository.refreshAvailableComics()
            val outData = Data.Builder().putInt(REFRESHING_COUNT, count).build();
            LogHelper.i("Available comics refreshed count=$count")
            Result.success(outData)
        } catch (ex: Exception) {
            LogHelper.e("Error refreshing available comics", ex)
            Result.failure()
        }
    }
}