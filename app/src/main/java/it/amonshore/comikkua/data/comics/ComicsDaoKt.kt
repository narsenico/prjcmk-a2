package it.amonshore.comikkua.data.comics

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface ComicsDaoKt {

    @Query("SELECT * FROM tComics WHERE removed = 0 AND selected = 1 ORDER BY name COLLATE NOCASE ASC")
    suspend fun getComics(): List<Comics>

    @Query("SELECT * FROM tComics WHERE removed = 0 AND selected = 1 ORDER BY name COLLATE NOCASE ASC")
    @Transaction
    suspend fun getRawComicsWithReleases(): List<ComicsWithReleases>
}