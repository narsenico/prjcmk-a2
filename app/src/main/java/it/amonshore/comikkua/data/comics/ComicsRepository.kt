package it.amonshore.comikkua.data.comics

import android.content.Context
import it.amonshore.comikkua.LogHelper
import it.amonshore.comikkua.data.ComikkuDatabase
import it.amonshore.comikkua.ui.isValidImageFileName

class ComicsRepository(private val context: Context) {

    private val _comicsDao = ComikkuDatabase.getDatabase(context).comicsDao()

    suspend fun count() = _comicsDao.count()

    suspend fun insert(comics: Comics) {
        _comicsDao.insert(comics)
    }

    suspend fun upsert(comics: Comics) {
        _comicsDao.upsert(comics)
    }

    suspend fun deleteRemoved(tag: String) {
        val removedIds = _comicsDao.getRemovedComicsIds(tag)
        _comicsDao.deleteRemoved(tag)

        // elimino anche le immagini
        // mi fido del fatto che removedIds contenga esattamente i comics rimossi con l'istruzione sopra
        try {
            context.filesDir
                .listFiles { _, name -> isValidImageFileName(name, removedIds) }
                ?.forEach { it.delete() }
        } catch (ex: Exception) {
            LogHelper.e("There was an error deleting image files", ex)
        }
    }

    suspend fun undoRemoved(tag: String) = _comicsDao.undoRemoved(tag)

    suspend fun markedAsRemoved(ids: List<Long>, tag: String): Int =
        _comicsDao.markedAsRemoved(ids, tag)

    suspend fun getComicsWithReleases(id: Long) =
        _comicsDao.getComicsWithReleases(id)

    suspend fun existsComicsWithName(name: String) =
        _comicsDao.getComicsByName(name) != null

    suspend fun getPublishers(): List<String> =
        _comicsDao.getPublishers()

    suspend fun getAuthors(): List<String> =
        _comicsDao.getAuthors()

    suspend fun getAllComicsWithReleases() = _comicsDao.getAllComicsWithReleases()

    fun getComicsWithReleasesFlow(id: Long) =
        _comicsDao.getComicsWithReleasesFlow(id)

    fun getComicsWithReleasesPagingSource(like: String? = null) =
        if (like == null) {
            _comicsDao.getComicsWithReleasesPagingSource()
        } else {
            _comicsDao.getComicsWithReleasesPagingSource(like)
        }

    suspend fun deleteAll(deleteImages: Boolean) {
        _comicsDao.deleteAll()

        if (deleteImages) {
            context.filesDir
                .listFiles { _, name -> isValidImageFileName(name) }
                ?.forEach { file -> file.delete() }
        }
    }
}