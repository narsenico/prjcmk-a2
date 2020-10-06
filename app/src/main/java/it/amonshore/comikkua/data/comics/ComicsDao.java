package it.amonshore.comikkua.data.comics;

import java.util.List;

import androidx.lifecycle.LiveData;
import androidx.paging.DataSource;
import androidx.paging.PagingSource;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;

@Dao
public interface ComicsDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(Comics comics);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    Long[] insert(Comics... comics);

    @Update
    int update(Comics comics);

    @Update
    int update(Comics... comics);

    @Query("UPDATE tComics SET removed = :removed WHERE id IN (:id)")
    @Transaction
    int updateRemoved(boolean removed, Long... id);

    @Query("UPDATE tComics SET removed = 0")
    @Transaction
    int undoRemoved();

    @Query("DELETE FROM tComics")
    int deleteAll();

    @Query("DELETE FROM tComics WHERE id = :id")
    int delete(long id);

    @Query("DELETE FROM tComics WHERE id in (:id)")
    @Transaction
    int delete(Long... id);

    @Query("DELETE FROM tComics WHERE removed = 1")
    @Transaction
    int deleteRemoved();

    @Query("SELECT * FROM tComics WHERE removed = 0 ORDER BY name COLLATE NOCASE ASC")
    LiveData<List<Comics>> getComics();

    @Query("SELECT * FROM tComics WHERE id = :id AND removed = 0")
    LiveData<Comics> getComics(long id);

    @Query("SELECT * FROM tComics WHERE name = :name AND removed = 0")
    LiveData<Comics> getComics(String name);

    @Query("SELECT * FROM tComics WHERE removed = 0 ORDER BY name COLLATE NOCASE ASC")
    List<Comics> getRawComics();

    @Query("SELECT * FROM tComics WHERE removed = 0 ORDER BY name COLLATE NOCASE ASC")
    @Transaction
    List<ComicsWithReleases> getRawComicsWithReleases();

    @Query("SELECT * FROM tComics WHERE removed = 0 ORDER BY name COLLATE NOCASE ASC")
    @Transaction
    LiveData<List<ComicsWithReleases>> getComicsWithReleases();

    @Query("SELECT * FROM tComics WHERE removed = 0 ORDER BY name COLLATE NOCASE ASC")
    @Transaction
    PagingSource<Integer, ComicsWithReleases> getComicsWithReleasesPagingSource();

    @Query("SELECT * FROM tComics WHERE removed = 0 AND " +
            "(name LIKE :likeName OR publisher LIKE :likeName OR authors LIKE :likeName OR notes LIKE :likeName) " +
            "ORDER BY name COLLATE NOCASE ASC")
    @Transaction
    PagingSource<Integer, ComicsWithReleases> getComicsWithReleasesPagingSource(String likeName);

    @Query("SELECT * FROM tComics WHERE name = :name AND removed = 0")
    @Transaction
    LiveData<List<ComicsWithReleases>> getComicsWithReleasesByName(String name);

    @Query("SELECT * FROM tComics WHERE id = :id AND removed = 0")
    @Transaction
    LiveData<ComicsWithReleases> getComicsWithReleases(long id);

    @Query("SELECT distinct(publisher) FROM tComics WHERE publisher IS NOT NULL AND publisher <> '' ORDER BY publisher")
    LiveData<List<String>> getPublishers();

    @Query("SELECT distinct(authors) FROM tComics WHERE authors IS NOT NULL AND authors <> '' ORDER BY authors")
    LiveData<List<String>> getAuthors();

    @Query("SELECT distinct(name) FROM tComics ORDER BY name COLLATE NOCASE ASC")
    LiveData<List<String>> getComicsName();
}
