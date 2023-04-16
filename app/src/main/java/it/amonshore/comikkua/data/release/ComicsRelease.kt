package it.amonshore.comikkua.data.release

import androidx.room.DatabaseView
import androidx.room.Embedded
import androidx.room.Ignore
import it.amonshore.comikkua.data.comics.Comics
import java.util.Objects

/**
 * Vista per le uscite.
 */
@DatabaseView(
    viewName = "vComicsReleases",
    value = """
        SELECT
        tComics.id as cid,
        tComics.name as cname,
        tComics.series as cseries,
        tComics.publisher as cpublisher,
        tComics.authors as cauthors,
        tComics.price as cprice,
        tComics.periodicity as cperiodicity,
        tComics.reserved as creserved,
        tComics.notes as cnotes,
        tComics.image as cimage,
        tComics.lastUpdate as clastUpdate,
        tComics.refJsonId as crefJsonId,
        tComics.sourceId as csourceId,
        tComics.selected as cselected,
        tComics.version as cversion,
        tComics.removed as cremoved,
        tComics.tag as ctag,
        tReleases.id as rid,
        tReleases.comicsId as rcomicsId,
        tReleases.number as rnumber,
        tReleases.date as rdate,
        tReleases.price as rprice,
        tReleases.purchased as rpurchased,
        tReleases.ordered as rordered,
        tReleases.notes as rnotes,
        tReleases.lastUpdate as rlastUpdate,
        tReleases.tag as rtag,
        tReleases.removed as rremoved
        FROM tComics INNER JOIN tReleases
        ON tComics.id = tReleases.comicsId
        WHERE tComics.removed = 0 AND tReleases.removed = 0 AND tComics.selected = 1
        """
)
open class ComicsRelease(
    /**
     * Indica la tipologia del dato estratto dalla query per un raggruppamento logico:
     * release persa, mancante, di questa settimana, etc.
     */
    val type: Int,
    @Embedded(prefix = "c") val comics: Comics,
    @Embedded(prefix = "r") val release: Release,
) : IReleaseViewModelItem {
    @Ignore override val id: Long = release.id
    @Ignore override val itemType = ITEM_TYPE

    override fun equals(other: Any?): Boolean {
        if (other === this) return true
        if (other is ComicsRelease) {
            return other.comics == comics && other.release == release
        }
        return false
    }

    override fun hashCode(): Int {
        return Objects.hash(comics, release)
    }

    companion object {
        const val ITEM_TYPE = 2
    }
}