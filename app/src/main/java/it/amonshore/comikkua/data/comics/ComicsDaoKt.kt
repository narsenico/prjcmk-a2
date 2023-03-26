package it.amonshore.comikkua.data.comics

import androidx.lifecycle.LiveData
import androidx.paging.PagingSource
import androidx.room.*

@Dao
interface ComicsDaoKt {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(comics: Comics)

    @Query("DELETE FROM tComics WHERE removed = 1")
    @Transaction
    suspend fun deleteRemoved()

    @Query("UPDATE tComics SET removed = 0")
    @Transaction
    suspend fun undoRemoved()

    @Query("UPDATE tComics SET removed = :removed WHERE id IN (:ids)")
    @Transaction
    suspend fun updateRemoved(ids: List<Long>, removed: Boolean): Int

    @Query("SELECT * FROM tComics WHERE removed = 0 AND selected = 1 ORDER BY name COLLATE NOCASE ASC")
    suspend fun getComics(): List<Comics>

    @Query("SELECT * FROM tComics WHERE removed = 0 AND selected = 1 ORDER BY name COLLATE NOCASE ASC")
    @Transaction
    suspend fun getAllComicsWithReleases(): List<ComicsWithReleases>

    @Query("SELECT * FROM tComics WHERE id = :id AND removed = 0 AND selected = 1")
    @Transaction
    fun getComicsWithReleases(id: Long): LiveData<ComicsWithReleases>

    @Query("SELECT * FROM tComics WHERE removed = 0 AND selected = 1 ORDER BY name COLLATE NOCASE ASC")
    @Transaction
    fun getComicsWithReleasesPagingSource(): PagingSource<Int, ComicsWithReleases>

    @Query(
        """
            SELECT * 
            FROM tComics 
            WHERE 
            removed = 0 AND selected = 1 
            AND (name LIKE :likeName 
                OR publisher LIKE :likeName 
                OR authors LIKE :likeName 
                OR notes LIKE :likeName)
            ORDER BY name COLLATE NOCASE ASC
            """
    )
    @Transaction
    fun getComicsWithReleasesPagingSource(likeName: String): PagingSource<Int, ComicsWithReleases>
}