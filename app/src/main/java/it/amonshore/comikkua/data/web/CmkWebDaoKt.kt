package it.amonshore.comikkua.data.web

import androidx.paging.PagingSource
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CmkWebDaoKt {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(comics: AvailableComics): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(comics: List<AvailableComics>)

    @Query("DELETE FROM tAvailableComics")
    suspend fun deleteAll()

    @Transaction
    suspend fun refresh(comics: List<AvailableComics>) {
        deleteAll()
        insert(comics)
    }

    @Query("SELECT * FROM tAvailableComics " +
            "ORDER BY name COLLATE NOCASE ASC, publisher COLLATE NOCASE ASC, version")
    fun getAvailableComicsPagingSource(): PagingSource<Int, AvailableComics>

    @Query("SELECT * FROM tAvailableComics WHERE" +
            "(name LIKE :likeName OR publisher LIKE :likeName) " +
            "ORDER BY name COLLATE NOCASE ASC, publisher COLLATE NOCASE ASC, version")
    fun getAvailableComicsPagingSource(likeName: String): PagingSource<Int, AvailableComics>

    @Query("SELECT * FROM tAvailableComics " +
            "ORDER BY name COLLATE NOCASE ASC, publisher COLLATE NOCASE ASC, version")
    fun getAvailableComicsFLow(): Flow<List<AvailableComics>>
}