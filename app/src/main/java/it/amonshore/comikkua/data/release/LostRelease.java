package it.amonshore.comikkua.data.release;

import androidx.room.DatabaseView;

/**
 * Vista per le uscite con data non ancora acquistate.
 */
@DatabaseView(viewName = "vLostReleases",
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
                "tReleases.id as rid, " +
                "tReleases.comicsId as rcomicsId, " +
                "tReleases.number as rnumber, " +
                "tReleases.date as rdate, " +
                "tReleases.price as rprice, " +
                "tReleases.purchased as rpurchased, " +
                "tReleases.ordered as rordered, " +
                "tReleases.notes as rnotes, " +
                "tReleases.lastUpdate as rlastUpdate " +
                "FROM tComics INNER JOIN tReleases " +
                "ON tComics.id = tReleases.comicsId " +
                "WHERE (date is not null and date <> '') " +
                "ORDER BY name COLLATE NOCASE ASC, number ASC")
public class LostRelease extends ComicsRelease {
    public final static int TYPE = 10;
}
