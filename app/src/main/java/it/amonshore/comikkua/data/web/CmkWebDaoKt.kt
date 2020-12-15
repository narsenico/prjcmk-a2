package it.amonshore.comikkua.data.web

import androidx.paging.PagingSource
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
abstract class CmkWebDaoKt {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insert(comics: AvailableComics): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insert(vararg comics: AvailableComics)

    @Query("DELETE FROM tAvailableComics")
    abstract fun deleteAll(): Int

    @Transaction
    open fun refresh(vararg comics: AvailableComics) {
        deleteAll()
        insert(*comics)
    }

    @Query("SELECT * FROM tAvailableComics " +
            "ORDER BY name COLLATE NOCASE ASC, publisher COLLATE NOCASE ASC, version")
    abstract fun getAvailableComicsPagingSource(): PagingSource<Int, AvailableComics>

    @Query("SELECT * FROM tAvailableComics WHERE" +
            "(name LIKE :likeName OR publisher LIKE :likeName) " +
            "ORDER BY name COLLATE NOCASE ASC, publisher COLLATE NOCASE ASC, version")
    abstract fun getAvailableComicsPagingSource(likeName: String): PagingSource<Int, AvailableComics>

    @Query("SELECT * FROM tAvailableComics " +
            "ORDER BY name COLLATE NOCASE ASC, publisher COLLATE NOCASE ASC, version")
    abstract fun getAvailableComicsFLow(): Flow<List<AvailableComics>>
}