package it.amonshore.comikkua.data.release

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy

@Dao
interface ReleaseDaoKt {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(release: Release): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(releases: List<Release>)
}