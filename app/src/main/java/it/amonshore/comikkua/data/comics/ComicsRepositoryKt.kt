package it.amonshore.comikkua.data.comics

import android.content.Context
import it.amonshore.comikkua.data.ComikkuDatabase

class ComicsRepositoryKt(context: Context) {

    private val _comicsDao = ComikkuDatabase.getDatabase(context).comicsDaoKt()

    suspend fun insert(comics: Comics) {
        _comicsDao.insert(comics)
    }

    suspend fun upsert(comics: Comics) {
        _comicsDao.upsert(comics)
    }

    suspend fun deleteRemoved() = _comicsDao.deleteRemoved()

    suspend fun undoRemoved() = _comicsDao.undoRemoved()

    suspend fun setRemoved(ids: List<Long>): Int =
        _comicsDao.updateRemoved(ids, true)

    suspend fun getComicsWithReleases(id: Long) =
        _comicsDao.getComicsWithReleases(id)

    suspend fun existsComicsWithName(name: String) =
        _comicsDao.getComicsByName(name) != null

    suspend fun getRemovedComicsIds(): List<Long> =
        _comicsDao.getRemovedComicsIds()

    suspend fun getPublishers(): List<String> =
        _comicsDao.getPublishers()

    suspend fun getAuthors(): List<String> =
        _comicsDao.getAuthors()

    fun getComicsWithReleasesFlow(id: Long) =
        _comicsDao.getComicsWithReleasesFlow(id)

    fun getComicsWithReleasesPagingSource(like: String? = null) =
        if (like == null) {
            _comicsDao.getComicsWithReleasesPagingSource()
        } else {
            _comicsDao.getComicsWithReleasesPagingSource(like)
        }
}