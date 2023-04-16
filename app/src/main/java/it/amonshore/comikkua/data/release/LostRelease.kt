package it.amonshore.comikkua.data.release

import androidx.room.DatabaseView
import it.amonshore.comikkua.Constants
import it.amonshore.comikkua.Constants.ReleaseTypeDef
import it.amonshore.comikkua.data.comics.Comics

/**
 * Vista per le uscite con data non ancora acquistate.
 */
@DatabaseView(
    viewName = "vLostReleases",
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
        AND (date is not null and date <> '')
        """
)
class LostRelease(
    type: Int,
    comics: Comics,
    release: Release,
) : ComicsRelease(type, comics, release) {
    companion object {
        @ReleaseTypeDef
        const val TYPE = Constants.RELEASE_LOST
    }
}