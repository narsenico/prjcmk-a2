package it.amonshore.comikkua.data.release;

import androidx.room.DatabaseView;
import it.amonshore.comikkua.Constants;

/**
 * Vista per tutte le release - sia acquistate che non - con una data di uscita.
 */
@DatabaseView(viewName = "vDatedReleases",
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
                "WHERE tComics.removed = 0 AND tReleases.removed = 0 " +
                "AND (date is not null and date <> '')"
)
public class DatedRelease extends ComicsRelease {
    @Constants.ReleaseTypeDef
    public final static int TYPE = Constants.RELEASE_DATED;
    @Constants.ReleaseTypeDef
    public final static int TYPE_NEXT = Constants.RELEASE_DATED_NEXT;
    @Constants.ReleaseTypeDef
    public final static int TYPE_OTHER = Constants.RELEASE_OTHER;
}
