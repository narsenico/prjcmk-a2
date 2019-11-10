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

@Dao
public interface ReleaseDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Release release);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Release... release);

    @Update()
    void update(Release release);

    @Update()
    void update(Release... releases);

    @Query("UPDATE tReleases SET purchased = :purchased, lastUpdate = :lastUpdate WHERE id IN (:id)")
    @Transaction
    void updatePurchased(boolean purchased, long lastUpdate, Long... id);

    @Query("UPDATE tReleases SET ordered = :ordered, lastUpdate = :lastUpdate WHERE id IN (:id)")
    @Transaction
    void updateOrdered(boolean ordered, long lastUpdate, Long... id);

    @Query("DELETE FROM tReleases")
    void deleteAll();

    @Query("DELETE FROM tReleases WHERE id = :id")
    void delete(long id);

    @Query("DELETE FROM tReleases WHERE id IN (:id)")
    @Transaction
    void delete(Long... id);

    @Query("SELECT * FROM tReleases WHERE id = :id")
    LiveData<Release> getRelease(long id);

    @Query("SELECT * FROM tReleases WHERE comicsId = :comicsId ORDER BY number ASC")
    LiveData<List<Release>> getReleases(long comicsId);

    @Query("SELECT * FROM tReleases WHERE id = :id")
    Release getRawRelease(long id);

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
    @Query("SELECT 10 as type, * FROM vLostReleases WHERE rdate < :refDate AND (rpurchased = 0 OR (rpurchased = 1 AND rlastUpdate >= :retainStart)) " +
            "UNION " +
            "SELECT 20 as type, * FROM vDatedReleases WHERE rdate >= :refDate and rdate < :refNextDate " +        // questo periodo
            "UNION " +
            "SELECT 21 as type, * FROM vDatedReleases WHERE rdate >= :refNextDate and rdate < :refOtherDate " +   // periodo successivo
            "UNION " +
            "SELECT 22 as type, * FROM vDatedReleases WHERE rdate > :refOtherDate " +                             // oltre
            "UNION " +
            "SELECT 100 as type, * FROM vMissingReleases WHERE rpurchased = 0 OR (rpurchased = 1 AND rlastUpdate >= :retainStart) " +
            "ORDER BY type, rdate, cname COLLATE NOCASE ASC, rnumber")
    @Transaction
    LiveData<List<ComicsRelease>> getAllReleases(@NonNull @Size(8) String refDate,
                                                 @NonNull @Size(8) String refNextDate,
                                                 @NonNull @Size(8) String refOtherDate,
                                                 long retainStart);
}
