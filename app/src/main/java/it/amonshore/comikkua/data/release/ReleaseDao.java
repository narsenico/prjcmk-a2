package it.amonshore.comikkua.data.release;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Size;
import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;
import it.amonshore.comikkua.Constants;

@Dao
public interface ReleaseDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(Release release);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    Long[] insert(Release... release);

    @Update()
    int update(Release release);

    @Update()
    int update(Release... releases);

    @Query("UPDATE tReleases SET purchased = :purchased, lastUpdate = :lastUpdate WHERE id IN (:id)")
    @Transaction
    void updatePurchased(boolean purchased, long lastUpdate, Long... id);

    @Query("UPDATE tReleases SET ordered = :ordered, lastUpdate = :lastUpdate WHERE id IN (:id)")
    @Transaction
    void updateOrdered(boolean ordered, long lastUpdate, Long... id);

    @Query("UPDATE tReleases SET removed = :removed WHERE id IN (:id)")
    @Transaction
    int updateRemoved(boolean removed, Long... id);

    @Query("UPDATE tReleases SET removed = 0")
    @Transaction
    int undoRemoved();

    @Query("DELETE FROM tReleases")
    void deleteAll();

    @Query("DELETE FROM tReleases WHERE id = :id")
    void delete(long id);

    @Query("DELETE FROM tReleases WHERE id IN (:id)")
    @Transaction
    void delete(Long... id);

    @Query("DELETE FROM tReleases WHERE comicsId = :comicsId AND number IN (:number)")
    @Transaction
    int deleteByNumber(long comicsId, int... number);

    @Query("DELETE FROM tReleases WHERE removed = 1")
    @Transaction
    int deleteRemoved();

    @Query("SELECT * FROM tReleases WHERE id = :id AND removed = 0")
    LiveData<Release> getRelease(long id);

    @Query("SELECT * FROM tReleases WHERE comicsId = :comicsId AND removed = 0 ORDER BY number ASC")
    LiveData<List<Release>> getReleases(long comicsId);

    @Query("SELECT * FROM tReleases WHERE id = :id AND removed = 0")
    Release getRawRelease(long id);

    @Query("SELECT " + DatedRelease.TYPE + " as type, * FROM vDatedReleases WHERE rpurchased = 0 AND rdate BETWEEN :startDate AND :endDate")
    List<ComicsRelease> getRawNotPurchasedReleases(@NonNull @Size(8) String startDate, @NonNull @Size(8) String endDate);

    /**
     * La query estrae:
     * - uscite non acquistate con data inferiore a refDate, oppure uscite acquistate con lastUpdate >= retainStart
     * - uscite nel periodo corrente, tra refDate e refNextDate
     * - uscite nel periodo successivo, tra refNextDate e refOtherDate
     * - uscite altri periodi, da refOtherDate
     * - uscite non aquistate senza data, oppure uscite acquistate con lastUpdate >= retainStart
     *
     * @param refDate      data di riferimento nel formato yyyyMMdd
     * @param refNextDate  data di riferimento del periodo successivo nel formato yyyyMMdd
     * @param refOtherDate data di riferimento per altri periodi nel formato yyyyMMdd
     * @param retainStart  limite inferiore per lastUpdate in ms
     * @return elenco ordinato di release
     */
    @Query("SELECT " + LostRelease.TYPE + " as type, * FROM vLostReleases WHERE rdate < :refDate AND (rpurchased = 0 OR (rpurchased = 1 AND rlastUpdate >= :retainStart)) " +
            "UNION " +
            "SELECT " + DatedRelease.TYPE + " as type, * FROM vDatedReleases WHERE rdate >= :refDate and rdate < :refNextDate " +        // questo periodo
            "UNION " +
            "SELECT " + DatedRelease.TYPE_NEXT + " as type, * FROM vDatedReleases WHERE rdate >= :refNextDate and rdate < :refOtherDate " +   // periodo successivo
            "UNION " +
            "SELECT " + DatedRelease.TYPE_OTHER + " as type, * FROM vDatedReleases WHERE rdate >= :refOtherDate " +                             // oltre
            "UNION " +
            "SELECT " + MissingRelease.TYPE + " as type, * FROM vMissingReleases WHERE rpurchased = 0 OR (rpurchased = 1 AND rlastUpdate >= :retainStart) " +
            "ORDER BY type, rdate, cname COLLATE NOCASE ASC, rnumber")
    @Transaction
    LiveData<List<ComicsRelease>> getComicsReleases(@NonNull @Size(8) String refDate,
                                                    @NonNull @Size(8) String refNextDate,
                                                    @NonNull @Size(8) String refOtherDate,
                                                    long retainStart);

    @Query("SELECT " + NotPurchasedRelease.TYPE + " as type, * FROM vNotPurchasedReleases WHERE cid = :comicsId " +
            "UNION " +
            "SELECT " + PurchasedRelease.TYPE + " as type, * FROM vPurchasedReleases WHERE cid = :comicsId " +
            "ORDER BY type, rnumber")
    @Transaction
    LiveData<List<ComicsRelease>> getComicsReleasesByComicsId(long comicsId);

    @Query("SELECT 0 as type, * FROM vComicsReleases WHERE rid in (:ids)")
    LiveData<List<ComicsRelease>> getComicsReleases(Long... ids);

    @Query("SELECT " + Constants.RELEASE_NEW +  " as type, * FROM vComicsReleases WHERE rtag = :tag " +
            "ORDER BY rdate, cname COLLATE NOCASE ASC, rnumber")
    LiveData<List<ComicsRelease>> getComicsReleasesByTag(String tag);
}
