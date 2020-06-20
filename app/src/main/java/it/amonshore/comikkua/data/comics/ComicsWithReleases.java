package it.amonshore.comikkua.data.comics;

import android.text.TextUtils;

import org.w3c.dom.Text;

import java.util.List;

import androidx.annotation.Nullable;
import androidx.room.Embedded;
import androidx.room.Entity;
import androidx.room.Relation;
import it.amonshore.comikkua.DateFormatterHelper;
import it.amonshore.comikkua.data.release.Release;

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

    @Nullable
    public Release getLastPurchasedRelease() {
        // presumo che siano ordinate per numero
        if (this.releases == null || this.releases.size() == 0) {
            return null;
        } else {
            Release last = null;
            for (Release release : this.releases) {
                if (release.purchased) {
                    last = release;
                } else {
                    break;
                }
            }
            return last;
        }
    }

    @Nullable
    public Release getNextToPurchaseRelease() {
        // presumo che siano ordinate per numero
        if (this.releases == null || this.releases.size() == 0) {
            return null;
        } else {
            for (Release release : this.releases) {
                if (!release.purchased) {
                    return release;
                }
            }
            return null;
        }
    }

    @Nullable
    public Release getLastRelease() {
        // presumo che siano ordinate per numero
        if (this.releases == null || this.releases.size() == 0) {
            return null;
        } else {
            return this.releases.get(this.releases.size() - 1);
        }
    }

    /**
     * @return numero di uscite non ancora acquistate
     */
    public int getNotPurchasedReleaseCount() {
        int count = 0;
        if (this.releases != null) {
            for (Release release : this.releases) {
                if (!release.purchased) {
                    ++count;
                }
            }
        }
        return count;
    }

    public int getReleaseCount() {
        return this.releases == null ? 0 : this.releases.size();
    }

    /**
     * Crea una nuova release per il comics, impostando in automatico il numero (+1 rispetto all'ultimo),
     * e la data di uscita (in base alla periodicità).
     * La release create non viene aggiunta all'elenco {@link ComicsWithReleases#releases}.
     *
     * @return nuova release
     */
    public Release createNextRelease() {
        final Release lastRelease = getLastRelease();
        final Release nextRelease;
        if (lastRelease != null) {
            String nextDate = null;
            if (!TextUtils.isEmpty(lastRelease.date) && !TextUtils.isEmpty(this.comics.periodicity)) {
                nextDate = DateFormatterHelper.toNextPeriod(lastRelease.date, this.comics.periodicity);
            }
            nextRelease = Release.create(this.comics.id, lastRelease.number + 1, nextDate);
        } else {
            nextRelease = Release.create(this.comics.id, 1);
        }
        return nextRelease;
    }

    public int getNextReleaseNumber() {
        final Release lastRelease = getLastRelease();
        if (lastRelease == null) {
            return 0;
        } else {
            return lastRelease.number + 1;
        }
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj == this) return true;

        if (obj instanceof ComicsWithReleases) {
            final ComicsWithReleases other = (ComicsWithReleases) obj;
            if (other.comics.id == this.comics.id &&
                    other.comics.lastUpdate == this.comics.lastUpdate) {
                if (other.releases != null && this.releases != null &&
                        other.releases.size() == this.releases.size()) {
                    for (int ii = 0; ii < other.releases.size(); ii++) {
                        if (!other.releases.get(ii).equals(this.releases.get(ii))) {
                            return false;
                        }
                    }
                    return true;
                } else if (other.releases == null && this.releases == null) {
                    return true;
                } else {
                    return false;
                }
            } else {
                return false;
            }
        } else {
            return false;
        }
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
