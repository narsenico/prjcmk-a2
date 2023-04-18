package it.amonshore.comikkua.data.release

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Ignore
import androidx.room.Index
import androidx.room.PrimaryKey
import it.amonshore.comikkua.BackupExclude
import it.amonshore.comikkua.data.comics.Comics
import java.time.LocalDate

@Entity(
    tableName = "tReleases",
    foreignKeys = [ForeignKey(
        entity = Comics::class,
        parentColumns = arrayOf("id"),
        childColumns = arrayOf("comicsId"),
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index(value = ["comicsId", "number"], unique = true)]
)
data class Release(
    @PrimaryKey(autoGenerate = true) val id: Long,
    val comicsId: Long,
    val number: Int,
    val date: LocalDate? = null,
    val price: Double = 0.0,
    val purchased: Boolean = false,
    val ordered: Boolean = false,
    val notes: String? = null,
    val lastUpdate: Long = 0,
    val removed: Boolean = false,
    @BackupExclude val tag: String? = null
) {

    @Ignore
    val hasNotes = !notes.isNullOrBlank()

    companion object {
        const val NEW_RELEASE_ID = 0L

        fun create(comicsId: Long, number: Int, date: LocalDate? = null) = Release(
            id = NEW_RELEASE_ID,
            comicsId = comicsId,
            number = number,
            date = date
        )
    }
}