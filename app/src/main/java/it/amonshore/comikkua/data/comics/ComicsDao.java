package it.amonshore.comikkua.data.comics;

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

    @Query("DELETE FROM tComics WHERE id = :id")
    void delete(long id);

    @Query("DELETE FROM tComics WHERE id in (:id)")
    @Transaction
    void delete(Long... id);

    @Query("SELECT * FROM tComics ORDER BY name COLLATE NOCASE ASC")
    LiveData<List<Comics>> getComics();

    @Query("SELECT * FROM tComics WHERE id = :id")
    LiveData<Comics> getComics(long id);

    @Query("SELECT * FROM tComics WHERE name = :name")
    LiveData<Comics> getComics(String name);

    @Query("SELECT * FROM tComics WHERE refJsonId = :refJsonId")
    Comics getRawComicsByRefJsonId(long refJsonId);

    @Query("SELECT * FROM tComics ORDER BY name COLLATE NOCASE ASC")
    List<Comics> getRawComics();

    @Query("SELECT * FROM tComics ORDER BY name COLLATE NOCASE ASC")
    @Transaction
    LiveData<List<ComicsWithReleases>> getComicsWithReleases();

    @Query("SELECT * FROM tComics ORDER BY name COLLATE NOCASE ASC")
    @Transaction
    DataSource.Factory<Integer, ComicsWithReleases> comicsWithReleases();

    @Query("SELECT * FROM tComics WHERE name LIKE :likeName OR publisher LIKE :likeName OR authors LIKE :likeName ORDER BY name COLLATE NOCASE ASC")
    @Transaction
    DataSource.Factory<Integer, ComicsWithReleases> comicsWithReleases(String likeName);

    @Query("SELECT * FROM tComics WHERE name = :name")
    @Transaction
    LiveData<List<ComicsWithReleases>> getComicsWithReleasesByName(String name);

    @Query("SELECT * FROM tComics WHERE id = :id")
    @Transaction
    LiveData<ComicsWithReleases> getComicsWithReleases(long id);

    @Query("SELECT distinct(publisher) FROM tComics WHERE publisher <> '' ORDER BY publisher")
    LiveData<List<String>> getPublishers();
}
