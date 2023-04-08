package it.amonshore.comikkua.data.release

import android.content.Context
import it.amonshore.comikkua.LogHelper
import it.amonshore.comikkua.atFirstDayOfWeek
import it.amonshore.comikkua.data.ComikkuDatabase
import it.amonshore.comikkua.data.comics.ComicsWithReleases
import it.amonshore.comikkua.data.web.toRelease
import it.amonshore.comikkua.services.CmkWebService
import it.amonshore.comikkua.toYearMonthDay
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.util.*

private const val ONE_DAY: Long = 86_400_000L

data class NewReleasesCountAndTag(val count: Int, val tag: String)

class ReleaseRepository(context: Context) {

    private val _database = ComikkuDatabase.getDatabase(context)
    private val _releaseDao = _database.releaseDaoKt()
    private val _comicsDao by lazy { _database.comicsDaoKt() }
    private val _service by lazy { CmkWebService.create() }

    fun getComicsReleasesByComicsIdFLow(comicsId: Long) =
        _releaseDao.getComicsReleasesByComicsIdFLow(comicsId)

    suspend fun getComicsReleases(ids: List<Long>) = _releaseDao.getComicsReleases(ids)

    suspend fun getNotPurchasedComicsReleases(releaseDateFrom: LocalDate, releaseDateTo: LocalDate) =
        _releaseDao.getNotPurchasedComicsReleases(releaseDateFrom.toYearMonthDay(), releaseDateTo.toYearMonthDay())

    fun getNotableComicsReleasesFlow(): Flow<List<ComicsRelease>> {
        val today = LocalDate.now()
        // il giorno di riferimento è il primo giorno della settimana in corso
        val refDate = today.atFirstDayOfWeek()
        // la settimana dopo
        val refNextDate = refDate.plusDays(7)
        val refOtherDate = refNextDate.plusDays(7)
        // per quanto riguarda le release precedenti estraggo anche quelle aquistate dal giorno prima (rispetto al corrente)
        //  (quelle successive verrebbero cmq estratte in quanto fanno parte del "periodo corrente")
        val retainStart = System.currentTimeMillis() - ONE_DAY

        LogHelper.d("prepare notable releases refDate=$refDate, refNextDate=$refNextDate, refOtherDate=$refOtherDate retainStart=$retainStart")

        return _releaseDao.getNotableComicsReleasesFlow(
            refDate = refDate.toYearMonthDay(),
            refNextDate = refNextDate.toYearMonthDay(),
            refOtherDate = refOtherDate.toYearMonthDay(),
            retainStart
        )
    }

    fun getComicsReleasesByTag(tag: String): Flow<List<ComicsRelease>> =
        _releaseDao.getComicsReleasesByTagFlow(tag)

    suspend fun getRelease(id: Long) = _releaseDao.getRelease(id)

    suspend fun insertReleases(releases: List<Release>) = _releaseDao.insert(releases)

    suspend fun updatePurchased(ids: List<Long>, purchased: Boolean) {
        val lastUpdate = System.currentTimeMillis()
        _releaseDao.updatePurchased(ids, purchased, lastUpdate)
    }

    suspend fun updateOrdered(ids: List<Long>, ordered: Boolean) {
        val lastUpdate = System.currentTimeMillis()
        _releaseDao.updateOrdered(ids, ordered, lastUpdate)
    }

    suspend fun togglePurchased(ids: List<Long>) {
        if (ids.isNotEmpty()) {
            val lastUpdate = System.currentTimeMillis()
            val purchased = !_releaseDao.getRelease(ids.first()).purchased
            _releaseDao.updatePurchased(ids, purchased, lastUpdate)
        }
    }

    suspend fun toggleOrdered(ids: List<Long>) {
        if (ids.isNotEmpty()) {
            val lastUpdate = System.currentTimeMillis()
            val ordered = !_releaseDao.getRelease(ids.first()).ordered
            _releaseDao.updateOrdered(ids, ordered, lastUpdate)
        }
    }

    suspend fun deleteRemoved() = _releaseDao.deleteRemoved()

    suspend fun undoRemoved() = _releaseDao.undoRemoved()

    suspend fun updateRemoved(ids: List<Long>, removed: Boolean): Int =
        _releaseDao.updateRemoved(ids, removed)

    suspend fun loadNewReleases(comics: ComicsWithReleases): Result<NewReleasesCountAndTag> =
        withContext(Dispatchers.IO + SupervisorJob()) {
            runCatching {
                val tag = UUID.randomUUID().toString()
                val releases = loadNewReleasesAsync(comics, tag).await()

                if (releases.isNotEmpty()) {
                    _releaseDao.insert(releases)
                }

                val res = NewReleasesCountAndTag(releases.size, tag)
                return@withContext Result.success(res)
            }
        }

    suspend fun loadNewReleases(): Result<NewReleasesCountAndTag> =
        withContext(Dispatchers.IO + SupervisorJob()) {
            runCatching {
                val comicsList = _comicsDao.getAllComicsWithReleases()
                val tag = UUID.randomUUID().toString()
                val releases = loadNewReleasesAsync(comicsList, tag).awaitAll()
                    .filter { it.isNotEmpty() }
                    .flatten()

                if (releases.isNotEmpty()) {
                    _releaseDao.insert(releases)
                }

                val res = NewReleasesCountAndTag(releases.size, tag)
                return@withContext Result.success(res)
            }
        }

    private suspend fun loadNewReleasesAsync(comics: ComicsWithReleases, tag: String) =
        coroutineScope {
            return@coroutineScope async {
                val refId =
                    comics.comics.sourceId // TODO: convertendo Comics in Kotlin potrà essere null
                val fromNumber = comics.nextReleaseNumber
                return@async _service.getReleases(refId, fromNumber)
                    .filter {
                        it.number >= fromNumber // TODO: cmkweb ancora non supporta il parametro fromNumber, quindi devo filtrare
                    }
                    .map { it.toRelease(comics.comics.id, tag) }
            }
        }

    private suspend fun loadNewReleasesAsync(comicsList: List<ComicsWithReleases>, tag: String) =
        coroutineScope {
            val calls = comicsList.map {
                async {
                    val comicsId = it.comics.id
                    val comicsName = it.comics.name
                    val releaseFrom = it.nextReleaseNumber
                    val releases = _service.getReleasesByTitle(comicsName, releaseFrom)
                    // TODO: Come chiave del comics dovrebbe essere usato [CmkWebComicsRelease.refId]
                    //  ma non esiste in [Release].
                    //  La chiave attuale è l'id del comics
                    //  (infatti anche [it.amonshore.comikkua.data.comics.Comics.refJsonId] nessuno lo valorizza).
                    releases.map { it.toRelease(comicsId, tag) }
                }
            }

            return@coroutineScope calls
        }
}