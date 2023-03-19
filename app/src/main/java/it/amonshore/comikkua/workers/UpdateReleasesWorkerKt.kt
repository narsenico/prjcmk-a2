package it.amonshore.comikkua.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import it.amonshore.comikkua.LogHelper
import it.amonshore.comikkua.data.ComikkuDatabase
import it.amonshore.comikkua.data.web.toRelease
import it.amonshore.comikkua.services.CmkWebService
import kotlinx.coroutines.*
import java.util.*

const val PREVENT_NOTIFICATION = "prevent_notification"
const val RELEASE_COUNT = "release_count"
const val RELEASE_TAG = "release_tag"

class UpdateReleasesWorkerKt(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    private val service = CmkWebService.create()

    override suspend fun doWork(): Result = try {
        withContext(Dispatchers.IO) myScope@{

            val database = ComikkuDatabase.getDatabase(applicationContext)
            val comicsDao = database.comicsDaoKt()
            val releaseDao = database.releaseDaoKt()
            val comics = comicsDao.getRawComicsWithReleases()
            val tag = UUID.randomUUID().toString()

            val calls = comics.map {
                async {
                    val comicsId = it.comics.id
                    val comicsName = it.comics.name
                    val releaseFrom = it.nextReleaseNumber
                    val releases = service.getReleasesByTitle(comicsName, releaseFrom)
                    // TODO: Come chiave del comics dovrebbe essere usato [CmkWebComicsRelease.refId]
                    //  ma non esiste in [Release].
                    //  La chiave attuale è l'id del comics
                    //  (infatti anche [it.amonshore.comikkua.data.comics.Comics.refJsonId] nessuno lo valorizza).
                    releases.map { it.toRelease(comicsId, tag) }
                }
            }

            val responses = calls.awaitAll()
            val releases = responses
                .filter { it.isNotEmpty() }
                .flatten()
            // TODO: insert non viene interrotto dall'annullamento del worker
            //  non so se è la cosa giusta da fare
            //  forse meglio eseguire un insert alla volta e controllare se il woker è attivo?
            releaseDao.insert(releases)

            val count = releases.size
//            var count = 0
//            for (releases in responses.filter { it.isNotEmpty() }) {
//                if (isStopped) {
//                    return@myScope Result.failure()
//                }
//                releaseDao.insert(releases)
//                count += releases.size
//            }

            // TODO: notificare se count > 0
            LogHelper.i("Added $count new releases")

            return@myScope success(count, tag)
        }
    } catch (_: CancellationException) {
        LogHelper.w("${UpdateReleasesWorkerKt::class.simpleName} was canceled")
        Result.failure()
    }

    private fun success(count: Int, tag: String): Result {
        val data = Data.Builder()
            .putInt(RELEASE_COUNT, count)
            .putString(RELEASE_TAG, tag)
            .build()
        return Result.success(data)
    }
}