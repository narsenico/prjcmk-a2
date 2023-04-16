package it.amonshore.comikkua.data.release

import androidx.annotation.Size
import androidx.room.*
import it.amonshore.comikkua.Constants
import kotlinx.coroutines.flow.Flow

@Dao
interface ReleaseDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(release: Release): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(releases: List<Release>)

    @Query("UPDATE tReleases SET purchased = :purchased, lastUpdate = :lastUpdate WHERE id IN (:ids)")
    @Transaction
    suspend fun updatePurchased(ids: List<Long>, purchased: Boolean, lastUpdate: Long)

    @Query("UPDATE tReleases SET ordered = :ordered, lastUpdate = :lastUpdate WHERE id IN (:ids)")
    @Transaction
    suspend fun updateOrdered(ids: List<Long>, ordered: Boolean, lastUpdate: Long)

    @Query("DELETE FROM tReleases WHERE removed = 1 AND tag = :tag")
    @Transaction
    suspend fun deleteRemoved(tag: String)

    @Query("UPDATE tReleases SET removed = 0 WHERE tag = :tag")
    @Transaction
    suspend fun undoRemoved(tag: String)

    @Query("UPDATE tReleases SET removed = 1, tag = :tag WHERE id IN (:ids)")
    @Transaction
    suspend fun markedAsRemoved(ids: List<Long>, tag: String): Int

    @Query("SELECT * FROM tReleases WHERE id = :id AND removed = 0")
    suspend fun getRelease(id: Long): Release

    @Query("SELECT 0 as type, * FROM vComicsReleases WHERE rid in (:ids)")
    suspend fun getComicsReleases(ids: List<Long>): List<ComicsRelease>

    @Query("SELECT ${DatedRelease.TYPE} as type, * FROM vDatedReleases WHERE rpurchased = 0 AND rdate BETWEEN :startDate AND :endDate")
    suspend fun getNotPurchasedComicsReleases(@Size(8) startDate: String, @Size(8) endDate: String): List<ComicsRelease>

    @Query(
        """SELECT ${LostRelease.TYPE} as type, * 
           FROM vLostReleases 
           WHERE 
           rdate < :refDate 
           AND (rpurchased = 0 OR (rpurchased = 1 AND rlastUpdate >= :retainStart)) 
           UNION 
           SELECT ${DatedRelease.TYPE} as type, * 
           FROM vDatedReleases 
           WHERE 
           rdate >= :refDate 
           and rdate < :refNextDate 
           UNION 
           SELECT ${DatedRelease.TYPE_NEXT} as type, * 
           FROM vDatedReleases 
           WHERE 
           rdate >= :refNextDate and rdate < :refOtherDate 
           UNION 
           SELECT ${DatedRelease.TYPE_OTHER} as type, * 
           FROM vDatedReleases 
           WHERE 
           rdate >= :refOtherDate 
           UNION 
           SELECT ${MissingRelease.TYPE} as type, * 
           FROM vMissingReleases 
           WHERE rpurchased = 0 OR (rpurchased = 1 AND rlastUpdate >= :retainStart) 
           ORDER BY type, rdate, cname COLLATE NOCASE ASC, rnumber
           """
    )
    @Transaction
    suspend fun getNotableComicsReleases(
        @Size(8) refDate: String,
        @Size(8) refNextDate: String,
        @Size(8) refOtherDate: String,
        retainStart: Long
    ): List<ComicsRelease>

    @Query(
        """
            SELECT ${NotPurchasedRelease.TYPE} as type, * FROM vNotPurchasedReleases WHERE cid = :comicsId
            UNION
            SELECT ${PurchasedRelease.TYPE} as type, * FROM vPurchasedReleases WHERE cid = :comicsId
            ORDER BY type, rnumber
            """
    )
    @Transaction
    suspend fun getComicsReleasesByComicsId(comicsId: Long): List<ComicsRelease>

    @Query(
        """SELECT ${LostRelease.TYPE} as type, * 
           FROM vLostReleases 
           WHERE 
           rdate < :refDate 
           AND (rpurchased = 0 OR (rpurchased = 1 AND rlastUpdate >= :retainStart)) 
           UNION 
           SELECT ${DatedRelease.TYPE} as type, * 
           FROM vDatedReleases 
           WHERE 
           rdate >= :refDate 
           and rdate < :refNextDate 
           UNION 
           SELECT ${DatedRelease.TYPE_NEXT} as type, * 
           FROM vDatedReleases 
           WHERE 
           rdate >= :refNextDate and rdate < :refOtherDate 
           UNION 
           SELECT ${DatedRelease.TYPE_OTHER} as type, * 
           FROM vDatedReleases 
           WHERE 
           rdate >= :refOtherDate 
           UNION 
           SELECT ${MissingRelease.TYPE} as type, * 
           FROM vMissingReleases 
           WHERE rpurchased = 0 OR (rpurchased = 1 AND rlastUpdate >= :retainStart) 
           ORDER BY type, rdate, cname COLLATE NOCASE ASC, rnumber
           """
    )
    @Transaction
    fun getNotableComicsReleasesFlow(
        @Size(8) refDate: String,
        @Size(8) refNextDate: String,
        @Size(8) refOtherDate: String,
        retainStart: Long
    ): Flow<List<ComicsRelease>>

    @Query(
        """SELECT ${Constants.RELEASE_NEW} as type, * 
              FROM vComicsReleases 
              WHERE rtag = :tag 
              ORDER BY rdate, cname COLLATE NOCASE ASC, rnumber
              """
    )
    fun getComicsReleasesByTagFlow(tag: String): Flow<List<ComicsRelease>>

    @Query(
        """
            SELECT ${NotPurchasedRelease.TYPE} as type, * FROM vNotPurchasedReleases WHERE cid = :comicsId
            UNION
            SELECT ${PurchasedRelease.TYPE} as type, * FROM vPurchasedReleases WHERE cid = :comicsId
            ORDER BY type, rnumber
            """
    )
    @Transaction
    fun getComicsReleasesByComicsIdFLow(comicsId: Long): Flow<List<ComicsRelease>>
}