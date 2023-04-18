package it.amonshore.comikkua.data.web

import androidx.room.Entity
import androidx.room.Ignore
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
    @SerializedName("ref_id") val sourceId: String,
    @SerializedName("title") val name: String,
    @SerializedName("editor") val publisher: String,
    @SerializedName("reissue") val version: Int,
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

    @delegate:Ignore
    val searchableName: String by lazy {
        "${name.uppercase()} ${publisher.uppercase()}"
    }

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
}