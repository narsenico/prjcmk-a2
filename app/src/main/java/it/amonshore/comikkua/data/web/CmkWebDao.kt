package it.amonshore.comikkua.data.web

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CmkWebDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(comics: List<AvailableComics>)

    @Query("DELETE FROM tAvailableComics")
    suspend fun deleteAll()

    @Query(
        """
        SELECT * FROM tAvailableComics
        ORDER BY name COLLATE NOCASE ASC, publisher COLLATE NOCASE ASC, version
    """
    )
    suspend fun getAvailableComicsList(): List<AvailableComics>

    @Query(
        """
        SELECT a.* 
        FROM tAvailableComics a LEFT OUTER JOIN tComics t ON a.sourceId = t.sourceId
        WHERE 
        t.sourceId IS NULL OR t.selected = 0
        ORDER BY a.name COLLATE NOCASE ASC, a.publisher COLLATE NOCASE ASC, a.version
    """
    )
    fun getNotFollowedComicsFLow(): Flow<List<AvailableComics>>
}