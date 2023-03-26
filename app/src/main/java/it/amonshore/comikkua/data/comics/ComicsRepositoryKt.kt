package it.amonshore.comikkua.data.comics

import android.content.Context
import androidx.lifecycle.LiveData
import it.amonshore.comikkua.data.ComikkuDatabase

class ComicsRepositoryKt(context: Context) {

    private val _comicsDao = ComikkuDatabase.getDatabase(context).comicsDaoKt()

    suspend fun insert(comics: Comics) {
        _comicsDao.insert(comics)
    }

    suspend fun deleteRemoved() = _comicsDao.deleteRemoved()

    suspend fun undoRemoved() = _comicsDao.undoRemoved()

    suspend fun updateRemoved(ids: List<Long>, removed: Boolean): Int =
        _comicsDao.updateRemoved(ids, removed)

    fun getComicsWithReleases(id: Long): LiveData<ComicsWithReleases> =
        _comicsDao.getComicsWithReleases(id)

    fun getComicsWithReleasesPagingSource(like: String? = null) =
        if (like == null) {
            _comicsDao.getComicsWithReleasesPagingSource()
        } else {
            _comicsDao.getComicsWithReleasesPagingSource(like)
        }
}