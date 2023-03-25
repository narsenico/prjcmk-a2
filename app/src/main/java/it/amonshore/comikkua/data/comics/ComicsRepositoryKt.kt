package it.amonshore.comikkua.data.comics

import android.content.Context
import androidx.lifecycle.LiveData
import it.amonshore.comikkua.data.ComikkuDatabase

class ComicsRepositoryKt(context: Context) {

    private val _comicsDao = ComikkuDatabase.getDatabase(context).comicsDaoKt()

    suspend fun insert(comics: Comics) {
        _comicsDao.insert(comics)
    }

    fun getComicsWithReleases(id: Long): LiveData<ComicsWithReleases> =
        _comicsDao.getComicsWithReleases(id)

    fun getComicsWithReleasesPagingSource(likeName: String? = null) =
        if (likeName == null) {
            _comicsDao.getComicsWithReleasesPagingSource()
        } else {
            _comicsDao.getComicsWithReleasesPagingSource(likeName)
        }
}