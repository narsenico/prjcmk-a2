package it.amonshore.comikkua.data;

import java.util.List;

import androidx.room.Embedded;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.Relation;

@Entity
public class ComicsWithReleases {
    @Embedded
    public Comics comics;

    /**
     * Tutte le release associate al comics.
     * Si presuppone che siano ordinate per number, visto che è stato definito un indice a livello di entity.
     */
    @Relation(parentColumn = "id", entityColumn = "comicsId", entity = Release.class)
    public List<Release> releases;

    @Ignore
    public Release getLastPurchasedRelease() {
        // TODO: per ora voglio solo controllare in che ordine sono messe le release
        return this.releases == null || this.releases.size() == 0 ? null : this.releases.get(this.releases.size() - 1);
    }

    @Ignore
    public Release getNextToPurchaseRelease() {
        // TODO: per ora voglio solo controllare in che ordine sono messe le release
        return this.releases == null || this.releases.size() == 0 ? null : this.releases.get(0);
    }

    /**
     *
     * @return numero di uscite non ancora acquistate
     */
    @Ignore
    public int getMissingReleaseCount() {
        int count = 0;
        if (this.releases != null) {
            for (Release release : this.releases) {
                if ((release.flags & Release.FLAG_PURCHASED) == 0) {
                    ++count;
                }
            }
        }
        return count;
    }

    /**
     * Crea una nuova istanza con {@link ComicsWithReleases#comics} inizializzato.
     * Mentre {@link ComicsWithReleases#comics} è null.
     *
     * @return nuova istanza vuota
     */
    public static ComicsWithReleases createNew() {
        final ComicsWithReleases cwr = new ComicsWithReleases();
        cwr.comics = Comics.create("");
        // cwr.releases => lo lascio null
        return cwr;
    }
}
