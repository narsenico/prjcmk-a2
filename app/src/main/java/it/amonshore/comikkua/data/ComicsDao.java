package it.amonshore.comikkua.data;

import java.util.List;

import androidx.lifecycle.LiveData;
import androidx.paging.DataSource;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;

@Dao
public interface ComicsDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Comics comics);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Comics... comics);

    @Update
    void update(Comics comics);

    @Update
    void update(Comics... comics);

    @Query("DELETE FROM tComics")
    void deleteAll();

    @Query("DELETE FROM tComics where id = :id")
    void delete(long id);

    @Query("DELETE FROM tComics where id in (:id)")
    @Transaction
    void delete(Long... id);

    @Query("SELECT * from tComics ORDER BY name COLLATE NOCASE ASC")
    LiveData<List<Comics>> getComics();

    @Query("SELECT * from tComics where id = :id")
    LiveData<Comics> getComics(long id);

    @Query("SELECT * from tComics ORDER BY name COLLATE NOCASE ASC")
    List<Comics> getRawComics();

    @Query("SELECT * from tComics ORDER BY name COLLATE NOCASE ASC")
    @Transaction
    LiveData<List<ComicsWithReleases>> getComicsWithReleases();

    @Query("SELECT * from tComics ORDER BY name COLLATE NOCASE ASC")
    @Transaction
    DataSource.Factory<Integer, ComicsWithReleases> comicsWithReleases();

    @Query("SELECT * from tComics where name like :likeName ORDER BY name COLLATE NOCASE ASC")
    @Transaction
    DataSource.Factory<Integer, ComicsWithReleases> comicsWithReleases(String likeName);

    @Query("SELECT * from tComics ORDER BY name COLLATE NOCASE ASC")
    @Transaction
    List<ComicsWithReleases> getRawComicsWithReleases();

    @Query("SELECT * from tComics where name = :name")
    @Transaction
    LiveData<List<ComicsWithReleases>> getComicsWithReleasesByName(String name);

    @Query("SELECT * from tComics where id = :id")
    @Transaction
    LiveData<ComicsWithReleases> getComicsWithReleases(long id);
}
