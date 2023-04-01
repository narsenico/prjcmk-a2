package it.amonshore.comikkua.data.comics

import androidx.paging.PagingSource
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ComicsDaoKt {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(comics: Comics)

    @Upsert
    suspend fun upsert(comics: Comics)

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

    @Query("SELECT * FROM tComics WHERE name = :name COLLATE NOCASE AND removed = 0 AND selected = 1")
    suspend fun getComicsByName(name: String): Comics?

    @Query("SELECT * FROM tComics WHERE removed = 0 AND selected = 1 ORDER BY name COLLATE NOCASE ASC")
    @Transaction
    suspend fun getAllComicsWithReleases(): List<ComicsWithReleases>

    @Query("SELECT * FROM tComics WHERE id = :id AND removed = 0 AND selected = 1")
    @Transaction
    suspend fun getComicsWithReleases(id: Long): ComicsWithReleases

    @Query("SELECT id FROM tComics WHERE removed = 1")
    suspend fun getRemovedComicsIds(): List<Long>

    @Query("SELECT * FROM tComics WHERE id = :id AND removed = 0 AND selected = 1")
    @Transaction
    fun getComicsWithReleasesFlow(id: Long): Flow<ComicsWithReleases>

    @Query("SELECT * FROM tComics WHERE removed = 0 AND selected = 1 ORDER BY name COLLATE NOCASE ASC")
    @Transaction
    fun getComicsWithReleasesPagingSource(): PagingSource<Int, ComicsWithReleases>

    @Query(
        """
            SELECT * 
            FROM tComics 
            WHERE 
            removed = 0 AND selected = 1 
            AND (name LIKE :like 
                OR publisher LIKE :like 
                OR authors LIKE :like 
                OR notes LIKE :like)
            ORDER BY name COLLATE NOCASE ASC
            """
    )
    @Transaction
    fun getComicsWithReleasesPagingSource(like: String): PagingSource<Int, ComicsWithReleases>

    @Query(
        """
        SELECT distinct(publisher) 
        FROM tComics 
        WHERE publisher IS NOT NULL AND publisher <> '' 
        ORDER BY publisher
        """
    )
    suspend fun getPublishers(): List<String>

    @Query(
        """
        SELECT distinct(authors) 
        FROM tComics 
        WHERE authors IS NOT NULL AND authors <> '' 
        ORDER BY authors
        """
    )
    suspend fun getAuthors(): List<String>

}