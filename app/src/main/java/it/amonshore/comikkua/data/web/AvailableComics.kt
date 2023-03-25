package it.amonshore.comikkua.data.web

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

/**
 * Rappresenta un comics disponibile per essere monitorato:
 * cioè le release possono essere aggiornate in automatico.
 * Una volta monitorato viene creato un record in tComcis con sourceId
 * valorizzato a tAvailableComics.id.
 */
@Entity(
    tableName = "tAvailableComics",
    indices = [Index("sourceId", unique = true), Index("name", "publisher", "version")]
)
data class AvailableComics(
    @SerializedName("id") val sourceId: String,
    @SerializedName("name") val name: String,
    @SerializedName("searchableName") val searchableName: String,
    @SerializedName("publisher") val publisher: String,
    @SerializedName("version") val version: Int,
) {
    /**
     * Id comics interno.
     * Room supporta solo id di tipo long.
     */
    @JvmField
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0

    val initial: String
        get() = if (name.isEmpty()) "" else name.substring(0, 1)

    override fun equals(other: Any?): Boolean {
        if (other === this) return true
        return if (other is AvailableComics) {
            other.sourceId == sourceId
        } else {
            false
        }
    }

    override fun hashCode(): Int {
        return sourceId.hashCode()
    }

    companion object {
        const val NO_COMICS_ID: Long = -1
    }
}