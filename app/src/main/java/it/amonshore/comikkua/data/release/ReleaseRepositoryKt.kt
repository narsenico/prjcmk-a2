package it.amonshore.comikkua.data.release

import android.app.Application
import it.amonshore.comikkua.data.ComikkuDatabase
import it.amonshore.comikkua.data.comics.ComicsWithReleases
import it.amonshore.comikkua.data.web.toRelease
import it.amonshore.comikkua.letNotEmpty
import it.amonshore.comikkua.services.CmkWebService

class ReleaseRepositoryKt(application: Application) {

    private val _releaseDao = ComikkuDatabase.getDatabase(application).releaseDaoKt()
    private val _service = CmkWebService.create()

    fun getComicsReleasesByComicsId(comicsId: Long) =
        _releaseDao.getComicsReleasesByComicsId(comicsId)

    suspend fun getComicsReleases(ids: List<Long>) = _releaseDao.getComicsReleases(ids)

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

    suspend fun refreshWithNewReleases(comics: ComicsWithReleases): Int {
        if (!comics.comics.isSourced) {
            return 0
        }

        val comicsId = comics.comics.id
        val refId = comics.comics.sourceId // TODO: convertendo Comics in Kotlin potrà essere null
        val fromNumber = comics.nextReleaseNumber
        return _service.getReleases(refId, fromNumber)
            .filter {
                it.number >= fromNumber // TODO: cmkweb ancora non supporta il parametro fromNumber, quindi devo filtrare
            }
            .map {
                it.toRelease(comicsId)
            }
            .letNotEmpty {
                _releaseDao.insert(it)
                it.size
            } ?: 0
    }
}