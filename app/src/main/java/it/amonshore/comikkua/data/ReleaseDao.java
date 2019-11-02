package it.amonshore.comikkua.data;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.paging.DataSource;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;

@Dao
public interface ReleaseDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Release release);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Release... release);

    @Query("DELETE FROM tReleases")
    void deleteAll();

    @Query("DELETE FROM tReleases WHERE id = :id")
    void delete(long id);

    @Query("DELETE FROM tReleases WHERE id IN (:id)")
    @Transaction
    void delete(Long... id);

    @Query("SELECT * FROM tReleases WHERE comicsId = :comicsId ORDER BY number ASC")
    LiveData<List<Release>> getReleases(long comicsId);

    @Query("SELECT * FROM vMissingReleases")
    LiveData<List<MissingRelease>> getMissingReleases();

    @Query("SELECT * FROM vLostReleases")
    LiveData<List<LostRelease>> getLostReleases();

    @Query("SELECT * FROM vLostReleases WHERE rdate BETWEEN :fromDate AND :toDate")
    LiveData<List<LostRelease>> getLostReleases(@NonNull String fromDate, @NonNull String toDate);

    @Query("SELECT * FROM vLostReleases WHERE rdate >= :fromDate")
    LiveData<List<LostRelease>> getLostReleasesFrom(@NonNull String fromDate);

    // TODO: passsare come parametro currentWeekStart/End, nextWeekStart/End e aggiungere condizioni
    //  deve estrarre:
    //  vLostReleases where date < currentWeekStart (persi)
    //  v(da fare) where date between currentWeekStart and currentWeekEnd (TIPO 3)
    //  v(da fare) where date between nextWeekStart and nextWeekEnd (TIPO 3)
    //  vMissingReleases
    @Query("SELECT * FROM vLostReleases UNION SELECT * FROM vMissingReleases ORDER BY type")
    @Transaction
    DataSource.Factory<Integer, ComicsRelease> allReleases();

}
