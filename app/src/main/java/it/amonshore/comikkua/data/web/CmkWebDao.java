package it.amonshore.comikkua.data.web;

import androidx.paging.PagingSource;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;

@Dao
public abstract class CmkWebDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public abstract Long insert(AvailableComics comics);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public abstract void insert(AvailableComics... comics);

    @Query("DELETE FROM tavailablecomics")
    public abstract int deleteAll();

    @Transaction
    public void refresh(AvailableComics... comics) {
        deleteAll();
        insert(comics);
    }

    @Query("SELECT * FROM tavailablecomics " +
            "ORDER BY name COLLATE NOCASE ASC, publisher COLLATE NOCASE ASC, version")
    @Transaction
    public abstract PagingSource<Integer, AvailableComics> getAvailableComicsPagingSource();

    @Query("SELECT * FROM tavailablecomics WHERE" +
            "(name LIKE :likeName OR publisher LIKE :likeName) " +
            "ORDER BY name COLLATE NOCASE ASC, publisher COLLATE NOCASE ASC, version")
    @Transaction
    public abstract PagingSource<Integer, AvailableComics> getAvailableComicsPagingSource(String likeName);
}