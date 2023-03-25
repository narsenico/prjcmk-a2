package it.amonshore.comikkua.data.web

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CmkWebDaoKt {

//    @Insert(onConflict = OnConflictStrategy.REPLACE)
//    suspend fun insert(comics: AvailableComics): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(comics: List<AvailableComics>)

    @Query("DELETE FROM tAvailableComics")
    suspend fun deleteAll()

//    @Transaction
//    suspend fun refresh(comics: List<AvailableComics>) {
//        deleteAll()
//        insert(comics)
//    }

//    @Query("SELECT * FROM tAvailableComics " +
//            "ORDER BY name COLLATE NOCASE ASC, publisher COLLATE NOCASE ASC, version")
//    fun getAvailableComicsPagingSource(): PagingSource<Int, AvailableComics>

//    @Query("SELECT * FROM tAvailableComics WHERE" +
//            "(name LIKE :likeName OR publisher LIKE :likeName) " +
//            "ORDER BY name COLLATE NOCASE ASC, publisher COLLATE NOCASE ASC, version")
//    fun getAvailableComicsPagingSource(likeName: String): PagingSource<Int, AvailableComics>

//    @Query("SELECT * FROM tAvailableComics " +
//            "ORDER BY name COLLATE NOCASE ASC, publisher COLLATE NOCASE ASC, version")
//    fun getAvailableComicsFLow(): Flow<List<AvailableComics>>

    @Query(
        """
        SELECT a.* 
        FROM tAvailableComics a LEFT OUTER JOIN tComics t ON a.sourceId = t.sourceId
        WHERE 
        t.sourceId IS NULL OR t.selected = 0
        ORDER BY a.name COLLATE NOCASE ASC, a.publisher COLLATE NOCASE ASC, a.version
    """
    )
    fun getNotFollowedComics(): Flow<List<AvailableComics>>
}