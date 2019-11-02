package it.amonshore.comikkua.data;

import androidx.room.DatabaseView;

@DatabaseView(viewName = "vLostReleases",
        value = "SELECT " +
                "1 as type, " +
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
                "tReleases.flags as rflags, " +
                "tReleases.notes as rnotes, " +
                "tReleases.lastUpdate as rlastUpdate " +
                "FROM tComics INNER JOIN tReleases " +
                "ON tComics.id = tReleases.comicsId " +
                "WHERE (date is not null and date <> '') and flags <> " + Release.FLAG_PURCHASED + " " +
                "ORDER BY name COLLATE NOCASE ASC, number ASC")
public class LostRelease extends ComicsRelease {
    public final static int TYPE = 1;
}
