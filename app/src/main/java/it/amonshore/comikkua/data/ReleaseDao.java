package it.amonshore.comikkua.data;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Size;
import androidx.lifecycle.LiveData;
import androidx.paging.DataSource;
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
    void update(boolean purchased, long lastUpdate, Long... id);

    @Query("DELETE FROM tReleases")
    void deleteAll();

    @Query("DELETE FROM tReleases WHERE id = :id")
    void delete(long id);

    @Query("DELETE FROM tReleases WHERE id IN (:id)")
    @Transaction
    void delete(Long... id);

    @Query("SELECT * FROM tReleases WHERE comicsId = :comicsId ORDER BY number ASC")
    LiveData<List<Release>> getReleases(long comicsId);

    /**
     * La query estrae:
     * - uscite non acquistate con data inferiore a refDate,
     * oppure uscite acquistate con lastUpdate >= retainStart
     * - uscite con data superiore a refDate
     * - uscite non aquistate senza data,
     * oppure uscite acquistate con lastUpdate >= retainStart
     *
     * @param refDate     data di riferimento nel formato yyyyMMdd
     * @param retainStart limite inferiore per lastUpdate in ms
     * @return elenco ordinato di release
     */
    @Query("SELECT * FROM vLostReleases WHERE rdate < :refDate AND (rpurchased = 0 OR (rpurchased = 1 AND rlastUpdate >= :retainStart)) " +
            "UNION " +
            "SELECT * FROM vDatedReleases WHERE rdate >= :refDate " +
            "UNION " +
            "SELECT * FROM vMissingReleases WHERE rpurchased = 0 OR (rpurchased = 1 AND rlastUpdate >= :retainStart) " +
            "ORDER BY type")
    @Transaction
    LiveData<List<ComicsRelease>> getAllReleases(@NonNull @Size(6) String refDate,
                                                 long retainStart);
}
