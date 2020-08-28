package it.amonshore.comikkua.data.release;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.DatabaseView;
import androidx.room.Embedded;
import it.amonshore.comikkua.data.comics.Comics;

/**
 * Vista per le uscite.
 */
@DatabaseView(viewName = "vComicsReleases",
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
                "WHERE tComics.removed = 0 AND tReleases.removed = 0"
)
public class ComicsRelease implements IReleaseViewModelItem {
    public final static int ITEM_TYPE = 2;

    /**
     * Indica la tipologia del dato estratto dalla query per un raggruppamento logico:
     * release persa, mancante, di questa settimana, etc.
     */
    public int type;

    @Embedded(prefix = "c")
    public Comics comics;

    @Embedded(prefix = "r")
    public Release release;

    @Override
    public long getId() {
        return this.release.id;
    }

    @Override
    public int getItemType() {
        return ITEM_TYPE;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj == this) return true;

        if (obj instanceof ComicsRelease) {
            final ComicsRelease other = (ComicsRelease) obj;
            return other.comics.id == this.comics.id &&
                    other.release.id == this.release.id &&
                    other.comics.lastUpdate == this.comics.lastUpdate &&
                    other.release.lastUpdate == this.release.lastUpdate;
        } else {
            return false;
        }
    }

    /**
     * Creo una nuova istanza di {@link ComicsRelease#release} inizializzato.
     *
     * @param comics comics a cui appartiene la release
     * @return nuova istanza
     */
    public static ComicsRelease createNew(@NonNull Comics comics) {
        final ComicsRelease cr = new ComicsRelease();
        cr.comics = comics;
        cr.release = Release.create(comics.id, 0);
        return cr;
    }
}
