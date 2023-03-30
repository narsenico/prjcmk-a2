package it.amonshore.comikkua.data.release

import androidx.annotation.Size
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ReleaseDaoKt {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(release: Release): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(releases: List<Release>)

    @Query("UPDATE tReleases SET purchased = :purchased, lastUpdate = :lastUpdate WHERE id IN (:ids)")
    @Transaction
    suspend fun updatePurchased(ids: List<Long>, purchased: Boolean, lastUpdate: Long)

    @Query("UPDATE tReleases SET ordered = :ordered, lastUpdate = :lastUpdate WHERE id IN (:ids)")
    @Transaction
    suspend fun updateOrdered(ids: List<Long>, ordered: Boolean, lastUpdate: Long);

    @Query("DELETE FROM tReleases WHERE removed = 1")
    @Transaction
    suspend fun deleteRemoved()

    @Query("UPDATE tReleases SET removed = 0")
    @Transaction
    suspend fun undoRemoved()

    @Query("UPDATE tReleases SET removed = :removed WHERE id IN (:ids)")
    @Transaction
    suspend fun updateRemoved(ids: List<Long>, removed: Boolean): Int

    @Query("SELECT * FROM tReleases WHERE id = :id AND removed = 0")
    suspend fun getRelease(id: Long): Release

    @Query("SELECT 0 as type, * FROM vComicsReleases WHERE rid in (:ids)")
    suspend fun getComicsReleases(ids: List<Long>): List<ComicsRelease>

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