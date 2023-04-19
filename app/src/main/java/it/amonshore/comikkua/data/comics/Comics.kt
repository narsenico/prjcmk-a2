package it.amonshore.comikkua.data.comics

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.Index
import androidx.room.PrimaryKey
import it.amonshore.comikkua.BackupExclude

@Entity(tableName = "tComics", indices = [Index("name"), Index("sourceId")])
data class Comics(
    @PrimaryKey(autoGenerate = true) val id: Long,
    val name: String,
    val series: String = "",
    val publisher: String = "",
    val authors: String = "",
    val price: Double = 0.0,
    val periodicity: String? = null,
    val reserved: Boolean = false,
    val notes: String = "",
    @BackupExclude val image: String? = null,
    val lastUpdate: Long = 0,
    val refJsonId: Long = 0,
    val removed: Boolean = false,
    @BackupExclude val tag: String? = null,
    val sourceId: String? = null,
    val selected: Boolean = false,
    val version: Int = 0
) {

    @Ignore val initial = if (name.isEmpty()) "" else name.substring(0, 1)
    @Ignore val isSourced = !sourceId.isNullOrEmpty()

    companion object {
        const val NO_COMICS_ID = -1L
        const val NEW_COMICS_ID = 0L
    }
}