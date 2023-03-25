package it.amonshore.comikkua.data.comics

import android.content.Context
import it.amonshore.comikkua.data.ComikkuDatabase

class ComicsRepositoryKt(context: Context) {

    private val _comicsDao = ComikkuDatabase.getDatabase(context).comicsDaoKt()

    suspend fun insert(comics: Comics) {
        _comicsDao.insert(comics)
    }
}