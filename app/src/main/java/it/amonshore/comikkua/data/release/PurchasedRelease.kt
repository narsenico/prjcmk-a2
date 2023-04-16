package it.amonshore.comikkua.data.release;

import androidx.room.DatabaseView;
import it.amonshore.comikkua.Constants;

/**
 * Vista per le uscite acquistate.
 */
@DatabaseView(viewName = "vPurchasedReleases",
        value = "SELECT " +
                "tComics.id as cid, " +
                "tComics.name as cname, " +
                "tComics.series as cseries, " +
                "tComics.publisher as cpublisher, " +
                "tComics.authors as cauthors, " +
                "tComics.price as cprice, " +
                "tComics.periodicity as cperiodicity, " +
                "tComics.reserved as creserved, " +
                "tComics.notes as cnotes, " +
                "tComics.image as cimage, " +
                "tComics.lastUpdate as clastUpdate, " +
                "tComics.refJsonId as crefJsonId, " +
                "tComics.sourceId as csourceId, " +
                "tComics.selected as cselected, " +
                "tComics.version as cversion, " +
                "tReleases.id as rid, " +
                "tReleases.comicsId as rcomicsId, " +
                "tReleases.number as rnumber, " +
                "tReleases.date as rdate, " +
                "tReleases.price as rprice, " +
                "tReleases.purchased as rpurchased, " +
                "tReleases.ordered as rordered, " +
                "tReleases.notes as rnotes, " +
                "tReleases.lastUpdate as rlastUpdate, " +
                "tReleases.tag as rtag " +
                "FROM tComics INNER JOIN tReleases " +
                "ON tComics.id = tReleases.comicsId " +
                "WHERE tComics.removed = 0 AND tReleases.removed = 0 AND tComics.selected = 1 AND purchased = 1"
)
public class PurchasedRelease extends ComicsRelease {
    @Constants.ReleaseTypeDef
    public final static int TYPE = Constants.RELEASE_PURCHASED;
}
