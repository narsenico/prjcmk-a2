package it.amonshore.comikkua.data;

import java.util.List;

import androidx.lifecycle.LiveData;
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

    @Query("DELETE FROM tReleases where id = :id")
    void delete(long id);

    @Query("DELETE FROM tReleases where id in (:id)")
    @Transaction
    void delete(Long... id);

    @Query("SELECT * from tReleases where comicsId = :comicsId ORDER BY number ASC")
    LiveData<List<Release>> getReleases(long comicsId);
}
